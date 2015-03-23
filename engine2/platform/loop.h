/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */


#ifndef _EVENT_LOOP_H_

#define _EVENT_LOOP_H_

#include <pthread.h>
#include "list.h"

typedef int32_t event_id;

#define EVENT_LOOP_STOP 1001

class EventLoop
{
public:
	class Event {
	public:
		Event():m_id(0), m_index(0), m_realtimeUs(0), m_opaque(NULL){}
		~Event(){}

		event_id		m_id;
		uint64_t		m_index;
		int64_t			m_realtimeUs;
		void*			m_opaque;
		virtual void action(void *opaque, int64_t now_us) = 0;
	};

    EventLoop();
    ~EventLoop();

    void start();
    
	void stop(bool flush);

	bool isRunning();

	// Posts an event to the front of the queue (after all events that
    // have previously been posted to the front but before timed events).
    int64_t postEventTohHeader(Event *evt);

    int64_t postEventToBack(Event *evt);

    // It is an error to post an event with a negative delay.
    int64_t postEventWithDelay(Event *evt, int64_t delayMs);

    void cancelEvent(event_id id);

private:
	EventLoop(const EventLoop &);
    
	EventLoop &operator=(const EventLoop &);

	void SetRunning(bool isRunning);

	// If the event is to be posted at a time that has already passed,
    // it will fire as soon as possible.
    int64_t postTimedEvent(Event *evt, int64_t realtimeUs);

	void dumpEventList();

    static void *ThreadWrapper(void *me);

    void threadEntry();

	static void onStop(void * opaque);

	int wait(int64_t usec);

private:
	class StopEvent:public EventLoop::Event {
	public:
		StopEvent(void * opaque){
			m_id		= EVENT_LOOP_STOP;
			m_opaque	= opaque;
		}
		~StopEvent(){}
		virtual void action(void *opaque, int64_t now_us);
	};

    pthread_t		mThread;
    pthread_mutex_t mLock;
    pthread_cond_t	mQueueNotEmptyCondition;
    pthread_cond_t	mQueueHeadChangedCondition;
	
    List			mEvtQueue;
    uint64_t		mEventIndex;
    
	bool			mRunning;
	bool			mStopped;

	pthread_mutex_t mLockState;
};

#endif // _EVENT_LOOP_H_
