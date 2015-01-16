/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */


#ifndef FF_LOOP_H_

#define FF_LOOP_H_

#include <pthread.h>
#include "list.h"

typedef int32_t event_id;

typedef void (* EventCallback)(void *opaque);

class Loop
{
public:
	struct Event {
		event_id		id;
		uint64_t		index;
		int64_t			realtimeUs;
		EventCallback	action;
	};

    Loop();
    ~Loop();

    void start();
    
	void stop();

	bool isRunning();

	void setInstance(void *ins){mInstance = ins;}

	// Posts an event to the front of the queue (after all events that
    // have previously been posted to the front but before timed events).
    int64_t postEventTohHeader(Event *evt);

    int64_t postEventToBack(Event *evt);

    // It is an error to post an event with a negative delay.
    int64_t postEventWithDelay(Event *evt, int64_t delayMs);

    void cancelEvent(event_id id);

private:
	Loop(const Loop &);
    
	Loop &operator=(const Loop &);

	void SetRunning(bool isRunning);

	// If the event is to be posted at a time that has already passed,
    // it will fire as soon as possible.
    int64_t postTimedEvent(Event *evt, int64_t realtimeUs);

	void dumpEventList();

    static void *ThreadWrapper(void *me);

    void threadEntry();

private:
    pthread_t		mThread;
    pthread_mutex_t mLock;
    pthread_cond_t	mQueueNotEmptyCondition;
    pthread_cond_t	mQueueHeadChangedCondition;

	void*			mInstance;
	
    List			mEvtQueue;
    uint64_t		mEventIndex;
    
	bool			mRunning;
	pthread_mutex_t mLockState;
};

#endif 
