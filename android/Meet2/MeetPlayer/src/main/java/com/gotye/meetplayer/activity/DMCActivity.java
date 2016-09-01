package com.gotye.meetplayer.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.IDlnaCallback;
import com.gotye.meetplayer.util.ImgUtil;
import com.pplive.dlna.DLNASdk;
import com.pplive.dlna.DLNASdkDMSItemInfo;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class DMCActivity extends AppCompatActivity {

    private TextView mTitle;
    private SeekBar mProgressBar;
    private ImageButton mBtnPause;
    private TextView mTvStartTime, mTvEndTime;
    private ImageView mImage;

    private final static int PROGRESS_RANGE = 1000;

    private DLNASdk mDLNAsdk;
    private DLNASdk.DLNASdkInterface mDLNAcallback;

    private String mDMRuuid;
    private String mPushUrl;
    private long mDuration = -1;
    private boolean mIsPlaying = false;
    private boolean mbPaused = false;
    private boolean mIsSeeking = false;

    private boolean mbActivityRunning = false;

    private MainHandler mHandler;
    private ProgressDialog mProgressDlg;

    private final static String TAG = "DMCActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmc);

        mTitle = (TextView)this.findViewById(R.id.info_title);
        mProgressBar = (SeekBar)this.findViewById(R.id.seek);
        mBtnPause = (ImageButton)this.findViewById(R.id.play_control_play_pause);
        mTvStartTime = (TextView)this.findViewById(R.id.start_time);
        mTvEndTime = (TextView)this.findViewById(R.id.end_time);
        mImage = (ImageView)this.findViewById(R.id.image);

        mBtnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlaying) {
                    if (mbPaused) {
                        mDLNAsdk.Play(mDMRuuid);
                        mBtnPause.setImageResource(R.drawable.pause);
                    }
                    else {
                        mDLNAsdk.Pause(mDMRuuid);
                        mBtnPause.setImageResource(R.drawable.play);
                    }

                    mbPaused = !mbPaused;
                }
                else {
                    Toast.makeText(DMCActivity.this, "DMR未播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsSeeking = false;

                if (mDuration > 0) {
                    int progress = seekBar.getProgress();
                    int sec = (int)(mDuration * progress / PROGRESS_RANGE);
                    LogUtil.info(TAG, "seek to: " + sec);

                    mDLNAsdk.Seek(mDMRuuid, sec);
                }
            }
        });

        mDLNAsdk = DLNASdk.getInstance();

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        mDMRuuid = intent.getStringExtra("dmr_uuid");
        mPushUrl = intent.getStringExtra("push_url");
        LogUtil.info(TAG, "dmr_uuid: " + mDMRuuid);

        mTitle.setText(title);

        pushToDMR();

        mHandler = new MainHandler(this);

        mProgressDlg = new ProgressDialog(DMCActivity.this);
        mProgressDlg.setTitle("DLNA 投屏");
        mProgressDlg.setMessage("设备连接中...");
        mProgressDlg.setCancelable(true);
        mProgressDlg.show();

        if (mPushUrl.toLowerCase().endsWith(".jpg") ||
                mPushUrl.toLowerCase().endsWith(".bmp"))
        {
            loadPicture();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mbActivityRunning = false;

        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mbActivityRunning = true;

        if (mIsPlaying) {
            mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PROGRESS);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsPlaying) {
            mDLNAsdk.Stop(mDMRuuid);
        }

        super.onBackPressed();
    }

    private void loadPicture() {
        Bitmap bmp = ImgUtil.getLocalBitmap(mPushUrl);
        if (bmp != null) {
            int new_w = 512;
            int new_h = (int) ( bmp.getHeight() * (512.0 / bmp.getWidth()) );
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, new_w, new_h, true);
            mImage.setImageBitmap(scaled);
            bmp.recycle();
        }
        else {
            LogUtil.error(TAG, "failed to load picture: " + mPushUrl);
            Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private static class MainHandler extends Handler {
        private WeakReference<DMCActivity> mWeakActivity;

        private static final int MSG_UPDATE_PROGRESS = 1001;

        public MainHandler(DMCActivity activity) {
            mWeakActivity = new WeakReference<DMCActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DMCActivity activity = mWeakActivity.get();
            if (activity == null) {
                LogUtil.debug(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    if (activity.mIsPlaying) {
                        if (activity.mDuration == -1) {
                            long duration = activity.mDLNAsdk.GetTotalTime(activity.mDMRuuid);
                            LogUtil.info(TAG, "get duration: " + duration);
                            if (duration > 0) {
                                activity.mDuration = duration;

                                activity.mProgressBar.setMax(PROGRESS_RANGE);
                                activity.mProgressBar.setProgress(0);
                                activity.mProgressBar.setEnabled(true);

                                activity.mTvEndTime.setText(getTimeString(duration));
                            }
                        }

                        long pos = activity.mDLNAsdk.GetPosition(activity.mDMRuuid);
                        LogUtil.info(TAG, "update: " + pos);

                        activity.mTvStartTime.setText(getTimeString(pos));

                        if (activity.mDuration > 0) {
                            int progress = (int) (pos * PROGRESS_RANGE / activity.mDuration);
                            activity.mProgressBar.setProgress(progress);
                        }

                        sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 500);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void pushToDMR() {
        mDLNAcallback = new DLNASdk.DLNASdkInterface() {
            @Override
            public void OnDeviceAdded(String uuid, String firendname, String logourl, int devicetype) {

            }

            @Override
            public void OnDeviceRemoved(String uuid, int devicetype) {

            }

            @Override
            public void OnLogPrintf(String msg) {

            }

            @Override
            public boolean OnConnect(String uuid, String requestName) {
                return false;
            }

            @Override
            public void OnConnectCallback(String uuid, int state) {

            }

            @Override
            public void OnDisConnect(String uuid) {

            }

            @Override
            public void OnDisConnectCallback(String uuid, boolean isTimeout) {

            }

            @Override
            public void OnRemoveTransportFile(String uuid, String transportuuid) {

            }

            @Override
            public void OnRemoveTransportFileCallback(String uuid, String transportuuid, boolean isTimeout) {

            }

            @Override
            public void OnAddTransportFile(String uuid, String transportuuid, String fileurl, String filename, String thumburl) {

            }

            @Override
            public void OnAddTransportFileCallback(String uuid, String transportuuid, int state) {

            }

            @Override
            public int OnSetURI(String url, String urltitle, String remoteip, int mediatype) {
                LogUtil.info(TAG, String.format(Locale.US,
                        "OnSetURI: url %s, title %s, remoteip %s, type: %d",
                        url, urltitle, remoteip, mediatype));

                mDLNAsdk.Play(mDMRuuid);
                return 0;
            }

            @Override
            public void OnPlay() {
                LogUtil.info(TAG, "OnPlay");
            }

            @Override
            public void OnPause() {
                LogUtil.info(TAG, "OnPause");
            }

            @Override
            public void OnStop() {
                LogUtil.info(TAG, "OnStop");
            }

            @Override
            public void OnSeek(long position) {
                LogUtil.info(TAG, "OnSeek: pos: " + position);

                if (!mIsSeeking && mDuration > 0) {
                    int progress = (int) (position * PROGRESS_RANGE / mDuration);
                    mProgressBar.setProgress(progress);
                }
            }

            @Override
            public void OnSetVolume(long volume) {

            }

            @Override
            public void OnSetMute(boolean mute) {

            }

            @Override
            public void OnVolumeChanged(String uuid, long lVolume) {
                LogUtil.info(TAG, "OnVolumeChanged: " + uuid + " , volume: " + lVolume);
            }

            @Override
            public void OnMuteChanged(String uuid, boolean bMute) {

            }

            @Override
            public void OnPlayStateChanged(String uuid, String state) {
                LogUtil.info(TAG, "OnPlayStateChanged: " + uuid + " , state: " + state);

                // c831222f-b617-4050-885a-0eff58629ed2 , state: PLAYING STOPPED
                if (state.equals("PLAYING")) {
                    mIsPlaying = true;

                    mHandler.sendEmptyMessage(MainHandler.MSG_UPDATE_PROGRESS);

                    if (mProgressDlg != null)
                        mProgressDlg.dismiss();
                }
                else if (state.equals("STOPPED")) {
                    mIsPlaying = false;
                }

            }

            @Override
            public void OnPlayUrlChanged(String uuid, String url) {
                LogUtil.info(TAG, "OnPlayUrlChanged: " + uuid + " , url: " + url);
            }

            @Override
            public void OnContainerChanged(String uuid, String item_id, String update_id) {

            }

            @Override
            public void OnGetCaps(String uuid, String caps) {

            }

            @Override
            public void OnSetUrl(String uuid, long error) {

            }

            @Override
            public void OnBrowse(boolean success, String uuid, String objectid, long count, long total, DLNASdkDMSItemInfo[] filelists) {

            }
        };

        IDlnaCallback callback = IDlnaCallback.getInstance();
        callback.setCallback(mDLNAcallback);

        mDLNAsdk.SetURI(mDMRuuid, mPushUrl);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mDLNAsdk.Play(mDMRuuid);
    }

    private static String getTimeString(long timeInSec) {
        return String.format(Locale.US,
                "%02d:%02d:%02d",
                timeInSec / 3600, (timeInSec % 3600) / 60, timeInSec % 60);
    }
}
