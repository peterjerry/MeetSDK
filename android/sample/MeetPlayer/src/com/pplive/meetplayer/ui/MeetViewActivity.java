package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pplive.meetplayer.R;
import com.pplive.meetplayer.ui.widget.MiniMediaController;
import com.pplive.meetplayer.util.Content;
import com.pplive.meetplayer.util.EPGUtil;
import com.pplive.meetplayer.util.Module;
import com.pplive.meetplayer.util.PlayLink2;
import com.pplive.meetplayer.util.PlayLinkUtil;
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.player.MeetVideoView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MeetViewActivity extends Activity {

	private final static String TAG = "MeetViewActivity";
	private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};
	
	private final static String TV_SERIES_LINK = "9037770";
	
	private final int MAX_DESC_LEN = 30;
	
	private final int MSG_PPTV_CLIP_LIST_DONE = 1001;
	private final int MSG_FAIL_TO_GET_DETAIL	= 2002;
	
	private final int LIST_MOVIE 		= 1;
	private final int LIST_TV_SERIES 	= 2;
	private final int LIST_LIVE		= 3;
	
	private final String[] from = { "title", "desc", "ft", "duration", "resolution" };
	
	private final int[] to = { R.id.tv_title, R.id.tv_description, 
			R.id.tv_ft, R.id.tv_duration, R.id.resolution};
	
	private int mListType = LIST_TV_SERIES;
	private int mPageNum = 1;
	private Uri mUri = null;
	private RelativeLayout mLayout;
	private MeetVideoView mVideoView;
	private MiniMediaController mController;
	private int preview_height = 0;
	private int mBufferingPertent = 0;
	
	private ProgressBar mBufferingProgressBar = null;
	private boolean mIsBuffering = false;
	
	private int mPlayerImpl = 0;
	private Button btnPlayerImpl;
	private Button btnMovies;
	private Button btnTVSeries;
	private Button btnLive;
	private Button btnNextPage;
	
	private MyAdapter mAdapter;
	private ListView lv_pptvlist;
	private List<Map<String, Object>> mPPTVClipList = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Java: onCreate()");
		
		// Full Screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
		    WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// No Titlebar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_meet_videoview);
		
		this.mLayout = (RelativeLayout) findViewById(R.id.view_preview);
		
		Util.initMeetSDK(this);
		Util.startP2PEngine(this);
		
		mVideoView = (MeetVideoView) findViewById(R.id.surface_view2);
		mVideoView.setOnCompletionListener(mCompletionListener);
		mVideoView.setOnErrorListener(mErrorListener);
		mVideoView.setOnInfoListener(mInfoListener);
		mVideoView.setOnPreparedListener(mPreparedListener);
		
		mController = (MiniMediaController) findViewById(R.id.video_controller2);
		
		mController.setInstance(this);
		
		mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering2);
		
		this.btnPlayerImpl = (Button) findViewById(R.id.btn_player_impl);
		this.btnPlayerImpl.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				final String[] PlayerImpl = {"Auto", "System", "NuPlayer", "FFPlayer"};
				
				Dialog choose_player_impl_dlg = new AlertDialog.Builder(MeetViewActivity.this)
				.setTitle("select player impl")
				.setSingleChoiceItems(PlayerImpl, mPlayerImpl, /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							Log.i(TAG, "select player impl: " + whichButton);
							
							mPlayerImpl = whichButton;
							Toast.makeText(MeetViewActivity.this, 
									"select type: " + PlayerImpl[whichButton], Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					})
				.create();
				choose_player_impl_dlg.show();	
			}
		});
		
		this.btnMovies = (Button) findViewById(R.id.btn_movies);
		this.btnMovies.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				mListType = LIST_MOVIE;
				mPageNum = 1;
				new ListPPTVTask().execute(mListType, mPageNum);
			}
		});
		
		this.btnTVSeries = (Button) findViewById(R.id.btn_tv_series);
		this.btnTVSeries.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				mListType = LIST_TV_SERIES;
				mPageNum = 1;
				new ListPPTVTask().execute(mListType, mPageNum);
			}
		});
		
		this.btnLive = (Button) findViewById(R.id.btn_live);
		this.btnLive.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				mListType = LIST_LIVE;
				mPageNum = 1;
				new ListPPTVTask().execute(mListType, mPageNum);
			}
		});
		
		this.btnNextPage = (Button) findViewById(R.id.btn_next_page);
		this.btnNextPage.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				mPageNum++;
				new ListPPTVTask().execute(mListType, mPageNum);
			}
		});
		
		mPPTVClipList = new ArrayList<Map<String, Object>>();
		
		new ListPPTVTask().execute(mListType, mPageNum);
		
		this.lv_pptvlist = (ListView) findViewById(R.id.lv_pptvlist);
		
		this.lv_pptvlist
		.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				Log.i(TAG, String.format("Java: onItemClick %d %d", position, id));
				
				HashMap<String, Object> item = (HashMap<String, Object>) lv_pptvlist.getItemAtPosition(position);
				String title = (String)item.get("title");
				String vid = (String)item.get("vid");
				Log.i(TAG, String.format("Java: title %s, vid %s", title, vid));
				
				short http_port = MediaSDK.getPort("http");
				Log.i(TAG, "Http port is: " + http_port);
				String uri = PlayLinkUtil.getPlayUrl(Integer.valueOf(vid), http_port, 1, 3, "");
				mUri = Uri.parse(uri);
				
				setupPlayer();
			}
		});
	}

	private class ListPPTVTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			boolean ret;
			
			mPPTVClipList.clear();
			
			int list_type = params[0];
			int start_page = params[1];
			
			switch(list_type) {
			case LIST_TV_SERIES:
				ret = fill_list_series();
				break;
			case LIST_MOVIE:
				ret = fill_list_movie(start_page);
				break;
			case LIST_LIVE:
				ret = fill_list_live(start_page);
				break;
			default:
				Log.e(TAG, "invalid list type : " + list_type);
				return false;
			}
			
			if(!ret)
				mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_DETAIL);
			else
				mHandler.sendEmptyMessage(MSG_PPTV_CLIP_LIST_DONE);
			
			return ret;
		}
	}
	
	private boolean fill_list_live(int start_page) {
		EPGUtil util = new EPGUtil();
		boolean ret = util.live(start_page, 15, 156);
		if (!ret)
			return false;
		
		List<PlayLink2> list = util.getLink();
		if (list == null)
			return false;
		
		int size = list.size();
		for (int i=0;i<size;i++) {
			HashMap<String, Object> new_item = new HashMap<String, Object>();
			PlayLink2 link = list.get(i);
			String desc = link.getDescription();
			if (desc.length() > MAX_DESC_LEN)
				desc = desc.substring(0, MAX_DESC_LEN) + "...";
			
			new_item.put("title", link.getTitle());
			new_item.put("desc", desc);
			new_item.put("ft", "1");
			new_item.put("duration", "N/A");
			new_item.put("resolution", "N/A");
			
			new_item.put("vid", link.getId());
			mPPTVClipList.add(new_item);
		}
		
		return true;
	}
	
	private boolean fill_list_series() {
		EPGUtil util = new EPGUtil();
		boolean ret = util.detail(TV_SERIES_LINK);
		if (!ret)
			return false;
		
		List<PlayLink2> list = util.getLink();
		if (list == null)
			return false;
		
		int size = list.size();
		for (int i=0;i<size;i++) {
			HashMap<String, Object> new_item = new HashMap<String, Object>();
			PlayLink2 link = list.get(i);
			String desc = link.getDescription();
			if (desc.length() > MAX_DESC_LEN)
				desc = desc.substring(0, MAX_DESC_LEN) + "...";
			
			new_item.put("title", link.getTitle());
			new_item.put("desc", desc);
			new_item.put("ft", "1");
			new_item.put("duration", String.valueOf(link.getDuration() / 60));
			new_item.put("resolution", link.getResolution().replace('|', 'x'));
			
			new_item.put("vid", link.getId());
			mPPTVClipList.add(new_item);
		}
		
		return true;
	}
	
	private boolean fill_list_movie(int start_page) {
		EPGUtil util = new EPGUtil();
		
		String link = "app://aph.pptv.com/v4/cate/movie?type=1";
		boolean ret = util.contents(link);
		if (!ret)
			return false;
		
		List<Content> ContentList = util.getContent();
		
		// save "type" for list()
		String ContentType = "";
		int pos = link.indexOf("type=");
		if (pos != -1) {
			ContentType = link.substring(pos, link.length());
		}
		
		String param = ContentList.get(5).getParam();
		if (param.startsWith("type="))
			ContentType = "";
		
		ret = util.list(param, ContentType, start_page, "order=n", 15);
		if (!ret)
			return false;
		
		List<PlayLink2> PlayLinkList = util.getLink();
		
		List<String> vidList = new ArrayList<String>();
		
		int size = PlayLinkList.size();
		for (int i=0;i<size;i++) {
			HashMap<String, Object> new_item = new HashMap<String, Object>();
			PlayLink2 link2 = PlayLinkList.get(i);
			
			String desc = "N/A";
			
			new_item.put("title", link2.getTitle());
			new_item.put("desc", desc);
			new_item.put("ft", "1");
			new_item.put("duration", String.valueOf(link2.getDuration() / 60));
			new_item.put("resolution", link2.getResolution().replace('|', 'x'));
			
			new_item.put("vid", link2.getId());
			mPPTVClipList.add(new_item);
			
			vidList.add(link2.getId());
		}
		
		for (int i=0;i<size;i++) {
			if (util.detail(vidList.get(i))) {
				List<PlayLink2> finallist =  util.getLink();
				if (finallist.size() > 0) {
					String desc = finallist.get(0).getDescription();
					if (desc.length() > MAX_DESC_LEN)
						desc = desc.substring(0, MAX_DESC_LEN) + "...";
					
					mPPTVClipList.get(i).put("desc", desc);
				}
			}
		}
		
		return true;
	}
	
	private Handler mHandler = new Handler(){  
		  
        @Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
			case MSG_PPTV_CLIP_LIST_DONE:
				mAdapter = new MyAdapter(MeetViewActivity.this, mPPTVClipList, R.layout.pptv_list,
						from, to);
				lv_pptvlist.setAdapter(mAdapter);
				break;
			case MSG_FAIL_TO_GET_DETAIL:
				Toast.makeText(MeetViewActivity.this, "failed to connect to server", Toast.LENGTH_SHORT).show();
				break;
			default:
				Log.w(TAG, "unknown msg.what " + msg.what);
				break;
            }
        }
	};
	
	@Override
	protected void onStart() {
		super.onStart();

		Log.i(TAG, "Java: onStart");
	}
	
	void update_preview_size(boolean isLandscape) {
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		
		if (isLandscape)
			mLayout.getLayoutParams().height = screen_height;
		else
			mLayout.getLayoutParams().height = screen_height * 2 / 5;
		mLayout.requestLayout();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "Java: onResume");
		
		int orient = getRequestedOrientation();
		Log.i(TAG, "Java: orient " + orient);
		
		boolean isLandscape = (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orient);
		update_preview_size(isLandscape);
		
		if (mVideoView != null)
			mVideoView.start();
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Java: onPause()");

		if (mVideoView != null)
			mVideoView.pause();
	}

	@Override
	protected void onStop() {
		super.onStop();

		Log.i(TAG, "Java: onStop()");

		if (isFinishing() && mVideoView != null)
			mVideoView.stopPlayback();
	}

	private void setupPlayer() {
		Log.i(TAG,"Step: setupPlayer()");

		mVideoView.stopPlayback();
		
		DecodeMode dec_mode = DecodeMode.AUTO;
		if (0 == mPlayerImpl) {
			dec_mode = DecodeMode.AUTO;
		}
		else if (1 == mPlayerImpl) {
			dec_mode = DecodeMode.HW_SYSTEM;
		}
		else if (3 == mPlayerImpl) {
			dec_mode = DecodeMode.SW;
		}
		else if (4 == mPlayerImpl) {
			dec_mode = DecodeMode.SW;
		}
		else {
			Toast.makeText(MeetViewActivity.this, "invalid player implement: " + Integer.toString(mPlayerImpl), 
				Toast.LENGTH_SHORT).show();
			return;
		}
		
		mVideoView.setDecodeMode(dec_mode);
		mVideoView.setVideoURI(mUri);
		
		mController.setMediaPlayer(mVideoView);
		
		String schema = mUri.getScheme();
		String path = null;
		
		if ("file".equalsIgnoreCase(schema))
			path = mUri.getPath();
		else
			path = mUri.toString();
		
		mVideoView.start();
		
		mBufferingProgressBar.setVisibility(View.VISIBLE);
		mIsBuffering = true;
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
			mBufferingProgressBar.setVisibility(View.INVISIBLE);
			mIsBuffering = false;
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
				Toast.makeText(MeetViewActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
			}
			
			return true;
		}
	};
	
	private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.i(TAG, "Java: OnPrepared");
			mBufferingProgressBar.setVisibility(View.GONE);
			mIsBuffering = false;
		}
	};
	
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
			return true;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			Log.i(TAG, "onLongPress!!!");
			/*if (mVideoView != null) {
				mVideoView.stopPlayback();
				
				mVideoView.setVideoURI(mUri);
				mVideoView.start();
			}*/
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
		int incr = -1;
		int mode;
		
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				
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
				}
				else {
					mVideoView.start();
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
	
	public void onConfigurationChanged(Configuration conf) {
		super.onConfigurationChanged(conf);
		
		Log.i(TAG, "Java: onConfigurationChanged");
		
		int orientation = getRequestedOrientation();
		Log.i(TAG, "Java: orientation " + orientation);
		boolean isLandscape = (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		update_preview_size(isLandscape);
	}  
	
	private void toggleMediaControlsVisiblity() {
    	if (mVideoView != null && mVideoView.isPlaying() && mController != null) {
	        if (mController.isShowing()) {
	        	mController.hide();
	        } else {
	        	mController.show();
	        }
    	}
    }
	
}
