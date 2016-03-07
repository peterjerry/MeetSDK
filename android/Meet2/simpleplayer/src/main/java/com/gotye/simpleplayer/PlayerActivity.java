package com.gotye.simpleplayer;

import java.io.IOException;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class PlayerActivity extends AppCompatActivity implements Callback,
	OnPreparedListener, OnVideoSizeChangedListener, OnCompletionListener, OnErrorListener, OnInfoListener
{
	private final static String TAG = "PlayerActivity";
    private final static String PLAY_URL = "http://data.vod.itc.cn/" +
            "?new=/205/151/pjpS1hRsRwWnl27JeDP1lC.mp4" +
            "&vid=2869033&ch=tv&cateCode=101;101100;101104;101106" +
            "&plat=6&mkey=91UZYM8cJOOpvQDw2wiFcHO57mZgUfFQ&prod=app";
	
	private DecodeMode mMode = DecodeMode.SW;
	private MediaPlayer mPlayer;
	private SurfaceView mPreview;
	private SurfaceHolder mHolder;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.player);
		getSupportActionBar().hide();

        mPreview = (SurfaceView)this.findViewById(R.id.preview);
        
		if (DecodeMode.HW_SYSTEM == mMode) {
			mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		else if (DecodeMode.SW == mMode){
			mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
			mPreview.getHolder().setFormat(PixelFormat.RGBX_8888);
		} 
       
        mPreview.getHolder().addCallback(this);
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
    	if (mPlayer != null) 		
    		return mGestureDetector.onTouchEvent(event);
    	
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

		mPlayer.setDisplay(mPreview.getHolder());
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setScreenOnWhilePlaying(true);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnVideoSizeChangedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnInfoListener(this);
		
		boolean succeed = true;
		try {
			mPlayer.setDataSource(PLAY_URL);
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
			e.printStackTrace();
			Log.e(TAG, "Java: prepareAsync() exception: " + e.getMessage());
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
		start_player();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
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
		mp.start();
	}
}
