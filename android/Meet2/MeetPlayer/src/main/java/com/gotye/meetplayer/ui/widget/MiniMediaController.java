package com.gotye.meetplayer.ui.widget;

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.gotye.meetsdk.player.MediaController;

import com.gotye.meetplayer.R;

public class MiniMediaController extends MediaController {
	
	@SuppressWarnings("unused")
	private final static String TAG = "MiniMediaController";

	private Context mContext;
	private boolean mIsLandscape = false; // 是否是横屏
	
	private View mControllerView;
	private SeekBar mProgressBar;
	private TextView mEndTime;
	private TextView mCurrentTime;
	private TextView mPlayerImpl;
	StringBuilder mFormatBuilder;
    Formatter mFormatter;  
    
    private final int MAX_RANDGE = 1000;
    
    private boolean mVolumerDragging = false;
    private boolean mIsShowing = false;
    
    private ImageButton mPauseBtn;
    private ImageButton mForwardBtn;
    private ImageButton mRewindBtn;
    private ImageButton	 mFullScreenBtn;
    
    private AppCompatActivity mInstance;
    
    public void setInstance(AppCompatActivity ins) {
    	mInstance = ins;
    }
    
    public MiniMediaController(Context context) {
    	super(context);
	}
    
	public MiniMediaController(Context context, AttributeSet attr) {
		super(context, attr);
		
		setFocusable(true);
        setFocusableInTouchMode(true);
		
		mControllerView = makeControllerView();
	}
	
	public void setPlayerImplement(String impl) {
		if (impl != null)
			mPlayerImpl.setText(impl);
	}
	
	public void updateLandscape(boolean isLandscape) {
		Log.i(TAG, "Java: updateLandscape " + isLandscape);
		
		if (mIsLandscape != isLandscape) {
			mIsLandscape = isLandscape;
			updateFullScreen();
		}
	}
	
	@Override
	public void onFinishInflate() {
        super.onFinishInflate();

		initControllerView(mControllerView);
	}
	
	protected View makeControllerView() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.layout_mini_media_controller, this, true);
		return v;
	}
	
	private void initControllerView(View v) {
		mPauseBtn = (ImageButton) v.findViewById(R.id.player_play_pause_btn);
		if (mPauseBtn != null) {
			mPauseBtn.requestFocus();
			mPauseBtn.setOnClickListener(mPlayPauseListener);
        }
		
		mRewindBtn = (ImageButton) v.findViewById(R.id.player_bf_btn);
		if (mRewindBtn != null) {
			mRewindBtn.setOnClickListener(mBwdListener);
        }
		
		mForwardBtn = (ImageButton) v.findViewById(R.id.player_ff_btn);
		if (mForwardBtn != null) {
			mForwardBtn.setOnClickListener(mFwdListener);
        }
		
		mFullScreenBtn = (ImageButton) v.findViewById(R.id.player_fullscreen_btn);
		if (mFullScreenBtn != null) {
			mFullScreenBtn.setOnClickListener(mFullScreenListener);
        }
		
		mProgressBar = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
		if (mProgressBar != null) {
			if (mProgressBar instanceof SeekBar) {
				SeekBar seeker = (SeekBar) mProgressBar;
				seeker.setOnSeekBarChangeListener(mProgressChangeListener);
			}
			mProgressBar.setMax(MAX_RANDGE);
		}
		
		mEndTime = (TextView) v.findViewById(R.id.end_time);
		mCurrentTime = (TextView) v.findViewById(R.id.current_time);
		mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        
        mPlayerImpl = (TextView) v.findViewById(R.id.tv_player_impl);
	}
	
	private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / MAX_RANDGE;

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
		if (mPlayer == null)
			return 0;
		
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
				long pos = (long)MAX_RANDGE * position / duration;
				mProgressBar.setProgress((int)pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgressBar.setSecondaryProgress(percent * 10);
		}
		
		if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
		}
		
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }
		
		return position;
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
	
	/**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPauseBtn != null && !mPlayer.canPause()) {
            	mPauseBtn.setEnabled(false);
            }
            if (mRewindBtn != null && !mPlayer.canSeekBackward()) {
                mRewindBtn.setEnabled(false);
            }
            if (mForwardBtn != null && !mPlayer.canSeekForward()) {
            	mForwardBtn.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
	
	/**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
        if (!mIsShowing) {
            setProgress();
            if (mPauseBtn != null) {
            	mPauseBtn.requestFocus();
            }
            disableUnsupportedButtons();
            mControllerView.setVisibility(View.VISIBLE);
            mIsShowing = true;
        }
        updatePausePlay();
        
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(UPDATE_PROGRESS);

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
            
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                	mControllerView.setVisibility(View.INVISIBLE);
                	mIsShowing = false;
                    break;
                /*case SHOW:
                	mControllerView.setVisibility(View.VISIBLE);
                	mHandler.sendEmptyMessage(UPDATE_PROGRESS);
                	mIsShowing = true;
                    break;*/
               case UPDATE_PROGRESS:
                   pos = setProgress();
                   // keep UI always show up
                   if (isShowing() && mPlayer.isPlaying()) {
                       msg = obtainMessage(UPDATE_PROGRESS);
                       sendMessageDelayed(msg, 1000 - (pos % 1000));
                   }
            	   break;
               default:
            	   Log.w(TAG, "unknown msg: " + msg.what);
            	   break;
            }
        }
	};
	
	@Override
    public void setEnabled(boolean enabled) {
        if (mPauseBtn != null) {
        	mPauseBtn.setEnabled(enabled);
        }
        if (mForwardBtn != null) {
            mForwardBtn.setEnabled(enabled);
        }
        if (mRewindBtn != null) {
        	mRewindBtn.setEnabled(enabled);
        }
        if (mProgressBar != null) {
        	mProgressBar.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }
	
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
        if (mPauseBtn == null)
            return;

        if (mPlayer.isPlaying()) {
            mPauseBtn.setImageResource(R.drawable.player_pause_btn);
        } else {
        	mPauseBtn.setImageResource(R.drawable.player_play_btn);
        }
    }
    
    private void updateFullScreen() {
    	if (mIsLandscape) {
            mFullScreenBtn.setImageResource(R.drawable.player_window_btn);
        } else {
        	mFullScreenBtn.setImageResource(R.drawable.player_fullscreen_btn);
        }
    }
    
    private View.OnClickListener mBwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            int pos = mPlayer.getCurrentPosition();
            pos -= 15000; // milliseconds
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
    
    
    
    private View.OnClickListener mFullScreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (null != mInstance) {
            	if (mIsLandscape) {
            		mInstance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            	}
            	else {
                    // toggle to full screen play mode
                    mInstance.getSupportActionBar().hide();
            		mInstance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            	}
            	
            	mIsLandscape = !mIsLandscape;
            	
            	updateFullScreen();
            }
            
        }
    };
    
    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseBtn != null) {
                	mPauseBtn.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        	int pos = mPlayer.getCurrentPosition();
        	pos -= 30000;
        	if (pos < 0)
        		pos = 0;
        	mPlayer.seekTo(pos);
        	if (mRewindBtn != null) {
        		mRewindBtn.requestFocus();
            }
        	return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        	int pos = mPlayer.getCurrentPosition();
        	pos += 30000;
        	if (pos > mPlayer.getDuration())
        		pos = mPlayer.getDuration();
        	mPlayer.seekTo(pos);
        	if (mForwardBtn != null) {
        		mForwardBtn.requestFocus();
            }
        	return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }
	
	private OnSeekBarChangeListener mProgressChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			
			int progress = seekBar.getProgress();
			if (mPlayer != null) {
				int duration = mPlayer.getDuration();
				if (duration > 0) {
					long position = (duration / 1000L) * progress;
					
					int pos = setProgress((int)position);
					mPlayer.seekTo(pos);
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
	
	public interface MediaPlayerControl extends android.widget.MediaController.MediaPlayerControl {
		void switchDisplayMode();
	}
}
