/*******************************************************************
	File Name:     apThread.h
	Author:        Shawn.Zhang
	Security:      SEACHANGE SHANGHAI
	Description:   implement class apThread
	Function Inventory: 
	Modification Log:
	When           Version        Who				What
	---------------------------------------------------------------------
	2007/06/28		1.0	0.0		  Shawn.Zhang		Create
	2008/08/22		1.0.0.1			Shawn.Zhang		stop in case block when thread proc exit first.
	2008/08/22		1.0.0.2			Shawn.Zhang		change thread priority.
	2009/03/31		1.0.0.3			Shawn.Zhang		add recycle.
	2009/03/31		1.0.0.4			Shawn.Zhang		HANDLE to void*; THREAD_PRIORITY_NORMAL to 0
********************************************************************/

#ifndef _WIN32_SHAWN_APTHREAD_H_
#define _WIN32_SHAWN_APTHREAD_H_

#pragma once

/////////////////////////////////////////////////////
/// apThread -  win32 thread wrapper.

class apThread
{
public:
	apThread  ();
	~apThread ();

	// is thread should exit thread_proc.
	bool isexit(unsigned int milliseconds = 0);

	// Start the thread running
	bool start(int nPriority = 0/*THREAD_PRIORITY_NORMAL*/) ;

	// Stop the thread
	bool stop();

	// Terminate the thread. Ungraceful and may result in locking/resource problems.
	bool end ();

	// Wait for thread to complete, 0 means infinite
	bool wait (unsigned int seconds = 0);

	void* exit_evt() const
	{
		return exitevent_;
	}

protected:
#ifdef _WIN64
	UINT_PTR	threadid_;	// thread id
#else
	void*		threadid_;	// thread id
#endif
	void*		exitevent_;	// exit event

	// Thread entry function, invoke thread_proc
	static unsigned int __stdcall thread_entry (void* obj);

	// Thread function, Override this in derived classes.
	virtual void thread_proc() = 0;

private:
	void recycle();
};


#endif // _WIN32_SHAWN_APTHREAD_H_
