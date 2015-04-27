package com.pplive.meetplayer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.pplive.media.MeetSDK;
import android.util.Log;

import com.pplive.sdk.MediaSDK;

public class Util {
	private final static String TAG = "Util";
	
	private final static String PREF_NAME = "settings";
	
	public static boolean startP2PEngine(Context context) {
		Log.d("Util", "startP2PEngine()");
		if (context == null) {
			return false;
		}

		String gid = "12";
		String pid = "161";
		String auth = "08ae1acd062ea3ab65924e07717d5994";

		String libPath = "/data/data/com.pplive.meetplayer/lib";
		MediaSDK.libPath = libPath; // Environment.getExternalStorageDirectory().getAbsolutePath() + "/ppp";
		MediaSDK.logPath = "/data/data/com.pplive.meetplayer/cache";
		MediaSDK.logOn = false;
		MediaSDK.setConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
		MediaSDK.setConfig("", "RtspManager", "addr", "127.0.0.1:5156+");
		
		long ret = -1;

		try {
			ret = MediaSDK.startP2PEngine(gid, pid, auth);
		} catch (Throwable e) {
			Log.e(TAG, e.toString());
		}

		Log.i(TAG, "Java: startP2PEngine result " + ret);
		return (ret != -1);// 端口占用&& ret != 9);
	}
	
	public static boolean initMeetSDK(Context ctx) {
		// upload util will upload /data/data/pacake_name/Cache/xxx
		// so must NOT change path
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PlayerPolicy.xml";
		File file = new File(path);
		try{
			FileInputStream fin = new FileInputStream(file);
			Log.i(TAG, "Java: PlayerPolicy file size: " + fin.available());
			
			byte[] buf = new byte[fin.available()];
			int readed = fin.read(buf);
			Log.i(TAG, "Java: PlayerPolicy read size: " + readed);
			String xml = new String(buf);
			Log.i(TAG, "Java: PlayerPolicy xml: " + xml);
			MeetSDK.setPlayerPolicy(new String(buf));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, "file not found");
		}
		catch(Exception e){   
			e.printStackTrace();
			Log.e(TAG, "an error occured while load policy", e);
		}
		
		MeetSDK.setLogPath(
				ctx.getCacheDir().getAbsolutePath() + "/meetplayer.log", 
				ctx.getCacheDir().getParentFile().getAbsolutePath() + "/");
		// /data/data/com.svox.pico/
		return MeetSDK.initSDK(ctx, "");
	}
	
	public static boolean writeSettings(Context ctx, String key, String value) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(key, value);
    	return editor.commit();
	}
	
	public static String readSettings(Context ctx, String key) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	return settings.getString(key, "");
	}
	
	public static boolean writeSettingsInt(Context ctx, String key, int value) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(key, value);
    	return editor.commit();
	}
	
	public static int readSettingsInt(Context ctx, String key) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	return settings.getInt(key, 0);
	}
	
	public static void copyFile(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			if (oldfile.exists()) { // 文件存在时
				InputStream inStream = new FileInputStream(oldPath); // 读入原文件
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread; // 字节数 文件大小
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
				fs.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "copy single file error");
			e.printStackTrace();
		}

	}

	public void copyFolder(String oldPath, String newPath) {

		try {
			(new File(newPath)).mkdirs(); // 如果文件夹不存在 则建立新文件夹
			File a = new File(oldPath);
			String[] file = a.list();
			File temp = null;
			for (int i = 0; i < file.length; i++) {
				if (oldPath.endsWith(File.separator)) {
					temp = new File(oldPath + file[i]);
				} else {
					temp = new File(oldPath + File.separator + file[i]);
				}

				if (temp.isFile()) {
					FileInputStream input = new FileInputStream(temp);
					FileOutputStream output = new FileOutputStream(newPath
							+ "/" + (temp.getName()).toString());
					byte[] b = new byte[1024 * 5];
					int len;
					while ((len = input.read(b)) != -1) {
						output.write(b, 0, len);
					}
					output.flush();
					output.close();
					input.close();
				}
				if (temp.isDirectory()) {// 如果是子文件夹
					copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "copy folder error");
			e.printStackTrace();

		}
	}
}
