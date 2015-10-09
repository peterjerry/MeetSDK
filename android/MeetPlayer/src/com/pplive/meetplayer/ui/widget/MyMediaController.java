package com.pplive.meetplayer.ui.widget;

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.pplive.media.player.MediaController;

import com.pplive.meetplayer.R;

public class MyMediaController extends MediaController {
	
	@SuppressWarnings("unused")
	private final static String TAG = "MyMediaController";
	
	private AudioManager mAudioManager;
	
	private Context mContext;
	private View mControllerView;
	private SeekBar mProgressBar;
	private VerticalSeekBar mVolumeBar;
	private TextView mEndTime;
	private TextView mCurrentTime;
	private TextView mFileName;
	StringBuilder mFormatBuilder;
    Formatter mFormatter;  
    
    private final int MAX_RANGE = 1000;
    
    private boolean mIsLivePlay = false;
    
    private boolean mVolumerDragging = false;
    private int mVolumeProgress;
    private boolean mIsShowing = false;
    
    private ImageButton mPlayPauseBtn;
    private ImageButton mFwdBtn;
    private ImageButton mBwdBtn;

    public MyMediaController(Context context) {
    	super(context);
    	
    	mContext = context;
	}
    
	public MyMediaController(Context context, AttributeSet attr) {
		super(context, attr);
		
		mContext = context;
		mControllerView = makeControllerView();
	}
	
	public void setLivePlay(boolean isLive) {
		mIsLivePlay = isLive;
	}
	
	public void setFileName(String name) {
		if (name != null)
			mFileName.setText(name);
	}
	
	@Override
	public void onFinishInflate() {
		initControllerView(mControllerView);
	}
	
	protected View makeControllerView() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.layout_media_controller, this, true);
		return v;
	}
	
	private void initControllerView(View v) {
		mPlayPauseBtn = (ImageButton) v.findViewById(R.id.player_play_pause_btn);
		if (mPlayPauseBtn != null) {
			mPlayPauseBtn.requestFocus();
			mPlayPauseBtn.setOnClickListener(mPlayPauseListener);
        }
		
		mBwdBtn = (ImageButton) v.findViewById(R.id.player_bf_btn);
		if (mBwdBtn != null) {
			mBwdBtn.setOnClickListener(mBwdListener);
        }
		
		mFwdBtn = (ImageButton) v.findViewById(R.id.player_ff_btn);
		if (mFwdBtn != null) {
			mFwdBtn.setOnClickListener(mFwdListener);
        }
		
		mProgressBar = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
		mProgressBar.setOnSeekBarChangeListener(mProgressChangeListener);
		mProgressBar.setMax(MAX_RANGE);
		
		mVolumeBar = (VerticalSeekBar) v.findViewById(R.id.mediacontroller_volume);
		mVolumeBar.setOnSeekBarChangeListener(mVolumeChangeListener);
		mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);			
		mVolumeBar.setMax(MAX_RANGE);
		
		mEndTime = (TextView) v.findViewById(R.id.end_time);
		mCurrentTime = (TextView) v.findViewById(R.id.current_time);
		mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        
        mFileName = (TextView) v.findViewById(R.id.textview_filename);
	}
	
	private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / MAX_RANGE;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
	
	private int setProgress() {
		if (mPlayer == null) {
			return 0;
		}
	
		int position = mPlayer.getCurrentPosition();
		return setProgress(position);
	}
	
	private int setProgress(int position) {
		if (mPlayer == null) {
			return 0;
		}
		
		int duration = mPlayer.getDuration();
		
		if (mProgressBar != null) {
			if (duration > 0) {
				long pos = (long)MAX_RANGE * position / duration;
				mProgressBar.setProgress((int)pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgressBar.setSecondaryProgress(percent * MAX_RANGE / 100);
		}
		
		if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
		}
		
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }
		
		return position;
	}
	
	private void setVolume(int volume) {
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0 /* flags */);
	}
	
	private void updateVolumeProgress() {
		if (mAudioManager == null)
			return;
		
		int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		if (mVolumeBar != null && !mVolumerDragging) {
			if (maxVolume > 0) {
				int pos = volume * MAX_RANGE / maxVolume;
				Log.i(TAG, String.format("Java: volume %d/%d, pos %d", volume, maxVolume, pos));
				mVolumeBar.setProgressAndThumb(pos);
			}
		}
	}
	
	private static final int sDefaultTimeout 	= 3000; // 3 sec
	
    private static final int FADE_OUT 			= 1;
    private static final int SHOW 				= 2;
    private static final int UPDATE_PROGRESS 	= 3;
	
    public boolean isShowing() {
    	return mIsShowing;
    }
    
    public void hide() {
    	mHandler.sendEmptyMessage(FADE_OUT);
    }
    
	public void show() {
		this.show(sDefaultTimeout);
	}
	
	public void show(int timeout) {
		mHandler.sendEmptyMessage(SHOW);

		mHandler.removeMessages(FADE_OUT);
        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.sendMessageDelayed(msg, timeout);
        }
	}
	
	private Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
            //Log.d(TAG, "handleMessage: " + msg.what);
            
            switch (msg.what) {
                case FADE_OUT:
                	mControllerView.setVisibility(View.INVISIBLE);
                	mIsShowing = false;
                    break;
                case SHOW:
                	updateVolumeProgress();
                    setProgress();
                	mControllerView.setVisibility(View.VISIBLE);
                	mPlayPauseBtn.requestFocus();
                	mHandler.sendEmptyMessage(UPDATE_PROGRESS);
                	mIsShowing = true;
                    break;
               case UPDATE_PROGRESS:
            	   //updateVolumeProgress();
                   setProgress();
                   // keep UI always show up
                   if (isShowing() && mPlayer.isPlaying()) {
                       msg = obtainMessage(UPDATE_PROGRESS);
                       sendMessageDelayed(msg, 500);
                   }
            	   break;
               default:
            	   Log.w(TAG, "unknown msg: " + msg.what);
            	   break;
            }
        }
	};
	
	private View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            if (mPlayer.isPlaying())
            	show(sDefaultTimeout);
            else
            	show(10000000); // never hide!
        }
    };
    
    @Override
    protected void doPauseResume() {
    	super.doPauseResume();
    	
    	updatePausePlay();
    }
    
    private void updatePausePlay() {
        if (mPlayPauseBtn == null)
            return;

        if (mPlayer.isPlaying()) {
            mPlayPauseBtn.setImageResource(R.drawable.player_pause_btn);
        } else {
        	mPlayPauseBtn.setImageResource(R.drawable.player_play_btn);
        }
    }
    
    private View.OnClickListener mBwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            int pos = mPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private View.OnClickListener mFwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            int pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };
	
	private SeekBar.OnSeekBarChangeListener mProgressChangeListener = new SeekBar.OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			
			int progress = seekBar.getProgress();
			if (mPlayer != null) {
				if (mIsLivePlay) {
					int new_duration = mPlayer.getDuration();
					int offset = new_duration - 1800 * 1000;
					int duration = 1800 * 1000;
					int position = duration / 1000 * progress;
					position += offset;
					if (position > new_duration)
						position = new_duration;
					if (position < offset)
						position = offset;
					mPlayer.seekTo(position);
				}
				else {
					int duration = mPlayer.getDuration();
					if (duration > 0) {
						long position = (duration / 1000L) * progress;
						
						int pos = setProgress((int)position);
						mPlayer.seekTo(pos);
					}
				}
			}
			
			show(sDefaultTimeout);
			
			mHandler.sendEmptyMessage(UPDATE_PROGRESS);
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			show(3600000);
			
			mHandler.removeMessages(UPDATE_PROGRESS);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				if (mPlayer != null) {
					int duration = mPlayer.getDuration();
					if (duration > 0) {
						long position = (duration / 1000L) * progress;
						setProgress((int)position);
					}
				}
			}
		}
	};
	
	private OnSeekBarChangeListener mVolumeChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			Log.i(TAG, "Java: onStopTrackingTouch()");
			
			show(sDefaultTimeout);
			mVolumerDragging = false;
			
			int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int volume = maxVolume * mVolumeProgress / 1000;
			
			Log.i(TAG, String.format("Java: setVolume %d.%d", volume, maxVolume));
			setVolume(volume);
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			Log.i(TAG, "Java: onStartTrackingTouch()");
			
			show(3600000);
			mVolumerDragging = true;
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			Log.d(TAG, String.format("Java: onProgressChanged() %d, fromUser: %s", progress, fromUser?"yes":"no"));
			if (fromUser)
				mVolumeProgress = progress;
		}
	};
	
	public interface MediaPlayerControl extends android.widget.MediaController.MediaPlayerControl {
		void switchDisplayMode();
	}
}
