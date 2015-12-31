/*******************************************************************
	File Name:     apFileLog.h
	Author:        Shawn.Zhang
	Security:      SEACHANGE SHANGHAI
	Description:   Implement class apLog
	Function Inventory: 
	Modification Log:
	When           Version        Who				What
	---------------------------------------------------------------------
	2008/07/07		1.0.0.0		  Shawn.Zhang		Create
					1.0.0.4
********************************************************************/

#pragma once

class apLog
{
public:
	typedef enum LOG_LEVEL
	{
		error = 0, warning = 10, info = 20, detail = 30

	} LOG_LEVEL;

	static bool init(const char* szLogName, apLog::LOG_LEVEL l = LOG_LEVEL::info);
	static void clear();

	static void print(long instanceID, enum LOG_LEVEL level, const char* szFmt, ...);

private:	
	static long				m_nContentSize;
	static LOG_LEVEL		m_eLevel;
public:
	static long				m_nInstanceID;
};

