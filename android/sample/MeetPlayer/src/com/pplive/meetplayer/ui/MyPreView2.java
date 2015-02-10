package com.pplive.meetplayer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.Gravity;
import android.view.GestureDetector; 
import android.widget.RelativeLayout;
import android.widget.MediaController;
import android.view.KeyEvent;
import android.view.MotionEvent;

import android.pplive.media.player.MediaPlayer;

/**
 * Displays a video file.  The VideoView class
 * can load images from various sources (such as resources or content
 * providers), takes care of computing its measurement from the video so that
 * it can be used in any layout manager, and provides various display options
 * such as scaling and tinting.
 */
public class MyPreView2 extends SurfaceView {
	private int mVideoWidth 			= 0;
	private int mVideoHeight			= 0;
	private int mLayoutWidth			= 0;
	private int mLayoutHeight			= 0;
	private MediaController mController;
	private MediaPlayer mPlayer;
	private GestureDetector mDetector;
	
	public MyPreView2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public MyPreView2(Context context, AttributeSet attr) {
		super(context, attr); 
		
		// TODO Auto-generated constructor stub
	}

	private static final String TAG = "MyPreView";
	
	public void SetVideoRes(int width, int height) {
		mVideoWidth 	= width;
		mVideoHeight	= height;
	}
	
	public void BindInstance(MediaController controller, MediaPlayer player) {
		mController = controller;
		mPlayer = player;
		
		mDetector = new GestureDetector(getContext(), new MyGestureListener(mPlayer));  
        setLongClickable(true);  
        this.setOnTouchListener(new OnTouchListener() {  
              
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                return mDetector.onTouchEvent(event);  
            }  
        });  
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x 	= e.getX();
		float y 	= e.getY();
		int mask 	= e.getActionMasked();
		Log.i(TAG, String.format("Java: MyPreview touch %d: %.3f %.3f", mask, x, y));
		if (MotionEvent.ACTION_UP == mask) {
			if(mController != null) {
        		if(!mController.isShowing()) {
        			mController.show(3000);
	    		}
			}
		}
		
		return true;
	}
	
}
