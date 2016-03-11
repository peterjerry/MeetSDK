package com.pplive.epg.youku;

import java.util.List;

public class Album { 
	private String mTitle;
	private String mStripe;
	private String mVideoId;
	private String mShowId;
	private String mTid;
	
	@SuppressWarnings("unused")
	private Album() {
		
	}
	
	public Album(String title, String vid, String show_id) {
		this.mTitle		= title;
		this.mVideoId	= vid;
		this.mShowId	= show_id;
	}
	
	public Album(String title, String vid, String stripe, String tid) {
		this.mTitle		= title;
		this.mVideoId	= vid;
		this.mStripe	= stripe;
		this.mTid		= tid;
	}
	
	public String getVideoId() {
		return mVideoId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getShowId() {
		return mShowId;
	}
	
	public String getTid() {
		return mTid;
	}
	
	public String getStripe() {
		return mStripe;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", videoId: ");
		sb.append(mVideoId);
		sb.append(", showId: ");
		sb.append(mShowId);
		sb.append(", tid: ");
		sb.append(mTid);
		sb.append(", stripe: ");
		sb.append(mStripe);
		
		return sb.toString();
	}
}
