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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.ListAdapter;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import com.pplive.meetplayer.util.CrashHandler;
import com.pplive.meetplayer.util.DownloadAsyncTask;
import com.pplive.meetplayer.util.EPGUtil;
import com.pplive.meetplayer.util.FeedBackFactory;
import com.pplive.meetplayer.util.IDlnaCallback;
import com.pplive.meetplayer.util.ListMediaUtil;
import com.pplive.meetplayer.util.LoadPlayLinkUtil;
import com.pplive.meetplayer.util.LogcatHelper;
import com.pplive.meetplayer.util.Util;
import com.pplive.dlna.DLNASdk;








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

import com.pplive.sdk.MediaSDK;
import com.pplive.thirdparty.BreakpadUtil;

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
	private MySimpleMediaController mMediaController;
	private RelativeLayout mLayout;
	private ProgressBar mBufferingProgressBar;
	private EditText et_playlink;
	private Button btn_ft;
	private Button btn_bw_type;
	private MediaPlayer mPlayer 				= null;
	private MyAdapter mAdapter;
	private ListView lv_filelist;
	
	private ProgressBar mDownloadProgressBar;
	private TextView mProgressTextView;
	private Dialog mUpdateDialog;
	
	private boolean mIsPreview					= true;
	
	private boolean mIsBuffering 				= false;
	private boolean mStoped					= false;
	private boolean mHomed						= false;
	
	// list
	private ListMediaUtil mListUtil;
	private final static String HTTP_SERVER_URL = "http://172.16.204.106/test/testcase/";
	
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
	
	// dlna
	private DLNASdk mDLNA;
	private IDlnaCallback mDLNAcallback;
	private final static int DLNA_LISTEN_PORT = 8787;
	
	// epg
	private List<Map<String, Object>> mEPGCatalogList = null;
	private List<Map<String, Object>> mEPGClipList = null;
	private int mLastEPGitem					= -1;
	private final int INIT_EPG_CATALOG			= -1; 

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
	final static int OPTION_DLNA_LIST			= Menu.FIRST + 12;
	final static int OPTION_EPG_LIST			= Menu.FIRST + 13;
	
	
	
	// message
	private final static int MSG_CLIP_LIST_DONE			= 101;
	private final static int MSG_CLIP_PLAY_DONE			= 102;
	private static final int MSG_UPDATE_PLAY_INFO 		= 201;
	private static final int MSG_UPDATE_RENDER_INFO		= 202;
	private static final int MSG_LOCAL_LIST_DONE			= 203;
	private static final int MSG_HTTP_LIST_DONE			= 204;
	private static final int MSG_FAIL_TO_LIST_HTTP_LIST	= 301;
	private static final int MSG_DISPLAY_SUBTITLE			= 401;
	private static final int MSG_HIDE_SUBTITLE			= 402;
	private static final int MSG_LIST_EPG_CLIP_ONE		= 501;
	private static final int MSG_LIST_EPG_CATALOG_DONE	= 502;
	private static final int MSG_FAIL_TO_CONNECT_EPG_SERVER		= 511;
	
	private ProgressDialog progDlg 				= null;
	
	private String mCurrentFolder;
	
	private final static String home_folder		= "/test2";
	
	private final static String HTTP_UPDATE_APK_URL = "http://172.16.204.106/test/test/";
	
	private final String[] from = { "filename", "mediainfo", "folder", "filesize", "resolution", "thumb" };
	
	private final int[] to = { R.id.tv_filename, R.id.tv_mediainfo, R.id.tv_folder, 
			R.id.tv_filesize, R.id.tv_resolution, R.id.iv_thumb };
	
	private boolean USE_BREAKPAD = false;
	private boolean mRegisterBreakpad = false;
	
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

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// compatible with tvbox
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
		// end of tvbox
		
		this.btnPlay = (Button) findViewById(R.id.btn_play);
		this.btnSelectTime = (Button) findViewById(R.id.btn_select_time);
		this.btnClipLocation = (Button) findViewById(R.id.btn_clip_location);
		this.btnPlayerImpl = (Button) findViewById(R.id.btn_player_impl);
		this.btnPPboxSel = (Button) findViewById(R.id.btn_ppbox);
		this.btnTakeSnapShot = (Button) findViewById(R.id.btn_take_snapshot);
		this.btnSelectAudioTrack = (Button) findViewById(R.id.btn_select_audiotrack);
		this.et_play_url = (EditText) findViewById(R.id.et_url);
		this.et_playlink = (EditText) findViewById(R.id.et_playlink);
		this.btn_ft = (Button) findViewById(R.id.btn_ft);
		this.btn_bw_type = (Button) findViewById(R.id.btn_bw_type);

		this.mPreview = (MyPreView) findViewById(R.id.preview);
		this.mLayout = (RelativeLayout) findViewById(R.id.layout_preview);
		
		this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
		
		this.mMediaController = new MySimpleMediaController(this);
		
		mTextViewInfo = new TextView(this);
		mTextViewInfo.setTextColor(Color.RED);
		mTextViewInfo.setTextSize(18);
		mTextViewInfo.setText("play info");
		
		mControllerLayout = new LinearLayout(this);
		mControllerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		mControllerLayout.setOrientation(LinearLayout.VERTICAL);
		//mControllerLayout.addView(mTextViewInfo);
		mLayout.addView(mTextViewInfo);
		addContentView(mControllerLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		
		if (home_folder.equals("")) {
			mCurrentFolder = "";
		}
		else {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				mCurrentFolder = Environment.getExternalStorageDirectory().getPath() + home_folder;
				File file = new File(mCurrentFolder);
				if (!file.isDirectory()) {
					mCurrentFolder = Environment.getExternalStorageDirectory().getPath();
				}
				setTitle(mCurrentFolder);
			}
			else {
				Toast.makeText(this, "sd card is not mounted!", Toast.LENGTH_SHORT).show();
			}
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
		
		mListUtil = new ListMediaUtil(this);
		
		initFeedback();
		
		//CrashHandler crashHandler = CrashHandler.getInstance();  
        //crashHandler.init(this);
		
		if (initMeetSDK() == false) {
			Toast.makeText(this, "failed to load meet lib", 
				Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		if (initDLNA() == false) {
			Toast.makeText(this, "failed to load meet lib", 
				Toast.LENGTH_SHORT).show();
		}
		
		Util.startP2PEngine(this);
				
		mHolder = mPreview.getHolder();
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
		mHolder.addCallback(this);

		this.lv_filelist = (ListView) findViewById(R.id.lv_filelist);
		
		new ListItemTask().execute(mCurrentFolder);
		
		this.lv_filelist
				.setOnItemClickListener(new ListView.OnItemClickListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onItemClick(AdapterView<?> arg0, View view,
							int position, long id) {
						// TODO Auto-generated method stub
						Log.i(TAG, String.format("onItemClick %d %d", position, id));
						
						HashMap<String, Object> item = (HashMap<String, Object>) lv_filelist.getItemAtPosition(position);
						String file_name = (String)item.get("filename");
						String file_path = (String)item.get("fullpath");
						Log.i(TAG, String.format("Java: full_path %s", file_path));
							
						if (file_name.equals("..")) {
							// up to parent folder
							if (mListLocalFile) {
								File file = new File(mCurrentFolder);
								String parent_folder = file.getParent();
								if (parent_folder == null ||parent_folder == mCurrentFolder) {
									Log.i(TAG, "already at root folder");
								}
								else {
									mCurrentFolder = parent_folder;
									setTitle(mCurrentFolder);
									new ListItemTask().execute(mCurrentFolder);
								}
							}
							else {
								// http parent folder list
								String url = file_path;
								int index = url.lastIndexOf('/', url.length() - 1 - 1);
								url = url.substring(0, index + 1);
								new ListItemTask().execute(url);
								setTitle(url);
							}
						}
						else {
							if (file_path.startsWith("http://")) {
								Log.i(TAG, "Java: http list file clicked");
								
								if (file_path.charAt(file_path.length() - 1) == '/') {
									Log.i(TAG, "Java: list http folder");
									setTitle(file_path);
									new ListItemTask().execute(file_path);		
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
										Log.i(TAG, "Java: list folder: " + file.getAbsolutePath());
										mCurrentFolder = file_path;
										setTitle(mCurrentFolder);
										new ListItemTask().execute(mCurrentFolder);
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
		            break;
		        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
		        	mAdapter.SetScrolling(true);
		            break;
		        case OnScrollListener.SCROLL_STATE_FLING:
		        	mAdapter.SetScrolling(true);
				}
			}
		});
		
		this.btn_ft.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] ft = {"流畅", "高清", "超清", "蓝光"};
				
				Dialog choose_ft_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select player impl")
				.setSingleChoiceItems(ft, Integer.parseInt((String) btn_ft.getText()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btn_ft.setText(Integer.toString(whichButton));
							dialog.dismiss();
						}
					})
				.create();
				choose_ft_dlg.show();	
			}
		});
		
		this.btn_bw_type.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] bw_type = {"P2P", "CDNP2P", "CDN", "PPTV"};

				Dialog choose_bw_type_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select player impl")
				.setSingleChoiceItems(bw_type, Integer.parseInt((String) btn_bw_type.getText()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btn_bw_type.setText(Integer.toString(whichButton));
							dialog.dismiss();
						}
					})
				.create();
				choose_bw_type_dlg.show();	
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

				LoadPlayLinkUtil ext_link = new LoadPlayLinkUtil();
				if (ext_link.LoadTvList()) {
					list_title.addAll(ext_link.getTitles());
					list_url.addAll(ext_link.getUrls());
				}
				
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
								btn_ft.setText(String.valueOf(ppbox_ft[whichButton]));
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
				tmp = btn_ft.getText().toString();
				ppbox_ft = Integer.parseInt(tmp);
				tmp = btn_bw_type.getText().toString();
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
				String listUrl;
				if (mListLocalFile) {
					// switch to http list(maybe failed)
					listUrl = HTTP_SERVER_URL;
				}
				else {
					// http->local always succeed
		        	listUrl = mCurrentFolder;
				}
				
				new ListItemTask().execute(listUrl);
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
	
	@SuppressWarnings("deprecation") // avoid setType warning
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
		
		stop_player();
		
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
	
	void stop_player() {
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
			case MSG_LOCAL_LIST_DONE:
				btnClipLocation.setText("http");
				mListLocalFile = true;
				break;
			case MSG_HTTP_LIST_DONE:
				setTitle(HTTP_SERVER_URL);
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
			case MSG_LIST_EPG_CATALOG_DONE:
				popupEPGSelDlg();
				break;
			case MSG_LIST_EPG_CLIP_ONE:
				popupEPGListDlg();
				break;
			case MSG_FAIL_TO_CONNECT_EPG_SERVER:
				Toast.makeText(ClipListActivity.this, "failed to connect to epg server", Toast.LENGTH_SHORT).show();
				break;
			default:
				Log.w(TAG, "unknown msg.what " + msg.what);
				break;
			}			 
        }
	}; 
	
	private void popupEPGSelDlg() {
		int size = mEPGCatalogList.size();
		if (size == 0) {
			Toast.makeText(this, "epg catalog is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> title_list = new ArrayList<String>();
		final ArrayList<Integer> index_list = new ArrayList<Integer>();
		
		for (int i=0;i<size;i++) {
			HashMap<String, Object> item = (HashMap<String, Object>) mEPGCatalogList.get(i);
			String title = (String)item.get("title");
			int index = (Integer)item.get("index");
			title_list.add(title);
			index_list.add(index);
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);
			
		//final String[] str_name_list = {"PP出品", "同步剧场", "热播电影", "会员电影", "卡通动漫"};
		
		Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select epg item")
		.setSingleChoiceItems(str_title_list, -1, /*default selection item number*/
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				if (whichButton >= 0) {
					//int item = EPGUtil.EPG_TYPE_NEWS + whichButton;
					int item = index_list.get(whichButton);
					if (item != mLastEPGitem) {
						mLastEPGitem = item;
						Toast.makeText(ClipListActivity.this, "loading epg clip...", Toast.LENGTH_SHORT).show();
						new EPGTask().execute(item);
					}
					else {
						// just display cache list
						mHandler.sendEmptyMessage(MSG_LIST_EPG_CLIP_ONE);
					}
				}
				
				dialog.dismiss();
			}
		})
		.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
			}})
		.create();
		choose_clip_dlg.show();
	}
	
	private void popupEPGListDlg() {

		int size = mEPGClipList.size();
		if (size > 0) {
			ArrayList<String> link_list = new ArrayList<String>();
			ArrayList<String> name_list = new ArrayList<String>();
			
			for (int i=0;i<size;i++) {
				HashMap<String, Object> item = (HashMap<String, Object>) mEPGClipList.get(i);
				String name = (String)item.get("title");
				String link = (String)item.get("link");
				name_list.add(name);
				link_list.add(link);
			}
			
			final String[] str_link_list = (String[])link_list.toArray(new String[link_list.size()]);
			final String[] str_name_list = (String[])name_list.toArray(new String[name_list.size()]);
			
			Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
			.setTitle("Select clip to play")
			.setSingleChoiceItems(str_name_list, -1, /*default selection item number*/
				new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					et_playlink.setText(str_link_list[whichButton]);
					dialog.cancel();
				}
			})
			.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
				}})
			.create();
			choose_clip_dlg.show();
		}
	}
	
	private class EPGTask extends AsyncTask<Integer, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Integer... params) {
        	EPGUtil epg = new EPGUtil();
        	
        	if (params[0] == INIT_EPG_CATALOG) {
        		if (mEPGCatalogList == null) {
            		if (epg.getCategory() == false) {
            			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
                		return false;
                	}
            		
                	mEPGCatalogList = epg.getCategoryList();
                	Log.i(TAG, "Java: epg getCategory() " + mEPGCatalogList.toArray().toString());
        		}
        		
                mHandler.sendEmptyMessage(MSG_LIST_EPG_CATALOG_DONE);
        	}
        	else {
        		if (epg.initEPG(params[0]) == false) {
            		mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
            		return false;
            	}
            	
        		mEPGClipList = epg.getClipList();
        		mHandler.sendEmptyMessage(MSG_LIST_EPG_CLIP_ONE);
        	}	
        	
        	return true;
        }
        
    	@Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {		
        }
	}
	
	private class ListItemTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
        	Log.i(TAG, "Java: doInBackground " +  params[0]);
        	
        	// update progress
        	// publishProgress(progresses)
        	
        	boolean ret = mListUtil.ListMediaInfo(params[0]);
        	if (ret) {
        		if (params[0].startsWith("http://")) {
        			 if (mListLocalFile)
        				 mHandler.sendEmptyMessage(MSG_HTTP_LIST_DONE);
        		}
        		else {
        			if (!mListLocalFile)
        				mHandler.sendEmptyMessage(MSG_LOCAL_LIST_DONE);
        		}
        	}
        	else {
        		if (mListLocalFile)
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_LIST_HTTP_LIST);
        	}
        	
        	return ret;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        	if (result) {
				mAdapter = new MyAdapter(ClipListActivity.this, mListUtil.getList(), R.layout.sd_list,
					from, to);
				lv_filelist.setAdapter(mAdapter);	
        	}
        	
        	progDlg.dismiss();
        }

        @Override
        protected void onPreExecute() {
			progDlg = new ProgressDialog(ClipListActivity.this);
			progDlg.setMessage("Loading clips");
			progDlg.setCancelable(false);
			progDlg.show(); 
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {
			
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
	
	private String addPlaylinkParam(String playlink, int ft, String bwt) {
    	playlink += "?ft=" + ft;
    	playlink += "&bwtype=" + bwt;
    	
    	playlink += "&platform=android3";
        playlink += "&type=phone.android.vip";
        playlink += "&sv=4.0.1";
		playlink += "&param=userType%3D1"; // fix cannot find blue-disk ft problem
    	
    	return playlink;
    }

	private void upload_crash_report(int type) {	
		MeetSDK.makePlayerlog();
		
		FeedBackFactory fbf = new FeedBackFactory(
				 Integer.toString(type), "123456", false, false);
		fbf.asyncFeedBack();
	}
	
	private void push_to_dmr() {
		if (mPlayUrl == null || mPlayUrl.equals("")) {
			Toast.makeText(this, "no url is set", Toast.LENGTH_SHORT).show();
			return;
		}
		
		int dev_num = mDLNAcallback.mDeviceMap.size();
		
		if (dev_num == 0) {
			Log.i(TAG, "Java: dlna no dlna device found");
			Toast.makeText(this, "no dlna device found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> dev_list = new ArrayList<String>();
		ArrayList<String> uuid_list = new ArrayList<String>();
		for (Object obj : mDLNAcallback.mDeviceMap.keySet()){
	          Object name = mDLNAcallback.mDeviceMap.get(obj);
	          Log.d(TAG, "Java: dlna [dlna dev] uuid: " + obj.toString() + " name: " + name.toString());
	          uuid_list.add(obj.toString());
	          dev_list.add(name.toString());
	    }
		
		final String[] str_uuid_list = (String[])uuid_list.toArray(new String[uuid_list.size()]);
		final String[] str_dev_list = (String[])dev_list.toArray(new String[dev_list.size()]);
		
		Dialog choose_device_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select device to push")
		.setSingleChoiceItems(str_dev_list, -1, /*default selection item number*/
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				
				String uuid = str_uuid_list[whichButton];
				String name = str_dev_list[whichButton];
				String uri;
				if (mPlayUrl.startsWith("/") || mPlayUrl.startsWith("file://")) 
					uri = mDLNA.GetServerFileUrl(mPlayUrl);
				else
					uri = mPlayUrl;
				if (uri.indexOf("127.0.0.1") != -1) { 
				    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
				    if (!wifiManager.isWifiEnabled()) {  
				    wifiManager.setWifiEnabled(true);    
				    }  
				    WifiInfo wifiInfo = wifiManager.getConnectionInfo();       
				    int ipAddress = wifiInfo.getIpAddress();   
				    String ip_addr = intToIp(ipAddress);  
				    
					uri = uri.replace("127.0.0.1", ip_addr);
				}
				mDLNA.SetURI(uuid, uri);
				Log.i(TAG, String.format("Java: dlna push url %s to uuid(%s) name(%s)", uri, uuid, name));
				
				dialog.cancel();
				
			}
		})
		.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
			}})
		.create();
		choose_device_dlg.show();
	}
	
	private String intToIp(int i) {       
        
        return (i & 0xFF ) + "." +       
      ((i >> 8 ) & 0xFF) + "." +       
      ((i >> 16 ) & 0xFF) + "." +       
      ( i >> 24 & 0xFF) ;  
   }   
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		int i = Menu.FIRST;
		
		SubMenu OptSubMenu = menu.addSubMenu(i, OPTION, Menu.NONE, "Option");
		OptSubMenu.setIcon(R.drawable.option);
		MenuItem previewMenuItem = OptSubMenu.add(i, OPTION_PREVIEW, Menu.NONE, "Preview");
		previewMenuItem.setCheckable(true);
		if (mIsPreview)
			previewMenuItem.setChecked(true);
		
		// dlna
		OptSubMenu.add(i, OPTION_DLNA_LIST, Menu.NONE, "dlna");
		// epg
		OptSubMenu.add(i, OPTION_EPG_LIST, Menu.NONE, "epg");
		
		menu.add(i, UPDATE_CLIP_LIST, Menu.NONE, "Update list")
			.setIcon(R.drawable.list);
		menu.add(i, UPDATE_APK, Menu.NONE, "Update apk")
			.setIcon(R.drawable.update);
		menu.add(i, UPLOAD_CRASH_REPORT, Menu.NONE, "Upload crash report")
			.setIcon(R.drawable.log);
		menu.add(i, QUIT, Menu.NONE, "Quit");
		
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
			if (mListLocalFile) {
				new ListItemTask().execute(mCurrentFolder);
			}
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
		case OPTION_DLNA_LIST:
			push_to_dmr();
			break;
		case OPTION_EPG_LIST:
			Toast.makeText(this, "loading epg catalog...", Toast.LENGTH_SHORT).show();
			new EPGTask().execute(INIT_EPG_CATALOG);
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
				//Log.d(TAG, "Java: subtitle: subtitle file: " + subtitle_full_path);
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

		
		mLayout.getLayoutParams().height = preview_height;
		mLayout.requestLayout(); //or invalidate();
		
		Log.i(TAG, "onResume()");
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Java: onPause()");

		if (mPlayer != null) {
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
	
	private boolean initDLNA() {
		mDLNA = new DLNASdk();
		if (!mDLNA.isLibLoadSuccess()) {
			Log.e(TAG, "Java: dlna failed to load dlna lib");
			return false;
		}
		
		mDLNAcallback = new IDlnaCallback();
		//mDLNA.setLogPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/xxxx_dlna.log");
		mDLNA.Init(mDLNAcallback);
		mDLNA.EnableRendererControler(true);
		
		//start file server
		Random rand =new Random();
		int i;
		i = rand.nextInt(100);
		int port = 10010;//DLNA_LISTEN_PORT + i;
		mDLNA.StartHttpServer(port);
		Log.i(TAG, String.format("Java: dlna start dlna server port: %d", port));
		return true;
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
        	if (mStoped)
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
	
	private final class MySimpleMediaController extends MediaController {

	    private MySimpleMediaController(Context context) {
	        super(new ContextThemeWrapper(context, R.style.MyPlayerTheme));
	    }
	}
	
	static {
		//System.loadLibrary("lenthevcdec");
	}
	
}
