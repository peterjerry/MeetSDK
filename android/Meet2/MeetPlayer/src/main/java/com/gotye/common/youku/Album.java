package com.gotye.common.youku;

public class Album {
    private String mTitle;
    private String mStripe;
    private String mShowId;
    private String mImg;
    private String mDesc;
    private String mTotalVV;
    private String mShowDate;
    private String mActor;
    private String mVideoId;
    private int mEpisodeTotal;

    @SuppressWarnings("unused")
    private Album() {

    }

    public Album(String title, String tid, String stripe,
                 String img, String totoal_vv, int total_episode) {
        this(title, tid/*showid*/, stripe,
                img, totoal_vv, null,
                null, null, null, total_episode);
    }

    public Album(String title, String show_id, String stripe,
                 String img, String total_vv, String show_date,
                 String desc, String actor, String video_id, int total_episode) {
        this.mTitle		= title;
        this.mShowId	= show_id;
        this.mStripe	= stripe;
        this.mImg		= img;
        this.mDesc		= desc;
        this.mTotalVV	= total_vv;
        this.mShowDate	= show_date;
        this.mActor		= actor;
        this.mVideoId   = video_id;
        this.mEpisodeTotal = total_episode;
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

    public String getActor() {
        return mActor;
    }

    public String getVid() {
        return mVideoId;
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
        if (mEpisodeTotal != 0) {
            sb.append(", total_ep: ");
            sb.append(mEpisodeTotal);
        }
        if (mTotalVV != null) {
            sb.append(", total_vv: ");
            sb.append(mTotalVV);
        }

        return sb.toString();
    }
}
