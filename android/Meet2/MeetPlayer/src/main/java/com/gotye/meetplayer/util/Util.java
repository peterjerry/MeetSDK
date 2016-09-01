package com.gotye.meetplayer.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;

import com.gotye.crashhandler.AppInfo;
import com.gotye.crashhandler.UploadFileTask;
import com.gotye.crashhandler.UploadLogTask;
import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetsdk.player.MediaInfo;
import com.gotye.meetsdk.player.TrackInfo;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.db.PPTVPlayhistoryDatabaseHelper;
import com.pplive.sdk.MediaSDK;

public class Util {
	private final static String TAG = "Util";
	
	public static final String player_log_path = 
			Environment.getExternalStorageDirectory().getAbsolutePath() + 
			"/meetplayer/player.log";
	public static final String log_path = 
			Environment.getExternalStorageDirectory().getAbsolutePath() + 
			"/meetplayer/meetplayer.log";
	public static final String upload_log_path = 
			Environment.getExternalStorageDirectory().getAbsolutePath() + 
			"/meetplayer/upload.log";
	
	private final static String PREF_NAME = "settings";
	
    private final static int ONE_KILOBYTE 	= 1024;
	private final static int ONE_MAGABYTE 	= (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE 	= (ONE_MAGABYTE * ONE_KILOBYTE);
	
	private static PPTVPlayhistoryDatabaseHelper mHistoryDB;
	
	public static boolean initLog(Context ctx) {
		String log_folder = ctx.getCacheDir().getParent();
		LogUtil.init(log_path, log_folder);
		
		return true;
	}
	
	public static boolean startP2PEngine(Context context) {
		Log.d("Util", "startP2PEngine()");
		if (context == null) {
			return false;
		}

		String gid = "12";
		String pid = "161";
		String auth = "08ae1acd062ea3ab65924e07717d5994";

		String libPath = "/data/data/com.gotye.meetplayer/lib";
		MediaSDK.libPath = libPath;
		MediaSDK.logPath = "/data/data/com.gotye.meetplayer/cache";
		MediaSDK.logOn = false;
		MediaSDK.setConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
		MediaSDK.setConfig("", "RtspManager", "addr", "127.0.0.1:5156+");
		
		Properties props = System.getProperties();
        String osArch = props.getProperty("os.arch");
        if (osArch != null && osArch.contains("aarch64"))
        	MediaSDK.libName = "ppbox_jni-armandroid-r4-gcc44-mt-1.1.0";
		
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
		if (file.exists()) {
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
		}
		
		MeetSDK.setLogPath(
				player_log_path, 
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

	public static boolean writeSettingsLong(Context ctx, String key, long value) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(key, value);
		return editor.commit();
	}

	public static long readSettingsLong(Context ctx, String key) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
		return settings.getLong(key, 0L);
	}

	public static void add_sohuvideo_history(Context ctx, String title, int vid, long aid, int site) {
		String key = "SohuPlayHistory";
		String regularEx = ",";
		final int save_max_count = 20;
		String value = readSettings(ctx, key);
		
		List<String> playHistoryList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, regularEx, false);
        while (st.hasMoreElements()) {
        	String token = st.nextToken();
        	playHistoryList.add(token);
        }
        
        String new_video = String.format("%s|%d|%d|%d", title, vid, aid, site);
        
        int count = playHistoryList.size();
        StringBuffer sb = new StringBuffer();
        int start = count - save_max_count + 1;
        if (start < 0)
        	start = 0;
        
        boolean isNewVideo = true;
        for (int i = start; i<count ; i++) {
        	String item = playHistoryList.get(i);
        	if (new_video.contains(item) && isNewVideo)
        		isNewVideo = false;
        	
        	sb.append(item);
        	sb.append(regularEx);
        }
        
        if (isNewVideo)
        	sb.append(new_video);
        else
        	Log.i(TAG, String.format("Java %s already in history list", new_video));
        
		writeSettings(ctx, key, sb.toString());
	}
	
	public static void add_pptvvideo_history(Context ctx, String title, 
			String playlink, String album_id, int ft) {
		if (mHistoryDB == null)
			mHistoryDB = PPTVPlayhistoryDatabaseHelper.getInstance(ctx);
		
		mHistoryDB.saveHistory(title, playlink, album_id, ft);
	}
	
	public static void save_pptvvideo_pos(Context ctx, String playlink, int pos/*msec*/) {
		if (mHistoryDB == null)
			mHistoryDB = PPTVPlayhistoryDatabaseHelper.getInstance(ctx);
		
		mHistoryDB.savePlayedPosition(playlink, pos);
	}
	
	public static String getUriPath(Uri uri) {
    	String urlPath;
		String scheme = uri.getScheme();
		if (scheme == null || scheme.equals("file")) {
			//local file
			urlPath = uri.getPath();
		}
		else {
			//network path
			urlPath = uri.toString();
		}
		
    	return urlPath;
    }
	
	public static String read_file(String path, String encode) {
		if (path == null || encode == null)
			return null;
		
		File file = new File(path);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		
		InputStreamReader inputStreamReader = null;  
	    try {  
	        inputStreamReader = new InputStreamReader(fis, encode);  
	    } catch (UnsupportedEncodingException e) {  
	        e.printStackTrace();  
	    }  
	    
	    BufferedReader reader = new BufferedReader(inputStreamReader);  
	    StringBuffer sb = new StringBuffer("");  
	    String line;  
	    try {  
	        while ((line = reader.readLine()) != null) {  
	            sb.append(line);  
	            sb.append("\n");  
	        }  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }  
	    
	    try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return sb.toString();  
	}
	
	public static boolean IsHaveInternet(final Context context) { 
        try { 
            ConnectivityManager manger = (ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE); 
 
            NetworkInfo info = manger.getActiveNetworkInfo(); 
            return (info != null && info.isConnected()); 
        } catch (Exception e) { 
        	e.printStackTrace();
            return false; 
        } 
    } 
	
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetworkInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiNetworkInfo != null)
            return wifiNetworkInfo.isConnected();

        return false;
	}

    public static boolean isLANConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo lanNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (lanNetworkInfo != null)
            return lanNetworkInfo.isConnected();

        return false;
    }

    public static String getWifiIpAddr(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return intToIp(ipAddress);
	}
	
	public static String getIpAddr(Context context) {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface intf = en.nextElement();
				//if (intf.getName().toLowerCase().equals("eth0")
				//		|| intf.getName().toLowerCase().equals("wlan0")) {
					for (Enumeration<InetAddress> enumIpAddr = intf
							.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							String ipaddress = inetAddress.getHostAddress();
							if (!ipaddress.contains("::")) {// ipV6的地址
								return ipaddress;
							}
						}
					}
				//} else {
				//	continue;
				//}
			}
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		
		return null;
	}

    public static void checkNetworkType(Context context) {
		if (IsHaveInternet(context) && !isLANConnected(context) && !isWifiConnected(context)) {
            Toast.makeText(context, "移动网络观看中，土豪请随意", Toast.LENGTH_SHORT).show();
        }
    }

    public static String GetNetworkType(Context context) {
        String strNetworkType = "Unknown";

        NetworkInfo networkInfo =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                strNetworkType = "WIFI";
            }
            else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                String _strSubTypeName = networkInfo.getSubtypeName();

                LogUtil.info(TAG, "Network getSubtypeName : " + _strSubTypeName);

                // TD-SCDMA   networkType is 17
                int networkType = networkInfo.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                        strNetworkType = "2G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                    case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                    case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                        strNetworkType = "3G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                        strNetworkType = "4G";
                        break;
                    default:
                        // http://baike.baidu.com/item/TD-SCDMA 中国移动 联通 电信 三种3G制式
                        if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA") || _strSubTypeName.equalsIgnoreCase("WCDMA") || _strSubTypeName.equalsIgnoreCase("CDMA2000"))
                        {
                            strNetworkType = "3G";
                        }
                        else
                        {
                            strNetworkType = _strSubTypeName;
                        }

                        break;
                }

                LogUtil.info(TAG, "Network getSubtype : " + Integer.valueOf(networkType).toString());
            }
			else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
				strNetworkType = "ETHERNET";
			}
        }

        LogUtil.info(TAG, "Network Type : " + strNetworkType);

        return strNetworkType;
    }

    public static String[] nsLookup(String hostname) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            if (addresses.length > 0) {
                String []ips = new String[addresses.length];
                for (int i = 0; i < addresses.length; i++) {
                    LogUtil.info(TAG, hostname + "[" + i + "]: "
                            + addresses[i].getHostAddress());
                    ips[i] = addresses[i].getHostAddress();
                }

                return ips;
            }
            else {
                LogUtil.error(TAG, "none ip addr resolved");
            }
        } catch (UnknownHostException uhe) {
            LogUtil.warn(TAG, "Unable to find: " +hostname);
        }

        return null;
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
	
	public static String getMediaInfoDescription(String path, MediaInfo info) {
    	StringBuffer sbInfo = new StringBuffer();

    	sbInfo.append("文件名 ");
    	sbInfo.append(path);
    	
    	sbInfo.append("\n分辨率 ");
    	sbInfo.append(info.getWidth());
    	sbInfo.append(" x ");
    	sbInfo.append(info.getHeight());
    	sbInfo.append("\n时长 ");
    	sbInfo.append(msecToString(info.getDuration()));
    	sbInfo.append("\n帧率 ");
    	sbInfo.append(String.format("%.2f", info.getFrameRate()));
    	sbInfo.append("\n码率 ");
    	sbInfo.append(info.getBitrate() / 1024);
    	sbInfo.append(" kbps");
    	if (info.getVideoCodecName() != null) {
    		sbInfo.append("\n视频编码 ");
    		sbInfo.append(info.getVideoCodecName());
    	}
    	String vProfile = info.getVideoCodecProfile();
    	if (vProfile != null) {
    		sbInfo.append("(profile ");
    		sbInfo.append(vProfile);
    		sbInfo.append(")");
    	}
    	
    	sbInfo.append("\n音轨数 ");
    	int audioNum = info.getAudioChannels();
    	sbInfo.append(audioNum);
    	
    	if (audioNum > 0) {
    		sbInfo.append("\n音轨编码 ");
    		List<TrackInfo> audioList = info.getAudioChannelsInfo();
    		if (audioList != null) {
	    		for (int i=0;i<audioNum;i++) {
	    			if (i > 0)
	    				sbInfo.append(" | ");
	    			
	    			sbInfo.append(audioList.get(i).getCodecName());
	    			String profile = audioList.get(i).getCodecProfile();
	    			if (profile != null && !profile.isEmpty()) {
	    	    		sbInfo.append("(profile ");
	    	    		sbInfo.append(profile);
	    	    		sbInfo.append(")");
	    	    	}
	    		}
    		}
    	}
    	
    	sbInfo.append("\n内嵌字幕数 ");
    	int subtitleNum = info.getSubtitleChannels();
    	sbInfo.append(subtitleNum);

    	if (subtitleNum > 0) {
    		sbInfo.append("\n字幕编码 ");
    		List<TrackInfo> subtitleList = info.getSubtitleChannelsInfo();
    		if (subtitleList != null) {
	    		for (int i=0;i<subtitleNum;i++) {
	    			if (i > 0)
	    				sbInfo.append(" | ");
	    			
	    			sbInfo.append(subtitleList.get(i).getCodecName());
	    		}
    		}
    	}
    	
    	Map<String, String> metadata = info.getMetaData();
    	if (metadata != null) {
    		sbInfo.append("\n容器元数据\n");
    		Iterator<Map.Entry<String, String>> iter = metadata.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
				String key = entry.getKey();
				String val = entry.getValue();
				sbInfo.append(key);
				sbInfo.append(":   ");
				sbInfo.append(val);
				sbInfo.append("\n");
			}
    	}

		return sbInfo.toString();
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

	public static String getHttpPage(String url) {
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
			URL realUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间

			conn.connect();

			if (conn.getResponseCode() != 200) {
				LogUtil.error(TAG, "http response is not 200 " + conn.getResponseCode());
				return null;
			}

			// 获取响应的输入流对象
			is = conn.getInputStream();

			// 创建字节输出流对象
			os = new ByteArrayOutputStream();
			// 定义读取的长度
			int len = 0;
			// 定义缓冲区
			byte buffer[] = new byte[1024];
			// 按照缓冲区的大小，循环读取
			while ((len = is.read(buffer)) != -1) {
				// 根据读取的长度写入到os对象中
				os.write(buffer, 0, len);
			}

			// 返回字符串
			return new String(os.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// 释放资源
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

    public static String postHttpPage(String url, String params) {
        return postHttpPage(url, params, null);
    }

	public static String postHttpPage(String url, String params, Map<String, String>headers) {
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
			URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			conn.setRequestMethod("POST");
            if (headers != null) {
                Set set = headers.keySet();

                for (Iterator iter = set.iterator(); iter.hasNext();) {
                    String key = (String)iter.next();
                    String value = headers.get(key);
                    LogUtil.info(TAG, "setRequestProperty " + key + " : " + value);
                    conn.setRequestProperty(key, value);
                }
            }

			if (params != null) {
				byte[] bypes = params.getBytes();
				conn.getOutputStream().write(bypes);// 输入参数
			}
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间

			conn.connect();

			if (conn.getResponseCode() != 200) {
				LogUtil.error(TAG, "http response is not 200 " + conn.getResponseCode());
				return null;
			}

			// 获取响应的输入流对象
			is = conn.getInputStream();

			// 创建字节输出流对象
			os = new ByteArrayOutputStream();
			// 定义读取的长度
			int len = 0;
			// 定义缓冲区
			byte buffer[] = new byte[1024];
			// 按照缓冲区的大小，循环读取
			while ((len = is.read(buffer)) != -1) {
				// 根据读取的长度写入到os对象中
				os.write(buffer, 0, len);
			}

			// 返回字符串
			return new String(os.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// 释放资源
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static void upload_crash_dump(final Context context) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

		File dmp_folder = new File(context.getCacheDir().getAbsolutePath());
		String[] exts = new String[]{"dmp", "zip"};
		File[] files = dmp_folder.listFiles(new FileFilterTest(exts));
		if (files != null && files.length > 0) {
			for (File f : files) {
				try {
					LogUtil.info(TAG, "ready to upload dump file " + f.getName());

					String upload_filename;
					if (f.getName().endsWith("dmp")) {
						String str_time = format.format((new Date()));
						String zipPath = context.getCacheDir().getAbsolutePath() + File.separator +
								"log.zip";
						String infoPath = context.getCacheDir().getAbsolutePath() +
								File.separator + str_time + "_info.txt";

						StringBuffer sb = new StringBuffer();
						sb.append("PHONE_MODEL: ").append(AppInfo.PHONE_MODEL).append("\n");
						sb.append("ANDROID_VERSION: ").append(AppInfo.ANDROID_VERSION).append("\n");
						sb.append("APP_PACKAGE: ").append(AppInfo.APP_PACKAGE).append("\n");
						sb.append("APP_VERSION: ").append(AppInfo.APP_VERSION).append("\n");

						MeetSDK.makePlayerlog();
						File file = new File(Util.player_log_path);
						if (file.exists()) {
							sb.append("==============player log==============\n");

							BufferedReader bf = new BufferedReader(new FileReader(file));
							String content;
							while (true) {
								content = bf.readLine();
								if (content == null)
									break;

								sb.append(content);
								sb.append("\n");
							}
						}

						writeBytestoFile(sb.toString(), infoPath);

						File[] logfiles = new File[2];
						logfiles[0] = f;
						logfiles[1] = new File(infoPath);
						ZipUtils.zipFiles(logfiles, zipPath);
						f.delete();
						upload_filename = zipPath;
					}
					else {
						upload_filename = f.getAbsolutePath();
					}

                    UploadFileTask task = new UploadFileTask(context);
                    task.setOnTaskListener(new UploadLogTask.TaskListener() {
                        @Override
                        public void onFinished(String msg, int code) {
                            Toast.makeText(context,
                                    msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String msg, int code) {
                            Toast.makeText(context,
                                    msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                    task.execute(upload_filename);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @param stacktrace
	 * 将崩溃写入文件系统
	 */
	public static void makeUploadLog(String stacktrace) {
		Log.i(TAG, "Java: makeUploadLog()");
		
		StringBuffer sb = new StringBuffer();
		if (stacktrace != null)
			sb.append(stacktrace);
		
		LogUtil.makeUploadLog();
		MeetSDK.makePlayerlog();
		
		BufferedReader bf1 = null;
		BufferedReader bf2 = null;
		String content = null;
		try {
			sb.append("==============MeetSDK log==============\n");
			
			File file = new File(Util.player_log_path);
			if (file.exists()) {
				bf1 = new BufferedReader(new FileReader(file));
				while (true) {
					content = bf1.readLine();
					if (content == null)
						break;
					
					sb.append(content);
					sb.append("\n");
				}
			}
			
			sb.append("==============MeetPlayer log==============\n");
			
			File file2 = new File(Util.log_path);
			if (file2.exists()) {
				bf2 = new BufferedReader(new FileReader(file2));
				while (true) {
					content = bf2.readLine();
					if (content == null)
						break;
					
					sb.append(content);
					sb.append("\n");
				}
			}

			writeLog(sb.toString(), Util.upload_log_path);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (bf1 != null) {
					bf1.close();
				}
				if (bf2 != null) {
					bf2.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

    /**
     * 将String数据存为文件
     */
    private static boolean writeBytestoFile(String strContext, String path) {
        byte[] b = strContext.getBytes();
        BufferedOutputStream stream = null;
        try {
            File file = new File(path);
            FileOutputStream fstream = new FileOutputStream(file);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return false;
    }

	/**
	 * 
	 * @param log
	 * @param filename
	 * @return 返回写入的文件路径
	 * 写入Log信息的方法，写入到SD卡里面
	 */
	private static boolean writeLog(String log, String filename) {
		File file = new File(filename);
		
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
			
		try  {
			file.createNewFile();
			FileWriter fw = new FileWriter(file, false);   
			BufferedWriter bw = new BufferedWriter(fw);
			//写入相关Log到文件
			bw.write(log);
			bw.newLine();
			bw.close();
			fw.close();
			return true;
		} catch (IOException e) {
			Log.e(TAG, "an error occured while writing file: " + e);
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String getFileSize(long size) {
	    String strSize;
	    if (size < 0)
	    	return "N/A";
	    
	    if (size > ONE_GIGABYTE)
			strSize = String.format("%.3f GB",
					(double) size / (double) ONE_GIGABYTE);
	    else if (size > ONE_MAGABYTE)
			strSize = String.format("%.3f MB",
					(double) size / (double) ONE_MAGABYTE);
		else if (size > ONE_KILOBYTE)
			strSize = String.format("%.3f kB",
					(double) size / (double) ONE_KILOBYTE);
		else
			strSize = String.format("%d Byte", size);
		return strSize;
    }
	
	private static String msecToString(long msec) {
		long msec_, sec, minute, hour, tmp;
		msec_ = msec % 1000;
		sec = msec / 1000;

		// sec = 3710
		tmp = sec % 3600; // 110
		hour = sec / 3600; // 1
		sec = tmp % 60; // 50
		minute = tmp / 60; // 1
		return String.format("%02d:%02d:%02d:%03d", hour, minute, sec, msec_);
	}
	
	private static String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}
}
