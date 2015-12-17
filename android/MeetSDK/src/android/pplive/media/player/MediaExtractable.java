package android.pplive.media.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaFormat;

public interface MediaExtractable {
	
	public abstract boolean advance();
	
	public abstract long getCachedDuration();
	
//	public abstract boolean getSampleCryptoInfo(MediaCodec.CryptoInfo info);
	
	public abstract int getSampleFlags();
	
	public abstract long getSampleTime();
	
	public abstract int getSampleTrackIndex();
	
	public abstract int getTrackCount();
	
	public abstract MediaFormat getTrackFormat(int index);
	
	public abstract boolean hasCachedReachedEndOfStream();
	
	public abstract int readSampleData(ByteBuffer byteBuf, int offset);
	
	public abstract void release();
	
	public abstract void stop();
	
	public abstract void seekTo(long timeUs, int mode);
	
	public abstract void selectTrack(int index);
	
	public abstract void setDataSource(String path) throws IOException;
	
//	public abstract void setDataSource(String path, Map<String, String> headers);
	
//	public abstract void setDataSource(FileDescriptor fd);
	
//	public abstract void setDataSource(FileDescriptor fd, long offset, long length);
	
//	public abstract void setDataSource(Context context, Uri uri, Map<String, String> headers);
	
	public abstract void unselectTrack(int index);
	
	public abstract boolean isSystemExtractor();

}
