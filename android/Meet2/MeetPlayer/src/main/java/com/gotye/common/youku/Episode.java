package com.gotye.common.youku;

import java.util.List;

public class Episode { 
	private String mTitle;
	private String mVideoId;
	private String mThumbUrl;
	private String mOnlineTime;
	private String mTotalVV;
	private String mDuration;
	private List<String> mStrmTypeList;
	private String mDescrition;
	
	@SuppressWarnings("unused")
	private Episode() {
		
	}
	
	public Episode(String title, String vid, List<String> strm_type_list) {
		this(title, vid, null, null, null, null, strm_type_list, null);
	}

	public Episode(String title, String vid, String thumb_url,
				   String online_time, String total_vv,
				   String duration, List<String> strm_type_list,
				   String description) {
		this.mTitle			= title;
		this.mVideoId		= vid;
        this.mThumbUrl      = thumb_url;
        this.mOnlineTime    = online_time;
        this.mTotalVV       = total_vv;
		this.mDuration		= duration;
		this.mStrmTypeList	= strm_type_list;
		this.mDescrition	= description;
	}
	
	public String getVideoId() {
		return mVideoId;
	}
	
	public String getTitle() {
		return mTitle;
	}

    public String getTotalVV() {
        return mTotalVV;
    }

    public String getOnlineTime() {
        return mOnlineTime;
    }

	public String getDuration() {
		return mDuration;
	}

    public String getThumbUrl() {
        return mThumbUrl;
    }

	public List<String> getStrmList() {
		return mStrmTypeList;
	}

    public String getDescrition() {
        return mDescrition;
    }

    public void setDescrition(String desc) {
        this.mDescrition = desc;
    }
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: ");
		sb.append(mTitle);
		sb.append(", videoId: ");
		sb.append(mVideoId);
        if (mThumbUrl != null) {
            sb.append(", thumb_url: ");
            sb.append(mThumbUrl);
        }
        if (mOnlineTime != null) {
            sb.append(", online_time: ");
            sb.append(mOnlineTime);
        }
        if (mTotalVV != null) {
            sb.append(", total_vv: ");
            sb.append(mTotalVV);
        }
		if (mDuration != null) {
			sb.append(", duration: ");
			sb.append(mDuration);
		}
        if (mStrmTypeList != null) {
            sb.append(", stream type: ");
            sb.append(mStrmTypeList.toString());
        }
        if (mDescrition != null) {
            sb.append(", desc: ");
            sb.append(mDescrition);
        }
		
		return sb.toString();
	}
}
