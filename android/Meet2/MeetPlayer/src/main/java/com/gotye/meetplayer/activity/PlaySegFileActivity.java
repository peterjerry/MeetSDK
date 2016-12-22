package com.gotye.meetplayer.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.media.FragmentMp4MediaPlayerV2;
import com.gotye.meetplayer.ui.MyPreView2;
import com.gotye.meetplayer.ui.widget.MicroMediaController;
import com.gotye.meetplayer.util.IDlnaCallback;
import com.gotye.meetplayer.util.Util;
import com.gotye.meetsdk.player.MediaController.MediaPlayerControl;
import com.gotye.meetsdk.player.MediaPlayer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class PlaySegFileActivity extends AppCompatActivity
		implements SurfaceHolder.Callback {
	private final static String TAG = "PlaySegFileActivity";
	
	private RelativeLayout mLayout;
	private MyPreView2 mView;
	private SurfaceHolder mHolder;
	protected FragmentMp4MediaPlayerV2 mPlayer;
    private int mPlayerImpl;
    private boolean mPrepared = false;
	private MicroMediaController mController;
	private MyMediaPlayerControl mMediaPlayerControl;
    private RelativeLayout mHoodLayout;
    private TextView mTvTitle;
    private TextView mTvSysTime;
    private TextView mTvBattery;
    private ImageButton mBtnBack;
    private ImageButton mBtnOption;
	protected ProgressBar mBufferingProgressBar;
    private TextView mTvInfo;
    private boolean mbTvInfoShowing = false;

    private BatteryReceiver batteryReceiver;
    private int mBatteryPct = 100;

    // stat
    private int decode_fps					= 0;
    private int render_fps 					= 0;
    private int decode_avg_msec 			= 0;
    private int render_avg_msec 			= 0;
    private int render_frame_num			= 0;
    private int decode_drop_frame			= 0;
    private int av_latency_msec				= 0;
    private int video_bitrate				= 0;

    protected String mUrlListStr;
    protected String mDurationListStr;
	protected String mTitle;
    protected int mFt = 2;
    protected int pre_seek_msec = -1;

    protected MainHandler mHandler;
	
	private int mVideoWidth, mVideoHeight;
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;

    protected boolean mIsBuffering = false;
	protected boolean mSwichingEpisode = false;

    // dlna
    protected String mDlnaDeviceUUID;
    protected String mDlnaDeviceName;
	
	/* 记录上一次按返回键的时间 */
    private long backKeyTime = 0L;
	
	private final static int MEDIA_CONTROLLER_TIMEOUT = 3000;
	
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private MediaPlayer.OnErrorListener	 mOnErrorListener;
	private MediaPlayer.OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdate;
	private MediaPlayer.OnInfoListener mOnInfoListener;
	
	private final static String url_list = "http://data.vod.itc.cn/?" +
			"new=/49/197/T9vx2eIRoGJa8v2svlzxkN.mp4&vid=1913402&ch=tv" +
			"&cateCode=115;115102;115103;115105&plat=6&mkey=lZzltZEl0zZ8BypeOS30vK03Trnxwtvj&prod=app," +
			"http://data.vod.itc.cn/?new=/208/249/k0qpKxLzSiu8jYs1996riF.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=sVCsCgJXiDnjxX_2LgPqycXUPkxWsOgb&prod=app," +
			"http://data.vod.itc.cn/?new=/95/201/DFpUuRsTRdmhqZqwkNfD5B.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=wK4roB-c4dClMKzMM5io8AkcU1woYZRK&prod=app," +
			"http://data.vod.itc.cn/?new=/94/253/XNegXt7MRBqTC7zfbSV9MB.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=m2lq1NlURrit46JngILRgC0-K7oIDT3I&prod=app," +
			"http://data.vod.itc.cn/?new=/241/134/TmaV7dUCTom7m9F5396l0D.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=kK4jgq0w6aS7b7z_Mm7h9GnS4QbxUfnx&prod=app";
	
	private final static String duration_list = "300.12,300.04,300.04,300.04,219.011";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_frag_mp4_player);

		if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent intent = getIntent();
        mPlayerImpl = intent.getIntExtra("player_impl", 1);
        if (intent.hasExtra("url_list") && intent.hasExtra("duration_list")) {
            mUrlListStr			= intent.getStringExtra("url_list");
            mDurationListStr	= intent.getStringExtra("duration_list");
            mTitle				= intent.getStringExtra("title");
            mFt                 = intent.getIntExtra("ft", 0);
            pre_seek_msec       = intent.getIntExtra("preseek_msec", -1);

            LogUtil.info(TAG, "Java: mDurationListStr " + mDurationListStr);
        }
        else {
            // just for test
            LogUtil.warn(TAG, "Java: use test url and duration list");

            mUrlListStr 		= url_list;
            mDurationListStr	= duration_list;
        }

		Util.initMeetSDK(this);

        this.mLayout 				= (RelativeLayout) findViewById(R.id.main_layout);
        this.mView 					= (MyPreView2) findViewById(R.id.player_view);
        this.mController 			= (MicroMediaController) findViewById(R.id.video_controller);
        this.mBufferingProgressBar 	= (ProgressBar) findViewById(R.id.progressbar_buffering);

        this.mHoodLayout = (RelativeLayout)this.findViewById(R.id.hood_layout);
        this.mTvTitle = (TextView)this.findViewById(R.id.player_title);
        this.mBtnBack = (ImageButton)this.findViewById(R.id.player_back_btn);
        this.mBtnOption = (ImageButton)this.findViewById(R.id.option_btn);
        this.mTvSysTime = (TextView)this.findViewById(R.id.tv_sys_time);
        this.mTvBattery = (TextView)this.findViewById(R.id.tv_battery);

        this.mTvInfo = (TextView)this.findViewById(R.id.tv_info);

        mBtnBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                finish();
            }
        });

        mBtnOption.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(PlaySegFileActivity.this, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.seg_player_menu, popup.getMenu());
                popup.show();

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        processMenuItem(id);
                        return true;
                    }
                });
            }
        });

        mView.setLongClickable(true); // MUST set to enable double-tap and single-tap-confirm
        mView.setOnTouchListener(mOnTouchListener);

		mMediaPlayerControl = new MyMediaPlayerControl();
		mController.setMediaPlayer(mMediaPlayerControl);
		
		SurfaceHolder holder = mView.getHolder();
        if (mPlayerImpl == 3) {
            holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
            holder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
        }
		holder.addCallback(this);
		
		m_playlink_list = new ArrayList<String>();
		m_duration_list = new ArrayList<Integer>();

        mHandler = new MainHandler(this);

        mOnInfoListener = new MediaPlayer.OnInfoListener() {
			
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				//LogUtil.debug(TAG, "Java: onInfo what " + what + " , extra " + extra);
				
				if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
					LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_START");
					mIsBuffering = true;
					mBufferingProgressBar.setVisibility(View.VISIBLE);
				}
				else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
                    LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_END");
					mIsBuffering = false;
					mBufferingProgressBar.setVisibility(View.GONE);
				}
				else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    LogUtil.info(TAG, "Java: onInfo MEDIA_INFO_VIDEO_RENDERING_START");
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_AVG_MSEC == what) {
                    decode_avg_msec = extra;
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_AVG_MSEC == what) {
                    render_avg_msec = extra;
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS == what) {
                    decode_fps = extra;
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS == what) {
                    render_fps = extra;
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME == what) {
                    render_frame_num = extra;
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_RENDER_INFO);
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC == what) {
                    av_latency_msec = extra;
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME == what) {
                    decode_drop_frame++;
                    LogUtil.info(TAG, String.format(Locale.US,
                            "Java: onInfo MEDIA_INFO_TEST_DROP_FRAME %d msec", extra));
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_RENDER_INFO);
                }
                else if (MediaPlayer.MEDIA_INFO_TEST_MEDIA_BITRATE == what) {
                    video_bitrate = extra;
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
                }
                else if(MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING == what) {
                    av_latency_msec = extra;

                    decode_fps = render_fps = 0;
                    decode_drop_frame = 0;
                    video_bitrate = 0;
                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PLAY_INFO);
                }
				
				return true;
			}
		};
		
		mOnBufferingUpdate = new MediaPlayer.OnBufferingUpdateListener() {
			
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int pct) {
				// TODO Auto-generated method stub
                LogUtil.info(TAG, "Java: onBufferingUpdate " + pct);
			}
		};
		
		mOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
			
			@Override
			public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
				// TODO Auto-generated method stub
				mVideoWidth		= w;
				mVideoHeight	= h;

                mHolder.setFixedSize(w, h);
                mView.SetVideoRes(w, h);
			}
		};
		
		mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
                LogUtil.info(TAG, "Java: onPrepared()");

                mPrepared = true;
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				toggleMediaControlsVisiblity();

                // MUST use mPlayer because mp is seg player
                if (pre_seek_msec > 0) {
                    mPlayer.seekTo(pre_seek_msec);
                    pre_seek_msec = -1;
                }

                mPlayer.start();
			}
		};
		
		mOnErrorListener = new MediaPlayer.OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int error, int extra) {
				// TODO Auto-generated method stub
                LogUtil.error(TAG, "Java: onError what " + error + " , extra " + extra);
				
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);

                if (mPlayerImpl == 1) {
                    mPlayerImpl = 3;
                    mView.setVisibility(View.INVISIBLE);
                    mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                    mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
                    mView.setVisibility(View.VISIBLE);

                    Util.writeSettingsInt(PlaySegFileActivity.this, "PlayerImpl", mPlayerImpl);
                    Toast.makeText(PlaySegFileActivity.this, "尝试使用软解模式", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(PlaySegFileActivity.this, "播放器错误: 错误码 " + error + " , 详细 " + extra,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
				
				return true;
			}
		};

		mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
                OnComplete();
			}
		};

        //注册广播接受者java代码
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //创建广播接受者对象
        batteryReceiver = new BatteryReceiver();

        //注册receiver
        registerReceiver(batteryReceiver, intentFilter);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.seg_player_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        processMenuItem(id);
        return true;
    }

    private void processMenuItem(int id) {
        switch (id) {
            case R.id.select_player_impl:
                popupPlayerImplDlg();
                break;
            case R.id.select_ft:
                popupSelectFT();
                break;
            case R.id.select_episode:
                onSelectEpisode();
                break;
            case R.id.next_episode:
                onSelectEpisode(1);
                break;
            case R.id.previous_episode:
                onSelectEpisode(-1);
                break;
            case R.id.show_mediainfo:
                popupMediaInfo();
                break;
            case R.id.show_relate_video:
                onShowRelateVideo();
                break;
            case R.id.dlna_push_to_dmr:
                push_to_dmr();
                break;
            case R.id.toggle_debug_info:
                mbTvInfoShowing = !mbTvInfoShowing;
                if (mbTvInfoShowing) {
                    mTvInfo.setVisibility(View.VISIBLE);
                }
                else {
                    mTvInfo.setVisibility(View.GONE);
                }
                break;
            default:
                break;
        }
    }

    private void popupPlayerImplDlg() {
        final String[] PlayerImpl = {"System", "XOPlayer", "FFPlayer"};

        Dialog choose_player_impl_dlg = new AlertDialog.Builder(PlaySegFileActivity.this)
                .setTitle("选择播放器类型")
                .setSingleChoiceItems(PlayerImpl, mPlayerImpl - 1, /*default selection item number*/
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton){
                                LogUtil.info(TAG, "select player impl: " + whichButton);

                                mPlayerImpl = whichButton + 1;
                                Util.writeSettingsInt(PlaySegFileActivity.this, "PlayerImpl", mPlayerImpl);
                                Toast.makeText(PlaySegFileActivity.this,
                                        "选择类型: " + PlayerImpl[whichButton], Toast.LENGTH_SHORT).show();

                                if (mPlayer != null) {
                                    pre_seek_msec = mPlayer.getCurrentPosition() - 5000;
                                    if (pre_seek_msec < 0)
                                        pre_seek_msec = 0;

                                    LogUtil.info(TAG, "pre_seek_msec set to: " + pre_seek_msec);

                                    mView.setVisibility(View.INVISIBLE);

                                    if (mPlayerImpl == 3/*ffplay*/) {
                                        SurfaceHolder holder = mView.getHolder();
                                        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
                                        holder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
                                    }

                                    mView.setVisibility(View.VISIBLE);
                                }

                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("取消", null)
                .create();
        choose_player_impl_dlg.show();
    }

    private void push_to_dmr() {
        int dev_num = IDlnaCallback.mDMRmap.size();

        if (dev_num == 0) {
            LogUtil.info(TAG, "Java: dlna no dlna device found");
            Toast.makeText(this, "未发现DMR设备", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> dev_list = new ArrayList<String>();
        ArrayList<String> uuid_list = new ArrayList<String>();
        for (String key : IDlnaCallback.mDMRmap.keySet()) {
            String name = IDlnaCallback.mDMRmap.get(key);
            LogUtil.info(TAG, "Java: dlna [dlna dmr] uuid: " + key + " name: " + name);
            uuid_list.add(key);
            dev_list.add(name);
        }

        final String[] str_uuid_list = uuid_list.toArray(new String[uuid_list.size()]);
        final String[] str_dev_list = dev_list.toArray(new String[dev_list.size()]);

        Dialog choose_device_dlg = new AlertDialog.Builder(this)
                .setTitle("选择dlna推送设备")
                .setItems(str_dev_list,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                mDlnaDeviceUUID = str_uuid_list[whichButton];
                                mDlnaDeviceName = str_dev_list[whichButton];

                                push_cdn_clip();
                            }
                        })
                .setNegativeButton("取消", null)
                .create();
        choose_device_dlg.show();
    }

    protected void push_cdn_clip() {

    }

    private void popupMediaInfo() {
        if (mPlayer == null) {
            Toast.makeText(this, "无法获取媒体信息", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuffer sbInfo = new StringBuffer();
        sbInfo.append("文件列表 ");
        sbInfo.append(m_playlink_list.toString());
        sbInfo.append("\n文件时长列表 ");
        sbInfo.append(m_duration_list.toString());

        sbInfo.append("\n文件时长列表2 [");
        int total_msec = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00")); // fix 08:00:01 322 bug
        for (int i=0;i<m_duration_list.size();i++) {
            int duration = m_duration_list.get(i);
            total_msec += duration;
            String str_duration = formatter.format(total_msec);

            if (i > 0)
                sbInfo.append("   ");
            sbInfo.append(str_duration);
        }
        sbInfo.append("]");

        sbInfo.append("\n分辨率 ");
        sbInfo.append(mVideoWidth);
        sbInfo.append(" x ");
        sbInfo.append(mVideoHeight);
        sbInfo.append("\n时长 ");
        sbInfo.append(mPlayer.getDuration());

        new AlertDialog.Builder(this)
                .setTitle("媒体信息")
                .setMessage(sbInfo.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    protected void OnComplete() {
        Toast.makeText(PlaySegFileActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
        mIsBuffering = false;
        mBufferingProgressBar.setVisibility(View.GONE);

        finish();
    }

    protected void onSelectEpisode() {

    }

    protected void onSelectFt() {

    }

    private void popupSelectFT() {
        final String[] ft_desc = {"流畅", "高清", "超清", "蓝光"};

        Dialog choose_ft_dlg = new AlertDialog.Builder(PlaySegFileActivity.this)
                .setTitle("选择码率")
                .setSingleChoiceItems(ft_desc, mFt,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (whichButton == mFt) {
                                    dialog.dismiss();
                                    return;
                                } else {
                                    Toast.makeText(PlaySegFileActivity.this,
                                            "选择码率: " + ft_desc[whichButton], Toast.LENGTH_SHORT).show();

                                    mFt = whichButton;
                                    pre_seek_msec = mPlayer.getCurrentPosition() - 5000;
                                    if (pre_seek_msec < 0)
                                        pre_seek_msec = 0;

                                    onSelectFt();
                                    dialog.dismiss();
                                }
                            }
                        })
                .create();
        choose_ft_dlg.show();
    }

    protected void onShowRelateVideo() {

    }

    protected void onSelectEpisode(int incr) {
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    };

    private GestureDetector mGestureDetector =
            new GestureDetector(getApplication(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    final int FLING_MIN_DISTANCE = 200;
                    final float FLING_MIN_VELOCITY = 1000.0f;
                    // 1xxx - 4xxx

                    float distance = e2.getX() - e1.getX();
                    if (Math.abs(distance) > FLING_MIN_DISTANCE
                            && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                        if (mPlayer != null) {
                            int pos = mPlayer.getCurrentPosition();
                            int incr = (distance > 0f ? 1 : -1);
                            pos += incr * 15000; // 15sec
                            if (pos > mPlayer.getDuration())
                                pos = mPlayer.getDuration();
                            else if (pos < 0)
                                pos = 0;

                            mPlayer.seekTo(pos);
                        }
                    }

                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    LogUtil.info(TAG, "Java: onSingleTapConfirmed!!!");

                    toggleMediaControlsVisiblity();
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent event) {
                    SwitchDisplayMode(1);
                    return true;
                }
            });

    private void SwitchDisplayMode(int incr) {
        mView.switchDisplayMode(incr);
        Toast.makeText(this, "切换显示模式至 " + mView.getDisplayMode(),
                Toast.LENGTH_SHORT).show();
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
        if (mPlayer != null && mPrepared) {
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!mController.isShowing()) {
                    if (mPlayer != null) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                                keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            SwitchDisplayMode(keyCode == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1);
                        } else {
                            toggleMediaControlsVisiblity();
                        }
                    }
                }

                return true;
            case KeyEvent.KEYCODE_BACK:
                if (mController.isShowing()) {
                    toggleMediaControlsVisiblity();
                } else if ((System.currentTimeMillis() - backKeyTime) > 2000) {
                    Toast.makeText(PlaySegFileActivity.this,
                            "press another time to exit", Toast.LENGTH_SHORT)
                            .show();
                    backKeyTime = System.currentTimeMillis();
                } else {
                    onBackPressed();
                }

                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}

        unregisterReceiver(batteryReceiver);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (mPlayer != null && event.getAction() == MotionEvent.ACTION_UP) {
			if (mController.isShowing())
				mController.hide();
			else
				mController.show(MEDIA_CONTROLLER_TIMEOUT);
		}
		
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		mHolder = sh;
		
		buildPlaylinkList();
		setupMediaPlayer();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		
	}

    protected void onShedule() {

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

    protected static class MainHandler extends Handler {
        private WeakReference<PlaySegFileActivity> mWeakActivity;

        protected final static int MSG_PLAY_NEXT_EPISODE 		= 1;
        protected final static int MSG_SHOW_MEDIA_CONTROLLER	= 3;
        protected final static int MSG_HIDE_HOOD                = 501;

        protected final static int MSG_INVALID_EPISODE_INDEX	= 101;
        protected final static int MSG_FAIL_TO_GET_PLAYLINK     = 102;
        protected final static int MSG_FAIL_TO_GET_STREAM		= 103;
        protected final static int MSG_FAIL_TO_GET_ALBUM_INFO	= 104;
        protected final static int MSG_INVALID_FT               = 105;

        private static final int MSG_UPDATE_PLAY_INFO 			= 201;
        private static final int MSG_UPDATE_RENDER_INFO			= 202;

        private static final int MSG_RESTART_PLAYER             = 301;

        protected static final int MSG_SHEDULE                  = 400;

        public MainHandler(PlaySegFileActivity activity) {
            mWeakActivity = new WeakReference<PlaySegFileActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaySegFileActivity activity = mWeakActivity.get();
            if (activity == null) {
                LogUtil.debug(TAG, "Got message for dead activity");
                return;
            }

            switch(msg.what) {
                case MSG_SHEDULE:
                    activity.onShedule();
                    break;
                case MSG_HIDE_HOOD:
                    activity.mHoodLayout.setVisibility(View.GONE);
                    break;
                case MSG_PLAY_NEXT_EPISODE:
                    activity.setupMediaPlayer();
                    break;
                case MSG_SHOW_MEDIA_CONTROLLER:
                    break;
                case MSG_INVALID_EPISODE_INDEX:
                    Toast.makeText(activity, "invalid episode", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_INVALID_FT:
                    Toast.makeText(activity, "invalid ft", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_FAIL_TO_GET_ALBUM_INFO:
                    Toast.makeText(activity, "failed to get album info", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_FAIL_TO_GET_PLAYLINK:
                    Toast.makeText(activity, "failed to get playlink", Toast.LENGTH_SHORT).show();
                    activity.finish();
                    break;
                case MSG_FAIL_TO_GET_STREAM:
                    Toast.makeText(activity, "failed to get stream", Toast.LENGTH_SHORT).show();
                    activity.finish();
                    break;
                case MSG_UPDATE_PLAY_INFO:
                case MSG_UPDATE_RENDER_INFO:
                    activity.mTvInfo.setText(String.format(Locale.US,
                            "%02d|%03d v-a: %+04d\n" +
                            "dec/render %d(%d)/%d(%d) fps/msec\nbitrate %d kbps",
                            activity.render_frame_num % 25, activity.decode_drop_frame % 1000, activity.av_latency_msec,
                            activity.decode_fps, activity.decode_avg_msec, activity.render_fps, activity.render_avg_msec,
                            activity.video_bitrate));
                    break;
                case MSG_RESTART_PLAYER:
                    activity.setupMediaPlayer();
                    break;
                default:
                    break;
            }
        }
    }
	
	boolean setupMediaPlayer() {
		if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                LogUtil.error(TAG, "failed to stop player: " + e.toString());
            }
			mPlayer.release();
			mPlayer = null;
		}

        mTvTitle.setText(mTitle);

        String decodemode_str;
        if (mPlayerImpl == 1)
            decodemode_str = "SYSTEM";
        else if (mPlayerImpl == 2)
            decodemode_str = "XOPlayer";
        else if (mPlayerImpl == 3)
            decodemode_str = "FFPlayer";
        else
            decodemode_str = "Unknown(" + mPlayerImpl + ")";
        Toast.makeText(this, String.format(Locale.US,
                "ready to play video: %s (ft %d), decode_mode: %s",
                mTitle, mFt, decodemode_str),
				Toast.LENGTH_SHORT).show();
		
		mPlayer = new FragmentMp4MediaPlayerV2(mPlayerImpl);
		mPlayer.reset();
		
		mPlayer.setDisplay(mHolder);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setScreenOnWhilePlaying(true);
		
		mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdate);
		mPlayer.setOnInfoListener(mOnInfoListener);
		mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
		mPlayer.setOnPreparedListener(mOnPreparedListener);
		mPlayer.setOnErrorListener(mOnErrorListener);
		mPlayer.setOnCompletionListener(mOnCompletionListener);
		
		boolean success = false;
		try {
			mPlayer.setDataSource(m_playlink_list, m_duration_list);
			success = true;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!success)
			return false;

        mIsBuffering = true;
        mPrepared = false;
        mBufferingProgressBar.setVisibility(View.VISIBLE);

        mPlayer.prepareAsync();
		return true;
	}

	protected void buildPlaylinkList() {
		m_playlink_list.clear();
		m_duration_list.clear();
		
		StringTokenizer st;
		int i=0;
		
		st = new StringTokenizer(mUrlListStr, ",", false);
		while (st.hasMoreElements()) {
			String url = st.nextToken();
            LogUtil.info(TAG, String.format(Locale.US,
                    "Java: segment #%d url: %s", i++, url));
			m_playlink_list.add(url);
		}
		
		st = new StringTokenizer(mDurationListStr, ",", false);
		i=0;
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00")); // fix 08:00:01 322 bug
        int total_msec = 0;
		while (st.hasMoreElements()) {
			String seg_duration = st.nextToken();
			int duration_msec = (int)(Double.valueOf(seg_duration) * 1000.0f);
            total_msec += duration_msec;
            String str_duration = formatter.format(total_msec);
			m_duration_list.add(duration_msec);

            LogUtil.info(TAG, String.format(Locale.US,
                    "Java: segment #%d duration: %s(%s)",
                    i++, seg_duration, str_duration));
		}
	}
	
	private class MyMediaPlayerControl implements MediaPlayerControl {

		@Override
		public boolean canPause() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean canSeekBackward() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean canSeekForward() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public int getBufferPercentage() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getCurrentPosition() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return 0;
			
			return mPlayer.getCurrentPosition();
		}

		@Override
		public int getDuration() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return 0;
			
			return mPlayer.getDuration();
		}

		@Override
		public boolean isPlaying() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return false;
			
			return mPlayer.isPlaying();
		}

		@Override
		public void pause() {
			// TODO Auto-generated method stub
			if (mPlayer != null)
				mPlayer.pause();
		}

		@Override
		public void seekTo(int msec) {
			// TODO Auto-generated method stub
            LogUtil.info(TAG, "Java: seekTo() " + msec + " msec");

			if (mPlayer != null)
				mPlayer.seekTo(msec);
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
			if (mPlayer != null)
				mPlayer.start();
		}
		
	}
}
