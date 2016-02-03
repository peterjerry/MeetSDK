package com.gotye.meetsdk.player;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Message;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.util.LogUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

public class OMXMediaPlayer extends BaseMediaPlayer {

	private long mNativeContext; // ISubtitle ctx, accessed by native methods
	private long mListenerContext; // accessed by native methods
	private static String libPath = "";
	
	public static boolean initPlayer(String path) {
		LogUtils.info("initPlayer()");
		
		if (path != null)
			libPath = path;
		
		boolean ret = native_init();
		if (!ret) {
			LogUtils.error("failed to init native omx player");
			return false;
		}
		
		return ret;
	}
	
	public OMXMediaPlayer(MediaPlayer mp) {
		super(mp);
		
		native_setup(new WeakReference<OMXMediaPlayer>(this));
	}

	@Override
	public void setDisplay(SurfaceHolder sh) {
		super.setDisplay(sh);
		
		setSurface(sh.getSurface());
	}
	
	@Override
	public void setSurface(Surface surface) {
		// TODO Auto-generated method stub
		_setVideoSurface(surface);
		updateSurfaceScreenOn();
	}

	@Override
	public MediaInfo getMediaInfo() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setDataSource(Context context, Uri uri,
			Map<String, String> headers) throws IllegalStateException,
			IOException, IllegalArgumentException, SecurityException {
		// TODO Auto-generated method stub
		String scheme = uri.getScheme();
		if (scheme == null || scheme.equals("file")) {
			//local file
			setDataSource(uri.getPath());
		}
		else {
			//network path
			setDataSource(uri.toString());
		}
	}

	@Override
	public void setDataSource(Context context, Uri uri)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException {
		// TODO Auto-generated method stub
		setDataSource(context, uri, null);
	}

	@Override
	public void setDataSource(String path) throws IllegalStateException,
			IOException, IllegalArgumentException, SecurityException {
		// TODO Auto-generated method stub
		_setDataSource(path);
	}

	@Override
	public void setDataSource(FileDescriptor fd) throws IOException,
			IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		_setDataSource(fd, 0, 0x7ffffffffffffffL);
	}

	@Override
	public void setDataSource(FileDescriptor fd, long offset, long length)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		_setDataSource(fd, offset, length);
	}

	@Override
	public int flags() throws IllegalStateException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bitmap getSnapShot(int width, int height, int fmt, int msec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepare() throws IOException, IllegalStateException {
		// TODO Auto-generated method stub

		Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
		msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE; // extra
		msg.arg2 = MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER;
		msg.sendToTarget();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLooping(boolean looping) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAudioStreamType(int streamType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSubtitleParser(SimpleSubTitleParser parser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TrackInfo[] getTrackInfo() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTimedTextSource(String path, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTimedTextSource(Context context, Uri uri, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectTrack(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deselectTrack(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DecodeMode getDecodeMode() {
		// TODO Auto-generated method stub
		return DecodeMode.HW_OMX;
	}

	/**
	 * Called from native code when an interesting event happens. This method
	 * just uses the EventHandler system to post the event back to the main app
	 * thread. We use a weak reference to the original MediaPlayer object so
	 * that the native code is safe from the object disappearing from underneath
	 * it. (This is the cookie passed to native_setup().)
	 */
	private static void postEventFromNative(Object mediaplayer_ref, int what,
			int arg1, int arg2, Object obj) {
		OMXMediaPlayer mp = (OMXMediaPlayer) ((WeakReference<?>) mediaplayer_ref).get();
		if (mp == null) {
			return;
		}

		if (mp.mEventHandler != null) {			
			Message msg = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
			msg.sendToTarget();
		}
	}
	
	private static native final boolean native_init();
	
	private native final void native_setup(Object mediaplayer_this);
	
	@Override
	public native void start() throws IllegalStateException;
	
	@Override
	public native void stop() throws IllegalStateException;
	
	@Override
	public native void pause() throws IllegalStateException;
	
	//Seeks to specified time position in milliseconds
	@Override
	public native void seekTo(int msec) throws IllegalStateException;
	
	//Returns the width of the video.
	@Override
	public native int getVideoWidth();

	//Returns the height of the video.
	@Override
	public native int getVideoHeight();
	
	//return the current position in milliseconds
	@Override
	public native int getCurrentPosition();

	//return the duration in milliseconds
	@Override
	public native int getDuration();
	
	//return buffering time in milliseconds
	@Override
	public native int getBufferingTime();
	
	//Checks whether the MediaPlayer is playing.
	//return true if currently playing, false otherwise
	@Override
	public native boolean isPlaying() throws IllegalStateException;
	
	@Override
	public native boolean isLooping() throws IllegalStateException;
	
	@Override
	public native void setOption(String option);
	
	private native void _setDataSource(String path) throws IOException,
	IllegalArgumentException, IllegalStateException;
	
	private native void _setDataSource(String path, Map<String, String> headers)
		throws IOException, IllegalArgumentException, IllegalStateException;
		
	private native void _setDataSource(FileDescriptor fd, long offset, long length)
		throws IOException, IllegalArgumentException, IllegalStateException;
	
	private native void _setVideoSurface(Surface surface);
	
	@Override
	public native void prepareAsync() throws IllegalStateException;
}
