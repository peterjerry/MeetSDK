package com.gotye.meetplayer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.gotye.meetsdk.player.MediaPlayer;

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
	
	// display mode
	public static final int SCREEN_FIT = 0; // 自适应
    public static final int SCREEN_STRETCH = 1; // 铺满屏幕 
    public static final int SCREEN_FILL = 2; // 放大裁切
    public static final int SCREEN_CENTER = 3; // 原始大小
    
    private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};
	
	private int mDisplayMode = SCREEN_FIT;
	
	public MyPreView2(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public MyPreView2(Context context, AttributeSet attr) {
		super(context, attr); 
		
		// TODO Auto-generated constructor stub
	}

	private static final String TAG = "MyPreView2";
	
	public void SetVideoRes(int width, int height) {
		mVideoWidth 	= width;
		mVideoHeight	= height;
	}
	
	public void switchDisplayMode() {
		mDisplayMode++;
		if (mDisplayMode > 3)
			mDisplayMode = 0;
	}
	
	public String getDisplayMode() {
		return mode_desc[mDisplayMode];
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.d(TAG, String.format("Java: onMeasure %d %d", widthMeasureSpec, heightMeasureSpec));
		
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            switch (mDisplayMode) {
            case SCREEN_CENTER:
                width = mVideoWidth;
                height = mVideoHeight;
                break;
            case SCREEN_FIT:
                if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                } else if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                }
            case SCREEN_FILL:
                if (mVideoWidth * height > width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height < width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                }
            case SCREEN_STRETCH:
            	// do nothing
                break;
            default:
                break;
            }
        }
        setMeasuredDimension(width, height);
    }
	
}
