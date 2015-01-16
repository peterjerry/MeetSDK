/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

//#undef __STRICT_ANSI__
#define __STDINT_LIMITS
//#define __STDC_LIMIT_MACROS

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
#define LOG_TAG "Loop"
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

Loop::Loop()
{
    mRunning = false;
    mEventIndex = 1;
    pthread_mutex_init(&mLock, NULL);
	pthread_mutex_init(&mLockState, NULL);
    pthread_cond_init(&mQueueNotEmptyCondition, NULL);
    pthread_cond_init(&mQueueHeadChangedCondition, NULL);
}

Loop::~Loop()
{
    stop();
    
    pthread_mutex_destroy(&mLock);
	pthread_mutex_destroy(&mLockState);
    pthread_cond_destroy(&mQueueNotEmptyCondition);
    pthread_cond_destroy(&mQueueHeadChangedCondition);
    LOGI("Loop destructor finished");
}

void Loop::start()
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

void Loop::stop()
{
    if (!isRunning())
        return;

	LOGI("Loop stop()");
	SetRunning(false);
	LOGI("Set running to false");

	pthread_mutex_lock(&mLock);
	pthread_cond_signal(&mQueueNotEmptyCondition);
    pthread_mutex_unlock(&mLock);

	LOGI("mQueueNotEmptyCondition signal");

	pthread_mutex_lock(&mLock);
	pthread_cond_signal(&mQueueHeadChangedCondition);
    pthread_mutex_unlock(&mLock);

	LOGI("mQueueHeadChangedCondition signal");

	LOGI("before pthread_join %p", mThread);
    pthread_join(mThread, NULL);

    mEvtQueue.Clear();
	LOGI("after Loop stop()");
}

bool Loop::isRunning()
{
	AutoLock autoLock(&mLockState);
	return mRunning;
}

void Loop::SetRunning(bool isRunning)
{
	AutoLock autoLock(&mLockState);
	mRunning = isRunning;
}


int64_t Loop::postEventTohHeader(Event *evt)
{
    // Reserve an earlier timeslot an INT64_MIN to be able to post
    // the StopEvent to the absolute head of the queue.
#ifdef _MSC_VER
	return postTimedEvent(evt, 0);
#else
    return postTimedEvent(evt, INT64_MIN + 1);
#endif
}

int64_t Loop::postEventToBack(Event *evt)
{
    return postTimedEvent(evt, INT64_MAX);
}

int64_t Loop::postEventWithDelay(Event *evt, int64_t delayMs)
{
    return postTimedEvent(evt, getNowUs() + delayMs * 1000ll);
}

int64_t Loop::postTimedEvent(Event *evt, int64_t realtimeUs)
{
	if (!isRunning())
		return -1;

	if (evt == NULL)
		return -1;

    // to find the proper position to place new event
	int32_t index = 0;
    while (index < mEvtQueue.GetLength()) {
        Event* item = (Event *)mEvtQueue[index];
		if (item && (realtimeUs < item->realtimeUs))
			break;

        index++;
	}

    Event* new_evt		= new Event();
	new_evt->id			= evt->id;
	new_evt->index		= mEventIndex;
	new_evt->action		= evt->action;
    new_evt->realtimeUs	= realtimeUs;
	mEvtQueue.Insert(index, new_evt);
#ifdef _MSC_VER
	LOGD("Insert event:%I64d(%d)", new_evt->index, new_evt->id);
#else
	LOGD("Insert event:%lld(%d)", new_evt->index, new_evt->id);
#endif
	mEventIndex++;

	if (mEvtQueue.GetLength() == 1) {// size change from 0 to 1
		pthread_mutex_lock(&mLock);
        pthread_cond_signal(&mQueueNotEmptyCondition);
		pthread_mutex_unlock(&mLock);
	}

    if (index == 0) {
		pthread_mutex_lock(&mLock);
        pthread_cond_signal(&mQueueHeadChangedCondition);
		pthread_mutex_unlock(&mLock);
	}

    return new_evt->index;
}

void Loop::cancelEvent(event_id id) {
	Event* evt = NULL;
    for (int i=0; i < mEvtQueue.GetLength(); i++) {
        evt = (Event *)mEvtQueue[i];
		if(evt->id == id) {
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
void *Loop::ThreadWrapper(void *me)
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
    
	LOGI("Loop thread started");
    static_cast<Loop *>(me)->threadEntry();
    
#ifdef __ANDROID__
    gs_jvm->DetachCurrentThread();
#endif
    LOGI("Loop thread exited");
    return NULL;
}

void Loop::threadEntry() {
    //prctl(PR_SET_NAME, (unsigned long)"ffplayer Loop", 0, 0, 0);

	int64_t nowUs = 0;
	Event* evt = NULL;
	//uint64_t eventIndex;

    while (isRunning()) {
        if (mEvtQueue.IsEmpty()) {
			pthread_mutex_lock(&mLock);
			pthread_cond_wait(&mQueueNotEmptyCondition, &mLock);
			pthread_mutex_unlock(&mLock);
		}

		if (mEvtQueue.IsEmpty()) {
			LOGI("event loop receive finish signal");
			break;
		}

		evt = (Event *)mEvtQueue[0];
		//eventIndex = evt->index;

		nowUs = getNowUs();
		int64_t whenUs = evt->realtimeUs;
#ifdef _MSC_VER
		//LOGI("nowUs:%I64d, whenUs:%I64d", nowUs, whenUs);
#else
		//LOGI("nowUs:%lld, whenUs:%lld", nowUs, whenUs);
#endif
		int64_t delayUs = 0;
		if (whenUs < 0 || whenUs == INT64_MAX)
			delayUs = 0;
		else
			delayUs = whenUs - nowUs;

		if (delayUs > 0) {
#ifdef _MSC_VER
			//LOGI("delayUs: %I64d", delayUs);
#else
			//LOGI("delayUs: %lld", delayUs);
#endif
			struct timespec ts;
			int32_t err;

			ts.tv_sec = delayUs / 1000000ll; // unit: sec
			ts.tv_nsec = (delayUs % 1000000ll) * 1000ll;
			pthread_mutex_lock(&mLock);
#if defined(__CYGWIN__) || defined(_MSC_VER)
			int64_t now_usec = getNowUs();
			int64_t now_sec = now_usec / 1000000;
			now_usec = now_usec - now_sec * 1000000;
			ts.tv_sec	+= now_sec;
            ts.tv_nsec	+= (long)now_usec * 1000;
			//ts.tv_sec = (long)(evt->realtimeUs / 1000000ll);
			//ts.tv_nsec = (long)(evt->realtimeUs - ts.tv_sec * 1000000ll);
			//LOGI("getNowMS %I64d, %I64d(%I64d)", getNowMs(), evt->realtimeUs / 1000, evt->realtimeUs / 1000 - getNowMs());
			
			err = pthread_cond_timedwait(&mQueueHeadChangedCondition, &mLock, &ts);
#else
			err = pthread_cond_timedwait_relative_np(&mQueueHeadChangedCondition, &mLock, &ts);
#endif
			pthread_mutex_unlock(&mLock);
			if (0 == err) {
				// interrupted by more urgent job!
				LOGD("interrupted by more urgent job");
				continue;
			}

			LOGD("pthread_cond_timedwait err = %d", err); // ETIMEDOUT 110(linux define 138)

			// wait interrupted by quit signal
			if(!isRunning()) {
				// all un-fired event is omited
				break;
			}
		}

		//dumpEventList();

		//this time, get 1st evt(may be different from the one before sleep)
        evt = (Event *)mEvtQueue.Remove(0);
		if (evt) {
            // Fire event with the lock NOT held.
			if(mInstance) {
				LOGD("fire %d %lld", evt->id, evt->index);
				evt->action(mInstance);
				LOGD("fire %d %lld done", evt->id, evt->index);
			}
        }
    } // end of while
}

void Loop::dumpEventList()
{
	Event* evt = NULL;
	LOGI("event list start, len = %d", mEvtQueue.GetLength());
    for (int i=0; i < mEvtQueue.GetLength(); i++) {
        evt = (Event *)mEvtQueue[i];
#ifndef _MSC_VER
		LOGI("event list: index %lld, id %d, realtime %lld", evt->index, evt->id, evt->realtimeUs);
#endif
	}
	LOGI("event list end");
}