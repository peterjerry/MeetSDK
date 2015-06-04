package com.pplive.common.sohu;

public class AlbumSohu {
	private int mColumnId;
	private String mColumnName;
	
	private String mTitle;
	private String mTip;
	private String mSecCateName;
	private int mVideoCount;
	private int mLastCount;
	private boolean mIsAlbum;
	
	private long mAid;
	private int mVid;
	private int mCid;
	private int mSite;
	
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
			long video_aid, int v_count, int last_count, 
			String imgHoriUrl, String imgVertUrl,
			boolean is_album, int site) {
		this(0, "", title, sec_cate, v_count, last_count, is_album,
				video_aid, 0, 0, "N/A", "", site,
				0.0f, 0.0f, "N/A", 
				"N/A", main_actor,
				"N/A", "N/A",
				imgHoriUrl, imgVertUrl, "",
				0);
	}
	
	public AlbumSohu(String title, int count, long aid, int vid, 
			boolean is_album, int site, String desc) {
		this(0, "", title, "", count, 0, is_album,
				aid, vid, 0, desc, "", site,
				0.0f, 0.0f, "N/A",
				"N/A", "N/A",
				"N/A", "N/A",
				"", "", "",
				0);
	}

	public AlbumSohu(int column_id, String column_name, 
			String title, String sec_cate, int count, int last_count, boolean is_album,
			long aid, int vid, int cid, String desc, String tip, int site,
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
		mIsAlbum		= is_album;
		
		mAid			= aid;
		mVid			= vid;
		mCid			= cid;
		mDesc			= desc;
		mTip			= tip;
		mSite			= site;
		
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
	
	public int getLastCount() {
		return mLastCount;
	}
	
	public int getVideoCount() {
		return mVideoCount;
	}
	
	public Boolean isAlbum() {
		return mIsAlbum;
	}
	
	public int getSite() {
		return mSite;
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
		sb.append(" 合集: ");
		if (mIsAlbum)
			sb.append("yes");
		else
			sb.append("no");
		
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
