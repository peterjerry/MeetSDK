package com.gotye.meetplayer.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.gotye.common.ZGUrl;
import com.gotye.common.iqiyi.IqiyiUtil;
import com.gotye.common.youku.YKUtil;
import com.gotye.crashhandler.UploadLogTask;
import com.gotye.meetplayer.adapter.LocalFileAdapter;
import com.gotye.meetplayer.ui.MyPreView2;
import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetsdk.player.MediaController.MediaPlayerControl;
import com.gotye.meetsdk.player.MediaInfo;
import com.gotye.meetsdk.player.MediaPlayer;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.TrackInfo;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.subtitle.SubTitleParser;
import com.gotye.meetsdk.subtitle.SubTitleSegment;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.pptv.CDNItem;
import com.gotye.common.pptv.Catalog;
import com.gotye.common.pptv.Content;
import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.Episode;
import com.gotye.common.pptv.Module;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.pptv.VirtualChannelInfo;
import com.gotye.common.sohu.PlaylinkSohu;
import com.gotye.common.sohu.PlaylinkSohu.SohuFtEnum;
import com.gotye.common.sohu.SohuUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.common.util.PlayBackTime;
import com.gotye.common.util.httpUtil;
import com.gotye.db.MediaStoreDatabaseHelper;
import com.pplive.dlna.DLNASdk;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.service.MediaScannerService;
import com.gotye.meetplayer.service.MyHttpService;
import com.gotye.meetplayer.ui.widget.MiniMediaController;
import com.gotye.meetplayer.ui.widget.MyMarqueeTextView;
import com.gotye.meetplayer.util.AtvUtils;
import com.gotye.meetplayer.util.DownloadAsyncTask;
import com.gotye.meetplayer.util.FeedBackFactory;
import com.gotye.meetplayer.util.FileFilterTest;
import com.gotye.meetplayer.util.IDlnaCallback;
import com.gotye.meetplayer.util.ListMediaUtil;
import com.gotye.meetplayer.util.LoadPlayLinkUtil;
import com.gotye.meetplayer.util.LogcatHelper;
import com.gotye.meetplayer.util.NetworkSpeed;
import com.gotye.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

// ONLY support external subtitle???
public class ClipListActivity extends AppCompatActivity implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayerControl, SurfaceHolder.Callback,
        SubTitleParser.OnReadyListener, OnFocusChangeListener {

    private final static String TAG = "ClipList";

    private final static String PORT_HTTP = "http";
    private final static String PORT_RTSP = "rtsp";

    private MyMarqueeTextView tv_title;
    private AppCompatButton btnPlay;
    private AppCompatButton btnSelectTime;
    private AppCompatButton btnClipLocation;
    private AppCompatButton btnPlayerImpl;
    private AppCompatButton btnPPboxSel;
    private AppCompatButton btnTakeSnapShot;
    private AppCompatButton btnSelectAudioTrack;
    private MyPreView2 mPreview;
    private boolean mPreviewFocused = false;
    private SurfaceHolder mHolder;
    private MiniMediaController mMediaController;
    private RelativeLayout mLayout;
    private ProgressBar mBufferingProgressBar;
    private EditText et_playlink;
    private AppCompatButton btn_ft;
    private AppCompatButton btn_bw_type;
    private ImageView imageDMR;
    private ImageView imageNoVideo;
    private ImageView imageBackward;
    private ImageView imageForward;
    private MediaPlayer mPlayer;
    private LocalFileAdapter mAdapter;
    private ListView lv_filelist;

    private ProgressBar mDownloadProgressBar;
    private TextView mProgressTextView;
    private Dialog mUpdateDialog;

    private int screen_width, screen_height;

    private DecodeMode mDecMode = DecodeMode.AUTO;
    private boolean mIsPreview;
    private boolean mIsLoop = false;
    private boolean mIsNoVideo = false;
    private boolean mIsRememberPos = true;
    private boolean mIsListAudioFile = false;
    private boolean mDebugInfo = false;
    private boolean mIsFlinging = false;

    private int mBufferingPertent = 0;
    private boolean mPrepared = false;
    private boolean mIsBuffering = false;
    private boolean mSubtitleStoped = false;
    private boolean mHomed = false;

    private WifiLock mWifiLock;

    private boolean isTVbox = false;
    private boolean isLandscape = false;

    // playback time
    private PlayBackTime mPlaybackTime;

    // list
    private ListMediaUtil mListUtil;
    //private final static String HTTP_SERVER_URL = "http://192.168.1.114:8088/testcase/";
    private final static String HTTP_SERVER_URL = "http://42.62.105.235/test/media/testcase/";

    private String mPlayUrl;
    private int mVideoWidth, mVideoHeight;
    private int mAudioTrackCount = 0;
    private int mAudioSelectedTrack = -1;
    private int mAudioFirstTrack = -1;
    private int mPlayerImpl = 0;
    // subtitle
    private SimpleSubTitleParser mSubtitleParser;
    private TextView mSubtitleTextView;
    private String mSubtitleText;
    private Thread mSubtitleThread;
    private boolean mSubtitleSeeking = false;
    private boolean mIsSubtitleUsed = false;
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

    private List<Content> mEPGContentList = null;
    private List<Module> mEPGModuleList = null;
    private List<Catalog> mEPGCatalogList = null;
    private List<PlayLink2> mEPGLinkList = null;
    private List<Episode> mVirtualLinkList = null;
    private String mEPGsearchKey; // for search
    private String mDLNAPushUrl;
    private String mLink;
    private String mEPGparam;
    private String mEPGtype;
    private int mEPGlistStartPage = 1;
    private int mEPGlistCount = 15;
    private boolean mListSearch = false;
    private boolean mIsVirtualChannel = false;
    private String mExtid;
    private int mSavedPlayLink; // for MeetViewActivity

    private final int EPG_ITEM_FRONTPAGE = 1;
    private final int EPG_ITEM_CATALOG = 2;
    private final int EPG_ITEM_DETAIL = 3;
    private final int EPG_ITEM_SEARCH = 4;
    private final int EPG_ITEM_CONTENT_LIST = 5;
    private final int EPG_ITEM_CONTENT_SURFIX = 6;
    private final int EPG_ITEM_LIST = 7;
    private final int EPG_ITEM_VIRTUAL_SOHU = 8;
    private final int EPG_ITEM_VIRTUAL_LIST = 9;
    private final int EPG_ITEM_CDN = 11;
    private final int EPG_ITEM_FT = 12;

    private boolean mListLocalFile = true;

    private TextView mTextViewInfo = null;

    private int decode_fps = 0;
    private int render_fps = 0;
    private int decode_avg_msec = 0;
    private int render_avg_msec = 0;
    private int render_frame_num = 0;
    private int decode_drop_frame = 0;
    private int av_latency_msec = 0;
    private int video_bitrate = 0;
    private int rx_speed = 0;
    private int tx_speed = 0;

    private int preview_height;

    private boolean mIsLivePlay = false;

    private String mAudioDst;
    private NetworkSpeed mSpeed;

    final static int ONE_MAGEBYTE = 1048576;
    final static int ONE_KILOBYTE = 1024;

    private BroadcastReceiver mScannerReceiver;
    private MediaStoreDatabaseHelper mMediaDB;

    private BroadcastReceiver mHttpServiceReceiver;

    // message
    private final static int MSG_CLIP_PLAY_DONE = 102;
    private static final int MSG_UPDATE_PLAY_INFO = 201;
    private static final int MSG_UPDATE_RENDER_INFO = 202;
    private static final int MSG_DISPLAY_SUBTITLE = 401;
    private static final int MSG_HIDE_SUBTITLE = 402;
    private static final int MSG_EPG_FRONTPAGE_DONE = 501;
    private static final int MSG_EPG_CATALOG_DONE = 502;
    private static final int MSG_EPG_DETAIL_DONE = 503;
    private static final int MSG_EPG_SEARCH_DONE = 504;
    private static final int MSG_EPG_CONTENT_LIST_DONE = 505;
    private static final int MSG_EPG_CONTENT_SURFIX_DONE = 506;
    private static final int MSG_EPG_LIST_DONE = 507;
    private static final int MSG_FAIL_TO_CONNECT_EPG_SERVER = 511;
    private static final int MSG_FAIL_TO_PARSE_EPG_RESULT = 512;
    private static final int MSG_WRONG_PARAM = 513;
    private static final int MSG_PUSH_CDN_CLIP = 601;
    private static final int MSG_PLAY_CDN_URL = 602;
    private static final int MSG_PLAY_CDN_FT = 603;

    private static final int MSG_UPDATE_NETWORK_SPEED = 701;

    private ProgressDialog progDlg = null;

    private String mCurrentFolder;

    private final static String home_folder = "";//"/test2";

    private final static String HTTP_UPDATE_APK_URL = "http://42.62.105.235/test/app/";

    private final String[] from = {"filename", "mediainfo", "folder", "filesize", "resolution", "thumb"};

    private final int[] to = {R.id.tv_filename, R.id.tv_mediainfo, R.id.tv_folder,
            R.id.tv_filesize, R.id.tv_resolution, R.id.iv_thumb};

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏标题栏
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        try {
            super.getWindow().addFlags(
                    WindowManager.LayoutParams.class.
                            getField("FLAG_NEEDS_MENU_KEY").getInt(null));
        } catch (NoSuchFieldException e) {
            // Ignore since this field won't exist in most versions of Android
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen_width = dm.widthPixels;
        screen_height = dm.heightPixels;
        LogUtil.info(TAG, String.format("Java: screen %dx%d", screen_width, screen_height));

        if (screen_width > screen_height)
            isTVbox = true;
        else
            isTVbox = false;

        if (isTVbox) {
            setContentView(R.layout.list_landscape);

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        } else {
            setContentView(R.layout.list);
        }

        //if (getSupportActionBar() != null)
        //    getSupportActionBar().hide();

        Util.upload_crash_dump(this);

        this.tv_title = (MyMarqueeTextView) this.findViewById(R.id.tv_title);
        this.btnPlay = (AppCompatButton) this.findViewById(R.id.btn_play);
        this.btnSelectTime = (AppCompatButton) this.findViewById(R.id.btn_select_time);
        this.btnClipLocation = (AppCompatButton) this.findViewById(R.id.btn_clip_location);
        this.btnPlayerImpl = (AppCompatButton) this.findViewById(R.id.btn_player_impl);
        this.btnPPboxSel = (AppCompatButton) this.findViewById(R.id.btn_ppbox);
        this.btnTakeSnapShot = (AppCompatButton) this.findViewById(R.id.btn_take_snapshot);
        this.btnSelectAudioTrack = (AppCompatButton) this.findViewById(R.id.btn_select_audiotrack);
        this.et_playlink = (EditText) this.findViewById(R.id.et_playlink);
        this.btn_ft = (AppCompatButton) this.findViewById(R.id.btn_ft);
        this.btn_bw_type = (AppCompatButton) this.findViewById(R.id.btn_bw_type);
        this.imageDMR = (ImageView) this.findViewById(R.id.iv_dlna_dmc);
        this.imageNoVideo = (ImageView) this.findViewById(R.id.iv_novideo);

        this.mPreview = (MyPreView2) this.findViewById(R.id.preview);
        this.mLayout = (RelativeLayout) this.findViewById(R.id.layout_preview);

        this.mBufferingProgressBar = (ProgressBar) this.findViewById(R.id.progressbar_buffering);
        this.mSubtitleTextView = (TextView) this.findViewById(R.id.textview_subtitle);

        this.mMediaController = (MiniMediaController) this.findViewById(R.id.mmc);

        this.imageBackward = (ImageView) this.findViewById(R.id.iv_seekbackward);
        this.imageForward = (ImageView) this.findViewById(R.id.iv_seekforward);

        mLayout.setLongClickable(true); // MUST set to enable double-tap and single-tap-confirm
        mLayout.setOnTouchListener(mOnTouchListener);
        mMediaController.setInstance(this);

        mTextViewInfo = (TextView) this.findViewById(R.id.tv_info);
        mTextViewInfo.setTextColor(Color.RED);
        mTextViewInfo.setTextSize(18);
        mTextViewInfo.setTypeface(Typeface.MONOSPACE);

        mLayout.setFocusable(true);
        mLayout.setOnFocusChangeListener(this);

        // set to false to solve cannot show menu problem
        tv_title.setMarquee(false);

        if (home_folder.equals("")) {
            mCurrentFolder = "";
        } else {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mCurrentFolder = Environment.getExternalStorageDirectory().getPath() + home_folder;
                File file = new File(mCurrentFolder);
                if (!file.isDirectory()) {
                    mCurrentFolder = Environment.getExternalStorageDirectory().getPath();
                }
                tv_title.setText(mCurrentFolder);
            } else {
                Toast.makeText(this, "sd card is not mounted!", Toast.LENGTH_SHORT).show();
            }
        }

        mListUtil = new ListMediaUtil(this);
        new ListItemTask().execute(mCurrentFolder);

        initFeedback();

        mEPG = new EPGUtil();

        if (Util.initMeetSDK(this) == false) {
            Toast.makeText(this, "failed to load meet lib",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (initDLNA() == false) {
            Toast.makeText(this, "failed to load dlna lib",
                    Toast.LENGTH_SHORT).show();
        }

        if (!Util.startP2PEngine(this)) {
            Toast.makeText(this, "failed to start p2p engine",
                    Toast.LENGTH_SHORT).show();
        }

        mMediaDB = MediaStoreDatabaseHelper.getInstance(this);

        mPlaybackTime = new PlayBackTime(this);

        mHolder = mPreview.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
        mHolder.addCallback(this);

        this.lv_filelist = (ListView) findViewById(R.id.lv_filelist);
        this.lv_filelist
                .setOnItemClickListener(new ListView.OnItemClickListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view,
                                            int position, long id) {
                        // TODO Auto-generated method stub
                        LogUtil.info(TAG, String.format("onItemClick %d %d", position, id));

                        Map<String, Object> item = mAdapter.getItem(position);
                        String file_name = (String) item.get("filename");
                        String file_path = (String) item.get("fullpath");
                        LogUtil.info(TAG, String.format("Java: full_path %s", file_path));

                        if (file_name.equals("..")) {
                            // up to parent folder
                            if (mListLocalFile) {
                                File file = new File(mCurrentFolder);
                                String parent_folder = file.getParent();
                                if (parent_folder == null || parent_folder == mCurrentFolder) {
                                    LogUtil.info(TAG, "already at root folder");
                                } else {
                                    mCurrentFolder = parent_folder;
                                    tv_title.setText(mCurrentFolder);
                                    new ListItemTask().execute(mCurrentFolder);
                                }
                            } else {
                                // http parent folder list
                                String url = file_path;
                                int index = url.lastIndexOf('/', url.length() - 1 - 1);
                                url = url.substring(0, index + 1);
                                new ListItemTask().execute(url);
                                tv_title.setText(url);
                            }
                        } else {
                            if (file_path.startsWith("http://")) {
                                LogUtil.info(TAG, "Java: http list file clicked");

                                if (file_path.charAt(file_path.length() - 1) == '/') {
                                    LogUtil.info(TAG, "Java: list http folder");
                                    tv_title.setText(file_path);
                                    new ListItemTask().execute(file_path);
                                } else {
                                    LogUtil.info(TAG, "Java: play http clip");
                                    String filename = file_path;
                                    int pos = file_path.lastIndexOf('/');
                                    if (pos != -1)
                                        filename = file_path.substring(pos + 1);
                                    start_player(filename, file_path);
                                }
                            } else {
                                // local file
                                File file = new File(file_path);
                                if (!file.exists()) {
                                    mMediaDB.deleteMediaInfo(file_path);

                                    List<Map<String, Object>> filelist = mListUtil.getList();
                                    filelist.remove(position);
                                    mAdapter.updateData(filelist);
                                    mAdapter.notifyDataSetChanged();

                                    Toast.makeText(ClipListActivity.this,
                                            "clip didn't exist and removed from list",
                                            Toast.LENGTH_SHORT).show();
                                }

                                if (file.isDirectory()) {
                                    File[] temp = file.listFiles();
                                    if (temp == null || temp.length == 0) {
                                        Toast.makeText(ClipListActivity.this, "folder is not valid or empty",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        LogUtil.info(TAG, "Java: list folder: " + file.getAbsolutePath());
                                        mCurrentFolder = file_path;
                                        tv_title.setText(mCurrentFolder);
                                        new ListItemTask().execute(mCurrentFolder);
                                    }
                                } else {
                                    start_player(file.getName(), file_path);
                                }
                            }
                        }
                    }
                });

        this.lv_filelist.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view,
                                           final int position, long id) {
                // TODO Auto-generated method stub
                final String[] action = {"delete", "rename", "detail"};
                Map<String, Object> item = mAdapter.getItem(position);
                final String file_name = (String) item.get("filename");
                final String file_path = (String) item.get("fullpath");

                Dialog choose_action_dlg = new AlertDialog.Builder(ClipListActivity.this)
                        .setTitle("select action")
                        .setItems(action, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton == 0) {
                                    // delete
                                    if (file_path.startsWith("/") || file_path.startsWith("file://")) {
                                        File file = new File(file_path);
                                        if (file.exists() && file.delete()) {
                                            mMediaDB.deleteMediaInfo(file.getAbsolutePath());

                                            LogUtil.info(TAG, "file: " + file_path + " deleted");
                                            Toast.makeText(ClipListActivity.this, "file " + file_name + " deleted!", Toast.LENGTH_SHORT).show();

                                            List<Map<String, Object>> filelist = mListUtil.getList();
                                            filelist.remove(position);
                                            mAdapter.updateData(filelist);
                                            mAdapter.notifyDataSetChanged();
                                        } else {
                                            LogUtil.error(TAG, "failed to delete file: " + file_path);
                                            Toast.makeText(ClipListActivity.this, "failed to delte file: " + file_path, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(ClipListActivity.this, "DELETE only support local file", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (whichButton == 1) {
                                    final EditText inputFilename = new EditText(ClipListActivity.this);
                                    inputFilename.setText(file_name);
                                    inputFilename.setHint("input new file name");

                                    AlertDialog.Builder builder = new AlertDialog.Builder(
                                            ClipListActivity.this);
                                    builder.setTitle("input new file name")
                                            .setIcon(android.R.drawable.ic_dialog_info)
                                            .setView(inputFilename)
                                            .setNegativeButton("Cancel", null);
                                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            int pos = file_path.lastIndexOf("/");
                                            if (pos == -1)
                                                return;

                                            String newFilename = inputFilename.getText().toString();
                                            LogUtil.info(TAG, String.format("Java change filename from %s to %s",
                                                    file_name, newFilename));
                                            String new_filepath = file_path.substring(0, pos + 1) +
                                                    inputFilename.getText().toString();

                                            File file = new File(file_path);
                                            file.renameTo(new File(new_filepath));
                                            Toast.makeText(ClipListActivity.this,
                                                    String.format("change filename from %s to %s",
                                                            file_name, newFilename),
                                                    Toast.LENGTH_SHORT).show();

                                            Map<String, Object> item = mListUtil.getList().get(position);
                                            item.put("filename", newFilename);
                                            item.put("fullpath", new_filepath);
                                            mListUtil.getList().set(position, item);

                                            mAdapter.updateData(mListUtil.getList());
                                            mAdapter.notifyDataSetChanged();

                                            mMediaDB.updatePath(file_path, new_filepath);
                                        }
                                    });
                                    builder.show();
                                } else if (whichButton == 2) {
                                    MediaInfo info = mMediaDB.getMediaInfo(file_path);
                                    if (info == null) {
                                        Toast.makeText(ClipListActivity.this, "media info is null", Toast.LENGTH_SHORT).show();
                                    } else {
                                        String strInfo = Util.getMediaInfoDescription(file_path, info);
                                        if (strInfo == null) {
                                            Toast.makeText(ClipListActivity.this, "strInfo is null", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                                ClipListActivity.this)
                                                .setTitle("媒体信息")
                                                .setMessage(strInfo)
                                                .setPositiveButton("确定", null);

                                        builder.show();
                                    }
                                } else {
                                    LogUtil.error(TAG, "Java: more action: unknown index " + whichButton);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                choose_action_dlg.show();

                return true; // already handled
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
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
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
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
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
                final String[] PlayerImpl = {"Auto", "System", "XOPlayer", "FFPlayer", "OMXPlayer"};

                Dialog choose_player_impl_dlg = new AlertDialog.Builder(ClipListActivity.this)
                        .setTitle("select player impl")
                        .setSingleChoiceItems(PlayerImpl, mPlayerImpl, /*default selection item number*/
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        LogUtil.info(TAG, "select player impl: " + whichButton);

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
                LogUtil.debug(TAG, "Java: PlayHistory(in PPboxSel) read: " + values);

                String[] str = values.split(regularEx);
                for (int i = 0; i < str.length; i++) {
                    // 后遗症|1233
                    LogUtil.info(TAG, "Java: history item #" + i + ": " + str[i]);
                    int pos = str[i].indexOf("|");
                    if (pos != -1) {
                        list_title.add(str[i].substring(0, pos));
                        list_vid.add(str[i].substring(pos + 1, str[i].length()));
                    }
                }

                final int history_size = list_vid.size();

                LogUtil.info(TAG, String.format("Java: fixed_size %d, history_size %d", fixed_size, history_size));

                // load tvlist.txt
                LoadPlayLinkUtil ext_link = new LoadPlayLinkUtil();
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tvlist.txt";
                if (ext_link.LoadTvList(path)) {
                    LogUtil.info(TAG, "Java: add tvlist.txt prog into list");
                    list_title.addAll(ext_link.getTitles());
                    list_url.addAll(ext_link.getUrls());
                }

                final String[] dlg_clipname = (String[]) list_title.toArray(new String[list_title.size()]);
                LogUtil.info(TAG, "Java: final dlg_clipname size " + dlg_clipname.length);

                final int ppbox_playlink[] = {18139131, 10110649, 17054339, 17461610, 17631361, 17611359};
                final int ppbox_ft[] = {2, 2, 2, 2, 3, 2};//2-超清, 3-bd

                Dialog choose_ppbox_res_dlg = new AlertDialog.Builder(ClipListActivity.this)
                        .setTitle("Select ppbox program")
                        .setItems(dlg_clipname,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (whichButton < fixed_size) {
                                            et_playlink.setText(String.valueOf(ppbox_playlink[whichButton]));
                                            btn_ft.setText(String.valueOf(ppbox_ft[whichButton]));
                                            LogUtil.info(TAG, String.format("Java: choose pre-set ppbox prog %d %s %d",
                                                    whichButton, dlg_clipname[whichButton], ppbox_playlink[whichButton]));
                                        } else if (whichButton < fixed_size + history_size) {
                                            et_playlink.setText(list_vid.get(whichButton - fixed_size));
                                            btn_ft.setText("1");
                                            LogUtil.info(TAG, String.format("Java: choose playhistory %d %s %s",
                                                    whichButton, dlg_clipname[whichButton], list_vid.get(whichButton - fixed_size)));
                                        } else {
                                            String url = list_url.get(whichButton - fixed_size - history_size);
                                            LogUtil.info(TAG, String.format("Java: choose tvplay.txt #%d title: %s, url: %s",
                                                    whichButton, list_title.get(whichButton), url));

                                            start_player(list_title.get(whichButton), url);
                                        }

                                        dialog.cancel();

                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                        .create();
                choose_ppbox_res_dlg.show();
            }
        });

        this.btnPlay.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                int ppbox_playid, ppbox_ft, ppbox_bw_type;
                String tmp;
                tmp = et_playlink.getText().toString();
                ppbox_playid = Integer.parseInt(tmp);
                tmp = btn_ft.getText().toString();
                ppbox_ft = Integer.parseInt(tmp);
                tmp = btn_bw_type.getText().toString();
                ppbox_bw_type = Integer.parseInt(tmp);

                short port = MediaSDK.getPort(PORT_HTTP);
                LogUtil.info(TAG, "Http port is: " + port);

                if (ppbox_playid >= 300000 && ppbox_playid < 400000 &&
                        mPlaybackTime.getPPTVTimeStr() == null) {
                    mIsLivePlay = true;
                    LogUtil.info(TAG, "Java: set mIsLivePlay to true");
                } else {
                    mIsLivePlay = false;
                    LogUtil.info(TAG, "Java: set mIsLivePlay to false");
                }

                if (ppbox_bw_type == 4) {// dlna
                    new EPGTask().execute(EPG_ITEM_CDN, ppbox_playid, ppbox_ft, 0); // 3rd params for MSG_PLAY_CDN_URL
                    return;
                }

                String ppbox_url = PlayLinkUtil.getPlayUrl(ppbox_playid, port, ppbox_ft, ppbox_bw_type,
                        mPlaybackTime.getPPTVTimeStr());

                start_player("N/A", ppbox_url);
            }
        });

        this.btnSelectTime.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlaybackTime.setPlaybackTime();
            }
        });

        this.btnClipLocation.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String listUrl;
                if (mListLocalFile) {
                    // switch to http list(maybe failed)
                    listUrl = HTTP_SERVER_URL;
                } else {
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
                    popupAudioTrackDialog();
                }
            }
        });
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    // UI
    private GestureDetector mGestureDetector =
            new GestureDetector(getApplication(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    //LogUtil.debug(TAG, "Java: onDown!!!");
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    // 1xxx - 4xxx
                    final int FLING_MIN_DISTANCE = 200;
                    final float FLING_MIN_VELOCITY = 1000.0f;
                    // 1xxx - 4xxx

                    float distance = e2.getX() - e1.getX();
                    if (Math.abs(distance) > FLING_MIN_DISTANCE
                            && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                        if (mPlayer != null && !isTVbox) {
                            int pos = mPlayer.getCurrentPosition();
                            int incr = distance > 0f ? 1 : -1;
                            pos += incr * 15000; // 15sec
                            if (pos > mPlayer.getDuration())
                                pos = mPlayer.getDuration();
                            else if (pos < 0)
                                pos = 0;

                            mPlayer.seekTo(pos);

                            if (incr > 0)
                                imageForward.setVisibility(View.VISIBLE);
                            else
                                imageBackward.setVisibility(View.VISIBLE);
                            mIsFlinging = true;
                            return true;
                        }
                    }

                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    LogUtil.info(TAG, "Java: onSingleTapConfirmed!!!");
                    if (mPlayer != null && mPrepared) {
                        if (mMediaController.isShowing())
                            mMediaController.hide();
                        else
                            mMediaController.show();
                    }

                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent event) {
                    LogUtil.info(TAG, "Java: onDoubleTap!!!");
                    if (mPlayer != null) {
                        if (isLandscape) {

                            mPreview.switchDisplayMode(1);
                            mLayout.requestLayout(); // force refresh layout

                            Toast.makeText(ClipListActivity.this, "switch to " + mPreview.getDisplayMode(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // toggle to full screen play mode
                            if (getSupportActionBar() != null)
                                getSupportActionBar().hide();
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        }
                    }

                    return true;
                }
            });

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        LogUtil.info(TAG, "Java: onConfigurationChanged");

        int orientation = getRequestedOrientation();
        LogUtil.info(TAG, "Java: orientation " + orientation);
        isLandscape = (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mMediaController.updateLandscape(isLandscape);

        if (isLandscape) {
            mLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            tv_title.setVisibility(View.GONE);
        } else {
            preview_height = screen_height * 2 / 5;
            mLayout.getLayoutParams().height = preview_height;
            tv_title.setVisibility(View.VISIBLE);
        }

        mLayout.requestLayout(); //or invalidate();
        //mPreview.requestLayout();
    }

    private void readSettings() {
        int value = Util.readSettingsInt(this, "isPreview");
        LogUtil.info(TAG, "readSettings isPreview: " + value);
        if (value == 1)
            mIsPreview = true;
        else
            mIsPreview = false;

        value = Util.readSettingsInt(this, "isLoop");
        LogUtil.info(TAG, "readSettings isLoop: " + value);
        if (value == 1)
            mIsLoop = true;
        else
            mIsLoop = false;

        value = Util.readSettingsInt(this, "isNoVideo");
        if (value == 1)
            mIsNoVideo = true;
        else
            mIsNoVideo = false;

        value = Util.readSettingsInt(this, "isRemberPos");
        if (value == 1)
            mIsRememberPos = true;
        else
            mIsRememberPos = false;

        value = Util.readSettingsInt(this, "isListAudioFile");
        if (value == 1)
            mIsListAudioFile = true;
        else
            mIsListAudioFile = false;

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
        mAudioDst = Util.readSettings(this, "last_audio_ip_port");
    }

    private void writeSettings() {
        Util.writeSettingsInt(this, "isPreview", mIsPreview ? 1 : 0);
        Util.writeSettingsInt(this, "isLoop", mIsLoop ? 1 : 0);
        Util.writeSettingsInt(this, "isNoVideo", mIsNoVideo ? 1 : 0);
        Util.writeSettingsInt(this, "isRemberPos", mIsRememberPos ? 1 : 0);
        Util.writeSettingsInt(this, "isListAudioFile", mIsListAudioFile ? 1 : 0);

        Util.writeSettingsInt(this, "PlayerImpl", mPlayerImpl);
        Util.writeSettings(this, "last_audio_ip_port", mAudioDst);
    }

    private void TakeSnapShot() {
        if (mPlayer == null) {
            Toast.makeText(this, "play is not playing",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        LogUtil.info(TAG, "Java: begin to get snapshot");
        long begin_time = System.currentTimeMillis();
        long elapsed;

        Bitmap bmp = mPlayer.getSnapShot(320, 240, 0, -1);
        if (null == bmp) {
            Toast.makeText(ClipListActivity.this, "failed to get snapshot",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        elapsed = System.currentTimeMillis() - begin_time;
        LogUtil.info(TAG, String.format("Java: use %d msec to get snapshot, begin to save png", elapsed));

        begin_time = System.currentTimeMillis();
        String save_folder = Environment.getExternalStorageDirectory().getPath() + "/test2/snapshot/";
        File folder = new File(save_folder);
        if (!folder.exists() && !folder.mkdirs()) {
            Toast.makeText(this, "create snapshot folder failed: " + save_folder, Toast.LENGTH_SHORT).show();
            return;
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
            LogUtil.info(TAG, String.format("Java: use %d msec to save picture", elapsed));
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
    private boolean start_player(String title, String path) {
        mDecMode = DecodeMode.UNKNOWN;
        if (0 == mPlayerImpl) {
            mDecMode = DecodeMode.AUTO;
        } else if (1 == mPlayerImpl) {
            mDecMode = DecodeMode.HW_SYSTEM;
        } else if (2 == mPlayerImpl) {
            boolean canPlay = false;

            if (path.startsWith("/") || path.startsWith("file://")) {
                MediaInfo info = MeetSDK.getMediaDetailInfo(path);
                if (info != null) {
                    if (info.getVideoCodecName() != null &&
                            (info.getVideoCodecName().equals("h264") || info.getVideoCodecName().equals("hevc"))) {
                        if (info.getAudioChannels() == 0)
                            canPlay = true;
                        else {
                            TrackInfo trackinfo = info.getAudioChannelsInfo().get(0);
                            if (trackinfo.getCodecName() != null &&
                                    (trackinfo.getCodecName().equals("aac") || trackinfo.getCodecName().equals("ac3")))
                                canPlay = true;
                        }
                    }
                }
            } else {
                // http://, rtmp://, etc...
                canPlay = true;
            }

            String fileName = "N/A";
            int index;

            if (path.startsWith("/") || path.startsWith("file://")) {
                index = path.lastIndexOf("/");
                if (index != -1)
                    fileName = path.substring(index + 1, path.length());
                else
                    fileName = path;
            } else {
                fileName = path;
            }

            if (!canPlay) {
                Toast.makeText(ClipListActivity.this, "XOPlayer cannot play: " + fileName,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            mDecMode = DecodeMode.HW_XOPLAYER;
        } else if (3 == mPlayerImpl) {
            mDecMode = DecodeMode.SW;
        } else if (4 == mPlayerImpl) {
            boolean canPlay = false;
            if (path.startsWith("/") || path.startsWith("file://")) {
                if (path.endsWith(".ts") || path.endsWith(".mpegts"))
                    canPlay = true;
            }

            if (!canPlay) {
                Toast.makeText(ClipListActivity.this, "OMXPlayer cannot play: " + path,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            mDecMode = DecodeMode.HW_OMX;
        } else {
            Toast.makeText(ClipListActivity.this, "invalid player implement: " + Integer.toString(mPlayerImpl),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        stop_player();

        mSubtitleStoped = false;
        mHomed = false;
        mBufferingPertent = 0;
        mPrepared = false;
        mDMRcontrolling = false;
        rx_speed = 0;
        tx_speed = 0;

        imageDMR.setVisibility(View.GONE);
        imageForward.setVisibility(View.GONE);
        imageBackward.setVisibility(View.GONE);

        mPlayUrl = path;
        tv_title.setText(path);
        LogUtil.info(TAG, "Java: clipname: " + mPlayUrl);

        btnSelectAudioTrack.setVisibility(View.INVISIBLE);

        if (!mIsPreview) {
            Uri uri = Uri.parse(path);
            LogUtil.info(TAG, "Java: goto PlayerActivity, uri:" + uri.toString());
            start_fullscreen_play(title, uri, mPlayerImpl);
        } else {
            if (DecodeMode.AUTO == mDecMode) {
                mDecMode = MeetSDK.getPlayerType(mPlayUrl);
                LogUtil.info(TAG, "Java: mDecMode " + mDecMode.toString());
            }

            // force refresh a new surface
            mPreview.setVisibility(View.INVISIBLE);

            if (DecodeMode.HW_SYSTEM == mDecMode) {
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            } else if (DecodeMode.SW == mDecMode) {
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
            }

            mPreview.setVisibility(View.VISIBLE);

            Util.checkNetworkType(this);

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
            mPlayer.setOnSeekCompleteListener(this);

            mPlayer.setLooping(mIsLoop);

            if (mAudioDst != null && !mAudioDst.isEmpty()) {
                LogUtil.info(TAG, "Java: set player option: " + mAudioDst);
                mPlayer.setOption("-dump_url " + mAudioDst);
            }

            if (path.startsWith("http://")) {
                mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

                mWifiLock.acquire();
            }

            try {
                mPlayer.setDataSource(path);
                mPlayer.prepareAsync();

                if (!mPlayUrl.startsWith("/") && !mPlayUrl.startsWith("file://")) {
                    // ONLY network media need buffering
                    mBufferingProgressBar.setVisibility(View.VISIBLE);
                    mIsBuffering = true;

                    mSpeed = new NetworkSpeed();
                    mHandler.sendEmptyMessage(MSG_UPDATE_NETWORK_SPEED);
                }

                return true;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                LogUtil.error(TAG, "failed to play: " + e.toString());
                Toast.makeText(this, "Java: failed to play: " + path, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void stop_player() {
        if (mPlayer != null) {
            mHandler.removeMessages(MSG_UPDATE_NETWORK_SPEED);

            mMediaController.hide();

            stop_subtitle();

            mMediaDB.savePlayedPosition(mPlayUrl, mPlayer.getCurrentPosition());
            LogUtil.info(TAG, "Java: save " + mPlayUrl + " , played pos " + mPlayer.getCurrentPosition());

            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;

            mIsLivePlay = false;

            if (mWifiLock != null) {
                try {
                    if (mWifiLock.isHeld())
                        mWifiLock.release();
                } catch (Exception e) {
                    //probably already released
                    LogUtil.error(TAG, e.getMessage());
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

            subtitle_filename = null;
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
            } else {
                mPlayer.seekTo(pos);
            }

            // update mBufferingPertent
            if (!mPlayUrl.startsWith("file://") && !mPlayUrl.startsWith("/")) {
                if (pos <= 0)
                    pos = 100; // avoid to be devided by zero
                mBufferingPertent = pos * 100 / mPlayer.getDuration() + 1;
                if (mBufferingPertent > 100)
                    mBufferingPertent = 100;
                LogUtil.info(TAG, "onBufferingUpdate: seekTo " + mBufferingPertent);
            }
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

        LogUtil.debug(TAG, String.format("Java: getCurrentPosition %d %d msec", mPlayer.getCurrentPosition(), pos));

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
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200)
                Toast.makeText(this, "GET post succeeded", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "GET post failed", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_NETWORK_SPEED:
                    int[] speed = mSpeed.currentSpeed();
                    if (speed != null) {
                        rx_speed = speed[0];
                        tx_speed = speed[1];
                        this.sendEmptyMessageDelayed(MSG_UPDATE_NETWORK_SPEED, 1000);
                    }
                case MSG_UPDATE_PLAY_INFO:
                case MSG_UPDATE_RENDER_INFO:
                    if (isLandscape) {
                        mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d "
                                        + "dec/render %d(%d)/%d(%d) fps/msec bitrate %d kbps\nrx %d kB/s, tx %d kB/s",
                                render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec,
                                decode_fps, decode_avg_msec, render_fps, render_avg_msec,
                                video_bitrate,
                                rx_speed, tx_speed));
                    } else {
                        mTextViewInfo.setText(String.format("%02d|%03d v-a: %+04d\n"
                                        + "dec/render %d(%d)/%d(%d) fps/msec\nbitrate %d kbps\nrx %d kB/s, tx %d kB/s",
                                render_frame_num % 25, decode_drop_frame % 1000, av_latency_msec,
                                decode_fps, decode_avg_msec, render_fps, render_avg_msec,
                                video_bitrate,
                                rx_speed, tx_speed));
                    }
                    break;
                case MSG_CLIP_PLAY_DONE:
                    Toast.makeText(ClipListActivity.this, "clip completed", Toast.LENGTH_SHORT).show();
                    mTextViewInfo.setText("play info");
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
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
                case MSG_EPG_DETAIL_DONE:
                    if (mEPGLinkList.size() > 1) {
                        Intent intent = new Intent(ClipListActivity.this, MeetViewActivity.class);
                        intent.putExtra("playlink", String.valueOf(mSavedPlayLink));
                        startActivity(intent);
                        break;
                    }
                case MSG_EPG_SEARCH_DONE:
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
                    } else {
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
                    LogUtil.info(TAG, String.format("Java: dlna push url(%s) to uuid(%s) name(%s)", mDLNAPushUrl, mDlnaDeviceUUID, mDlnaDeviceName));
                    Toast.makeText(ClipListActivity.this,
                            String.format("push url to dmr %s", mDlnaDeviceName), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PLAY_CDN_URL:
                    LogUtil.info(TAG, "cdn url set %s" + mDLNAPushUrl);
                    stop_player();
                    start_player("N/A", mDLNAPushUrl);
                    break;
                case MSG_PLAY_CDN_FT:
                    btn_ft.setText(String.valueOf(msg.arg1));
                    LogUtil.info(TAG, "Java: set ft to: " + msg.arg1);
                    break;
                default:
                    LogUtil.warn(TAG, "Java: unknown msg.what " + msg.what);
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

        for (int i = 0; i < size; i++) {
            Module c = mEPGModuleList.get(i);
            String title = c.getTitle();
            title_list.add(title);
            if (isFrontpage)
                value_list.add(String.valueOf(c.getIndex())); // index in programs
            else
                value_list.add(c.getLink()); // index in programs
        }

        final String[] str_title_list = (String[]) title_list.toArray(new String[size]);

        Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("Select epg module")
                .setItems(str_title_list,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton >= 0) {
                                    if (isFrontpage) {
                                        int item = Integer.valueOf(value_list.get(whichButton));
                                        Toast.makeText(ClipListActivity.this, "loading epg clip...", Toast.LENGTH_SHORT).show();
                                        new EPGTask().execute(EPG_ITEM_CATALOG, item);
                                    } else {
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
                                                LogUtil.info(TAG, "Java: switch to audio mode");
                                            }
                                        } else {
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
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
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

        for (int i = 0; i < size; i++) {
            Content c = mEPGContentList.get(i);
            String title = c.getTitle();
            title_list.add(title);
            param_list.add(c.getParam()); // index in programs
        }

        final String[] str_title_list = (String[]) title_list.toArray(new String[size]);

        Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("Select epg content")
                .setItems(str_title_list,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton >= 0) {
                                    mEPGparam = param_list.get(whichButton);
                                    if (mEPGparam.startsWith("type="))
                                        mEPGtype = "";
                                    LogUtil.info(TAG, String.format("Java: epg content param: %s, type: %s", mEPGparam, mEPGtype));
                                    mEPGlistStartPage = 1;
                                    new EPGTask().execute(EPG_ITEM_LIST, mEPGlistStartPage, mEPGlistCount);
                                }

                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
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

        for (int i = 0; i < size; i++) {
            Catalog c = mEPGCatalogList.get(i);
            if (c.getVid() == null)
                continue;

            title_list.add(c.getTitle());
            vid_list.add(c.getVid()); // index in programs
        }

        final String[] str_title_list = (String[]) title_list.toArray(new String[title_list.size()]);

        Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("Select epg item")
                .setItems(str_title_list,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton >= 0) {
                                    int vid = Integer.valueOf(vid_list.get(whichButton));
                                    LogUtil.info(TAG, "Java: epg vid: " + vid);
                                    new EPGTask().execute(EPG_ITEM_DETAIL, vid);
                                }

                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .create();
        choose_clip_dlg.show();
    }

    private void popupEPGCollectionDlg() {
        int size;
        if (mIsVirtualChannel) {
            size = mVirtualLinkList.size();
        } else {
            size = mEPGLinkList.size();
        }

        if (size > 0) {
            ArrayList<String> title_list = new ArrayList<String>();
            final ArrayList<String> link_list = new ArrayList<String>();

            if (mIsVirtualChannel) {
                for (int i = 0; i < size; i++) {
                    Episode e = mVirtualLinkList.get(i);
                    String title = e.getTitle();
                    String extid = e.getExtId();
                    title_list.add(title);
                    link_list.add(extid);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    PlayLink2 l = mEPGLinkList.get(i);
                    String title = l.getTitle();
                    String link = l.getId();
                    title_list.add(title);
                    link_list.add(link);
                }
            }

            final String[] str_title_list = (String[]) title_list.toArray(new String[title_list.size()]);

            Dialog choose_clip_dlg = new AlertDialog.Builder(ClipListActivity.this)
                    .setTitle(String.format("Select clip to play(page #%d)", mEPGlistStartPage))
                    .setItems(str_title_list,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (mIsVirtualChannel) {
                                        mExtid = link_list.get(whichButton);
                                        int index = (mEPGlistStartPage - 1) * mEPGlistCount + whichButton;
                                        new EPGTask().execute(EPG_ITEM_VIRTUAL_SOHU, index);
                                    } else {
                                        int vid = Integer.valueOf(link_list.get(whichButton));
                                        new EPGTask().execute(EPG_ITEM_DETAIL, vid);
                                    }
                                    dialog.cancel();
                                }
                            })
                    .setPositiveButton("More...",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (mListSearch)
                                        new EPGTask().execute(EPG_ITEM_SEARCH, ++mEPGlistStartPage, mEPGlistCount);
                                    else {
                                        if (mIsVirtualChannel) {
                                            new EPGTask().execute(EPG_ITEM_VIRTUAL_LIST, ++mEPGlistStartPage, mEPGlistCount);
                                        } else {
                                            new EPGTask().execute(EPG_ITEM_LIST, ++mEPGlistStartPage, mEPGlistCount);
                                        }
                                    }
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                    .create();
            choose_clip_dlg.show();
        }
    }

    private void popupSelectSubtitle() {
        final String sub_folder = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/test2/subtitle";

        File file = new File(sub_folder);
        String[] list = {"srt", "ass"};
        File[] subtitle_files = file.listFiles(new FileFilterTest(list));
        if (subtitle_files == null) {
            Toast.makeText(this, "no subtitle file found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> filename_list = new ArrayList<String>();
        for (int i = 0; i < subtitle_files.length; i++) {
            filename_list.add(subtitle_files[i].getName());
        }
        final String[] str_file_list = (String[]) filename_list.toArray(new String[filename_list.size()]);

        Dialog choose_subtitle_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("select subtitle")
                .setItems(str_file_list, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        subtitle_filename = sub_folder + "/" + str_file_list[whichButton];
                        LogUtil.info(TAG, "Load subtitle file: " + subtitle_filename);
                        Toast.makeText(ClipListActivity.this,
                                "Load subtitle file: " + subtitle_filename, Toast.LENGTH_SHORT).show();
                        if (mPlayer != null) {
                            start_subtitle(subtitle_filename);
                        }

                        dialog.dismiss();
                    }
                })
                .create();
        choose_subtitle_dlg.show();
    }

    private void testConvert() {
        List<CDNItem> liveitem_list = mEPG.live_cdn(300156);
        if (liveitem_list != null && liveitem_list.size() != 0) {
            CDNItem liveitem = liveitem_list.get(0);
            String block_url_fmt = "http://%s/live/%s/" +
                    "%d.block?ft=1&platform=android3" +
                    "&type=phone.android.vip&sdk=1" +
                    "&channel=162&vvid=41&k=%s";

            String st = liveitem.getST();
            long start_time = new Date(st).getTime() / 1000;
            start_time -= 45;
            start_time -= (start_time % 5);

            String httpUrl = String.format(block_url_fmt,
                    liveitem.getHost(), liveitem.getRid(), start_time, liveitem.getKey());
            LogUtil.info(TAG, "Java: live flv block: " + httpUrl);

            String save_filepath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    String.format("/%d.ts", start_time);
            LogUtil.info(TAG, "Java: transcode mpegts block: " + save_filepath);
            byte[] in_flv = new byte[1048576];

            int in_size = httpUtil.httpDownloadBuffer(httpUrl, 1400, in_flv);
            byte[] out_ts = new byte[1048576];

            StringBuffer sbHex = new StringBuffer();
            for (int i = 0; i < 16; i++) {
                sbHex.append(String.format("0x%02x ", in_flv[i]));
            }

            byte[] header = new byte[4];
            header[0] = in_flv[0];
            header[1] = in_flv[1];
            header[2] = in_flv[2];
            header[3] = '\0';
            String strHeader = "";
            try {
                strHeader = new String(header, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            LogUtil.info(TAG, "Java: flv file context " + sbHex.toString() +
                    " , string: " + strHeader);

            int out_size = MeetSDK.Convert(in_flv, in_size, out_ts, 0, 0);
            LogUtil.info(TAG, "Java: out_size " + out_size);

            // save output ts file
            saveFile(out_ts, out_size, save_filepath);
        }
    }

    private void saveFile(byte[] buffer, int size, String filePath) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        int offset = 0;
        int left = size;
        int chunk_size = 1024;

        try {
            File file = new File(filePath);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            while (left > 0) {
                int towrite = chunk_size;
                if (left < towrite)
                    towrite = left;
                bos.write(buffer, offset, towrite);

                offset += towrite;
                left -= towrite;
                //LogUtil.info(TAG, "Java: ts write " + towrite);
            }

            LogUtil.info(TAG, "Java: total write file size " + offset);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void popupAudioTrackDialog() {
        MediaInfo info = null;
        try {
            info = mPlayer.getMediaInfo();
            if (info == null || info.getAudioChannels() == 0) {
                Toast.makeText(this, "Cannot get audio track", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            LogUtil.error(TAG, "getMediaInfo exception: " + e.getMessage());
            return;
        }

        List<TrackInfo> trackList = info.getAudioChannelsInfo();
        if (trackList == null || trackList.size() == 0) {
            Toast.makeText(this, "Cannot get audio track info", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> lang_list = new ArrayList<String>();

        int c = trackList.size();
        for (int i = 0; i < c; i++) {
            TrackInfo t = trackList.get(i);
            String title = t.getTitle();
            String lang = t.getLanguage();

            String name;
            if (title != null && !title.isEmpty())
                name = title;
            else if (lang != null && !lang.isEmpty())
                name = lang;
            else if (i == 0)
                name = "默认";
            else
                name = "N/A";

            String desc = getTrackTitle(true, i, name);

            lang_list.add(desc);
        }

        final String[] str_lang_list = (String[]) lang_list.toArray(new String[lang_list.size()]);

        Dialog choose_audio_track_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("select audio track")
                .setSingleChoiceItems(str_lang_list, mAudioSelectedTrack - mAudioFirstTrack, /*default selection item number*/
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mAudioSelectedTrack == mAudioFirstTrack + whichButton)
                                    return;

                                mAudioSelectedTrack = mAudioFirstTrack + whichButton;

                                LogUtil.info(TAG, "Java: selectTrack #" + mAudioSelectedTrack);
                                mPlayer.selectTrack(mAudioSelectedTrack);
                                Toast.makeText(ClipListActivity.this,
                                        "switch to audio #" + mAudioSelectedTrack + " 语言 " + str_lang_list[whichButton],
                                        Toast.LENGTH_SHORT).show();

                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel", null)
                .create();
        choose_audio_track_dlg.show();
    }

    private String getTrackTitle(boolean isAudio, int position, String value) {
        if ("eng".equals(value)) {
            value = "英语";
        } else if ("chi".equals(value) || "chn".equals(value)) {
            value = "汉语";
        } else if ("fra".equals(value)) {
            value = "法语";
        } else if ("ita".equals(value)) {
            value = "意大利语";
        } else if ("jpn".equals(value)) {
            value = "日语";
        } else if ("spa".equals(value)) {
            value = "西班牙语";
        } else if ("kor".equals(value)) {
            value = "韩语";
        } else if ("rus".equals(value)) {
            value = "俄语";
        }

        return String.format("%s%d (%s)", isAudio ? "音轨" : "字幕", position + 1, value);
    }

    private void push_cdn_clip() {
        //mDLNA.EnableRendererControler(true);
        mDLNA.SetURI(mDlnaDeviceUUID, mDLNAPushUrl);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mDLNA.Play(mDlnaDeviceUUID);

        mHandler.sendEmptyMessage(MSG_PUSH_CDN_CLIP);
    }

    boolean decide_virtual() {
        boolean ret;

        mEPGLinkList = mEPG.getLink();
        if (mEPGLinkList.size() == 0) {
            LogUtil.info(TAG, "virtual channel");
            mIsVirtualChannel = true;
            mEPGlistStartPage = 1;

            List<VirtualChannelInfo> infoList = mEPG.getVchannelInfo();
            for (int i = 0; i < infoList.size(); i++) {
                VirtualChannelInfo info = infoList.get(i);

                if (info.getSiteId() == 3 /* site */) {
                    ret = mEPG.virtual_channel(info.getTitle(),
                            info.getInfoId(), mEPGlistCount, info.getSiteId(), 1);
                    if (!ret) {
                        LogUtil.error(TAG, "failed to get virtual_channel");
                        mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                        return false;
                    }

                    break;
                }
            }

            mVirtualLinkList = mEPG.getVirtualLink();
        } else {
            mIsVirtualChannel = false;
        }

        return true;
    }

    public class ParseVideoTask extends AsyncTask<String, Integer, Boolean> {

        private String video_url;
        private ZGUrl zgUrl;
        private String play_url;
        private boolean is_youku = true;

        @Override
        protected void onPostExecute(Boolean ret) {
            if (ret) {
                //start_player("N/A", play_url);

                Intent intent = new Intent(ClipListActivity.this,
                        is_youku ? PlayYoukuActivity.class : PlayIqiyiActivity.class);
                intent.putExtra("url_list", zgUrl.urls);
                intent.putExtra("duration_list", zgUrl.durations);
                intent.putExtra("title", zgUrl.title.isEmpty() ? "N/A" : zgUrl.title);
                intent.putExtra("ft", 2);
                intent.putExtra("player_impl", mPlayerImpl);
                intent.putExtra("vid", zgUrl.vid);
                intent.putExtra("video_url", video_url);
                startActivity(intent);
            } else {
                Toast.makeText(ClipListActivity.this, "解析视频地址失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            video_url = params[0];

            if (video_url.contains("youku")) {
                is_youku = true;

                String vid = YKUtil.getVid(video_url);
                zgUrl = YKUtil.getPlayZGUrl(ClipListActivity.this, vid);
                if (zgUrl == null) {
                    LogUtil.error(TAG, "failed to get youku ZGUrl, vid " + vid);
                    return false;
                }

                return true;
            }
            else if (video_url.contains("iqiyi.com")) {
                is_youku = false;

                zgUrl = IqiyiUtil.getPlayZGUrl(video_url, 2);
                if (zgUrl == null) {
                    LogUtil.error(TAG, "failed to get iqiyi ZGUrl, video_url " + video_url);
                    return false;
                }

                return true;
            }

            return false;
        }
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
            } else if (EPG_ITEM_CATALOG == type) {
                if (!mEPG.catalog(id)) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                    return false;
                }

                mEPGCatalogList = mEPG.getCatalog();
                if (mEPGCatalogList.size() == 0)
                    return false;

                mHandler.sendEmptyMessage(MSG_EPG_CATALOG_DONE);
            } else if (EPG_ITEM_DETAIL == type) {
                LogUtil.info(TAG, "Java: epg detail() " + id);

                if (!mEPG.detail(String.valueOf(id))) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                    return false;
                }

                if (decide_virtual()) {
                    mSavedPlayLink = id;
                    LogUtil.info(TAG, "Java: EPG_ITEM_DETAIL mSavedPlayLink " + mSavedPlayLink);
                    mHandler.sendEmptyMessage(MSG_EPG_DETAIL_DONE);
                }
            } else if (EPG_ITEM_SEARCH == type) {
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
            } else if (EPG_ITEM_CONTENT_LIST == type) {
                if (!mEPG.contents_list()) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
                    return false;
                }

                mEPGModuleList = mEPG.getModule();
                if (mEPGModuleList.size() == 0)
                    return false;

                mHandler.sendEmptyMessage(MSG_EPG_CONTENT_LIST_DONE);
            } else if (EPG_ITEM_CONTENT_SURFIX == type) {
                if (mLink == null || mLink.isEmpty() || !mEPG.contents(mLink)) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                    return false;
                }

                mEPGContentList = mEPG.getContent();
                if (mEPGContentList.size() == 0)
                    return false;

                mHandler.sendEmptyMessage(MSG_EPG_CONTENT_SURFIX_DONE);
            } else if (EPG_ITEM_LIST == type) {
                if (mEPGparam == null || mEPGparam.isEmpty() || params.length != 3) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                    return false;
                }

                if (params.length < 2) {
                    mHandler.sendEmptyMessage(MSG_WRONG_PARAM);
                    return false;
                }

                int start_page = params[1];
                int count = params[2];
                if (!mEPG.list(mEPGparam, mEPGtype, start_page, "order=n", count, false)) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                    return false;
                }

                if (decide_virtual())
                    mHandler.sendEmptyMessage(MSG_EPG_LIST_DONE);
            } else if (EPG_ITEM_CDN == type) {
                LogUtil.info(TAG, "Java: EPGTask start to getCDNUrl");
                if (params.length < 4) {
                    LogUtil.error(TAG, "Java: EPG_ITEM_CDN params is invalid");
                    return false;
                }

                int ft = params[2];
                mDLNAPushUrl = mEPG.getCDNUrl(String.valueOf(id), String.valueOf(ft), false, mIsNoVideo);
                if (mDLNAPushUrl == null) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
                    return false;
                }

                int isPushUrl = params[3];
                if (isPushUrl > 0)
                    push_cdn_clip();
                else
                    mHandler.sendEmptyMessage(MSG_PLAY_CDN_URL);
            } else if (EPG_ITEM_FT == type) {
                LogUtil.info(TAG, "Java: EPGTask start to getCDNUrl");
                int[] ft_list = mEPG.getAvailableFT(String.valueOf(id));
                if (ft_list == null || ft_list.length == 0) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
                    return false;
                }


                int ft = -1;
                for (int i = ft_list.length - 1; i >= 0; i--) {
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
            } else if (EPG_ITEM_VIRTUAL_SOHU == type) {
                int index = params[1]; // start episode index

                int pos = mExtid.indexOf('|');
                String sid = mExtid.substring(0, pos);
                String vid = mExtid.substring(pos + 1, mExtid.length());
                SohuUtil sohu = new SohuUtil();

                PlaylinkSohu l = sohu.playlink_pptv(Integer.valueOf(vid), Integer.valueOf(sid));

                if (l == null) {
                    mHandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
                    return false;
                }

                int info_id = 0;
                List<VirtualChannelInfo> infoList = mEPG.getVchannelInfo();
                for (int i = 0; i < infoList.size(); i++) {
                    VirtualChannelInfo info = infoList.get(i);
                    if (info.getSiteId() == 3) {
                        info_id = info.getInfoId();
                    }
                    break;
                }

                Intent intent = new Intent(ClipListActivity.this,
                        PlaySohuActivity.class);
                intent.putExtra("url_list", l.getUrl(SohuFtEnum.SOHU_FT_HIGH));
                intent.putExtra("duration_list", l.getDuration(SohuFtEnum.SOHU_FT_HIGH));
                intent.putExtra("title", l.getTitle());
                intent.putExtra("info_id", info_id);
                intent.putExtra("index", index);
                startActivity(intent);
                return true;
            } else if (EPG_ITEM_VIRTUAL_LIST == type) {
                LogUtil.info(TAG, "Java: EPG_ITEM_VIRTUAL_LIST");
                List<VirtualChannelInfo> infoList = mEPG.getVchannelInfo();
                for (int i = 0; i < infoList.size(); i++) {
                    VirtualChannelInfo info = infoList.get(i);

                    if (info.getSiteId() == 3 /*sohu*/) {
                        int start_page = params[1];
                        int count = params[2];
                        ret = mEPG.virtual_channel(info.getTitle(), info.getInfoId(),
                                count, info.getSiteId(), start_page);
                        if (!ret) {
                            LogUtil.error(TAG, "failed to get virtual_channel");
                            mHandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
                            return false;
                        }

                        mVirtualLinkList = mEPG.getVirtualLink();
                        mHandler.sendEmptyMessage(MSG_EPG_DETAIL_DONE);
                        break;
                    }
                }
            } else {
                LogUtil.warn(TAG, "Java: EPGTask invalid type: " + type);
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
        private String mPath;

        @Override
        protected Boolean doInBackground(String... params) {
            mPath = params[0];
            LogUtil.info(TAG, "Java: doInBackground " + mPath);

            // update progress
            // publishProgress(progresses)

            return mListUtil.ListMediaInfo(mPath);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (mPath.startsWith("http://")) {
                    setTitle(HTTP_SERVER_URL);
                    btnClipLocation.setText("local");
                    mListLocalFile = false;
                } else {
                    btnClipLocation.setText("http");
                    mListLocalFile = true;
                }

                if (mAdapter == null) {
                    mAdapter = new LocalFileAdapter(ClipListActivity.this, mListUtil.getList(),
                            R.layout.pptv_list);
                    lv_filelist.setAdapter(mAdapter);
                } else {
                    mAdapter.updateData(mListUtil.getList());
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                if (!mListLocalFile) {
                    Toast.makeText(ClipListActivity.this, "failed to connect to http server",
                            Toast.LENGTH_SHORT).show();
                    btnClipLocation.setText("http");
                }
            }

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

    private void start_fullscreen_play(String title, Uri uri, int player_impl) {
        LogUtil.info(TAG, "java: start_fullscreen_play: " + uri.toString());

        Intent intent = new Intent(ClipListActivity.this, VideoPlayerActivity.class);
        /*if (mDoScan) {
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setClassName("com.pplive.tvduck", "com.pplive.tvduck.PlayerActivity");
	        intent.putExtra(Intent.ACTION_VIEW, uri);
		}*/

        intent.setData(uri);
        intent.putExtra("title", title);
        intent.putExtra("impl", player_impl);
        startActivity(intent);
    }

    private void push_to_dmr() {
        if (mPlayUrl == null || mPlayUrl.equals("")) {
            mPlayUrl = getClipboardText();
            if (mPlayUrl == null) {
                Toast.makeText(this, "no url is set", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int dev_num = IDlnaCallback.mDMRmap.size();

        if (dev_num == 0) {
            LogUtil.info(TAG, "Java: dlna no dlna device found");
            Toast.makeText(this, "no dlna device found", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> dev_list = new ArrayList<String>();
        ArrayList<String> uuid_list = new ArrayList<String>();
        for (Object obj : IDlnaCallback.mDMRmap.keySet()) {
            Object name = IDlnaCallback.mDMRmap.get(obj);
            LogUtil.debug(TAG, "Java: dlna [dlna dev] uuid: " + obj.toString() + " name: " + name.toString());
            uuid_list.add(obj.toString());
            dev_list.add(name.toString());
        }

        final String[] str_uuid_list = (String[]) uuid_list.toArray(new String[uuid_list.size()]);
        final String[] str_dev_list = (String[]) dev_list.toArray(new String[dev_list.size()]);

        Dialog choose_device_dlg = new AlertDialog.Builder(ClipListActivity.this)
                .setTitle("Select device to push")
                .setItems(str_dev_list,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                mDlnaDeviceUUID = str_uuid_list[whichButton];
                                mDlnaDeviceName = str_dev_list[whichButton];

                                if (mPlayUrl.startsWith("http://127.0.0.1")) {
                                    int link = Integer.valueOf(et_playlink.getText().toString());
                                    int ft = Integer.valueOf(btn_ft.getText().toString());
                                    new EPGTask().execute(EPG_ITEM_CDN, link, ft, 1);
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
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .create();
        choose_device_dlg.show();
    }

    private boolean start_subtitle(String filename) {
        LogUtil.info(TAG, "Java: subtitle start_subtitle " + filename);

        stop_subtitle();

        mSubtitleParser = new SimpleSubTitleParser();
        mSubtitleParser.setListener(this);

        mSubtitleParser.setDataSource(filename);
        mSubtitleParser.prepareAsync();

        return true;
    }

    private void stop_subtitle() {
        LogUtil.info(TAG, "Java: subtitle stop_subtitle");

        if (mIsSubtitleUsed) {
            mSubtitleStoped = true;
            mSubtitleThread.interrupt();

            try {
                LogUtil.info(TAG, "Java subtitle before join");
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

    private String getClipboardText() {
        ClipboardManager cmb = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cmb.hasPrimaryClip()) {
            ClipData cd = cmb.getPrimaryClip();
            int count = cd.getItemCount();
            if (count > 0) {
                String strText = cd.getItemAt(0).getText().toString();
                LogUtil.info(TAG, "Java: clipboard manager: " + strText);
                return strText;
            }
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.cliplist_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(2);
        SubMenu submenu = item.getSubMenu();
        MenuItem previewMenuItem = submenu.getItem(0);
        MenuItem loopMenuItem = submenu.getItem(1);
        MenuItem noVideoMenuItem = submenu.getItem(2);
        MenuItem rememberPosMenuItem = submenu.getItem(3);
        MenuItem listAudioFileMenuItem = submenu.getItem(4);

        previewMenuItem.setChecked(mIsPreview);
        loopMenuItem.setChecked(mIsLoop);
        rememberPosMenuItem.setChecked(mIsRememberPos);
        listAudioFileMenuItem.setChecked(mIsListAudioFile);

        if ("4".equals(btn_bw_type.getText())) // cdn play
            noVideoMenuItem.setEnabled(true);
        else
            noVideoMenuItem.setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder;

        int id = item.getItemId();
        Intent intent;

        switch (id) {
            case R.id.list_clip:
                if (mListLocalFile) {
                    if (mMediaDB.getMediaStore() == null) {
                        intent = new Intent(getApplicationContext(), MediaScannerService.class);
                        startService(intent);
                        return true;
                    }

                    new ListItemTask().execute(mCurrentFolder);
                }
                break;
            case R.id.scan_media:
                intent = new Intent(getApplicationContext(), MediaScannerService.class);
                startService(intent);
                break;
            case R.id.upload_crash_report:
                Util.makeUploadLog("manual upload\n\n");

                final ProgressDialog progDlg = new ProgressDialog(this);
                progDlg.setTitle("系统管理");
                progDlg.setMessage("日志上传中...");
                progDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progDlg.setCancelable(true);
                progDlg.show();

                UploadLogTask task = new UploadLogTask(this);
                task.setOnTaskListener(new UploadLogTask.TaskListener() {
                    @Override
                    public void onFinished(String msg, int code) {
                        progDlg.dismiss();

                        Toast.makeText(ClipListActivity.this,
                                msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String msg, int code) {
                        progDlg.dismiss();

                        Toast.makeText(ClipListActivity.this,
                                msg, Toast.LENGTH_SHORT).show();
                    }
                });
                task.execute(Util.upload_log_path, "common");
                break;
            case R.id.update_apk:
                setupUpdater();
                break;
            case R.id.quit:
                this.finish();
                break;
            case R.id.preview:
                mIsPreview = !mIsPreview;
                item.setChecked(mIsPreview);
                break;
            case R.id.loop:
                mIsLoop = !mIsLoop;
                item.setChecked(mIsLoop);
                LogUtil.info(TAG, "set loop to: " + mIsLoop);
                break;
            case R.id.no_video:
                mIsNoVideo = !mIsNoVideo;
                item.setChecked(mIsNoVideo);
                imageNoVideo.setVisibility(mIsNoVideo ? View.VISIBLE : View.GONE);
                break;
            case R.id.remember_last_pos:
                mIsRememberPos = !mIsRememberPos;
                item.setChecked(mIsRememberPos);
                imageNoVideo.setVisibility(mIsNoVideo ? View.VISIBLE : View.GONE);
                break;
            case R.id.list_audio_file:
                mIsListAudioFile = !mIsListAudioFile;
                item.setChecked(mIsListAudioFile);
                break;
            case R.id.debug_info:
                mDebugInfo = !mDebugInfo;
                item.setChecked(mDebugInfo);
                mTextViewInfo.setVisibility(mDebugInfo ? View.VISIBLE : View.GONE);
                break;
            case R.id.load_subtitle:
                popupSelectSubtitle();
                break;
            case R.id.audio_dst:
                final EditText inputDst = new EditText(this);
                String last_ip_port = Util.readSettings(this, "last_audio_ip_port");
                inputDst.setText(last_ip_port);
                inputDst.setHint("input ip/port");

                builder = new AlertDialog.Builder(this);
                builder.setTitle("input ip and port")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(inputDst)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        mAudioDst = inputDst.getText().toString();
                        LogUtil.info(TAG, "Java save last_audio_ip_port: " + mAudioDst);
                        Toast.makeText(ClipListActivity.this, "音频推送地址设置为: " + mAudioDst, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
                break;
            case R.id.clean_media_db:
                mMediaDB.clearMediaStore();
                break;
            case R.id.dlna_dmr:
                push_to_dmr();
                break;
            case R.id.list_http:
                final EditText inputUrl = new EditText(this);
                String last_http_url = Util.readSettings(this, "last_http_url");
                if (last_http_url == null || last_http_url.isEmpty())
                    last_http_url = "http://42.62.105.235/test/rec/vod";
                inputUrl.setText(last_http_url);
                inputUrl.setHint("输入服务器地址");

                builder = new AlertDialog.Builder(this);
                builder.setTitle("输入媒体库网址")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(inputUrl)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String url = inputUrl.getText().toString();
                        Util.writeSettings(ClipListActivity.this, "last_http_url", url);
                        Intent intent = new Intent(ClipListActivity.this, HttpViewerActivity.class);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                });
                builder.show();
                break;
            case R.id.pptv_frontpage:
                if (!Util.IsHaveInternet(this)) {
                    Toast.makeText(this, "network is not connected", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Toast.makeText(this, "loading epg catalog...", Toast.LENGTH_SHORT).show();
                new EPGTask().execute(EPG_ITEM_FRONTPAGE);
                break;
            case R.id.pptv_video:
                if (!Util.IsHaveInternet(this)) {
                    Toast.makeText(this, "network is not connected", Toast.LENGTH_SHORT).show();
                    return true;
                }

                intent = new Intent(ClipListActivity.this, PPTVVideoActivity.class);
                startActivity(intent);
                break;
            case R.id.sohu_video:
                if (!Util.IsHaveInternet(this)) {
                    Toast.makeText(this, "network is not connected", Toast.LENGTH_SHORT).show();
                    return true;
                }

                intent = new Intent(ClipListActivity.this, SohuVideoActivity.class);
                startActivity(intent);
                break;
            case R.id.youku_video:
                intent = new Intent(ClipListActivity.this, YoukuVideoActivity.class);
                startActivity(intent);
                break;
            case R.id.inke_live:
                intent = new Intent(ClipListActivity.this, InkeActivity.class);
                startActivity(intent);
                break;
            case R.id.parse_url:
                String video_url = getClipboardText();
                if (video_url == null) {
                    Toast.makeText(ClipListActivity.this, "剪贴板中没有视频地址", Toast.LENGTH_SHORT).show();
                } else {
                    boolean bParse = false;
                    if ((video_url.contains("youku") && video_url.contains("id_")) ||
                            video_url.contains("iqiyi.com"))
                        bParse = true;

                    if (bParse) {
                        new ParseVideoTask().execute(video_url);
                    }
                    else {
                        if (video_url.startsWith("http://wechat.bestv.com.cn")) {
                            // http://wechat.bestv.com.cn/activity/androidPlay.jsp
                            // ?playUrl=http%3A%2F%2Fwx.live.bestvcdn.com.cn%2Flive%2Fprogram%2Flive991%2Fweixinhddfws%2Findex.m3u8
                            // %3Fse%3Dweixin%26ct%3D1%26starttime%3D1437409560%26endtime%3D1437413340%26_cp
                            // %3D1%26_fk%3D4BE9666ECD5CA07A02A52EC9689B81621E354113967482965E8A7CC79F52A526
                            // &token=&t=%E4%B8%9C%E6%96%B9%E5%8D%AB%E8%A7%86%20%E6%9E%81%E9%99%90%E6%8C%91%E6%88%98%E7%AC%AC%E4%BA%94%E9%9B%86
                            // &seq=1&actcode=&tabIndex=1&topOffset=0&channelAbbr=dfws&type=0&channelCode=Umai:CHAN/1325@BESTV.SMG.SMG

                            int pos = video_url.indexOf("?playUrl=");
                            if (pos == -1) {
                                LogUtil.error(TAG, "bestv playUrl= not found");
                                Toast.makeText(this, "百事通播放地址解析失败", Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            String origin_url = video_url.substring(pos + "?playUrl=".length());
                            try {
                                String decoded_url = URLDecoder.decode(origin_url, "UTF-8");

                                int pos1 = decoded_url.indexOf("&t=");
                                int pos2 = decoded_url.indexOf("&seq=");
                                if (pos1 > -1 && pos2 > -1) {
                                    String play_url = decoded_url.substring(0, pos1) + decoded_url.substring(pos2);
                                    LogUtil.info(TAG, "Java: to play bestv url: " + play_url);
                                    start_player("百事通视频", play_url);
                                } else {
                                    LogUtil.error(TAG, "failed to parse bestv url");
                                    Toast.makeText(this, "百事通播放地址解析失败", Toast.LENGTH_SHORT).show();
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                LogUtil.error(TAG, e.toString());
                            }

                            return true;
                        }

                        if (video_url.startsWith("rtmp://") || video_url.contains("a8.com/live")) {
                            video_url += "?type=gotyelive";
                        }
                        start_player("N/A", video_url);
                    }
                }
                break;
            default:
                break;
        }

        return true;//super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        LogUtil.debug(TAG, "Java: onInfo: " + what + " " + extra);

        if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
            mBufferingProgressBar.setVisibility(View.VISIBLE);
            mIsBuffering = true;
            LogUtil.info(TAG, "Java: MEDIA_INFO_BUFFERING_START");
        } else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
            mBufferingProgressBar.setVisibility(View.GONE);
            mIsBuffering = false;
            LogUtil.info(TAG, "Java: MEDIA_INFO_BUFFERING_END");
        } else if (MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE == what) {
            String short_type;
            if (MediaPlayer.PLAYER_IMPL_TYPE_SYSTEM_PLAYER == extra) {
                short_type = "sys";
            } else if (MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER == extra) {
                short_type = "xo";
            } else if (MediaPlayer.PLAYER_IMPL_TYPE_FF_PLAYER == extra) {
                short_type = "ff";
            } else if (MediaPlayer.PLAYER_IMPL_TYPE_OMX_PLAYER == extra) {
                short_type = "omx";
            } else {
                short_type = "un";
            }
            mMediaController.setPlayerImplement(short_type);
        } else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_AVG_MSEC == what) {
            decode_avg_msec = extra;
        } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_AVG_MSEC == what) {
            render_avg_msec = extra;
        } else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS == what) {
            decode_fps = extra;
            mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
        } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS == what) {
            render_fps = extra;
            mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
        } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME == what) {
            render_frame_num = extra;
            mHandler.sendEmptyMessage(MSG_UPDATE_RENDER_INFO);
        } else if (MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC == what) {
            av_latency_msec = extra;
        } else if (MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME == what) {
            decode_drop_frame++;
            mHandler.sendEmptyMessage(MSG_UPDATE_RENDER_INFO);
        } else if (MediaPlayer.MEDIA_INFO_TEST_MEDIA_BITRATE == what) {
            video_bitrate = extra;
            mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_INFO);
        } else if (android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {

        } else if (MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING == what) {
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
        LogUtil.error(TAG, "Java: onError: " + framework_err + "," + impl_err);
        Toast.makeText(ClipListActivity.this, String.format("failed to play clip: %d %d", framework_err, impl_err), Toast.LENGTH_SHORT).show();

        if (mIsBuffering) {
            mBufferingProgressBar.setVisibility(View.GONE);
            mIsBuffering = false;
        }

        try {
            mPlayer.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mPlayer.release();
        mPlayer = null;

        Util.makeUploadLog("failed to play: " + mPlayUrl + "\n\n");

        UploadLogTask task = new UploadLogTask(this);
        task.execute(Util.upload_log_path, "failed to play");

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "onCompletion");
        mMediaDB.savePlayedPosition(mPlayUrl, 0);

        mMediaController.hide();
        mHandler.sendEmptyMessage(MSG_CLIP_PLAY_DONE);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: onPrepared");

        render_frame_num = 0;
        decode_drop_frame = 0;

        int pos = mMediaDB.getLastPlayedPosition(mPlayUrl);
        LogUtil.info(TAG, "Java: get " + mPlayUrl + " , played pos " + pos);
        if (mIsRememberPos && pos > 0) {
            mPlayer.seekTo(pos);
            LogUtil.info(TAG, "Java: pre-seek to " + pos + " msec");
        }

        LogUtil.info(TAG, String.format("Java: width %d, height %d", mPlayer.getVideoWidth(), mPlayer.getVideoHeight()));
        mPlayer.start();

        mMediaController.setMediaPlayer(this);
        mMediaController.setEnabled(true);

        if (mIsBuffering) {
            mBufferingProgressBar.setVisibility(View.GONE);
            mIsBuffering = false;
        }

        mPrepared = true;

        // audio track(activate track info)
        try {
            MediaInfo info = mp.getMediaInfo();
            if (info != null) {
                ArrayList<TrackInfo> audioTrackList = info.getAudioChannelsInfo();
                if (audioTrackList != null && audioTrackList.size() > 0) {
                    for (TrackInfo trackInfo : audioTrackList) {
                        LogUtil.info(TAG, String.format("Java: audio Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s",
                                trackInfo.getStreamIndex(),
                                trackInfo.getId(),
                                trackInfo.getCodecName(),
                                trackInfo.getLanguage(),
                                trackInfo.getTitle()));
                    }

                    mAudioFirstTrack = audioTrackList.get(0).getStreamIndex();
                    mAudioSelectedTrack = mAudioFirstTrack;
                    mAudioTrackCount = info.getAudioChannels();

                    if (audioTrackList.size() > 1)
                        btnSelectAudioTrack.setVisibility(View.VISIBLE);
                }

                ArrayList<TrackInfo> subtitleTrackList = info.getSubtitleChannelsInfo();
                for (TrackInfo trackInfo : subtitleTrackList) {
                    LogUtil.info(TAG, String.format("Java: subtitle Trackinfo: streamindex #%d id %d, codec %s, lang %s, title %s",
                            trackInfo.getStreamIndex(),
                            trackInfo.getId(),
                            trackInfo.getCodecName(),
                            trackInfo.getLanguage(),
                            trackInfo.getTitle()));
                }
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            LogUtil.error(TAG, "getMediaInfo exception: " + e.getMessage());
        }

        // subtitle
        if (mPlayUrl.startsWith("/") || mPlayUrl.startsWith("file://")) {
            // local file
            if (subtitle_filename == null) {
                String subtitle_full_path;
                int index = mPlayUrl.lastIndexOf('.') + 1;
                String tmp = mPlayUrl.substring(0, index);

                String[] exts = {"srt", "ass"};
                for (String ext : exts) {
                    subtitle_full_path = tmp + ext;

                    File subfile = new File(subtitle_full_path);
                    //LogUtil.debug(TAG, "Java: subtitle: subtitle file: " + subtitle_full_path);
                    if (subfile.exists()) {
                        subtitle_filename = subtitle_full_path;
                        break;
                    }
                }
            }

            if (subtitle_filename != null) {
                LogUtil.info(TAG, "Java: subtitle: subtitle file found: " + subtitle_filename);
                start_subtitle(subtitle_filename);
            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // TODO Auto-generated method stub
        LogUtil.debug(TAG, "onBufferingUpdate: " + percent);
        mBufferingPertent = percent;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, String.format("Java: onVideoSizeChanged(%d %d)", w, h));

        if (w == 0 || h == 0) {
            mVideoWidth = 640;
            mVideoHeight = 480;
            LogUtil.info(TAG, "Java: onVideoSizeChanged, no video stream, use default resolution: 640x480");
        } else {
            mVideoWidth = w;
            mVideoHeight = h;
        }

        SurfaceHolder holder = mPreview.getHolder();
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        mPreview.SetVideoRes(mVideoWidth, mVideoHeight);

        // will trigger onMeasure()
        mPreview.measure(MeasureSpec.AT_MOST, MeasureSpec.AT_MOST);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // TODO Auto-generated method stub
        if (mIsFlinging) {
            imageBackward.setVisibility(View.INVISIBLE);
            imageForward.setVisibility(View.INVISIBLE);
            mIsFlinging = false;
        }
    }

    private void updatePreveiwUI() {
        // view
        int width = mLayout.getMeasuredWidth();
        int height = mLayout.getMeasuredHeight();

        LogUtil.info(TAG, String.format("Java: adjust_ui preview %d x %d, video %d x %d", width, height, mVideoWidth, mVideoHeight));

        RelativeLayout.LayoutParams sufaceviewParams = (RelativeLayout.LayoutParams) mPreview.getLayoutParams();
        if (mVideoWidth * height > width * mVideoHeight) {
            LogUtil.info(TAG, "adjust_ui surfaceview is too tall, correcting");
            sufaceviewParams.width = width;
            sufaceviewParams.height = width * mVideoHeight / mVideoWidth;
        } else if (mVideoWidth * height < width * mVideoHeight) {
            LogUtil.info(TAG, "adjust_ui surfaceview is too wide, correcting");
            sufaceviewParams.width = height * mVideoWidth / mVideoHeight;
            sufaceviewParams.height = height;
        } else {
            sufaceviewParams.height = height;
            sufaceviewParams.width = width;
        }

        LogUtil.info(TAG, String.format("adjust_ui surfaceview setLayoutParams %d %d",
                sufaceviewParams.width, sufaceviewParams.height));
        mPreview.setLayoutParams(sufaceviewParams);
    }

    private void setupUpdater() {
        String name;
        Properties props = System.getProperties();
        String osArch = props.getProperty("os.arch");
        if (osArch != null && osArch.contains("86")) {
            name = "x86";
        } else if (osArch != null && osArch.contains("aarch64")) {
            name = "arm64";
        } else {
            name = "arm";
        }
        final String apk_name = "MeetPlayer-" + name + "-release.apk";

        if (null != apk_name && apk_name.length() > 0) {
            mDownloadProgressBar = (ProgressBar) findViewById(R.id.progressbar_download);
            mProgressTextView = (TextView) findViewById(R.id.textview_progress);

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(this);

            builder.setMessage("下载并更新应用?");

            builder.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadApk(apk_name);
                        }
                    });

            builder.setNeutralButton("取消", null);

            mUpdateDialog = builder.create();
            mUpdateDialog.show();
        }
    }

    private void downloadApk(String apk_name) {
        final String path;

        String url = HTTP_UPDATE_APK_URL + apk_name;
        path = Environment.getExternalStorageDirectory().getPath() + "/" + apk_name;
        LogUtil.info(TAG, "to download apk: " + path);

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

    private void installApk(String apk_fullpath) {
        LogUtil.info(TAG, "installApk: " + apk_fullpath);

        File apkfile = new File(apk_fullpath);
        if (!apkfile.exists()) {
            LogUtil.error(TAG, "apk file does not exist: " + apk_fullpath);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apk_fullpath)), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.info(TAG, "Java: onResume()");

        readSettings();

        if (!isTVbox) {
            LogUtil.info(TAG, String.format("screen %d x %d, preview height %d", screen_width, screen_height, preview_height));

            preview_height = screen_height * 2 / 5;
            mLayout.getLayoutParams().height = preview_height;
            mLayout.requestLayout(); //or invalidate();
        }

        if (Util.IsHaveInternet(this)) {
            String org_title = tv_title.getText().toString();
            StringBuffer sb = new StringBuffer();
            sb.append(org_title);
            if (!org_title.contains("ip:")) {
                sb.append("ip: ");
                sb.append(Util.getIpAddr(this));
            }
            if (!org_title.contains("http_port:")) {
                int http_port = MyHttpService.getPort();
                if (http_port != MyHttpService.DEFAULT_HTTP_PORT) {
                    sb.append(", http_port: ");
                    sb.append(http_port);
                }
            }

            tv_title.setText(sb.toString());

            //if (sb.toString().length() > 64)
            //    tv_title.setMarquee(false);
        }
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        LogUtil.info(TAG, "Java: onStart()");

        // Register receivers
        mScannerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.info(TAG, "Java: action Action: " + action);
                if (action.equals(MediaScannerService.ACTION_MEDIA_SCANNER_FINISHED)) {
                    Toast.makeText(ClipListActivity.this, "scan finished", Toast.LENGTH_SHORT).show();

                    new ListItemTask().execute("");
                } else if (action.equals(MediaScannerService.ACTION_MEDIA_SCANNER_STARTED)) {
                    Toast.makeText(ClipListActivity.this, "scan started", Toast.LENGTH_SHORT).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaScannerService.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(MediaScannerService.ACTION_MEDIA_SCANNER_FINISHED);

        registerReceiver(mScannerReceiver, filter);

        // Register receivers
        mHttpServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtil.info(TAG, "Java: action Action: " + action);
                if (action.equals(MyHttpService.ACTION_SERVICE_STARTED)) {
                    int port = intent.getIntExtra("http_port", -1);
                    String title = String.format(Locale.US,
                            "%s, http_port: %d", tv_title.getText().toString(), port);
                    tv_title.setText(title);
                }
            }
        };

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(MyHttpService.ACTION_SERVICE_STARTED);
        registerReceiver(mHttpServiceReceiver, filter2);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.info(TAG, "Java: onPause()");

        if (mPlayer != null && mPlayer.getDecodeMode() == DecodeMode.HW_XOPLAYER) {
            Log.i(TAG, "XOPlayer did not support HOME and resume");
            stop_player();
        }
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }

        writeSettings();

        //MeetSDK.closeLog();

        //MediaSDK.stopP2PEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //unbindService(dlna_conn);
        LogUtil.info(TAG, "Java: onDestroy()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.info(TAG, "Java: onStop()");

        if (mScannerReceiver != null) {
            unregisterReceiver(mScannerReceiver);
            mScannerReceiver = null;
        }

        if (mHttpServiceReceiver != null) {
            unregisterReceiver(mHttpServiceReceiver);
            mHttpServiceReceiver = null;
        }

        if (isFinishing()) {
            stop_player();
        } else {
            mHomed = true;
        }
    }

    @Override
    public void openOptionsMenu() {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: openOptionsMenu()");
        super.openOptionsMenu();
    }

    private void initFeedback() {
        LogcatHelper helper = LogcatHelper.getInstance();
        helper.init(this);
        AtvUtils.sContext = this;
        FeedBackFactory.sContext = this;
    }
	
	/*DLNAService.MyBinder binder;
	private ServiceConnection dlna_conn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			LogUtil.info(TAG, "Java: ClipActivity onServiceConnected()");
			binder = (DLNAService.MyBinder)service;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			LogUtil.info(TAG, "Java: ClipActivity onServiceDisconnected()");
		}
		
	};*/

    boolean add_list_history(String title, int playlink) {
        String key = "PlayHistory";
        String regularEx = ",";
        String values;
        values = Util.readSettings(this, key);

        LogUtil.info(TAG, "Java: PlayHistory read: " + values);
        String[] str = values.split(regularEx);
        int start = str.length - 10;
        if (start < 0)
            start = 0;
        for (int i = 0; i < str.length; i++) {
            LogUtil.debug(TAG, String.format("Java: PlayHistory #%d %s", i, str[i]));
        }

        // clip_name|11223,clip_2|34455
        StringBuffer save_values = new StringBuffer();
        for (int i = start; i < str.length; i++) {
            save_values.append(str[i]);
            save_values.append(regularEx);
        }

        save_values.append(title);
        save_values.append("|");
        save_values.append(playlink);

        //LogUtil.debug(TAG, "Java: PlayHistory write: " + save_values.toString());
        return Util.writeSettings(this, key, save_values.toString());
    }

    private boolean initDLNA() {
        //bindService(new Intent(DLNAService.ACTION), serv_conn, BIND_AUTO_CREATE);
		
		/*Intent intent = new Intent();
		intent.setAction(DLNAService.ACTION);
		startService(intent);
		bindService(intent, dlna_conn, Service.BIND_AUTO_CREATE);*/

        mDLNA = new DLNASdk();
        if (!mDLNA.isLibLoadSuccess()) {
            LogUtil.error(TAG, "Java: dlna failed to load dlna lib");
            return false;
        }

        mDLNAcallback = new IDlnaCallback(null);
        //mDLNA.setLogPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/xxxx_dlna.log");
        mDLNA.Init(mDLNAcallback);
        mDLNA.EnableRendererControler(true);

        //start file server
        Random rand = new Random();
        int i;
        i = rand.nextInt(100);
        int port = DLNA_LISTEN_PORT + i;
        mDLNA.StartHttpServer(port);
        LogUtil.info(TAG, String.format("Java: dlna start dlna server port: %d", port));
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: surfaceChanged()");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, String.format("Java: surfaceCreated() %s", holder.toString()));
        if (mPlayer != null && mHomed) {
            mPlayer.setDisplay(holder);
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: surfaceDestroyed()");
    }

    // callback of subtitle
    @Override
    public void onPrepared(boolean success, String msg) {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, String.format("Java: subtitle onPrepared() %s, %s", success ? "done" : "failed", msg));

        if (success) {
            mSubtitleThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    display_subtitle_thr();
                }
            });
            mSubtitleThread.start();
            mIsSubtitleUsed = true;

            mSubtitleTextView.setVisibility(View.VISIBLE);
            Toast.makeText(ClipListActivity.this,
                    "subtitle: " + subtitle_filename + " loaded",
                    Toast.LENGTH_SHORT).show();
        } else {
            mSubtitleTextView.setVisibility(View.INVISIBLE);
            Toast.makeText(ClipListActivity.this,
                    "failed to load subtitle: " + subtitle_filename + " , msg: " + msg,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private synchronized void display_subtitle_thr() {
        LogUtil.info(TAG, "Java: subtitle thread started");

        final int SLEEP_MSEC = 50;
        SubTitleSegment seg;
        long from_msec = 0;
        long to_msec = 0;
        long hold_msec;
        long target_msec;

        boolean isDisplay = true;
        boolean isDropItem = false;

        if (mPlayer != null) {
            // sync position
            mSubtitleSeeking = true;
            mSubtitleParser.seekTo(mPlayer.getCurrentPosition());

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
                    LogUtil.error(TAG, "Java: subtitle next_segment is null");
                    break;
                }

                mSubtitleText = seg.getData();
                from_msec = seg.getFromTime();
                to_msec = seg.getToTime();
                hold_msec = to_msec - from_msec;
                LogUtil.info(TAG, String.format("Java: subtitle from %d, to %d, hold %d, %s",
                        seg.getFromTime(), seg.getToTime(), hold_msec,
                        seg.getData()));
                target_msec = from_msec;
            } else {
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
                    //LogUtil.debug(TAG, "Java: subtitle wait");
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LogUtil.info(TAG, "Java: subtitle interrupted");
                    e.printStackTrace();
                    break;
                }
            }

            if (isDropItem == true) {
                // drop last subtitle item
                isDisplay = true;
                isDropItem = false;
                mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
                LogUtil.info(TAG, "Java: subtitle send hide");
                continue;
            }

            if (isDisplay) {
                mHandler.sendEmptyMessage(MSG_DISPLAY_SUBTITLE);
            } else {
                mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
            }

            isDisplay = !isDisplay;
        }

        mHandler.sendEmptyMessage(MSG_HIDE_SUBTITLE);
        mSubtitleParser.close();
        mSubtitleParser = null;
        LogUtil.info(TAG, "Java: subtitle thread exited");
    }

    @Override
    public void onSeekComplete() {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: subtitle onSeekComplete");
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
        } else {
            if (mLayout != null) {
                Drawable drawable2 = getResources().getDrawable(R.drawable.bg_border2);
                mLayout.setBackground(drawable2);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        LogUtil.debug(TAG, "Java: dispatchKeyEvent action " + action + " ,keyCode: " + keyCode);

        if (mPreviewFocused && action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (mPlayer != null && mPrepared) {
                    if (!mMediaController.isShowing()) {
                        mMediaController.show(5000);
                        return true;
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (!mPreviewFocused && !isTVbox && isLandscape) {
            // restore to portait normal view
            if (getSupportActionBar() != null)
                getSupportActionBar().show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        }

        super.onBackPressed();
    }

    static {
        //System.loadLibrary("lenthevcdec");
    }

}
