package com.pplive.common.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class httpUtil {
	private final static String TAG = "httpUtil";
	
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
	           InputStream inStream = conn.getInputStream();  
	           FileOutputStream fs = new FileOutputStream(saveFile);  
	  
	           byte[] buffer = new byte[1204];  
	           while ((byteread = inStream.read(buffer)) != -1) {  
	               bytesum += byteread;  
	               Log.d(TAG, "Java: http download size " + bytesum);  
	               fs.write(buffer, 0, byteread);  
	           }  
	           
	           Log.i(TAG, "Java: total file size: " + bytesum);  
	           return true;  
	       } catch (FileNotFoundException e) {  
	           e.printStackTrace();  
	           return false;  
	       } catch (IOException e) {  
	           e.printStackTrace();  
	           return false;  
	       }  
	   }  
}
