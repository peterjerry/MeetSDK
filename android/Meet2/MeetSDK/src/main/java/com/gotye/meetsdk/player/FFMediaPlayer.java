package com.gotye.meetsdk.player;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.os.Message;
import android.os.Parcel;
import com.gotye.meetsdk.util.LogUtils;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;

public class FFMediaPlayer extends BaseMediaPlayer {

	private long mNativeContext; // ISubtitle ctx, accessed by native methods
	private long mListenerContext; // accessed by native methods
	private long mSurface;// accessed by native methods
	private static String libPath = "";
    private boolean mUseRenderer = false;
	
	public static boolean initPlayer(String path) {
		LogUtils.info("initPlayer()");
		
		if (path != null)
			libPath = path;
		
		boolean ret = native_init();
		if (!ret) {
			LogUtils.error("failed to init native player");
			return false;
		}
		
		return ret;
	}
	
	public FFMediaPlayer(MediaPlayer mp) {
		super(mp);
        mUseRenderer = false;
		native_setup(new WeakReference<FFMediaPlayer>(this));
	}

	// capability
	// TV BOX does not need to call this check.
	public static boolean checkCompatibility(int checkWhat, Surface surface) {
		boolean ret = false;
		try {
			ret = native_checkCompatibility(checkWhat, surface);
			
		} catch(Exception ex) {
		    LogUtils.error("Exception", ex);
		} 
		
		return ret;
	}

	public static int checkSoftwareDecodeLevel() {
		return native_checkSoftwareDecodeLevel();
	}

	public static int getCpuArchNumber() {
		return native_getCpuArchNumber();
	}
	
	// media info and snapshot
	public static MediaInfo getMediaInfo(String mediaFilePath) {
		MediaInfo info = new MediaInfo(mediaFilePath);
		return native_getMediaInfo(mediaFilePath, info) ? info : null;
	}
	
	public static MediaInfo getMediaDetailInfo(String mediaFilePath) {
		MediaInfo info = new MediaInfo(mediaFilePath);
		return native_getMediaDetailInfo(mediaFilePath, info) ? info : null;
	}
	
	static Bitmap createVideoThumbnail(String mediaFilePath, int kind) {
		Bitmap bitmap = null;
		
		if (mediaFilePath.toLowerCase().endsWith("flac") || mediaFilePath.toLowerCase().endsWith("ape") ||
				mediaFilePath.toLowerCase().endsWith("wav"))
			return null;
		
		MediaInfo info = getThumbnail(mediaFilePath);
		if (null != info) {
			int[] colors = info.getThumbnail();
			int width = info.getThumbnailWidth();
			int height = info.getThumbnailHeight();
			LogUtils.debug("createVideoThumbnail: colors.length: " + colors.length);
			LogUtils.debug("createVideoThumbnail: width: " + width + "; height: " + height);
			bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
		}
		
		return bitmap;
	}
	
	private static MediaInfo getThumbnail(String mediaFilePath) {
		MediaInfo info = new MediaInfo(mediaFilePath);
		return native_getThumbnail(mediaFilePath, info) ? info : null;
	}
	
	public Bitmap getSnapShot(int width, int height, int fmt, int msec) {
		SnapShot pic = new SnapShot();
		boolean ret;
		
		ret = native_getSnapShot(width, height, fmt, msec, pic);
		if (!ret)
			return null;
			
		return Bitmap.createBitmap(pic.mPicture, pic.mSnapShotWidth, pic.mSnapShotHeight, Bitmap.Config.ARGB_8888); //RGB_565 ARGB_8888
	}
	
	/**
	 * Called from native code when an interesting event happens. This method
	 * just uses the EventHandler system to post the event back to the main app
	 * thread. We use a weak reference to the original MediaPlayer object so
	 * that the native code is safe from the object disappearing from underneath
	 * it. (This is the cookie passed to native_setup().)
	 */
	private static void postEventFromNative(Object mediaplayer_ref, int what,
			int extra1, int extra2, Object obj) {
		FFMediaPlayer mp = (FFMediaPlayer) ((WeakReference<?>) mediaplayer_ref).get();
		if (mp == null) {
			return;
		}

		if (mp.mEventHandler != null) {			
			Message msg = mp.mEventHandler.obtainMessage(what, extra1, extra2, obj);
			msg.sendToTarget();
		}
	}
	
	// play procedure
	@Override
	public void setDataSource(Context context, Uri uri)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException {
		// TODO Auto-generated method stub
		setDataSource(context, uri, null);
	}
	
	/**
	 * Sets the data source as a content Uri.
	 * 
	 * @param context
	 *            the Context to use when resolving the Uri
	 * @param uri
	 *            the Content URI of the data you want to play
	 * @param headers
	 *            the headers to be sent together with the request for the data
	 * @throws IllegalStateException
	 *             if it is called in an invalid state
	 * @hide pending API council
	 */
	public void setDataSource(Context context, Uri uri,
			Map<String, String> headers) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {

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
	public void setDataSource(String path) throws IllegalStateException,
			IOException, IllegalArgumentException, SecurityException {
		// TODO Auto-generated method stub
		_setDataSource(path);
	}
	
	@Override
	public void setDataSource(FileDescriptor fd) throws IOException,
			IllegalArgumentException, IllegalStateException {
		// intentionally less than LONG_MAX
		_setDataSource(fd, 0, 0x7ffffffffffffffL);
	}
	
	@Override
	public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException,
			IllegalArgumentException, IllegalStateException {
		// intentionally less than LONG_MAX
		_setDataSource(fd, offset, length);
	}

	@Override
	public void setDisplay(SurfaceHolder sh) {
		super.setDisplay(sh);

		if (sh != null)
        	setSurface(sh.getSurface());
	}
	
	@Override
	public void setSurface(Surface surface) {
		// TODO Auto-generated method stub
        if (!mUseRenderer) {
            _setVideoSurface(surface);
        }

		updateSurfaceScreenOn();
	}
	
	@Override
	public void prepare() throws IOException, IllegalStateException {
		// TODO Auto-generated method stub
		_prepare();
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		_prepareAsync();
	}

	@Override
	public void start() throws IllegalStateException {
		// TODO Auto-generated method stub
		stayAwake(true);
		_start();
	}

	@Override
	public void stop() throws IllegalStateException {
		// TODO Auto-generated method stub
		stayAwake(false);
		_stop();
	}

	@Override
	public void pause() throws IllegalStateException {
		// TODO Auto-generated method stub
		stayAwake(false);
		_pause();
	}

	@Override
	public void seekTo(int msec) throws IllegalStateException {
		// TODO Auto-generated method stub
		_seekTo(msec);
	}
	
	@Override
	public void setWakeMode(Context context, int mode) {
		super.setWakeMode(context, mode);
	}
	
	@Override
	public void release() {
		super.release();
		
		_release();
		// make sure none of the listeners get called anymore
		// 2015.1.20 guoliangma solve "quick" new-open when another is in "preparing" state
        mEventHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void reset() {
		stayAwake(false);
        mUseRenderer = false;
		_reset();
		// make sure none of the listeners get called anymore
        mEventHandler.removeCallbacksAndMessages(null);
	}

	public void setRenderer(long renderer) {
        native_setRenderer(renderer);
        mUseRenderer = true;
    }
	
	public static native String native_getVersion();

	//Returns the width of the video.
	@Override
	public native int getVideoWidth();

	//Returns the height of the video.
	@Override
	public native int getVideoHeight();

	//Checks whether the MediaPlayer is playing.
	//return true if currently playing, false otherwise
	@Override
	public native boolean isPlaying() throws IllegalStateException;

	//Seeks to specified time position in milliseconds
	public native void _seekTo(int msec) throws IllegalStateException;

	//return the current position in milliseconds
	@Override
	public native int getCurrentPosition();

	//return the duration in milliseconds
	@Override
	public native int getDuration();
	
	//return buffering time in milliseconds
	@Override
	public native int getBufferingTime();

	@Override
	public native int flags() throws IllegalStateException;

	@Override
	public void setAudioStreamType(int streamType) {
		_setAudioStreamType(streamType);
	}

	@Override
	public TrackInfo[] getTrackInfo() throws IllegalStateException {
		// TODO Auto-generated method stub
		return native_getTrackInfo();
	}
	
	@Override
    public void setSubtitleParser(SimpleSubTitleParser parser) {
        // TODO Auto-generated method stub
        native_setSubtitleParser(parser);
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

    /**
     * @param index: index of all streams(NOT audio index)
     */
	@Override
	public void selectTrack(int index) throws IllegalStateException {
		// TODO Auto-generated method stub
		LogUtils.info("selectTrack #" + index);
		
		MediaInfo info = this.getMediaInfo();
		if (info == null)
			_selectAudioChannel(index);
		else {
			ArrayList<com.gotye.meetsdk.player.TrackInfo> subtitleTrackInfo = info.getSubtitleChannelsInfo();
			
			if (subtitleTrackInfo != null && subtitleTrackInfo.size() > 0 && 
					index >= subtitleTrackInfo.get(0).getStreamIndex())
				_selectSubtitleChannel(index);
			else
				_selectAudioChannel(index);
		}
	}

	@Override
	public void deselectTrack(int index) throws IllegalStateException {
		// TODO Auto-generated method stub
		LogUtils.info("deselectTrack #" + index);
	}
	
	@Override
	public DecodeMode getDecodeMode() {
		return DecodeMode.SW;
	}
	
	@Override
	public native void setOption(String option);
	
	@Override
	public MediaInfo getMediaInfo() throws IllegalStateException {
		MediaInfo info = new MediaInfo();
		if (native_getCurrentMediaInfo(info))
			return info;
		
		return null;
	}
	
	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setNextMediaPlayer(MediaPlayer next) {
		
	}
	
	private native void _setDataSource(String path) throws IOException,
			IllegalArgumentException, IllegalStateException;
			
	private native void _setDataSource(String path, Map<String, String> headers)
			throws IOException, IllegalArgumentException, IllegalStateException;
			
	private native void _setDataSource(FileDescriptor fd, long offset, long length)
			throws IOException, IllegalArgumentException, IllegalStateException;
	
	private native void _setVideoSurface(Surface surface);
	
	private native void _setAudioStreamType(int streamType);
	
	private native void _selectAudioChannel(int index);
	
	private native void _selectSubtitleChannel(int index);
	
	private native void _prepare() throws IOException, IllegalStateException;
	
	private native void _prepareAsync() throws IllegalStateException;
	
	private native void _reset();
	
	private native void _release();
	
	private native void _start() throws IllegalStateException;

	private native void _stop() throws IllegalStateException;

	private native void _pause() throws IllegalStateException;
	
	// state
	private native boolean _isPlaying() throws IllegalStateException;

	public native boolean isLooping();
	
	// set
	public native void setVolume(float leftVolume, float rightVolume);

	public native Bitmap getFrameAt(int msec) throws IllegalStateException;
	
	public native void setLooping(boolean looping);
	
	// capability
	private static native boolean native_checkCompatibility(int checkWhat, Surface surface) throws IllegalStateException;
    
	private static native String native_getBestCodec(String appPath);

	private static native int native_checkSoftwareDecodeLevel();

	private static native int native_getCpuArchNumber();
	
	public static native String getBestCodec(String appPath);
	
	public static native boolean native_supportSoftDecode();

	// media info and snapshot
	private static native boolean native_getMediaInfo(String mediaFilePath, MediaInfo info);
	
	private static native boolean native_getMediaDetailInfo(String mediaFilePath, MediaInfo info);
	
	private static native boolean native_getThumbnail(String mediaFilePath, MediaInfo info);
	
	private native boolean native_getSnapShot(int width, int height, int fmt, int msec, SnapShot pic);
	
	private native TrackInfo[] native_getTrackInfo();
	
	private native boolean native_getCurrentMediaInfo(MediaInfo info);
	
	// init and uninit
	private static native final boolean native_init();
	
	private native final void native_setup(Object mediaplayer_this);
	
	// subtitle
	private native void native_setSubtitleParser(SimpleSubTitleParser parser);
	
	// not used
	private native static int snoop(short[] outData, int kind);
	
	private native final int native_invoke(Parcel request, Parcel reply);
	
	private native final int native_setMetadataFilter(Parcel request);
	
	private native final boolean native_getMetadata(boolean update_only,
			boolean apply_filter, Parcel reply);
	
	private native int native_suspend_resume(boolean isSuspend);

	public native void native_setRenderer(long nativeSurface);

}
