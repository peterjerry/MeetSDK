package com.gotye.meetplayer.activity;

import java.util.List;
import java.util.Locale;

import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.Episode;
import com.gotye.common.sohu.AlbumSohu;
import com.gotye.common.sohu.EpisodeSohu;
import com.gotye.common.sohu.PlaylinkSohu;
import com.gotye.common.sohu.PlaylinkSohu.SohuFtEnum;
import com.gotye.common.sohu.SohuUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.util.Util;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class PlaySohuActivity extends PlaySegFileActivity {
	private final static String TAG = "PlaySohuActivity";

	private int mInfoId, mIndex;
	private long mAid;
	private int mVid;
	private int mSite = -1;

	private EPGUtil mEPG;
	private List<Episode> mVirtualLinkList;
	
	private SohuUtil mSohu;
	private List<EpisodeSohu> mEpisodeList;
	private int sohu_page_index = -1;
	private int sohu_episode_cnt = -1;
	final private int sohu_page_size = 10;
	
	private final static int LIST_PPTV = 1;
	private final static int LIST_SOHU = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if (intent.hasExtra("url_list") && intent.hasExtra("duration_list")) {
			mInfoId				= intent.getIntExtra("info_id", -1);
    		mIndex				= intent.getIntExtra("index", -1);
    		
    		// for sohu
    		mAid				= intent.getLongExtra("aid", -1);
            mVid                = intent.getIntExtra("vid", -1);
    		mSite				= intent.getIntExtra("site", -1);
		}
		else {
			LogUtil.warn(TAG, "Java: use test url and duration list");

			mInfoId				= -1;
    		mIndex				= -1;
		}

		mSohu = new SohuUtil();
	}

    @Override
    protected void OnComplete() {
        if (mInfoId != -1) {
            new NextEpisodeTask().execute(LIST_PPTV, 1);
            return;
        }
        else if (mAid != -1) {
            new NextEpisodeTask().execute(LIST_SOHU, 1);
            return;
        }

        Toast.makeText(PlaySohuActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
        mIsBuffering = false;
        mBufferingProgressBar.setVisibility(View.GONE);

        finish();
    }

    @Override
    protected void onSelectEpisode(int incr) {
        if (mInfoId != -1) {
            new NextEpisodeTask().execute(LIST_PPTV, incr);
        }
        else if (mAid != -1) {
            new NextEpisodeTask().execute(LIST_SOHU, incr);
        }
    }

	@Override
	protected void push_cdn_clip() {
		// http://www.sohu.com?aid=100000000&site=123&streamtype=sohu
		String push_url = String.format(Locale.US,
				"http://www.sohu.com?aid=%d&vid=%d&site=%d&streamtype=sohu",
				mAid, mVid, mSite);
		LogUtil.info(TAG, "push_url: " + push_url);

		Intent intent = new Intent(PlaySohuActivity.this, DMCActivity.class);
		intent.putExtra("title", mTitle);
		intent.putExtra("push_url", push_url);
		intent.putExtra("dmr_uuid", mDlnaDeviceUUID);
		startActivity(intent);

		finish();
	}
	
	private class NextEpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			mSwichingEpisode = false;
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			
			int action = params[0];
			int incr = params[1];
			
			PlaylinkSohu l = null;
			
			if (action == LIST_PPTV) {
				if (mVirtualLinkList == null) {
					mEPG = new EPGUtil();
					boolean ret;
					ret = mEPG.virtual_channel(mTitle, mInfoId, 500, 3/*sohu*/, 1);
					if (!ret) {
						LogUtil.error(TAG, "failed to get virtual_channel");
						return false;
					}
			
					mVirtualLinkList = mEPG.getVirtualLink();
				}
				
				mIndex += incr;
				if (mIndex < 0 || mIndex > mVirtualLinkList.size() - 1) {
                    LogUtil.info(TAG, String.format("Java: meet end %d %d", mIndex, mVirtualLinkList.size()));
					mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
					return false;
				}
				
				Episode e = mVirtualLinkList.get(mIndex);
				String ext_id = e.getExtId();
				int pos = ext_id.indexOf('|');
	    		String sid = ext_id.substring(0, pos);
	    		String vid = ext_id.substring(pos + 1, ext_id.length());
				
	    		l = mSohu.playlink_pptv(Integer.valueOf(vid), Integer.valueOf(sid));
			}
			else if (action == LIST_SOHU) {
				if (sohu_episode_cnt == -1) {
					AlbumSohu al = mSohu.album_info(mAid);
					if (al == null) {
						LogUtil.error(TAG, String.format("Java: failed to get album info"));
						mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_ALBUM_INFO);
						return false;
					}
					
					sohu_episode_cnt = al.getLastCount();
                    LogUtil.info(TAG, "Java: sohu_episode_cnt " + sohu_episode_cnt);
				}
				
				mIndex += incr;
				if (mIndex < 0 || mIndex > sohu_episode_cnt - 1) {
                    LogUtil.error(TAG, String.format("Java: mIndex is invlaid %d, sohu_episode_cnt %d",
                            mIndex, sohu_episode_cnt));
					mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
					return false;
				}
				
				int page_index  = mIndex / sohu_page_size + 1;
				
				if (mEpisodeList == null || page_index != sohu_page_index) {
					boolean ret;
					ret = mSohu.episode(mAid, mIndex / sohu_page_size + 1, sohu_page_size);
					if (!ret) {
						LogUtil.error(TAG, "failed to get virtual_channel");
						return false;
					}
			
					mEpisodeList = mSohu.getEpisodeList();
				}

				int pos = mIndex - (page_index - 1) * sohu_page_size;
				EpisodeSohu ep = mEpisodeList.get(pos);
				
				if (mAid > 1000000000000L)
					l = mSohu.video_info(mSite, ep.mVid, mAid);
				else
					l = mSohu.playlink_pptv(ep.mVid, 0);
				
				Util.add_sohuvideo_history(PlaySohuActivity.this, 
						l.getTitle(), ep.mVid, mAid, mSite);
			}
	    		
    		if (l == null) {
    			LogUtil.error(TAG, "Failed to get next video");
    			mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_PLAYLINK);
        		return false;
    		}
    		
    		mTitle = l.getTitle();
    		
    		SohuFtEnum ft = SohuFtEnum.SOHU_FT_ORIGIN;
			mUrlListStr = l.getUrl(ft);
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_SUPER;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_HIGH;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_NORMAL;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_STREAM);
    			return false;
    		}
    		
    		mDurationListStr	= l.getDuration(ft);
			
			buildPlaylinkList();
			
			mHandler.sendEmptyMessage(MainHandler.MSG_PLAY_NEXT_EPISODE);
			
			return true;
		}
		
	}

}
