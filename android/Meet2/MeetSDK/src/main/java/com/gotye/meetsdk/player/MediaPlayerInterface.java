package com.gotye.meetsdk.player;

import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Map;

import android.content.Context;
import android.media.MediaPlayer.TrackInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.MediaPlayer.OnBufferingUpdateListener;
import com.gotye.meetsdk.player.MediaPlayer.OnCompletionListener;
import com.gotye.meetsdk.player.MediaPlayer.OnErrorListener;
import com.gotye.meetsdk.player.MediaPlayer.OnInfoListener;
import com.gotye.meetsdk.player.MediaPlayer.OnPreparedListener;
import com.gotye.meetsdk.player.MediaPlayer.OnSeekCompleteListener;
//import com.gotye.meetsdk.player.MediaPlayer.OnTimedTextListener;
import com.gotye.meetsdk.player.MediaPlayer.OnVideoSizeChangedListener;

public interface MediaPlayerInterface {

	public abstract void setDataSource(Context context, Uri uri, Map<String, String> headers)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException;
			
	public abstract void setDataSource(Context context, Uri uri)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException;

	public abstract void setDataSource(String path)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException;

	public abstract void setDataSource(FileDescriptor fd)
			throws IOException,
			IllegalArgumentException, IllegalStateException;
			
	public abstract void setDataSource(FileDescriptor fd, long offset, long length) 
			throws IOException,
			IllegalArgumentException, IllegalStateException;
			
	public abstract void setDisplay(SurfaceHolder sh);
	
	public abstract void setSurface (Surface surface);
	
	public abstract int flags() throws IllegalStateException;
	
	public abstract Bitmap getSnapShot(int width, int height, int fmt, int msec);

	public abstract void prepare() throws IOException, IllegalStateException;

	public abstract void prepareAsync() throws IllegalStateException;

	public abstract void start() throws IllegalStateException;

	public abstract void stop() throws IllegalStateException;

	public abstract void pause() throws IllegalStateException;

	public abstract void seekTo(int msec) throws IllegalStateException;

	public abstract void release();

	public abstract void reset();

	public abstract int getCurrentPosition();

	public abstract int getDuration();

	public abstract int getVideoWidth();

	public abstract int getVideoHeight();
	
	public abstract int getBufferingTime();

	public abstract boolean isPlaying();
	
	public abstract boolean isLooping();

	public abstract void setLooping (boolean looping);

	public abstract void setAudioStreamType(int streamType);
	
	public abstract void setSubtitleParser(SimpleSubTitleParser parser);

	public abstract TrackInfo[] getTrackInfo() throws IllegalStateException;

	public abstract void addTimedTextSource(String path, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException;

	public abstract void addTimedTextSource(Context context, Uri uri,
			String mimeType) throws IOException, IllegalArgumentException,
			IllegalStateException;

	public abstract void selectTrack(int index);

	public abstract void deselectTrack(int index);

	public abstract void setOnBufferingUpdateListener(OnBufferingUpdateListener listener);

	public abstract void setOnCompletionListener(OnCompletionListener listener);

	public abstract void setOnErrorListener(OnErrorListener listener);

	public abstract void setOnInfoListener(OnInfoListener listener);

	public abstract void setOnPreparedListener(OnPreparedListener listener);

	public abstract void setOnSeekCompleteListener(OnSeekCompleteListener listener);

	public abstract void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener);

	//public abstract void setOnTimedTextListener(OnTimedTextListener listener);
	
	public abstract void setScreenOnWhilePlaying(boolean screenOn);
	
	public abstract void setWakeMode(Context context, int mode);
	
	public abstract DecodeMode getDecodeMode();

	public abstract void setOption(String option);
	
	public abstract MediaInfo getMediaInfo() throws IllegalStateException;
	
	public abstract int getAudioSessionId();

	public abstract void setNextMediaPlayer(MediaPlayer next);
}
