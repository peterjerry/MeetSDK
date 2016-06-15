package com.gotye.meetplayer.activity;

import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

public class PPTVPlayerActivity extends VideoPlayerActivity {

	private final static String TAG = "PPTVPlayerActivity";
	
	private final static int TASK_DETAIL			= 1;
	private final static int TASK_NEXT_EP			= 2;
    private final static int TASK_ITEM_CDN_URL      = 3;
	private final static int TASK_ITEM_FT			= 4;
	
	private EPGUtil mEPG;
	private List<PlayLink2> mEpisodeList;
	private String episode_title;
	private int mEpisodeIndex;
	private int mAlbumId;
	private int mPlaylink;
	private boolean mIsVip = false;

    private ProgressDialog mProgressDlg;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent 	= getIntent();
		mPlaylink		= intent.getIntExtra("playlink", -1);
		mAlbumId 		= intent.getIntExtra("album_id", -1);
		mEpisodeIndex	= intent.getIntExtra("index", -1);
		mIsVip			= intent.getBooleanExtra("is_vip", false);

		LogUtil.info(TAG, String.format("playlink %d, album_id %d, ep_index %d",
				mPlaylink, mAlbumId, mEpisodeIndex));
		
		mEPG = new EPGUtil();

        mProgressDlg = new ProgressDialog(this);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		
		if (mVideoView.isPlaying()) {
			int pos = mVideoView.getCurrentPosition();
			if (mPlaylink != -1 && (mPlaylink < 300000 || mPlaylink > 400000) && pos > 5000) {
				Util.save_pptvvideo_pos(this, String.valueOf(mPlaylink), pos);
				LogUtil.info(TAG, String.format(Locale.US,
                        "Java: vid %d played pos %d msec saved", mPlaylink, pos));
			}
		}
		
		super.onPause();
	}

    @Override
    protected void onSwitchBW() {
        pre_seek_msec = mVideoView.getCurrentPosition() - 5000;
        if (pre_seek_msec < 0)
            pre_seek_msec = 0;

        if (mIsVip) {
            mProgressDlg.setMessage("码流切换中...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.show();

            new EpisodeTask().execute(TASK_ITEM_CDN_URL, mPlaylink);
        }
        else {
            String old_url = mUri.toString();
            int pos = old_url.indexOf("%3Fft%3D");
            if (pos != -1) {
                String old_ft = old_url.substring(pos, pos + "%3Fft%3D".length() + 1);
                String new_ft = "%3Fft%3D" + mFt;
                mUri = Uri.parse(old_url.replace(old_ft, new_ft));
                mHandler.sendEmptyMessage(MainHandler.MSG_RESTART_PLAYER);
            }
        }
    }

	@Override
	protected void onCompleteImpl() {
        if (mPlaylink != -1 && (mPlaylink < 300000 || mPlaylink > 400000)) {
            Util.save_pptvvideo_pos(this, String.valueOf(mPlaylink), 0);
            LogUtil.info(TAG, String.format(Locale.US,
                    "Java: vid %d play complete, reset position", mPlaylink));
        }

		mVideoView.stopPlayback();
		
		if (mEpisodeList == null) {
			if (mAlbumId == -1) {
				Toast.makeText(this, "剧集列表为空", Toast.LENGTH_SHORT).show();
				finish();
			}
			else {
				mEpisodeIndex++;
				new EpisodeTask().execute(TASK_DETAIL, mAlbumId);
			}
		}
		else {
			mEpisodeIndex++;
			mHandler.sendEmptyMessage(MainHandler.MSG_EPISODE_DONE);
		}
	}

	@Override
	protected void onSelectEpisode(int incr) {
        mEpisodeIndex += incr;
		LogUtil.info(TAG, "ep changed to: " + mEpisodeIndex);

        if (mEpisodeList == null) {
            if (mAlbumId == -1) {
                Toast.makeText(this, "剧集列表为空", Toast.LENGTH_SHORT).show();
                mSwichingEpisode = false;
            }
            else {
                new EpisodeTask().execute(TASK_DETAIL, mAlbumId);
            }
        }
        else {
            mHandler.sendEmptyMessage(MainHandler.MSG_EPISODE_DONE);
        }
	}

    @Override
    protected void onEpisodeDone() {
        mSwichingEpisode = false;

        if (mEpisodeList.size() < 2) {
            LogUtil.info(TAG, "episode list size is ONLY one");
            finish();
            return;
        }

        // check ep index
        if (mEpisodeIndex < 0) {
            mEpisodeIndex = mEpisodeList.size() - 1;
            Toast.makeText(PPTVPlayerActivity.this,
                    "switch to ep tail, list size: " + mEpisodeList.size(),
                    Toast.LENGTH_SHORT).show();
        }
        else if (mEpisodeIndex > mEpisodeList.size() - 1) {
            mEpisodeIndex = 0;
            Toast.makeText(PPTVPlayerActivity.this,
                    "switch to ep head, list size: " + mEpisodeList.size(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

		LogUtil.info(TAG, "ready to get ep: " + mEpisodeIndex);
        PlayLink2 pl = mEpisodeList.get(mEpisodeIndex);
        String playlink = pl.getId();
        short http_port = MediaSDK.getPort("http");
        String url = PlayLinkUtil.getPlayUrl(
                Integer.valueOf(playlink), http_port, mFt, 3, null);
        mUri = Uri.parse(url);

        String info = String.format(Locale.US,
                "ready to play video %s, playlink: %s, ft: %d",
                pl.getTitle(), playlink, mFt);
        LogUtil.info(TAG, info);
        Toast.makeText(PPTVPlayerActivity.this, info, Toast.LENGTH_SHORT).show();

        Util.add_pptvvideo_history(PPTVPlayerActivity.this,
                pl.getTitle(), playlink, String.valueOf(mAlbumId), mFt);

        mTitle = pl.getTitle();

        setupPlayer();
    }
	
	private class EpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
            mSwichingEpisode = false;

            if (mIsVip)
                mProgressDlg.dismiss();

			if (!result) {
				LogUtil.error(TAG, "failed to get episode");
				Toast.makeText(PPTVPlayerActivity.this, 
						"获取分集列表失败", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			if (action == TASK_DETAIL) {
				if (params.length < 2) {
					LogUtil.error(TAG, "Java: failed to call detail()");
					return false;
				}
				
				int vid = params[1];
				if (!mEPG.detail(String.valueOf(vid))) {
					LogUtil.error(TAG, "Java: failed to call detail()");
					return false;
				}
			
				mEpisodeList = mEPG.getLink();
				mHandler.sendEmptyMessage(MainHandler.MSG_EPISODE_DONE);
			}
			else if (action == TASK_ITEM_FT) {
				LogUtil.info(TAG, "Java: EPGTask start to get available FT");
        		
        		int vid = params[1];
        		int []ft_list = mEPG.getAvailableFT(String.valueOf(vid));
        		if (ft_list == null || ft_list.length == 0) {
        			mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		int ft = -1;
        		for (int i=ft_list.length - 1;i>=0;i--) {
        			if (ft_list[i] >= 0 && ft_list[i] < 4) {
        				ft = ft_list[i];
        				break;
        			}
        		}
        		
        		if (ft == -1) {
        			mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		Util.add_pptvvideo_history(PPTVPlayerActivity.this, episode_title, 
        				String.valueOf(vid), String.valueOf(mAlbumId), ft);
        		
        		Message msg = mHandler.obtainMessage(MainHandler.MSG_PLAY_CDN_FT, ft, ft);
    	        msg.sendToTarget();
        	}
            else if (action == TASK_ITEM_CDN_URL) {
                LogUtil.info(TAG, "Java: EPGTask start to getCDNUrl");

                int vid = params[1];
                String url = mEPG.getCDNUrl(String.valueOf(vid), String.valueOf(mFt), false, false);

                if (url == null) {
                    mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_TO_GET_FT);
                    return false;
                }

                mUri = Uri.parse(url);
                mHandler.sendMessage(mHandler.obtainMessage(MainHandler.MSG_RESTART_PLAYER));
            }
			else {
				LogUtil.error(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
}
