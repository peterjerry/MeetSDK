package com.gotye.simpleplayer;

import android.net.Uri;
import android.os.Bundle;
import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetsdk.player.MediaPlayer;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.player.MediaPlayer.OnCompletionListener;
import com.gotye.meetsdk.player.MediaPlayer.OnErrorListener;
import com.gotye.meetsdk.player.MediaPlayer.OnInfoListener;
import com.gotye.meetsdk.player.MediaPlayer.OnPreparedListener;
import com.gotye.meetsdk.player.MediaPlayer.OnVideoSizeChangedListener;
import com.gotye.meetsdk.player.MeetVideoView;

import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class ViewPlayerActivity extends AppCompatActivity implements
	OnPreparedListener, OnVideoSizeChangedListener, OnCompletionListener, OnErrorListener, OnInfoListener
{
	private final static String TAG = "ViewPlayerActivity";
    private final static String PLAY_URL = "http://data.vod.itc.cn/" +
            "?new=/205/151/pjpS1hRsRwWnl27JeDP1lC.mp4" +
            "&vid=2869033&ch=tv&cateCode=101;101100;101104;101106" +
            "&plat=6&mkey=91UZYM8cJOOpvQDw2wiFcHO57mZgUfFQ&prod=app";

	private MeetVideoView mVideoView;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.meetview);
		getSupportActionBar().hide();
        
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
