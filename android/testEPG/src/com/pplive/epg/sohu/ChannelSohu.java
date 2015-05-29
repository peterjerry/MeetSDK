package com.pplive.epg.sohu;

public class ChannelSohu {
	public String mTitle;
	public String mChannelId;
	public String mIconUrl;
	public String mCateUrl;
	public int mCid;
	public int mCateCode;
	
	/*
	"channeled":"1000021000",
	"icon":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_episode_4.7.1.png",
	"name":"电视剧",
	"cate_api":"http://api.tv.sohu.com/v4/category/teleplay.json?",
	"cid":2,
	"cate_code":101,
	"icon_selected":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_episode_4.7.1_selected.png",
	*/
	
	public ChannelSohu(String title, String channelId, String iconUrl, String cate_api, 
			int cid, int cate_code) {
		mTitle		= title;
		mChannelId	= channelId;
		mIconUrl	= iconUrl;
		mCateUrl	= cate_api;
		mCid		= cid;
		mCateCode	= cate_code;
	}
}
