#pragma once

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include "dbghelp.h"
#include <string>

// based on dbghelp.h
typedef BOOL (WINAPI *MINIDUMPWRITEDUMP)(HANDLE hProcess, DWORD dwPid, HANDLE hFile, MINIDUMP_TYPE DumpType,
									CONST PMINIDUMP_EXCEPTION_INFORMATION ExceptionParam,
									CONST PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
									CONST PMINIDUMP_CALLBACK_INFORMATION CallbackParam
									);

class apMiniDump
{
public:
	apMiniDump(void);
	~apMiniDump(void);
	void setdump(const char* dump_filename);

private:
	static LONG WINAPI TopLevelFilter( struct _EXCEPTION_POINTERS *pExceptionInfo );
private:
	static std::string s_dump_filename;
};
