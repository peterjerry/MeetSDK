package com.gotye.common.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class httpUtil {
	private static String TAG = "httpUtil";

    private final static String DESKTOP_CHROME_USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/46.0.2490.86 Safari/537.36";
    private final static String YOUKU_USER_AGENT =
            "Youku HD;3.9.4;iPhone OS;7.1.2;iPad4,1";
	
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
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(5000);// 设置超时的时间
            conn.setConnectTimeout(5000);// 设置链接超时的时间
            conn.setRequestProperty("User-Agent", DESKTOP_CHROME_USER_AGENT);
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

    public static int httpDownloadBuffer(String httpUrl,
                                         int from, byte[] buffer) {
        return httpDownloadBuffer(httpUrl, null, from, buffer);
    }

	public static int httpDownloadBuffer(String httpUrl,
                                         String Cookie, int from, byte [] buffer){
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
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间
            conn.setRequestProperty("User-Agent", DESKTOP_CHROME_USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
            if (Cookie != null)
                conn.setRequestProperty("Cookie", Cookie);

			if (from > 0)
				conn.setRequestProperty("RANGE", String.format("bytes=%d-", from)); // Range: bytes=500-999
			
			InputStream inStream = conn.getInputStream();
			LogUtil.info(TAG, "inStream available: " + inStream.available());
			
			int offset = 0;
			int count = 1024;
			while ((byteread = inStream.read(buffer, offset, count)) != -1) {	
				bytesum += byteread;
				offset += byteread;
				//Log.i(TAG, "read " + byteread);
			}

            LogUtil.info(TAG, "Java: total file size: " + bytesum);
			return bytesum;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return -1;
	}

    public static String getHttpPage(String url) {
        return getHttpPage(url, null, false, null);
    }

	public static String getHttpPage(String url,
									 String UserAgent, boolean setEncoding, String cookie) {
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间
			if (UserAgent != null)
				conn.setRequestProperty("User-Agent", UserAgent);
			if (setEncoding)
				conn.setRequestProperty("Encoding", "gzip, deflate");

			if (cookie != null)
				conn.setRequestProperty("Cookie", cookie);

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
}
