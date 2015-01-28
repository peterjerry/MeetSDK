package com.pplive.meetplayer.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.os.Build;
import android.media.AudioManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.graphics.Color;
import android.util.DisplayMetrics; // for display width and height
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.app.Dialog;

import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.AtvUtils;
import com.pplive.meetplayer.util.DownloadAsyncTask;
import com.pplive.meetplayer.util.FeedBackFactory;
import com.pplive.meetplayer.util.LogcatHelper;




import com.pplive.meetplayer.util.Util;

// for thread
import android.os.Handler;  
import android.os.Message;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.subtitle.SimpleSubTitleParser;
import android.pplive.media.subtitle.SubTitleParser;
import android.pplive.media.subtitle.SubTitleSegment;
import android.pplive.media.player.TrackInfo;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.provider.MediaStore;
import android.database.Cursor;

import com.pplive.sdk.MediaSDK;
import com.pplive.thirdparty.BreakpadUtil;

import org.apache.ivy.util.url.ApacheURLLister;

public class ClipListActivity extends Activity implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
		MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener,
		MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnBufferingUpdateListener,
		MediaPlayerControl, SurfaceHolder.Callback, SubTitleParser.Callback {

	private final static String TAG = "ClipList";	
		
	private Button btnPlay;
	private Button btnSelectTime;
	private Button btnClipLocation;
	private Button btnPlayerImpl;
	private Button btnPPboxSel;
	private Button btnTakeSnapShot;
	private Button btnSelectAudioTrack;
	private EditText et_play_url;
	private MyPreView mPreview;
	private SurfaceHolder mHolder;
	private MyMediaController mMediaController;
	private RelativeLayout mLayout;
	private ProgressBar mBufferingProgressBar;
	private EditText et_playlink;
	private EditText et_ft;
	private EditText et_bw_type;
	private MediaPlayer mPlayer 				= null;
	private MyAdapter mAdapter;
	
	private ProgressBar mDownloadProgressBar;
	private TextView mProgressTextView;
	private Dialog mUpdateDialog;
	
	private boolean mIsPreview					= true;
	
	private boolean mIsBuffering 				= false;
	private boolean mStoped					= false;
	private boolean mQuit						= false;
	private boolean mHomed						= false;
	
	private String mPlayUrl;
	private int mAudioTrackNum = 4;
	private int mAudioChannel = 1;
	private int mPlayerImpl = 0;
	
	// subtitle
	private SimpleSubTitleParser mSubtitleParser;
	private TextView mSubtitleTextView;
	private String mSubtitleText;
	private Thread mSubtitleThread;
	private boolean mSubtitleSeeking = false;
	private boolean mIsSubtitleUsed;
	
	private List<URL> mHttpFileList;
	private List<URL> mHttpFolderList;
	private boolean mListLocalFile				= true;
	
	private LinearLayout mControllerLayout 		= null;
	private TextView mTextViewInfo 				= null;
	
	private int decode_fps 						= 0;
	private int render_fps 						= 0;
	private int decode_avg_msec 				= 0;
	private int render_avg_msec 				= 0;
	private int render_frame_num				= 0;
	private int decode_drop_frame				= 0;
	private int av_latency_msec					= 0;
	private int video_bitrate					= 0;
	
	private int preview_height;
	private String str_play_link_surfix;
	
	private enum MEET_PLAY_TYPE {
		UNKNOWN_TYPE,
		LOCAL_TYPE,
		HTTP_TYPE,
		PPTV_VOD_TYPE,
		PPTV_LIVE_TYPE
	};
	private MEET_PLAY_TYPE play_type			= MEET_PLAY_TYPE.PPTV_VOD_TYPE;

	final static int ONE_MAGEBYTE 				= 1048576;
	final static int ONE_KILOBYTE 				= 1024;
	
	// menu item
	final static int UPDATE_CLIP_LIST			= Menu.FIRST;
	final static int UPDATE_APK				= Menu.FIRST + 1;
	final static int UPLOAD_CRASH_REPORT		= Menu.FIRST + 2;
	final static int QUIT 						= Menu.FIRST + 3;
	final static int OPTION 					= Menu.FIRST + 4;
	final static int OPTION_PREVIEW			= Menu.FIRST + 11;
	
	private ListView lv_filelist 				= null;
	List<Map<String, Object>> list_clips		= null;
	
	// message
	private final static int MSG_CLIP_LIST_DONE			= 101;
	private final static int MSG_CLIP_PLAY_DONE			= 102;
	private static final int MSG_UPDATE_PLAY_INFO 		= 201;
	private static final int MSG_UPDATE_RENDER_INFO		= 202;
	private static final int MSG_UPDATE_HTTP_LIST			= 203;
	private static final int MSG_FAIL_TO_LIST_HTTP_LIST	= 301;
	private static final int MSG_DISPLAY_SUBTITLE			= 401;
	private static final int MSG_HIDE_SUBTITLE			= 402;
	
	private ProgressDialog progDlg 				= null;
	
	private File mCurrentFolder					= null;
	
	private final static String home_folder		= "/test2";
	
	//private final static String H265_TEST_PLAYLINK = "http://127.0.0.1:9106/record.m3u8?type=pplive3&playlink=300146%3fft%3D2%26type%3dphone.android%26h265%3d2";

	private final static String HTTP_SERVER_URL = "http://172.16.204.106/test/testcase/";
	
	private final static String HTTP_UPDATE_APK_URL = "http://172.16.204.106/test/test/";
	
	private final String[] from = { "filename", "mediainfo", "folder", "filesize", "resolution", "thumb" };
	
	private final int[] to = { R.id.tv_filename, R.id.tv_mediainfo, R.id.tv_folder, 
			R.id.tv_filesize, R.id.tv_resolution, R.id.iv_thumb };
	
	private final boolean USE_BREAKPAD = false;
	private static boolean mRegisterBreakpad = false;
	
	// copy from PPTV code
	
	public static final String P2PType_CDN = "2";

    public static final String P2PType_CDNP2P = "1";

    public static final String P2PType_P2P = "0";

    /**
     * P2P type,茂哥提供，0：只是用P2P,1：CDN+P2P，2：CDN
     * 最新增加t参数，所以修改？bwtype为&bwtype。play接口会返回
     */
    public final static String P2PType = "bwtype=";

    /** 点播 */
    public static final String TYPE_PPVOD2 = "ppvod2";

    /** 直播 */
    public static final String TYPE_PPLIVE3 = "pplive3";
    
    public static final String TYPE_UNICOM = "ppliveunicom";


    private static final String HOST = "127.0.0.1";

    private static final String HTTP_MP4_RECORD_PPVOD2 = "http://" + HOST + ":%s/record.mp4?type=ppvod2&playlink=%s";

    private static final String HTTP_M3U8_RECORD_PPVOD2 = "http://" + HOST
            + ":%s/record.m3u8?type=ppvod2&playlink=%s&mux.M3U8.segment_duration=5";

    private static final String HTTP_M3U8_RECORD_PPVOD2_CHUNKED = HTTP_M3U8_RECORD_PPVOD2 + "&chunked=true";

    private static final String HTTP_M3U8_PLAY_PPLIVE3 = "http://" + HOST+":%s/play.m3u8?type=pplive3&playlink=%s";

    private static final String RTSP_ES_URL = "rtsp://" + HOST + ":%s/play.es?type=%s&playlink=%s";

    private static final String PPVOD2_URL = "ppvod2:///%s";

    private static final String PPLIVE3_URL = "pplive3:///%s";

    private static final String HTTP_MP4_PLAYINFO = "http://" + HOST + ":%s/playinfo.mp4";

    private static final String HTTP_MP4_MEDIAINFO_PPVOD2 = "http://" + HOST + ":%s/mediainfo.mp4?type=ppvod2&playlink=%s";

    private static final String HTTP_M3U8_CLOSE_URL = "http://" + HOST + ":%s/close";

    /** http端口 */
    private final static String PORT_HTTP = "http";

    /** rtsp端口 */
    private final static String PORT_RTSP = "rtsp";
	
	 /** 码流 */
    /** baseline */
    public static final int FT_BASELINE = 5;

    /** 流畅 */
    public static final int FT_LOW = 0;

    /** 高清 */
    public static final int FT_DVD = 1;

    /** 超清 */
    public static final int FT_HD = 2;

    /** 蓝光 */
    public static final int FT_BD = 3;

    public static final int FT_UNKNOWN = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLandscape;
		if (getResources().getConfiguration().orientation == 1) 
			isLandscape = false;
		else
			isLandscape = true;
		
		if(isLandscape)
			setContentView(R.layout.list_landscape);
		else
			setContentView(R.layout.list);
		
		Log.i(TAG, "Java: onCreate()");
		
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		if (isLandscape)
			preview_height = screen_height;
		else
			preview_height = screen_height * 2 / 5;
		Log.i(TAG, String.format("screen %dx%d, preview height %d", screen_width, screen_height, preview_height));
		
		this.btnPlay = (Button) findViewById(R.id.btn_play);
		this.btnSelectTime = (Button) findViewById(R.id.btn_select_time);
		this.btnClipLocation = (Button) findViewById(R.id.btn_clip_location);
		this.btnPlayerImpl = (Button) findViewById(R.id.btn_player_impl);
		this.btnPPboxSel = (Button) findViewById(R.id.btn_ppbox);
		this.btnTakeSnapShot = (Button) findViewById(R.id.btn_take_snapshot);
		this.btnSelectAudioTrack = (Button) findViewById(R.id.btn_select_audiotrack);
		this.et_play_url = (EditText) findViewById(R.id.et_url);
		this.et_playlink = (EditText) findViewById(R.id.et_playlink);
		this.et_ft = (EditText) findViewById(R.id.et_ft);
		this.et_bw_type = (EditText) findViewById(R.id.et_bw_type);

		this.mPreview = (MyPreView) findViewById(R.id.preview);
		this.mLayout = (RelativeLayout) findViewById(R.id.layout_preview);
		
		this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
		
		this.mMediaController = new MyMediaController(this);
		
		mControllerLayout = new LinearLayout(this);
		mControllerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		mControllerLayout.setOrientation(LinearLayout.VERTICAL);
		mTextViewInfo = new TextView(this);
		mTextViewInfo.setTextColor(Color.RED);
		mTextViewInfo.setTextSize(18);
		mTextViewInfo.setText("play info");
		mControllerLayout.addView(mTextViewInfo);
		addContentView(mControllerLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			mCurrentFolder = new File(Environment.getExternalStorageDirectory().getPath() + home_folder);
			if (!mCurrentFolder.isDirectory()) {
				mCurrentFolder = new File(Environment.getExternalStorageDirectory().getPath());
			}
			setTitle(mCurrentFolder.getAbsolutePath());
		}
		else {
			Toast.makeText(this, "sd card is not mounted!", Toast.LENGTH_SHORT).show();
		}
		
		if (USE_BREAKPAD && !mRegisterBreakpad)
        {
            try
            {
                BreakpadUtil.registerBreakpad(new File(getCacheDir().getAbsolutePath()));
                mRegisterBreakpad = true;
            }
            catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }
        }
		
		initFeedback();
		
		if (initMeetSDK() == false) {
			Toast.makeText(this, "failed to load meet lib", 
				Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		Util.startP2PEngine(this);
				
		mHolder = mPreview.getHolder();
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
		mHolder.addCallback(this);

		this.lv_filelist = (ListView) findViewById(R.id.lv_filelist);
		this.list_clips = new ArrayList<Map<String, Object>>();
		
		new LongOperation().execute("");
		
		this.lv_filelist
				.setOnItemClickListener(new ListView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View view,
							int position, long id) {
						// TODO Auto-generated method stub
						Log.i(TAG, String.format("onItemClick %d %d", position, id));
						
						HashMap item = (HashMap)lv_filelist.getItemAtPosition(position);
						String file_name = (String)item.get("filename");
						String file_path = (String)item.get("fullpath");
						Log.i(TAG, String.format("Java: full_path %s", file_path));
							
						if (file_name.equals("..")) {
							// up to parent folder
							String parent_folder;
							
							if (mListLocalFile) {
								parent_folder = mCurrentFolder.getParent();
								if (parent_folder != null) {
									mCurrentFolder = new File(parent_folder);
									setTitle(mCurrentFolder.getAbsolutePath());
									new LongOperation().execute("");
								}
							}
							else {
								list_http_server(file_path);
							}
						}
						else {
							if (file_path.startsWith("http")) {
								Log.i(TAG, "Java: http list file clicked");
								
								if (file_path.charAt(file_path.length() - 1) == '/') {
									Log.i(TAG, "Java: list http folder");
									list_http_server(file_path);		
								}
								else {
									Log.i(TAG, "Java: play http clip");
									play_type = MEET_PLAY_TYPE.HTTP_TYPE;
									start_player(file_path);
								}
							}
							else {
								File file = new File(file_path);
								
								if (file.isDirectory()) {
									File[] temp = file.listFiles();  
									if (temp == null || temp.length == 0) {  
										Toast.makeText(ClipListActivity.this, "folder is not valid or empty", 
											Toast.LENGTH_SHORT).show();  
									}
									else {
										Log.i(TAG, "Java: folder: " + file.getAbsolutePath());
										mCurrentFolder = file;
										setTitle(file.getAbsolutePath());
										new LongOperation().execute("");
									}
								}
								else {
									play_type = MEET_PLAY_TYPE.LOCAL_TYPE;
									start_player(file_path);
								}
							}
						}
					}
				});
				
		this.lv_filelist.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				switch (scrollState) {
		        case OnScrollListener.SCROLL_STATE_IDLE:
		        	mAdapter.SetScrolling(false);
		        	mAdapter.notifyDataSetChanged();
		        	Log.i(TAG, "set to false");
		            break;
		        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
		        	mAdapter.SetScrolling(true);
		        	Log.i(TAG, "set to true");
		            break;
		        case OnScrollListener.SCROLL_STATE_FLING:
		        	mAdapter.SetScrolling(true);
		        	Log.i(TAG, "set to true");
				}
			}
		});
		
		this.btnPlayerImpl.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				final String[] PlayerImpl = {"Auto", "System", "NuPlayer", "FFPlayer"};
				
				Dialog choose_player_impl_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select player impl")
				.setSingleChoiceItems(PlayerImpl, mPlayerImpl, /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							Log.i(TAG, "select player impl: " + whichButton);
							
							mPlayerImpl = whichButton;
							Toast.makeText(ClipListActivity.this, 
									"select type: " + PlayerImpl[whichButton], Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					})
				.create();
				choose_player_impl_dlg.show();	
			}
		});
		
		this.btnPPboxSel.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				final ArrayList<String> list_title = new ArrayList<String>();
				final ArrayList<String> list_url = new ArrayList<String>();
				
				list_title.add("阿森纳");
				list_title.add("韩国傻瓜");
				list_title.add("恋爱的技术");
				list_title.add("约会专家");
				list_title.add("恋爱相对论");
				list_title.add("后遗症");
				list_title.add("吉林卫视");
				list_title.add("新娱乐");
				list_title.add("东方电影");
				list_title.add("新闻综合");
				list_title.add("东方购物");
				list_title.add("电视剧");
				list_title.add("星尚");
				list_title.add("第一财经");
				
				final int fixed_size = list_title.size();

				LoadTvList(list_title, list_url);
				
				final String[] ppbox_clipname = (String[])list_title.toArray(new String[list_title.size()]);  
				
				final int ppbox_playlink[] = {18139131, 10110649, 17054339, 17461610, 17631361, 17611359, 
					300176, 300151, 300149, 300156, 300254, 300153, 300155, 300154};
				//final String ppbox_clipname[] = {"h265测试", "快速测试", "阿森纳", "韩国傻瓜", "恋爱的技术", "约会专家", "恋爱相对论", "后遗症", 
				//	"吉林卫视", "新娱乐" , "东方电影", "新闻综合", "东方购物", "电视剧", "星尚", "第一财经"};
				final int ppbox_type[] = {0, 0, 0, 0, 0, 0, 
					1, 1, 1, 1, 1, 1, 1, 1}; //0-vod.m3u8, 1-play.m3u8
				final int ppbox_ft[] = {2, 2, 2, 2, 3, 2, 
					1, 1, 1, 1, 1, 1, 1, 1};//2-超清, 3-bd
				
				Dialog choose_ppbox_res_dlg = new AlertDialog.Builder(ClipListActivity.this)
					.setTitle("Select ppbox program")
					.setSingleChoiceItems(ppbox_clipname, -1, /*default selection item number*/
						new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton) {
							if (whichButton < fixed_size) {
								et_playlink.setText(String.valueOf(ppbox_playlink[whichButton]));
								et_ft.setText(String.valueOf(ppbox_ft[whichButton]));
								if (0 == ppbox_type[whichButton]) {
									play_type = MEET_PLAY_TYPE.PPTV_VOD_TYPE;
								}
								else {
									play_type = MEET_PLAY_TYPE.PPTV_LIVE_TYPE;
								}
								
								Log.i(TAG, String.format("Java: choose %d %s %d", 
										whichButton, ppbox_clipname[whichButton], ppbox_playlink[whichButton]));
							}
							else {
								String url = list_url.get(whichButton - fixed_size);
								Log.i(TAG, String.format("Java: choose #%d title: %s, url: %s", 
										whichButton, list_title.get(whichButton), url));
								
								start_player(url);
							}
							
							dialog.cancel();
							
						}
					})
					.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog, int whichButton){
						}})
					.create();
				choose_ppbox_res_dlg.show();	
			}
		});			

		this.btnPlay.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				Log.i(TAG, "onClick play: " + et_play_url.getText().toString());
				int ppbox_playid, ppbox_ft, ppbox_bw_type;
				String tmp;
				tmp = et_playlink.getText().toString();
				ppbox_playid = Integer.parseInt(tmp);
				tmp = et_ft.getText().toString();
				ppbox_ft = Integer.parseInt(tmp);
				tmp = et_bw_type.getText().toString();
				ppbox_bw_type = Integer.parseInt(tmp);
				
				String ppbox_url;
				String str_playlink;
				str_playlink = addPlaylinkParam(Integer.toString(ppbox_playid), ppbox_ft, Integer.toString(ppbox_bw_type));

				try {
					str_playlink = URLEncoder.encode(str_playlink, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
				short port = MediaSDK.getPort(PORT_HTTP);
				Log.i(TAG, "Http port is: " + port);
				
				if (MEET_PLAY_TYPE.PPTV_VOD_TYPE == play_type ||
						MEET_PLAY_TYPE.LOCAL_TYPE == play_type ||
						MEET_PLAY_TYPE.HTTP_TYPE == play_type) {
					// vod
					ppbox_url = String.format(HTTP_M3U8_RECORD_PPVOD2, port, str_playlink);
					/*int index = ppbox_url.indexOf("record.m3u8");
					String StrtoEnc = ppbox_url.substring(index, ppbox_url.length());
					Log.i(TAG, "before base64 encode  " + StrtoEnc);
					String base64String = Base64.encodeToString(StrtoEnc.getBytes(), Base64.NO_WRAP);
					ppbox_url = String.format("http://127.0.0.1:%d/base64%s.m3u8", port, base64String);
					Log.i(TAG, "base64 encoded url:  " + ppbox_url);*/
				}
				else if (MEET_PLAY_TYPE.PPTV_LIVE_TYPE == play_type) {
					// live
					if (str_play_link_surfix == null || str_play_link_surfix.equals("")) {
						// real live
						ppbox_url = String.format(HTTP_M3U8_PLAY_PPLIVE3, port, str_playlink) + "&m3u8seekback=true"; // &chunked=true
					}
					else {
						// fake vod
						ppbox_url = String.format(HTTP_M3U8_PLAY_PPLIVE3, port, str_playlink) + str_play_link_surfix;
					}
				}
				else {
					Toast.makeText(ClipListActivity.this, "invalid play type: " + play_type, 
							Toast.LENGTH_SHORT).show();					
					return;
				}
				
				Log.i(TAG, "Java: toPlay: " + ppbox_url);
				
				start_player(ppbox_url);
			}
		});
		
		this.btnSelectTime.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(ClipListActivity.this, TimePickerActivity.class);
				startActivityForResult(intent, 1);
			}
		});
		
		this.btnClipLocation.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				// TODO Auto-generated method stub
				Log.i(TAG, "onClick toParent: ");
				
				if (mListLocalFile) {
					// switch to http list(maybe failed)
					list_http_server(HTTP_SERVER_URL);
				}
				else {
					// http->local always succeed
					new LongOperation().execute("");
					btnClipLocation.setText("http");
					mListLocalFile = true;
				}
			}
		});
		
		this.btnTakeSnapShot.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				// TODO Auto-generated method stub
				TakeSnapShot();
			}
		});
		
		btnSelectAudioTrack.setVisibility(View.VISIBLE);
		this.btnSelectAudioTrack.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				// TODO Auto-generated method stub
				if (mPlayer != null) {
					mAudioChannel++;
					if (mAudioChannel > mAudioTrackNum)
						mAudioChannel = 1;
					// fixme!
					mPlayer.selectTrack(mAudioChannel);
				}
			}
		});
	}
	
	private void LoadTvList(ArrayList<String> list_title, ArrayList<String> list_url) {
		String str_tvlist = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tvlist.txt";
		File file = new File(str_tvlist);
		if (file.exists()) {
		    FileInputStream fin = null;
		    
			try {
			    fin = new FileInputStream(file);
			    
			    byte[] buf = new byte[fin.available()];
			    
		    	fin.read(buf);
		    	String s = new String(buf);

			    int pos = 0;
			    while (true) {
			    	int comma = s.indexOf(',', pos);
			    	int newline = s.indexOf('\n', pos);
			    	if (comma == -1)
			    		break;
			    	if (newline == -1)
			    		newline = s.length();
			    	
			    	String title = s.substring(pos, comma);
			    	String url = s.substring(comma + 1, newline);
			    	Log.i(TAG, String.format("Java: filecontext title: %s url: %s", title, url));
			    	list_title.add(title);
			    	list_url.add(url);
			    	pos = newline + 1;
			    }
			    
			    fin.close();
			      
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void TakeSnapShot() {
		if (mPlayer == null) {
			Toast.makeText(this, "play is not playing", 
				Toast.LENGTH_SHORT).show();
			return;
		}
	
		Log.i(TAG, "Java: begin to get snapshot");
		long begin_time = System.currentTimeMillis();
		long elapsed;
		
		Bitmap bmp = mPlayer.getSnapShot(320, 240, 0, -1);
		if (null == bmp) {
			Toast.makeText(ClipListActivity.this, "failed to get snapshot", 
				Toast.LENGTH_SHORT).show();
				return;
		}
		
		elapsed = System.currentTimeMillis() - begin_time;
		Log.i(TAG, String.format("Java: use %d msec to get snapshot, begin to save png", elapsed));
		
		begin_time = System.currentTimeMillis();
		String save_folder = Environment.getExternalStorageDirectory().getPath() + "/test2/snapshot/";
		File folder = new File(save_folder);
		if (!folder.exists()) {
			folder.mkdir();
		}
		
		Date date = new Date(begin_time);
		SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
		String timestring = dateFmt.format(date);
		
		String picName = String.format("%s_%d.jpg", timestring, begin_time);
		File f = new File(save_folder, picName);
		if (f.exists()) {
		   f.delete();
		}
		try {
		   FileOutputStream out = new FileOutputStream(f);
		   bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
		   out.flush();
		   out.close();
		   elapsed = System.currentTimeMillis() - begin_time;
		   Log.i(TAG, String.format("Java: use %d msec to save picture", elapsed));
		   Toast.makeText(ClipListActivity.this, "picture: " + save_folder + picName + " saved", 
				Toast.LENGTH_SHORT).show();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){//判断父activity中的哪个按钮
            case 1://如果是按钮1
            	break;
            case 2://如果是按钮2
            	break;
            default:
            	break;
        }
        
        switch(resultCode){//判断是哪个子activity
            case 1://如果是子activity1
            	String strStartTime = intent.getStringExtra("start_time");
            	long start_time = intent.getLongExtra("start_time_sec", 0);
                int duration = intent.getIntExtra("duration_min", 60);
                Log.i(TAG, String.format("activity return result: %s %d(%d min)", strStartTime, start_time, duration));
                
                str_play_link_surfix = String.format("&begin_time=%d&end_time=%d", start_time, start_time + duration * 60);
                try {
                	str_play_link_surfix = URLEncoder.encode(str_play_link_surfix, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	break;
            case 2://如果是子activity2
            	break;
            default:
            	break;
        }
	}
	
	private void list_http_server(final String http_url) {
		Log.i(TAG, "Java list_http_server: " + http_url);
		setTitle(http_url);	
		
		new Thread(){  
			@Override  
			public void run() {  
				// TODO Auto-generated method stub  
				super.run();
				
				try {
					ApacheURLLister lister = new ApacheURLLister();
					URL url;
					url = new URL(http_url);
					mHttpFileList = lister.listFiles(url); //listAll
					for(int i = 0; i < mHttpFileList.size(); i++) {
						URL full_path = (URL)mHttpFileList.get(i);
						Log.i(TAG, "http file: " + full_path.toString());
					}
					
					mHttpFolderList = lister.listDirectories(url);
					for(int i = 0; i < mHttpFolderList.size(); i++) {
						URL full_path = (URL)mHttpFolderList.get(i);
						Log.i(TAG, "http folder: " + full_path.toString());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					mHandler.sendEmptyMessage(MSG_FAIL_TO_LIST_HTTP_LIST);
					return;
				}
				
				mHandler.sendEmptyMessage(MSG_UPDATE_HTTP_LIST);  
			}  
		}.start();
	}
	
	private int start_player(String path) {
		mPlayUrl = path;
		
		setTitle(path);
		Log.i(TAG, "Java: clipname: " + mPlayUrl);
		
		DecodeMode dec_mode = DecodeMode.UNKNOWN;
		if (0 == mPlayerImpl) {
			dec_mode = DecodeMode.AUTO;
		}
		else if (1 == mPlayerImpl) {
			dec_mode = DecodeMode.HW_SYSTEM;
		}
		else if (2 == mPlayerImpl) {
			boolean canPlay = false;
			String str_ext;
			String fileName;
			int index;
			index = path.indexOf("http://");
			if (-1 != index)
				canPlay = true;
				
			index = path.lastIndexOf("/");
			fileName = path.substring(index + 1, path.length());
			index = fileName.lastIndexOf(".");
			if (index != -1) {
				str_ext = fileName.substring(index + 1, fileName.length());
				if (str_ext.equals("mp4") || str_ext.equals("flv") || str_ext.equals("mov"))
					canPlay = true;
			}
			
			if (canPlay == false) {
				Toast.makeText(ClipListActivity.this, "Nu Player cannot play: " + fileName, 
					Toast.LENGTH_SHORT).show();
				return -1;
			}
			
			dec_mode = DecodeMode.SW;
		}									
		else if (3 == mPlayerImpl) {
			dec_mode = DecodeMode.SW;
		}
		else if (4 == mPlayerImpl) {
			dec_mode = DecodeMode.SW;
		}
		else {
			Toast.makeText(ClipListActivity.this, "invalid player implement: " + Integer.toString(mPlayerImpl), 
				Toast.LENGTH_SHORT).show();
			return -1;
		}
		
		//Toast.makeText(ClipListActivity.this, mIsOMXSurface ? "is omx" : "not omx", Toast.LENGTH_SHORT).show();
		
		if (mPlayer != null) {
			mStoped = true;
			if (mIsSubtitleUsed) {
				mSubtitleThread.interrupt();
				
				try {
					mSubtitleThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
			
			// fix last video is still in buffering state
			if (mIsBuffering) {
				mBufferingProgressBar.setVisibility(View.GONE);
				mIsBuffering = false;
			}
		}
		
		if (!mIsPreview) {
			//Uri uri = Uri.fromFile(file);
			Uri uri = Uri.parse(path);
			Log.i(TAG, "Java: goto PlayerActivity, uri:" + uri.toString());
			start_fullscreen_play(uri, mPlayerImpl);
		}
		else {
			MediaInfo info;
			File file = new File(path);
			if (file.exists()) {
				info = MeetSDK.getMediaDetailInfo(file);
				if (info != null) {
					ArrayList<TrackInfo> audioTrackList = info.getAudioChannelsInfo();
					for (TrackInfo trackInfo : audioTrackList) {
						Log.i(TAG, String.format("Java： audio Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s", 
							trackInfo.getStreamIndex(), 
							trackInfo.getId(), 
							trackInfo.getCodecName(), 
							trackInfo.getLanguage(),
							trackInfo.getTitle()));
					}
					
					if (info.getAudioChannels() > 1)
						btnSelectAudioTrack.setVisibility(View.VISIBLE);
					else
						btnSelectAudioTrack.setVisibility(View.INVISIBLE);
					
					ArrayList<TrackInfo> subtitleTrackList = info.getSubtitleChannelsInfo();
					for (TrackInfo trackInfo : subtitleTrackList) {
						Log.i(TAG, String.format("Java： subtitle Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s", 
							trackInfo.getStreamIndex(), 
							trackInfo.getId(), 
							trackInfo.getCodecName(), 
							trackInfo.getLanguage(),
							trackInfo.getTitle()));
					}
				}
				
			}
			else {
				btnSelectAudioTrack.setVisibility(View.INVISIBLE);
			}
			
			if (DecodeMode.AUTO == dec_mode) {
				dec_mode = MeetSDK.getPlayerType(mPlayUrl);
				Log.i(TAG, "Java: dec_mode " + dec_mode.toString());
			}
			
			// force refresh a new surface
			mPreview.setVisibility(View.INVISIBLE);
			
			if (DecodeMode.HW_SYSTEM == dec_mode) {
				mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			}
			else if (DecodeMode.SW == dec_mode){
				mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
				mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
			}
			
			mPreview.setVisibility(View.VISIBLE);
			
			mPlayer = new MediaPlayer(dec_mode);
			
			mPreview.BindInstance(mMediaController, mPlayer);
			
			// fix Mediaplayer setVideoSurfaceTexture failed: -17
			mPlayer.setDisplay(null);
			mPlayer.reset();

			mPlayer.setDisplay(mPreview.getHolder());
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setScreenOnWhilePlaying(true);
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnVideoSizeChangedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnErrorListener(this);
			mPlayer.setOnBufferingUpdateListener(this);
			mPlayer.setOnInfoListener(this);

			mStoped = false;
			mHomed = false;
			
			boolean succeed = true;
			try {
				mPlayer.setDataSource(path);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				succeed = false;
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				succeed = false;
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				succeed = false;
				e.printStackTrace();
			}
			
			try {
				mPlayer.prepareAsync();
			}
			catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				succeed = false;
				e.printStackTrace();
				Log.e(TAG, "Java: prepareAsync() exception: " + e.getMessage());
			}
			
			if (succeed) {
				mBufferingProgressBar.setVisibility(View.VISIBLE);
				mIsBuffering = true;
			}
			else {
				Toast.makeText(this, "Java: failed to play: " + path, Toast.LENGTH_SHORT).show();
			}
		}
		
		return 0;
	}
	
	private void ListHttpMediaInfo(List folderList, List filelist) {
		Log.i(TAG, "Java: ListHttpMediaInfo");

		list_clips.clear();
		
		HashMap<String, Object> parent_folder = new HashMap<String, Object>();
		parent_folder.put("filename", "..");
		parent_folder.put("filesize", "N/A");
		parent_folder.put("resolution", "N/A");
		parent_folder.put("fullpath", HTTP_SERVER_URL);
		parent_folder.put("thumb", R.drawable.folder);
		list_clips.add(parent_folder);
		
		for (int i = 0; i < folderList.size(); i++) {
			URL url = (URL)folderList.get(i);
			String clip_fullpath = url.toString();
			Log.i(TAG, "http folder: " + clip_fullpath);
			
			int index = clip_fullpath.lastIndexOf("/", clip_fullpath.length() - 2);
			String foldername;
			foldername = clip_fullpath.substring(index + 1, clip_fullpath.length() - 1).toString();
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("filename", foldername);
			map.put("mediainfo", "N/A");
			map.put("folder", HTTP_SERVER_URL);
			map.put("filesize", "N/A");
			map.put("modify", "N/A");
			map.put("resolution", "N/A");
			map.put("fullpath", clip_fullpath);
			map.put("thumb", R.drawable.folder);

			Log.i(TAG, "folder: " + foldername + " added to list");
			list_clips.add(map);
		}
		
		for (int i = 0; i < filelist.size(); i++) {
			URL url = (URL)filelist.get(i);
			String clip_fullpath = url.toString();
			Log.i(TAG, "http file: " + clip_fullpath);
			
			int index = clip_fullpath.lastIndexOf("/");
			String filename;
			filename = clip_fullpath.substring(index + 1, clip_fullpath.length()).toString();
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("filename", filename);
			map.put("mediainfo", "N/A");
			map.put("folder", HTTP_SERVER_URL);
			map.put("filesize", "N/A");
			map.put("modify", "N/A");
			map.put("resolution", "N/A");
			map.put("fullpath", clip_fullpath);
			map.put("thumb", R.drawable.http);

			Log.i(TAG, "video: " + filename + " added to list");
			list_clips.add(map);
		}
		
		//SimpleAdapter adapter = new SimpleAdapter(ClipListActivity.this, list_clips, R.layout.sd_list,
		//	from, to);
		mAdapter = new MyAdapter(ClipListActivity.this, list_clips, R.layout.sd_list,
			from, to);
		lv_filelist.setAdapter(mAdapter);
	}
	
	/////////////////////////////////////////////
    //implements MediaPlayerControl
	public void start() {
		if (mPlayer != null)
			mPlayer.start();
	}
	
	public void seekTo(int pos) {
		if (mPlayer != null)
			mPlayer.seekTo(pos);
		if (mSubtitleParser != null) {
			mSubtitleThread.interrupt();
			mSubtitleSeeking = true;
			
			mSubtitleParser.seekTo(pos);
		}
	}
	
	public void pause() {
		if (mPlayer != null)
			mPlayer.pause();
	}
	
	public boolean isPlaying() {
		if (mPlayer == null)
			return false;
			
		return mPlayer.isPlaying();
	}
	
	public int getDuration() {
		if (mPlayer == null)
			return 0;
		
		if (isLivePlay())
			return (1800 * 1000);
		
		return mPlayer.getDuration();
	}
	
	public int getCurrentPosition() {
		if (mPlayer == null)
			return 0;
		
		int pos = mPlayer.getCurrentPosition();
		if (isLivePlay()) {
			int new_duration = mPlayer.getDuration();
			int offset = new_duration - 1800 * 1000;
			pos -= offset;
			if (pos > 1800 * 1000)
				pos = 1800 * 1000;
		}
		
		Log.d(TAG, String.format("Java: getCurrentPosition %d %d msec", mPlayer.getCurrentPosition(), pos));
		
		return pos;
	}
	
	public int getBufferPercentage() {
		if (mPlayer == null)
			return 0;
			
		int pct, duration;
		duration = mPlayer.getDuration();
		if (duration == 0) // avoid divide by zero
			pct = 0;
		else 
			pct = (mPlayer.getCurrentPosition() + mPlayer.getBufferingTime()) * 100 / duration;
		Log.i(TAG, String.format("Java: getBufferPercentage: %d(%d, %d)", pct, mPlayer.getCurrentPosition(), mPlayer.getBufferingTime()));
		return pct;
	}

	public boolean canPause() {
		return true;
	}

	public boolean canSeekBackward() {
		return true;
	}

	public boolean canSeekForward() {
		return true;
	}
	
	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//end of : implements MediaPlayerControl
	//////////////////////////////////////////
	
	private boolean isLivePlay() {
		if (MEET_PLAY_TYPE.PPTV_LIVE_TYPE == play_type) {
			if (str_play_link_surfix == null || str_play_link_surfix.equals(""))
				return true;
		}
		
		return false;
	}
	
	private void close_hls() {
		short port = MediaSDK.getPort("http");
		String strCloseURL = String.format("127.0.0.1:%d/close", port);
		URL url;
		try {
			url = new URL(strCloseURL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			if (conn.getResponseCode()==200)
                Toast.makeText(this, "GET提交成功", Toast.LENGTH_SHORT).show();  
            else
            	Toast.makeText(this, "GET提交失败", Toast.LENGTH_SHORT).show();  
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Handler mHandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
			case MSG_CLIP_LIST_DONE:
				break;
			case MSG_UPDATE_PLAY_INFO:
			case MSG_UPDATE_RENDER_INFO:
				mTextViewInfo.setText(String.format("%02d|%03d av:%+04d fps/msec %d(%d)/%d(%d)\n %d kbps", 
					render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec, 
					decode_fps, decode_avg_msec, render_fps, render_avg_msec,
					video_bitrate));
				break;
			case MSG_CLIP_PLAY_DONE:
				Toast.makeText(ClipListActivity.this, "clip completed", Toast.LENGTH_SHORT).show();
				mTextViewInfo.setText("play info");
				break;
			case MSG_UPDATE_HTTP_LIST:
				ListHttpMediaInfo(mHttpFolderList, mHttpFileList);
				btnClipLocation.setText("local");
				mListLocalFile = false;
				break;
			case MSG_FAIL_TO_LIST_HTTP_LIST:
				Toast.makeText(ClipListActivity.this, "failed to connect to http server", 
					Toast.LENGTH_SHORT).show();
				btnClipLocation.setText("http");
				break;
			case MSG_DISPLAY_SUBTITLE:
				mSubtitleTextView.setText(mSubtitleText);
				break;
			case MSG_HIDE_SUBTITLE:
				mSubtitleTextView.setText("");
				break;
			default:
				Log.w(TAG, "unknown msg.what " + msg.what);
				break;
			}			 
        }
	}; 
	
	private class LongOperation extends AsyncTask<String, Void, String> {
		class FileComparator implements Comparator<File> {
			@Override
			public int compare(File f1, File f2) {
				if (f1.isFile() && f2.isDirectory())
					return 1;
				if (f2.isFile() && f1.isDirectory())
					return -1;
					
				String s1=f1.getName().toString().toLowerCase();
				String s2=f2.getName().toString().toLowerCase();
				return s1.compareTo(s2);
		    }
		}
		
		class FilePathComparator implements Comparator<String> {
			@Override
			public int compare(String path1, String path2) {
					
				String s1=path1.toLowerCase();
				String s2=path2.toLowerCase();
				return s1.compareTo(s2);
		    }
		}
		
        @Override
        protected String doInBackground(String... params) {
			if (mCurrentFolder != null) {
				File[] files = mCurrentFolder.listFiles();
				Arrays.sort(files, new FileComparator());
				ListMediaInfo(files);
			}
			
            Log.i(TAG, "Java: Long Operation done.");
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
			mAdapter = new MyAdapter(ClipListActivity.this, list_clips, R.layout.sd_list,
				from, to);
			lv_filelist.setAdapter(mAdapter);
			progDlg.dismiss();
        }

        @Override
        protected void onPreExecute() {
			progDlg = new ProgressDialog(ClipListActivity.this);
			//progDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			//progDlg.setTitle("MeetPlayer");
			progDlg.setMessage("Loading clips");
			progDlg.setCancelable(false);
			progDlg.show(); 
        }

        @Override
        protected void onProgressUpdate(Void... values) {
			
        }
    }

	private void start_fullscreen_play(Uri uri, int player_impl) {
		Log.i(TAG, "java: start_fullscreen_play");

		Intent intent = new Intent(ClipListActivity.this,
				VideoPlayerActivity.class);
		Log.i(TAG, "to play uri: " + uri.toString());

		intent.setData(uri);
		intent.putExtra("impl", player_impl);
		startActivity(intent);
	}

	private String msecToString(long msec)
	{
		long msec_, sec, minute, hour, tmp;
		msec_ = msec % 1000;
		sec = msec / 1000;
		
		// sec = 3710
		tmp = sec % 3600; // 110
		hour = sec / 3600; // 1
		sec = tmp % 60; // 50
		minute = tmp / 60; // 1
		return String.format("%02d:%02d:%02d:%03d", hour, minute, sec, msec_);
	}
	
	private String addPlaylinkParam(String playlink, int ft, String bwt) {
    	playlink += "?ft=" + ft;
    	playlink += "&bwtype=" + bwt;
    	
    	playlink += "&platform=android3";
        playlink += "&type=phone.android.vip";
        playlink += "&sv=4.0.1";
		playlink += "&param=userType%3D1"; // fix cannot find blue-disk ft problem
    	
    	return playlink;
    }
	
	private void ListMediaInfo(File[] files) {
		Log.i(TAG, "Java: ListMediaInfo");

		list_clips.clear();
		
		if (home_folder.equals("")) {
			String[] thumbColumns = new String[]{
					MediaStore.Video.Thumbnails.DATA,
					MediaStore.Video.Thumbnails.VIDEO_ID
			};
			
			String[] mediaColumns = new String[]{
					MediaStore.Video.Media.DATA,
					MediaStore.Video.Media._ID,
					MediaStore.Video.Media.TITLE,
					MediaStore.Video.Media.MIME_TYPE,
					MediaStore.Video.Media.DURATION,
					MediaStore.Video.Media.SIZE
			};
			
			ContentResolver cr = getContentResolver();  //cr.query
	        Cursor cur = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumns,  
	                null, null, null);  
	        if (cur == null) {
	        	Toast.makeText(getApplicationContext(), "no cursor", Toast.LENGTH_SHORT).show();
				return;
			}
			
			final int displayNameId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
			final int dataId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			final int durationId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
			final int sizeId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
				String title = cur.getString(displayNameId);
				long duration = cur.getLong(durationId);
				long size = cur.getLong(sizeId);
				String path = cur.getString(dataId);
				
				// bypass pptv folder
				if (path.indexOf("/pptv/") != -1)
					continue;
				
				File file = new File(path);
				if (!file.exists()) {
					Log.w(TAG, "failed to open file: " + path);
					continue;
				}
				
				Log.i(TAG, String.format("title: %s, path %s, duration: %d, size %d", title, path, duration, size));
				
				String filesize;
				if (size > ONE_MAGEBYTE)
					filesize = String.format("%.3f MB",
							(float) size / (float) ONE_MAGEBYTE);
				else if (size > ONE_KILOBYTE)
					filesize = String.format("%.3f kB",
							(float) size / (float) ONE_KILOBYTE);
				else
					filesize = String.format("%d Byte", size);
					
				long modTime = file.lastModified();
				
				MediaInfo info = MeetSDK.getMediaDetailInfo(file);
				
				String string_res;

				if (info != null) {
					string_res = String.format("%dx%d %s", 
							info.getWidth(), info.getHeight(), msecToString(info.getDuration()));
					Log.d(TAG, "Java: media info: " + string_res);

					int index = path.lastIndexOf(".");
					String s;
					if (index != -1)
						s = path.substring(0, index).toString();
					else
						s = path;
				}
				else {
					string_res = "N/A";
					Log.w(TAG, "video: " + path + " cannot get media info");
				}
				
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("filename", title);
				map.put("mediainfo", QueryMediaInfo(file));
				map.put("folder", GetFileFolder(path));
				map.put("filesize", filesize);
				map.put("modify", dateFormat.format(new Date(modTime)));
				map.put("resolution", string_res);
				map.put("fullpath", path);
				map.put("thumb", path/*R.drawable.clip*/);
				
				Log.i(TAG, "video: " + title + " added to list");
				list_clips.add(map);
			}
		}
		else {
			// add parent folder ".." line
			HashMap<String, Object> parent_folder = new HashMap<String, Object>();
			parent_folder.put("filename", "..");
			parent_folder.put("mediainfo", "N/A");
			parent_folder.put("folder", "N/A");
			parent_folder.put("filesize", "N/A");
			parent_folder.put("resolution", "N/A");
			parent_folder.put("fullpath", "..");
			parent_folder.put("thumb", R.drawable.folder);
			list_clips.add(parent_folder);
			
			if (files != null) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");

				for (File file : files) {
					String fileName = file.getName();
					if (fileName.endsWith("srt") || fileName.endsWith("ass"))
						continue;
					
					if (file.isFile() && !file.isHidden())
					{
						long modTime = file.lastModified();

						String filesize;
						if (file.length() > ONE_MAGEBYTE)
							filesize = String.format("%.3f MB",
									(float) file.length() / (float) ONE_MAGEBYTE);
						else if (file.length() > ONE_KILOBYTE)
							filesize = String.format("%.3f kB",
									(float) file.length() / (float) ONE_KILOBYTE);
						else
							filesize = String.format("%d Byte", file.length());

						MediaInfo info = MeetSDK.getMediaDetailInfo(file);

						if (info != null) {
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("filename", file.getName());
							map.put("mediainfo", QueryMediaInfo(file));
							map.put("folder", GetFileFolder(file.getAbsolutePath()));
							map.put("filesize", filesize);
							map.put("modify", dateFormat.format(new Date(modTime)));
							
							String string_res = String.format("%dx%d %s", 
									info.getWidth(), info.getHeight(), msecToString(info.getDuration()));
							map.put("resolution", string_res);
							Log.i(TAG, "Java: media info: " + string_res);
							
							map.put("fullpath", file.getAbsolutePath());
							map.put("thumb", file.getAbsolutePath()/*R.drawable.clip*/);

							int index = fileName.lastIndexOf(".");
							String s;
							if (index != -1)
								s = fileName.substring(0, index).toString();
							else
								s = fileName;
							
							Log.i(TAG, "video: " + fileName + " added to list");
							list_clips.add(map);
						}
						else {
							Log.w(TAG, "video: " + fileName + " cannot get media info");
						}
					}
					else if(file.isDirectory() && !file.isHidden()) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put("filename", file.getName());
						map.put("mediainfo", "N/A");
						map.put("folder", "N/A");
						map.put("filesize", "N/A");
						map.put("modify", "N/A");
						map.put("resolution", "N/A");
						map.put("fullpath", file.getAbsolutePath());
						map.put("thumb", R.drawable.folder);

						int index = fileName.lastIndexOf(".");
						String s;
						if (index != -1)
							s = fileName.substring(0, index).toString();
						else
							s = fileName;
						
						Log.i(TAG, "video: " + fileName + " added to list");
						list_clips.add(map);
					}
				}
			} else {
				Log.e(TAG, "file path is empty");
			}
		}
	}
	
	private String QueryMediaInfo(File file) {
		if(file == null || !file.exists())
			return "N/A";
		
		MediaInfo info = MeetSDK.getMediaDetailInfo(file);
		
		StringBuffer sbMediaInfo = new StringBuffer();

		sbMediaInfo.append("ext:");
		sbMediaInfo.append(GetFileExt(file.getAbsolutePath()));
		
		if (info == null) {
			Log.w(TAG, "video: " + file.getAbsolutePath() + " cannot get media info");
			return sbMediaInfo.toString();
		}
		sbMediaInfo.append(", f:");
		String strFormat = info.getFormatName();
		if (strFormat.length() > 6)
			strFormat = strFormat.substring(0, 6);
		sbMediaInfo.append(strFormat);
		
		if (info.getVideoCodecName() != null) {
			sbMediaInfo.append(", v:");
			sbMediaInfo.append(info.getVideoCodecName());
		}
		
		ArrayList<TrackInfo> audiolist = info.getAudioChannelsInfo();
		if (audiolist.size() > 0) {
			sbMediaInfo.append(", a:");
		}
		for (int i=0;i<audiolist.size();i++) {
			TrackInfo item = audiolist.get(i);
			sbMediaInfo.append(item.getCodecName());
			sbMediaInfo.append("(");
			
			if(item.getTitle() != null) {
				sbMediaInfo.append(item.getTitle());
			}
			else if(item.getLanguage() != null) {
				sbMediaInfo.append(item.getLanguage());
			}
			else {
				sbMediaInfo.append("默认");
			}
			sbMediaInfo.append(")|");
		}
		ArrayList<TrackInfo> subtitlelist = info.getSubtitleChannelsInfo();
		if (subtitlelist.size() > 0) {
			sbMediaInfo.append(", s:");
		}
		for (int i=0;i<subtitlelist.size();i++) {
			TrackInfo item = subtitlelist.get(i);
			sbMediaInfo.append(item.getCodecName());
			sbMediaInfo.append("(");
			
			if(item.getTitle() != null) {
				sbMediaInfo.append(item.getTitle());
			}
			else if(item.getLanguage() != null) {
				sbMediaInfo.append(item.getLanguage());
			}
			else {
				sbMediaInfo.append("默认");
			}
			sbMediaInfo.append(")|");
		}
		
		return sbMediaInfo.toString();
	}
	
	private String GetFileExt(String path) {
		String file_ext;
		int pos;
		pos = path.lastIndexOf(".");
		if (pos == -1) {
			file_ext = "N/A";
		}
		else {
			file_ext = path.substring(pos + 1);
		}
		
		return file_ext;
	}
	
	private String GetFileFolder(String path) {
		String file_folder;
		int pos1, pos2;
		pos1 = path.lastIndexOf('/');
		pos2 = path.lastIndexOf('/', pos1 - 1);
		if (pos2 == -1) {
			file_folder = "N/A";
		}
		else {
			file_folder = path.substring(pos2 + 1, pos1);
		}
		
		return file_folder;
	}

	private void upload_crash_report(int type)
	{	
		MeetSDK.makePlayerlog();
		
		FeedBackFactory fbf = new FeedBackFactory(
				 Integer.toString(type), "123456", false, false);
		fbf.asyncFeedBack();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		int i = Menu.FIRST;
		menu.add(i, UPDATE_CLIP_LIST, Menu.NONE, "Update list")
			.setIcon(R.drawable.list);
		menu.add(i, UPDATE_APK, Menu.NONE, "Update apk")
			.setIcon(R.drawable.update);
		menu.add(i, UPLOAD_CRASH_REPORT, Menu.NONE, "Upload crash report")
			.setIcon(R.drawable.log);
		menu.add(i, QUIT, Menu.NONE, "Quit");
		
		SubMenu fileSubMenu = menu.addSubMenu(i, OPTION, Menu.NONE, "Option");
		fileSubMenu.setIcon(R.drawable.option);
		MenuItem newMenuItem = fileSubMenu.add(i, OPTION_PREVIEW, Menu.NONE, "Preview");
		newMenuItem.setCheckable(true);
		if (mIsPreview)
			newMenuItem.setChecked(true);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		/*
		 * int id = item.getItemId(); if (id == R.id.action_settings) { return
		 * true; } return super.onOptionsItemSelected(item);
		 */
		int id = item.getItemId();
		switch (id) {
		case UPDATE_CLIP_LIST:
			if (mListLocalFile) 
				new LongOperation().execute("");
			break;
		case UPLOAD_CRASH_REPORT:
			upload_crash_report(3);
			break;
		case UPDATE_APK:
			Log.i(TAG, "update apk");
			setupUpdater();
			break;
		case QUIT:
			this.finish();
			break;
		case OPTION_PREVIEW:
			if (mIsPreview)
				item.setChecked(false);
			else
				item.setChecked(true);
			mIsPreview = !mIsPreview;
			break;
		default:
			Log.w(TAG, "bad menu item selected: " + id);
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Java: onInfo: " + what + " " + extra);
		
		if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
			mBufferingProgressBar.setVisibility(View.VISIBLE);
			mIsBuffering = true;
			Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_START");
		}
		else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
			mBufferingProgressBar.setVisibility(View.GONE);
			mIsBuffering = false;
			Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_END");
		}		
		else if (MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE == what) {
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
			Toast.makeText(ClipListActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
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
		
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onError: " + framework_err + "," + impl_err);
		Toast.makeText(ClipListActivity.this, String.format("failed to play clip: %d %d", framework_err, impl_err), Toast.LENGTH_SHORT).show();
		
		if (mIsBuffering) {
			mBufferingProgressBar.setVisibility(View.GONE);
			mIsBuffering = false;
		}
		
		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
		
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			upload_crash_report(2);
		
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onCompletion");
		mPlayer.stop();
		mMediaController.hide();
		mHandler.sendEmptyMessage(MSG_CLIP_PLAY_DONE);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPrepared");
		
		render_frame_num = 0;
		decode_drop_frame = 0;
		
		//if (mListLocalFile)
		//	mPlayer.setLooping(true);
		
		Log.i(TAG, String.format("Java: width %d, height %d", mPlayer.getVideoWidth(), mPlayer.getVideoHeight()));
		mPlayer.start();
		
		attachMediaController();
		
		mBufferingProgressBar.setVisibility(View.GONE);
		mIsBuffering = false;
		
		// subtitle
		if (mPlayUrl.startsWith("/")) {
			// local file
			String subtitle_full_path;
			int index = mPlayUrl.lastIndexOf('.') + 1;
			String tmp = mPlayUrl.substring(0, index);
			
			String[] exts = {"srt", "ass"};
			for(String ext:exts) {
				subtitle_full_path = tmp + ext;
				
				File subfile = new File(subtitle_full_path);
				Log.i(TAG, "Java: subtitle: subtitle file: " + subtitle_full_path);
		        if (subfile.exists()) {
		        	Log.i(TAG, "Java: subtitle: subtitle file found: " + subtitle_full_path);
		        	
					mSubtitleParser = new SimpleSubTitleParser();
					mSubtitleParser.setOnPreparedListener(this);
					
					mSubtitleParser.setDataSource(subtitle_full_path);
					mSubtitleParser.prepareAsync();
					break;
		        }
			}
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onBufferingUpdate: " + percent);
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
		// TODO Auto-generated method stub
		
		if (w == 0 || h == 0) {
			mHolder.setFixedSize(640, 480);
			mPreview.SetVideoRes(640, 480);
			Log.i(TAG, "Java: onVideoSizeChanged, no video stream, use default resolution: 640x480");
		}
		else {
			mHolder.setFixedSize(w, h);
			mPreview.SetVideoRes(w, h);
		}
		
		mPreview.measure(MeasureSpec.AT_MOST, MeasureSpec.AT_MOST/*EXACTLY*/);
	}
	
	private void setupUpdater() {
//		final String codec = MediaPlayer.getBestCodec("/data/data/com.pplive.meetplayer/");
		final String apk_name = "MeetPlayer-debug.apk";
		Log.d(TAG, "ready to download apk: " + apk_name);
		if (null != apk_name && apk_name.length() > 0) {
			mDownloadProgressBar = (ProgressBar) findViewById(R.id.progressbar_download);
			mProgressTextView = (TextView) findViewById(R.id.textview_progress);
			
			AlertDialog.Builder builder = 
				new AlertDialog.Builder(this); 
			
			builder.setMessage("Download");

			builder.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						downloadApk(apk_name);
					}
				});

			builder.setNeutralButton("No",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});

			mUpdateDialog = builder.create();
			mUpdateDialog.show();
		}
	}
	
	private void downloadApk(String apk_name) {
		final String path;
		
		String url = HTTP_UPDATE_APK_URL + apk_name;
		path = Environment.getExternalStorageDirectory().getPath() + home_folder + "/" + apk_name;
		Log.i(TAG, "to download apk: " + path);
		
		DownloadAsyncTask downloadTask = new DownloadAsyncTask() {

			final String format = getResources().getString(R.string.format_progress);

			@Override
			protected void onPreExecute() {
				mDownloadProgressBar.setVisibility(View.VISIBLE);
				mProgressTextView.setVisibility(View.VISIBLE);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				String msg = null;
				if (result) {
					msg = MSG_DOWNLOAD_SUCCESS;
				} else {
					msg = MSG_DOWNLOAD_FAILED;
				}

				mProgressTextView.setText("");
				mProgressTextView.setVisibility(View.GONE);
				mDownloadProgressBar.setVisibility(View.GONE);

				Toast toast = 
					Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
				toast.show();
				
				if (result) {
					installApk(path);
				}
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				int progress = values[0];
				mProgressTextView.setText(String.format(format, progress));
			}
		};
		
		downloadTask.execute(url, path);
	}
	
	private void installApk(String apk_fullpath)
    {
		Log.i(TAG, "installApk: " + apk_fullpath);
		
        File apkfile = new File(apk_fullpath);
        if (!apkfile.exists()) {
        	Log.e(TAG, "apk file does not exist: " + apk_fullpath);
            return;
        }
			
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apk_fullpath)), "application/vnd.android.package-archive");
        startActivity(intent);
    }

	@Override
	protected void onResume() {
		super.onResume();
		
		Log.i(TAG, "Java: onResume()");
		
		//mQuit = false;
		
		mLayout.getLayoutParams().height = preview_height;
		mLayout.requestLayout(); //or invalidate();
		
		Log.i(TAG, "onResume()");
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Java: onPause()");

		if(mPlayer != null) {
			mPlayer.pause();
			//mPlayer.suspend();
		}
			
		//MeetSDK.closeLog();
		
		//MediaSDK.stopP2PEngine();
	}

	@Override
	protected void onStop() {
		super.onStop();

		Log.i(TAG, "Java: onStop()");

		if (isFinishing()) {
			if (mPlayer != null) {
				//mQuit = true;
				mStoped = true;
				
				if (mIsSubtitleUsed) {
					mSubtitleThread.interrupt();
					
					try {
						mSubtitleThread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				mPlayer.stop();
			}
		}
		else {
			mHomed = true;
		}
	}
	
	private boolean initMeetSDK() {
		// upload util will upload /data/data/pacake_name/Cache/xxx
		// so must NOT change path
		MeetSDK.setLogPath(
				getCacheDir().getAbsolutePath() + "/meetplayer.log", 
				getCacheDir().getParentFile().getAbsolutePath() + "/");
		// /data/data/com.svox.pico/
		return MeetSDK.initSDK(this, "");
	}
	
	private void initFeedback() {
		LogcatHelper helper = LogcatHelper.getInstance();
		helper.init(this);
		AtvUtils.sContext = this;
		FeedBackFactory.sContext = this;
	}
	
    private void attachMediaController() {
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(mPreview);
        //mMediaController.setPadding(0, 0, 0, 0);
        mMediaController.setEnabled(true);
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: surfaceChanged()");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(TAG, String.format("Java: surfaceCreated() %s", holder.toString()));
		if (mPlayer != null && mHomed) {
			mPlayer.setDisplay(holder);
			mPlayer.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: surfaceDestroyed()");
	}
	
	// callback of subtitle
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
        
        while (true) {
        	if (/*mQuit || */mStoped)
                break;
        	
        	if (isDisplay) {
        		seg = mSubtitleParser.next();
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
    			target_msec = mPlayer.getDuration();
        	}
        
            while (mPlayer.getCurrentPosition() < target_msec/* || mSubtitleSeeking == true*/) {
            	if (isDropItem == true) {
            		if (mSubtitleSeeking == false) {
            			break;
            		}
            	}
            	
            	try {
					wait(SLEEP_MSEC);
					Log.i(TAG, "Java: subtitle wait");
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

	@Override
	public void onSeekComplete() {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: subtitle onSeekComplete");
		mSubtitleSeeking = false;
	}
	// end of "callback of subtitle"
	
	private final class MyMediaController extends MediaController {

	    private MyMediaController(Context context) {
	        super(new ContextThemeWrapper(context, R.style.MyPlayerTheme));
	    }
	}
	
	static {
		//System.loadLibrary("lenthevcdec");
	}
	
}
