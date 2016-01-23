package com.gotye.common.sohu;

public class EpisodeSohu {
	public String mTitle;
	public String mPicUrl;
	public long mAid;
	public int mVid;
	public String mPlayUrl;
	
	public EpisodeSohu(String title, String picUrl, long aid, int vid, String url) {
		mTitle		= title;
		mPicUrl		= picUrl;
		mAid		= aid;
		mVid		= vid;
		mPlayUrl	= url;
	}
}
