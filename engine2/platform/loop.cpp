/*
 * Copyright (C) 2015 Guoliang Ma  guoliangma@pptv.com
 *
 */

#define __STDINT_LIMITS

#include "loop.h"

#include <stdint.h>
#ifndef _MSC_VER
#include <sys/time.h>
#include <sys/resource.h>
#include <errno.h>
#include <unistd.h>
#endif
#include <stdint.h>
#include "autolock.h"
#include "utils.h"
#define LOG_TAG "EventLoop"
#include "log.h"

#if defined(__CYGWIN__)
#ifndef   UINT64_C
#define   UINT64_C(value)__CONCAT(value,ULL)
#endif
#define INT64_MIN        (__INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (__INT64_C(9223372036854775807))
#endif

#if defined(_MSC_VER)
#ifndef UINT64_C
#define UINT64_C(val) val##ui64
#define INT64_C(val)  val##i64
#define INT64_MIN        (INT64_C(-9223372036854775807)-1)
#define INT64_MAX        (INT64_C(9223372036854775807))
#endif
#endif

#ifdef __ANDROID__
#include <jni.h>
extern JavaVM* gs_jvm;
#endif

Event::~Event()
{
}

void StopEvent::action(void *opaque, int64_t now_us)
{
	EventLoop *ins = (EventLoop *)opaque;
	ins->setStop();
}

EventLoop::EventLoop()
{
    mRunning			= false;
	mStopped			= false;

    mEventIndex			= 0;

    pthread_mutex_init(&mLock, NULL);
	pthread_mutex_init(&mLockState, NULL);
    pthread_cond_init(&mQueueNotEmptyCondition, NULL);
    pthread_cond_init(&mQueueHeadChangedCondition, NULL);
}

EventLoop::~EventLoop()
{
    stop(false);
    
    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mLockState);
    pthread_cond_destroy(&mQueueNotEmptyCondition);
    pthread_cond_destroy(&mQueueHeadChangedCondition);
    LOGI("Loop destructor finished");
}

void EventLoop::start()
{
    if (isRunning())
        return;

    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
	
	SetRunning(true);

    pthread_create(&mThread, &attr, ThreadWrapper, this);

    pthread_attr_destroy(&attr);
}

void EventLoop::stop(bool flush)
{
    if (!isRunning())
        return;

	StopEvent *stopEvent = new StopEvent(this);
	if (flush)
		postEventToBack(stopEvent);
	else
		postTimedEvent(stopEvent, INT64_MIN);

	void *dummy;
	LOGI("before pthread_join %p", mThread);
	pthread_join(mThread, &dummy);

	mEvtQueue.Clear();

	mRunning = false;
	LOGI("after EventLoop stop()");
}

bool EventLoop::isRunning()
{
	AutoLock autoLock(&mLockState);
	return mRunning;
}

void EventLoop::SetRunning(bool isRunning)
{
	AutoLock autoLock(&mLockState);
	mRunning = isRunning;
}


int64_t EventLoop::postEventTohHeader(Event *evt)
{
    // Reserve an earlier timeslot an INT64_MIN to be able to post
    // the StopEvent to the absolute head of the queue.
#ifdef _MSC_VER
	return postTimedEvent(evt, 0);
#else
    return postTimedEvent(evt, INT64_MIN + 1);
#endif
}

int64_t EventLoop::postEventToBack(Event *evt)
{
    return postTimedEvent(evt, INT64_MAX);
}

int64_t EventLoop::postEventWithDelay(Event *evt, int64_t delayMs)
{
    return postTimedEvent(evt, getNowUs() + delayMs * 1000ll);
}

int64_t EventLoop::postTimedEvent(Event *evt, int64_t realtimeUs)
{
	if (!isRunning())
		return -1;

	if (evt == NULL)
		return -1;

	AutoLock autoLock(&mLock);

    // to find the proper position to place new event
	int32_t index = 0;
    while (index < mEvtQueue.GetLength()) {
        Event* item = (Event *)mEvtQueue[index];
		if (item && (realtimeUs < item->m_realtimeUs))
			break;

        index++;
	}

	evt->m_index		= mEventIndex;
	evt->m_realtimeUs	= realtimeUs;

	if (index == 0)
		pthread_cond_signal(&mQueueHeadChangedCondition);

	mEvtQueue.Insert(index, evt);

#ifdef _MSC_VER
	LOGD("Insert event:%I64d(%d)", evt->m_index, evt->m_id);
#else
	LOGD("Insert event:%lld(%d)", evt->m_index, evt->m_id);
#endif
	mEventIndex++;

	pthread_cond_signal(&mQueueNotEmptyCondition);

    return evt->m_index;
}

void EventLoop::cancelEvent(event_id id) {
	Event* evt = NULL;
    for (int i=0; i < mEvtQueue.GetLength(); i++) {
        evt = (Event *)mEvtQueue[i];
		if(evt->m_id == id) {
			mEvtQueue.Remove(i);
			if (i == 0) {
				pthread_mutex_lock(&mLock);
				pthread_cond_signal(&mQueueHeadChangedCondition);
				pthread_mutex_unlock(&mLock);
			}

			delete evt;
			evt = NULL;
		}
	}
}

// static
void * EventLoop::ThreadWrapper(void *me)
{
#ifdef __ANDROID__
    JNIEnv *env = NULL;
    gs_jvm->AttachCurrentThread(&env, NULL);
    LOGD("getpriority before:%d", getpriority(PRIO_PROCESS, 0));
    LOGD("sched_getscheduler:%d", sched_getscheduler(0));

    int videoThreadPriority = -6;
    if(setpriority(PRIO_PROCESS, 0, videoThreadPriority) != 0) {
        LOGE("set video thread priority failed");
    }
    LOGD("getpriority after:%d", getpriority(PRIO_PROCESS, 0));
#endif
    
	LOGI("EventLoop thread started");
    static_cast<EventLoop *>(me)->threadEntry();
    
#ifdef __ANDROID__
    gs_jvm->DetachCurrentThread();
#endif
    LOGI("EventLoop thread exited");
    return NULL;
}

void EventLoop::threadEntry() {
    //prctl(PR_SET_NAME, (unsigned long)"ffplayer Loop", 0, 0, 0);

	int64_t nowUs = 0;

	pthread_mutex_lock(&mLock);
    while (!mStopped) {
		Event* evt = NULL;

		while (mEvtQueue.IsEmpty())
			pthread_cond_wait(&mQueueNotEmptyCondition, &mLock);

		while (!mStopped) {
			if (mEvtQueue.IsEmpty()) {
				// The only event in the queue could have been cancelled
				// while we were waiting for its scheduled time.
				break;
			}

			evt = (Event *)mEvtQueue[0];

			nowUs = getNowUs();
			int64_t whenUs = evt->m_realtimeUs;
#ifdef _MSC_VER
		//LOGI("nowUs:%I64d, whenUs:%I64d", nowUs, whenUs);
#else
		//LOGI("nowUs:%lld, whenUs:%lld", nowUs, whenUs);
#endif

			int64_t delayUs;
			if (whenUs < 0 || whenUs == INT64_MAX) {
				delayUs = 0;
			} else {
				delayUs = whenUs - nowUs;
			}

			if (delayUs > 0) {
				static int64_t kMaxTimeoutUs = 10000000ll;  // 10 secs
				bool timeoutCapped = false;
				if (delayUs > kMaxTimeoutUs) {
					LOGW("delay_us exceeds max timeout: %lld us", delayUs);

					// We'll never block for more than 10 secs, instead
					// we will split up the full timeout into chunks of
					// 10 secs at a time. This will also avoid overflow
					// when converting from us to ns.
					delayUs = kMaxTimeoutUs;
					timeoutCapped = true;
				}

				int32_t err = wait(delayUs);

				if (!timeoutCapped && err == -ETIMEDOUT) {
					// We finally hit the time this event is supposed to
					// trigger.
					nowUs = getNowUs();
					break;
				}
			}

			//dumpEventList();

			//this time, get 1st evt(may be different from the one before sleep)
			evt = (Event *)mEvtQueue.Remove(0);
			if (evt) {
				// Fire event with the lock NOT held.
			
				LOGD("action #%lld %d", evt->m_index, evt->m_id);
				pthread_mutex_unlock(&mLock);
				evt->action(evt->m_opaque, nowUs);
				pthread_mutex_lock(&mLock);
				LOGD("action #%lld %d done", evt->m_index, evt->m_id);

				delete evt;
				evt = NULL;
			}
		} // end of while() 3
    } // end of while

	pthread_mutex_unlock(&mLock);
}

int EventLoop::wait(int64_t usec)
{
	struct timespec ts;
	ts.tv_sec = usec / 1000000ll; // unit: sec
	ts.tv_nsec = (usec % 1000000ll) * 1000ll;

#if defined(__CYGWIN__) || defined(_MSC_VER)
	int64_t now_usec = getNowUs();
	int64_t now_sec = now_usec / 1000000;
	now_usec = now_usec - now_sec * 1000000;
	ts.tv_sec	+= now_sec;
	ts.tv_nsec	+= (long)now_usec * 1000;
			
	return pthread_cond_timedwait(&mQueueHeadChangedCondition, &mLock, &ts);
#else
	return pthread_cond_timedwait_relative_np(&mQueueHeadChangedCondition, &mLock, &ts);
#endif
}

void EventLoop::onStop(void * opaque)
{
	EventLoop *ins = (EventLoop *)opaque;
	ins->mStopped = true;
	LOGI("onStop set mStopped done!");
}

void EventLoop::dumpEventList()
{
	Event* evt = NULL;
	LOGI("event list start, len = %d", mEvtQueue.GetLength());
    for (int i=0; i < mEvtQueue.GetLength(); i++) {
        evt = (Event *)mEvtQueue[i];
#ifndef _MSC_VER
		LOGI("event list: index %lld, id %d, realtime %lld", evt->m_index, evt->m_id, evt->m_realtimeUs);
#endif
	}
	LOGI("event list end");
}