#ifndef _LIB_PLAYER_H_
#define _LIB_PLAYER_H_

#include <stdint.h>

#ifdef BUILD_TS_CONVERT
typedef bool (*CONVERT_FUN) (uint8_t* , int , uint8_t* , int *, int, int);
extern CONVERT_FUN convertFun;
#endif

bool loadPlayerLib();

void unloadPlayerLib();

#endif // _LIB_PLAYER_H_