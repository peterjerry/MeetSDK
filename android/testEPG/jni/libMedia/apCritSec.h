#ifndef __APCRITSEC_H__
#define __APCRITSEC_H__

#pragma once

#include <Winbase.h>
//////////////////////////////////////////////////////////////////////////
// class CBaseCritSec
class CBaseCritSec
{
public:
	CBaseCritSec()
	{
		InitializeCriticalSection(&m_CritSec);
	}

	virtual ~CBaseCritSec()
	{
		DeleteCriticalSection(&m_CritSec);
	}
	
public:
	virtual void Lock()
	{
		EnterCriticalSection(&m_CritSec);
	}

	virtual void UnLock()
	{
		LeaveCriticalSection(&m_CritSec);
	}

	virtual BOOL IsLock()
	{
		return TRUE;//TryEnterCriticalSection(&m_CritSec);
	}
	
private:
	CRITICAL_SECTION m_CritSec;
};

//////////////////////////////////////////////////////////////////////////
// class CAutoLockCritSec
class CAutoLockCritSec
{
public:
	CAutoLockCritSec(CBaseCritSec *pCritSec)
	{
		m_Lock = pCritSec;
		if(m_Lock != NULL)
			m_Lock->Lock();
	}

	virtual ~CAutoLockCritSec()
	{
		if(m_Lock != NULL)
			m_Lock->UnLock();
	}
	
private:
	CBaseCritSec *m_Lock;
};


#endif //__APCRITSEC_H__
