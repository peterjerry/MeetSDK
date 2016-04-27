package com.gotye.simpleplayer;

import java.lang.ref.WeakReference;
import java.util.Locale;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetsdk.player.MediaPlayer;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.MediaPlayer.OnCompletionListener;
import com.gotye.meetsdk.player.MediaPlayer.OnErrorListener;
import com.gotye.meetsdk.player.MediaPlayer.OnInfoListener;
import com.gotye.meetsdk.player.MediaPlayer.OnPreparedListener;
import com.gotye.meetsdk.player.MediaPlayer.OnVideoSizeChangedListener;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends AppCompatActivity implements Callback,
	OnPreparedListener, OnVideoSizeChangedListener, OnCompletionListener, OnErrorListener, OnInfoListener
{
	private final static String TAG = "PlayerActivity";
    /*private final static String PLAY_URL = "http://data.vod.itc.cn/" +
            "?new=/205/151/pjpS1hRsRwWnl27JeDP1lC.mp4" +
            "&vid=2869033&ch=tv&cateCode=101;101100;101104;101106" +
            "&plat=6&mkey=91UZYM8cJOOpvQDw2wiFcHO57mZgUfFQ&prod=app";*/

    private final static String PLAY_URL = "http://42.62.105.235" +
            "/test/media/transformer2-H264-1080P_2_chn_aac.mp4";
	
	private DecodeMode mMode = DecodeMode.HW_XOPLAYER;
	private MediaPlayer mPlayer;
    private MediaController mMediaController;
	private SurfaceView mPreview;
	private SurfaceHolder mHolder;
    private ProgressBar mBufferingProgressBar;
    private boolean mIsBuffering = false;
    private TextView mTvInfo;

    private MainHandler mHandler;

    // stat
    private int decode_fps					= 0;
    private int render_fps 					= 0;
    private int decode_avg_msec 			= 0;
    private int render_avg_msec 			= 0;
    private int render_frame_num			= 0;
    private int decode_drop_frame			= 0;
    private int av_latency_msec				= 0;
    private int video_bitrate				= 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.player);
		if (getSupportActionBar() != null)
			getSupportActionBar().hide();

        this.mPreview = (SurfaceView)this.findViewById(R.id.preview);
        this.mBufferingProgressBar = (ProgressBar) this.findViewById(R.id.progressbar_buffering);
        this.mTvInfo = (TextView)this.findViewById(R.id.tv_info);
        
		if (DecodeMode.HW_SYSTEM == mMode) {
			mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		else if (DecodeMode.SW == mMode){
			mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
			mPreview.getHolder().setFormat(PixelFormat.RGBX_8888);
		} 
       
        mPreview.getHolder().addCallback(this);

        mHandler = new MainHandler(this);
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
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	// TODO Auto-generated method stub
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mMediaController.isShowing())
                mMediaController.hide();
            else if (mPlayer != null && mPlayer.isPlaying())
                mMediaController.show();
        }
    	
    	return super.onTouchEvent(event);
    }
    
    private GestureDetector mGestureDetector = 
		new GestureDetector(getApplication(), new GestureDetector.SimpleOnGestureListener() {
			
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                    Toast.makeText(PlayerActivity.this, "player paused", Toast.LENGTH_SHORT).show();
                }
                else {
                    mPlayer.start();
                    Toast.makeText(PlayerActivity.this, "player resumed", Toast.LENGTH_SHORT).show();
                }
                return true;
			};
			
			@Override
			public boolean onDoubleTap(MotionEvent event) {
				Intent intent = new Intent(PlayerActivity.this, ViewPlayerActivity.class);
				startActivity(intent);
				finish();
				
				Toast.makeText(PlayerActivity.this, "go to meetvideoview test", Toast.LENGTH_SHORT).show();
				return true;
			}
	});			
	
	private void start_player() {	
		MeetSDK.initSDK(this);
		
		mPlayer = new MediaPlayer(mMode);
		
		// fix Mediaplayer setVideoSurfaceTexture failed: -17
		mPlayer.setDisplay(null);
		mPlayer.reset();

        mPlayer.setLooping(true);
		mPlayer.setDisplay(mPreview.getHolder());
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setScreenOnWhilePlaying(true);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnVideoSizeChangedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnInfoListener(this);

        try {
            mPlayer.setDataSource(PLAY_URL);
            mPlayer.prepareAsync();
            mBufferingProgressBar.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Java: open player exception: " + e.getMessage());
            Toast.makeText(this, "Java: failed to play: " + PLAY_URL, Toast.LENGTH_SHORT).show();
        }
	}

	@Override
	public void surfaceChanged(SurfaceHolder sh, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		mHolder = sh;
        mMediaController = new MediaController(this);
		start_player();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
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
        else if(MediaPlayer.MEDIA_INFO_TEST_DECODE_AVG_MSEC == what) {
            decode_avg_msec = extra;
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_AVG_MSEC == what) {
            render_avg_msec = extra;
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS == what) {
            decode_fps = extra;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_PLAY_INFO));
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS == what) {
            render_fps = extra;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_PLAY_INFO));
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME == what) {
            render_frame_num = extra;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_RENDER_INFO));
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC == what) {
            av_latency_msec = extra;
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME == what) {
            decode_drop_frame++;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_RENDER_INFO));
        }
        else if(MediaPlayer.MEDIA_INFO_TEST_MEDIA_BITRATE == what) {
            video_bitrate = extra;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_PLAY_INFO));
        }
        else if(android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {

        }
        else if(MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING == what) {
            av_latency_msec = extra;

            decode_fps = render_fps = 0;
            decode_drop_frame = 0;
            video_bitrate = 0;
            mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_UPDATE_PLAY_INFO));
        }

		return false;
	}

    private static class MainHandler extends Handler {
        private WeakReference<PlayerActivity> mWeakActivity;

        private static final int MSG_UPDATE_PLAY_INFO       = 1002;
        private static final int MSG_UPDATE_RENDER_INFO     = 1003;

        public MainHandler(PlayerActivity activity) {
            mWeakActivity = new WeakReference<PlayerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PlayerActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.e(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_UPDATE_PLAY_INFO:
                case MSG_UPDATE_RENDER_INFO:
                    activity.mTvInfo.setText(String.format(Locale.US,
                            "%02d|%03d v-a: %+04d\n" +
                            "dec/render %d(%d)/%d(%d) fps/msec\n" +
                            "bitrate %d kbps",
                            activity.render_frame_num % 25,
                            activity.decode_drop_frame % 1000, activity.av_latency_msec,
                            activity.decode_fps, activity.decode_avg_msec,
                            activity.render_fps, activity.render_avg_msec,
                            activity.video_bitrate));
                    break;
                default:
                    break;
            }
        }
    }

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
		// TODO Auto-generated method stub
		mHolder.setFixedSize(w, h);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
        mBufferingProgressBar.setVisibility(View.GONE);

        mMediaController.setMediaPlayer(mControl);
        mMediaController.setAnchorView(mPreview);
        mMediaController.show();
		mp.start();
	}

    private MediaController.MediaPlayerControl mControl = new MediaController.MediaPlayerControl() {
        @Override
        public void start() {
            if (mPlayer != null)
                mPlayer.start();
        }

        @Override
        public void pause() {
            if (mPlayer != null)
                mPlayer.pause();
        }

        @Override
        public int getDuration() {
            if (mPlayer != null)
                return mPlayer.getDuration();

            return 0;
        }

        @Override
        public int getCurrentPosition() {
            if (mPlayer != null)
                return mPlayer.getCurrentPosition();

            return 0;
        }

        @Override
        public void seekTo(int pos) {
            if (mPlayer != null)
                mPlayer.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            if (mPlayer != null)
                return mPlayer.isPlaying();

            return false;
        }

        @Override
        public int getBufferPercentage() {
            return 0;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return 0;
        }
    };
}
