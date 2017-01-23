package com.gotye.meetsdk.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.util.LogUtils;

class SystemMediaExtractor implements MediaExtractable {
	
	private final static long SEEK_GAP_USEC = 3000000L; // 3 sec
	
	private MediaExtractor mExtractor;
	private int mSampleTrackIndex = -1;
	private boolean mSeeking = false;
	private boolean mGetVideoFlushPkt = false;
	private boolean mGetAudioFlushPkt = false;
	
	SystemMediaExtractor() {
		this.mExtractor = new MediaExtractor();
	}

	@Override
	public boolean advance() {
		if (mSeeking) {
			if (mGetVideoFlushPkt && mGetAudioFlushPkt) {
				LogUtils.error("set mSeeking false");
				mSeeking = false;
			}
			
			return true;
		}
		
		return mExtractor.advance();
	}

	@Override
	public long getCachedDuration() {
		return mExtractor.getCachedDuration();
	}

	@Override
	public int getSampleFlags() {
		return mExtractor.getSampleFlags();
	}

	@Override
	public long getSampleTime() {
		return mExtractor.getSampleTime();
	}

	@Override
	public int getSampleTrackIndex() {
		if (mSeeking) {
			if (!mGetVideoFlushPkt)
				mSampleTrackIndex = 0; // video
			else
				mSampleTrackIndex = 1; // audio
		}
		else {
			mSampleTrackIndex = mExtractor.getSampleTrackIndex();
		}
		
		return mSampleTrackIndex;
	}

	@Override
	public int getTrackCount() {
		return mExtractor.getTrackCount();
	}

	@Override
	public MediaFormat getTrackFormat(int index) {
		return mExtractor.getTrackFormat(index);
	}

	@Override
	public boolean hasCachedReachedEndOfStream() {
		return mExtractor.hasCacheReachedEndOfStream();
	}

	@Override
	public int readSampleData(ByteBuffer byteBuf, int offset) {
		if (mSeeking) {
			String str_flush = "FLUSH";
			byte [] flush_pkt = str_flush.getBytes();
			byteBuf.position(offset);
			byteBuf.put(flush_pkt);
			byteBuf.flip();
			
			if (mSampleTrackIndex == 0 && mGetVideoFlushPkt == false) {
				mGetVideoFlushPkt = true;
			}
					
			if (mSampleTrackIndex == 1 && mGetAudioFlushPkt == false) {
				mGetAudioFlushPkt = true;
			}
			
			// 关于flp: 将limit属性设置为当前的位置
			// 关于rewind: 是在limit属性已经被设置合适的情况下使用的。
			// 也就是说这两个方法虽然都能够使指针返回到缓冲区的第一个位置，但是flip在调整指针之前
			return 5;
		}
		
		return mExtractor.readSampleData(byteBuf, offset);
	}

	@Override
	public void release() {
		mExtractor.release();
	}

	@Override
	public void seekTo(long timeUs, int mode) {
		mExtractor.seekTo(timeUs, mode);
		/*while (true) {
			long SampleTimeUs = mExtractor.getSampleTime();
			long diff = Math.abs(SampleTimeUs - timeUs);
			if (diff <= SEEK_GAP_USEC)
				break;
			
			LogUtils.error(String.format("Java: drop mis-match sample: %d, diff %d", 
					SampleTimeUs, diff));
			if (!mExtractor.advance())
				break;
		}*/
		
		mSeeking = true;
		mGetVideoFlushPkt = false;
		mGetAudioFlushPkt = false;
	}

	@Override
	public void selectTrack(int index) {
		mExtractor.selectTrack(index);
	}

	@Override
	public void setDataSource(String path) throws IOException {
		try{
			LogUtils.info("Java: setDataSource() " + path);
			mExtractor.setDataSource(path);
		}
		catch (Exception e){
            e.printStackTrace();
            LogUtils.error("Java: setDataSource() Exception" + path + ", error: " + e.getMessage());
		}
	}

	@Override
	public void unselectTrack(int index) {
		mExtractor.unselectTrack(index);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSystemExtractor() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void setVideoAhead(int msec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSubtitleParser(SimpleSubTitleParser parser) {

	}

	@Override
	public int readPacket(int stream_index, ByteBuffer byteBuf, int offset) {
		return 0;
	}

	@Override
	public int decodeAudio(ByteBuffer inBuf, int inSize, ByteBuffer outBuf) { return 0;}

	@Override
	public int getBitrate() {
		return 0;
	}
}
