package com.gotye.common.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class httpUtil {
	private static String TAG = "httpUtil";
	
	public static boolean httpDownload(String httpUrl,String saveFile){  
		// 下载网络文件
		int bytesum = 0;
		int byteread = 0;

		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		try {
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("RANGE", "bytes=1400-"); // Range: bytes=500-999
			
			InputStream inStream = conn.getInputStream();
			FileOutputStream fs = new FileOutputStream(saveFile);

			byte[] buffer = new byte[1024];
			while ((byteread = inStream.read(buffer)) != -1) {
				bytesum += byteread;
				// System.out.println(bytesum);
				fs.write(buffer, 0, byteread);
			}

			Log.i(TAG, "Java: total file size: " + bytesum);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static int httpDownloadBuffer(String httpUrl, int from, byte [] buffer){  
		// 下载网络文件
		int bytesum = 0;
		int byteread = 0;

		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return -1;
		}

		try {
			URLConnection conn = url.openConnection();
			if (from > 0)
				conn.setRequestProperty("RANGE", String.format("bytes=%d-", from)); // Range: bytes=500-999
			
			InputStream inStream = conn.getInputStream();
			Log.i(TAG, "inStream available: " + inStream.available());
			
			int offset = 0;
			int count = 1024;
			while ((byteread = inStream.read(buffer, offset, count)) != -1) {	
				bytesum += byteread;
				offset += byteread;
				//Log.i(TAG, "read " + byteread);
			}

			Log.i(TAG, "Java: total file size: " + bytesum);
			return bytesum;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
}
