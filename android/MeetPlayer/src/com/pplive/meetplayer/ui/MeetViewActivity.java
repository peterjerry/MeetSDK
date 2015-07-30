package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pplive.common.pptv.Catalog;
import com.pplive.common.pptv.Content;
import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.ui.widget.MiniMediaController;
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MeetViewActivity extends Activity implements OnFocusChangeListener {

	private final static String TAG = "MeetViewActivity";
	private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};
	
	private final static String TV_SERIES_LINK = "9037770";
	
	private final int MAX_DESC_LEN = 30;
	
	private static final int MSG_PPTV_CLIP_LIST_DONE	= 1001;
	private static final int MSG_UPDATE_PLAY_INFO 	= 1002;
	private static final int MSG_UPDATE_RENDER_INFO	= 1003;
	private static final int MSG_FAIL_TO_GET_DETAIL	= 2002;
	
	private final int LIST_MOVIE 		= 1;
	private final int LIST_TV_SERIES 	= 2;
	private final int LIST_LIVE		= 3;
	private final int LIST_FRONTPAGE	= 4;
	
	private final String[] from = { "title", "desc", "ft", "duration", "resolution" };
	
	private final int[] to = { R.id.tv_title, R.id.tv_description, 
			R.id.tv_ft, R.id.tv_duration, R.id.tv_resolution};
	
	private int mListType = LIST_TV_SERIES;
	private int mPageNum = 1;
	private Uri mUri = null;
	private RelativeLayout mPreviewLayout;
	private LinearLayout mCtrlLayout;
	private RelativeLayout.LayoutParams mCtrlLayoutParams;
	private MeetVideoView mVideoView;
	private MiniMediaController mController;
	
	private int decode_fps						= 0;
	private int render_fps 					= 0;
	private int decode_avg_msec 				= 0;
	private int render_avg_msec 				= 0;
	private int render_frame_num				= 0;
	private int decode_drop_frame				= 0;
	private int av_latency_msec				= 0;
	private int video_bitrate					= 0;

	private int mBufferingPertent = 0;
	
	private ProgressBar mBufferingProgressBar = null;
	private boolean mIsBuffering = false;
	
	private int mPlayerImpl = 0;
	private Button btnPlayerImpl;
	private Button btnMovies;
	private Button btnTVSeries;
	private Button btnLive;
	private Button btnNextPage;
	private Button btnFt;
	private TextView mTextViewInfo;
	
	private boolean mPreviewFocused = false;
	private boolean mStartFromPortrait = false;
	
	private boolean mSideBarShowed = true;
	
	private PPTVAdapter mAdapter;
	private ListView lv_pptvlist;
	private List<Map<String, Object>> mPPTVClipList = null;
	private int mLastPlayItemPos = -1;
	private String mPlaylink;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Java: onCreate()");
		
		Intent intent = getIntent();
		if (intent.hasExtra("playlink")) {
			mPlaylink = intent.getStringExtra("playlink");
		}
		else {
			mPlaylink = TV_SERIES_LINK;
		}
		
		// Full Screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
		    WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// No Titlebar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_meet_videoview);
		
		this.mPreviewLayout = (RelativeLayout) findViewById(R.id.view_preview);
		this.mCtrlLayout = (LinearLayout) findViewById(R.id.layout_ctrl);
		mCtrlLayoutParams = (RelativeLayout.LayoutParams) mCtrlLayout.getLayoutParams();
		
		//mPreviewLayout.setFocusable(true);
		//mPreviewLayout.setOnFocusChangeListener(this);
		
		mTextViewInfo = (TextView) findViewById(R.id.tv_info);
		mTextViewInfo.setTextColor(Color.RED);
		mTextViewInfo.setTextSize(18);
		mTextViewInfo.setTypeface(Typeface.MONOSPACE);
		
		Util.initMeetSDK(this);
		Util.startP2PEngine(this);
		
		mVideoView = (MeetVideoView) findViewById(R.id.surface_view2);
		mVideoView.setOnCompletionListener(mCompletionListener);
		mVideoView.setOnErrorListener(mErrorListener);
		mVideoView.setOnInfoListener(mInfoListener);
		mVideoView.setOnPreparedListener(mPreparedListener);
		
		String audio_opt = Util.readSettings(this, "last_audio_ip_port");
		if (audio_opt != null && !audio_opt.isEmpty())
			mVideoView.setOption(audio_opt);
		
		mController = (MiniMediaController) findViewById(R.id.video_controller2);
		
		mController.setInstance(this);
		mController.setFocusable(true);
		
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
		
		this.btnFt = (Button)  findViewById(R.id.btn_ft);
		this.btnFt.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] ft = {"流畅", "高清", "超清", "蓝光"};
				
				Dialog choose_ft_dlg = new AlertDialog.Builder(MeetViewActivity.this)
				.setTitle("select ft")
				.setSingleChoiceItems(ft, Integer.parseInt(btnFt.getText().toString()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btnFt.setText(Integer.toString(whichButton));
							dialog.dismiss();
						}
					})
				.create();
				choose_ft_dlg.show();	
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
				
				mLastPlayItemPos = position;
				start_player(mLastPlayItemPos);
			}
		});
	}

	private void start_player(int index) {
		HashMap<String, Object> item = (HashMap<String, Object>)mAdapter.getItem(index);
		String title = (String)item.get("title");
		String vid = (String)item.get("vid");
		int ft = (Integer)item.get("ft");
		if (ft  == -1)
			ft = Integer.valueOf(btnFt.getText().toString());
		String info = String.format("title %s, vid %s ft %d", title, vid, ft);
		Log.i(TAG, "Java: " + info);
		Toast.makeText(this, "play " + info, Toast.LENGTH_SHORT).show();
		
		short http_port = MediaSDK.getPort("http");
		Log.i(TAG, "Http port is: " + http_port);
		
		String uri = PlayLinkUtil.getPlayUrl(Integer.valueOf(vid), http_port, ft, 3, "");
		Log.i(TAG, "Java: getPlayerUrl " + uri);
		mUri = Uri.parse(uri);
		
		setupPlayer();
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
			case LIST_FRONTPAGE:
				ret = fill_list_frontpage(start_page);
				break;
			default:
				Log.e(TAG, "invalid list type : " + list_type);
				return false;
			}
			
			if (!ret)
				mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_DETAIL);
			else
				mHandler.sendEmptyMessage(MSG_PPTV_CLIP_LIST_DONE);
			
			return ret;
		}
	}
	
	private boolean fill_list_frontpage(int index) {
		EPGUtil util = new EPGUtil();
		boolean ret = util.catalog(2); // 今日热点
		if (!ret)
			return false;
		
		List<Catalog> list = util.getCatalog();
		if (list == null || list.size() == 0)
			return false;
		
		List<PlayLink2> playlinkList = null;
		
		int found_cnt = 0;
		for (int i=0;i<list.size();i++) {
			String vid = list.get(2).getVid();
			if (vid == null) {
				Log.e(TAG, "vid is null");
				return false;
			}
			
			ret = util.detail(vid);
			if (!ret) {
				Log.e(TAG, "failed to get detail");
				return false;
			}
			
			playlinkList = util.getLink();
			int size = playlinkList.size();
			if (size > 3) {
				found_cnt++;
				
				if (found_cnt == index)
					break;
			}
		}
		
		if (playlinkList == null || playlinkList.size() <= 3) {
			mPageNum = 1;
			return false;
		}
		
		int size = playlinkList.size();
		
		for (int i=0;i<size;i++) {
			PlayLink2 link = playlinkList.get(i);
			String desc = link.getDescription();
			if (desc.length() > MAX_DESC_LEN)
				desc = desc.substring(0, MAX_DESC_LEN) + "...";
			
			int []ft_list = util.getAvailableFT(link.getId());
			if (ft_list == null || ft_list.length == 0)
				continue;
			
			int ft = -1;
			for (int j=ft_list.length - 1;j>=0;j--) {
				if (ft_list[j] >=0 && ft_list[j] <= 3) {
					ft = ft_list[j];
					break;
				}
			}
			
			if (ft == -1)
				continue;
			
			HashMap<String, Object> new_item = new HashMap<String, Object>();
			
			new_item.put("title", link.getTitle());
			new_item.put("desc", desc);
			new_item.put("ft", ft);
			new_item.put("duration", "N/A");
			new_item.put("resolution", "N/A");
			
			new_item.put("vid", link.getId());
			mPPTVClipList.add(new_item);
		}
		
		return true;
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
			new_item.put("ft", 1);
			new_item.put("duration", "N/A");
			new_item.put("resolution", "N/A");
			
			new_item.put("vid", link.getId());
			mPPTVClipList.add(new_item);
		}
		
		return true;
	}
	
	private boolean fill_list_series() {
		EPGUtil util = new EPGUtil();
		boolean ret = util.detail(mPlaylink);
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
			new_item.put("ft", -1);
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
		
		ret = util.list(param, ContentType, start_page, "order=n", 15, false);
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
			new_item.put("ft", -1);
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
				if (mAdapter == null) {
					mAdapter = new PPTVAdapter(MeetViewActivity.this, mPPTVClipList, R.layout.pptv_list);
					lv_pptvlist.setAdapter(mAdapter);
				}
				else {
					mAdapter.updateData(mPPTVClipList);
					mAdapter.notifyDataSetChanged();
				}
				break;
			case MSG_FAIL_TO_GET_DETAIL:
				Toast.makeText(MeetViewActivity.this, "failed to connect to server", Toast.LENGTH_SHORT).show();
				break;
			case MSG_UPDATE_PLAY_INFO:
			case MSG_UPDATE_RENDER_INFO:
				/*if (isLandscape) {
					mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d "
							+ "dec/render %d(%d)/%d(%d) fps/msec bitrate %d kbps", 
						render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
						decode_fps, decode_avg_msec, render_fps, render_avg_msec,
						video_bitrate));
				}
				else {*/
					mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d\n"
							+ "dec/render %d(%d)/%d(%d) fps/msec\nbitrate %d kbps", 
						render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
						decode_fps, decode_avg_msec, render_fps, render_avg_msec,
						video_bitrate));
				//}
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
	
	void setup_layout(boolean isLandscape) {
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		
		RelativeLayout.LayoutParams paramsPreview = (RelativeLayout.LayoutParams)mPreviewLayout.getLayoutParams();
		RelativeLayout.LayoutParams paramsCtrl = (RelativeLayout.LayoutParams)mCtrlLayout.getLayoutParams();
		
		if (isLandscape) {
			if (mStartFromPortrait) {
				paramsPreview.width = screen_width;
				paramsPreview.height = screen_height;
				paramsPreview.addRule(RelativeLayout.BELOW, R.id.layout_preview);
			}
			else {
				paramsPreview.height = screen_height;
				paramsPreview.addRule(RelativeLayout.RIGHT_OF, R.id.layout_ctrl);
				
				paramsCtrl.width = screen_width / 4;
				paramsCtrl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
		}
		else {
			paramsPreview.height = screen_height * 2 / 5;
			paramsPreview.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			
			paramsCtrl.addRule(RelativeLayout.BELOW, R.id.view_preview);
		}
		
		// lp4.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		
		mCtrlLayout.setLayoutParams(paramsCtrl);
		mPreviewLayout.setLayoutParams(paramsPreview);
		
		//mPreviewLayout.requestLayout();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "Java: onResume");
		
		int orient = getRequestedOrientation();
		Log.i(TAG, "Java: orient " + orient);
		
		//boolean isLandscape = (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orient);
		boolean isLandscape = false;
		
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		if (screen_width > screen_height)
			isLandscape = true;
		
		mStartFromPortrait = !isLandscape;
		setup_layout(isLandscape);
		
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
		mVideoView.start();
		
		mController.setMediaPlayer(mVideoView);
		
		mBufferingProgressBar.setVisibility(View.VISIBLE);
		mIsBuffering = true;
	}

	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			Log.i(TAG, "MEDIA_PLAYBACK_COMPLETE");
			Toast.makeText(MeetViewActivity.this, "OnCompletionListener", Toast.LENGTH_SHORT).show();
					
			mVideoView.stopPlayback();
			
			// play next clip
			mLastPlayItemPos++;
			if (mLastPlayItemPos >= lv_pptvlist.getCount())
				mLastPlayItemPos = 0;
			
			lv_pptvlist.setSelection(mLastPlayItemPos);
			Log.i(TAG, "play next item pos: " + mLastPlayItemPos);
			start_player(mLastPlayItemPos);
		}
	};

	private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
			Log.i(TAG, "Error: " + framework_err + "," + impl_err);
			Toast.makeText(MeetViewActivity.this, 
					String.format("onError what: %d, extra %d", framework_err, impl_err), Toast.LENGTH_SHORT).show();
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
		}
	});
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.i(TAG, "onTouchEvent()" + event.toString());
		
		return mDoubleTapListener.onTouchEvent(event);
	}
	
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mSideBarShowed && mVideoView != null) {
			showMenu(false);
			return;
		}
		
		super.onBackPressed();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		Log.d(TAG, "keyCode: " + keyCode);
		
		//if (!mPreviewFocused)
		//	return super.onKeyDown(keyCode, event);
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			if (!mSideBarShowed)
				showMenu(true);
			break;
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (!mSideBarShowed && mVideoView != null && !mController.isShowing()) {
				mController.show(5000);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_UP:
			if (!mSideBarShowed && mVideoView != null) {
				int incr;
				if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode)
					incr = 1;
				else
					incr = -1;
				
				switchDisplayMode(incr);
				return true;
			}
		default:
			Log.d(TAG, "no spec action: " + keyCode);
			break;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	public void onConfigurationChanged(Configuration conf) {
		super.onConfigurationChanged(conf);
		
		Log.i(TAG, "Java: onConfigurationChanged");
		
		int orientation = getRequestedOrientation();
		Log.i(TAG, "Java: orientation " + orientation);
		boolean isLandscape = (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setup_layout(isLandscape);
	}  
	
	private void showMenu(boolean isShow) {
		if (isShow) {
			mSideBarShowed = true;
			mCtrlLayoutParams.leftMargin = 0;
		} else {
			mSideBarShowed = false;
			mCtrlLayoutParams.leftMargin = 0 - mCtrlLayoutParams.width;
			mPreviewLayout.requestFocus();
		}
		mCtrlLayout.setLayoutParams(mCtrlLayoutParams);
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

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		// TODO Auto-generated method stub
		mPreviewFocused = hasFocus;
		
		if (hasFocus) {
			if (mPreviewLayout != null) {
				Drawable drawable1 = getResources().getDrawable(R.drawable.bg_border1); 
				mPreviewLayout.setBackground(drawable1);
			}
		}
		else {
			if (mPreviewLayout != null) {
				Drawable drawable2 = getResources().getDrawable(R.drawable.bg_border2); 
				mPreviewLayout.setBackground(drawable2);
			}
		}
	}
	
}
