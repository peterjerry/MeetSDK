package com.gotye.meetsdk.player;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetsdk.util.LogUtils;
import com.gotye.meetsdk.player.MediaController.MediaPlayerControl;
import com.gotye.meetsdk.player.MediaPlayer.OnCompletionListener;
import com.gotye.meetsdk.player.MediaPlayer.OnErrorListener;
import com.gotye.meetsdk.player.MediaPlayer.OnInfoListener;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;

import java.io.IOException;
import java.util.Map;

// 2015.1.13 guoliangma change code from 4.2_r1 android.widget.VideoView
/**
 * Displays a video file.  The VideoView class
 * can load images from various sources (such as resources or content
 * providers), takes care of computing its measurement from the video so that
 * it can be used in any layout manager, and provides various display options
 * such as scaling and tinting.
 */
public class MeetVideoView extends SurfaceView implements MediaPlayerControl {
    private String TAG = "gotye/VideoView";
    // settable by the client
    private Uri         mUri;
    private Map<String, String> mHeaders;
    private int         mDuration;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
	
	// mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

	// 2015.1.13 guoliangma added
	// display mode
	public static final int SCREEN_FIT = 0; // 自适应
    public static final int SCREEN_STRETCH = 1; // 铺满屏幕 
    public static final int SCREEN_FILL = 2; // 放大裁切
    public static final int SCREEN_CENTER = 3; // 原始大小
	
	private int mDisplayMode = SCREEN_FIT;
	
	private DecodeMode mDecodeMode = DecodeMode.AUTO;
	private DecodeMode mDecodeModeImpl;
	private Context mContext;
	private int mAudioChannel = -1; // default
	private String mOption;
	// end of guoliangma added
	
    // All the stuff we need for playing and showing a video
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSurfaceWidth;
    private int         mSurfaceHeight;
    //private MediaController mMediaController;
    private OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private int         mCurrentBufferPercentage;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener  mOnInfoListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;

    public MeetVideoView(Context context) {
        super(context);
        initVideoView();
    }

    public MeetVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initVideoView();
    }

    public MeetVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        initVideoView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            switch (mDisplayMode) {
            case SCREEN_CENTER:
                width = mVideoWidth;
                height = mVideoHeight;
                break;
            case SCREEN_FIT:
                if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                } else if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                }
				break;
            case SCREEN_FILL:
                if (mVideoWidth * height > width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height < width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                }
				break;
            case SCREEN_STRETCH:
                /* Do nothing */
                break;
            default:
                break;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MeetVideoView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MeetVideoView.class.getName());
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                /* Parent says we can be as big as we want. Just don't be larger
                 * than max size imposed on ourselves.
                 */
                result = desiredSize;
                break;

            case MeasureSpec.AT_MOST:
                /* Parent says we can be as big as we want, up to specSize.
                 * Don't be larger than specSize, and don't be larger than
                 * the max size imposed on ourselves.
                 */
                result = Math.min(desiredSize, specSize);
                break;

            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

    @SuppressWarnings("deprecation") // avoid setType warning
    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSHCallback);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * @hide
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
		LogUtils.info("setVideoURI: " + uri.toString());
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        // 2015.1.27 guoliangma
        //openVideo();
        mDecodeModeImpl = mDecodeMode;
        if (DecodeMode.AUTO == mDecodeModeImpl)
        	mDecodeModeImpl = MeetSDK.getPlayerType(mUri);
        
        setVisibility(View.INVISIBLE);
        mDecodeModeImpl.setSurfaceType(getHolder());
        setVisibility(View.VISIBLE);
        
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
			LogUtils.info("stopPlayback");
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }
    }

    private void openVideo() {
		if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
        	LogUtils.debug("openVideo() not ready for playback");
        	if (mUri == null)
        		LogUtils.debug("mUri is null");
        	else
        		LogUtils.debug("mSurfaceHolder is null");
        	return;
        }

		LogUtils.info("openVideo() " + mUri.toString());
		
        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the framework.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer(mDecodeModeImpl);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mDuration = -1;
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(/*mOnInfoListener*/mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            if (mOption != null)
            	mMediaPlayer.setOption(mOption);
            if (mAudioChannel != -1) {
            	mMediaPlayer.selectTrack(mAudioChannel);
            	mAudioChannel = -1;
            	LogUtils.info(String.format("pre-set audio track to #%d", mAudioChannel));
            }
            mMediaPlayer.prepareAsync();
            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
			LogUtils.error("Unable to open content(IOException): " + mUri);
            Log.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
        } catch (IllegalArgumentException ex) {
			LogUtils.error("Unable to open content(IllegalArgumentException): " + mUri);
            Log.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
        } catch (IllegalStateException ex) {
        	LogUtils.error("Unable to open content(IllegalStateException): " + mUri);
            Log.e(TAG, "Unable to open content(IllegalStateException): " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
        }
    }
    
    public void setMediaController(MediaController controller) {
    	if (controller != null) {
    		controller.setMediaPlayer(this);
            controller.setEnabled(isInPlaybackState());
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    requestLayout();
                }
            }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;

            // 2015.1.13 guoliangma always set three flag to true
			// fix me!
            mCanPause = mCanSeekBack = mCanSeekForward = true;


            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            LogUtils.info("onPrepared: width: " + mVideoWidth + ", height: " + mVideoHeight);

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    // We didn't actually change the size (it was already at the size
                    // we need), so we won't get a "surface changed" callback, so
                    // start the video here instead of in the callback.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };
	
	private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = 
		new MediaPlayer.OnSeekCompleteListener() {
        public void onSeekComplete(MediaPlayer mp) {
            LogUtils.info("onSeekCompletion");
            mCurrentState = STATE_PLAYING;
            mTargetState = STATE_PLAYING;
            
            if (mOnSeekCompleteListener != null) {
                mOnSeekCompleteListener.onSeekComplete(mp);
            }
        } 
    };

    private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
		
            Log.e(TAG, "Error: " + framework_err + "," + impl_err);
			LogUtils.error(String.format("onError: %d %d", framework_err, impl_err));
			if (framework_err == MediaPlayer.MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR)
                LogUtils.error("onError: MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR");
				
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }

            return true;
        }
    };

    // 2015.1.13 guoliangma added
    private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (mOnInfoListener != null) {
                return mOnInfoListener.onInfo(mp, what, extra);
            }
            
            //todo
            return false;
        }
    };
    // end of added
    
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l)
    {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener l)
    {
        mOnCompletionListener = l;
    }
	
	/**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param l the callback that will be run
     */
	public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l)
	{
        mOnSeekCompleteListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(OnErrorListener l)
    {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(OnInfoListener l) {
        mOnInfoListener = l;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        public void surfaceChanged(SurfaceHolder holder, int format,
                                    int w, int h)
        {
        	LogUtils.info("video surfaceChanged()");
        	Log.d(TAG, "video surfaceChanged()");
        	
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState =  (mTargetState == STATE_PLAYING);
            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        public void surfaceCreated(SurfaceHolder holder)
        {
        	LogUtils.info("video surfaceCreated()");
            mSurfaceHolder = holder;
            openVideo();
        }

        public void surfaceDestroyed(SurfaceHolder holder)
        {
        	LogUtils.info("video surfaceDestroyed()");
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            release(true);
        }
    };

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                                     keyCode != KeyEvent.KEYCODE_MENU &&
                                     keyCode != KeyEvent.KEYCODE_CALL &&
                                     keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                } else {
                    start();
                }
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                }
                return true;
            }
        }
        
        return false;//super.onKeyDown(keyCode, event);
    }

    public void start() {
    	LogUtils.info("start()");
        if (isInPlaybackState()) {
        	LogUtils.info("start() mMediaPlayer.start");
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
        openVideo();
    }

    // cache duration as mDuration for faster access
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec) {
        if (isInPlaybackState()) {
			LogUtils.info(String.format("seekto() pos: %d msec", msec));
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
			LogUtils.info(String.format("seekto() pre-set pos: %d msec", msec));
            mSeekWhenPrepared = msec;
        }
    }

    /**
     * @brief		是否正在播放
     * @return     true if playing
     */
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    /**
     * @brief		获取缓冲百分比
     * @return     已缓冲数据占媒体时长百分比(0-100)
     */
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    /**
     * @brief		是否支持暂停
     * @return     true if support Pause
     */
    public boolean canPause() {
        return mCanPause;
    }

    /**
     * @brief		是否支持回退
     * @return     true if support seek back
     */
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    /**
     * @brief		是否支持前进
     * @return     true if support seek forward
     */
    public boolean canSeekForward() {
        return mCanSeekForward;
    }
	
	// 2015.1.13 guoliangma added
    /**
     * @brief		设置解码模式
     * param[in]    mode 解码模式
     */
	public void setDecodeMode(DecodeMode mode) {
        mDecodeMode = mode;
    }

	/**
     * @brief		获取当前解码模式
     * @return     解码模式 DecodeMode
     */
    public DecodeMode getDecodeMode() {
        if (null != mMediaPlayer) {
            return mMediaPlayer.getDecodeMode();
        }

        return DecodeMode.UNKNOWN;
    }
    
    /**
     * @brief		选择音轨
     * @param[in]	index: index of all streams(NOT audio index)
     */
    public void selectAudioChannel(int index){
        mAudioChannel = index;
        if (mMediaPlayer != null) {
        	mMediaPlayer.selectTrack(mAudioChannel);
        	mAudioChannel = -1;
        }
    }
    
    /**
     * @brief		获取当前显示缩放模式
     * @return     当前显示模式
     */
	public int getDisplayMode() {
		return mDisplayMode;
	}

	/**
    * @brief		设置显示缩放模式
    * @param[in]	mode 显示模式
    * @ int SCREEN_FIT = 0; // 自适应
    * @ int SCREEN_STRETCH = 1; // 铺满屏幕 
    * @ int SCREEN_FILL = 2; // 放大裁切
    * @ int SCREEN_CENTER = 3; // 原始大小
    */
	public void setDisplayMode(int mode) {
		LogUtils.info(String.format("setDisplayerMode %d", mode));
		mDisplayMode = mode;
        requestLayout();
	}

	/**
    * @brief		循环切换显示缩放模式
    * 自适应 -> 铺满屏幕 -> 放大裁切 -> 原始大小
    */
	public void switchDisplayMode() {
		setDisplayMode((mDisplayMode + 1) % 4);
	}
	
	/**
    * @brief		设置自定义参数
    * @param[in]	opt 参数
    * e.g. -param1 val1\n-param2 val2\param3 val3
    */
	public void setOption(String opt) {
		mOption = opt;
	}
	
	/**
    * @brief		获取当前播放媒体文件的媒体信息   
    * @return      MediaInfo 数据结构，详见结构体
    */
	public MediaInfo getMediaInfo() {
		if (mMediaPlayer != null)
			return mMediaPlayer.getMediaInfo();
		
		return null;
	}
	
	/**
    * @brief		获取音频sessionId 自有播放器模式总是返回0   
    * @return      sessionId
    */
	public int getAudioSessionId() {
		if (mMediaPlayer != null) {
			return mMediaPlayer.getAudioSessionId();
		}
		
		return 0;
	}
}
