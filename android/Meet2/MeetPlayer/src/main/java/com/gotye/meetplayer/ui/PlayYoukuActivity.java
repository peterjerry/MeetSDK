package com.gotye.meetplayer.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;

import java.util.List;

public class PlayYoukuActivity extends PlaySegFileActivity {
	private final static String TAG = "PlayYoukuActivity";

	private String mShowId;
    private int mEpisodeIndex;
    private int mPageIndex;
    private final static int page_size = 10;
	private List<Episode> mEpisodeList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
        mShowId				= intent.getStringExtra("show_id");
        mEpisodeIndex		= intent.getIntExtra("index", -1);
        mPageIndex	        = intent.getIntExtra("page_index", 1);
	}

    @Override
    protected void OnComplete() {
        if (mShowId != null && mEpisodeIndex != -1) {
            new NextEpisodeTask().execute(1);
            return;
        }

        Toast.makeText(PlayYoukuActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
        mIsBuffering = false;
        mBufferingProgressBar.setVisibility(View.GONE);

        finish();
    }

    @Override
    protected void onSelectEpisode(int incr) {
        if (mShowId != null && mEpisodeIndex != -1)
            new NextEpisodeTask().execute(incr);
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

			int incr = params[0];

            if (mEpisodeList == null)
                mEpisodeList = YKUtil.getEpisodeList(mShowId, mPageIndex, page_size);

            if (mEpisodeList == null) {
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                return false;
            }

            mEpisodeIndex += incr;
            if (mEpisodeIndex < 0 || mEpisodeIndex >= mEpisodeList.size()) {
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                return false;
            }

            Episode ep = mEpisodeList.get(mEpisodeIndex);
            YKUtil.ZGUrl zg = YKUtil.getZGUrls(ep.getVideoId());
            if (zg == null) {
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                return false;
            }

            mTitle = ep.getTitle();
            mUrlListStr = zg.urls;
            mDurationListStr = zg.durations;

			buildPlaylinkList();
			
			mHandler.sendEmptyMessage(MainHandler.MSG_PLAY_NEXT_EPISODE);
			
			return true;
		}
		
	}

}
