#include "libplayer.h"

#define LOG_TAG "libplayer"
#include "pplog.h"
#include "platform/platforminfo.h"
#include "jniUtils.h" // for vstrcat
#include "FFMediaPlayer.h" // for player
#include "FFMediaExtractor.h" // for extractor
#include "OMXMediaPlayer.h" // for omx player
#include "gles2.h" // for gles2 renderer
#include <stdio.h>
#include <string.h> // for strcasestr
#include <jni.h>
#include <dlfcn.h> // for dlopen ...
#include <cpu-features.h>
#include <sys/system_properties.h> // for __system_property_get

#ifdef BUILD_TS_CONVERT
CONVERT_FUN convertFun = NULL;
#endif

extern JavaVM* gs_jvm;
extern PlatformInfo* gPlatformInfo;

static void* player_handle_software = NULL;

static const char* getCodecLibName(uint64_t cpuFeatures)
{
	return "libplayer.so";

	/*
	const char* codecLibName = NULL;
	
	PPLOGI("android_getCpuFamily %d", android_getCpuFamily());
	
	char value[PROP_VALUE_MAX];
	__system_property_get("ro.product.cpu.abi", value);
	char* occ = strcasestr(value,"x86");
	if (occ) {
		// x86 arch
		PPLOGI("the device is x86 platform");
		codecLibName = "libplayer.so";
		return codecLibName;
	}

	if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
	{
		//neon
		PPLOGI("the device supports neon");
		codecLibName = "libplayer_neon.so";
	}
	else if((cpuFeatures & ANDROID_CPU_ARM_FEATURE_ARMv7) != 0)
	{
		//v7_vfpv3d16
		PPLOGI("the device supports v7_vfpv3d16");
		codecLibName = "libplayer_tegra2.so";
	}
	else if ((cpuFeatures & ANDROID_CPU_ARM_FEATURE_VFP) != 0)
	{
		//armv6_vfp
		PPLOGI("the device supports armv6_vfp");
		codecLibName = "libplayer_v6_vfp.so";
	}
	else if((cpuFeatures & ANDROID_CPU_ARM_FEATURE_LDREX_STREX) != 0)
	{
		//armv6
		PPLOGI("the device supports armv6");
		codecLibName = "libplayer_v6.so";
	}
	else
	{
		//armv5te or lower
		PPLOGI("the device supports armv5te");
		codecLibName = "libplayer_v5te.so";
	}

	return codecLibName;*/
}

static
void* loadLibrary(const char* libPath)
{
	PPLOGD("Before Load lib %s", libPath);
	void* lib_handle = dlopen(libPath, RTLD_NOW);

	if (lib_handle == NULL) {
		PPLOGE("Load lib %s error: %s", libPath, dlerror());
		return NULL;
	}

	PPLOGI("Load lib %s success", libPath);
	return lib_handle;
}

bool loadPlayerLib()
{
	PPLOGI("loadPlayerLib");

	void** player_handle = NULL; // handle to shared library
	player_handle = &player_handle_software;

	PPLOGI("Before Player Library Load.");
	char* libPath = NULL;

	// load player lib
	if (*player_handle == NULL) {
		PPLOGI("using ffplay");
		uint64_t cpuFeatures = android_getCpuFeatures();
		const char* libName = getCodecLibName(cpuFeatures); //libplayer.so

		do {
			//0. try to load from specified lib path
			if (strcmp(gPlatformInfo->lib_path, "") != 0) {
				libPath = vstrcat(gPlatformInfo->lib_path, libName);
				PPLOGI("Load from specified lib path #0: %s", libPath);
				*player_handle = loadLibrary(libPath);
				if (!player_handle)
					PPLOGW("step0 loadLibrary failed: %s", libPath);
				free(libPath);
				if (*player_handle)
					break;
			}

			//1. try to load from app lib path
			libPath = vstrcat(gPlatformInfo->app_path, "lib/", libName);
			PPLOGI("Load from app lib path #1: %s", libPath);
			*player_handle = loadLibrary(libPath);
			if (!player_handle)
				PPLOGW("step1 loadLibrary failed: %s", libPath);
			free(libPath);
			if (*player_handle)
				break;

			// 2015.6.9 guoliangma added to fix arm64 load error
			//2. try to load system lib path
			PPLOGI("Load from system lib path #2: %s", libName);
			*player_handle = loadLibrary(libName);
			if (!player_handle)
				PPLOGW("step2 loadLibrary failed: %s", libPath);
		}while(0);

		if (NULL == *player_handle) {
			PPLOGE("oops: all loadlibrary try failed");
			return false;
		}
	}

	PPLOGI("Before init component");
#ifdef BUILD_FFPLAYER
	if (!setup_player(*player_handle))
		return false;
#endif

#ifdef BUILD_TS_CONVERT
#ifdef BUILD_ONE_LIB
	convertFun = my_convert;
#else
	convertFun = (CONVERT_FUN)dlsym(*player_handle, "my_convert");
	if (convertFun == NULL) {
		PPLOGE("Init convert() failed: %s", dlerror());
		return false;
	}
#endif
#endif

#ifdef BUILD_FFEXTRACTOR
	if (!setup_extractor(*player_handle))
		return false;
#endif

#ifdef BUILD_OMXPLAYER
	if (!setup_omxplayer(*player_handle))
		return false;
#endif

#ifdef BUILD_GLES
	if (!setup_renderer(*player_handle))
		return false;
#endif

	PPLOGI("After init component");

	return true;
}

void unloadPlayerLib()
{
#ifndef BUILD_ONE_LIB
	PPLOGD("Start unloading player lib");
	if (player_handle_software != NULL) {
		dlclose(player_handle_software);
		player_handle_software = NULL;
	}
#endif
}