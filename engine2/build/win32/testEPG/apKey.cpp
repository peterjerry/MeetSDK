#include "stdafx.h"
#include "apKey.h"
#include <time.h>

int ENCRYPT_ROUNDS = 32;
int DELTA = 0x9E3779B9;
int FINAL_SUM = 0xC6EF3720;
int BLOCK_SIZE_TWICE = (4 << 2);

static int Str2Time(const uint8_t *str, int len);

static void Time2Str(int timet, uint8_t *str, int len);

static void TEncrypt(uint8_t *buffer, int buf_size, uint8_t *key, int len);

static int Str2Hex(uint8_t *buffer, int buf_size, uint8_t *hexstr, int hs_size);

static int64_t getuint(int64_t l);

apKey::apKey(void)
{
}


apKey::~apKey(void)
{
}

uint8_t * apKey::getKey(int64_t server_time)
{
	uint8_t bytes[16]	= {0};
    uint8_t key[16]		= {0};
	uint8_t result[33]	= {0};

    const char* keystr = "qqqqqww";

    for (int i = 0; i < 16; i++) {
        key[i] = i < strlen(keystr) ? keystr[i] : 0;
    }

    int64_t timet = server_time;
    timet -= 100;
    Time2Str((int)timet, bytes, 16);

    srand((unsigned int)time(NULL));

    for (int i = 0; i < 16; i++) {
        if (bytes[i] == 0)
            bytes[i] = rand() % 256;
    }

    TEncrypt(bytes, 16, key, 16);
    Str2Hex(bytes, 16, result, 33);

	uint8_t *result_str = new uint8_t[32 + 1];
	memcpy(result_str, result, 32);
	result_str[32] = '\0';
    return result_str;
}

static int GetkeyFromstr(uint8_t *str, int len)
{
	int key = 0;
	for (int i = 0; i < len; i++)
	{
		key ^= (int) (str[i]) << (i % 4 * 8);
	}
	return key;
}

static int Str2Hex(uint8_t *buffer, int buf_size, uint8_t *hexstr, int hs_size)
{
	if (hs_size < 2 * buf_size + 1)
		return 0;

	for (int i=0;i<buf_size;i++) {
		static char tmp[4] = {0};
		sprintf_s(tmp, "%02x", buffer[i]);
		memcpy(hexstr + 2 * i, tmp + 1, 1);
		memcpy(hexstr + 2 * i + 1, tmp, 1);
	}

	hexstr[2 * buf_size] = '\0';
	return 1;
}

static int Hex2Str(uint8_t *result, int hs_size, uint8_t *buffer, int buf_size)
{
	if (2 * buf_size < hs_size)
		return 0;

	for (int i = 0; i < hs_size / 2; i++) {
		buffer[i] = (byte) ((result[2 * i] - (result[2 * i] > '9' ? 'a' - (char) 10 : '0')) | ((result[2 * i + 1] - (result[2 * i + 1] > '9' ? 'a' - (char) 10
			: '0')) << 4));
	}

	return 1;
}

static void TEncrypt(uint8_t *buffer, int buf_size, uint8_t *key, int len)
{
	int64_t k0 = GetkeyFromstr(key, len);

	int64_t k1 = 0;
	int64_t k2 = 0;
	int64_t k3 = 0;
	k1 = k0 << 8 | k0 >> 24;
	k2 = k0 << 16 | k0 >> 16;
	k3 = k0 << 24 | k0 >> 8;
	for (int i = 0; i + BLOCK_SIZE_TWICE <= buf_size; i += BLOCK_SIZE_TWICE)
	{
		int64_t v0 = 0, v1 = 0, sum = 0;
		for (int k = 0; k < 4; k++) {
			v0 |= (int64_t) (buffer[i + k] & 0xff) << (k * 8);
			v1 |= (int64_t) (buffer[i + k + 4] & 0xff) << (k * 8);
		}

		for (int j = 0; j < ENCRYPT_ROUNDS; j++) {
			sum += DELTA;
			sum = getuint(sum);

			v0 += getuint(getuint(getuint(v1 << 4) + k0) ^ getuint(v1 + sum))
				^ getuint(getuint(v1 >> 5) + k1);
			v0 = getuint(v0);
			v1 += getuint(getuint(getuint(v0 << 4) + k2) ^ getuint(v0 + sum))
				^ getuint(getuint(v0 >> 5) + k3);
			v1 = getuint(v1);
		}

		for (int k = 0; k < 4; k++) {
			buffer[i + k]		= (v0 >> (k * 8)) & 0xFF;
			buffer[i + k + 4]	= (v1 >> (k * 8)) & 0xFF;
		}
	}
}

static int64_t getuint(int64_t l)
{
	return l & 0xffffffffL;
}

static int Str2Time(const uint8_t *str, int len)
{
	int key = 0;

	for (int i = 0; i < len && i < 8; i++) {
		key |= (int) (str[i] - (str[i] > '9' ? 'a' - 10 : '0')) << (28 - i % 8 * 4);
	}

	return key;
}

static void Time2Str(int timet, uint8_t *str, int len)
{
	for (int i = 0; i < len && i < 8; i++) {
		str[i] = (uint8_t) ((timet >> (28 - i % 8 * 4)) & 0x0F);
		str[i] += (uint8_t) (str[i] > 9 ? 'a' - 10 : '0');
	}
}