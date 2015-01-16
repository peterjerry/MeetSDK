package com.pplive.meetplayer.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

public class AtvUtils {

	public static Context sContext;
    public static String mDeviceID;

	/**
	 * 初始化包信息packInfo
	 * 
	 * @return
	 */
	private static PackageInfo initPackInfo() {
		PackageManager packageManager = sContext.getPackageManager();
		PackageInfo packInfo = null;
		try {
			packInfo = packageManager != null ? packageManager.getPackageInfo(sContext.getPackageName(),
                    0) : null;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return packInfo;
	}

	/**
	 * 获得版本信息
	 * 
	 * @return
	 */
	public static String getAppVersionName() {
		PackageInfo info = initPackInfo();
		return info != null ? info.versionName : null;
	}

	public static int getAppVersionCode() {
		PackageInfo info = initPackInfo();
		return info != null ? info.versionCode : -1;
	}
	
	public static String getChannelCode() {
		String channelCode = getMetaData("CHANNEL");
		if (channelCode != null) {
			return channelCode;
		}
		return null;
	}
	
	public static String getMetaData(String key) {
		try {
			ApplicationInfo info = sContext.getPackageManager()
					.getApplicationInfo(sContext.getPackageName(),
							PackageManager.GET_META_DATA);
			Object value = info.metaData.get(key);
			if (value != null) {
				return value.toString();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	/**
	 * 获取设备唯一标识
	 * 
	 * @return
	 */
	public static final String generateUUID() {
		String s = "";
		String s1 = android.provider.Settings.Secure.getString(
				sContext.getContentResolver(), "android_id");
		if (s1 == null)
			s1 = "";
		String s2;
		String s3;
		WifiInfo wifiinfo;
		String s4;
		if (Build.VERSION.SDK_INT >= 9) {
			s2 = Build.SERIAL;
			if (s2 == null)
				s2 = "";
		} else {
			s2 = getDeviceSerial();
		}
		s3 = "";
		wifiinfo = ((WifiManager) sContext.getSystemService("wifi"))
				.getConnectionInfo();
		if (wifiinfo != null) {
			s3 = wifiinfo.getMacAddress();
			if (s3 == null)
				s3 = "";
		}
		try {
			s4 = s + s1 + s2 + s3;
			
			s4 = getMD5String(s4);
		} catch (NoSuchAlgorithmException nosuchalgorithmexception) {
			nosuchalgorithmexception.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return s4;
	}

	private static final String getDeviceSerial() {
		String s;
		try {
			Method method = Class.forName("android.os.Build")
					.getDeclaredMethod("getString",
							new Class[] { Class.forName("java.lang.String") });
			if (!method.isAccessible())
				method.setAccessible(true);
			s = (String) method.invoke(new Build(), "ro.serialno");
		} catch (ClassNotFoundException classnotfoundexception) {
			classnotfoundexception.printStackTrace();
			return "";
		} catch (NoSuchMethodException nosuchmethodexception) {
			nosuchmethodexception.printStackTrace();
			return "";
		} catch (InvocationTargetException invocationtargetexception) {
			invocationtargetexception.printStackTrace();
			return "";
		} catch (IllegalAccessException illegalaccessexception) {
			illegalaccessexception.printStackTrace();
			return "";
		}
		return s;
	}
	
	private static final String getMD5String(String s)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		byte abyte0[] = MessageDigest.getInstance("SHA-1").digest(s.getBytes("utf-8"));
		Formatter formatter = new Formatter();
		try {
			int i = abyte0.length;
			for (int j = 0; j < i; j++) {
				byte byte0 = abyte0[j];
				formatter.format("%02x", byte0);
			}
			return formatter.toString();
		} finally {
			formatter.close();
		}
	}

	public static final String getLocalMacAddress() {
		WifiManager wifi = (WifiManager) sContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		return info.getMacAddress();
	}
	
	//获取本地IP
		public static String getLocalIpAddress() {
			try {
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()&& !inetAddress.isLinkLocalAddress()) {
							return inetAddress.getHostAddress().toString();
						}
					}
				}
			} catch (SocketException ex) {
				Log.e("WifiPreference IpAddress", ex.toString());
			}

			return null;
		} 
		
		/**
		 * 根据Ip获取mac地址
		 * @param context
		 * @return
		 */
		public static String getLocalMacAddressFromIp(Context context) {
			String mac_s = "";
			try {
				byte[] mac;
				NetworkInterface ne = NetworkInterface.getByInetAddress(InetAddress.getByName(getLocalIpAddress()));
				mac = ne.getHardwareAddress();
				mac_s = byte2hex(mac);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mac_s;
		}
		
		private static String byte2hex(byte[] b) {
			StringBuffer sb = new StringBuffer(b.length);
			String stmp = "";
			int len = b.length;
			for (int i = 0; i < len; i++) {
				stmp = Integer.toHexString(b[i] & 0xFF);
				if (stmp.length() == 1) {
					sb = sb.append("0").append(stmp);
				} else {
					sb = sb.append(stmp);
				}
			}
			return String.valueOf(sb);
		}
}
