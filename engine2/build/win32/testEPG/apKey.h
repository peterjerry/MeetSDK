#pragma once
#include <stdint.h>

class apKey
{
public:
	apKey(void);
	~apKey(void);

	static uint8_t * genKey(int64_t server_time);
};

