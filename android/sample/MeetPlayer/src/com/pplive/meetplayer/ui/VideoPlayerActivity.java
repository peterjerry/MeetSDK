package com.pplive.meetplayer.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pplive.meetplayer.R;
import com.pplive.meetplayer.ui.widget.MyMediaController;
import com.pplive.sdk.MediaSDK;

import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MeetVideoView;
import android.pplive.media.player.MediaPlayer.DecodeMode;

public class VideoPlayerActivity extends Activity {

	private static final String TAG = "VideoPlayerActivity";

	private Uri mUri = null;
	private DecodeMode mDecodeMode;
	private MeetVideoView mVideoView = null;
	private MyMediaController mController;
	private ProgressBar mBufferingProgressBar = null;
	
	private boolean mIsBuffering = false;

	private ProgressBar mDownloadProgressBar = null;
	private TextView mProgressTextView = null;
	private Dialog mUpdateDialog = null;
	
	private boolean needUpdate = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Java: onCreate()");
		
		Intent intent = getIntent();
		mUri = intent.getData();
		int impl = intent.getIntExtra("impl", 0);
		Log.i(TAG, String.format("Java player impl: %d", impl));
		
		switch(impl) {
		case 0:
			mDecodeMode = DecodeMode.AUTO;
			break;
		case 1:
			mDecodeMode = DecodeMode.HW_SYSTEM;
			break;
		case 3:
			mDecodeMode = DecodeMode.SW;
			break;
		default:
			Log.w(TAG, String.format("Java: unknown DecodeMode: %d", impl));
			mDecodeMode = DecodeMode.SW;
			break;
		}
		Log.i(TAG, "Java: mUri " + mUri.toString());

		setContentView(R.layout.activity_video_player);
		
		setupSDK();
		
	}

	@Override
	protected void onStart() {
		super.onStart();

		Log.i(TAG, "Java: onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "Java: onResume");
		
		setupPlayer();
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Java: onPause()");

		mVideoView.pause();
	}

	@Override
	protected void onStop() {
		super.onStop();

		Log.i(TAG, "Java: onStop()");

		mVideoView.stopPlayback();
	}
	
	private void setupSDK() {
		MeetSDK.setAppRootDir(getCacheDir().getParentFile().getAbsolutePath() + "/");
		if (android.os.Build.CPU_ABI == "x86")
			MeetSDK.setPPBoxLibName("libppbox-android-x86-gcc44-mt-1.1.0.so");
    	else
    		MeetSDK.setPPBoxLibName("libppbox-armandroid-r4-gcc44-mt-1.1.0.so");
		MeetSDK.setLogPath(getCacheDir().getAbsolutePath() + "/meetplayer.log", getCacheDir().getAbsolutePath() + "/");
		
		MeetSDK.initSDK(this, "");
		
		String gid = "13";//12
		String pid = "162";//161
		String auth = "08ae1acd062ea3ab65924e07717d5994";

		File cacheDirFile	= getCacheDir();
		String dataDir		= cacheDirFile.getParentFile().getAbsolutePath();
		String libDir		= dataDir + "/lib";
		String logDir		= cacheDirFile.getAbsolutePath(); //Environment.getExternalStorageDirectory().getPath() + "/pptv";
		
		MediaSDK.libPath = libDir;
		MediaSDK.logPath = logDir;
		MediaSDK.logOn = false;
		MediaSDK.setConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
		MediaSDK.setConfig("", "RtspManager", "addr", "127.0.0.1:5156+");
		
		MediaSDK.startP2PEngine(gid, pid, auth);
	}

	private void setupPlayer() {
		Log.i(TAG,"Step: setupPlayer()");

		mController = (MyMediaController) findViewById(R.id.video_controller);
		mVideoView = (MeetVideoView) findViewById(R.id.surface_view);
		
		mVideoView.setVisibility(View.INVISIBLE);
		
		mVideoView.setDecodeMode(mDecodeMode);
		mVideoView.setVideoURI(mUri);
		mController.setMediaPlayer(mVideoView);
		mVideoView.setOnCompletionListener(mCompletionListener);
		mVideoView.setOnErrorListener(mErrorListener);
		mVideoView.setOnInfoListener(mInfoListener);
		mVideoView.setOnPreparedListener(mPreparedListener);

		mVideoView.setVisibility(View.VISIBLE);
		
		mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		if(mBufferingProgressBar == null)
			Log.e(TAG, "mBufferingProgressBar is null");
		
		mVideoView.start();
	}

	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			Log.d(TAG, "MEDIA_PLAYBACK_COMPLETE");
			mVideoView.stopPlayback();
			finish();
		}
	};

	private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
			Log.d(TAG, "Error: " + framework_err + "," + impl_err);
			mVideoView.stopPlayback();
			finish();
			return true;
		}
	};
	
	private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
		
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.d(TAG, "Java: onInfo: " + what + " " + extra);
			
			if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_START) && !mIsBuffering) {
				mBufferingProgressBar.setVisibility(View.VISIBLE);
				mIsBuffering = true;
			} else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
				mBufferingProgressBar.setVisibility(View.GONE);
				mIsBuffering = false;
			} else if (MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE == what) {
				String str_player_type;
				if (MediaPlayer.PLAYER_IMPL_TYPE_SYSTEM_PLAYER == extra)
					str_player_type = "System Player";
				else if(MediaPlayer.PLAYER_IMPL_TYPE_NU_PLAYER == extra)
					str_player_type = "Nu Player";
				else if(MediaPlayer.PLAYER_IMPL_TYPE_FF_PLAYER == extra)
					str_player_type = "FF Player";
				else if(MediaPlayer.PLAYER_IMPL_TYPE_PP_PLAYER == extra)
					str_player_type = "PP Player";
				else
					str_player_type = "Unknown Player";
				Toast.makeText(VideoPlayerActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
			}
			
			return true;
		}
	};
	
	private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer arg0) {
			Log.i(TAG, "Java: OnPrepared");
			mController.show();
		}
	};
	
	// UI
	private GestureDetector mDoubleTapListener = 
			new GestureDetector(getApplication(), new GestureDetector.SimpleOnGestureListener() {
				
		public boolean onSingleTapConfirmed(MotionEvent e) {
			
			Log.i(TAG, "onSingleTapConfirmed!!!");
			toggleMediaControlsVisiblity();
			
			return false;
		};
		
		@Override
		public boolean onDoubleTap(MotionEvent event) {
			
			Log.i(TAG, "onDoubleTap!!!");
			if (mVideoView != null)
				mVideoView.switchDisplayMode();
			
			return true;
		}
		
	});
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.i(TAG, "onTouchEvent()" + event.toString());
		
		return mDoubleTapListener.onTouchEvent(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		Log.d(TAG, "keyCode: " + keyCode);
		
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				mController.show();
				
				if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
					int pos = mVideoView.getCurrentPosition();
					pos += 30000;
					if(pos > mVideoView.getDuration())
						pos = mVideoView.getDuration();
					mVideoView.seekTo(pos);
				}
				else if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
					int pos = mVideoView.getCurrentPosition();
					pos -= 30000;
					if(pos < 0)
						pos = 0;
					mVideoView.seekTo(pos);
				}
				else if (KeyEvent.KEYCODE_DPAD_CENTER == keyCode) {
					if (mVideoView.isPlaying())
						mVideoView.pause();
					else
						mVideoView.resume();
				}
				return true;
			default:
				return super.onKeyDown(keyCode, event);
			}
	}
	
    public void toggleMediaControlsVisiblity() {
    	if (mVideoView != null && mVideoView.isPlaying() && mController != null) {
	        if (mController.isShowing()) {
	        	mController.hide();
	        } else {
	        	mController.show();
	        }
    	}
    }
	
}
