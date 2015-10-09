package com.pplive.epg.shooter;

public class SearchItem {

	public String mNativeName;
	public String mVideoName;
	public int mId;
	public String mLangDesc;
	
	public SearchItem(String nativeName, String videoName, int id) {
		this(nativeName, videoName, id, "N/A");
	}
	
	public SearchItem(String nativeName, String videoName, int id, String langDesc) {
		mNativeName	= nativeName;
		mVideoName	= videoName;
		mId			= id;
		mLangDesc	= langDesc;
	}
}
