package com.gotye.meetplayer.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

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
	
	public boolean LoadTvList(String filepath) {
    	String s = Util.read_file(filepath, "utf-8");
    	if (s == null)
    		return false;
    	
    	Log.i(TAG, "Java: file content " + s.replace("\n", ""));

    	StringTokenizer st = new StringTokenizer(s, "\n", false);
	    while (st.hasMoreElements()) {
	    	String line = st.nextToken();
	    	if (line.startsWith("#") || line.startsWith(" "))
	    		continue;
	    	
	    	int delim = line.indexOf(',');
	    	if (delim == -1)
	    		delim = line.indexOf(' ');
	    	if (delim == -1)
	    		continue;
	    	
	    	String title = line.substring(0, delim);
	    	String url = line.substring(delim + 1);
	    	Log.i(TAG, String.format("Java: filecontext title: %s url: %s", title, url));
	    	mTitleList.add(title);
	    	mUrlList.add(url);
    	}
	    
	    if (mTitleList.size() == 0)
	    	Log.w(TAG, "Java: tvlist.txt file content may be corrupted");
	    
		return true;
	}
}
