package com.gotye.common.youku;

import java.util.List;

public class Episode { 
	private String mTitle;
	private String mVideoId;
	List<String> mStrmTypeList;
	
	@SuppressWarnings("unused")
	private Episode() {
		
	}
	
	public Episode(String title, String vid, List<String> strm_type_list) {
		this.mTitle			= title;
		this.mVideoId		= vid;
		this.mStrmTypeList	= strm_type_list;
	}
	
	public String getVideoId() {
		return mVideoId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public List<String> getStrmList() {
		return mStrmTypeList;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", videoId: ");
		sb.append(mVideoId);
		sb.append(", stream type: ");
		sb.append(mStrmTypeList.toString());
		
		return sb.toString();
	}
}
