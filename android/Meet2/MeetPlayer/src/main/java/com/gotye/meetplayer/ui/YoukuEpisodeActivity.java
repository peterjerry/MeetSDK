package com.gotye.meetplayer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.meetplayer.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class YoukuEpisodeActivity extends AppCompatActivity {
	private final static String TAG = "YoukuEpisodeActivity";

    private MainHandler mHandler;

	private GridView gridView = null;  
    private MySohuEpAdapter adapter = null;
    
    private final static int TASK_EPISODE		= 1;
    private final static int TASK_PLAYLINK		= 2;
    private final static int TASK_MORELIST		= 3;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH	= 2;

    private int album_page_index = 1;
    private int episode_page_index = 1;
    private int search_page_index = 1;
    private int episode_page_incr = 1;
    private final static int page_size = 10;
    
    private List<Map<String, Object>> data2;

    private List<Album> mAlbumList;
    private List<Episode> mEpisodeList;

    private String mShowId;
    private String mVid;
    private String mPlayUrl;
    private String mTitle;
    private int mEpisodeIndex = -1;
    private YKUtil.ZGUrl mZGUrl;

    private String title;
    private int channel_id = -1;
    private String filter;
    private int sub_channel_id = -1;
    private int sub_channel_type = -1;

    private String search_key;
    
    boolean noMoreData = false;
    boolean loadingMore = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		Intent intent = getIntent();
        title = intent.getStringExtra("title");
        filter = intent.getStringExtra("filter");
        channel_id = intent.getIntExtra("channel_id", -1);
		sub_channel_id = intent.getIntExtra("sub_channel_id", -1);
        sub_channel_type = intent.getIntExtra("sub_channel_type", -1);
		if (intent.hasExtra("search_key")) {
            search_key = intent.getStringExtra("search_key");
            setTitle(getResources().getString(R.string.title_activity_youku_video) +
                    "  " + search_key);
        }
        else {
            setTitle(getResources().getString(R.string.title_activity_youku_video) +
                    "  " + title);
        }

		if (sub_channel_id == -1 && search_key == null) {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
            finish();
			return;
		}
		
		setContentView(R.layout.activity_sohu_episode);
		
		this.gridView = (GridView) findViewById(R.id.grid_view);
		this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
									long id) {
				// TODO Auto-generated method stub

				Map<String, Object> item = adapter.getItem(position);

				mShowId = (String)item.get("show_id");
                if (mShowId == null) {
                    mVid = (String)item.get("vid");
                    if (mVid != null)
                        new EPGTask().execute(TASK_PLAYLINK);
                    else
                        Toast.makeText(YoukuEpisodeActivity.this, "video id is null", Toast.LENGTH_LONG).show();
                }
                else {
                    int count = (Integer)item.get("episode_total");
                    if (count > 30) {
                        // " last_count - 1" fix 709 case
                        episode_page_index = (count + 9) / page_size;
                        episode_page_incr = -1;
                    }
                    else {
                        episode_page_index = 1;
                        episode_page_incr = 1;
                    }

                    new EPGTask().execute(TASK_EPISODE);
                }
			}

		});
		
		this.gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int position, long id) {
                // TODO Auto-generated method stub

                Map<String, Object> item = adapter.getItem(position);
                String description = (String) item.get("desc");
                new AlertDialog.Builder(YoukuEpisodeActivity.this)
                        .setTitle("专辑介绍")
                        .setMessage(description)
                        .setPositiveButton("确定", null)
                        .show();
                return true;
            }
        });
		
		this.gridView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
                LogUtil.debug(TAG, String.format("Java: onScroll first %d, visible %d, total %d",
                        firstVisibleItem, visibleItemCount, totalItemCount));

                int lastInScreen = firstVisibleItem + visibleItemCount;
                if (totalItemCount > 0 && lastInScreen == totalItemCount && !noMoreData) {
                    if (!loadingMore) {
                        loadingMore = true;
                        new EPGTask().execute(TASK_MORELIST);
                    }
                }
            }
        });

        mHandler = new MainHandler(this);

	    if (search_key != null)
	    	new SetDataTask().execute(SET_DATA_SEARCH);
	    else
	    	new SetDataTask().execute(SET_DATA_LIST);
	}

    private static class MainHandler extends Handler {
        private final static int MSG_EPISODE_DONE	    = 1;
        private final static int MSG_PLAYLINK_DONE	    = 2;
        private final static int MSG_MORELIST_DONE	    = 3;

        private final static int MSG_FAIL_GET_EPISODE	= 1001;

        private WeakReference<YoukuEpisodeActivity> mWeakActivity;

        public MainHandler(YoukuEpisodeActivity activity) {
            mWeakActivity = new WeakReference<YoukuEpisodeActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            YoukuEpisodeActivity activity = mWeakActivity.get();
            if (activity == null) {
                LogUtil.debug(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_EPISODE_DONE:
                    activity.popupSelectEpisodeDlg();
                    break;
                case MSG_PLAYLINK_DONE:
                    /*Intent intent = new Intent(activity, VideoPlayerActivity.class);
                    Uri uri = Uri.parse(activity.mPlayUrl);
                    intent.setData(uri);
                    intent.putExtra("title", activity.mTitle);
                    intent.putExtra("impl", 3); // force ffplay
                    */

                    Intent intent = new Intent(activity, PlayYoukuActivity.class);
                    intent.putExtra("url_list", activity.mZGUrl.urls);
                    intent.putExtra("duration_list", activity.mZGUrl.durations);
                    intent.putExtra("title", activity.mTitle);
                    intent.putExtra("ft", 2);
                    intent.putExtra("show_id", activity.mShowId);
                    intent.putExtra("page_index", activity.episode_page_index);
                    intent.putExtra("index", activity.mEpisodeIndex);

                    activity.startActivity(intent);
                    break;
                case MSG_MORELIST_DONE:
                    activity.adapter.notifyDataSetChanged();
                    break;
                case MSG_FAIL_GET_EPISODE:
                    Toast.makeText(activity, "failed to get episode", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }
	
	private void popupSelectEpisodeDlg() {
		int size = mEpisodeList.size();
		if (size == 0) {
			Toast.makeText(this, "episode list is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> title_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			title_list.add(mEpisodeList.get(i).getTitle());
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);
		
		Dialog choose_episode_dlg = new AlertDialog.Builder(YoukuEpisodeActivity.this)
		.setTitle("Select episode")
		.setItems(str_title_list,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Episode ep = mEpisodeList.get(whichButton);
                        mVid = ep.getVideoId();
                        mTitle = ep.getTitle();
                        mEpisodeIndex = whichButton;
                        new EPGTask().execute(TASK_PLAYLINK);
                        dialog.dismiss();
                    }
		})
        .setPositiveButton("More...",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        episode_page_index += episode_page_incr;
                        if (episode_page_index > 0)
                            new EPGTask().execute(TASK_EPISODE);
                        else
                            Toast.makeText(YoukuEpisodeActivity.this, "No more episode", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    }
                })
        .setNeutralButton("Page",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        popupSelectPage(episode_page_index);

                        dialog.dismiss();
                    }
                })
		.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
		.create();
		choose_episode_dlg.show();
	}

    private void popupSelectPage(int default_page) {
        AlertDialog.Builder builder;

        final EditText inputKey = new EditText(this);
        inputKey.setText(String.valueOf(default_page));
        inputKey.setHint("select episode page");

        builder = new AlertDialog.Builder(this);
        builder.setTitle("input page number").setIcon(android.R.drawable.ic_dialog_info).setView(inputKey)
                .setNegativeButton("Cancel", null);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                episode_page_index = Integer.valueOf(inputKey.getText().toString());
                new EPGTask().execute(TASK_EPISODE);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private HashMap<String, Object> fill_episode(Album al) {
        HashMap<String, Object> episode = new HashMap<String, Object>();

        // get more detail
        Album detail_album = YKUtil.getAlbumInfo(al.getShowId());
        if (detail_album == null)
            return null;

        episode.put("title", detail_album.getTitle());
        //episode.put("img_url", detail_album.getImgUrl());
        episode.put("img_url", "N/A");
        episode.put("desc", detail_album.getDescription());
        episode.put("tip", detail_album.getStripe());
        episode.put("show_id", detail_album.getShowId());
        episode.put("total_vv", detail_album.getTotalVV());
        episode.put("actor", detail_album.getActor());
        episode.put("vid", detail_album.getVid());
        episode.put("episode_total", detail_album.getEpisodeTotal());

        episode.put("is_album", true);
        return episode;
    }

	private class EPGTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			
			if (!result) {
				LogUtil.error(TAG, "failed to get episode");
				Toast.makeText(YoukuEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
            int action = params[0];
			
			if (action == TASK_EPISODE) {
                int retry = 3;
                while(retry > 0) {
                    mEpisodeList = YKUtil.getEpisodeList(mShowId, episode_page_index, page_size);
                    if (mEpisodeList != null)
                        break;

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    retry--;
                }

                if (mEpisodeList == null) {
                    LogUtil.error(TAG, "Java: failed to call getEpisodeList()");
                    mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_GET_EPISODE);
                    return true;
                }

                if (mEpisodeList.size() == 1) {
                    Episode ep = mEpisodeList.get(0);
                    mVid = ep.getVideoId();
                    mTitle = ep.getTitle();
                    mEpisodeIndex = -1;

                    mZGUrl = YKUtil.getZGUrls(mVid);
                    if (mZGUrl == null) {
                        LogUtil.error(TAG, "Java: failed to call getZGUrls() vid: " + mVid);
                        return false;
                    }
                    /*mPlayUrl = YKUtil.getPlayUrl(mVid);
                    if (mPlayUrl == null) {
                        LogUtil.error(TAG, "Java: failed to call getPlayUrl() vid: " + mVid);
                        return false;
                    }*/

                    mHandler.sendEmptyMessage(MainHandler.MSG_PLAYLINK_DONE);
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_EPISODE_DONE);
			}
			else if (action == TASK_PLAYLINK){
                if (mVid == null) {
                    LogUtil.error(TAG, "Java: mVid is null");
                    return false;
                }

                /*mPlayUrl = YKUtil.getPlayUrl(mVid);
				if (mPlayUrl == null) {
                    LogUtil.error(TAG, "Java: failed to call getPlayUrl() vid: " + mVid);
					return false;
				}*/
                mZGUrl = YKUtil.getZGUrls(mVid);
                if (mZGUrl == null) {
                    LogUtil.error(TAG, "Java: failed to call getZGUrls() vid: " + mVid);
                    return false;
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_PLAYLINK_DONE);
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
                mAlbumList = YKUtil.getAlbums(channel_id, filter, sub_channel_id, sub_channel_type,
                        album_page_index, page_size);
				if (mAlbumList == null || mAlbumList.size() < page_size) {
                    LogUtil.info(TAG, "Java: no more album");
					noMoreData = true;
					return false;
				}

				loadingMore = false;

                List<Map<String, Object>> listData = adapter.getData();

                int c = mAlbumList.size();
                for (int i=0;i<c;i++) {
                    Album al = mAlbumList.get(i);
                    HashMap<String, Object> episode = fill_episode(al);
                    if (episode == null)
                        continue;

                    listData.add(episode);
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_MORELIST_DONE);
			}
			else {
                LogUtil.error(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			
			if (!result) {
                LogUtil.error(TAG, "Java: failed to get sub channel");
				return;
			}
			
			adapter = new MySohuEpAdapter(YoukuEpisodeActivity.this, data2);
		    gridView.setAdapter(adapter);  
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
                mAlbumList = YKUtil.search(search_key, search_page_index, page_size);
                if (mAlbumList == null) {
                    LogUtil.error(TAG, "Java: failed to call subchannel()");
                    return false;
                }
			}
			else {
                mAlbumList = YKUtil.getAlbums(channel_id,
                        filter, sub_channel_id, sub_channel_type,
                        album_page_index, page_size);
				if (mAlbumList == null) {
                    LogUtil.error(TAG, "Java: failed to call subchannel()");
					return false;
				}
			}
						  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
            LogUtil.info(TAG, "Java album size: " + c);
			for (int i=0;i<c;i++) {
                Album al = mAlbumList.get(i);
                HashMap<String, Object> episode = fill_episode(al);
                if (episode == null)
                    continue;

				data2.add(episode);
			}
			
			return true;
		}
	}

}
