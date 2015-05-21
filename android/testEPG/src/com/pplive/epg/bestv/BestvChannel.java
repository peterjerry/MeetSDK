package com.pplive.epg.bestv;

public class BestvChannel {

	private String mTitle;			// 第一财经
	private String mChannelAbbr;	// dycj
	private String mChannelCode;	// Umai:CHAN/1314@BESTV.SMG.SMG
	private String mChannelID;
	private String mChannelNumber;
	private String mImgUrl;
	private String mPlayUrl;
	
	BestvChannel(String title, String ImgUrl, String PlayUrl) {
		this(title, "N/A", "N/A", "N/A", "0", ImgUrl, PlayUrl);
	}
	
	BestvChannel(String title, String abbr, String code, String id, String number, String ImgUrl, String PlayUrl) {
		mTitle 			= title;
		mChannelAbbr	= abbr;
		mChannelCode	= code;
		mChannelID		= id;
		mChannelNumber	= number;
		mImgUrl			= ImgUrl;
		mPlayUrl		= PlayUrl;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getImgUrl() {
		return mImgUrl;
	}
	
	public String getPlayUrl() {
		return mPlayUrl;
	}
	
	public String getChannelCode() {
		return mChannelCode;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		sb.append("频道名称: ");
		sb.append(mTitle);
		sb.append(" ， 频道缩写: ");
		sb.append(mChannelAbbr);
		sb.append(" , 频道代码: ");
		sb.append(mChannelCode);
		sb.append(" , 频道id");
		sb.append(mChannelID);
		sb.append(" , 频道编号");
		sb.append(mChannelNumber);
		sb.append(" , 台标");
		sb.append(mImgUrl);
		sb.append(" , 播放地址");
		sb.append(mPlayUrl);
		
		
		return sb.toString();
	}
}
