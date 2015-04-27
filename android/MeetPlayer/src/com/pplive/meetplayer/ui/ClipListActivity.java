package com.pplive.meetplayer.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.view.ContextThemeWrapper;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.os.Build;
import android.media.AudioManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.util.DisplayMetrics; // for display width and height
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;

import com.pplive.meetplayer.R;

import com.pplive.meetplayer.service.DLNAService;
import com.pplive.meetplayer.util.AtvUtils;
import com.pplive.meetplayer.util.Catalog;
import com.pplive.meetplayer.util.Content;
import com.pplive.meetplayer.util.DownloadAsyncTask;
import com.pplive.meetplayer.util.EPGUtil;
import com.pplive.meetplayer.util.FeedBackFactory;
import com.pplive.meetplayer.util.IDlnaCallback;
import com.pplive.meetplayer.util.ListMediaUtil;
import com.pplive.meetplayer.util.LoadPlayLinkUtil;
import com.pplive.meetplayer.util.LogcatHelper;
import com.pplive.meetplayer.util.Module;
import com.pplive.meetplayer.util.PlayLink2;
import com.pplive.meetplayer.util.PlayLinkUtil;
import com.pplive.meetplayer.util.Util;
import com.pplive.meetplayer.ui.widget.MiniMediaController;
import com.pplive.meetplayer.ui.widget.MySimpleMediaController;
import com.pplive.dlna.DLNASdk;
import com.pplive.dlna.DLNASdk.DLNASdkInterface;
import com.pplive.dlna.DLNASdkDMSItemInfo;









// for thread
import android.os.Handler;  
import android.os.Message;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaController.MediaPlayerControl;
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
		MediaPlayerControl, SurfaceHolder.Callback, SubTitleParser.Callback, OnFocusChangeListener, 
		DLNASdkInterface {

	private final static String TAG = "ClipList";
	
    private final static String PORT_HTTP = "http";
    private final static String PORT_RTSP = "rtsp";
		
	private Button btnPlay;
	private Button btnSelectTime;
	private Button btnClipLocation;
	private Button btnPlayerImpl;
	private Button btnPPboxSel;
	private Button btnTakeSnapShot;
	private Button btnSelectAudioTrack;
	private EditText et_play_url;
	private MyPreView2 mPreview;
	private boolean mPreviewFocused = false;
	private SurfaceHolder mHolder;
	private MiniMediaController mMediaController;
	private RelativeLayout mLayout;
	private ProgressBar mBufferingProgressBar;
	private EditText et_playlink;
	private Button btn_ft;
	private Button btn_bw_type;
	private ImageView imageDMR;
	private ImageView imageNoVideo;
	private MediaPlayer mPlayer 				= null;
	private LocalFileAdapter mAdapter;
	private ListView lv_filelist;
	
	private ProgressBar mDownloadProgressBar;
	private TextView mProgressTextView;
	private Dialog mUpdateDialog;
	
	private DecodeMode mDecMode = DecodeMode.AUTO;
	private boolean mIsPreview	;
	private boolean mIsLoop					= false;
	private boolean mIsNoVideo					= false;
	private MenuItem noVideoMenuItem;
	
	private int mBufferingPertent				= 0;
	private boolean mIsBuffering 				= false;
	private boolean mStoped					= false;
	private boolean mHomed						= false;
	
	private WifiLock mWifiLock;
	
	private boolean isLandscape = false;
	
	// playback
	private long mStartTimeSec = 0;
	private int mDuration = 0;
	
	// list
	private ListMediaUtil mListUtil;
	private final static String HTTP_SERVER_URL = "http://172.16.204.106/test/testcase/";
	
	private String mPlayUrl;
	private int mVideoWidth, mVideoHeight;
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
	private String subtitle_filename;
	
	// dlna
	private DLNASdk mDLNA;
	private IDlnaCallback mDLNAcallback;
	private final static int DLNA_LISTEN_PORT = 10010;
	private String mDlnaDeviceUUID;
	private String mDlnaDeviceName;
	private boolean mDMRcontrolling = false;
	
	// epg
	private EPGUtil mEPG;
	
	private List<Content> mEPGContentList 	= null;
	private List<Module> mEPGModuleList 	= null;
	private List<Catalog> mEPGCatalogList = null;
	private List<PlayLink2> mEPGLinkList 	= null;
	private String mEPGsearchKey; // for search
	private String mDLNAPushUrl;
	private String mLink;
	private String mEPGparam;
	private String mEPGtype;
	private int mEPGlistStartPage = 1;
	private int mEPGlistCount = 15;
	private boolean mListLive = false;
	private boolean mListSearch = false;
	private final int EPG_ITEM_FRONTPAGE		= 1;
	private final int EPG_ITEM_CATALOG			= 2;
	private final int EPG_ITEM_DETAIL			= 3;
	private final int EPG_ITEM_SEARCH			= 4;
	private final int EPG_ITEM_CONTENT_LIST	= 5;
	private final int EPG_ITEM_CONTENT_SURFIX	= 6;
	private final int EPG_ITEM_LIST			= 7;
	private final int EPG_ITEM_CDN				= 11;
	private final int EPG_ITEM_FT				= 12;

	private boolean mListLocalFile				= true;
	
	private TextView mTextViewInfo 				= null;
	
	private int decode_fps						= 0;
	private int render_fps 					= 0;
	private int decode_avg_msec 				= 0;
	private int render_avg_msec 				= 0;
	private int render_frame_num				= 0;
	private int decode_drop_frame				= 0;
	private int av_latency_msec				= 0;
	private int video_bitrate					= 0;
	
	private int preview_height;
	
	private String mPlayerLinkSurfix;
	private boolean mIsLivePlay = false;

	final static int ONE_MAGEBYTE 				= 1048576;
	final static int ONE_KILOBYTE 				= 1024;
	
	// menu item
	final static int OPTION 					= Menu.FIRST;
	final static int UPDATE_CLIP_LIST			= Menu.FIRST + 1;
	final static int UPDATE_APK				= Menu.FIRST + 2;
	final static int UPLOAD_CRASH_REPORT		= Menu.FIRST + 3;
	final static int QUIT 						= Menu.FIRST + 4;
	final static int OPTION_COMMON				= Menu.FIRST + 11;
	final static int OPTION_DLNA_DMR			= Menu.FIRST + 12;
	final static int OPTION_DLNA_DMS			= Menu.FIRST + 13;
	final static int OPTION_EPG_FRONTPAGE		= Menu.FIRST + 14;
	final static int OPTION_EPG_CONTENT		= Menu.FIRST + 15;
	final static int OPTION_EPG_SEARCH			= Menu.FIRST + 16;
	final static int OPTION_COMMON_PREVIEW		= Menu.FIRST + 21;
	final static int OPTION_COMMON_LOOP		= Menu.FIRST + 22;
	final static int OPTION_COMMON_NO_VIDEO	= Menu.FIRST + 23;
	final static int OPTION_COMMON_MEETVIEW	= Menu.FIRST + 24;
	final static int OPTION_COMMON_SUBTITLE	= Menu.FIRST + 25;
	
	
	// message
	private final static int MSG_CLIP_LIST_DONE					= 101;
	private final static int MSG_CLIP_PLAY_DONE					= 102;
	private static final int MSG_UPDATE_PLAY_INFO 				= 201;
	private static final int MSG_UPDATE_RENDER_INFO				= 202;
	private static final int MSG_LOCAL_LIST_DONE					= 203;
	private static final int MSG_HTTP_LIST_DONE					= 204;
	private static final int MSG_FAIL_TO_LIST_HTTP_LIST			= 301;
	private static final int MSG_DISPLAY_SUBTITLE					= 401;
	private static final int MSG_HIDE_SUBTITLE					= 402;
	private static final int MSG_EPG_FRONTPAGE_DONE				= 501;
	private static final int MSG_EPG_CATALOG_DONE					= 502;
	private static final int MSG_EPG_DETAIL_DONE					= 503;
	private static final int MSG_EPG_SEARCH_DONE					= 504;
	private static final int MSG_EPG_CONTENT_LIST_DONE			= 505;
	private static final int MSG_EPG_CONTENT_SURFIX_DONE			= 506;
	private static final int MSG_EPG_LIST_DONE					= 507;
	private static final int MSG_FAIL_TO_CONNECT_EPG_SERVER		= 511;
	private static final int MSG_FAIL_TO_PARSE_EPG_RESULT			= 512;
	private static final int MSG_WRONG_PARAM						= 513;
	private static final int MSG_PUSH_CDN_CLIP					= 601;
	private static final int MSG_PLAY_CDN_URL						= 602;
	private static final int MSG_PLAY_CDN_FT						= 603;
	
	private ProgressDialog progDlg 				= null;
	
	private String mCurrentFolder;
	
	private final static String home_folder		= "/test2";
	
	private final static String HTTP_UPDATE_APK_URL = "http://172.16.204.106/test/test/";
	
	private final String[] from = { "filename", "mediainfo", "folder", "filesize", "resolution", "thumb" };
	
	private final int[] to = { R.id.tv_filename, R.id.tv_mediainfo, R.id.tv_folder, 
			R.id.tv_filesize, R.id.tv_resolution, R.id.iv_thumb };
	
	private boolean USE_BREAKPAD = false;
	private boolean mBreakpadRegisterDone = false;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		    
		// compatible with tvbox
		if (getResources().getConfiguration().orientation == 1) 
			isLandscape = false;
		else
			isLandscape = true;
		
		if (isLandscape) {
			setContentView(R.layout.list_landscape);
			
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
	                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		else {
			setContentView(R.layout.list);
		}
		
		Log.i(TAG, "Java: onCreate()");
		
		/*DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		if (isLandscape)
			preview_height = screen_height;
		else
			preview_height = screen_height * 2 / 5;
		Log.i(TAG, String.format("screen %dx%d, preview height %d", screen_width, screen_height, preview_height));*/
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
		this.imageDMR = (ImageView) findViewById(R.id.iv_dlna_dmc);
		this.imageNoVideo = (ImageView) findViewById(R.id.iv_novideo);
		
		this.mPreview = (MyPreView2) findViewById(R.id.preview);
		this.mLayout = (RelativeLayout) findViewById(R.id.layout_preview);
		
		this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
		
		this.mMediaController = (MiniMediaController) findViewById(R.id.mmc);
	
		mMediaController.setInstance(this);
		
		readSettings();
		
		mTextViewInfo = (TextView) findViewById(R.id.tv_info);
		mTextViewInfo.setTextColor(Color.RED);
		mTextViewInfo.setTextSize(18);
		mTextViewInfo.setTypeface(Typeface.MONOSPACE);
		
		mLayout.setFocusable(true);
		mLayout.setOnFocusChangeListener(this);
		//mLayout.addView(mTextViewInfo);
		
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
		
		if (USE_BREAKPAD && !mBreakpadRegisterDone) {
            try {
                BreakpadUtil.registerBreakpad(new File(getCacheDir().getAbsolutePath()));
                mBreakpadRegisterDone = true;
            }
            catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
		
		mListUtil = new ListMediaUtil(this);
		
		initFeedback();
		
		mEPG = new EPGUtil();
		
		//CrashHandler crashHandler = CrashHandler.getInstance();  
        //crashHandler.init(this);
		
		if (Util.initMeetSDK(this) == false) {
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
						
						Map<String, Object> item = mAdapter.getItem(position);
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
									start_player(file_path);
								}
							}
						}
					}
				});
		
		this.lv_filelist.setOnItemLongClickListener(new ListView.OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				final String[] action = {"delete", "detail"};
				Map<String, Object> item = mAdapter.getItem(position);
				final String file_name = (String)item.get("filename");
				final String file_path = (String)item.get("fullpath");

				Dialog choose_action_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select action")
				.setItems(action, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							if (whichButton == 0) {
								// delete
								if (file_path.startsWith("/") || file_path.startsWith("file://")) {
									File file = new File(file_path);
									if (file.exists() && file.delete()) {
										Log.i(TAG, "file: " + file_path + " deleted");
										Toast.makeText(ClipListActivity.this, "file " + file_name + " deleted!", Toast.LENGTH_SHORT).show();
										
										new ListItemTask().execute(mCurrentFolder);
									}
									else {
										Log.e(TAG, "failed to delete file: " + file_path);
										Toast.makeText(ClipListActivity.this, "failed to delte file: " + file_path, Toast.LENGTH_SHORT).show();
									}
								}
								else {
									Toast.makeText(ClipListActivity.this, "DELETE only support local file", Toast.LENGTH_SHORT).show();
								}
							}
							else {
								// todo
							}

							dialog.dismiss();
						}
					})
				.setNegativeButton("Cancel", null)
				.create();
				choose_action_dlg.show();
				
				return false;
			}
			
		});
		
		this.btn_ft.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] ft = {"流畅", "高清", "超清", "蓝光"};
				
				Dialog choose_ft_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select ft")
				.setSingleChoiceItems(ft, Integer.parseInt(btn_ft.getText().toString()), /*default selection item number*/
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
				final String[] bw_type = {"P2P", "CDNP2P", "CDN", "PPTV", "DLNA"};

				Dialog choose_bw_type_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select bw_type")
				.setSingleChoiceItems(bw_type, Integer.parseInt((String) btn_bw_type.getText()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btn_bw_type.setText(Integer.toString(whichButton));
							if (noVideoMenuItem != null) {
								if (whichButton == 4)
									noVideoMenuItem.setEnabled(true);
								else
									noVideoMenuItem.setEnabled(false);
							}
							
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
				//Log.i(TAG, "Java: getCount " + binder.getCount());
				
				final String[] PlayerImpl = {"Auto", "System", "XOPlayer", "FFPlayer"};
				
				Dialog choose_player_impl_dlg = new AlertDialog.Builder(ClipListActivity.this)
				.setTitle("select player impl")
				.setSingleChoiceItems(PlayerImpl, mPlayerImpl, /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							Log.i(TAG, "select player impl: " + whichButton);
							
							mPlayerImpl = whichButton;
							Util.writeSettingsInt(ClipListActivity.this, "PlayerImpl", mPlayerImpl);
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
				
				final int fixed_size = list_title.size();
				
				// load play history
				final ArrayList<String> list_vid = new ArrayList<String>();
				
				String key = "PlayHistory";
				String regularEx = ",";
				String values = Util.readSettings(ClipListActivity.this, key);
		        Log.d(TAG, "Java: PlayHistory(in PPboxSel) read: " + values);
		        String []str = values.split(regularEx);
		        for (int i=0;i<str.length;i++) {
		        	// 后遗症|1233
		        	Log.i(TAG, "Java: history item #" + i + ": " + str[i]);
		        	int pos = str[i].indexOf("|");
		        	if (pos != -1) {
		        		list_title.add(str[i].substring(0, pos));
		        		list_vid.add(str[i].substring(pos + 1, str[i].length()));
		        	}
		        }
		        
		        final int history_size = list_vid.size();
		        
		        Log.i(TAG, String.format("Java: fixed_size %d, history_size %d", fixed_size, history_size));
		        
				// load tvlist.txt
				LoadPlayLinkUtil ext_link = new LoadPlayLinkUtil();
				if (ext_link.LoadTvList()) {
					list_title.addAll(ext_link.getTitles());
					list_url.addAll(ext_link.getUrls());
				}
				
				final String[] ppbox_clipname = (String[])list_title.toArray(new String[list_title.size()]);  
				
				final int ppbox_playlink[] = {18139131, 10110649, 17054339, 17461610, 17631361, 17611359};
				final int ppbox_ft[] = {2, 2, 2, 2, 3, 2};//2-超清, 3-bd
				
				Dialog choose_ppbox_res_dlg = new AlertDialog.Builder(ClipListActivity.this)
					.setTitle("Select ppbox program")
					.setSingleChoiceItems(ppbox_clipname, -1, /*default selection item number*/
						new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton) {
							if (whichButton < fixed_size) {
								et_playlink.setText(String.valueOf(ppbox_playlink[whichButton]));
								btn_ft.setText(String.valueOf(ppbox_ft[whichButton]));
								Log.i(TAG, String.format("Java: choose %d %s %d", 
										whichButton, ppbox_clipname[whichButton], ppbox_playlink[whichButton]));
							}
							else if (whichButton < fixed_size + history_size) {
								et_playlink.setText(list_vid.get(whichButton - fixed_size));
								btn_ft.setText("1");
								Log.i(TAG, String.format("Java: choose playhistory %d %s %s", 
										whichButton, ppbox_clipname[whichButton], list_vid.get(whichButton - fixed_size)));
							}
							else {
								String url = list_url.get(whichButton - fixed_size - history_size);
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
				
				short port = MediaSDK.getPort(PORT_HTTP);
				Log.i(TAG, "Http port is: " + port);
				
				if (ppbox_playid >= 300000 && ppbox_playid < 300999 && 
						(mPlayerLinkSurfix == null || mPlayerLinkSurfix.equals(""))) {
					mIsLivePlay = true;
					Log.i(TAG, "Java: set mIsLivePlay to true");
				}
				else {
					mIsLivePlay = false;
					Log.i(TAG, "Java: set mIsLivePlay to false");
				}
				
				if (ppbox_bw_type == 4) {// dlna
					new EPGTask().execute(EPG_ITEM_CDN, ppbox_playid, 0); // 3rd params for MSG_PLAY_CDN_URL
					return;
				}
				
				String ppbox_url = PlayLinkUtil.getPlayUrl(ppbox_playid, port, ppbox_ft, ppbox_bw_type, mPlayerLinkSurfix);
				
				start_player(ppbox_url);
			}
		});
		
		this.btnSelectTime.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Intent intent = new Intent(ClipListActivity.this, TimePickerActivity.class);
				//startActivityForResult(intent, 1);
				
				setPlaybackTime();
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
	
	private void readSettings() {
		int value = Util.readSettingsInt(this, "isPreview");
		Log.i(TAG, "readSettings isPreview: " + value);
		if (value == 1)
			mIsPreview = true;
		else
			mIsPreview = false;
		
		value = Util.readSettingsInt(this, "isLoop");
		Log.i(TAG, "readSettings isLoop: " + value);
		if (value == 1)
			mIsLoop = true;
		else
			mIsLoop = false;
		
		value = Util.readSettingsInt(this, "isNoVideo");
		if (value == 1)
			mIsNoVideo = true;
		else
			mIsNoVideo = false;
		
		mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
	}
	
	private void setPlaybackTime() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this); 
        View view = View.inflate(this, R.layout.date_time_dialog, null); 
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker); 
        final TimePicker timePicker = (TimePicker) view.findViewById(R.id.time_picker);
        final EditText etDuration = (EditText) view.findViewById(R.id.et_duration);
        builder.setView(view); 

        Calendar cal = Calendar.getInstance(); 
        cal.setTimeInMillis(System.currentTimeMillis()); 
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null); 

        timePicker.setIs24HourView(true); 
        timePicker.setCurrentHour(18/*cal.get(Calendar.HOUR_OF_DAY)*/); 
        timePicker.setCurrentMinute(30/*cal.get(Calendar.MINUTE)*/); 
 
        builder.setTitle("select start time"); 
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() { 

            @Override 
            public void onClick(DialogInterface dialog, int which) { 

            	int year, month, day, hour, min;
            	year = datePicker.getYear();
            	month = datePicker.getMonth();
            	day  = datePicker.getDayOfMonth();
            	hour = timePicker.getCurrentHour();
            	min = timePicker.getCurrentMinute();
            	
            	String strHour = String.format("%02d", hour);
                String strMin = String.format("%02d", min);
                
            	StringBuffer sb = new StringBuffer(); 
                sb.append(String.format("%d-%02d-%02d",  
                        year, month, day)); 
                sb.append(" ");
                sb.append(strHour).append(":").append(strMin); 
                
                String strTime;
                strTime = String.format("%d-%02d-%02d %02d:%02d",
                		datePicker.getYear(),
                        datePicker.getMonth(), 
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());
                
                // step1
                GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min, 0);
                mStartTimeSec = gc.getTimeInMillis() / 1000;
            	
            	// step2
            	String strDuration =  etDuration.getText().toString();
            	mDuration = Integer.parseInt(strDuration);
            	
            	Log.i(TAG, String.format("start_time %d sec, duration %d min", mStartTimeSec, mDuration));
            	
            	mPlayerLinkSurfix = String.format("&begin_time=%d&end_time=%d", 
                		mStartTimeSec, mStartTimeSec + mDuration * 60);
                try {
                	mPlayerLinkSurfix = URLEncoder.encode(mPlayerLinkSurfix, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                Log.i(TAG, "Java: mPlayerLinkSurfix final: " + mPlayerLinkSurfix);
                
                dialog.cancel();
                Toast.makeText(ClipListActivity.this, 
                		String.format("toggle to playback mode start %s, duration %d min", 
                				sb.toString(), mDuration), 
                		Toast.LENGTH_SHORT).show();
            } 
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mPlayerLinkSurfix = "";
				mStartTimeSec = 0;
				Toast.makeText(ClipListActivity.this, 
                		String.format("toggle to live mode"), Toast.LENGTH_SHORT).show();
			}
        	
        });
        
        Dialog dialog = builder.create(); 
        dialog.show();
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
	
	@SuppressWarnings("deprecation") // avoid setType warning
	private int start_player(String path) {
		mPlayUrl = path;
		
		setTitle(path);
		Log.i(TAG, "Java: clipname: " + mPlayUrl);
		
		mDecMode = DecodeMode.UNKNOWN;
		if (0 == mPlayerImpl) {
			mDecMode = DecodeMode.AUTO;
		}
		else if (1 == mPlayerImpl) {
			mDecMode = DecodeMode.HW_SYSTEM;
		}
		else if (2 == mPlayerImpl) {
			boolean canPlay = false;
			
			if (path.startsWith("http://")) {
				canPlay = true;
			}
			else if (path.startsWith("/") || path.startsWith("file://")) {
				MediaInfo info = MeetSDK.getMediaDetailInfo(path);
				if (info != null) {
					if (info.getVideoCodecName() != null && 
					(info.getVideoCodecName().equals("h264") || info.getVideoCodecName().equals("hevc"))) {
						if (info.getAudioChannels() == 0)
							canPlay = true;
						else {
							TrackInfo trackinfo = info.getAudioChannelsInfo().get(0);
							if (trackinfo.getCodecName() != null && trackinfo.getCodecName().equals("aac"))
								canPlay = true;
						}
					}
				}
			}
			
			String fileName = "N/A";
			int index;	
			
			if (path.startsWith("/") || path.startsWith("file://")) {
				index = path.lastIndexOf("/");
				if (index != -1)
					fileName = path.substring(index + 1, path.length());
				else
					fileName = path;
			}
			else {
				fileName = path;
			}
			
			if (canPlay == false) {
				Toast.makeText(ClipListActivity.this, "XOPlayer cannot play: " + fileName, 
					Toast.LENGTH_SHORT).show();
				return -1;
			}
			
			mDecMode = DecodeMode.HW_XOPLAYER;
		}									
		else if (3 == mPlayerImpl) {
			mDecMode = DecodeMode.SW;
		}
		else if (4 == mPlayerImpl) {
			mDecMode = DecodeMode.SW;
		}
		else {
			Toast.makeText(ClipListActivity.this, "invalid player implement: " + Integer.toString(mPlayerImpl), 
				Toast.LENGTH_SHORT).show();
			return -1;
		}
		
		stop_player();
		
		btnSelectAudioTrack.setVisibility(View.GONE);
		
		if (!mIsPreview) {
			Uri uri = Uri.parse(path);
			Log.i(TAG, "Java: goto PlayerActivity, uri:" + uri.toString());
			start_fullscreen_play(uri, mPlayerImpl);
		}
		else {
			if (path.startsWith("file://") || path.startsWith("/")) {
				// only get mediainfo from local file
				MediaInfo info;
				
				info = MeetSDK.getMediaDetailInfo(path);
				if (info != null) {
					ArrayList<TrackInfo> audioTrackList = info.getAudioChannelsInfo();
					for (TrackInfo trackInfo : audioTrackList) {
						Log.i(TAG, String.format("Java: audio Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s", 
							trackInfo.getStreamIndex(), 
							trackInfo.getId(), 
							trackInfo.getCodecName(), 
							trackInfo.getLanguage(),
							trackInfo.getTitle()));
					}
					
					if (info.getAudioChannels() > 1)
						btnSelectAudioTrack.setVisibility(View.VISIBLE);	
					
					ArrayList<TrackInfo> subtitleTrackList = info.getSubtitleChannelsInfo();
					for (TrackInfo trackInfo : subtitleTrackList) {
						Log.i(TAG, String.format("Java: subtitle Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s", 
							trackInfo.getStreamIndex(), 
							trackInfo.getId(), 
							trackInfo.getCodecName(), 
							trackInfo.getLanguage(),
							trackInfo.getTitle()));
					}
				}
			}
			
			if (DecodeMode.AUTO == mDecMode) {
				mDecMode = MeetSDK.getPlayerType(mPlayUrl);
				Log.i(TAG, "Java: mDecMode " + mDecMode.toString());
			}
			
			// force refresh a new surface
			mPreview.setVisibility(View.INVISIBLE);
			
			if (DecodeMode.HW_SYSTEM == mDecMode) {
				mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			}
			else if (DecodeMode.SW == mDecMode){
				mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
				mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
			}
			
			mPreview.setVisibility(View.VISIBLE);
			
			mPlayer = new MediaPlayer(mDecMode);
			
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
			
			mPlayer.setLooping(mIsLoop);
			
			if (path.startsWith("http://")) {
				mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
					    .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

				mWifiLock.acquire();
			}

			mStoped 			= false;
			mHomed 				= false;
			mBufferingPertent 	= 0;
			mDMRcontrolling		= false;
			imageDMR.setVisibility(View.GONE);
			
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
			mMediaController.hide();
			
			mStoped = true;
			if (mIsSubtitleUsed) {
				mSubtitleThread.interrupt();
				
				try {
					Log.i(TAG, "Java subtitle before join");
					mSubtitleThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				subtitle_filename = null;
			}
			
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;

			mIsLivePlay = false;
			
			if (mWifiLock != null) {
				try {
					if (mWifiLock.isHeld())
						mWifiLock.release();
				}
				catch(Exception e){
			        //probably already released
			        Log.e(TAG, e.getMessage());
			    }
				
				mWifiLock = null;
			}
			
			// fix last video is still in buffering state
			if (mIsBuffering) {
				mBufferingProgressBar.setVisibility(View.GONE);
				mIsBuffering = false;
			}
			
			// cannot run in main thread
			//if (mPlayUrl.startsWith("http://127.0.0.1"))
			//	close_hls();
		}
	}
	
	/////////////////////////////////////////////
    //implements MediaPlayerControl
	public void start() {
		if (mPlayer != null)
			mPlayer.start();
		
		if (mDMRcontrolling)
			mDLNA.Play(mDlnaDeviceUUID);
	}
	
	public void seekTo(int pos) {
		if (mPlayer != null) {
			if (mIsLivePlay) {
				int new_duration = mPlayer.getDuration();
				int offset = new_duration - 1800 * 1000;
				pos += offset;
				if (pos > new_duration)
					pos = new_duration;
				if (pos < offset)
					pos = offset;
				mPlayer.seekTo(pos);
			}
			else {
				mPlayer.seekTo(pos);
			}
			
			// update mBufferingPertent
			if (pos <= 0)
				pos = 1; // avoid to be devided by zero
			mBufferingPertent = pos * 100 / mPlayer.getDuration() + 1;
			if (mBufferingPertent > 100)
				mBufferingPertent = 100;
			Log.i(TAG, "onBufferingUpdate: seekTo " + mBufferingPertent);
		}
		
		if (mSubtitleParser != null) {
			mSubtitleThread.interrupt();
			mSubtitleSeeking = true;
			
			mSubtitleParser.seekTo(pos);
		}
		
		if (mDMRcontrolling) {
			mDLNA.Seek(mDlnaDeviceUUID, pos / 1000);
		}
	}
	
	public void pause() {
		if (mPlayer != null)
			mPlayer.pause();
		
		if (mDMRcontrolling)
			mDLNA.Pause(mDlnaDeviceUUID);
	}
	
	public boolean isPlaying() {
		if (mPlayer == null)
			return false;
			
		return mPlayer.isPlaying();
	}
	
	public int getDuration() {
		if (mPlayer == null)
			return 0;
		
		if (mIsLivePlay)
			return (1800 * 1000);
		
		return mPlayer.getDuration();
	}
	
	public int getCurrentPosition() {
		if (mPlayer == null)
			return 0;
		
		int pos = mPlayer.getCurrentPosition();
		if (mIsLivePlay) {
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

		return mBufferingPertent;
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
	//end of : implements MediaPlayerControl
	//////////////////////////////////////////
	
	private void close_hls() {
		short port = MediaSDK.getPort("http");
		String strCloseURL = String.format("http://127.0.0.1:%d/close", port);
		URL url;
		try {
			url = new URL(strCloseURL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			if (conn.getResponseCode()==200)
                Toast.makeText(this, "GET post succeeded", Toast.LENGTH_SHORT).show();  
            else
            	Toast.makeText(this, "GET post failed", Toast.LENGTH_SHORT).show();  
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
			case MSG_EPG_FRONTPAGE_DONE:
				popupEPGModuleDlg(true);
				break;
			case MSG_EPG_CONTENT_LIST_DONE:
				popupEPGModuleDlg(false);
				break;
			case MSG_EPG_CATALOG_DONE:
				popupEPGCatalogDlg();
				break;
			case MSG_EPG_SEARCH_DONE:
			case MSG_EPG_DETAIL_DONE:
			case MSG_EPG_LIST_DONE:
				if (mEPGLinkList.size() == 1) {
					boolean ret = add_list_history(mEPGLinkList.get(0).getTitle(), 
							Integer.valueOf(mEPGLinkList.get(0).getId()));
					
					if (!ret)
						Toast.makeText(ClipListActivity.this, "failed to save play history", Toast.LENGTH_SHORT).show();
					
					et_playlink.setText(mEPGLinkList.get(0).getId());
					
					Toast.makeText(
							ClipListActivity.this,
							String.format("\"%s\" was selected", mEPGLinkList.get(0).getTitle()),
							Toast.LENGTH_SHORT).show();
					
					new EPGTask().execute(EPG_ITEM_FT, Integer.valueOf(mEPGLinkList.get(0).getId()));
				}
				else {
					popupEPGCollectionDlg();
				}
				break;
			case MSG_EPG_CONTENT_SURFIX_DONE:
				popupEPGContentDlg();
				break;
			case MSG_WRONG_PARAM:
				Toast.makeText(ClipListActivity.this, "epg: wrong param input", Toast.LENGTH_SHORT).show();
				break;
			case MSG_FAIL_TO_CONNECT_EPG_SERVER:
				Toast.makeText(ClipListActivity.this, "failed to connect to epg server", Toast.LENGTH_SHORT).show();
				break;
			case MSG_FAIL_TO_PARSE_EPG_RESULT:
				Toast.makeText(ClipListActivity.this, "failed to parse epg result", Toast.LENGTH_SHORT).show();
				break;
			case MSG_PUSH_CDN_CLIP:
				mDMRcontrolling = true;
				imageDMR.setVisibility(View.VISIBLE);
				Log.i(TAG, String.format("Java: dlna push url(%s) to uuid(%s) name(%s)", mDLNAPushUrl, mDlnaDeviceUUID, mDlnaDeviceName));
				Toast.makeText(ClipListActivity.this, 
						String.format("push url to dmr %s", mDlnaDeviceName), Toast.LENGTH_SHORT).show();
				break;
			case MSG_PLAY_CDN_URL:
				Log.i(TAG, "cdn url set %s"+ mDLNAPushUrl);
				stop_player();
				start_player(mDLNAPushUrl);
				break;
			case MSG_PLAY_CDN_FT:
				btn_ft.setText(String.valueOf(msg.arg1));
				Log.i(TAG, "Java: set ft to: "+ msg.arg1);
				break;
			default:
				Log.w(TAG, "Java: unknown msg.what " + msg.what);
				break;
			}			 
        }
	}; 
	
	private void popupEPGModuleDlg(final boolean isFrontpage) {
		int size = mEPGModuleList.size();
		if (size == 0) {
			Toast.makeText(this, "epg module is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		final ArrayList<String> title_list = new ArrayList<String>();
		final ArrayList<String> value_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			Module c = mEPGModuleList.get(i);
			String title = c.getTitle();
			title_list.add(title);
			if (isFrontpage)
				value_list.add(String.valueOf(c.getIndex())); // index in programs
			else
				value_list.add(c.getLink()); // index in programs
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);

		Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select epg module")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				if (whichButton >= 0) {
						if (isFrontpage) {
							int item = Integer.valueOf(value_list.get(whichButton));
							Toast.makeText(ClipListActivity.this, "loading epg clip...", Toast.LENGTH_SHORT).show();
							new EPGTask().execute(EPG_ITEM_CATALOG, item);
						}
						else {
							if (title_list.get(whichButton).equals("直播"))
								mListLive = true;
							else
								mListLive = false;
							
							mLink = value_list.get(whichButton);
							
							int pos = mLink.indexOf("type=");
							if (pos != -1) {
								mEPGtype = mLink.substring(pos, mLink.length());
								
								if (mEPGtype.contains("type=211118")) {
									btn_bw_type.setText("4");
									if (!mIsNoVideo) {
										mIsNoVideo = true;
										imageNoVideo.setVisibility(View.VISIBLE);
									}
									Log.i(TAG, "Java: switch to audio mode");
								}
							}
							else {
								mEPGtype = null;
							}
							
							Toast.makeText(ClipListActivity.this, "loading epg clip...", Toast.LENGTH_SHORT).show();
							new EPGTask().execute(EPG_ITEM_CONTENT_SURFIX);
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
	
	private void popupEPGContentDlg() {
		int size = mEPGContentList.size();
		if (size == 0) {
			Toast.makeText(this, "epg content is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> title_list = new ArrayList<String>();
		final ArrayList<String> param_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			Content c = mEPGContentList.get(i);
			String title = c.getTitle();
			title_list.add(title);
			param_list.add(c.getParam()); // index in programs
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);

		Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select epg content")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				if (whichButton >= 0) {
					mEPGparam = param_list.get(whichButton);
					if (mEPGparam.startsWith("type="))
						mEPGtype = "";
					Log.i(TAG, String.format("Java: epg content param: %s, type: %s", mEPGparam, mEPGtype));
					mEPGlistStartPage = 1;
					
					if (mListLive) {
						if(2 == whichButton)
							mEPGtype = "164";
						else if(3 == whichButton)
							mEPGtype = "156";
						else {
							Toast.makeText(ClipListActivity.this, "invalid live type", Toast.LENGTH_SHORT).show();
							dialog.dismiss();
							return;
						}
					}
					
					new EPGTask().execute(EPG_ITEM_LIST, mEPGlistStartPage, mEPGlistCount);
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
	
	private void popupEPGCatalogDlg() {
		int size = mEPGCatalogList.size();
		if (size == 0) {
			Toast.makeText(this, "epg catalog is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> title_list = new ArrayList<String>();
		final ArrayList<String> vid_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			Catalog c = mEPGCatalogList.get(i);
			if (c.getVid() == null)
				continue;
				
			title_list.add(c.getTitle());
			vid_list.add(c.getVid()); // index in programs
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[title_list.size()]);

		Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select epg item")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				if (whichButton >= 0) {
						int vid = Integer.valueOf(vid_list.get(whichButton));
						Log.i(TAG, "Java: epg vid: " + vid);
						new EPGTask().execute(EPG_ITEM_DETAIL, vid);						
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
	
	private void popupEPGCollectionDlg() {
		int size = mEPGLinkList.size();
		if (size > 0) {
			ArrayList<String> title_list = new ArrayList<String>();
			final ArrayList<String> link_list = new ArrayList<String>();
			
			for (int i=0;i<size;i++) {
				PlayLink2 l = mEPGLinkList.get(i);
				String title = l.getTitle();
				String link = l.getId();
				title_list.add(title);
				link_list.add(link);
			}
			
			final String[] str_title_list = (String[])title_list.toArray(new String[title_list.size()]);
			
			Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
			.setTitle(String.format("Select clip to play(page #%d)", mEPGlistStartPage))
			.setItems(str_title_list, 
				new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					int vid = Integer.valueOf(link_list.get(whichButton));
					if (mListLive) {
						et_playlink.setText(String.valueOf(vid));
						Log.i(TAG, "Java: live id " + vid);
						
						boolean ret = add_list_history(mEPGLinkList.get(whichButton).getTitle(), vid);
						if (!ret)
							Toast.makeText(ClipListActivity.this, "failed to save play history", Toast.LENGTH_SHORT).show();
						
						Toast.makeText(ClipListActivity.this, String.format("live channel %s(%d) was set",
								mEPGLinkList.get(whichButton).getTitle(), vid), Toast.LENGTH_SHORT).show();
					}
					else {
						new EPGTask().execute(EPG_ITEM_DETAIL, vid);
					}
					dialog.cancel();
				}
			})
			.setPositiveButton("More...", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						if (mListSearch)
							new EPGTask().execute(EPG_ITEM_SEARCH, ++mEPGlistStartPage, mEPGlistCount);
						else
							new EPGTask().execute(EPG_ITEM_LIST, ++mEPGlistStartPage, mEPGlistCount);
					}
				})
			.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
					}
				})
			.create();
			choose_clip_dlg.show();
		}
	}
	
	private void popupSelectSubtitle() {
		final String sub_folder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test2";
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
		
		Dialog choose_subtitle_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("select subtitle")
		.setItems(str_file_list, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					subtitle_filename = sub_folder + "/" + str_file_list[whichButton];
					Log.i(TAG, "Load subtitle file: " + subtitle_filename);
					Toast.makeText(ClipListActivity.this, 
							"Load subtitle file: " + subtitle_filename, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}
			})
		.create();
		choose_subtitle_dlg.show();
	}
	
	private class FileFilterTest implements FileFilter {

		String []condition = null;

		public FileFilterTest(String []condition) {
			this.condition = condition;
		}

		@Override
		public boolean accept(File pathname) {
			// TODO Auto-generated method stub
			if (condition == null || condition.length == 0)
				return false;
			
			String filename = pathname.getName();
			String ext = filename;
			int pos = filename.lastIndexOf('.');
			if (pos == -1)
				return false;
			
			ext = filename.substring(pos + 1, filename.length());
			for (int i=0;i<condition.length;i++) {
				if (ext.equals(condition[i]))
					return true;
			}
			
			return false;
		}
	}

	private void popupDMSDlg() {
		int dev_num = IDlnaCallback.mDMSmap.size();
		
		if (dev_num == 0) {
			Log.i(TAG, "Java: dlna no dms device found");
			Toast.makeText(this, "no dlna dms device found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> dev_list = new ArrayList<String>();
		ArrayList<String> uuid_list = new ArrayList<String>();
		for (Object obj : IDlnaCallback.mDMSmap.keySet()){
	          Object name = IDlnaCallback.mDMSmap.get(obj);
	          Log.d(TAG, "Java: dlna [dlna dev] uuid: " + obj.toString() + " name: " + name.toString());
	          uuid_list.add(obj.toString());
	          dev_list.add(name.toString());
	    }
		
		final String[] str_uuid_list = (String[])uuid_list.toArray(new String[uuid_list.size()]);
		final String[] str_dev_list = (String[])dev_list.toArray(new String[dev_list.size()]);
		
		Dialog choose_dms_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("select dms")
		.setItems(str_dev_list, /*default selection item number*/
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					
					dialog.dismiss();
				}
			})
		.create();
		choose_dms_dlg.show();	
	}
	
	private void push_cdn_clip() {
		//mDLNA.EnableRendererControler(true);
		mDLNA.SetURI(mDlnaDeviceUUID, mDLNAPushUrl);
		mHandler.sendEmptyMessage(MSG_PUSH_CDN_CLIP);
	}
	
	private class EPGTask extends AsyncTask<Integer, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Integer... params) {
        	int type = params[0];
        	int id = -1;
        	if (params.length > 1)
        		id = params[1];
        	
        	boolean ret;
        	
        	mListSearch = false;
        	
        	if (EPG_ITEM_FRONTPAGE == type) {
        		if (!mEPG.frontpage()) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
        			return false;
        		}
        		
        		mEPGModuleList = mEPG.getModule();
    			if (mEPGModuleList.size() == 0)
    				return false;
        		
        		mHandler.sendEmptyMessage(MSG_EPG_FRONTPAGE_DONE);
        	}
        	else if (EPG_ITEM_CATALOG == type) {
        		if (!mEPG.catalog(id)) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return false;
        		}
        		
    			mEPGCatalogList = mEPG.getCatalog();
    			if (mEPGCatalogList.size() == 0)
    				return false;
				
				mHandler.sendEmptyMessage(MSG_EPG_CATALOG_DONE);
        	}
        	else if (EPG_ITEM_DETAIL == type){
				Log.i(TAG, "Java: epg detail() " + id);
				
        		if (!mEPG.detail(String.valueOf(id))) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return false;
        		}

    			mEPGLinkList = mEPG.getLink();
    			if (mEPGLinkList.size() == 0)
    				return false;

    			mHandler.sendEmptyMessage(MSG_EPG_DETAIL_DONE);
        	}
        	else if (EPG_ITEM_SEARCH == type) {
        		if (params.length < 2) {
        			mHandler.sendEmptyMessage(MSG_WRONG_PARAM);
					return false;
				}
        		
        		int start_page = params[1];
        		int count = params[2];
        		
        		if (!mEPG.search(mEPGsearchKey, 0, 0/* 0-只正片，1-非正片，-1=不过滤*/, start_page, count)) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
    				return false;
    			}
        		
        		mListSearch = true;
    			mEPGLinkList = mEPG.getLink();
    			if (mEPGLinkList.size() == 0)
    				return false;

    			mHandler.sendEmptyMessage(MSG_EPG_SEARCH_DONE);
        	}
        	else if (EPG_ITEM_CONTENT_LIST == type) {
        		if (!mEPG.contents_list()) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
    				return false;
    			}
        		
    			mEPGModuleList = mEPG.getModule();
    			if (mEPGModuleList.size() == 0)
    				return false;

    			mHandler.sendEmptyMessage(MSG_EPG_CONTENT_LIST_DONE);
        	}
        	else if (EPG_ITEM_CONTENT_SURFIX == type) {
        		if (mLink == null || mLink.isEmpty() || !mEPG.contents(mLink)) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return false;
        		}
        		
    			mEPGContentList = mEPG.getContent();
    			if (mEPGContentList.size() == 0)
    				return false;

    			mHandler.sendEmptyMessage(MSG_EPG_CONTENT_SURFIX_DONE);
        	}
        	else if (EPG_ITEM_LIST == type) {
        		if (!mListLive && (mEPGparam == null || mEPGparam.isEmpty() || params.length != 3)) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return false;
        		}
        		
        		if (params.length < 2) {
        			mHandler.sendEmptyMessage(MSG_WRONG_PARAM);
					return false;
				}
        		
        		int start_page = params[1];
        		int count = params[2];
        		if (mListLive) {
        			int live_type = Integer.valueOf(mEPGtype);
        			Log.i(TAG, "Java: EPGTask start to live() " + live_type);
        			ret = mEPG.live(start_page, count, live_type);
        		}
        		else {
        			ret = mEPG.list(mEPGparam, mEPGtype, start_page, "order=n", count);
        		}
        		
        		if (!ret) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return false;
        		}
        		
    			mEPGLinkList = mEPG.getLink();
    			if (mEPGLinkList.size() == 0)
    				return false;

    			mHandler.sendEmptyMessage(MSG_EPG_LIST_DONE);
        	}
        	else if (EPG_ITEM_CDN == type) {
        		Log.i(TAG, "Java: EPGTask start to getCDNUrl");
        		mDLNAPushUrl = mEPG.getCDNUrl(String.valueOf(id), btn_ft.getText().toString(), false, mIsNoVideo);
        		if (mDLNAPushUrl == null) {
            		mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
            		return false;
            	}
        		
        		if (params.length > 2)
        			mHandler.sendEmptyMessage(MSG_PLAY_CDN_URL);
        		else
        			push_cdn_clip();
        	}
        	else if (EPG_ITEM_FT == type) {
        		Log.i(TAG, "Java: EPGTask start to getCDNUrl");
        		int []ft_list = mEPG.getAvailableFT(String.valueOf(id));
        		if (ft_list == null || ft_list.length == 0) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
            		return false;
        		}
        		
        		
        		int ft = -1;
        		for (int i=ft_list.length - 1;i>=0;i--) {
        			if (ft_list[i] >= 0 && ft_list[i] < 4) {
        				ft = ft_list[i];
        				break;
        			}
        		}
        		
        		if (ft == -1) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
            		return false;
        		}
        		
        		Message msg = mHandler.obtainMessage(MSG_PLAY_CDN_FT, ft, 0);
    	        msg.sendToTarget();
        	}
        	else {
        		Log.w(TAG, "Java: EPGTask invalid type: " + type);
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
        	Log.i(TAG, "Java: doInBackground " + params[0]);
        	
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
        	if (result)
        		mHandler.sendEmptyMessage(MSG_CLIP_LIST_DONE);
        	progDlg.dismiss();
        }

        @Override
        protected void onPreExecute() {
			progDlg = new ProgressDialog(ClipListActivity.this);
			progDlg.setMessage("Loading clips");
			progDlg.setCancelable(true);
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

	private void upload_crash_report(int type) {	
		MeetSDK.makePlayerlog();
		
		FeedBackFactory fbf = new FeedBackFactory(
				 Integer.toString(type), "123456", true, false);
		fbf.asyncFeedBack();
	}
	
	private void push_to_dmr() {
		if (mPlayUrl == null || mPlayUrl.equals("")) {
			Toast.makeText(this, "no url is set", Toast.LENGTH_SHORT).show();
			return;
		}
		
		int dev_num = IDlnaCallback.mDMRmap.size();
		
		if (dev_num == 0) {
			Log.i(TAG, "Java: dlna no dlna device found");
			Toast.makeText(this, "no dlna device found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayList<String> dev_list = new ArrayList<String>();
		ArrayList<String> uuid_list = new ArrayList<String>();
		for (Object obj : IDlnaCallback.mDMRmap.keySet()){
	          Object name = IDlnaCallback.mDMRmap.get(obj);
	          Log.d(TAG, "Java: dlna [dlna dev] uuid: " + obj.toString() + " name: " + name.toString());
	          uuid_list.add(obj.toString());
	          dev_list.add(name.toString());
	    }
		
		final String[] str_uuid_list = (String[])uuid_list.toArray(new String[uuid_list.size()]);
		final String[] str_dev_list = (String[])dev_list.toArray(new String[dev_list.size()]);
		
		Dialog choose_device_dlg = new AlertDialog.Builder(ClipListActivity.this)
		.setTitle("Select device to push")
		.setItems(str_dev_list,
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				
				mDlnaDeviceUUID = str_uuid_list[whichButton];
				mDlnaDeviceName = str_dev_list[whichButton];
				
				if (mPlayUrl.startsWith("http://127.0.0.1")) {
					int link = Integer.valueOf(et_playlink.getText().toString());
					new EPGTask().execute(EPG_ITEM_CDN, link);
					dialog.cancel();
					return;
				}
				
				if (mPlayUrl.startsWith("/") || mPlayUrl.startsWith("file://")) 
					mDLNAPushUrl = mDLNA.GetServerFileUrl(mPlayUrl);
				else
					mDLNAPushUrl = mPlayUrl;
				
				push_cdn_clip();
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
		SubMenu OptSubMenu = menu.addSubMenu(Menu.NONE, OPTION, Menu.FIRST, "Option");
		OptSubMenu.setIcon(R.drawable.option);
		
		SubMenu commonMenu = OptSubMenu.addSubMenu(Menu.NONE, OPTION_COMMON, Menu.FIRST, "common");
		// dlna
		OptSubMenu.add(Menu.NONE, OPTION_DLNA_DMR, Menu.FIRST + 1, "dlna dmr");
		//OptSubMenu.add(Menu.NONE, OPTION_DLNA_DMS, Menu.FIRST + 2, "dlna dms");
		// epg
		OptSubMenu.add(Menu.NONE, OPTION_EPG_FRONTPAGE, Menu.FIRST + 3, "epg frontpage");
		OptSubMenu.add(Menu.NONE, OPTION_EPG_CONTENT, Menu.FIRST + 4, "epg content");
		OptSubMenu.add(Menu.NONE, OPTION_EPG_SEARCH, Menu.FIRST + 5, "epg search");
				
		MenuItem previewMenuItem = commonMenu.add(Menu.NONE, OPTION_COMMON_PREVIEW, Menu.FIRST, "Preview");
		previewMenuItem.setCheckable(true);
		if (mIsPreview)
			previewMenuItem.setChecked(true);
		
		MenuItem loopMenuItem = commonMenu.add(Menu.NONE, OPTION_COMMON_LOOP, Menu.FIRST + 1, "Loop");
		loopMenuItem.setCheckable(true);
		if (mIsLoop)
			loopMenuItem.setChecked(true);
		
		noVideoMenuItem = commonMenu.add(Menu.NONE, OPTION_COMMON_NO_VIDEO, Menu.FIRST + 2, "NoVideo");
		noVideoMenuItem.setCheckable(true);
		if ("4".equals(btn_bw_type.getText())) // cdn play
			noVideoMenuItem.setEnabled(true);
		else
			noVideoMenuItem.setEnabled(false);
		if (mIsNoVideo)
			noVideoMenuItem.setChecked(false);
		
		commonMenu.add(Menu.NONE, OPTION_COMMON_MEETVIEW, Menu.FIRST + 3, "test view");
		commonMenu.add(Menu.NONE, OPTION_COMMON_SUBTITLE, Menu.FIRST + 4, "load subtitle");
		
		menu.add(Menu.NONE, UPDATE_CLIP_LIST, Menu.FIRST + 1, "Update list")
			.setIcon(R.drawable.list);
		menu.add(Menu.NONE, UPDATE_APK, Menu.FIRST + 2, "Update apk")
			.setIcon(R.drawable.update);
		menu.add(Menu.NONE, UPLOAD_CRASH_REPORT, Menu.FIRST + 3, "Upload crash report")
			.setIcon(R.drawable.log);
		menu.add(Menu.NONE, QUIT, Menu.FIRST + 4, "Quit");
		
		return true;//super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
		case OPTION_COMMON_PREVIEW:
			if (mIsPreview)
				item.setChecked(false);
			else
				item.setChecked(true);
			mIsPreview = !mIsPreview;
			Util.writeSettingsInt(this, "isPreview", mIsPreview ? 1 : 0);
			break;
		case OPTION_COMMON_LOOP:
			if (mIsLoop)
				item.setChecked(false);
			else
				item.setChecked(true);
			mIsLoop = !mIsLoop;
			Util.writeSettingsInt(this, "isLoop", mIsLoop ? 1 : 0);
			Log.i(TAG, "set loop to: " + mIsLoop);
			break;
		case OPTION_COMMON_NO_VIDEO:
			if (mIsNoVideo) {
				item.setChecked(false);
				imageNoVideo.setVisibility(View.GONE);
			}
			else {
				item.setChecked(true);
				imageNoVideo.setVisibility(View.VISIBLE);
			}
			mIsNoVideo = !mIsNoVideo;
			Util.writeSettingsInt(this, "IsNoVideo", mIsNoVideo ? 1 : 0);
			break;
		case OPTION_COMMON_MEETVIEW:
			Intent intent = new Intent(ClipListActivity.this, MeetViewActivity.class);
			startActivity(intent);
			break;
		case OPTION_COMMON_SUBTITLE:
			popupSelectSubtitle();
			break;
		case OPTION_DLNA_DMR:
			push_to_dmr();
			break;
		case OPTION_DLNA_DMS:
			popupDMSDlg();
			break;
		case OPTION_EPG_FRONTPAGE:
			/*if (mEPGModuleList != null)
				mHandler.sendEmptyMessage(MSG_EPG_FRONTPAGE_DONE);
			else {*/
				Toast.makeText(this, "loading epg catalog...", Toast.LENGTH_SHORT).show();
				new EPGTask().execute(EPG_ITEM_FRONTPAGE);
			//}
			break;
		case OPTION_EPG_SEARCH:
			final EditText inputKey = new EditText(this);
        	String last_key = Util.readSettings(this, "last_searchkey");
        	Log.i(TAG, "Java last_key: " + last_key);
        	inputKey.setText(last_key);
			inputKey.setHint("input search key");
			
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("input key").setIcon(android.R.drawable.ic_dialog_info).setView(inputKey)
	                .setNegativeButton("Cancel", null);
	        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface dialog, int which) {
	            	mEPGsearchKey = inputKey.getText().toString();
	            	Log.i(TAG, "Java save last_key: " + mEPGsearchKey);
	            	Util.writeSettings(ClipListActivity.this, "last_searchkey", mEPGsearchKey);
					
	            	Toast.makeText(ClipListActivity.this, "search epg...", Toast.LENGTH_SHORT).show();
	            	mEPGlistStartPage = 1;
	    			new EPGTask().execute(EPG_ITEM_SEARCH, mEPGlistStartPage, mEPGlistCount);
	             }
	        });
	        builder.show();
			break;
		case OPTION_EPG_CONTENT:
			Toast.makeText(this, "loading epg contents...", Toast.LENGTH_SHORT).show();
			new EPGTask().execute(EPG_ITEM_CONTENT_LIST);
			break;
		default:
			Log.w(TAG, "bad menu item selected: " + id);
			return false;
		}
		
		return true;//super.onOptionsItemSelected(item);
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
			else if(MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER == extra)
				str_player_type = "XO Player";
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

	@Override
	public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
		// TODO Auto-generated method stub
		Log.e(TAG, "Java: onError: " + framework_err + "," + impl_err);
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
		
		mVideoWidth = mp.getVideoWidth();
		mVideoHeight = mp.getVideoHeight();
		
		// view
		int width	= mLayout.getMeasuredWidth();
		int height 	= mLayout.getMeasuredHeight();
		
		Log.i(TAG, String.format("adjust_ui preview %d x %d, video %d x %d", width, height, mVideoWidth, mVideoHeight)); 
		
		mPreview.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
		
		RelativeLayout.LayoutParams sufaceviewParams = (RelativeLayout.LayoutParams) mPreview.getLayoutParams();
		if ( mVideoWidth * height  > width * mVideoHeight ) { 
			Log.i(TAG, "adjust_ui surfaceview is too tall, correcting");
			sufaceviewParams.width	= width;
			sufaceviewParams.height = width * mVideoHeight / mVideoWidth;
		}
		else if ( mVideoWidth * height  < width * mVideoHeight ) { 
			Log.i(TAG, "adjust_ui surfaceview is too wide, correcting"); 
			sufaceviewParams.width = height * mVideoWidth / mVideoHeight;
			sufaceviewParams.height= height;
		}
		else {
           sufaceviewParams.height	= height;
           sufaceviewParams.width 	= width;
		}
		
		Log.i(TAG, String.format("adjust_ui surfaceview setLayoutParams %d %d", 
				sufaceviewParams.width, sufaceviewParams.height)); 
		mPreview.setLayoutParams(sufaceviewParams);
		
		mPreview.BindInstance(mMediaController, mPlayer);
		
		Log.i(TAG, String.format("Java: width %d, height %d", mPlayer.getVideoWidth(), mPlayer.getVideoHeight()));
		mPlayer.start();
		
		attachMediaController();
		
		mBufferingProgressBar.setVisibility(View.GONE);
		mIsBuffering = false;
		
		// subtitle
		if (mPlayUrl.startsWith("/")) {
			// local file
			if (subtitle_filename == null) {
				String subtitle_full_path;
				int index = mPlayUrl.lastIndexOf('.') + 1;
				String tmp = mPlayUrl.substring(0, index);
				
				String[] exts = {"srt", "ass"};
				for(String ext:exts) {
					subtitle_full_path = tmp + ext;
					
					File subfile = new File(subtitle_full_path);
					//Log.d(TAG, "Java: subtitle: subtitle file: " + subtitle_full_path);
			        if (subfile.exists()) {
			        	subtitle_filename = subtitle_full_path;
						break;
			        }
				}
			}
			
			if (subtitle_filename != null) {
				Log.i(TAG, "Java: subtitle: subtitle file found: " + subtitle_filename);
	        	
				mSubtitleParser = new SimpleSubTitleParser();
				mSubtitleParser.setOnPreparedListener(this);
				
				mSubtitleParser.setDataSource(subtitle_filename);
				mSubtitleParser.prepareAsync();
			}
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onBufferingUpdate: " + percent);
		mBufferingPertent = percent;
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
		// TODO Auto-generated method stub
		Log.i(TAG, String.format("onVideoSizeChanged(%d %d)", w, h));
		
		if (w == 0 || h == 0) {
			mHolder.setFixedSize(640, 480);
			mPreview.SetVideoRes(640, 480);
			Log.i(TAG, "Java: onVideoSizeChanged, no video stream, use default resolution: 640x480");
		}
		else {
			mHolder.setFixedSize(w, h);
			mPreview.SetVideoRes(w, h);
		}
		
		// will trigger onMeasure() 
		mPreview.measure(MeasureSpec.AT_MOST, MeasureSpec.AT_MOST);
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
			
			builder.setMessage("Download new APK?");

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
		path = Environment.getExternalStorageDirectory().getPath() + "/" + apk_name;
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

		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int screen_height	= dm.heightPixels;
		if (!isLandscape) {
			Log.i(TAG, String.format("screen %dx%d, preview height %d", screen_width, screen_height, preview_height));
			
			preview_height = screen_height * 2 / 5;
			mLayout.getLayoutParams().height = preview_height;
			mLayout.requestLayout(); //or invalidate();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "Java: onPause()");

		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.pause();
		}
			
		//MeetSDK.closeLog();
		
		//MediaSDK.stopP2PEngine();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unbindService(dlna_conn);
		Log.i(TAG, "Java: onDestroy()");
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
	
	private void initFeedback() {
		LogcatHelper helper = LogcatHelper.getInstance();
		helper.init(this);
		AtvUtils.sContext = this;
		FeedBackFactory.sContext = this;
	}
	
	DLNAService.MyBinder binder;
	private ServiceConnection dlna_conn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Java: ClipActivity onServiceConnected()");
			binder = (DLNAService.MyBinder)service;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Java: ClipActivity onServiceDisconnected()");
		}
		
	};
	
	boolean add_list_history(String title, int playlink) {
		String key = "PlayHistory";
		String regularEx = ",";
		String values;
	    values = Util.readSettings(this, key);
	    
	    Log.i(TAG, "Java: PlayHistory read: " + values);
	    String []str = values.split(regularEx);
	    int start = str.length - 10;
	    if (start < 0)
	    	start = 0;
	    for (int i=0;i<str.length;i++) {
	    	Log.d(TAG, String.format("Java: PlayHistory #%d %s", i, str[i]));
	    }
	    
	    // clip_name|11223,clip_2|34455
	    StringBuffer save_values = new StringBuffer();
	    for (int i=start;i<str.length;i++) {
	    	save_values.append(str[i]);
	    	save_values.append(regularEx);
	    }
	    
	    save_values.append(title);
	    save_values.append("|");
	    save_values.append(playlink);
		
	    //Log.d(TAG, "Java: PlayHistory write: " + save_values.toString());
	    return Util.writeSettings(this, key, save_values.toString());
	}
	
	private boolean initDLNA() {
		//bindService(new Intent(DLNAService.ACTION), serv_conn, BIND_AUTO_CREATE);
		
		Intent intent = new Intent();
		intent.setAction(DLNAService.ACTION);
		//startService(intent);
		bindService(intent, dlna_conn, Service.BIND_AUTO_CREATE);
		
		mDLNA = new DLNASdk();
		if (!mDLNA.isLibLoadSuccess()) {
			Log.e(TAG, "Java: dlna failed to load dlna lib");
			return false;
		}
		
		mDLNAcallback = new IDlnaCallback(this);
		//mDLNA.setLogPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/xxxx_dlna.log");
		mDLNA.Init(mDLNAcallback);
		mDLNA.EnableRendererControler(true);
		
		//start file server
		Random rand =new Random();
		int i;
		i = rand.nextInt(100);
		int port = DLNA_LISTEN_PORT + i;
		mDLNA.StartHttpServer(port);
		Log.i(TAG, String.format("Java: dlna start dlna server port: %d", port));
		return true;
	}
	
    private void attachMediaController() {
        mMediaController.setMediaPlayer(this);
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
        
        while (!mStoped) {
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
    			target_msec = mPlayer.getDuration();
        	}
        
            while (mPlayer != null && mPlayer.getCurrentPosition() < target_msec/* || mSubtitleSeeking == true*/) {
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

	@Override
	public void onSeekComplete() {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: subtitle onSeekComplete");
		mSubtitleSeeking = false;
	}
	// end of "callback of subtitle"
	
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		// TODO Auto-generated method stub
		mPreviewFocused = hasFocus;
		
		if (hasFocus) {
			if (mLayout != null) {
				Drawable drawable1 = getResources().getDrawable(R.drawable.bg_border1); 
				mLayout.setBackground(drawable1);
			}
		}
		else {
			if (mLayout != null) {
				Drawable drawable2 = getResources().getDrawable(R.drawable.bg_border2); 
				mLayout.setBackground(drawable2);
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode: " + keyCode);
		int incr = -1;
		
		if (!mPreviewFocused)
			return super.onKeyDown(keyCode, event);
		
		switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_MENU:
				if (mPlayer != null && !mMediaController.isShowing()) {
					mMediaController.show(5000);
					return true;
				}
				break;
			default:
				Log.d(TAG, "no spec action: " + keyCode);
				break;
			}
		
		return super.onKeyDown(keyCode, event);
	}
	
	static {
		//System.loadLibrary("lenthevcdec");
	}

	// dlna interface
	@Override
	public void OnDeviceAdded(String uuid, String firendname, String logourl,
			int devicetype) {}

	@Override
	public void OnDeviceRemoved(String uuid, int devicetype) {}

	@Override
	public void OnLogPrintf(String msg) {}

	@Override
	public boolean OnConnect(String uuid, String requestName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void OnConnectCallback(String uuid, int state) {}

	@Override
	public void OnDisConnect(String uuid) {}

	@Override
	public void OnDisConnectCallback(String uuid, boolean isTimeout) {}

	@Override
	public void OnRemoveTransportFile(String uuid, String transportuuid) {}

	@Override
	public void OnRemoveTransportFileCallback(String uuid,
			String transportuuid, boolean isTimeout) {}

	@Override
	public void OnAddTransportFile(String uuid, String transportuuid,
			String fileurl, String filename, String thumburl) {}

	@Override
	public void OnAddTransportFileCallback(String uuid, String transportuuid,
			int state) {}

	@Override
	public int OnSetURI(String url, String urltitle, String remoteip,
			int mediatype) {
		// TODO Auto-generated method stub
		if (mDLNA != null)
			mDLNA.Play(mDlnaDeviceUUID);
		
		return 0;
	}

	@Override
	public void OnPlay() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnPause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnStop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnSeek(long position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnSetVolume(long volume) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnSetMute(boolean mute) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnVolumeChanged(String uuid, long lVolume) {}

	@Override
	public void OnMuteChanged(String uuid, boolean bMute) {}

	@Override
	public void OnPlayStateChanged(String uuid, String state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnPlayUrlChanged(String uuid, String url) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnContainerChanged(String uuid, String item_id, String update_id) {}

	@Override
	public void OnGetCaps(String uuid, String caps) {}

	@Override
	public void OnSetUrl(String uuid, long error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnBrowse(boolean success, String uuid, String objectid,
			long count, long total, DLNASdkDMSItemInfo[] filelists) {
		// TODO Auto-generated method stub
		
	}
}
