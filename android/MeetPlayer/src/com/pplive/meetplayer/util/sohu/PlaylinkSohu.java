package com.pplive.meetplayer.util.sohu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.util.Log;

public class PlaylinkSohu {
	private final static String TAG = "PlaylinkSohu";
	
	private String mTitle;
	private String mUrlHigh;
	private String mUrlNormal;
	private String 	mHighDuration;
	private String	mNormalDuration;
	
	private List<String> mHighUrlList;
	private List<String> mNormalUrlList;
	
	@SuppressWarnings("unused")
	private PlaylinkSohu() {
		
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getUrl(int ft) {
		if (ft == 1)
			return mUrlNormal;
		else
			return mUrlHigh;
	}
	
	public String getDuration(int ft) {
		if (ft ==1 )
			return mNormalDuration;
		else
			return mHighDuration;
	}
	
	public List<String> getUrlListbyFT(int ft) {
		if (ft == 1)
			return mNormalUrlList;
		else
			return mHighUrlList;
	}
	
	public PlaylinkSohu(String title, String normal_url, String high_url, 
			String normal_duration, String high_duration) {
		mTitle				= title;
		mUrlNormal			= normal_url;
		mUrlHigh			= high_url;
		mHighDuration		= normal_duration;
		mNormalDuration		= high_duration;
		
		mNormalUrlList	= getList(mUrlNormal);
		mHighUrlList	= getList(mUrlHigh);
		
	}
	
	private List<String> getList(String url_list) {
		List<String> cliplist = new ArrayList<String>();
		StringTokenizer st;
        st = new StringTokenizer(url_list, ",", false);
        int i = 0;
		while (st.hasMoreElements()) {
			String url = st.nextToken();
			//Log.d(TAG, String.format("Java: segment #%d url: %s", i, url));
			cliplist.add(url);
			i++;
		}
		
		return cliplist;
	}
}
