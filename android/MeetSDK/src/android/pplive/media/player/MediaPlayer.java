package android.pplive.media.player;

import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer.TrackInfo;
import android.media.TimedText;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.pplive.media.MeetSDK;
import android.pplive.media.util.LogUtils;
import android.pplive.media.subtitle.SimpleSubTitleParser;

public class MediaPlayer implements MediaPlayerInterface {
	private final static String TAG = "pplive/MediaPlayer";
	
	/**
       Constant to retrieve only the new metadata since the last
       call.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_UPDATE_ONLY = true;

    /**
       Constant to retrieve all the metadata.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_ALL = false;

    /**
       Constant to enable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean APPLY_METADATA_FILTER = true;

    /**
       Constant to disable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean BYPASS_METADATA_FILTER = false;
	
	// seek capability
	public static final int CAN_SEEK_BACKWARD 	= 1;
	public static final int CAN_SEEK_FORWARD 		= 2;
	public static final int CAN_PAUSE 			= 4;
	public static final int CAN_SEEK 				= 8;
	
	public static final int PLAYER_IMPL_TYPE_SYSTEM_PLAYER	= 10001;
	public static final int PLAYER_IMPL_TYPE_XO_PLAYER		= 10002;
	public static final int PLAYER_IMPL_TYPE_FF_PLAYER		= 10003;
	public static final int PLAYER_IMPL_TYPE_PP_PLAYER		= 10004;
	
	/**
	 * @author guoliangma
	 *
	 */
	@SuppressWarnings("deprecation") // avoid setType warning
    public enum DecodeMode {
        HW_SYSTEM {
            @Override
            public void setSurfaceType(SurfaceHolder holder) {
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        },
        HW_XOPLAYER {
        	@Override
            public MediaPlayerInterface newInstance(MediaPlayer mp) {
            	LogUtils.info("player_select xoplayer");
                return new XOMediaPlayer(mp);
            }
        	
            @Override
            public void setSurfaceType(SurfaceHolder holder) {
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        },
        SW {
            @Override
            public MediaPlayerInterface newInstance(MediaPlayer mp) {
            	LogUtils.info("player_select ffplayer");
                return new FFMediaPlayer(mp);
            }
            
            @Override
            public void setSurfaceType(SurfaceHolder holder) {
                holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                holder.setFormat(PixelFormat.RGBA_8888);
            }
        },
        AUTO, UNKNOWN;

        public MediaPlayerInterface newInstance(MediaPlayer mp) {
        	LogUtils.info("player_select system player");
            return new SystemMediaPlayer(mp);
        }
        
        public void setSurfaceType(SurfaceHolder holder) {
            
        }
    }
	
	private Context mContext = null;
	private MediaPlayerInterface mPlayer = null;
	private DecodeMode mDecodeMode = DecodeMode.SW;
	private String mPath = null;
	private Surface mSurface = null;
	private SurfaceHolder mHolder = null;
	private boolean mScreenOn = false;
	private int mWakeMode;
	private String mOption;

	public MediaPlayer() {
		this(DecodeMode.SW);
	}
	
	/**
	 * @param mode valid value: DecodeMode.SW, DecodeMode.HW_SYSTEM
	 * @param mode invalid value: DecodeMode.AUTO(should use MeetSDK.isOMXSurface to decide mode first)
	 */
	public MediaPlayer(DecodeMode mode) {
		mDecodeMode = mode;
	}
	
	public DecodeMode getDecodeMode() {
		return mDecodeMode;
	}
	
	@Override
	public int flags() throws IllegalStateException {
		if (mPlayer != null) {
			return mPlayer.flags();
		}
		else {
		    LogUtils.error("MediaPlayer hasn't initialized!!!");
			throw new IllegalStateException("MediaPlayer hasn't initialized!!!");
		}
	}
	
	@Override
	public void setDataSource(Context context, Uri uri, Map<String, String> headers)
		throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException {
		mPath = MeetSDK.Uri2String(context, uri);
	}
	
	@Override
	public void setDataSource(Context context, Uri uri)
			throws IllegalStateException, IOException,
			IllegalArgumentException, SecurityException {
		setDataSource(context, uri, null);
	}

	@Override
	public void setDataSource(String path) throws IllegalStateException,
			IOException, IllegalArgumentException, SecurityException {
		mPath = path;
	}
	
	@Override
	public void setDataSource(FileDescriptor fd)
			throws IOException,
			IllegalArgumentException, IllegalStateException {
		setDataSource(fd, 0, -1);
	}
			
	@Override		
	public void setDataSource(FileDescriptor fd, long offset, long length) 
			throws IOException,
			IllegalArgumentException, IllegalStateException {
		LogUtils.error("MediaPlayer setDataSource(fd) is not supported");
		throw new IllegalStateException("MediaPlayer setDataSource(fd) is not supported");
	}
	
	@Override
	public void setDisplay(SurfaceHolder sh) {
		if (null != sh) {
			mHolder		= sh;
			mSurface	= sh.getSurface();
		}
		
		setSurface();
	}
	
	private void setSurface() {
		if (null != mPlayer && null != mHolder)
			mPlayer.setDisplay(mHolder);
	}
	
	private void setupMediaPlayer() throws IllegalStateException {
		if (mPlayer != null) {
		    LogUtils.error("MediaPlayer is already setup.");
		    throw new IllegalStateException("MediaPlayer is already setup.");
		}
		
		if (mPath == null) {
		    LogUtils.error("Media Path is not set.");
		    throw new IllegalStateException("Media Path is not set.");
		}
		
		if (DecodeMode.AUTO == mDecodeMode) {
			LogUtils.error("DecodeMode = AUTO is invalid");
		    throw new IllegalStateException("invalid DecodeMode");
		}
		
		mPlayer = mDecodeMode.newInstance(this);
		
		// 2015.3.23 guoliangma move here to fix s39h cannot play pptv url problem
		// setLooping called after setDataSource will throw (-38, 0) when using SystemPlayer
		setLooping();
		
		if (null == mPlayer)
			throw new IllegalStateException("failed to create new instance of MediaPlayer");
		
		try {
			mPlayer.setDataSource(mPath);
		} catch (IllegalArgumentException e) {
		    LogUtils.error("IllegalArgumentException", e);
			throw new IllegalStateException(e);
		} catch (IOException e) {
		    LogUtils.error("IOException", e);
			throw new IllegalStateException(e);
		}
		
		setSurface();
		setAudioStreamType();
		
		setOnBufferingUpdateListener();
		setOnCompletionListener();
		setOnErrorListener();
		setOnInfoListener();
		setOnPreparedListener();
		setOnSeekCompleteListener();
		setOnVideoSizeChangedListener();
		
		setScreenOnWhilePlaying();
		setWakeMode();
		
		setOption();
	}
	
	@Override
	public void prepare() throws IOException, IllegalStateException {
		LogUtils.info("prepare");

		setupMediaPlayer();
		
		if (null != mPlayer) {
		    setSubtitleParser();
			mPlayer.prepare();
		}
		else {
			LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		setupMediaPlayer();
		
		if (mPlayer != null) {
			setSubtitleParser();
			mPlayer.prepareAsync();
		}
		else {
		    LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void start() throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.start();
		}
		else {
		    LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void stop() throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.stop();
		}
		else {
		    LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void pause() throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.pause();
		}
		else {
		    LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void seekTo(int msec) throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.seekTo(msec);
		}
		else {
		    LogUtils.error("MediaPlayer has't initialized!!!");
			throw new IllegalStateException("MediaPlayer has't initialized!!!");
		}
	}

	@Override
	public void release() {
		if (mPlayer != null) {
	        mOnPreparedListener = null;
	        mOnBufferingUpdateListener = null;
	        mOnCompletionListener = null;
	        mOnSeekCompleteListener = null;
	        mOnErrorListener = null;
	        mOnInfoListener = null;
	        mOnVideoSizeChangedListener = null;
	        
			mPlayer.release();
		}
	}

	@Override
	public void reset() {
		if (mPlayer != null) {
			mPlayer.reset();
		}
	}

	@Override
	public int getCurrentPosition() {
		if (mPlayer != null) {
			return mPlayer.getCurrentPosition();
		}
		
		return 0;
	}

	@Override
	public int getDuration() {
		if (mPlayer != null) {
			return mPlayer.getDuration();
		}
		
		return 0;
	}

	@Override
	public int getVideoWidth() {
		if (mPlayer != null) {
			return mPlayer.getVideoWidth();
		}
		
		return 0;
	}

	@Override
	public int getVideoHeight() {
		if (mPlayer != null) {
			return mPlayer.getVideoHeight();
		}
		
		return 0;
	}
	
	@Override
	public int getBufferingTime() {
	    if (mPlayer != null) {
	        return mPlayer.getBufferingTime();
	    }
	    
	    return 0;
	}

	@Override
	public boolean isPlaying() {
		if (mPlayer != null) {
			return mPlayer.isPlaying();
		}
		return false;
	}
	
	@Override
	public boolean isLooping() {
		if (mPlayer != null) {
			return mPlayer.isLooping();
		}
		return false;
	}
	
	private boolean mLooping = false;
	@Override
	public void setLooping(boolean looping) {
		LogUtils.info("MediaPlayer setLooping: " + looping);
		mLooping = looping;
	}
	
	private void setLooping() {
		if (mPlayer != null) {
			mPlayer.setLooping(mLooping);
		}
	}
	
	private int mStreamType = 0;
	@Override
	public void setAudioStreamType(int streamtype) {
		LogUtils.info("MediaPlayer setAudioStreamType: " + streamtype);
		mStreamType = streamtype;
	}
	
	private void setAudioStreamType() {
		if (mPlayer != null) {
			mPlayer.setAudioStreamType(mStreamType);
		}
	}
	
	SimpleSubTitleParser mSubtitleParser = null;
	@Override
    public void setSubtitleParser(SimpleSubTitleParser parser) {
        mSubtitleParser = parser;
        
    }
    
    private void setSubtitleParser() {
        if(mSubtitleParser != null) {
            mPlayer.setSubtitleParser(mSubtitleParser);
        }

    }

	/**
	 * Interface definition for a callback to be invoked when the media source
	 * is ready for playback.
	 */
	public interface OnPreparedListener {
		/**
		 * Called when the media file is ready for playback.
		 * 
		 * @param mp
		 *            the MediaPlayer that is ready for playback
		 */
		void onPrepared(MediaPlayer mp);
	}
	
	/**
	 * Register a callback to be invoked when the media source is ready for
	 * playback.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	@Override
	public void setOnPreparedListener(OnPreparedListener listener) {
		mOnPreparedListener = listener;
	}
	private void setOnPreparedListener() {
		if (mPlayer != null) {
			mPlayer.setOnPreparedListener(mOnPreparedListener);
		}
	}

	private OnPreparedListener mOnPreparedListener;

	/**
	 * Interface definition for a callback to be invoked when playback of a
	 * media source has completed.
	 */
	public interface OnCompletionListener {
		/**
		 * Called when the end of a media source is reached during playback.
		 * 
		 * @param mp
		 *            the MediaPlayer that reached the end of the file
		 */
		void onCompletion(MediaPlayer mp);
	}
	/**
	 * Register a callback to be invoked when the end of a media source has been
	 * reached during playback.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	@Override
	public void setOnCompletionListener(OnCompletionListener listener) {
		mOnCompletionListener = listener;
	}
	private void setOnCompletionListener() {
		if (mPlayer != null) {
			mPlayer.setOnCompletionListener(mOnCompletionListener);
		}
	}

	private OnCompletionListener mOnCompletionListener;

	/**
	 * Interface definition of a callback to be invoked indicating buffering
	 * status of a media resource being streamed over the network.
	 */
	public interface OnBufferingUpdateListener {
		/**
		 * Called to update status in buffering a media stream received through
		 * progressive HTTP download. The received buffering percentage
		 * indicates how much of the content has been buffered or played. For
		 * example a buffering update of 80 percent when half the content has
		 * already been played indicates that the next 30 percent of the content
		 * to play has been buffered.
		 * 
		 * @param mp
		 *            the MediaPlayer the update pertains to
		 * @param percent
		 *            the percentage (0-100) of the content that has been
		 *            buffered or played thus far
		 */
		void onBufferingUpdate(MediaPlayer mp, int percent);
	}
	/**
	 * Register a callback to be invoked when the status of a network stream's
	 * buffer has changed.
	 * 
	 * @param listener
	 *            the callback that will be run.
	 */
	@Override
	public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
		mOnBufferingUpdateListener = listener;
	}
	
	private void setOnBufferingUpdateListener() {
		if (mPlayer != null) {
			mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
		}
	}

	private OnBufferingUpdateListener mOnBufferingUpdateListener;

	/**
	 * Interface definition of a callback to be invoked indicating the
	 * completion of a seek operation.
	 */
	public interface OnSeekCompleteListener {
		/**
		 * Called to indicate the completion of a seek operation.
		 * 
		 * @param mp
		 *            the MediaPlayer that issued the seek operation
		 */
		public void onSeekComplete(MediaPlayer mp);
	}
	/**
	 * Register a callback to be invoked when a seek operation has been
	 * completed.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
		mOnSeekCompleteListener = listener;
	}
	private void setOnSeekCompleteListener() {
		if (mPlayer != null) {
			mPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
		}
	}

	private OnSeekCompleteListener mOnSeekCompleteListener;

	/**
	 * Interface definition of a callback to be invoked when the video size is
	 * first known or updated
	 */
	public interface OnVideoSizeChangedListener {
		/**
		 * Called to indicate the video size
		 * 
		 * The video size (width and height) could be 0 if there was no video,
		 * no display surface was set, or the value was not determined yet.
		 * 
		 * @param mp
		 *            the MediaPlayer associated with this callback
		 * @param width
		 *            the width of the video
		 * @param height
		 *            the height of the video
		 */
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
	}
	/**
	 * Register a callback to be invoked when the video size is known or
	 * updated.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	public void setOnVideoSizeChangedListener(
			OnVideoSizeChangedListener listener) {
		mOnVideoSizeChangedListener = listener;
	}
	private void setOnVideoSizeChangedListener() {
		if (mPlayer != null) {
			mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
		}
	}

	private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

	/*
	 * Do not change these values without updating their counterparts in
	 * include/media/mediaplayer.h!
	 */
	/**
	 * Unspecified media player error.
	 * 
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	public static final int MEDIA_ERROR_UNKNOWN = 1;

	/**
	 * Media server died. In this case, the application must release the
	 * MediaPlayer object and instantiate a new one.
	 * 
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	public static final int MEDIA_ERROR_SERVER_DIED = 100;

	/**
	 * The video is streamed and its container is not valid for progressive
	 * playback i.e the video's index (e.g moov atom) is not at the start of the
	 * file.
	 * 
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
	
	public static final int MEDIA_ERROR_CODEC_NOT_FOUND 				   	= 300;
	public static final int MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR 			= -200;

	/** File or network related operation errors. */
	public static final int MEDIA_ERROR_IO = -1004;
	/** Bitstream is not conforming to the related coding standard or file spec. */
	public static final int MEDIA_ERROR_MALFORMED = -1007;
	/**
	 * Bitstream is conforming to the related coding standard or file spec, but
	 * the media framework does not support the feature.
	 */
	public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
	/**
	 * Some operation takes too long to complete, usually more than 3-5 seconds.
	 */
	public static final int MEDIA_ERROR_TIMED_OUT = -110;
	
	public static final int MEDIA_ERROR_FAIL_TO_READ_PACKET		= 301;
	public static final int MEDIA_ERROR_FAIL_TO_OPEN				= 303;
	public static final int MEDIA_ERROR_FAIL_TO_SEEK				= 304;
	
	public static final int MEDIA_ERROR_AUDIO_PLAYER 	= 311;
	public static final int MEDIA_ERROR_AUDIO_RENDER 	= 312;
	public static final int MEDIA_ERROR_VIDEO_PLAYER 	= 313;
	public static final int MEDIA_ERROR_VIDEO_RENDER 	= 314;
	public static final int MEDIA_ERROR_DEMUXER		= 315;
	
	public static final int MEDIA_ERROR_AUDIO_DECODER = 321;
	public static final int MEDIA_ERROR_VIDEO_DECODER = 322;

	/**
	 * Interface definition of a callback to be invoked when there has been an
	 * error during an asynchronous operation (other errors will throw
	 * exceptions at method call time).
	 */
	public interface OnErrorListener {
		/**
		 * Called to indicate an error.
		 * 
		 * @param mp
		 *            the MediaPlayer the error pertains to
		 * @param what
		 *            the type of error that has occurred:
		 *            <ul>
		 *            <li>{@link #MEDIA_ERROR_UNKNOWN}
		 *            <li>{@link #MEDIA_ERROR_SERVER_DIED}
		 *            </ul>
		 * @param extra
		 *            an extra code, specific to the error. Typically
		 *            implementation dependent.
		 *            <ul>
		 *            <li>{@link #MEDIA_ERROR_IO}
		 *            <li>{@link #MEDIA_ERROR_MALFORMED}
		 *            <li>{@link #MEDIA_ERROR_UNSUPPORTED}
		 *            <li>{@link #MEDIA_ERROR_TIMED_OUT}
		 *            </ul>
		 * @return True if the method handled the error, false if it didn't.
		 *         Returning false, or not having an OnErrorListener at all,
		 *         will cause the OnCompletionListener to be called.
		 */
		boolean onError(MediaPlayer mp, int what, int extra);
	}
	/**
	 * Register a callback to be invoked when an error has happened during an
	 * asynchronous operation.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	@Override
	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}
	private void setOnErrorListener() {
		if (mPlayer != null) {
			mPlayer.setOnErrorListener(mOnErrorListener);
		}
	}

	private OnErrorListener mOnErrorListener;

	/**
	 * Interface definition of a callback to be invoked to communicate some info
	 * and/or warning about the media or its playback.
	 */
	public interface OnInfoListener {
		/**
		 * Called to indicate an info or a warning.
		 * 
		 * @param mp
		 *            the MediaPlayer the info pertains to.
		 * @param what
		 *            the type of info or warning.
		 *            <ul>
		 *            <li>{@link #MEDIA_INFO_UNKNOWN}
		 *            <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
		 *            <li>{@link #MEDIA_INFO_VIDEO_RENDERING_START}
		 *            <li>{@link #MEDIA_INFO_BUFFERING_START}
		 *            <li>{@link #MEDIA_INFO_BUFFERING_END}
		 *            <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
		 *            <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
		 *            <li>{@link #MEDIA_INFO_METADATA_UPDATE}
		 *            <li>{@link #MEDIA_INFO_UNSUPPORTED_SUBTITLE}
		 *            <li>{@link #MEDIA_INFO_SUBTITLE_TIMED_OUT}
		 *            </ul>
		 * @param extra
		 *            an extra code, specific to the info. Typically
		 *            implementation dependent.
		 * @return True if the method handled the info, false if it didn't.
		 *         Returning false, or not having an OnErrorListener at all,
		 *         will cause the info to be discarded.
		 */
		boolean onInfo(MediaPlayer mp, int what, int extra);
	}
	/**
	 * Register a callback to be invoked when an info/warning is available.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */
	@Override
	public void setOnInfoListener(OnInfoListener listener) {
		mOnInfoListener = listener;
	}
	private void setOnInfoListener() {
		if (mPlayer != null) {
			mPlayer.setOnInfoListener(mOnInfoListener);
		}
	}

	private OnInfoListener mOnInfoListener;

	/**
	 * Register a callback to be invoked when a timed text is available for
	 * display.
	 * 
	 * @param listener
	 *            the callback that will be run
	 */

	/**
	 * Interface definition of a callback to be invoked when a timed text is
	 * available for display.
	 */
	//public interface OnTimedTextListener {
		/**
		 * Called to indicate an avaliable timed text
		 * 
		 * @param mp
		 *            the MediaPlayer associated with this callback
		 * @param text
		 *            the timed text sample which contains the text needed to be
		 *            displayed and the display format.
		 */
	//	public void onTimedText(MediaPlayer mp, TimedText text);
	//}
	
	/*private OnTimedTextListener mOnTimedTextListener;
	@Override
	public void setOnTimedTextListener(OnTimedTextListener listener) {
		mOnTimedTextListener = listener;
	}
	private void setOnTimedTextListener() {
		if (mPlayer != null) {
			mPlayer.setOnTimedTextListener(mOnTimedTextListener);
		}
	}*/

	@Override
	public TrackInfo[] getTrackInfo() throws IllegalStateException {
		if (mPlayer != null) {
			return mPlayer.getTrackInfo();
		} else {
			return null;
		}
	}

	public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
	
	@Override
	public void addTimedTextSource(String path, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		if (mPlayer != null) {
			mPlayer.addTimedTextSource(path, mimeType);
		}
	}

	@Override
	public void addTimedTextSource(Context context, Uri uri, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		if (mPlayer != null) {
			mPlayer.addTimedTextSource(context, uri, mimeType);
		}
	}

	@Override
	public void selectTrack(int index) throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.selectTrack(index);
		}
	}

	@Override
	public void deselectTrack(int index) throws IllegalStateException {
		if (mPlayer != null) {
			mPlayer.deselectTrack(index);
		}
	}
	
	// snapshot
	// width, height: output demension, fmt: 0-RGBX_8888, msec: -1 : get current frame 
	@Override
	public Bitmap getSnapShot(int width, int height, int fmt, int msec) {
		if (mPlayer != null) {
			if (mPlayer instanceof FFMediaPlayer || mPlayer instanceof XOMediaPlayer) { 
				// mDecodeMode == DecodeMode.SW || DecodeMode.HW_XOPLAYER
				LogUtils.info("getSnapShot: " + msec);
				return mPlayer.getSnapShot(width, height, fmt, msec);
			}
			
			return null;
		} else {
		    LogUtils.error("mMeetPlayer is null");
			throw new IllegalStateException("mMeetPlayer is null");
		}
	}

    /**
     * @param screenOn Supply true to keep the screen on, false to allow it
     * to turn off.
     */
    public void setScreenOnWhilePlaying(boolean screenOn) {
    	mScreenOn = screenOn;
    	setScreenOnWhilePlaying();
    }
    
    private void setScreenOnWhilePlaying() {
    	if (mPlayer != null) {
			mPlayer.setScreenOnWhilePlaying(mScreenOn);
		}
    }
    
    public void setWakeMode(Context ctx, int mode) {
		mContext = ctx;
		mWakeMode = mode;
		setWakeMode();
	}
	
	private void setWakeMode() {
		if (mPlayer != null && mContext != null) {
			mPlayer.setWakeMode(mContext, mWakeMode);
		}
	}
	
	public void setOption(String opt) {
		mOption = opt;
	}
	
	private void setOption() {
		if (mPlayer != null) {
			mPlayer.setOption(mOption);
		}
	}
	
	// event
    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    protected static final int MEDIA_NOP = 0; // interface test message
    protected static final int MEDIA_PREPARED = 1;
    protected static final int MEDIA_PLAYBACK_COMPLETE = 2;
    protected static final int MEDIA_BUFFERING_UPDATE = 3;
    protected static final int MEDIA_SEEK_COMPLETE = 4;
    protected static final int MEDIA_SET_VIDEO_SIZE = 5;
    protected static final int MEDIA_TIMED_TEXT = 99;
    protected static final int MEDIA_ERROR = 100;
    protected static final int MEDIA_INFO = 200;
    
	/**
	 * Unspecified media player info.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_UNKNOWN = 1;

	/**
	 * The video is too complex for the decoder: it can't decode frames fast
	 * enough. Possibly only the audio plays fine at this stage.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
	
	/** 
	 * MediaPlayer is temporarily pausing playback internally in order to
	 * buffer more data.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_BUFFERING_START = 701;

	/** 
	 * MediaPlayer is resuming playback after filling buffers.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_BUFFERING_END = 702;
	
	/**
	 * Bad interleaving means that a media has been improperly interleaved or
	 * not interleaved at all, e.g has all the video samples first then all the
	 * audio ones. Video is playing but a lot of disk seeks may be happening.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

	/**
	 * The media cannot be seeked (e.g live stream)
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

	/**
	 * A new set of metadata is available.
	 * 
	 * @see android.media.MediaPlayer.OnInfoListener
	 */
	public static final int MEDIA_INFO_METADATA_UPDATE = 802;
	
	// for test performace
	public static final int MEDIA_INFO_TEST_DECODE_AVG_MSEC	= 901;
	public static final int MEDIA_INFO_TEST_RENDER_AVG_MSEC	= 902;
	public static final int MEDIA_INFO_TEST_DECODE_FPS		= 903;
	public static final int MEDIA_INFO_TEST_RENDER_FPS		= 904;
	
	public static final int MEDIA_INFO_TEST_RENDER_FRAME		= 905;
	public static final int MEDIA_INFO_TEST_LATENCY_MSEC		= 906;
	public static final int MEDIA_INFO_TEST_DROP_FRAME		= 907;
	public static final int MEDIA_INFO_TEST_IO_BITRATE		= 921;
	public static final int MEDIA_INFO_TEST_MEDIA_BITRATE		= 922;

	public static final int MEDIA_INFO_TEST_PLAYER_TYPE		= 911;
	
}
