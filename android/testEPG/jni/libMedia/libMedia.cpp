// libMedia.cpp : 定义 DLL 应用程序的导出函数。
//

#include "stdafx.h"
#include "libMedia.h"
#include "com_pplive_epg_pptv_NativeMedia.h"
#include "apFormatConverter.h"
#define LOG_TAG "libMedia"
#include "log.h"

// 这是导出变量的一个示例
LIBMEDIA_API int nlibMedia=0;

// 这是导出函数的一个示例。
LIBMEDIA_API int fnlibMedia(void)
{
	return 42;
}

// 这是已导出类的构造函数。
// 有关类定义的信息，请参阅 libMedia.h
ClibMedia::ClibMedia()
{
	return;
}

extern "C" JNIEXPORT jint JNICALL Java_com_pplive_epg_pptv_NativeMedia_test
  (JNIEnv *env, jclass thiz, jstring instring)
{
	const char *str = (const char *)env->GetStringUTFChars(instring, JNI_FALSE);
    printf("%s\n",str);
    //testfunc(str);
    env->ReleaseStringUTFChars(instring, str);
    return strlen(str);
}

/*
 * Class:     com_pplive_epg_pptv_NativeMedia
 * Method:    Convert
 * Signature: ([BI[BII)I
 */
extern "C" JNIEXPORT jint JNICALL Java_com_pplive_epg_pptv_NativeMedia_Convert
  (JNIEnv *env, jclass clazz, jbyteArray in_flv, jint in_size, jbyteArray out_ts, jint process_timestamp, jint first_seg)
{
	LOGI("Convert()");

	int ret = -1;

	jbyte* flv_data = env->GetByteArrayElements(in_flv, NULL);
	jsize flv_data_size = in_size;

	jbyte* ts_data = env->GetByteArrayElements(out_ts, NULL);
	jsize ts_data_size = env->GetArrayLength(out_ts);

	LOGI("before call my_convert() flv %p %d, ts %p %d", flv_data, flv_data_size, ts_data, ts_data_size);
	apFormatConverter cvt;
	bool bRet = cvt.convert((uint8_t *)flv_data, flv_data_size, (uint8_t *)ts_data, (int *)&ts_data_size, process_timestamp, first_seg);
	if (bRet)
		ret = ts_data_size;
	LOGI("after call my_convert() %d", ret);

	env->ReleaseByteArrayElements(in_flv, flv_data, 0);
	env->ReleaseByteArrayElements(out_ts, ts_data, 0);
	return ret;
}

