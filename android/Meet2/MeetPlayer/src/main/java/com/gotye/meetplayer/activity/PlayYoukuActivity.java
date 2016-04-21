package com.gotye.meetplayer.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class PlayYoukuActivity extends PlaySegFileActivity {
	private final static String TAG = "PlayYoukuActivity";

	private String mShowId;
    private String mVid;
    private int mEpisodeIndex;
    private final static int page_size = 10;
	private List<Episode> mEpisodeList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent   = getIntent();
        mShowId			= intent.getStringExtra("show_id");
        mVid            = intent.getStringExtra("vid");
        mEpisodeIndex	= intent.getIntExtra("episode_index", -1);
	}

    @Override
    protected void OnComplete() {
        if (mShowId != null && mEpisodeIndex != -1) {
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
            YKPlayhistoryDatabaseHelper.getInstance(this)
                    .savePlayedPosition(mVid, mPlayer.getCurrentPosition());
        }

        super.onPause();
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

    @Override
    protected void onSelectEpisode() {
        if (mEpisodeList == null) {
            new NextEpisodeTask().execute(NextEpisodeTask.ACTION_LIST_EPISODE);
            return;
        }
        else if (mEpisodeList.isEmpty()) {
            Toast.makeText(this, "选集列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        popupSelectEpDlg();
    }

    @Override
    protected void onSelectEpisode(int incr) {
        if (mShowId != null && mEpisodeIndex != -1)
            new NextEpisodeTask().execute(NextEpisodeTask.ACTION_EPISODE_INCR, incr);
        else
            LogUtil.warn(TAG, "NO episode available");
    }
	
	private class NextEpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

        private final static int ACTION_EPISODE_INCR    = 1;
        private final static int ACTION_EPISODE_INDEX   = 2;
        private final static int ACTION_LIST_EPISODE    = 3;

        private int action;

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

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
                int index = 1;
                mEpisodeList = new ArrayList<Episode>();
                while (true) {
                    List<Episode> epList =  YKUtil.getEpisodeList(mShowId, index, page_size);
                    if (epList == null || epList.isEmpty())
                        break;

                    mEpisodeList.addAll(epList);
                    if (epList.size() < 10)
                        break;

                    index++;
                }

                if (mEpisodeList == null) {
                    mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                    return false;
                }
            }

            if (action == ACTION_EPISODE_INCR) {
                int incr = params[1];

                mEpisodeIndex += incr;
                if (mEpisodeIndex < 0 || mEpisodeIndex >= mEpisodeList.size()) {
                    mHandler.sendEmptyMessage(MainHandler.MSG_INVALID_EPISODE_INDEX);
                    return false;
                }
            }
            else if (action == ACTION_EPISODE_INDEX){
                mEpisodeIndex = params[1];
            }
            else {
                // just get ep list
                return true;
            }

            Episode ep = mEpisodeList.get(mEpisodeIndex);
            mVid = ep.getVideoId();

            YKUtil.ZGUrl zg = YKUtil.getPlayUrl2(mVid);
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
