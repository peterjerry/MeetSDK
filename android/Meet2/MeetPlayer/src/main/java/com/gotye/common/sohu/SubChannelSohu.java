package com.gotye.common.sohu;

public class SubChannelSohu {
	/*
	"name":"推荐",
	"load_more":1,
	"sub_channel_type":0,
	"sub_channel_id":1000000
	 */
	
	public String mTitle;
	public int mSubChannelType;
	public int mSubChannelId;
	
	public SubChannelSohu(String title, int type, int id) {
		mTitle			= title;
		mSubChannelType	= type;
		mSubChannelId	= id;
	}
}
