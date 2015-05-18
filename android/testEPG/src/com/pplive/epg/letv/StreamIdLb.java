package com.pplive.epg.letv;

public class StreamIdLb { 
	private String mId;
	private String mUrl;

	@SuppressWarnings("unused")
	private StreamIdLb() {
		
	}
	
	public StreamIdLb(String id, String url) {
		mId = id;
		mUrl = url;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
}
