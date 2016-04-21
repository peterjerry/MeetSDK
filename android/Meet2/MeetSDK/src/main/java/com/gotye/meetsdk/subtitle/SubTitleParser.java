package com.gotye.meetsdk.subtitle;

public interface SubTitleParser {

	void close();

	SubTitleSegment next();

	void prepareAsync();

	void seekTo(long msec);

	void setDataSource(String filePath);

	void setListener(OnReadyListener listener);

	interface OnReadyListener {
		
		void onPrepared(boolean success, String msg);
		
		void onSeekComplete();
	}
}
