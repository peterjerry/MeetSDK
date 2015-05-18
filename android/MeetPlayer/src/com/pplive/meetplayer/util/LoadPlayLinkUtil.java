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
		if (!file.exists()) {
			Log.w(TAG, "Java: tvlist.txt not existed!");
			return false;
		}
		
	    FileInputStream fin = null;
	    
		try {
		    fin = new FileInputStream(file);
		    int filesize = fin.available();
		    Log.i(TAG, "Java: tvlist.txt filesize " + filesize);
		    
		    byte[] buf = new byte[filesize];
		    
	    	fin.read(buf);
	    	String s = new String(buf);
			// fix win32 txt problem
	    	s = s.replace("\r\n", "\n");
	    	Log.i(TAG, "Java: tvlist.txt file content " + s.replace("\n", ""));

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
		    
		    if (mTitleList.size() == 0)
		    	Log.w(TAG, "Java: tvlist.txt file content may be corrupted");
		    
		    fin.close();
		    ret = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Java: tvlist.txt FileNotFoundException" + e.getMessage());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Java: tvlist.txt IOException" + e.getMessage());
		}
		
		return ret;
	}
}
