package com.pplive.epg;

public class Catalog { 
	private int mIndex;
	private String mId;
	private String mTitle;

	private String mLink;
	
	private Catalog() {
		
	}
	
	public Catalog(int index, String id, String title) {
		mIndex 			= index;
		mId				= id;
		mTitle			= title;
	}
	
	public Catalog(String title, String link) {
		mIndex			= -1;
		mId				= "N/A";
		mTitle			= title;
		mLink			= link;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getVid() {
		if (mLink == null || mLink.isEmpty())
			return null;
		
		int pos1, pos2;
		pos1 = mLink.indexOf("&vid=");
		pos2 = mLink.indexOf("&sid=");
		if (pos1 == -1 || pos2 == -1)
			return null;
         
		String vid = mLink.substring(pos1 + 5, pos2);
		return vid;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getIndex() {
		return mIndex;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		
		if (mIndex != -1) {
			sb.append(", #");
			sb.append(mIndex);
			sb.append(", id: ");
			sb.append(mId);
		}
		
		if (mLink != null) {
			sb.append(", vid: ");
			sb.append(getVid());
		}
		
		return sb.toString();
	}
}
