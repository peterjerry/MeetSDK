package com.pplive.common.pptv;

public class PlayLink2 { 
	private String mTitle;
	private String mId;
	private String mDescription;
	private int mVideoCount;
	private String mImgUrl;
	
	private String mMark;
	private String mDirector;
	private String mAct;
	private String mYear;
	private String mArea;
	private String mResolution;
	private int mWidth, mHeight;
	private int mDurationSec;
	
	private String mExtTitle;

	@SuppressWarnings("unused")
	private PlayLink2() {
		
	}
	
	public PlayLink2(String title, String id, String desc) {
		this(title, "", id, desc,
				0, "",
				"", "", "", 
				"", "",
				"", 0);
	}
	
	public PlayLink2(String title, String id, String desc, String imgUrl) {
		this(title, "", id, desc, 
				0, imgUrl,
				"", "", "", 
				"", "",
				"", 0);
	}

	public PlayLink2(String title, String ext_title, String id, String desc, 
			int video_count, String img_url, 
			String mark, String director, String act, 
			String year, String area,
			String resolution, int duration_sec) {
		mTitle 			= title;
		mExtTitle		= ext_title;
		mId				= id;
		mDescription	= desc;
		
		mVideoCount		= video_count;
		mImgUrl			= img_url;
		
		mMark			= mark;
		mDirector		= director;
		mAct			= act;
		
		mYear			= year;
		mArea			= area;
		
		mResolution		= resolution;
		mDurationSec	= duration_sec;
		
		setResolution();
	}
	
	public String getId() {
		return mId;
	}
	
	public String getTitle() {
		// fix catalog main title + sub title
		if (mExtTitle != null && mExtTitle.length() > 5)
			return mExtTitle;
		
		StringBuffer sb = new StringBuffer();
		sb.append(mTitle);
		if (mExtTitle != null && !mExtTitle.isEmpty()) {
			sb.append("(");
			sb.append(mExtTitle);
			sb.append(")");
		}
		
		return sb.toString();
	}
	
	public String getImgUrl() {
		return mImgUrl;
	}
	
	public int getVideoCount() {
		return mVideoCount;
	}
	
	public String getArea() {
		return mArea;
	}
	
	public String getDescription() {
		return mDescription;
	}
	
	public String getResolution() {
		return mResolution;
	}
	
	private void setResolution() {
		if (mResolution == null || mResolution.isEmpty())
			return;
		
    	int pos;
    	pos = mResolution.indexOf('|');
    	if (pos == -1) {
    		mWidth = mHeight = 0;
    	}
    	else {
    		mWidth = Integer.valueOf(mResolution.substring(0, pos));
    		mHeight = Integer.valueOf(mResolution.substring(pos + 1, mResolution.length()));
    	}
	}
	
	public int getWidth() {
    	return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public int getDuration() {
		return mDurationSec;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("标题: ");
		sb.append(getTitle());
		sb.append(", id: ");
		sb.append(mId);
		sb.append(", 描述: ");
		sb.append(mDescription);
		sb.append(", mark: ");
		sb.append(mMark);
		sb.append(", 导演: ");
		sb.append(mDirector);
		sb.append(", 主演: ");
		sb.append(mAct);
		sb.append(", 年份: ");
		sb.append(mYear);
		sb.append(", 地区: ");
		sb.append(mArea);
		sb.append(", 图片预览: ");
		sb.append(mImgUrl);
		sb.append(", 集数: ");
		sb.append(mVideoCount);
		sb.append(", 分辨率: ");
		sb.append(String.format("%dx%d", mWidth, mHeight));
		sb.append(", 时长: ");
		sb.append(mDurationSec);
		sb.append("秒");
		
		return sb.toString();
	}
}
