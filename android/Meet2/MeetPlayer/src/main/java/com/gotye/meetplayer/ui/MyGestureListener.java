package com.gotye.meetplayer.ui;

import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import com.gotye.meetsdk.player.MediaPlayer;

public class MyGestureListener extends SimpleOnGestureListener {
	private MediaPlayer mPlayer;
	
	private static final String TAG = "MyGestureListener";

	
	public MyGestureListener(MediaPlayer player) {
		mPlayer = player;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Log.i(TAG, "onDoubleTap : " + e.getAction());
		return super.onDoubleTap(e);
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		Log.i(TAG, "onDoubleTapEvent : " + e.getAction());
		return super.onDoubleTapEvent(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		Log.i(TAG, "onDown : " + e.getAction());
		return super.onDown(e);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Log.i(TAG, "onFling e1 : " + e1.getAction() + ", e2 : " + e2.getAction() + ", distanceX : " + velocityX + ", distanceY : " + velocityY);
		return super.onFling(e1, e2, velocityX, velocityY);
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Log.i(TAG, "onLongPress : " + e.getAction());
		super.onLongPress(e);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.i(TAG, "onScroll e1 : " + e1.getAction() + ", e2 : " + e2.getAction() + ", distanceX : " + distanceX + ", distanceY : " + distanceY);
		int duration = mPlayer.getDuration();
		int seek_pos = mPlayer.getCurrentPosition() + (int)-distanceX * 100;
		if (seek_pos >= 0 && seek_pos <= duration);
			mPlayer.seekTo(seek_pos);
			
		return super.onScroll(e1, e2, distanceX, distanceY);
	}

	@Override
	public void onShowPress(MotionEvent e) {
		Log.i(TAG, "onShowPress : " + e.getAction());
		super.onShowPress(e);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		Log.i(TAG, "onSingleTapConfirmed : " + e.getAction());
		return super.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		Log.i(TAG, "onSingleTapUp : " + e.getAction());
		return super.onSingleTapUp(e);
	}
}
