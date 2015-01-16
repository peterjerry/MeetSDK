#include ".\apminidump.h"

#include <tchar.h>

#include <stdio.h>

std::string apMiniDump::s_dump_filename;

apMiniDump::apMiniDump(void)
{
}

apMiniDump::~apMiniDump(void)
{
}

void apMiniDump::setdump(const char* dump_filename)
{
	apMiniDump::s_dump_filename = dump_filename;

	// if this assert fires then you have two instances of MiniDumper
	// which is not allowed
	::SetUnhandledExceptionFilter( TopLevelFilter );

}

LONG apMiniDump::TopLevelFilter( struct _EXCEPTION_POINTERS *pExceptionInfo )
{
	char m_szAppName[1024] = {"DcnHelper"};

	LONG retval = EXCEPTION_CONTINUE_SEARCH;
	HWND hParent = NULL;						// find a better value for your app

	// firstly see if dbghelp.dll is around and has the function we need
	// look next to the EXE first, as the one in System32 might be old 
	// (e.g. Windows 2000)
	HMODULE hDll = NULL;
	char szDbgHelpPath[_MAX_PATH];

	if (GetModuleFileName( NULL, szDbgHelpPath, _MAX_PATH ))
	{
		char *pSlash = _tcsrchr( szDbgHelpPath, '\\' );
		if (pSlash)
		{
#if _MSC_VER >= 1700
			_tcscpy_s( pSlash+1, _MAX_PATH - (pSlash - szDbgHelpPath) -1, "DBGHELP.DLL" );
#else
			_tcscpy( pSlash+1, "DBGHELP.DLL" );
#endif
			hDll = ::LoadLibrary( szDbgHelpPath );
		}
	}

	if (hDll==NULL)
	{
		// load any version we can
		hDll = ::LoadLibrary( "DBGHELP.DLL" );
	}

	LPCTSTR szResult = NULL;

	if (hDll)
	{
		MINIDUMPWRITEDUMP pDump = (MINIDUMPWRITEDUMP)::GetProcAddress( hDll, "MiniDumpWriteDump" );
		if (pDump)
		{
			char szDumpPath[_MAX_PATH];
			char szScratch [_MAX_PATH];

			// work out a good place for the dump file
			//if (!GetTempPath( _MAX_PATH, szDumpPath ))
			//_tcscpy( szDumpPath, "c:\\log\\" );
			//_tcscat( szDumpPath, "mpa_service_dump" );
			//_tcscat( szDumpPath, ".dmp" );
#if _MSC_VER >= 1700
			_tcscpy_s( szDumpPath, _MAX_PATH, apMiniDump::s_dump_filename.c_str());
#else
			_tcscpy( szDumpPath, apMiniDump::s_dump_filename.c_str());
#endif

			// ask the user if they want to save a dump file
			//if (::MessageBox( NULL, "Something bad happened in your program, would you like to save a diagnostic file?", m_szAppName, MB_YESNO )==IDYES)
			{
				// create the file
				HANDLE hFile = ::CreateFile( szDumpPath, GENERIC_WRITE, FILE_SHARE_WRITE, NULL, CREATE_ALWAYS,\
											FILE_ATTRIBUTE_NORMAL, NULL );

				if (hFile!=INVALID_HANDLE_VALUE)
				{
					_MINIDUMP_EXCEPTION_INFORMATION ExInfo;

					ExInfo.ThreadId = ::GetCurrentThreadId();
					ExInfo.ExceptionPointers = pExceptionInfo;
					ExInfo.ClientPointers = NULL;

					// write the dump
					BOOL bOK = pDump( GetCurrentProcess(), GetCurrentProcessId(), hFile, MiniDumpWithDataSegs, &ExInfo, NULL, NULL );
					if (bOK)
					{
#if _MSC_VER >= 1700
						sprintf_s( szScratch, _MAX_PATH, "Saved dump file to '%s'", szDumpPath );
#else
						sprintf( szScratch, "Saved dump file to '%s'", szDumpPath );
#endif
						szResult = szScratch;
						retval = EXCEPTION_EXECUTE_HANDLER;
					}
					else
					{
#if _MSC_VER >= 1700
						sprintf_s( szScratch, _MAX_PATH, "Failed to save dump file to '%s' (error %d)", szDumpPath, GetLastError() );
#else
						sprintf( szScratch, "Failed to save dump file to '%s' (error %d)", szDumpPath, GetLastError() );
#endif
						szResult = szScratch;
					}
					::CloseHandle(hFile);
				}
				else
				{
#if _MSC_VER >= 1700
					sprintf_s( szScratch, _MAX_PATH, "Failed to create dump file '%s' (error %d)", szDumpPath, GetLastError() );
#else
					sprintf( szScratch, "Failed to create dump file '%s' (error %d)", szDumpPath, GetLastError() );
#endif
					szResult = szScratch;
				}
			}
		}
		else
		{
			szResult = "DBGHELP.DLL too old";
		}
	}
	else
	{
		szResult = "DBGHELP.DLL not found";
	}

	if (szResult) {
		::DebugBreak();
		::MessageBox( NULL, szResult, m_szAppName, MB_OK );
	}


	return retval;
}