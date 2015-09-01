package com.pplive.epg.letv;

import java.util.List;

public class VideoInfo {
	private int mVid;
	private int mCid;
	private int mPid;
	private String mTitle;
	private String mSubTitle;
	private int mDurationSec;
	private List<String> mBrList;
	
	public VideoInfo(int vid, int cid, int pid, String title, String subtitle, 
			int duration, List<String>br_list) {
		mVid 		= vid;
		mCid 		= cid;
		mPid		= pid;
		mTitle		= title;
		mSubTitle	= subtitle;
		mDurationSec= duration;
		mBrList		= br_list;
	}
	
	public int getVid() {
		return mVid;
	}
	
	public int getCid() {
		return mCid;
	}
	
	public int getPid() {
		return mPid;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getSubTitle() {
		return mSubTitle;
	}
	
	public List<String> getBrList() {
		return mBrList;
	}
}
