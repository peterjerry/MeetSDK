package com.pplive.epg.youku;

public class Channel { 
	private String mTitle;
	private int mChannelId;
	
	@SuppressWarnings("unused")
	private Channel() {
		
	}
	
//	{
//	channel_id: 1001,
//	is_youku_channel: 0,
//	content_type: 1,
//	title: "排行榜"
//	},
	
	public Channel(String title, int id) {
		this.mTitle		= title;
		this.mChannelId	= id;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getChannelId() {
		return mChannelId;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", channel_id: ");
		sb.append(mChannelId);
		
		return sb.toString();
	}
	
}
