package android.pplive.media.player;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.TimedText;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.pplive.media.subtitle.SimpleSubTitleParser;
import android.pplive.media.util.LogUtils;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.player.MediaPlayer.OnBufferingUpdateListener;
import android.pplive.media.player.MediaPlayer.OnCompletionListener;
import android.pplive.media.player.MediaPlayer.OnErrorListener;
import android.pplive.media.player.MediaPlayer.OnInfoListener;
import android.pplive.media.player.MediaPlayer.OnPreparedListener;
import android.pplive.media.player.MediaPlayer.OnSeekCompleteListener;
//import android.pplive.media.player.MediaPlayer.OnTimedTextListener;
import android.pplive.media.player.MediaPlayer.OnVideoSizeChangedListener;

@SuppressLint("NewApi")
public class SystemMediaPlayer extends android.media.MediaPlayer implements
		MediaPlayerInterface {

	private static final int PAUSE_AVAILABLE         = 1; // Boolean
	private static final int SEEK_BACKWARD_AVAILABLE = 2; // Boolean
	private static final int SEEK_FORWARD_AVAILABLE  = 3; // Boolean
	private static final int SEEK_AVAILABLE          = 4; // Boolean
	
	private static final int NEW_PAUSE_AVAILABLE         = 29; // Boolean
	private static final int NEW_SEEK_BACKWARD_AVAILABLE = 30; // Boolean
	private static final int NEW_SEEK_FORWARD_AVAILABLE  = 31; // Boolean	
		
	private MediaPlayer mMediaPlayer;

	public SystemMediaPlayer(MediaPlayer mp) {
		mMediaPlayer = mp;
	}
	
	@Override
	public int getBufferingTime() {
		//not implement in system player
	    return 0;
	}
	
	@Override
	public int flags() throws IllegalStateException {
		int cap = 0; //CAN_PAUSE | CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD  | CAN_SEEK;
		
		try {
			
			Class<?> clazz = android.media.MediaPlayer.class;

			Method getMetadataMethod = clazz.getDeclaredMethod("getMetadata", boolean.class, boolean.class);
			getMetadataMethod.setAccessible(true);
			Object metadata = getMetadataMethod.invoke(this, false, false);
			
			if (metadata != null) {
				clazz = metadata.getClass();
				Method getBooleanMethod = clazz.getDeclaredMethod("getBoolean", int.class);
				getBooleanMethod.setAccessible(true);
				
				cap |= (Boolean) getBooleanMethod.invoke(metadata, PAUSE_AVAILABLE) || 
							(Boolean) getBooleanMethod.invoke(metadata, NEW_PAUSE_AVAILABLE) ? MediaPlayer.CAN_PAUSE : 0;
				cap |= (Boolean) getBooleanMethod.invoke(metadata, SEEK_BACKWARD_AVAILABLE) ||
							(Boolean) getBooleanMethod.invoke(metadata, NEW_SEEK_BACKWARD_AVAILABLE) ? MediaPlayer.CAN_SEEK_BACKWARD : 0;
				cap |= (Boolean) getBooleanMethod.invoke(metadata, SEEK_FORWARD_AVAILABLE) ||
							(Boolean) getBooleanMethod.invoke(metadata, NEW_SEEK_FORWARD_AVAILABLE) ? MediaPlayer.CAN_SEEK_FORWARD : 0;
				cap |= (Boolean) getBooleanMethod.invoke(metadata, SEEK_AVAILABLE)? MediaPlayer.CAN_SEEK : 0;
			}
			
		} catch (Exception e) {
		    LogUtils.error("Exception", e);
		}
		
		return cap;
	}
	
	@Override
	public void setDisplay(SurfaceHolder sh) {
		super.setDisplay(sh);
	}
	
	@Override
	public void setAudioStreamType(int streamType) {
		super.setAudioStreamType(streamType);
	}
	
	@Override
	public void selectTrack(int index) {
		// TODO Auto-generated method stub
		super.selectTrack(index);
	}
	
	@Override
	public void deselectTrack(int index) {
		// TODO Auto-generated method stub
		super.deselectTrack(index);
	}
	
	public void setSubtitleParser(SimpleSubTitleParser parser) {
		// not implement
	}
	
	@Override
	public boolean isLooping () {
		return super.isLooping();
	}
	
	@Override
	public void setLooping (boolean looping) {
		super.setLooping(looping);
	}
	
	@Override
	public void prepareAsync() throws IllegalStateException {
		
		try {
			super.prepareAsync();
			if (null != mOnInfoListener) {
				mOnInfoListener.onInfo(mMediaPlayer, 
					MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE, 
					MediaPlayer.PLAYER_IMPL_TYPE_SYSTEM_PLAYER);
			}
		} catch(Exception e) {
			if (null != mOnErrorListener) {
				mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR, 0);
			}
			else
			{
			    LogUtils.error("mOnErrorListener is NULL", e);
			}
		}
	}
	
	@Override
	public void prepare() throws IllegalStateException {
		
		try {
			super.prepare();
			if (null != mOnInfoListener) {
				mOnInfoListener.onInfo(mMediaPlayer, 
					MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE, 
					MediaPlayer.PLAYER_IMPL_TYPE_SYSTEM_PLAYER);
			}
		} catch(Exception e) {
			if (null != mOnErrorListener) {
				mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR, 0);
			}
			else
			{
			    LogUtils.error("mOnErrorListener is NULL", e);
			}
		}
	}
	
	@Override
	public Bitmap getSnapShot(int width, int height, int fmt, int msec) {
		return null;
	}

	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
	private android.media.MediaPlayer.OnBufferingUpdateListener mSystemOnBufferingUpdateListener = new OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
			if (null != mOnBufferingUpdateListener) {
				mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer,
						percent);
			}
		}
	};

	@Override
	public void setOnBufferingUpdateListener(
			MediaPlayer.OnBufferingUpdateListener listener) {
		mOnBufferingUpdateListener = listener;
		super.setOnBufferingUpdateListener(mSystemOnBufferingUpdateListener);
	}

	private MediaPlayer.OnCompletionListener mOnCompletionListener = null;
	private android.media.MediaPlayer.OnCompletionListener mSystemCompletionListener = new OnCompletionListener() {
		@Override
		public void onCompletion(android.media.MediaPlayer mp) {
			if (null != mOnCompletionListener) {
				mOnCompletionListener.onCompletion(mMediaPlayer);
			}
		}
	};

	@Override
	public void setOnCompletionListener(
			MediaPlayer.OnCompletionListener listener) {
		mOnCompletionListener = listener;
		super.setOnCompletionListener(mSystemCompletionListener);
	}

	private MediaPlayer.OnErrorListener mOnErrorListener = null;
	private android.media.MediaPlayer.OnErrorListener mSystemErrorListener = new OnErrorListener() {
		@Override
		public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
			if (null != mOnErrorListener) {
				return mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_SYSTEM_PLAYER_COMMON_ERROR, extra);
			}
			return false;
		}
	};

	@Override
	public void setOnErrorListener(
			MediaPlayer.OnErrorListener listener) {
		mOnErrorListener = listener;
		super.setOnErrorListener(mSystemErrorListener);

	}

	private MediaPlayer.OnInfoListener mOnInfoListener = null;
	private android.media.MediaPlayer.OnInfoListener mSystemOnInfoListener = new OnInfoListener() {
		@Override
		public boolean onInfo(android.media.MediaPlayer mp, int what, int extra) {
			if (null != mOnInfoListener) {
				return mOnInfoListener.onInfo(mMediaPlayer, what, extra);
			}
			return false;
		}
	};

	@Override
	public void setOnInfoListener(
			MediaPlayer.OnInfoListener listener) {
		mOnInfoListener = listener;
		super.setOnInfoListener(mSystemOnInfoListener);
	}

	private MediaPlayer.OnPreparedListener mOnPreparedListener = null;
	private android.media.MediaPlayer.OnPreparedListener mSystemOnPreparedListener = new OnPreparedListener() {
		@Override
		public void onPrepared(android.media.MediaPlayer mp) {
			if (null != mOnPreparedListener) {
				mOnPreparedListener.onPrepared(mMediaPlayer);
			}

		}
	};

	@Override
	public void setOnPreparedListener(
			MediaPlayer.OnPreparedListener listener) {
		mOnPreparedListener = listener;
		super.setOnPreparedListener(mSystemOnPreparedListener);
	}

	private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = null;
	private android.media.MediaPlayer.OnSeekCompleteListener mSystemOnSeekCompleteListener = new OnSeekCompleteListener() {
		@Override
		public void onSeekComplete(android.media.MediaPlayer mp) {
			if (null != mOnSeekCompleteListener) {
				mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
			}
		}
	};

	@Override
	public void setOnSeekCompleteListener(
			MediaPlayer.OnSeekCompleteListener listener) {
		mOnSeekCompleteListener = listener;
		super.setOnSeekCompleteListener(mSystemOnSeekCompleteListener);
	}

	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = null;
	private android.media.MediaPlayer.OnVideoSizeChangedListener mSystemOnVideoSizeChangedListener = new OnVideoSizeChangedListener() {
		@Override
		public void onVideoSizeChanged(android.media.MediaPlayer mp, int width,
				int height) {
			if (null != mOnVideoSizeChangedListener) {
				mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer,
						width, height);
			}
		}
	};

	@Override
	public void setOnVideoSizeChangedListener(
			MediaPlayer.OnVideoSizeChangedListener listener) {
		mOnVideoSizeChangedListener = listener;
		super.setOnVideoSizeChangedListener(mSystemOnVideoSizeChangedListener);
	}

	/*private MediaPlayer.OnTimedTextListener mOnTimedTextListener = null;
	private android.media.MediaPlayer.OnTimedTextListener mSystemOnTimedTextListener = new OnTimedTextListener() {

		@Override
		public void onTimedText(android.media.MediaPlayer mp, TimedText text) {

			Log.v("SystemMediaPlayer", "onTimedText");

			if (mOnTimedTextListener != null) {
				mOnTimedTextListener.onTimedText(mMediaPlayer, text);
			}
		}
	};

	@Override
	public void setOnTimedTextListener(
			MediaPlayer.OnTimedTextListener listener) {
		mOnTimedTextListener = listener;
		super.setOnTimedTextListener(mSystemOnTimedTextListener);
	}*/
	
	@Override
	public void setScreenOnWhilePlaying(boolean screenOn) {
        super.setScreenOnWhilePlaying(screenOn);
    }
	
	@Override
	public void setWakeMode(Context context, int mode) {
		super.setWakeMode(context, mode);
	}

	@Override
	public DecodeMode getDecodeMode() {
		return DecodeMode.HW_SYSTEM;
	}
}
