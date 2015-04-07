package android.pplive.media.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.pplive.media.util.LogUtils;

class DefaultMediaExtractor implements MediaExtractable {
	
	private MediaExtractor mExtractor;
	
	DefaultMediaExtractor() {
		mExtractor = new MediaExtractor();
	}

	@Override
	public boolean advance() {
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
		return mExtractor.getSampleTrackIndex();
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
		return mExtractor.readSampleData(byteBuf, offset);
	}

	@Override
	public void release() {
		mExtractor.release();
	}

	@Override
	public void seekTo(long timeUs, int mode) {
		mExtractor.seekTo(timeUs, mode);
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
            LogUtils.error("Java: setDataSource() Exception" + path + ", e: " + e.getMessage());
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

}
