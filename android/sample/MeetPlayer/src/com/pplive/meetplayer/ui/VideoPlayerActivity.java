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
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MeetVideoView;
import android.pplive.media.player.MediaPlayer.DecodeMode;

public class VideoPlayerActivity extends Activity {

	private final static String TAG = "VideoPlayerActivity";
	
	private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};

	private Uri mUri = null;
	private DecodeMode mDecodeMode = DecodeMode.AUTO;
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
		
		Util.initMeetSDK(this);
		
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
	
	@Deprecated
	private void setupSDK() {
		MeetSDK.setLogPath(getCacheDir().getAbsolutePath() + "/meetplayer.log", 
				getCacheDir().getAbsolutePath() + "/");
		MeetSDK.initSDK(this, "");

		Util.startP2PEngine(this);
	}

	private void setupPlayer() {
		Log.i(TAG,"Step: setupPlayer()");

		mController = (MyMediaController) findViewById(R.id.video_controller);
		mVideoView = (MeetVideoView) findViewById(R.id.surface_view);
		
		mVideoView.setDecodeMode(mDecodeMode);
		mVideoView.setVideoURI(mUri);
		mController.setMediaPlayer(mVideoView);
		mVideoView.setOnCompletionListener(mCompletionListener);
		mVideoView.setOnErrorListener(mErrorListener);
		mVideoView.setOnInfoListener(mInfoListener);
		mVideoView.setOnPreparedListener(mPreparedListener);
		
		String schema = mUri.getScheme();
		String path = null;
		
		if ("file".equalsIgnoreCase(schema))
			path = mUri.getPath();
		else
			path = mUri.toString();
		
		String name = getFileName(path);
		mController.setFileName(name);
		
		mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		
		mVideoView.start();
	}

	private String getFileName(String path) {
		String name = "N/A";
		if (path.startsWith("/") || path.startsWith("file://")) {
			int pos = path.lastIndexOf('/');
			if (pos != -1)
				name = path.substring(pos + 1, path.length());
			
			if (name.length() > 16)
				name = name.substring(0, 16) + "...";
		}
		else if(path.startsWith("http://")) {
			int pos1, pos2;
			String tmp;
			pos1 = path.indexOf("?");
			if (pos1 == -1)
				name = path;
			else {
				tmp = path.substring(0, pos1);
				pos2 = tmp.lastIndexOf('/');
				if (pos2 == -1)
					name = path;
				else
					name = tmp.substring(pos2 + 1, tmp.length());
			}
			
			int pos3;
			pos3 = path.indexOf("playlink=");
			if (pos3 != -1) {
				String link = path.substring(pos3 + 9, path.indexOf("%3F", pos3));
				name += ", link " + link;
			}
			
			int pos4;
			pos4 = path.indexOf("Fft%3D");
			if (pos4 != -1) {
				String link = path.substring(pos4 + 6, pos4 + 7);
				name += ", ft " + link;
			}
		}
		
		return name;
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
			switchDisplayMode(1);
			return true;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			Log.i(TAG, "onLongPress!!!");
			if (mVideoView != null) {
				mVideoView.stopPlayback();
				
				mVideoView.setVideoURI(mUri);
				mVideoView.start();
			}
		}
	});
	
	private void switchDisplayMode(int incr) {
		if (mVideoView != null) {
			int mode = mVideoView.getDisplayMode();
			mode = mode + incr;
			if (mode < 0)
				mode = 3;
			else if(mode > 3)
				mode = 0;
			mVideoView.setDisplayMode(mode);
			Toast.makeText(this, "mode switch to " + mode_desc[mode], Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.i(TAG, "onTouchEvent()" + event.toString());
		
		return mDoubleTapListener.onTouchEvent(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		Log.d(TAG, "keyCode: " + keyCode);
		int incr = -1;
		int mode;
		
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
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
				
				return true;
			case KeyEvent.KEYCODE_ENTER: // 66
				if (mVideoView.isPlaying()) {
					mVideoView.pause();
					mController.show(10000000);
				}
				else {
					mVideoView.start();
					mController.show();
				}
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_UP:
				if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode)
					incr = 1;
				else
					incr = -1;
				
				switchDisplayMode(incr);
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
