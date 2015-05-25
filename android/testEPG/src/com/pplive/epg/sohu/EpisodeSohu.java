package com.pplive.epg.sohu;

public class EpisodeSohu {
	public String mTitle;
	public String mPicUrl;
	public int mAid;
	public int mVid;
	public String mPlayUrl;
	
	public EpisodeSohu(String title, String picUrl, int aid, int vid, String url) {
		mTitle		= title;
		mPicUrl		= picUrl;
		mAid		= aid;
		mVid		= vid;
		mPlayUrl	= url;
	}
}
