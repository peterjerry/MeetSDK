package com.pplive.epg.letv;

public class PlayLinkLb { 
	private String mName;
	private String mUrl;

	@SuppressWarnings("unused")
	private PlayLinkLb() {
		
	}
	
	public PlayLinkLb(String name, String url) {
		mName = name;
		mUrl = url;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
}
