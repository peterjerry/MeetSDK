package so.cym.crashhandlerdemo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.pplive.common.util.LogUtil;
import com.pplive.meetplayer.util.Util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.pplive.media.MeetSDK;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler{

	/** TAG */
	private static final String TAG = "CrashHandler";

	/** mDefaultHandler */
	private Thread.UncaughtExceptionHandler defaultHandler;

	/** instance */
	private static CrashHandler instance = new CrashHandler();

	/** infos */
	private Map<String, String> infos = new HashMap<String, String>();

	/** formatter */
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/** context*/
	private Context context;
	
	private CrashHandler() {}

	public static CrashHandler getInstance() {
		if (instance == null) {
			instance = new CrashHandler();
		}
		return instance;
	}

	/**
	 * 
	 * @param ctx
	 * 初始化，此处最好在Application的OnCreate方法里来进行调用
	 */
	public void init(Context ctx, String url) {
		this.context = ctx;
		defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
		
		PackageManager localPackageManager = ctx.getPackageManager();
	    try {
	      PackageInfo localPackageInfo = localPackageManager.getPackageInfo(ctx.getPackageName(), 0);
	      AppInfo.APP_VERSION = localPackageInfo.versionName;
	      AppInfo.APP_PACKAGE = localPackageInfo.packageName;
	      AppInfo.FILES_PATH = ctx.getFilesDir().getAbsolutePath();
	      AppInfo.PHONE_MODEL = Build.MODEL;
	      AppInfo.ANDROID_VERSION = Build.VERSION.RELEASE;
	      AppInfo.URL = url;
	    }
	    catch (PackageManager.NameNotFoundException localNameNotFoundException)
	    {
	      localNameNotFoundException.printStackTrace();
	    }
	    
	    Log.i(TAG, new StringBuilder().append("Java: TRACE_VERSION: ").append(AppInfo.TraceVersion).toString());
	    Log.i(TAG, new StringBuilder().append("Java: APP_VERSION: ").append(AppInfo.APP_VERSION).toString());
	    Log.i(TAG, new StringBuilder().append("Java: APP_PACKAGE: ").append(AppInfo.APP_PACKAGE).toString());
	    Log.i(TAG, new StringBuilder().append("Java: FILES_PATH: ").append(AppInfo.FILES_PATH).toString());
	    Log.i(TAG, new StringBuilder().append("Java: URL: ").append(AppInfo.URL).toString());
	}

	/**
	 * uncaughtException
	 * 在这里处理为捕获的Exception
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		Log.e(TAG, "Java: uncaughtException()");
		
		handleException(throwable);
		defaultHandler.uncaughtException(thread, throwable);
	}
	private boolean handleException(Throwable ex) {
		Log.e(TAG, "Java: handleException()");
		
		if (ex == null) {
			Log.e(TAG, "Java: Throwable ex is null");
			return false;
		}
		
		collectDeviceInfo(context);
		writeCrashInfoToFile(ex);
		restart();
		return true;
	}

	/**
	 * 
	 * @param ctx
	 * 手机设备相关信息
	 */
	public void collectDeviceInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
					PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				String versionName = pi.versionName == null ? "null"
						: pi.versionName;
				String versionCode = pi.versionCode + "";
				infos.put("versionName", versionName);
				infos.put("versionCode", versionCode);
				infos.put("crashTime", formatter.format(new Date()));
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "an error occured when collect package info", e);
		}
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field: fields) {
			try {
				field.setAccessible(true);
				infos.put(field.getName(), field.get(null).toString());
				Log.d(TAG, field.getName() + " : " + field.get(null));
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}
	}

	/**
	 * 
	 * @param ex
	 * 将崩溃写入文件系统
	 */
	private void writeCrashInfoToFile(Throwable ex) {
		Log.i(TAG, "Java: writeCrashInfoToFile()");
		
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> entry: infos.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			sb.append(key + "=" + value + "\n");
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		String result = writer.toString();
		sb.append(result);
		sb.append("\n\n");
		
		Util.makeUploadLog(sb.toString());
	}

	private void restart(){
		 try{    
             Thread.sleep(1000);    
         }catch (InterruptedException e){    
        	 Log.e(TAG, "error : "+ e);    
         }     
		 
         Intent intent = new Intent(context.getApplicationContext(), SendCrashActivity.class);  
         PendingIntent restartIntent = PendingIntent.getActivity(    
        		 context.getApplicationContext(), 0, intent,    
                 Intent.FLAG_ACTIVITY_NEW_TASK);                                                 
         //退出程序                                          
         AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);    
         mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,    
                 restartIntent); // 1秒钟后重启应用   
	}
	
}
