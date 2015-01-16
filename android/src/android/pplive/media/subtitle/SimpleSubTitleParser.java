package android.pplive.media.subtitle;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.pplive.media.util.LogUtils;

public class SimpleSubTitleParser implements Handler.Callback, SubTitleParser {
	private static final int WHAT_LOAD_SUBTILTE = 9001;
	private static final int WHAT_SEEKTO 		= 9002;
	private static final int WHAT_CLOSE 		= 9003;
	
	private static boolean sLoadLibSuccess = false;
	
	private static boolean isLoadLibSuccess() {
		return sLoadLibSuccess;
	}
	
	private SubTitleSegment mSegment;
	
	private int mNativeContext;
	private Handler mInnerHandler;
	
	public SimpleSubTitleParser() {
		if (!isLoadLibSuccess()) {
			throw new IllegalStateException("Load Lib failed");
		}
		
		native_setup();
		
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
				native_loadSubtitle(mFilePath, false /* isMediaFile */);
				break;
			}
			case WHAT_SEEKTO: {
				long msec = msg.getData().getLong("seek_to_msec");
				native_seekTo(msec);
				break;
			}
			case WHAT_CLOSE: {
				native_close();
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
		boolean ret = native_next(mSegment);
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
	
	private Callback mSubTitleCallback; 
	
	@Override
	public void setOnPreparedListener(Callback callback) {
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
	
	private static native void native_init();
	
	private native void native_setup();

	private native void native_loadSubtitle(String filePath, boolean isMediaFile);
	
	private native boolean native_next(SubTitleSegment segment);
	
	private native void native_seekTo(long msec);
	
	private native void native_close();
	
	static {
		try {
			System.loadLibrary("subtitle-jni");
			native_init();
			
			sLoadLibSuccess = true;
		} catch (SecurityException e) {
			e.printStackTrace();
			LogUtils.error("subtitle SecurityException");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			LogUtils.error("subtitle UnsatisfiedLinkError");
		} catch (IllegalStateException e) {
			e.printStackTrace();
			LogUtils.error("subtitle IllegalStateException");
		}
	}
}
