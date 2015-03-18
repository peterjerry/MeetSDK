package com.pplive.epg;

public class Content {
	private String mId;
	private String mTitle;
	private String mParam;
	private String mPosition;
	
	@SuppressWarnings("unused")
	private Content() {
		
	}
	
	public Content(String id, String title, String param, String pos) {
		this.mId		= id;
		this.mTitle		= title;
		this.mParam		= param;
		this.mPosition	= pos;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getParam() {
		return mParam;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("id: ");
		sb.append(mId);
		sb.append(", title: ");
		sb.append(mTitle);
		sb.append(", param: ");
		sb.append(mParam);
		sb.append(", pos: ");
		sb.append(mPosition);
		
		return sb.toString();
	}
}
