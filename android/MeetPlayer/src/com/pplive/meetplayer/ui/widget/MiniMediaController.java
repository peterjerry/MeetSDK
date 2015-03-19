package com.pplive.meetplayer.ui.widget;

import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.pplive.media.player.MediaController;

import com.pplive.meetplayer.R;

public class MiniMediaController extends MediaController {
	
	@SuppressWarnings("unused")
	private final static String TAG = "MiniMediaController";

	private Context mContext;
	private boolean mIsLand = false; // 是否是横屏
	
	private View mControllerView;
	private SeekBar mProgressBar;
	private TextView mEndTime;
	private TextView mCurrentTime;
	private TextView mFileName;
	StringBuilder mFormatBuilder;
    Formatter mFormatter;  
    
    private final int MAX_RANDGE = 1000;
    
    private boolean mVolumerDragging = false;
    private boolean mIsShowing = false;
    
    private ImageButton mPlayPauseBtn;
    private ImageButton mFwdBtn;
    private ImageButton mBwdBtn;
    private ImageButton	 mFullScreenBtn;
    
    private Activity mInstance;
    
    public void setInstance(Activity ins) {
    	mInstance = ins;
    }
    
    public MiniMediaController(Context context) {
    	super(context);
	}
    
	public MiniMediaController(Context context, AttributeSet attr) {
		super(context, attr);
		
		mControllerView = makeControllerView();
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
		View v = inflater.inflate(R.layout.layout_mini_media_controller, this, true);
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
        
        mFileName = (TextView) v.findViewById(R.id.textview_filename);
  
        this.setOnTouchListener(mTouchListener);
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
		if(mPlayer == null) {
			Log.e(TAG, "aaaaa setProgress() player is null");
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
	
	public void show(int timeout) {
		mHandler.sendEmptyMessage(SHOW);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
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
                case SHOW:
                	mControllerView.setVisibility(View.VISIBLE);
                	mHandler.sendEmptyMessage(UPDATE_PROGRESS);
                	mIsShowing = true;
                    break;
               case UPDATE_PROGRESS:
                   pos = setProgress();
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
    
    private void updateFullScreen() {
    	if (mFullScreenBtn == null)
            return;
    	
    	if (mIsLand) {
            mFullScreenBtn.setImageResource(R.drawable.player_window_btn);
        } else {
        	mFullScreenBtn.setImageResource(R.drawable.player_fullscreen_btn);
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
    
    
    
    private View.OnClickListener mFullScreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (null != mInstance) {
            	if (mIsLand)
            		mInstance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            	else
            		mInstance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            	
            	mIsLand = !mIsLand;
            	
            	updateFullScreen();
            }
            
        }
    };
    
    private GestureDetector mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
    	
    	public boolean onDoubleTap(MotionEvent e) {
//    		Log.d(TAG, "onDoubleTap!!!");
    		
    		if (mPlayer instanceof MediaPlayerControl) {
    			((MediaPlayerControl) mPlayer).switchDisplayMode();
    		}
    		
    		return false;
    	};
    	
    	public boolean onSingleTapConfirmed(MotionEvent e) {
//    		Log.d(TAG, "onSingleTapConfirmed!!!");
    		
    		
    		return false;
    	};
    });
    
    private OnTouchListener mTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			return mGestureDetector.onTouchEvent(event);
		}
	};
	
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