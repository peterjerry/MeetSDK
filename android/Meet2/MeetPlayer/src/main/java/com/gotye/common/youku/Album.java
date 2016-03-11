package com.gotye.common.youku;

public class Album {
    private String mTitle;
    private String mStripe;
    private String mVideoId;
    private String mShowId;
    private String mTid;
    private String mImg;
    private String mDesc;
    private String mTotalVV;
    private String mShowDate;
    private String mActor;
    private int mEpisodeTotal;

    @SuppressWarnings("unused")
    private Album() {

    }

    public Album(String title, String vid, String show_id,
                 String stripe, String img, String total_vv,
                 String show_date, String desc, String actor, int total_episode) {
        this(title, show_id, stripe, null, vid, img, total_vv,
                show_date, desc, actor, total_episode);
    }

    public Album(String title, String tid, String stripe) {
        this(title, null, stripe, tid, null, null, null, null, null, null, 1);
    }

    public Album(String title, String show_id,
                 String stripe, String tid, String vid,
                 String img, String total_vv, String show_date,
                 String desc, String actor, int total_episode) {
        this.mTitle		= title;
        this.mShowId	= show_id;
        this.mStripe	= stripe;
        this.mTid		= tid;
        this.mVideoId	= vid;
        this.mImg		= img;
        this.mDesc		= desc;
        this.mTotalVV	= total_vv;
        this.mShowDate	= show_date;
        this.mActor		= actor;
        this.mEpisodeTotal = total_episode;
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

    public String getTid() {
        return mTid;
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

    public String getActor() {
        return mActor;
    }

    public int getEpisodeTotal() {
        return mEpisodeTotal;
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
        if (mTid != null) {
            sb.append(", tid: ");
            sb.append(mTid);
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
        if (mEpisodeTotal != 0) {
            sb.append(", total: ");
            sb.append(mEpisodeTotal);
        }

        return sb.toString();
    }
}
