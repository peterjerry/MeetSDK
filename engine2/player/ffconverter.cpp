#include <stdint.h>
#ifdef BUILD_TS_CONVERT
#include "apFormatConverter.h" // for ts converter
#endif
#define LOG_TAG "ffconverter"
#include "log.h"

extern "C" bool my_convert(uint8_t* flv_data, int flv_data_size, uint8_t* ts_data, int *out_size, int process_timestamp, int first_seg)
{
	LOGI("my_convert()");
#ifdef BUILD_TS_CONVERT
	apFormatConverter converter;
	return converter.convert(flv_data, flv_data_size, ts_data, out_size, process_timestamp, first_seg);
#else
	LOGW("BUILD_TS_CONVERT is NOT enabled");
	return false;
#endif
}