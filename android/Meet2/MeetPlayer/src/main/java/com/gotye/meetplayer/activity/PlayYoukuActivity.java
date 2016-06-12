package com.gotye.meetplayer.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class PlayYoukuActivity extends PlaySegFileActivity {
	private final static String TAG = "PlayYoukuActivity";

	private String mShowId;
    private String mVid;
    private int mEpisodeIndex;
    private final static int page_size = 10;
	private List<Episode> mEpisodeList;

    private String search_key;
    private int search_orderby = 1; // orderby 1-综合排序 2-最新发布 3-最多播放
    private int search_page_index = -1;
    private String search_filter;
    private final static int search_page_size = 20;
    private int search_index = -1; // index in page

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent   = getIntent();
        mShowId			= intent.getStringExtra("show_id");
        mVid            = intent.getStringExtra("vid");
        mEpisodeIndex	= intent.getIntExtra("episode_index", -1);

        if (intent.hasExtra("search_key")) {
            search_key = intent.getStringExtra("search_key");
            search_filter = intent.getStringExtra("search_filter");
            search_orderby = intent.getIntExtra("search_orderby", 1);
            search_page_index = intent.getIntExtra("search_page_index", -1);
            search_index = intent.getIntExtra("search_index", -1);

            LogUtil.info(TAG, String.format(Locale.US,
                    "play in search mode: key: %s, filter: %s, orderby %d, page_index #%d, index #%d",
                    search_key, search_filter, search_orderby, search_page_index, search_index));
        }
	}

    @Override
    protected void OnComplete() {
        YKPlayhistoryDatabaseHelper.getInstance(this).savePlayedPosition(mVid, 0);

        if ((mShowId != null && mEpisodeIndex != -1) ||
                (search_key != null && search_page_index != -1 && search_index != -1 )) {
            new NextEpisodeTask().execute(NextEpisodeTask.ACTION_EPISODE_INCR, 1);
            return;
        }

        Toast.makeText(PlayYoukuActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
        mIsBuffering = false;
        mBufferingProgressBar.setVisibility(View.GONE);

        finish();
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            int pos = mPlayer.getCurrentPosition() - 10000;
            if (pos > 10000) {
                YKPlayhistoryDatabaseHelper.getInstance(this)
                        .savePlayedPosition(mVid, pos);
            }
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.seg_player_menu, menu);
        return true;
    }

    @Override
    protected void onShowRelateVideo() {
        new RelateVideoTask().execute();
    }

    private void popupSelectEpDlg() {
        List<String> titleList = new ArrayList<String>();
        for (int i=0;i<mEpisodeList.size();i++) {
            titleList.add(mEpisodeList.get(i).getTitle());
        }
        final String []title = titleList.toArray(new String[titleList.size()]);

        Dialog choose_ep_dlg = new AlertDialog.Builder(PlayYoukuActivity.this)
                .setTitle("选择集数")
                .setSingleChoiceItems(title, mEpisodeIndex, /*default selection item number*/
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton){
                                new NextEpisodeTask().execute(
                                        NextEpisodeTask.ACTION_EPISODE_INDEX, whichButton);

                                dialog.dismiss();
                            }
                        })
                .create();
        choose_ep_dlg.show();
    }

    private void stopPlayer() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onSelectEpisode() {
        if (mEpisodeList == null) {
            stopPlayer();
            new NextEpisodeTask().execute(NextEpisodeTask.ACTION_LIST_EPISODE);
        }
        else if (mEpisodeList.isEmpty()) {
            Toast.makeText(this, "选集列表为空", Toast.LENGTH_SHORT).show();
        }
        else {
            popupSelectEpDlg();
        }
    }

    @Override
    protected void onSelectEpisode(int incr) {
        if ((mShowId != null && mEpisodeIndex != -1) || search_key != null) {
            new NextEpisodeTask().execute(NextEpisodeTask.ACTION_EPISODE_INCR, incr);
        }
        else {
            LogUtil.warn(TAG, "NO episode available");
        }
    }

    @Override
    protected void onSelectFt() {
        new PlayLinkTask().execute();
    }

    private class RelateVideoTask extends AsyncTask<Integer, Integer, Boolean> {
        private ProgressDialog mProgressDlg;
        private YKUtil.MixResult mResult;

        @Override
        protected void onPreExecute() {
            mProgressDlg = new ProgressDialog(PlayYoukuActivity.this);
            mProgressDlg.setMessage("相关视频获取中...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            mProgressDlg.dismiss();

            if (result) {
                if (mResult.mEpisodeList != null && mResult.mEpisodeList.size() > 0)
                    popupRelate();
                else
                    Toast.makeText(PlayYoukuActivity.this, "相关视频列表为空", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(PlayYoukuActivity.this, "获取相关视频失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            // TODO Auto-generated method stub
            mResult = YKUtil.relate(mVid, 1);

            return (mResult != null);
        }

        private void popupRelate() {
            List<String> listTitle = new ArrayList<>();

            final List<Episode> relateList = mResult.mEpisodeList;
            for (int i=0;i<relateList.size();i++) {
                Episode ep = relateList.get(i);
                listTitle.add(ep.getTitle());
            }
            final String[] title = listTitle.toArray(new String[listTitle.size()]);

            Dialog choose_relate_dlg = new AlertDialog.Builder(PlayYoukuActivity.this)
                    .setTitle("选择相关视频")
                    .setNegativeButton("取消", null)
                    .setItems(title,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Episode ep = relateList.get(whichButton);
                                    mVid = ep.getVideoId();
                                    mTitle = ep.getTitle();
                                    LogUtil.info(TAG, "ready to parse relate video: " + mVid);
                                    new PlayLinkTask().execute();
                                    dialog.dismiss();
                                }
                            })
                    .create();
            choose_relate_dlg.show();
        }
    }

    private class PlayLinkTask extends AsyncTask<Integer, Integer, Boolean> {
        private ProgressDialog mProgressDlg;

        @Override
        protected void onPreExecute() {
            mProgressDlg = new ProgressDialog(PlayYoukuActivity.this);
            mProgressDlg.setMessage("播放地址解析中...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            mProgressDlg.dismiss();
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            // TODO Auto-generated method stub
            YKUtil.ZGUrl zg = YKUtil.getPlayZGUrl(PlayYoukuActivity.this, mVid, mFt);
            if (zg == null) {
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_FT);
                return false;
            }

            mUrlListStr = zg.urls;
            mDurationListStr = zg.durations;
            buildPlaylinkList();

            mHandler.sendEmptyMessage(MainHandler.MSG_PLAY_NEXT_EPISODE);
            return true;
        }
    }

	private class NextEpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

        private final static int ACTION_EPISODE_INCR    = 1;
        private final static int ACTION_EPISODE_INDEX   = 2;
        private final static int ACTION_LIST_EPISODE    = 3;

        private int action;

        private ProgressDialog mProgressDlg;

        @Override
        protected void onPreExecute() {
            mProgressDlg = new ProgressDialog(PlayYoukuActivity.this);
            mProgressDlg.setMessage("播放地址解析中...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.show();
        }

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
            mProgressDlg.dismiss();

            mSwichingEpisode = false;

            if (result) {
                if (action == ACTION_LIST_EPISODE)
                    popupSelectEpDlg();
            }
            else {
                Toast.makeText(PlayYoukuActivity.this,
                        "获取选集列表失败", Toast.LENGTH_SHORT).show();
            }
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub

			action = params[0];

            if (mEpisodeList == null) {
                if (search_key != null) {
                    YKUtil.MixResult search_result = YKUtil.soku(
                            search_key, search_filter, search_orderby, search_page_index);
                    if (search_result != null && search_result.mEpisodeList != null &&
                            search_result.mEpisodeList.size() > 0) {
                        mEpisodeList = search_result.mEpisodeList;
                        mEpisodeIndex = search_index;
                    } else {
                        LogUtil.error(TAG, "failed to get search list");
                        Toast.makeText(PlayYoukuActivity.this, "获取搜索列表失败",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    int index = 1;
                    mEpisodeList = new ArrayList<Episode>();
                    while (true) {
                        List<Episode> epList = YKUtil.getEpisodeList(mShowId, index++, page_size);
                        if (epList != null && !epList.isEmpty()) {
                            mEpisodeList.addAll(epList);
                        } else {
                            break;
                        }
                    }

                    if (mEpisodeList == null) {
                        LogUtil.error(TAG, "mEpisodeList is null");
                        mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                        return false;
                    }
                }
            }

            if (action == ACTION_EPISODE_INCR) {
                int incr = params[1];
                mEpisodeIndex += incr;
            }
            else if (action == ACTION_EPISODE_INDEX){
                mEpisodeIndex = params[1];
            }
            else {
                // just get ep list
                return true;
            }

            if (mEpisodeIndex < 0 || mEpisodeIndex >= mEpisodeList.size()) {
                LogUtil.error(TAG, String.format(Locale.US,
                        "mEpisodeIndex %d, mEpisodeList.size() %d",
                        mEpisodeIndex, mEpisodeList.size()));
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                return false;
            }

            Episode ep = mEpisodeList.get(mEpisodeIndex);
            mVid = ep.getVideoId();

            YKUtil.ZGUrl zg = YKUtil.getPlayZGUrl(PlayYoukuActivity.this, mVid);
            if (zg == null) {
                mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                return false;
            }

            mTitle = ep.getTitle();
            mUrlListStr = zg.urls;
            mDurationListStr = zg.durations;

            YKPlayhistoryDatabaseHelper.getInstance(PlayYoukuActivity.this)
                    .saveHistory(mTitle, mVid, mShowId, mEpisodeIndex);

			buildPlaylinkList();
			
			mHandler.sendEmptyMessage(MainHandler.MSG_PLAY_NEXT_EPISODE);
			
			return true;
		}
		
	}

}
