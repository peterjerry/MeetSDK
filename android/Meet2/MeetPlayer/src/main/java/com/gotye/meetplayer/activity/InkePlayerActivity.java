package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.util.httpUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.InkeAdapter;
import com.gotye.meetplayer.media.FragmentMp4MediaPlayerV2;
import com.gotye.meetplayer.ui.MyPreView2;
import com.gotye.meetplayer.util.Util;
import com.gotye.meetsdk.player.MediaPlayer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InkePlayerActivity extends AppCompatActivity
        implements SurfaceHolder.Callback {

    private final static String TAG = "InkePlayerActivity";

    private MediaPlayer mPlayer;
    private MyPreView2 mView;
    private SurfaceHolder mHolder;
    private ProgressBar mBufferingProgressBar;
    private TextView mTvInfo;

    private String mPlayUrl;
    private int mPlayerImpl;
    private int mVideoWidth, mVideoHeight;
    private boolean mIsBuffering = false;
    private long mStartMsec;

    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnErrorListener	 mOnErrorListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inke_player);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent intent = getIntent();
        mPlayUrl = intent.getStringExtra("play_url");

        this.mView = (MyPreView2)this.findViewById(R.id.player_view);
        this.mBufferingProgressBar = (ProgressBar)this.findViewById(R.id.progressbar_buffering);
        this.mTvInfo = (TextView)this.findViewById(R.id.tv_info);

        Util.initMeetSDK(this);

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
        if (mPlayerImpl == 0)
            mPlayerImpl = 2;

        SurfaceHolder holder = mView.getHolder();
        if (mPlayerImpl == 3) {
            holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
            holder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
        }
        holder.addCallback(this);

        mOnInfoListener = new MediaPlayer.OnInfoListener() {

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                // TODO Auto-generated method stub
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
                else if (what == MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME) {
                    LogUtil.info(TAG, String.format(Locale.US,
                            "Java: onInfo MEDIA_INFO_TEST_DROP_FRAME %d msec", extra));
                }

                return true;
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

                mIsBuffering = false;
                mBufferingProgressBar.setVisibility(View.GONE);

                mp.start();

                long load_msec = System.currentTimeMillis() - mStartMsec;
                Toast.makeText(InkePlayerActivity.this,
                        String.format(Locale.US, "加载时间: %d msec", load_msec),
                        Toast.LENGTH_SHORT).show();
            }
        };

        mOnErrorListener = new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int error, int extra) {
                // TODO Auto-generated method stub
                LogUtil.error(TAG, "Java: onError what " + error + " , extra " + extra);

                mIsBuffering = false;
                mBufferingProgressBar.setVisibility(View.GONE);

                Toast.makeText(InkePlayerActivity.this, "Error " + error + " , extra " + extra,
                        Toast.LENGTH_SHORT).show();
                finish();

                return true;
            }
        };

        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                finish();
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mPlayer.release();
        }
    }

    private boolean SetupPlayer() {
        mStartMsec = System.currentTimeMillis();

        MediaPlayer.DecodeMode mode;
        if (mPlayerImpl == 2)
            mode = MediaPlayer.DecodeMode.HW_XOPLAYER;
        else
            mode = MediaPlayer.DecodeMode.SW;

        mPlayer = new MediaPlayer(mode);
        mPlayer.reset();

        mPlayer.setDisplay(mHolder);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setScreenOnWhilePlaying(true);

        mPlayer.setOnInfoListener(mOnInfoListener);
        mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mPlayer.setOnPreparedListener(mOnPreparedListener);
        mPlayer.setOnErrorListener(mOnErrorListener);
        mPlayer.setOnCompletionListener(mOnCompletionListener);

        try {
            mPlayer.setDataSource(mPlayUrl);
            mIsBuffering = true;
            mBufferingProgressBar.setVisibility(View.VISIBLE);

            mPlayer.prepareAsync();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;

        SetupPlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
