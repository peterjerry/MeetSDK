package com.pplive.epg.sohu;

public class ClipSohu {
	private String mTitle;
	private String mSecCateName;
	private int mCount;
	private int mAid;
	private int mVid;
	
	private String mHoriImgUrl;
	private String mVertImgUrl;
	private String mBigImgUrl;
	
	private double mScore;
	private double mDoubanScore;
	private String mScoreTip;
	private String mDirector;
	private String mAct;
	private String mYear;
	private String mArea;
	private String mDesc;
	private int mDurationSec;

	@SuppressWarnings("unused")
	private ClipSohu() {
		
	}
	
	public ClipSohu(String title, int aid, int vid, String desc) {
		this(title, "", 1, aid, vid, desc, 
				0.0f, 0.0f, "N/A", 
				"N/A", "N/A",
				"N/A", "N/A",
				"", "", "",
				0);
	}

	public ClipSohu(String title, String sec_cate, int count, 
			int aid, int vid, String desc, 
			double score, double douban_score, String tip, 
			String director, String act, 
			String year, String area,
			String imgHoriUrl, String imgVertUrl, String imgBigUrl,
			int duration_sec) {
		mTitle 			= title;
		mSecCateName	= sec_cate;
		mCount			= count;
		mAid			= aid;
		mVid			= vid;
		
		mScore			= score;
		mDoubanScore	= douban_score;
		mScoreTip		= tip;
		
		mDesc			= desc;
		mDirector		= director;
		mAct			= act;
		
		mYear			= year;
		mArea			= area;
		
		mHoriImgUrl		= imgHoriUrl;
		mVertImgUrl		= imgHoriUrl;
		mBigImgUrl		= imgBigUrl;
		
		mDurationSec	= duration_sec;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getVid() {
		return mVid;
	}
	
	public int getAid() {
		return mAid;
	}
	
	public String getImgUrl(boolean vertical) {
		if (vertical)
			return mVertImgUrl;
		else
			return mHoriImgUrl;
	}
	
	public String getDescription() {
		return mDesc;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("标题: ");
		sb.append(mTitle);
		sb.append(", 分类: ");
		sb.append(mSecCateName);
		sb.append(", aid: ");
		sb.append(mAid);
		sb.append(", vid: ");
		sb.append(mVid);
		sb.append(", 描述: ");
		sb.append(mDesc);
		sb.append(", 评分: ");
		sb.append(mScore);
		sb.append(", 豆瓣评分: ");
		sb.append(mDoubanScore);
		sb.append(", 导演: ");
		sb.append(mDirector);
		sb.append(", 主演: ");
		sb.append(mAct);
		sb.append(", 年份: ");
		sb.append(mYear);
		sb.append(", 地区: ");
		sb.append(mArea);
		sb.append(", 时长: ");
		sb.append(mDurationSec);
		
		return sb.toString();
	}
}
