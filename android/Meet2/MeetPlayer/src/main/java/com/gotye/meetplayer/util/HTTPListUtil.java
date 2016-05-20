package com.gotye.meetplayer.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.ivy.util.url.ApacheURLLister;

import android.util.Log;

import com.gotye.common.util.LogUtil;

public class HTTPListUtil {
	
	private final static String TAG = "HTTPListUtil";
	
	private List<URL> mHttpFileList;
	private List<URL> mHttpFolderList;
	
	public List<URL> getFile() {
		return mHttpFileList;
	}
	
	public List<URL> getFolder() {
		return mHttpFolderList;
	}
	
	@SuppressWarnings("unchecked")
	public boolean ListHTTPList(String http_url) {
		Log.i(TAG, "Java list_http_server: " + http_url);
		
		try {
			ApacheURLLister lister = new ApacheURLLister();
			URL url;
			url = new URL(http_url);
			mHttpFileList = lister.listFiles(url); //listAll
			for (int i = 0; i < mHttpFileList.size(); i++) {
				URL full_path = (URL)mHttpFileList.get(i);
				LogUtil.info(TAG, "http file: " + full_path.toString());
			}
			
			mHttpFolderList = lister.listDirectories(url);
			for (int i = 0; i < mHttpFolderList.size(); i++) {
				URL full_path = (URL)mHttpFolderList.get(i);
				LogUtil.info(TAG, "http folder: " + full_path.toString());
			}

			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
}
