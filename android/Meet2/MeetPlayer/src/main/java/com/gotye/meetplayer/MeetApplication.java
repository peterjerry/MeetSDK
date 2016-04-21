package com.gotye.meetplayer;

import com.gotye.crashhandler.CrashHandler;
import com.gotye.thirdparty.BreakpadUtil;

import com.gotye.meetplayer.service.MyHttpService;
import com.gotye.meetplayer.util.Util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.File;

public class MeetApplication extends Application {
	
	private final static String TAG = "MeetApplication";

	public static final String CRASH_HOST = "http://42.62.105.235";
	//public static final String CRASH_HOST = "http://iloveyaya.zz.vc"; //31.220.16.178
    //public static final String CRASH_HOST = "http://192.168.1.114:8088";
    public static final String UPLOAD_CRASH_LOG_URL = CRASH_HOST + "/crash/crash.php";
	public static final String UPLOAD_DUMP_URL = CRASH_HOST + "/dump/upload.php";
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		/*ImageLoaderConfiguration config =
				new ImageLoaderConfiguration.Builder(getApplicationContext())
						.threadPriority(Thread.NORM_PRIORITY - 1)
						.threadPoolSize(16)
						.denyCacheImageMultipleSizesInMemory()
                        .tasksProcessingOrder(QueueProcessingType.LIFO)
						.diskCacheSize(50 * 1024 * 1024)
                        .diskCacheFileCount(100)
						.build();
   
		ImageLoader.getInstance().init(config);*/
		
		//ExceptionHandler.register(this, "http://iloveyaya.zz.vc/crash/crash.php");
		
		Util.initLog(this);
        
        CrashHandler.getInstance().init(this, UPLOAD_CRASH_LOG_URL);

        BreakpadUtil.registerBreakpad(new File(getCacheDir().getAbsolutePath()));
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK); 
		MyBroadcastReceiver receiver = new MyBroadcastReceiver(); 
		registerReceiver(receiver, filter);
		
		Intent intent = new Intent(getApplicationContext(), MyHttpService.class);  
		startService(intent);
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
					if ("com.gotye.meetplayer.service.MyHttpService".equals(service.service.getClassName())) {
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