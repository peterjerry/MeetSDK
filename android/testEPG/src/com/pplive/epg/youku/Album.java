package com.pplive.epg.youku;

public class Album { 
	private String mTitle;
	private String mStripe;
	private String mVideoId;
	private String mShowId; // showid == tid
	private String mImg;
	private String mDesc;
	private String mTotalVV;
	private String mShowDate;
	private String mActor;
	
	@SuppressWarnings("unused")
	private Album() {
		
	}
	
	public Album(String title, String tid, String stripe) {
		this(title, tid/*show_id*/, stripe, null, null, null, null, null, null);
	}
	
	public Album(String title, String show_id, 
			String stripe, String vid, 
			String img, String total_vv, String show_date,
			String desc, String actor) {
		this.mTitle		= title;
		this.mShowId	= show_id;
		this.mStripe	= stripe;
		this.mVideoId	= vid;
		this.mImg		= img;
		this.mDesc		= desc;
		this.mTotalVV	= total_vv;
		this.mShowDate	= show_date;
		this.mActor		= actor;
	}
	
	public String getVideoId() {
		return mVideoId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getShowId() {
		return mShowId;
	}
	
	public String getStripe() {
		return mStripe;
	}
	
    public String getImgUrl() {
        return mImg;
    }

    public String getTotalVV() {
        return mTotalVV;
    }
    
    public String getDescription() {
        return mDesc;
    }
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		if (mVideoId != null) {
			sb.append(", videoId: ");
			sb.append(mVideoId);
		}
		if (mShowId != null) {
			sb.append(", showId: ");
			sb.append(mShowId);
		}
		if (mStripe != null) {
			sb.append(", stripe: ");
			sb.append(mStripe);
		}
		if (mDesc != null) {
			sb.append(", description: ");
			sb.append(mDesc);
		}
		if (mActor != null) {
			sb.append(", actor: ");
			sb.append(mActor);
		}
		if (mImg != null) {
			sb.append(", img_url: ");
			sb.append(mImg);
		}
		
		return sb.toString();
	}
}
