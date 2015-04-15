package com.pplive.meetplayer.service;

import java.util.Map;
import java.util.Random;

import com.pplive.dlna.DLNASdk;
import com.pplive.meetplayer.util.IDlnaCallback;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class DLNAService extends Service {
	private final static String TAG = "DLNAService";
	public static final String ACTION = "com.pplive.meetplayer.DLNAService";
	
	// dlna
	private DLNASdk mDLNA;
	private IDlnaCallback mDLNAcallback;
	private final static int DLNA_LISTEN_PORT = 10010;
	
	public DLNASdk getSDK() {return mDLNA;}
	public Map<String, String> getDeviceList() {return mDLNAcallback.mDMRmap;}
	
	private MyBinder binder = new MyBinder();
	private int count;
	private boolean quit = false;
	
	public class MyBinder extends Binder {
		public int getCount() {
			return count;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: DLNAService onBind()");
		return binder;
	}
	
	@Override  
    public void onCreate() {  
        Log.i(TAG, "Java: DLNAService onCreate");  
        super.onCreate();
        
        new Thread() {
        	@Override
        	public void run() {
        		while(!quit) {
        			try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        			
        			count++;
        		}
        	}
        }.start();
    }  
      
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "Java: DLNAService onUnbind()");  
		return true;
	}
	
    @Override  
    public int onStartCommand(Intent intent, int flags, int startId) {  
        Log.i(TAG, "Java: DLNAService onStartCommand");
       
        return super.onStartCommand(intent, flags, startId);  
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

    	this.quit = true;
    	Log.i(TAG, "Java: DLNAService onDestroy()");
    }
}
	
