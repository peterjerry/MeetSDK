package com.gotye.meetsdk.player;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.MediaPlayer.OnBufferingUpdateListener;
import com.gotye.meetsdk.player.MediaPlayer.OnCompletionListener;
import com.gotye.meetsdk.player.MediaPlayer.OnErrorListener;
import com.gotye.meetsdk.player.MediaPlayer.OnInfoListener;
import com.gotye.meetsdk.player.MediaPlayer.OnPreparedListener;
import com.gotye.meetsdk.player.MediaPlayer.OnSeekCompleteListener;
import com.gotye.meetsdk.player.MediaPlayer.OnVideoSizeChangedListener;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.util.LogUtils;
import android.view.SurfaceHolder;

abstract class BaseMediaPlayer implements MediaPlayerInterface {
	private SurfaceHolder mHolder = null;
	
	private MediaPlayer.OnBufferingUpdateListener 	mOnBufferingUpdateListener 	= null;
	private MediaPlayer.OnCompletionListener 		mOnCompletionListener 		= null;
	private MediaPlayer.OnErrorListener 			mOnErrorListener 			= null;
	private MediaPlayer.OnInfoListener 				mOnInfoListener 			= null;
	private MediaPlayer.OnPreparedListener 			mOnPreparedListener 		= null;
	private MediaPlayer.OnSeekCompleteListener 		mOnSeekCompleteListener 	= null;
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = null;
	//private MediaPlayer.OnTimedTextListener			mOnTimedTextListener		= null;

	private PowerManager.WakeLock mWakeLock = null;
	private boolean mScreenOnWhilePlaying = false;
	private boolean mStayAwake = false;
	
	private PlayState mState = PlayState.IDLE;
	
	protected enum PlayState {
		IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, STOPPING, STOPPED, PAUSED, PLAYBACK_COMPLETED, END, ERROR
	};
	
	protected /* synchronized */ void setState(PlayState state) {
		mState = state;
	}
	
	protected /* synchronized */ PlayState getState() {
		return mState;
	}
	
	protected BaseMediaPlayer(MediaPlayer mp) {
		Looper looper;
	    if ((looper = Looper.myLooper()) != null) {
	        mEventHandler = new EventHandler(mp, looper);
	    } else if ((looper = Looper.getMainLooper()) != null) {
	        mEventHandler = new EventHandler(mp, looper);
	    } else {
	        mEventHandler = null;
	    }
	}
	
    protected void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    protected void updateSurfaceScreenOn() {
        if (mHolder != null) {
        	LogUtils.info("Java: updateSurfaceScreenOn: mScreenOnWhilePlaying "
        		+ mScreenOnWhilePlaying + " , mStayAwake " + mStayAwake);
            mHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }
    
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
			
	@Override
	public void setDisplay(SurfaceHolder sh) {
		mHolder	= sh;
	}
	
	public abstract int flags() throws IllegalStateException;
	
	public abstract Bitmap getSnapShot(int width, int height, int fmt, int msec);

	public abstract void prepare() throws IOException, IllegalStateException;

	public abstract void prepareAsync() throws IllegalStateException;

	public abstract void start() throws IllegalStateException;

	public abstract void stop() throws IllegalStateException;

	public abstract void pause() throws IllegalStateException;

	public abstract void seekTo(int msec) throws IllegalStateException;

	@Override
	public void release() {
		stayAwake(false);
		updateSurfaceScreenOn();
		mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnVideoSizeChangedListener = null;
        //mOnTimedTextListener = null;
	}

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

	@Override
	public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
		mOnBufferingUpdateListener = listener;
	}

	@Override
	public void setOnCompletionListener(OnCompletionListener listener) {
		mOnCompletionListener = listener;
	}

	@Override
	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}

	@Override
	public void setOnInfoListener(OnInfoListener listener) {
		mOnInfoListener = listener;
	}

	@Override
	public void setOnPreparedListener(OnPreparedListener listener) {
		mOnPreparedListener = listener;
	}

	@Override
	public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
		mOnSeekCompleteListener = listener;
	}

	@Override
	public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
		mOnVideoSizeChangedListener = listener;
	}

	/*@Override
	public void setOnTimedTextListener(OnTimedTextListener listener) {
		mOnTimedTextListener = listener;
	}*/
	
	/**
     * Control whether we should use the attached SurfaceHolder to keep the
     * screen on while video playback is occurring.  This is the preferred
     * method over {@link #setWakeMode} where possible, since it doesn't
     * require that the application have permission for low-level wake lock
     * access.
     *
     * @param screenOn Supply true to keep the screen on, false to allow it
     * to turn off.
     */
	@Override
	public void setScreenOnWhilePlaying(boolean screenOn) {
    	LogUtils.info("setScreenOnWhilePlaying: " + screenOn);
        if (mScreenOnWhilePlaying != screenOn) {
            if (screenOn && mHolder == null) {
            	LogUtils.warn("setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }
	
	/**
     * Set the low-level power management behavior for this MediaPlayer.  This
     * can be used when the MediaPlayer is not playing through a SurfaceHolder
     * set with {@link #setDisplay(SurfaceHolder)} and thus can use the
     * high-level {@link #setScreenOnWhilePlaying(boolean)} feature.
     *
     * <p>This function has the MediaPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link android.os.PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context the Context to use
     * @param mode    the power/wake mode to set
     * @see android.os.PowerManager
     */
	@Override
	public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }
	
	public abstract DecodeMode getDecodeMode();
	
	protected EventHandler mEventHandler;
	
	protected class EventHandler extends Handler
    {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
        	
            switch(msg.what) {
            case MediaPlayer.MEDIA_PREPARED:
				if (mOnPreparedListener != null) {
					mOnPreparedListener.onPrepared(mMediaPlayer);
				}
				return;
				
			case MediaPlayer.MEDIA_PLAYBACK_COMPLETE:
				if (mOnCompletionListener != null) {
					mOnCompletionListener.onCompletion(mMediaPlayer);
				}
				// 2015.6.8 guoliangma move here
				stayAwake(false);
				return;
				
			case MediaPlayer.MEDIA_BUFFERING_UPDATE:
				if (mOnBufferingUpdateListener != null) {
					mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1 /* percent */);
				}
				return;

			case MediaPlayer.MEDIA_SEEK_COMPLETE:
				if (mOnSeekCompleteListener != null) {
					mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
				}
				// 2015.6.8 guoliangma comment out to fix "keep screen on" problem
				//stayAwake(false);
				return;

			case MediaPlayer.MEDIA_SET_VIDEO_SIZE:
				if (mOnVideoSizeChangedListener != null) {
					mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1 /* width */, msg.arg2 /* height */);
				}
				return;

			case MediaPlayer.MEDIA_ERROR:
				if (mOnErrorListener != null) {
					mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
				}
				stayAwake(false);
				return;

			case MediaPlayer.MEDIA_INFO:
				if (mOnInfoListener != null) {
					mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
				}
				return;
				
            case MediaPlayer.MEDIA_TIMED_TEXT:
                // todo
                return;

            case MediaPlayer.MEDIA_NOP: // interface test message - ignore
                break;

            default:
            	LogUtils.error("Unknown message type " + msg.what);
				return;
            }
        }
    }
}
