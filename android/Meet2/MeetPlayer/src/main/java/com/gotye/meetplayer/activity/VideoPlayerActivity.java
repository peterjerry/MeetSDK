package com.gotye.meetplayer.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.crashhandler.UploadLogTask;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.ui.widget.MicroMediaController;
import com.gotye.meetplayer.util.FileFilterTest;
import com.gotye.meetplayer.util.NetworkSpeed;
import com.gotye.meetplayer.util.Util;

import com.gotye.meetsdk.player.MediaPlayer;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.MeetVideoView;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.subtitle.SubTitleParser;
import com.gotye.meetsdk.subtitle.SubTitleSegment;

// ONLY support external subtitle???
public class VideoPlayerActivity extends AppCompatActivity
        implements SubTitleParser.OnReadyListener {

    private final static String TAG = "VideoPlayerActivity";

    private final static String[] mode_desc = {
            "自适应", "铺满屏幕", "放大裁切", "原始大小"};

    protected MeetVideoView mVideoView;
    protected MicroMediaController mController;
    private int mVideoWidth;
    private int mVideoHeight;
    private ProgressBar mBufferingProgressBar = null;

    protected Uri mUri = null;
    protected int mPlayerImpl = 0;
    protected int mFt = 0;
    protected int mBestFt = 3;
    protected String mTitle;
    protected int pre_seek_msec = -1;

    private boolean mPrepared = false;
    private boolean mIsBuffering = false;
    private boolean isScrolling = false;

    private LinearLayout mDragViewLayout;
    private TextView mTvSeekTime;
    private ImageView mSeekIcon;

    /* 记录上一次按返回键的时间 */
    private long backKeyTime = 0L;

    protected boolean mSwichingEpisode = false;

    // debug info
    private TextView mTextViewDebugInfo;
    private boolean mbShowDebugInfo = false;
    private NetworkSpeed mSpeed;

    private BatteryReceiver batteryReceiver;
    private int mBatteryPct = 100;

    private RelativeLayout mHoodLayout;
    private TextView mTvTitle;
    private ImageButton mBtnBack;
    private TextView mTvSysTime;
    private TextView mTvBattery;

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

    // subtitle
    private SimpleSubTitleParser mSubtitleParser;
    private TextView mSubtitleTextView;
    private String mSubtitleText;
    private Thread mSubtitleThread;
    private boolean mSubtitleSeeking = false;
    private boolean mIsSubtitleUsed = false;
    private String subtitle_filename;
    private boolean mSubtitleStoped = false;

    protected MainHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mHandler = new MainHandler(this);

        Intent intent = getIntent();
        mUri = intent.getData();
        LogUtil.info(TAG, "Java: mUri " + mUri.toString());

        if (intent.hasExtra("impl"))
            mPlayerImpl = intent.getIntExtra("impl", 0);
        else
            mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
        LogUtil.info(TAG, "Java player impl: " + mPlayerImpl);

        mTitle = intent.getStringExtra("title");
        mFt = intent.getIntExtra("ft", 0);
        mBestFt = intent.getIntExtra("best_ft", 3);

        pre_seek_msec = intent.getIntExtra("preseek_msec", -1);

        setContentView(R.layout.activity_video_player);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        this.mController = (MicroMediaController) findViewById(R.id.video_controller);
        this.mVideoView = (MeetVideoView) findViewById(R.id.video_view);
        this.mSubtitleTextView = (TextView) findViewById(R.id.textview_subtitle);
        this.mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
        this.mTextViewDebugInfo = (TextView) findViewById(R.id.tv_debuginfo);

        this.mDragViewLayout = (LinearLayout) this.findViewById(R.id.player_drag_view);
        this.mTvSeekTime = (TextView) this.findViewById(R.id.tv_seek_time);
        this.mSeekIcon = (ImageView) this.findViewById(R.id.player_seek_icon);

        this.mTextViewDebugInfo.setTextColor(Color.RED);
        this.mTextViewDebugInfo.setTextSize(18);
        this.mTextViewDebugInfo.setTypeface(Typeface.MONOSPACE);

        this.mController.setMediaPlayer(mVideoView);

        this.mHoodLayout = (RelativeLayout)this.findViewById(R.id.hood_layout);
        this.mTvTitle = (TextView)this.findViewById(R.id.player_title);
        this.mBtnBack = (ImageButton)this.findViewById(R.id.player_back_btn);
        this.mTvSysTime = (TextView)this.findViewById(R.id.tv_sys_time);
        this.mTvBattery = (TextView)this.findViewById(R.id.tv_battery);

        mBtnBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                finish();
            }
        });

        this.mVideoView.setLongClickable(true); // MUST set to enable double-tap and single-tap-confirm
        this.mVideoView.setOnTouchListener(mOnTouchListener);

        Util.initMeetSDK(this);

        // 注册广播接受者java代码
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //创建广播接受者对象
        batteryReceiver = new BatteryReceiver();

        //注册receiver
        registerReceiver(batteryReceiver, intentFilter);
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
        LogUtil.info(TAG, "Java: onOptionsItemSelected " + id);

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

                if (path.contains("&playlink=") ||
                        path.contains("&type=phone.android.vip")/*for cdn url*/) {
                    popupSelectFT();
                }
                else {
                    Toast.makeText(this, "当前视频无法选择码率", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.select_subtitle:
                popupSelectSubtitle();
                break;
            case R.id.show_mediainfo:
                popupMediaInfo();
                break;
            case R.id.toggle_debug_info:
                mbShowDebugInfo = !mbShowDebugInfo;

                if (mbShowDebugInfo)
                    mTextViewDebugInfo.setVisibility(View.VISIBLE);
                else
                    mTextViewDebugInfo.setVisibility(View.GONE);
                break;
            default:
                LogUtil.warn(TAG, "unknown menu id " + id);
                break;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        LogUtil.info(TAG, "Java: onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();

        LogUtil.info(TAG, "Java: onResume");

        setupPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LogUtil.info(TAG, "Java: onPause()");

        mVideoView.pause();

        unregisterReceiver(batteryReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        LogUtil.info(TAG, "Java: onStop()");

        stop_subtitle();
        stopPlayer();
    }

    private class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            //判断它是否是为电量变化的Broadcast Action
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                //获取当前电量
                int level = intent.getIntExtra("level", 0);
                //电量的总刻度
                int scale = intent.getIntExtra("scale", 100);

                mBatteryPct = level * 100 / scale;

                // fix tvbox problem
                if (mBatteryPct == 0)
                    mBatteryPct = 100;
            }
        }

    }

    private void popupMediaInfo() {
        String decodedUrl;
        try {
            decodedUrl = URLDecoder.decode(mUri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        StringBuffer sbInfo = new StringBuffer();
        sbInfo.append("文件名 ");
        sbInfo.append(decodedUrl);

        sbInfo.append("\n分辨率 ");
        sbInfo.append(mVideoWidth);
        sbInfo.append(" x ");
        sbInfo.append(mVideoHeight);

        new AlertDialog.Builder(this)
                .setTitle("媒体信息")
                .setMessage(sbInfo.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    protected void onSwitchBW() {

    }

    private void popupSelectFT() {
        final String[] ft_desc = {"流畅", "高清", "超清", "蓝光"};

        Dialog choose_ft_dlg = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setTitle("select ft")
                .setSingleChoiceItems(ft_desc, mFt,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton == mFt) {
                                    dialog.dismiss();
                                    return;
                                } else {
                                    if (whichButton > mBestFt) {
                                        Toast.makeText(VideoPlayerActivity.this,
                                                "该码率: " + ft_desc[whichButton] + " 无效",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(VideoPlayerActivity.this,
                                                "选择码率: " + ft_desc[whichButton], Toast.LENGTH_SHORT).show();

                                        mFt = whichButton;
                                        onSwitchBW();
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
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                LogUtil.info(TAG, "select player impl: " + whichButton);

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

        Dialog choose_subtitle_dlg = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setTitle("select subtitle")
                .setItems(str_file_list, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        stop_subtitle();

                        subtitle_filename = sub_folder + "/" + str_file_list[whichButton];
                        LogUtil.info(TAG, "Load subtitle file: " + subtitle_filename);
                        Toast.makeText(VideoPlayerActivity.this,
                                "Load subtitle file: " + subtitle_filename, Toast.LENGTH_SHORT).show();
                        start_subtitle(subtitle_filename);
                    }
                })
                .create();
        choose_subtitle_dlg.show();
    }

    private boolean start_subtitle(String filename) {
        LogUtil.info(TAG, "Java: subtitle start_subtitle " + filename);

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

    protected void setupPlayer() {
        LogUtil.info(TAG, "Step: setupPlayer()");

        DecodeMode DecMode;
        switch (mPlayerImpl) {
            case 0:
                DecMode = DecodeMode.AUTO;
                break;
            case 1:
                DecMode = DecodeMode.HW_SYSTEM;
                break;
            case 2:
                DecMode = DecodeMode.HW_XOPLAYER;
                break;
            case 3:
                DecMode = DecodeMode.SW;
                break;
            default:
                LogUtil.warn(TAG, String.format(Locale.US,
                        "Java: unknown DecodeMode: %d", mPlayerImpl));
                DecMode = DecodeMode.SW;
                break;
        }

        stopPlayer();

        mPrepared = false;

        mVideoView.setDecodeMode(DecMode);
        mVideoView.setVideoURI(mUri);
        mVideoView.setOnCompletionListener(mCompletionListener);
        mVideoView.setOnErrorListener(mErrorListener);
        mVideoView.setOnInfoListener(mInfoListener);
        mVideoView.setOnPreparedListener(mPreparedListener);

        String audio_opt = Util.readSettings(this, "last_audio_ip_port");
        if (audio_opt != null && !audio_opt.isEmpty())
            mVideoView.setOption("-dump_url " + audio_opt);

        String schema = mUri.getScheme();
        String path = null;
        if ("file".equalsIgnoreCase(schema))
            path = mUri.getPath();
        else
            path = mUri.toString();

        String title;
        if (mTitle != null) {
            title = mTitle;
        } else {
            title = getFileName(path);
        }
        mTvTitle.setText(title);

        if (pre_seek_msec != -1) {
            LogUtil.info(TAG, "Java: pre seek to " + pre_seek_msec + " msec");

            mVideoView.seekTo(pre_seek_msec);
            pre_seek_msec = -1;
        }

        if (!path.startsWith("/") && !path.startsWith("file://")) {
            // ONLY network media need buffering
            mBufferingProgressBar.setVisibility(View.VISIBLE);
            mIsBuffering = true;

            mSpeed = new NetworkSpeed();
            mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_NETWORK_SPEED);
        }

        mVideoView.start();
    }

    protected void stopPlayer() {
        LogUtil.info(TAG, "stopPlayer()");

        mBufferingProgressBar.setVisibility(View.GONE);
        mHandler.removeMessages(MainHandler.MSG_UPDATE_NETWORK_SPEED);

        mVideoView.stopPlayback();
    }

    private String getFileName(String path) {
        String name = "N/A";
        if (path.startsWith("/") || path.startsWith("file://")) {
            int pos = path.lastIndexOf('/');
            if (pos != -1)
                name = path.substring(pos + 1, path.length());

            if (name.length() > 16)
                name = name.substring(0, 16) + "...";
        } else if (path.startsWith("http://")) {
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

    protected void onCompleteImpl() {
        finish();
    }

    protected void onErrorImpl() {
        Util.makeUploadLog("failed to play: " + mUri.toString() + "\n\n");
        UploadLogTask task = new UploadLogTask(VideoPlayerActivity.this);
        task.execute(Util.upload_log_path, "failed to play");
        finish();
    }

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            LogUtil.info(TAG, "MEDIA_PLAYBACK_COMPLETE");

            stopPlayer();

            onCompleteImpl();
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            LogUtil.error(TAG, "Error: " + framework_err + "," + impl_err);

            Toast.makeText(VideoPlayerActivity.this,
                    String.format("Player onError: what %d extra %d", framework_err, impl_err),
                    Toast.LENGTH_SHORT).show();

            stopPlayer();

            onErrorImpl();
            return true;
        }
    };

    private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            LogUtil.debug(TAG, "Java: onInfo: " + what + " " + extra);

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
                else if (MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER == extra)
                    str_player_type = "XO Player";
                else if (MediaPlayer.PLAYER_IMPL_TYPE_FF_PLAYER == extra)
                    str_player_type = "FF Player";
                else if (MediaPlayer.PLAYER_IMPL_TYPE_OMX_PLAYER == extra)
                    str_player_type = "OMX Player";
                else
                    str_player_type = "Unknown Player";
                Toast.makeText(VideoPlayerActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
            } else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_AVG_MSEC == what) {
                decode_avg_msec = extra;
            } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_AVG_MSEC == what) {
                render_avg_msec = extra;
            } else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS == what) {
                decode_fps = extra;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
            } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS == what) {
                render_fps = extra;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
            } else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME == what) {
                render_frame_num = extra;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_RENDER_INFO);
            } else if (MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC == what) {
                av_latency_msec = extra;
            } else if (MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME == what) {
                decode_drop_frame++;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_RENDER_INFO);
            } else if (MediaPlayer.MEDIA_INFO_TEST_MEDIA_BITRATE == what) {
                video_bitrate = extra;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
            } else if (android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {

            } else if (MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING == what) {
                av_latency_msec = extra;

                decode_fps = render_fps = 0;
                decode_drop_frame = 0;
                video_bitrate = 0;
                mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
            }

            return true;
        }
    };

    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            LogUtil.info(TAG, "Java: OnPrepared");

            mPrepared = true;
            toggleMediaControlsVisiblity();

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mBufferingProgressBar.setVisibility(View.GONE);
        }
    };

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
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    if (e2.getEventTime() - e1.getDownTime() < 150)
                        return true;

                    mHandler.removeMessages(MainHandler.MSG_SCROLL_SEEK);

                    if (mVideoView.isPlaying()) {
                        if (!isScrolling) {
                            mDragViewLayout.setVisibility(View.VISIBLE);
                            isScrolling = true;
                        }

                        float delta = e2.getX() - e1.getX();
                        float pct = delta / 1000;

                        int duration = mVideoView.getDuration();
                        int pos = mVideoView.getCurrentPosition();
                        if (duration > 0) {
                            int new_pos = pos + (int)(duration * pct);
                            if (new_pos < 0)
                                new_pos = 0;
                            else if (new_pos > duration)
                                new_pos = duration;

                            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
                            formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00")); // fix 08:00:01 322 bug
                            String str_pos = formatter.format(new_pos);
                            String str_duration = formatter.format(duration);
                            mTvSeekTime.setText(String.format(Locale.US, "%s / %s", str_pos, str_duration));

                            mSeekIcon.setImageResource(delta >= 0 ? R.drawable.player_small_forward
                                    : R.drawable.player_small_backword);

                            Message msg = mHandler.obtainMessage(MainHandler.MSG_SCROLL_SEEK, new_pos, 0);
                            mHandler.sendMessageDelayed(msg, 300);
                        }
                    }

                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    final int FLING_MIN_DISTANCE = 100;
                    final float FLING_MIN_VELOCITY = 2000.0f;

                    if (Math.abs(e1.getX() - e2.getX()) > FLING_MIN_DISTANCE
                            && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                        float delta = e2.getX() - e1.getX();
                        float pct = delta / 1000.f;

                        if (mVideoView.isPlaying()) {
                            int duration = mVideoView.getDuration();
                            int pos = mVideoView.getCurrentPosition();
                            if (duration > 0) {
                                int new_pos;
                                if (isScrolling) {
                                    new_pos = pos + (int) (duration * pct);
                                } else {
                                    int incr = (e2.getX() > e1.getX() ? 1 : -1);
                                    new_pos = pos + 15000 * incr;
                                }

                                if (new_pos < 0)
                                    new_pos = 0;
                                else if (new_pos > duration)
                                    new_pos = duration;

                                mVideoView.seekTo(new_pos);

                                if (isScrolling) {
                                    mDragViewLayout.setVisibility(View.GONE);
                                    isScrolling = false;
                                } else {
                                    mDragViewLayout.setVisibility(View.VISIBLE);

                                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
                                    formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00")); // fix 08:00:01 322 bug
                                    String str_pos = formatter.format(new_pos);
                                    String str_duration = formatter.format(duration);
                                    mTvSeekTime.setText(String.format(Locale.US, "%s / %s", str_pos, str_duration));

                                    mSeekIcon.setImageResource(delta >= 0 ? R.drawable.player_small_forward
                                            : R.drawable.player_small_backword);

                                    Message msg = mHandler.obtainMessage(MainHandler.MSG_HIDE_DRAG_VIEW, new_pos, 0);
                                    mHandler.sendMessageDelayed(msg, 2000);
                                }
                            }
                        }
                    }

                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleMediaControlsVisiblity();
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent event) {
                    switchDisplayMode(1);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    LogUtil.info(TAG, "onLongPress!!!");
                }
            });

    private void switchDisplayMode(int incr) {
        if (mVideoView != null) {
            int mode = mVideoView.getDisplayMode();
            mode = mode + incr;
            if (mode < 0)
                mode = 3;
            else if (mode > 3)
                mode = 0;
            mVideoView.setDisplayMode(mode);
            Toast.makeText(this, "mode switch to " + mode_desc[mode], Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // micro mediacontroller
        // should pay more attention to dispatchKeyEvent()
        // sometimes event NOT come
        // it's weird
        // maybe layout component order take effect?
        // MeetVideoView onKeyDown has changed to return FALSE by default
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                switchDisplayMode(keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1: -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!mController.isShowing()) {
                    toggleMediaControlsVisiblity();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!mController.isShowing()) {
                    if (!mSwichingEpisode) {
                        mSwichingEpisode = true;
                        onSelectEpisode(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                    }
                    else {
                        LogUtil.warn(TAG, "already switching epsode...");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (mController.isShowing()) {
                    toggleMediaControlsVisiblity();
                } else if ((System.currentTimeMillis() - backKeyTime) > 2000) {
                    Toast.makeText(VideoPlayerActivity.this,
                            "再按一次退出", Toast.LENGTH_SHORT)
                            .show();
                    backKeyTime = System.currentTimeMillis();
                } else {
                    onBackPressed();
                }
                return true;
            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void onSelectEpisode(int incr) {
    }

    private void showHood() {
        showHood(3000);
    }

    private void showHood(int msec) {
        mHandler.removeMessages(MainHandler.MSG_HIDE_HOOD);

        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
        String str_time = format.format((new Date()));
        mTvSysTime.setText(str_time);
        mTvBattery.setText(String.format(Locale.US,
                "电池 %d", mBatteryPct));

        mHoodLayout.setVisibility(View.VISIBLE);
        if (msec > 0)
            mHandler.sendEmptyMessageDelayed(MainHandler.MSG_HIDE_HOOD, msec);
    }

    public void toggleMediaControlsVisiblity() {
        if (mPrepared) {
            if (mController.isShowing()) {
                mController.hide();
                mHoodLayout.setVisibility(View.GONE);
            } else {
                mController.show();
                showHood();
            }
        }
    }

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
            Toast.makeText(VideoPlayerActivity.this,
                    "subtitle: " + subtitle_filename + " loaded",
                    Toast.LENGTH_SHORT).show();
        } else {
            mSubtitleTextView.setVisibility(View.INVISIBLE);
            Toast.makeText(VideoPlayerActivity.this,
                    "failed to load subtitle: " + subtitle_filename + " , msg: " + msg,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSeekComplete() {
        // TODO Auto-generated method stub
        LogUtil.info(TAG, "Java: subtitle onSeekComplete");
        mSubtitleSeeking = false;
    }

    protected void onEpisodeDone() {

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
                    LogUtil.error(TAG, "Java: subtitle next_segment is null");
                    break;
                }

                mSubtitleText = seg.getData();
                from_msec = seg.getFromTime();
                to_msec = seg.getToTime();
                hold_msec = to_msec - from_msec;
                LogUtil.info(TAG, String.format(Locale.US,
                        "Java: subtitle frome %d, to %d, hold %d, %s",
                        seg.getFromTime(), seg.getToTime(), hold_msec,
                        seg.getData()));
                target_msec = from_msec;
            } else {
                target_msec = to_msec;
            }

            if (mSubtitleSeeking) {
                isDropItem = true;
                target_msec = mVideoView.getDuration();
            }

            while (mVideoView != null && mVideoView.getCurrentPosition() < target_msec) {
                if (isDropItem) {
                    if (!mSubtitleSeeking)
                        break;
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

            LogUtil.info(TAG, "Java: subtitle mSubtitleSeeking: " + mSubtitleSeeking);
            if (isDropItem) {
                // drop last subtitle item
                isDisplay = true;
                isDropItem = false;
                mHandler.sendEmptyMessage(MainHandler.MSG_HIDE_SUBTITLE);
                continue;
            }

            if (isDisplay) {
                mHandler.sendEmptyMessage(MainHandler.MSG_DISPLAY_SUBTITLE);
            } else {
                mHandler.sendEmptyMessage(MainHandler.MSG_HIDE_SUBTITLE);
            }

            isDisplay = !isDisplay;
        }

        mHandler.sendEmptyMessage(MainHandler.MSG_HIDE_SUBTITLE);
        mSubtitleParser.close();
        mSubtitleParser = null;
        LogUtil.info(TAG, "Java: subtitle thread exited");
    }

    protected static class MainHandler extends Handler {
        private WeakReference<VideoPlayerActivity> mWeakActivity;

        // message
        public final static int MSG_DISPLAY_SUBTITLE       = 401;
        public final static int MSG_HIDE_SUBTITLE          = 402;
        public final static int MSG_UPDATE_PLAY_INFO       = 403;
        public final static int MSG_UPDATE_RENDER_INFO     = 404;
        public final static int MSG_UPDATE_NETWORK_SPEED   = 405;
        public final static int MSG_RESTART_PLAYER         = 406;
        public final static int MSG_HIDE_HOOD              = 501;

        public final static int MSG_SCROLL_SEEK             = 301;
        public final static int MSG_HIDE_DRAG_VIEW          = 302;

        public final static int MSG_EPISODE_DONE            = 601;
        public final static int MSG_PLAY_CDN_FT		        = 602;
        public final static int MSG_FAIL_TO_GET_FT	        = 622;
        public final static int MSG_FAIL_TO_GET_CDN_URL     = 623;

        public MainHandler(VideoPlayerActivity activity) {
            mWeakActivity = new WeakReference<VideoPlayerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoPlayerActivity activity = mWeakActivity.get();
            if (activity == null) {
                LogUtil.debug(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_HIDE_HOOD:
                    activity.mHoodLayout.setVisibility(View.GONE);
                    break;
                case MSG_UPDATE_NETWORK_SPEED:
                    int[] speed = activity.mSpeed.currentSpeed();
                    if (speed != null) {
                        activity.rx_speed = speed[0];
                        activity.tx_speed = speed[1];
                        this.sendEmptyMessageDelayed(MSG_UPDATE_NETWORK_SPEED, 1000);
                    }
                case MSG_UPDATE_PLAY_INFO:
                case MSG_UPDATE_RENDER_INFO:
                    if (activity.mbShowDebugInfo) {
                        activity.mTextViewDebugInfo.setText(String.format(Locale.US,
                                "%02d|%03d v-a: %+04d dec/render %d(%d)/%d(%d) fps/msec\n" +
                                        "bitrate %d kbps\n" +
                                        "rx %d kB/s, tx %d kB/s",
                                activity.render_frame_num % 25, activity.decode_drop_frame % 1000, activity.av_latency_msec,
                                activity.decode_fps, activity.decode_avg_msec, activity.render_fps, activity.render_avg_msec,
                                activity.video_bitrate,
                                activity.rx_speed, activity.tx_speed));
                    }
                    break;
                case MSG_DISPLAY_SUBTITLE:
                    activity.mSubtitleTextView.setText(activity.mSubtitleText);
                    break;
                case MSG_HIDE_SUBTITLE:
                    activity.mSubtitleTextView.setText("");
                    break;
                case MSG_RESTART_PLAYER:
                    activity.setupPlayer();
                    break;
                case MSG_SCROLL_SEEK:
                    LogUtil.info(TAG, "MSG_SCROLL_SEEK");
                    if (activity.isScrolling) {
                        activity.mVideoView.seekTo(msg.arg1);
                        activity.mDragViewLayout.setVisibility(View.GONE);
                        activity.isScrolling = false;
                    }
                    break;
                case MSG_HIDE_DRAG_VIEW:
                    activity.mDragViewLayout.setVisibility(View.GONE);
                    break;
                case MSG_EPISODE_DONE:
                    activity.onEpisodeDone();
                    break;
                case MSG_PLAY_CDN_FT:
                    LogUtil.info(TAG, "MSG_PLAY_CDN_FT");
                    break;
                case MSG_FAIL_TO_GET_FT:
                    LogUtil.error(TAG, "failed to get ft");
                    Toast.makeText(activity, "获取CDN FT失败", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_FAIL_TO_GET_CDN_URL:
                    LogUtil.error(TAG, "failed to get cdn url");
                    Toast.makeText(activity, "获取CDN播放地址失败", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    LogUtil.warn(TAG, "Java: unknown msg.what " + msg.what);
                    break;
            }
        }
    }

}
