/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */

#include "packetqueue.h"

#define LOG_TAG "PacketQueue"
#include "log.h"
#include "ppffmpeg.h"
#include "autolock.h"

PacketQueue::PacketQueue()
{
    mCachedSize = 0;
    mCount		= 0;
    mDuration	= 0;
	mLastPTS	= AV_NOPTS_VALUE;
	pthread_mutex_init(&mLock, NULL);
}

PacketQueue::~PacketQueue()
{
    flush();
	pthread_mutex_destroy(&mLock);
    LOGI("PacketQueue destructor");
}

status_t PacketQueue::put(AVPacket* pkt)
{
	AutoLock autoLock(&mLock);

    if (pkt == NULL)
        return ERROR;
    
	mPacketList.Append(pkt);
    mCachedSize += pkt->size;
    mCount++;
    LOGD("mCount:%d", mCount);
	if (pkt->duration == 0 && mLastPTS != AV_NOPTS_VALUE && pkt->pts != AV_NOPTS_VALUE)
		mDuration += (pkt->pts - mLastPTS);
    else
		mDuration += pkt->duration;
	mLastPTS = pkt->pts;
	LOGD("put stream #%d: mDuration %lld", pkt->stream_index, mDuration);
	
    return OK;
}

AVPacket* PacketQueue::get()
{
	AutoLock autoLock(&mLock);

    AVPacket* pPacket = (AVPacket*)mPacketList.Remove(0);
    if(pPacket != NULL)
    {
        mCachedSize -= pPacket->size;
        mCount--;
        LOGD("mCount:%d", mCount);
        mDuration-=pPacket->duration;
        LOGD("mDuration:%lld", mDuration);
    }
    else
    {
        mCachedSize = 0;
        mCount = 0;
        mDuration = 0;
        LOGD("mCount:%d", mCount);
    }
    return pPacket;
}

AVPacket* PacketQueue::peek()
{
	AutoLock autoLock(&mLock);

	return (AVPacket*)mPacketList.GetAt(0);
}

void PacketQueue::flush()
{
	AutoLock autoLock(&mLock);

    while(!mPacketList.IsEmpty()) {
        AVPacket* pPacket = (AVPacket*)mPacketList.Remove(0);
        av_free_packet(pPacket);
        av_free(pPacket);
        pPacket = NULL;
    }

    mCachedSize = 0;
    mCount = 0;
    mDuration = 0;
}

uint32_t PacketQueue::size()
{
	AutoLock autoLock(&mLock);
    return mCachedSize;
}

uint32_t PacketQueue::count()
{
	AutoLock autoLock(&mLock);
    return mCount;
}

int64_t PacketQueue::duration()
{
	AutoLock autoLock(&mLock);
    return mDuration;
}