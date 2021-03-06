package com.gotye.common.youku;

public class Catalog { 
	private String mTitle;
	private String mFilter;
	private int mSubChannelType;
	private int mSubChannelId;
	
	@SuppressWarnings("unused")
	private Catalog() {
		
	}
	
	public Catalog(String title, String filter, 
			int sub_channel_type, int sub_channel_id) {
		this.mTitle				= title;
		this.mFilter			= filter;
		this.mSubChannelType	= sub_channel_type;
		this.mSubChannelId		= sub_channel_id;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getFilter() {
		return mFilter;
	}
	
	public int getSubChannelType() {
		return mSubChannelType;
	}
	
	public int getSubChannelId() {
		return mSubChannelId;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		if (mFilter != null && !mFilter.isEmpty()) {
			sb.append(", filter: ");
			sb.append(mFilter);
		}
		sb.append(", sub_channel_type: ");
		sb.append(mSubChannelType);
		sb.append(", sub_channel_id: ");
		sb.append(mSubChannelId);
		
		return sb.toString();
	}
	
}
