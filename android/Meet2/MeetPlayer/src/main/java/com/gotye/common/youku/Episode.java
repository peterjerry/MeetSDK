package com.gotye.common.youku;

import java.util.List;

public class Episode { 
	private String mTitle;
	private String mVideoId;
	private String mThumbUrl;
	private String mOnlineTime;
	private String mTotalVV;
	private List<String> mStrmTypeList;
	
	@SuppressWarnings("unused")
	private Episode() {
		
	}
	
	public Episode(String title, String vid, List<String> strm_type_list) {
		this(title, vid, null, null, null, strm_type_list);
	}

	public Episode(String title, String vid, String thumb_url,
				   String online_time, String total_vv, List<String> strm_type_list) {
		this.mTitle			= title;
		this.mVideoId		= vid;
        this.mThumbUrl      = thumb_url;
        this.mOnlineTime    = online_time;
        this.mTotalVV       = total_vv;
		this.mStrmTypeList	= strm_type_list;
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

    public String getThumbUrl() {
        return mThumbUrl;
    }

	public List<String> getStrmList() {
		return mStrmTypeList;
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
        if (mStrmTypeList != null) {
            sb.append(", stream type: ");
            sb.append(mStrmTypeList.toString());
        }
		
		return sb.toString();
	}
}
