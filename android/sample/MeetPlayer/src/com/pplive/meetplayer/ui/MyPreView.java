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
public class MyPreView extends SurfaceView {
	private int mVideoWidth 			= 0;
	private int mVideoHeight			= 0;
	private int mLayoutWidth			= 0;
	private int mLayoutHeight			= 0;
	//private RelativeLayout mLayout 		= null;
	private MediaController mController;
	private MediaPlayer mPlayer;
	private GestureDetector mDetector;
	
	public MyPreView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public MyPreView(Context context, AttributeSet attr) {
		super(context, attr); 
		
		// TODO Auto-generated constructor stub
	}

	private static final String TAG = "MyPreView";
	
	public void SetVideoRes(int width, int height) {
		mVideoWidth 	= width;
		mVideoHeight	= height;
	}
	
	void BindInstance(MediaController controller, MediaPlayer player) {
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
		
		//return mDetector.onTouchEvent(event);
	}
	
	String get_desc(int spec) {
		String desc;
		
		switch(spec) {
		case MeasureSpec.UNSPECIFIED:
			desc = "UNSPECIFIED";
			break;
		case MeasureSpec.EXACTLY:
			desc = "EXACTLY";
			break;
		case MeasureSpec.AT_MOST:
			desc = "AT_MOST";
			break;
		default:
			desc = "unknown";
			break;
		}
		
		return desc;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.d(TAG, String.format("onMeasure(0): widthMeasureSpec: %s, heightMeasureSpec: %s", 
		//		get_desc(MeasureSpec.getMode(widthMeasureSpec)), get_desc(MeasureSpec.getMode(heightMeasureSpec))));
		
		int LayoutWidth = getDefaultSize(-1, widthMeasureSpec);
		int LayoutHeight = getDefaultSize(-1, heightMeasureSpec);
		
		//Log.d(TAG, String.format("onMeasure(0): LayoutWidth: %d, LayoutHeight: %d", LayoutWidth, LayoutHeight));
		int displaySize[] = mCurrentDisplayMode.getDisplaySize(LayoutWidth, LayoutHeight, mVideoWidth, mVideoHeight);
		
		int width = displaySize[0];
		int height = displaySize[1];
		
		//Log.d(TAG, String.format("onMeasure(1): width: %d; height: %d", width, height));
		
		if (width == 0 || height == 0) {
			width = widthMeasureSpec;
			height = heightMeasureSpec;
		}

		//Log.d(TAG, String.format("setMeasuredDimension: width: %d, height: %d", width, height));

		setMeasuredDimension(width, height);
	}

	private DisplayMode mCurrentDisplayMode = DisplayMode.FULL_SCREEN_BY_SCALE;
	private enum DisplayMode {
		ORIGINAL_SIZE {
			@Override
			public int[] getDisplaySize(int screenWidth, int screenHeight, int VideoWidth, int videoHeight) {
				
				return new int[] {VideoWidth, videoHeight};
			}
		},
		FULL_SCREEN {
			@Override
			public int[] getDisplaySize(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {
//				Log.d(TAG, "FULL_SCREEN");
//				Log.d(TAG, String.format("wWidth: %d; wHeight: %d; vWidth: %d; vHeight: %d", screenWidth, screenHeight, videoWidth, videoHeight));
				
				return new int[] {screenWidth, screenHeight};
			}
		},
		FULL_SCREEN_BY_SCALE {
			@Override
			public int[] getDisplaySize(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {
				//Log.i(TAG, "FULL_SCREEN_BY_SCALE");
				//Log.i(TAG, String.format("wWidth: %d; wHeight: %d; vWidth: %d; vHeight: %d", 
				//		screenWidth, screenHeight, videoWidth, videoHeight));
				
				float widthScale = (float)(screenWidth) / videoWidth;
				float heightScale = (float)(screenHeight) / videoHeight;
				
				float scale = (widthScale < heightScale) ? widthScale : heightScale;
				
				//Log.i(TAG, String.format("scale: %f", scale));
				
				int width = (int)(videoWidth * scale);
				int height = (int)(videoHeight * scale);
				
				width = (width >= screenWidth) ? screenWidth : width;
				height = (height >= screenHeight) ? screenHeight : height;
				
				//Log.i(TAG, String.format("width: %d; height: %d", width, height));
				
				return new int[] {width, height};
			}
		};

		public abstract int[] getDisplaySize(int screenWidth, int screenHeight, int VideoWidth, int videoHeight);
		public final DisplayMode switchFullScreenMode() {
			int len = DisplayMode.values().length;
			int cur = this.ordinal();
			int next = (cur + 1) % len;
			return DisplayMode.values()[next];
		}
	};
}
