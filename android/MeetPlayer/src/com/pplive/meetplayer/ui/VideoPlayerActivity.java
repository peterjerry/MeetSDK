package com.pplive.meetplayer.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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

	private Uri mUri = null;
	private DecodeMode mDecodeMode = DecodeMode.AUTO;
	private MeetVideoView mVideoView = null;
	private MyMediaController mController;
	private ProgressBar mBufferingProgressBar = null;
	
	private boolean mIsBuffering = false;
	
	// subtitle
	private SimpleSubTitleParser mSubtitleParser;
	private TextView mSubtitleTextView;
	private String mSubtitleText;
	private Thread mSubtitleThread;
	private boolean mSubtitleSeeking = false;
	private boolean mIsSubtitleUsed = false;
	private String subtitle_filename;
	private boolean mSubtitleStoped = false;
	
	private static final int MSG_DISPLAY_SUBTITLE					= 401;
	private static final int MSG_HIDE_SUBTITLE					= 402;
	
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
		
		this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
		this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		
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
		case R.id.select_subtitle:
			popupSelectSubtitle();
			break;
		case R.id.toggle_debug_info:
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
					subtitle_filename = sub_folder + "/" + str_file_list[whichButton];
					Log.i(TAG, "Load subtitle file: " + subtitle_filename);
					Toast.makeText(VideoPlayerActivity.this, 
							"Load subtitle file: " + subtitle_filename, Toast.LENGTH_SHORT).show();
					if (mVideoView != null) {
						start_subtitle(subtitle_filename);
					}
					
					dialog.dismiss();
				}
			})
		.create();
		choose_subtitle_dlg.show();
	}
	
	private boolean start_subtitle(String filename) {
		Log.i(TAG, "Java: subtitle start_subtitle " + filename);
    	
		stop_subtitle();
		
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
	
	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			Log.i(TAG, "MEDIA_PLAYBACK_COMPLETE");
			mVideoView.stopPlayback();
			finish();
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
		int incr = -1;
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
				mController.show();
				
				if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode || 
						KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
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
					else if(pos < 0)
						pos = 0;
					mVideoView.seekTo(pos);
				}
				else if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode || 
						KeyEvent.KEYCODE_DPAD_UP == keyCode) {
					if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode)
						incr = 1;
					else
						incr = -1;
					
					switchDisplayMode(incr);
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
		}
		else {
			mSubtitleTextView.setVisibility(View.INVISIBLE);
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
        	mSubtitleParser.seekTo(mVideoView.getCurrentPosition());
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
			/*case MSG_CLIP_LIST_DONE:
				if (mAdapter == null) {
					mAdapter = new LocalFileAdapter(ClipListActivity.this, mListUtil.getList(), R.layout.pptv_list);
					lv_filelist.setAdapter(mAdapter);
				}
				else {
					mAdapter.updateData(mListUtil.getList());
					mAdapter.notifyDataSetChanged();
				}
				break;
			case MSG_UPDATE_PLAY_INFO:
			case MSG_UPDATE_RENDER_INFO:
				if (isLandscape) {
					mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d "
							+ "dec/render %d(%d)/%d(%d) fps/msec bitrate %d kbps", 
						render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
						decode_fps, decode_avg_msec, render_fps, render_avg_msec,
						video_bitrate));
				}
				else {
					mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d\n"
							+ "dec/render %d(%d)/%d(%d) fps/msec\nbitrate %d kbps", 
						render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
						decode_fps, decode_avg_msec, render_fps, render_avg_msec,
						video_bitrate));
				}
				break;
			case MSG_CLIP_PLAY_DONE:
				Toast.makeText(ClipListActivity.this, "clip completed", Toast.LENGTH_SHORT).show();
				mTextViewInfo.setText("play info");
				break;*/
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
