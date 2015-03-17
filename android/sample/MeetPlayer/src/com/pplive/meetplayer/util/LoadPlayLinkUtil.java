package com.pplive.meetplayer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class LoadPlayLinkUtil {
	private final static String TAG = "LoadPlayLinkUtil";
	
	public LoadPlayLinkUtil() {
		mTitleList = new ArrayList<String>();
		mUrlList = new ArrayList<String>();
	}
	
	private ArrayList<String> mTitleList;
	private ArrayList<String> mUrlList;
	
	public ArrayList<String> getTitles() {
		return mTitleList;
	}
	
	public ArrayList<String> getUrls() {
		return mUrlList;
	}
	
	public boolean LoadTvList() {
		boolean ret = false;
		
		String str_tvlist = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tvlist.txt";
		File file = new File(str_tvlist);
		if (file.exists()) {
		    FileInputStream fin = null;
		    
			try {
			    fin = new FileInputStream(file);
			    
			    byte[] buf = new byte[fin.available()];
			    
		    	fin.read(buf);
		    	String s = new String(buf);
		    	s = s.replace("\r\n", "\n");

			    int pos = 0;
			    while (true) {
			    	int comma = s.indexOf(',', pos);
			    	int newline = s.indexOf('\n', pos);
			    	if (comma == -1)
			    		break;
			    	if (newline == -1)
			    		newline = s.length();
			    	
			    	String title = s.substring(pos, comma);
			    	String url = s.substring(comma + 1, newline);
			    	Log.i(TAG, String.format("Java: filecontext title: %s url: %s", title, url));
			    	mTitleList.add(title);
			    	mUrlList.add(url);
			    	pos = newline + 1;
			    }
			    
			    fin.close();
			    ret = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return ret;
	}
}
