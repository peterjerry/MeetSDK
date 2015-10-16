package com.pplive.simpleplayer;

import java.io.IOException;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.player.MediaPlayer.OnCompletionListener;
import android.pplive.media.player.MediaPlayer.OnErrorListener;
import android.pplive.media.player.MediaPlayer.OnInfoListener;
import android.pplive.media.player.MediaPlayer.OnPreparedListener;
import android.pplive.media.player.MediaPlayer.OnVideoSizeChangedListener;
import android.pplive.media.player.MeetVideoView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class ViewPlayerActivity extends Activity implements  
	OnPreparedListener, OnVideoSizeChangedListener, OnCompletionListener, OnErrorListener, OnInfoListener
{
	private final static String TAG = "ViewPlayerActivity";
	private final static String PLAY_URL = "http://data.vod.itc.cn/?new=/116/182/JsRP1MCXQnScuAJY919BGO.mp4&vid=2149117&ch=tv&cateCode=100;100105;100106;100108;100111&plat=6&mkey=OBka7_GT0lXxy1nJCASOmFKQzS-hFA58&prod=app";
	
	private MeetVideoView mVideoView;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meetview);
        
        mVideoView = (MeetVideoView) findViewById(R.id.surface_view);
        start_videoview();
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	
    	mVideoView.stopPlayback();
    }

    void start_videoview() {
    	MeetSDK.initSDK(this);
    	
    	mVideoView.setDecodeMode(DecodeMode.SW);
    	mVideoView.setVideoURI(Uri.parse(PLAY_URL));
    	mVideoView.setOnCompletionListener(this);
    	mVideoView.setOnErrorListener(this);
    	mVideoView.setOnInfoListener(this);
    	mVideoView.setOnPreparedListener(this);
    	
    	mVideoView.start();
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
		
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		
	}
}
