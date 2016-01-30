package com.gotye.meetsdk.player;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.gotye.meetsdk.util.LogUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Michael.Ma on 2016/1/28.
 */
public class YUVRender implements GLSurfaceView.Renderer {
    private static final String TAG = "YUVRender";

    private Context mContext;
    private long mNativeContext = -1;
    private MediaPlayer mPlayer;
    private boolean mOpened = false;

    public YUVRender(Context context) {
        mContext = context;
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mPlayer = mp;
        openVideo();
    }

    public void requestRender() {
        this.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mNativeContext = nativeInit(width, height);
        openVideo();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        nativeRender(mNativeContext);
    }

    private void openVideo() {
        if (mOpened)
            return;

        if (mNativeContext != -1 && mPlayer != null) {
            LogUtils.info("setNativeSurface " + mNativeContext);
            mPlayer.setNativeSurface(mNativeContext);
            mPlayer.setScreenOnWhilePlaying(true);
            mPlayer.prepareAsync();
            mOpened = true;
        }
    }

    public static native long nativeInit(int width, int height);

    public static native void nativeRender(long context);
}


