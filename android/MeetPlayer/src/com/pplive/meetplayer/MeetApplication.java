package com.pplive.meetplayer;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import com.pplive.meetplayer.service.MediaScannerService;
import com.pplive.meetplayer.service.MyHttpService;
import com.pplive.meetplayer.util.Util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class MeetApplication extends Application {
	
	private final static String TAG = "MeetApplication";
	
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
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK); 
		MyBroadcastReceiver receiver = new MyBroadcastReceiver(); 
		registerReceiver(receiver, filter);
		
		Intent intent = new Intent(getApplicationContext(), MyHttpService.class);  
		startService(intent);
		
		Intent intent2 = new Intent(getApplicationContext(), MediaScannerService.class);
		startService(intent2);
	}
	
	@Override
	public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Java: onLowMemory()");
        
        //ImageLoader.getInstance().clearMemoryCache();
    }
	
	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
				// 检查Service状态
				boolean isServiceRunning = false;
				ActivityManager manager = (ActivityManager) getApplicationContext()
						.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningServiceInfo service : manager
						.getRunningServices(Integer.MAX_VALUE)) {
					if ("com.pplive.meetplayer.service.MyHttpService".equals(service.service.getClassName())) {
						isServiceRunning = true;
						break;
					}

				}
				
				if (!isServiceRunning) {
					Intent i = new Intent(context, MyHttpService.class);
					context.startService(i);
					Log.i(TAG, "Java: restart MyHttpService service");
				}
			}
			else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
				// 开机启动服务
				//Intent i = new Intent(context, MyHttpService.class);
				//context.startService(i);
			}
		}

	}
}