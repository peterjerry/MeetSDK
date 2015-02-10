/*******************************************************************
File Name:     apThread.cpp
Author:        Shawn.Zhang
Security:     
Description:   implement class apThread
Function Inventory: 
Modification Log:
When           Version        Who				What
---------------------------------------------------------------------
2007/06/28		1.0.0.0			Shawn.Zhang		created.
2008/08/22		1.0.0.1			Shawn.Zhang		stop in case block when thread proc exit first.
2008/08/22		1.0.0.2			Shawn.Zhang		change thread priority.
2009/03/31		1.0.0.3			Shawn.Zhang		add recycle.
********************************************************************/
#include "stdafx.h"
#include <windows.h>
#include <process.h>    // _beginthreadex(), _endthreadex()
#include "apThread.h"

apThread::apThread  () : threadid_(0), exitevent_(NULL) 
{
	exitevent_ = CreateEvent(NULL, TRUE, FALSE, NULL);
}

apThread::~apThread ()
{
	recycle();
}

bool apThread::start (int nPriority) 
{	
	//if(exitevent_ == NULL)
	//	return false;
#ifdef _WIN64
		threadid_ = _beginthreadex (0, 0, thread_entry, this,
		0, NULL);
#else
	threadid_ = (void*)_beginthreadex (0, 0, thread_entry, this,
		0, (unsigned int*) &threadid_);
#endif
	//if (threadid_ != 0)
	//	SetThreadPriority((HANDLE)threadid_, nPriority);

	//if (threadid_ != 0)
	//	ResumeThread ((HANDLE)threadid_);

	return (threadid_ != 0);
}

bool apThread::stop()
{
	if(threadid_ == 0)
		return true;

	DWORD exitCode = 0;
	BOOL bRet = GetExitCodeThread((HANDLE)threadid_, &exitCode);

	if(bRet && exitCode == STILL_ACTIVE)
	{
		if(exitevent_)
		{
			SetEvent(exitevent_);
			wait();
		}
	}

	// coment out for re-start thread several times in RGB3
	//recycle();
	ResetEvent(exitevent_);
	if(threadid_)
		CloseHandle ((HANDLE)threadid_);
	threadid_ = 0;

	return true;
}

bool apThread::end() 
{
	TerminateThread ((HANDLE) threadid_, 0);
	return true;
}

bool apThread::wait (unsigned int milliseconds)
{
	DWORD wait = milliseconds;
	if (wait == 0) wait = INFINITE;
	DWORD status = WaitForSingleObject ((HANDLE) threadid_, wait);
	return (status != WAIT_TIMEOUT);
}

unsigned int __stdcall apThread::thread_entry (void* obj)
{
	// Call the overriden thread function
	apThread* t = reinterpret_cast<apThread*>(obj);
	t->thread_proc ();
	return 0;
}

bool apThread::isexit(unsigned int milliseconds)
{ 
	DWORD exitCode = 0;
	BOOL bRet = GetExitCodeThread((HANDLE)threadid_, &exitCode);

	if(bRet && exitCode != STILL_ACTIVE)
		return true;

	return (WAIT_OBJECT_0 == WaitForSingleObject(exitevent_, milliseconds)); 
}

void apThread::recycle()
{
	if(exitevent_)
		CloseHandle (exitevent_);
	exitevent_ = NULL;

	if(threadid_)
		CloseHandle ((HANDLE)threadid_);
	threadid_ = 0;
}