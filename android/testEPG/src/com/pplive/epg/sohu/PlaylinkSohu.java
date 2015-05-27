package com.pplive.epg.sohu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class PlaylinkSohu {
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
	
	public String getUrl() {
		return mUrlHigh;
	}
	
	public List<String> getUrlListbyFT(int ft) {
		if (ft == 1)
			return mNormalUrlList;
		else
			return mHighUrlList;
	}
	
	/*
	 * param normal_duration unit: sec, e.g. 150.3,150.3,160
	 */
	public PlaylinkSohu(String title, String normal_url, String high_url, 
			String normal_duration, String high_duration) {
		mTitle				= title;
		mUrlNormal			= normal_url;
		mUrlHigh			= high_url;
		mNormalDuration		= normal_duration;
		mHighDuration		= high_duration;
		
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
			//System.out.println(String.format("Java: segment #%d url: %s", i, url));
			cliplist.add(url);
			i++;
		}
		
		return cliplist;
	}
}
