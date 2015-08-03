package com.pplive.meetplayer.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pplive.meetplayer.R;
import com.pplive.meetplayer.ui.widget.MyMediaController;
import com.pplive.meetplayer.util.FileFilterTest;
import com.pplive.meetplayer.util.Util;

import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MeetVideoView;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.subtitle.SimpleSubTitleParser;
import android.pplive.media.subtitle.SubTitleSegment;
import android.pplive.media.subtitle.SubTitleParser.Callback;

public class VideoPlayerActivity extends Activity implements Callback {

	private final static String TAG = "VideoPlayerActivity";
	
	private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};

	protected MeetVideoView mVideoView = null;
	protected MyMediaController mController;
	private ProgressBar mBufferingProgressBar = null;
	
	protected Uri mUri = null;
	protected int mPlayerImpl = 0;
	protected int mFt = 0;
	protected int mBestFt = 3;
	protected String mTitle;
	protected int pre_seek_msec = -1;
	
	private boolean mIsBuffering = false;
	
	/* 记录上一次按返回键的时间 */
    private long backKeyTime = 0L;
    
    // debug info
 	private TextView mTextViewDebugInfo;
 	private boolean mbShowDebugInfo = false;
 	
 	private int decode_fps						= 0;
	private int render_fps 					= 0;
	private int decode_avg_msec 				= 0;
	private int render_avg_msec 				= 0;
	private int render_frame_num				= 0;
	private int decode_drop_frame				= 0;
	private int av_latency_msec				= 0;
	private int video_bitrate					= 0;
	
	// subtitle
	private SimpleSubTitleParser mSubtitleParser;
	private TextView mSubtitleTextView;
	private String mSubtitleText;
	private Thread mSubtitleThread;
	private boolean mSubtitleSeeking = false;
	private boolean mIsSubtitleUsed = false;
	private String subtitle_filename;
	private boolean mSubtitleStoped = false;
	
	// message
	private static final int MSG_DISPLAY_SUBTITLE					= 401;
	private static final int MSG_HIDE_SUBTITLE					= 402;
	private static final int MSG_UPDATE_PLAY_INFO 				= 403;
	private static final int MSG_UPDATE_RENDER_INFO				= 404;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Java: onCreate()");
		
		Intent intent = getIntent();
		mUri = intent.getData();
		Log.i(TAG, "Java: mUri " + mUri.toString());
		
		if (intent.hasExtra("impl"))
			mPlayerImpl = intent.getIntExtra("impl", 0);
		else
			mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
		Log.i(TAG, "Java player impl: " + mPlayerImpl);
		
		mTitle = intent.getStringExtra("title");
		mFt = intent.getIntExtra("ft", 0);
		mBestFt = intent.getIntExtra("best_ft", 3);
		
		setContentView(R.layout.activity_video_player);
		
		this.mController = (MyMediaController) findViewById(R.id.video_controller);
		this.mVideoView = (MeetVideoView) findViewById(R.id.video_view);
		this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
		this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		this.mTextViewDebugInfo = (TextView) findViewById(R.id.tv_debuginfo);
		
		this.mTextViewDebugInfo.setTextColor(Color.RED);
		this.mTextViewDebugInfo.setTextSize(18);
		this.mTextViewDebugInfo.setTypeface(Typeface.MONOSPACE);
		
		this.mController.setMediaPlayer(mVideoView);
		
		Util.initMeetSDK(this);
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {  
        MenuInflater menuInflater = new MenuInflater(getApplication());  
        menuInflater.inflate(R.menu.videoplayer_menu, menu);  
        return super.onCreateOptionsMenu(menu);  
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Log.i(TAG, "Java: onOptionsItemSelected " + id);
		
		switch (id) {
		case R.id.select_player_impl:
			popupSelectPlayerImpl();
			break;
		case R.id.select_ft:
			String path;
			String scheme = mUri.getScheme();
			if ("file".equalsIgnoreCase(scheme))
				path = mUri.getPath();
			else
				path = mUri.toString();
			
			if (path.contains("&playlink="))
				popupSelectFT();
			break;
		case R.id.select_subtitle:
			popupSelectSubtitle();
			break;
		case R.id.toggle_debug_info:
			mbShowDebugInfo = !mbShowDebugInfo;
			if (mbShowDebugInfo)
				mTextViewDebugInfo.setVisibility(View.VISIBLE);
			else
				mTextViewDebugInfo.setVisibility(View.GONE);
			break;
		default:
			Log.w(TAG, "unknown menu id " + id);
			break;
		}
		
		return true;
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
		stop_subtitle();
	}

	private void popupSelectFT() {
		final String[] ft_desc = {"流畅", "高清", "超清", "蓝光"};
		
		Dialog choose_ft_dlg = new AlertDialog.Builder(VideoPlayerActivity.this)
		.setTitle("select ft")
		.setSingleChoiceItems(ft_desc, mFt,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton){
					if (whichButton == mFt) {
						dialog.dismiss();
						return;
					}
					else {
						if (whichButton > mBestFt) {
							Toast.makeText(VideoPlayerActivity.this, 
									"该码率: " + ft_desc[whichButton] + " 无效", 
									Toast.LENGTH_SHORT).show();
						}
						else {
							Toast.makeText(VideoPlayerActivity.this, 
								"选择码率: " + ft_desc[whichButton], Toast.LENGTH_SHORT).show();
							
							mFt = whichButton;
							
							String old_url = mUri.toString();
							int pos = old_url.indexOf("%3Fft%3D");
							if (pos != -1) {
								String old_ft = old_url.substring(pos, pos + "%3Fft%3D".length() + 1);
								String new_ft = "%3Fft%3D" + mFt;
								mUri = Uri.parse(old_url.replace(old_ft, new_ft));
								
								pre_seek_msec = mVideoView.getCurrentPosition() - 5000;
								if (pre_seek_msec < 0)
									pre_seek_msec = 0;
								
								setupPlayer();
							}
							
							dialog.dismiss();
						}
					}
				}
			})
		.create();
		choose_ft_dlg.show();
	}
	
	private void popupSelectPlayerImpl() {
		final String[] player_desc = {"Auto", "System", "XOPlayer", "FFPlayer"};
		
		Dialog choose_player_impl_dlg = new AlertDialog.Builder(VideoPlayerActivity.this)
		.setTitle("select player impl")
		.setSingleChoiceItems(player_desc, mPlayerImpl, /*default selection item number*/
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					Log.i(TAG, "select player impl: " + whichButton);
					
					if (mPlayerImpl != whichButton) {
						mPlayerImpl = whichButton;
						Util.writeSettingsInt(VideoPlayerActivity.this, "PlayerImpl", mPlayerImpl);
						Toast.makeText(VideoPlayerActivity.this, 
								"select type: " + player_desc[whichButton], Toast.LENGTH_SHORT).show();

						pre_seek_msec = mVideoView.getCurrentPosition() - 5000;
						if (pre_seek_msec < 0)
							pre_seek_msec = 0;
						
						setupPlayer();
					}
					
					dialog.dismiss();
				}
			})
		.create();
		choose_player_impl_dlg.show();
	}
	
	private void popupSelectSubtitle() {
		final String sub_folder = Environment.getExternalStorageDirectory().getAbsolutePath() + 
				"/test2/subtitle";
		
		File file = new File(sub_folder);
		String []list = {"srt", "ass"};
		File [] subtitle_files = file.listFiles(new FileFilterTest(list));
		if (subtitle_files == null) {
			Toast.makeText(this, "no subtitle file found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> filename_list = new ArrayList<String>();
		for (int i=0;i<subtitle_files.length;i++) {
			filename_list.add(subtitle_files[i].getName());
		}
		final String[] str_file_list = (String[])filename_list.toArray(new String[filename_list.size()]);
		
		Dialog choose_subtitle_dlg = new AlertDialog.Builder(VideoPlayerActivity.this)
		.setTitle("select subtitle")
		.setItems(str_file_list, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					stop_subtitle();
					
					subtitle_filename = sub_folder + "/" + str_file_list[whichButton];
					Log.i(TAG, "Load subtitle file: " + subtitle_filename);
					Toast.makeText(VideoPlayerActivity.this, 
							"Load subtitle file: " + subtitle_filename, Toast.LENGTH_SHORT).show();
					start_subtitle(subtitle_filename);
				}
			})
		.create();
		choose_subtitle_dlg.show();
	}
	
	private boolean start_subtitle(String filename) {
		Log.i(TAG, "Java: subtitle start_subtitle " + filename);
		
		mSubtitleParser = new SimpleSubTitleParser();
		mSubtitleParser.setOnPreparedListener(this);
		
		mSubtitleParser.setDataSource(filename);
		mSubtitleParser.prepareAsync();
		
		return true;
	}
	
	private void stop_subtitle() {
		Log.i(TAG, "Java: subtitle stop_subtitle");
		
		if (mIsSubtitleUsed) {
			mSubtitleStoped = true;
			mSubtitleThread.interrupt();
			
			try {
				Log.i(TAG, "Java subtitle before join");
				mSubtitleThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			subtitle_filename = null;
			mIsSubtitleUsed = false;
			mSubtitleStoped = false;
		}
	}
	
	protected void setupPlayer() {
		Log.i(TAG,"Step: setupPlayer()");

		DecodeMode DecMode;
		switch (mPlayerImpl) {
		case 0:
			DecMode = DecodeMode.AUTO;
			break;
		case 1:
			DecMode = DecodeMode.HW_SYSTEM;
			break;
		case 3:
			DecMode = DecodeMode.SW;
			break;
		default:
			Log.w(TAG, String.format("Java: unknown DecodeMode: %d", mPlayerImpl));
			DecMode = DecodeMode.SW;
			break;
		}
		
		mVideoView.stopPlayback();
		
		mVideoView.setDecodeMode(DecMode);
		mVideoView.setVideoURI(mUri);
		mVideoView.setOnCompletionListener(mCompletionListener);
		mVideoView.setOnErrorListener(mErrorListener);
		mVideoView.setOnInfoListener(mInfoListener);
		mVideoView.setOnPreparedListener(mPreparedListener);
		
		String audio_opt = Util.readSettings(this, "last_audio_ip_port");
		if (audio_opt != null && !audio_opt.isEmpty())
			mVideoView.setOption(audio_opt);
		
		String schema = mUri.getScheme();
		String path = null;
		
		if ("file".equalsIgnoreCase(schema))
			path = mUri.getPath();
		else
			path = mUri.toString();
		
		if (mTitle != null) {
			mController.setFileName(mTitle);
		}
		else {
			String name = getFileName(path);
			mController.setFileName(name);
		}

		if (pre_seek_msec != -1) {
			mVideoView.seekTo(pre_seek_msec);
			pre_seek_msec = -1;
		}
		
		mVideoView.start();
		
		mBufferingProgressBar.setVisibility(View.VISIBLE);
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
	
	protected void onComplete() {
		finish();
	}
	
	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			Log.i(TAG, "MEDIA_PLAYBACK_COMPLETE");
			
			mVideoView.stopPlayback();
			onComplete();
		}
	};

	private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
			Log.e(TAG, "Error: " + framework_err + "," + impl_err);
			
			mBufferingProgressBar.setVisibility(View.GONE);
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
				else if(MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER == extra)
					str_player_type = "XO Player";
				else if(MediaPlayer.PLAYER_IMPL_TYPE_FF_PLAYER == extra)
					str_player_type = "FF Player";
				else if(MediaPlayer.PLAYER_IMPL_TYPE_PP_PLAYER == extra)
					str_player_type = "PP Player";
				else
					str_player_type = "Unknown Player";
				Toast.makeText(VideoPlayerActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_DECODE_AVG_MSEC == what) {
				decode_avg_msec = extra;
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_AVG_MSEC == what) {
				render_avg_msec = extra;
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS == what) {
				decode_fps = extra;
				mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS == what) {
				render_fps = extra;
				mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME == what) {
				render_frame_num = extra;
				mHandler.sendEmptyMessage(MSG_UPDATE_RENDER_INFO);
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC == what) {
				av_latency_msec = extra;
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME == what) {
				decode_drop_frame++;
				mHandler.sendEmptyMessage(MSG_UPDATE_RENDER_INFO);
			}
			else if(MediaPlayer.MEDIA_INFO_TEST_MEDIA_BITRATE == what) {
				video_bitrate = extra;
				mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
			}
			else if(android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
				
			}
			else if(MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING == what) {
				av_latency_msec = extra;
				
				decode_fps = render_fps = 0;
				decode_drop_frame = 0;
				video_bitrate = 0;
				mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
			}
			
			return true;
		}
	};
	
	private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.i(TAG, "Java: OnPrepared");
			mController.show();
			mBufferingProgressBar.setVisibility(View.GONE);
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
		int incr;
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			if (!mController.isShowing())
				mController.show();
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (mController.isShowing()) {
				if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode)
					incr = 1;
				else
					incr = -1;
	
				int pos = mVideoView.getCurrentPosition();
				int step = mVideoView.getDuration() / 100 + 1000;
				Log.i(TAG, String.format("Java pos %d, step %s", pos, step));
				if (step > 30000)
					step = 30000;
				pos += (incr * step);
				if (pos > mVideoView.getDuration())
					pos = mVideoView.getDuration();
				else if (pos < 0)
					pos = 0;
				mVideoView.seekTo(pos);
				
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
		case KeyEvent.KEYCODE_BACK:
			if (mController.isShowing()) {
				mController.hide();
			}
			else if ((System.currentTimeMillis() - backKeyTime) > 2000) {
				Toast.makeText(VideoPlayerActivity.this,
					"再按一次退出", Toast.LENGTH_SHORT)
					.show();
				backKeyTime = System.currentTimeMillis();
			} else {
				onBackPressed();
			}
			return true;
		case KeyEvent.KEYCODE_MENU:
			openOptionsMenu();
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
    public void toggleMediaControlsVisiblity() {
    	if (mVideoView.isPlaying() && mController != null) {
	        if (mController.isShowing()) {
	        	mController.hide();
	        } else {
	        	mController.show();
	        }
    	}
    }

	@Override
	public void onPrepared(boolean success, String msg) {
		// TODO Auto-generated method stub
		Log.i(TAG, String.format("Java: subtitle onPrepared() %s, %s", success?"done":"failed", msg));
		
		if (success) {
			mSubtitleThread = new Thread(new Runnable(){
				@Override
				public void run() {
					display_subtitle_thr();
				}
			});
			mSubtitleThread.start();
			mIsSubtitleUsed = true;
			
			mSubtitleTextView.setVisibility(View.VISIBLE);
			Toast.makeText(VideoPlayerActivity.this, 
					"subtitle: " + subtitle_filename + " loaded", 
					Toast.LENGTH_SHORT).show();
		}
		else {
			mSubtitleTextView.setVisibility(View.INVISIBLE);
			Toast.makeText(VideoPlayerActivity.this, 
					"failed to load subtitle: " + subtitle_filename + " , msg: " + msg, 
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onSeekComplete() {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: subtitle onSeekComplete");
		mSubtitleSeeking = false;
	}
	
	private synchronized void display_subtitle_thr() {
        Log.i(TAG, "Java: subtitle thread started");

        final int SLEEP_MSEC = 50;
        SubTitleSegment seg;
        long from_msec = 0;
        long to_msec = 0;
        long hold_msec;
        long target_msec;
        
        boolean isDisplay = true;
        boolean isDropItem = false;
        
        if (mVideoView != null) {
        	// sync position
        	mSubtitleSeeking = true;
        	mSubtitleParser.seekTo(mVideoView.getCurrentPosition());
        	
        	while (mSubtitleSeeking && !mSubtitleStoped) {
        		try {
					Thread.sleep(SLEEP_MSEC);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        while (!mSubtitleStoped) {
        	if (isDisplay) {
        		seg = mSubtitleParser.next();
        		if (seg == null) {
        			Log.e(TAG, "Java: subtitle next_segment is null");
        			break;
        		}
        		
        		mSubtitleText = seg.getData();
                from_msec = seg.getFromTime();
                to_msec = seg.getToTime();
                hold_msec = to_msec - from_msec;
                Log.i(TAG, String.format("Java: subtitle frome %d, to %d, hold %d, %s", 
                	seg.getFromTime(), seg.getToTime(), hold_msec,
                	seg.getData()));
                target_msec = from_msec;
        	}
        	else {
            	target_msec = to_msec;
        	}
        	
    		if (mSubtitleSeeking == true) {
    			isDropItem = true;
    			target_msec = mVideoView.getDuration();
        	}
        
            while (mVideoView != null && mVideoView.getCurrentPosition() < target_msec) {
            	if (isDropItem == true) {
            		if (mSubtitleSeeking == false) {
            			break;
            		}
            	}
            	
            	try {
					wait(SLEEP_MSEC);
					//Log.d(TAG, "Java: subtitle wait");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.i(TAG, "Java: subtitle interrupted");
					e.printStackTrace();
					break;
				}
            }
            
            Log.i(TAG, "Java: subtitle mSubtitleSeeking: " + mSubtitleSeeking);
            if (isDropItem == true) {
        		// drop last subtitle item
        		isDisplay = true;
        		isDropItem = false;
        		mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
        		continue;
        	}

            if (isDisplay) {
            	mHandler.sendEmptyMessage(MSG_DISPLAY_SUBTITLE);
            }
            else {
            	mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
            }
            
            isDisplay = !isDisplay;
        }
        
        mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
        mSubtitleParser.close();
        mSubtitleParser = null;
        Log.i(TAG, "Java: subtitle thread exited");
    }
	
	private Handler mHandler = new Handler(){  
		  
        @Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
			case MSG_UPDATE_PLAY_INFO:
			case MSG_UPDATE_RENDER_INFO:
				if (mbShowDebugInfo) {
					mTextViewDebugInfo.setText(String.format("%02d|%03d v-a: %+04d "
							+ "dec/render %d(%d)/%d(%d) fps/msec bitrate %d kbps", 
						render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
						decode_fps, decode_avg_msec, render_fps, render_avg_msec,
						video_bitrate));
				}
				break;
			case MSG_DISPLAY_SUBTITLE:
				mSubtitleTextView.setText(mSubtitleText);
				break;
			case MSG_HIDE_SUBTITLE:
				mSubtitleTextView.setText("");
				break;
			default:
				Log.w(TAG, "Java: unknown msg.what " + msg.what);
				break;
			}			 
        }
	}; 
}
