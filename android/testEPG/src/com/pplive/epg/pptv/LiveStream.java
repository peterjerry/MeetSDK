package com.pplive.epg.pptv;

public class LiveStream {
	public String id;
	public String title;
	public String channelID;
	public String start_time;
	public String end_time;
	public String imageURL;
	
	LiveStream(String id, String title, String ChannelID, String start, String end, String imageURL) {
		this.id			= id;
		this.title		= title;
		this.channelID	= ChannelID;
		this.start_time	= start;
		this.end_time	= end;
		this.imageURL	= imageURL;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		sb.append("Title: ");
		sb.append(title);
		sb.append(" , id: ");
		sb.append(id);
		sb.append(" , start: ");
		sb.append(start_time);
		sb.append(" , end: ");
		sb.append(end_time);
		sb.append(" , channel_id: ");
		sb.append(channelID);
		
		return sb.toString();
	}
}
