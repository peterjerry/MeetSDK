package com.pplive.common.bestv;

public class BestvChannel {

	private String mTitle;			// 第一财经
	private String mChannelAbbr;	// dycj
	private String mChannelCode;	// Umai:CHAN/1314@BESTV.SMG.SMG
	private String mChannelID;
	private String mChannelNumber;
	private String mIconUrl;
	private String mSnapshotUrl;
	private String mPlayUrl;
	
	BestvChannel(String title, String iconUrl, String PlayUrl) {
		this(title, "N/A", "N/A", "N/A", "0", iconUrl, "NOPIC", PlayUrl);
	}
	
	BestvChannel(String title, String abbr, String code, String id, String number, 
			String iconUrl, String SnapshotUrl, String PlayUrl) {
		mTitle 			= title;
		mChannelAbbr	= abbr;
		mChannelCode	= code;
		mChannelID		= id;
		mChannelNumber	= number;
		mIconUrl		= iconUrl;
		mSnapshotUrl	= SnapshotUrl;
		mPlayUrl		= PlayUrl;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getSnapshotUrl() {
		return mSnapshotUrl;
	}
	
	public String getIconUrl() {
		return mIconUrl;
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
		sb.append(mIconUrl);
		sb.append(" , 预览");
		sb.append(mSnapshotUrl);
		sb.append(" , 播放地址");
		sb.append(mPlayUrl);
		
		
		return sb.toString();
	}
}
