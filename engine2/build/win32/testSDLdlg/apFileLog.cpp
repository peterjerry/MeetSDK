/*******************************************************************
File Name:     apFileLog.cpp
Author:        Shawn.Zhang
Security:      SEACHANGE SHANGHAI
Description:   implement class apLog
Function Inventory: 
Modification Log:
When           Version        Who				What
---------------------------------------------------------------------
2008/07/07		1.0.0.0		  Shawn.Zhang		Create
				1.0.0.1		  Shawn.Zhang		Log can share open/delete, limit size    
				1.0.0.2		  Shawn.Zhang		Log resume
				1.0.0.3		  Shawn.Zhang		set level
				1.0.0.4		  Shawn.Zhang		Add instance ID
********************************************************************/

#include "stdafx.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tchar.h>

#include <stdio.h>
#include <stdarg.h>

#include <fstream>

#include "apFileLog.h"

using namespace std;

const int MAX_LOG_SIZE = 32*1024*1024;

long apLog::m_nContentSize = 0;
apLog::LOG_LEVEL apLog::m_eLevel;
long			 apLog::m_nInstanceID = 0;

HANDLE g_fLog = INVALID_HANDLE_VALUE;

std::string getTimeStr()
{
	const int buf_size = 128;
	char szBuf[buf_size];
	SYSTEMTIME st;
	GetLocalTime(&st);

	int n = _snprintf_s(szBuf, buf_size, "%02d-%02d-%02d %02d:%02d:%02d %03d %05d\t",
		st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond, st.wMilliseconds, apLog::m_nInstanceID);

	return string(szBuf);
}

std::string getLogLevel(apLog::LOG_LEVEL l)
{
	string s;
	if( l == apLog::error)
		s = "error ";
	if( l == apLog::warning)
		s = "warning ";
	if( l == apLog::info)
		s = "info ";
	if( l == apLog::detail)
		s = "detail ";
	return s;
}

bool apLog::init(const char* szLogName, apLog::LOG_LEVEL l)
{
	m_nContentSize	= 0;
	m_eLevel		= l;

	g_fLog = CreateFile(szLogName,            
		GENERIC_WRITE,          
		FILE_SHARE_READ|FILE_SHARE_WRITE|FILE_SHARE_DELETE,
		NULL,                  
		OPEN_ALWAYS,				
		FILE_ATTRIBUTE_NORMAL, 
		NULL);                 
	if (g_fLog == INVALID_HANDLE_VALUE) 
		return false;

	std::string sHeader("\r\n***********************************************\r\n Log ready to begin.\r\n\r\n");
	DWORD dwWritten = 0;
	WriteFile(g_fLog, sHeader.c_str(), (DWORD)sHeader.size(), &dwWritten, NULL);
	m_nContentSize += dwWritten;

	if(m_nContentSize >= MAX_LOG_SIZE)	{
		SetFilePointer(g_fLog, 0, 0, FILE_BEGIN);
		m_nContentSize = 0;
	}
	else
	{
		DWORD dwSize = SetFilePointer(g_fLog, 0, 0, FILE_END);
		m_nContentSize += dwSize;
	}

	return true;
}

void apLog::clear()
{
	if(g_fLog != INVALID_HANDLE_VALUE)
		CloseHandle(g_fLog);
	g_fLog = INVALID_HANDLE_VALUE;
}

void apLog::print(long instanceID, enum LOG_LEVEL level, const char* szFmt, ...)
{
	try
	{
		if((int)level > (int)m_eLevel)  //more 
			return;

		string sLog;
		const int max_buf_size = 1024*4;
		char szBuf[max_buf_size];

		va_list args;
		va_start(args, szFmt);
		int n = _vsnprintf_s(szBuf, max_buf_size, szFmt, args);
		va_end(args);

		m_nInstanceID = instanceID;

		sLog += getTimeStr();
		sLog += getLogLevel(level);
		sLog += szBuf;
		sLog += "\r\n";

		if(g_fLog != INVALID_HANDLE_VALUE)
		{
			DWORD dwWritten = 0;
			WriteFile(g_fLog, sLog.c_str(), (DWORD)sLog.size(), &dwWritten, NULL);
			m_nContentSize += dwWritten;

			if(m_nContentSize >= MAX_LOG_SIZE)	{
				SetFilePointer(g_fLog, 0, 0, FILE_BEGIN);
				m_nContentSize = 0;
			}
		}
	}
	catch(...)
	{

	}
}