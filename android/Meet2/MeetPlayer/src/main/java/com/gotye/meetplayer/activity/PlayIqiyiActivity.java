package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.gotye.common.ZGUrl;
import com.gotye.common.iqiyi.IqiyiUtil;
import com.gotye.common.util.LogUtil;

/**
 * Created by Michael.Ma on 2016/6/20.
 */
public class PlayIqiyiActivity extends PlaySegFileActivity {
    private final static String TAG = "PlayIqiyiActivity";

    private String mVideoUrl;
    private String mVid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mVid = intent.getStringExtra("vid");
        mVideoUrl = intent.getStringExtra("video_url");

        mHandler.sendEmptyMessageDelayed(MainHandler.MSG_SHEDULE, 300 * 1000);
    }

    @Override
    protected void onShedule() {
        new EPGTask().execute(mVideoUrl);
        mHandler.sendEmptyMessageDelayed(MainHandler.MSG_SHEDULE, 300 * 1000);
    }

    private class EPGTask extends AsyncTask<String, Integer, Boolean> {
        private ZGUrl zgUrl;

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mUrlListStr = zgUrl.urls;
                mDurationListStr = zgUrl.durations;
                PlayIqiyiActivity.this.buildPlaylinkList();
                LogUtil.info(TAG, "iqiyi play list updated");
            }
            else {
                LogUtil.error(TAG, "failed to update iqiyi play list");
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            zgUrl = IqiyiUtil.getPlayZGUrl(params[0], 2);
            return (zgUrl != null);
        }
    }
}
