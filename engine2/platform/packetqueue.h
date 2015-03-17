/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */
 
#ifndef FF_PACKETQUEUE_H
#define FF_PACKETQUEUE_H
#include "list.h"
#include <pthread.h>

struct AVPacket;

// this class has lock protect
class PacketQueue
{
public:
	PacketQueue();
	~PacketQueue();
	
	status_t put(AVPacket* pkt);
	AVPacket* get();
	AVPacket* peek();
	uint32_t size();
	uint32_t count();
	void flush();
	int64_t duration();
	
private:
    uint32_t mCachedSize;
    uint32_t mCount;
	int64_t mDuration;
	List mPacketList;
	pthread_mutex_t mLock;
};

#endif // FF_PACKETQUEUE_H