package com.gotye.meetplayer.ui.widget;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.gotye.common.util.LogUtil;
import com.gotye.meetsdk.player.MediaController;

import com.gotye.meetplayer.R;

public class MicroMediaController extends MediaController {
	
	@SuppressWarnings("unused")
	private final static String TAG = "MicroMediaController";

	private Context mContext;
	
	private View mControllerView;
	private AppCompatSeekBar mProgressBar;
	private TextView mEndTime;
	private TextView mCurrentTime;
	StringBuilder mFormatBuilder;
    Formatter mFormatter;  
    
    private final int MAX_RANDGE = 1000;
    
    private boolean mIsShowing = false;
    
    private ImageButton mPauseBtn;

    // for seek
    private boolean mIsSeeking = false;
    private int mSeekStep = 0; // unit msec
    private int mSeekingPos;
    private final static int SEEK_TIMEOUT = 2000; // 2 sec

    MyHandler mHandler;
    
    public MicroMediaController(Context context) {
    	super(context);

        mContext = context;
	}
    
	public MicroMediaController(Context context, AttributeSet attr) {
		super(context, attr);

        mContext = context;
		
		setFocusable(true);
        setFocusableInTouchMode(true);
		
		mControllerView = makeControllerView();
        mHandler = new MyHandler(this);
    }
	
	@Override
	public void onFinishInflate() {
        super.onFinishInflate();

		initControllerView(mControllerView);
	}
	
	protected View makeControllerView() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.layout_micro_media_controller, this, true);
		return v;
	}
	
	private void initControllerView(View v) {
		mPauseBtn = (ImageButton) v.findViewById(R.id.player_play_pause_btn);
		if (mPauseBtn != null) {
			mPauseBtn.requestFocus();
			mPauseBtn.setOnClickListener(mPlayPauseListener);
        }
		
		mProgressBar = (AppCompatSeekBar) v.findViewById(R.id.mediacontroller_progress);
		if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(mProgressChangeListener);
			mProgressBar.setMax(MAX_RANDGE);
		}
		
		mEndTime = (TextView) v.findViewById(R.id.end_time);
		mCurrentTime = (TextView) v.findViewById(R.id.current_time);
		mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
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

    private int getPosition() {
        if (mIsSeeking)
            return mSeekingPos;

        return mPlayer.getCurrentPosition();
    }

	private int setProgress() {
		if (mPlayer == null)
			return 0;
		
		int position = getPosition();
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
    private static final int MSG_SEEK           = 4;
	
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

    private static class MyHandler extends Handler {
        WeakReference<MicroMediaController> mController;

        MyHandler(MicroMediaController instance) {
            mController = new WeakReference<MicroMediaController>(instance);
        }

        @Override
        public void handleMessage(Message msg) {
            MicroMediaController ins = mController.get();
            switch (msg.what) {
                case FADE_OUT:
                    ins.mControllerView.setVisibility(View.INVISIBLE);
                    ins.mIsShowing = false;
                    break;
                case UPDATE_PROGRESS:
                    int pos;
                    if (ins.mIsSeeking)
                        pos = 0;
                    else
                        pos = ins.setProgress();
                    // keep UI always show up
                    if (ins.isShowing() && ins.mPlayer.isPlaying()) {
                        msg = obtainMessage(UPDATE_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case MSG_SEEK:
                    if (ins.mPlayer != null)
                        ins.mPlayer.seekTo(ins.mSeekingPos);

                    ins.show();// reset hide time to 3 sec
                    ins.mIsSeeking = false;
                    break;
                default:
                    LogUtil.warn(TAG, "unknown msg: " + msg.what);
                    break;
            }
        }
    }
	
	@Override
    public void setEnabled(boolean enabled) {
        if (mPauseBtn != null) {
        	mPauseBtn.setEnabled(enabled);
        }
        if (mProgressBar != null) {
        	mProgressBar.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }
	
	private OnClickListener mPlayPauseListener = new OnClickListener() {
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
    
    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = (event.getRepeatCount() == 0 &&
                event.getAction() == KeyEvent.ACTION_DOWN);
        LogUtil.info(TAG, "keyCode: " + keyCode + " , uniqueDown: " + uniqueDown);
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
            process_seek(-1);
        	return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            process_seek(1);
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

    private void process_seek(int incr) {
        if (!mIsSeeking) {
            // show controller until seek complete
            show(0);

            mSeekingPos = mPlayer.getCurrentPosition();

            mSeekStep = 10000; // 10 sec
            if (mSeekStep > mPlayer.getDuration() / 100) {
                mSeekStep = mPlayer.getDuration() / 100 + 1000; // min seek step is 1 sec
            }
            mIsSeeking = true;
        }

        mSeekingPos += (incr * mSeekStep);
        if (mSeekingPos > mPlayer.getDuration())
            mSeekingPos = mPlayer.getDuration();
        else if (mSeekingPos < 0)
            mSeekingPos = 0;

        setProgress(mSeekingPos);

        mHandler.removeMessages(MSG_SEEK);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SEEK), SEEK_TIMEOUT);
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
