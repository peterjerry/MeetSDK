#ifndef _STRPTIME_H_
#define _STRPTIME_H_

extern "C" {
char * strptime(const char *buf, const char *fmt, struct tm *tm);
}

#endif // _STRPTIME_H_