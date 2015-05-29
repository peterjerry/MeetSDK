package com.pplive.common.pptv;

public class Module {
	private int mIndex;
	private String mTitle;
	private String mTarget;
	private String mLink;
	
	@SuppressWarnings("unused")
	private Module() {
		
	}
	
	public Module(int index, String title) {
		this(index, title, "", "");
	}
	
	public Module(int index, String title, String target, String link) {
		this.mIndex		= index;
		this.mTitle		= title;
		this.mTarget	= target;
		this.mLink		= link;
	}
	
	public int getIndex() {
		return mIndex;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getLink() {
		return mLink;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("index: ");
		sb.append(mIndex);
		sb.append(", title: ");
		sb.append(mTitle);
		sb.append(", target: ");
		sb.append(mTarget);
		sb.append(", link: ");
		sb.append(mLink);
		
		return sb.toString();
	}
}
