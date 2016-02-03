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
    private static final int INVALID_HANDLE = -1;

    private long mNativeContext = INVALID_HANDLE;
    private MediaPlayer mPlayer;
    private boolean mOpened = false;

    public YUVRender(GLSurfaceView glView) {
        mNativeContext = nativeInit(glView);
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mPlayer = mp;
        mOpened = false;
        openVideo();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.info("onSurfaceCreated()");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.info("onSurfaceChanged() " + width + " x " + height);
        nativeResize(mNativeContext, width, height);
        openVideo();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        nativeRender(mNativeContext);
    }

    private void openVideo() {
        if (mOpened)
            return;

        if (mNativeContext != INVALID_HANDLE && mPlayer != null) {
            LogUtils.info("setNativeSurface " + mNativeContext);
            mPlayer.setNativeSurface(mNativeContext);
            try {
                mPlayer.prepareAsync();
                mOpened = true;
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        else {
            LogUtils.error("failed to open video");
        }
    }

    public static native long nativeInit(GLSurfaceView ins);

    public static native void nativeResize(long context, int width, int height);

    public static native void nativeRender(long context);

    static {
        System.loadLibrary("meet");
    }
}


