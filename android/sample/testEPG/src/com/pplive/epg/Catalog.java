package com.pplive.epg;

public class Catalog { 
	private String mTitle;
	private String mTarget;
	private String mLink;
	
	@SuppressWarnings("unused")
	private Catalog() {
		
	}
	
	public Catalog(String title, String target, String link) {
		this.mTitle		= title;
		this.mTarget	= target;
		this.mLink		= link;
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", vid: ");
		sb.append(getVid());
		
		return sb.toString();
	}

	public String getTarget() {
		return mTarget;
	}
}
