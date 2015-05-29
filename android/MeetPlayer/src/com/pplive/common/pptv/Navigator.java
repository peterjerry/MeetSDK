package com.pplive.common.pptv;

public class Navigator { 
	private String mName;
	private String mId;
	private int mCount;

	private Navigator() {
		
	}
	
	public Navigator(String name, String id, int count) {
		mName 			= name;
		mId				= id;
		mCount			= count;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getName() {
		return mName;
	}
	
	public int getCount() {
		return mCount;
	}
}
