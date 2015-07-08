package com.pplive.meetplayer;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.pplive.meetplayer.util.MyNanoHTTPD;

import fi.iki.elonen.NanoHTTPD;

import android.app.Application;
import android.util.Log;

public class MeetApplication extends Application {
	
	private final static String TAG = "MeetApplication";
	private final static int HTTP_PORT = 8080;
	
	private NanoHTTPD nanoHttpd;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "Java: onCreate()");
		
		/*ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
        .threadPriority(Thread.NORM_PRIORITY - 1).threadPoolSize(16)
           .denyCacheImageMultipleSizesInMemory().tasksProcessingOrder(QueueProcessingType.LIFO)
           .diskCacheSize(50 * 1024 * 1024).diskCacheFileCount(100)
           .build();
   
		ImageLoader.getInstance().init(config);*/
		
		nanoHttpd = new MyNanoHTTPD(this, HTTP_PORT, null);  
        try {  
            nanoHttpd.start();  
        } catch(Exception e) {  
            e.printStackTrace();  
        }  
	}
	
	@Override
	public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Java: onLowMemory()");
        
        //ImageLoader.getInstance().clearMemoryCache();
    }
}