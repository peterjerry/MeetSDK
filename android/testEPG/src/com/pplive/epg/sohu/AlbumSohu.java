package com.pplive.epg.sohu;

public class AlbumSohu {
	private int mColumnId;
	private String mColumnName;
	
	private String mTitle;
	private String mTip;
	private String mSecCateName;
	private int mVideoCount;
	private int mLastCount;
	
	private long mAid;
	private int mVid;
	private int mCid;
	
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
	private AlbumSohu() {
		
	}
	
	public AlbumSohu(String title, String sec_cate, String main_actor,
			long video_aid, int v_count, int last_count) {
		this(0, "", title, sec_cate, v_count, last_count, 
				video_aid, 0, 0, "N/A", "",
				0.0f, 0.0f, "N/A", 
				"N/A", main_actor,
				"N/A", "N/A",
				"", "", "",
				0);
	}
	
	public AlbumSohu(String title, int count, long aid, int vid, String desc) {
		this(0, "", title, "", count, 0, 
				aid, vid, 0, desc, "",
				0.0f, 0.0f, "N/A", 
				"N/A", "N/A",
				"N/A", "N/A",
				"", "", "",
				0);
	}

	public AlbumSohu(int column_id, String column_name, 
			String title, String sec_cate, int count, int last_count,
			long aid, int vid, int cid, String desc, String tip,
			double score, double douban_score, String score_tip, 
			String director, String act, 
			String year, String area,
			String imgHoriUrl, String imgVertUrl, String imgBigUrl,
			int duration_sec) {
		mColumnId		= column_id;
		mColumnName		= column_name;
		
		mTitle 			= title;
		mSecCateName	= sec_cate;
		mVideoCount		= count;
		mLastCount		= last_count;
		
		mAid			= aid;
		mVid			= vid;
		mCid			= cid;
		mDesc			= desc;
		mTip			= tip;
		
		mScore			= score;
		mDoubanScore	= douban_score;
		mScoreTip		= tip;	
		
		mDirector		= director;
		mAct			= act;
		
		mYear			= year;
		mArea			= area;
		
		mHoriImgUrl		= imgHoriUrl;
		mVertImgUrl		= imgVertUrl;
		mBigImgUrl		= imgBigUrl;
		
		mDurationSec	= duration_sec;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getTip() {
		return mTip;
	}
	
	public int getVid() {
		return mVid;
	}
	
	public long getAid() {
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
		sb.append("列: ");
		sb.append(mColumnName);
		sb.append(", 列id: ");
		sb.append(mColumnId);
		sb.append(", 标题: ");
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
