package com.gotye.meetsdk.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.*;

import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;

public interface MediaExtractable {
	
	boolean advance();

	long getCachedDuration();

	//	boolean getSampleCryptoInfo(MediaCodec.CryptoInfo info);

	int getSampleFlags();

	long getSampleTime();

	int getSampleTrackIndex();

	int getTrackCount();

	MediaFormat getTrackFormat(int index);

	boolean hasCachedReachedEndOfStream();

	int readSampleData(ByteBuffer byteBuf, int offset);

	void release();

	void stop();

    void seekTo(long timeUs, int mode);

	void selectTrack(int index);

	void setDataSource(String path) throws IOException;

	//	void setDataSource(String path, Map<String, String> headers);

	//	void setDataSource(FileDescriptor fd);

	//	setDataSource(FileDescriptor fd, long offset, long length);

	// void setDataSource(Context context, Uri uri, Map<String, String> headers);

	void unselectTrack(int index);

	boolean isSystemExtractor();

	void setVideoAhead(int msec);

	void setSubtitleParser(SimpleSubTitleParser parser);

    int readPacket(int stream_index, ByteBuffer byteBuf, int offset);

	int decodeAudio(ByteBuffer inBuf, int inSize, ByteBuffer outBuf);
}
