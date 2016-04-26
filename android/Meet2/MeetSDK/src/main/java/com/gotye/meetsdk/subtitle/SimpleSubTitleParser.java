package com.gotye.meetsdk.subtitle;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.gotye.meetsdk.util.LogUtils;

public class SimpleSubTitleParser implements SubTitleParser, Handler.Callback {
	private static final int WHAT_LOAD_SUBTILTE = 9001;
	private static final int WHAT_SEEKTO 		= 9002;
	private static final int WHAT_CLOSE 		= 9003;

	private long mNativeContext;
	private static String slibPath = "";
	private static boolean slibLoaded = false;
	public static boolean initParser(String path) {
		LogUtils.info("initParser()");
		
		if (!slibLoaded) {
			if (path != null) {
				slibPath = path;
				
				if (!slibPath.equals("") && !slibPath.endsWith("/"))
					slibPath += "/";
			}
			
			try {
				String loader = "subtitle-jni";
				
				if (slibPath != null && !slibPath.equals("")) {
					String full_name;
					full_name = slibPath + "lib" + loader + ".so";
					LogUtils.info("System.load() try load: " + full_name);
					System.load(full_name);
					LogUtils.info("System.load() " + full_name + " done!");
				}
				else {
					LogUtils.info("System.loadLibrary() try load subtitle");
					System.loadLibrary(loader);
					LogUtils.info("System.loadLibrary() subtitle loaded");
				}
				
				nativeInit();
				
				slibLoaded = true;
				
			}
			catch (SecurityException e) {
				e.printStackTrace();
				LogUtils.error("subtitle SecurityException: " + e.toString());
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
				LogUtils.error("subtitle UnsatisfiedLinkError: " + e.toString());
			} catch (IllegalStateException e) {
				e.printStackTrace();
				LogUtils.error("subtitle IllegalStateException: " + e.toString());
			}
		}
		
		return slibLoaded;
	}
	
	private static boolean isLoadLibSuccess() {
		return slibLoaded;
	}
	
	private SubTitleSegment mSegment;

	private Handler mInnerHandler;
	
	public SimpleSubTitleParser() {
		if (!isLoadLibSuccess()) {
			throw new IllegalStateException("Load Lib failed");
		}
		
		nativeSetup();
		
		HandlerThread ht = new HandlerThread("SampleSubTitleParser");
		ht.start();
		mInnerHandler = new Handler(this);
		mSegment = new SubTitleSegment();
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		boolean ret = true;
		switch (msg.what) {
			case WHAT_LOAD_SUBTILTE: {
				nativeLoadSubtitle(mFilePath, false /* isMediaFile */);
				break;
			}
			case WHAT_SEEKTO: {
				long msec = msg.getData().getLong("seek_to_msec");
				nativeSeekTo(msec);
				break;
			}
			case WHAT_CLOSE: {
				nativeClose();
				break;
			}
			default: {
				ret = false;
				break;
			}
		}
		return ret;
	}

	@Override
	public void close() {
		mInnerHandler.sendEmptyMessage(WHAT_CLOSE);
	}
	
	@Override
	public SubTitleSegment next() {
		boolean ret = nativeNext(mSegment);
		return ret ? mSegment : null;
	}
	
	@Override
	public void prepareAsync() {
		mInnerHandler.sendEmptyMessage(WHAT_LOAD_SUBTILTE);
	}

	@Override
	public void seekTo(long msec) {
		Message msg = mInnerHandler.obtainMessage(WHAT_SEEKTO);
		Bundle data = new Bundle();
		data.putLong("seek_to_msec", msec);
		msg.setData(data);
		mInnerHandler.sendMessage(msg);
	}
	
	private String mFilePath = null;
	
	@Override
	public void setDataSource(String filePath) {
	    LogUtils.info(filePath);
		mFilePath = filePath;
	}
	
	private OnReadyListener mSubTitleCallback;
	
	@Override
	public void setListener(OnReadyListener callback) {
		mSubTitleCallback = callback;
	}

	private void onPrepared(boolean success, String msg) {
		if (mSubTitleCallback != null) {
			mSubTitleCallback.onPrepared(success, msg);
		} else {
		}
	}
	
	private void onSeekComplete() {
		if (mSubTitleCallback != null) {
			mSubTitleCallback.onSeekComplete();
		}
	}
	
	private static native void nativeInit();
	
	private native void nativeSetup();

	private native void nativeLoadSubtitle(String filePath, boolean isMediaFile);
	
	private native boolean nativeNext(SubTitleSegment segment);
	
	private native void nativeSeekTo(long msec);
	
	private native void nativeClose();
}
