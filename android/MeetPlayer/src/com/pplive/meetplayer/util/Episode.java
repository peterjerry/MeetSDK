package com.pplive.meetplayer.util;

public class Episode { 
	private String mTitle;
	private int mSiteId;
	private String mVid;
	private String mExtId;
	
	@SuppressWarnings("unused")
	private Episode() {
		
	}
	
	public Episode(int siteId, String title, String vid, String extid) {
		this.mSiteId	= siteId;
		this.mTitle		= title;
		this.mVid		= vid;
		this.mExtId		= extid;
	}
	
	public String getVid() {
		return mVid;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getExtId() {
		return mExtId;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("siteId: ");
		sb.append(mSiteId);
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", vid: ");
		sb.append(mVid);
		sb.append(", extid: ");
		sb.append(mExtId);
		
		return sb.toString();
	}
}
