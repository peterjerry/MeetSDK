package com.pplive.common.sohu;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class PlaylinkSohu {
	public enum SOHU_FT {
		SOHU_FT_NORMAL,
		SOHU_FT_HIGH,
		SOHU_FT_SUPER,
		SOHU_FT_ORIGIN
	};
	
	private String mTitle;
	private String mUrlNormal;
	private String mUrlHigh;
	private String mUrlSuper;
	private String mUrlOrigin;
	
	private String	mNormalDuration;
	private String 	mHighDuration;
	private String 	mSuperDuration;
	private String 	mOriginDuration;
	
	@SuppressWarnings("unused")
	private PlaylinkSohu() {
		
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getUrl(SOHU_FT ft) {
		if (ft == SOHU_FT.SOHU_FT_NORMAL)
			return mUrlNormal;
		else if (ft == SOHU_FT.SOHU_FT_HIGH)
			return mUrlHigh;
		else if (ft == SOHU_FT.SOHU_FT_SUPER)
			return mUrlSuper;
		else if (ft == SOHU_FT.SOHU_FT_ORIGIN)
			return mUrlOrigin;
		else
			return null;
	}
	
	/*
	 * unit sec, sepearted by comma e.g. 150.1,150.1,100.2
	 */
	public String getDuration(SOHU_FT ft) {
		if (ft == SOHU_FT.SOHU_FT_NORMAL)
			return mNormalDuration;
		else if (ft == SOHU_FT.SOHU_FT_HIGH)
			return mHighDuration;
		else if (ft == SOHU_FT.SOHU_FT_SUPER)
			return mSuperDuration;
		else if (ft == SOHU_FT.SOHU_FT_ORIGIN)
			return mOriginDuration;
		else
			return null;
	}
	
	/*
	 * param normal_duration unit: sec, e.g. 150.3,150.3,160
	 */
	public PlaylinkSohu(String title, 
			String normal_url, String high_url, 
			String normal_duration, String high_duration) {
		this(title, normal_url, high_url, "", "", normal_duration, high_duration, "", "");	
	}
	
	/*
	 * param normal_duration unit: sec, e.g. 150.3,150.3,160
	 */
	public PlaylinkSohu(String title, 
			String normal_url, String high_url, String super_url, String origin_url, 
			String normal_duration, String high_duration, String super_duration, String origin_duration) {
		mTitle				= title;
		mUrlNormal			= normal_url;
		mUrlHigh			= high_url;
		mUrlSuper			= super_url;
		mUrlOrigin			= origin_url;
		mNormalDuration		= normal_duration;
		mHighDuration		= high_duration;
		mSuperDuration		= super_duration;
		mOriginDuration		= origin_duration;
	}
	
	/*private List<String> getList(String url_list) {
		List<String> cliplist = new ArrayList<String>();
		StringTokenizer st;
        st = new StringTokenizer(url_list, ",", false);
        int i = 0;
		while (st.hasMoreElements()) {
			String url = st.nextToken();
			cliplist.add(url);
			i++;
		}
		
		return cliplist;
	}*/
}
