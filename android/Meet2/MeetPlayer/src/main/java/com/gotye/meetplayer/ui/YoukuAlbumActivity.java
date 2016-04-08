package com.gotye.meetplayer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.ui.widget.HorizontalTextListView;
import com.gotye.meetplayer.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class YoukuAlbumActivity extends AppCompatActivity {
	private final static String TAG = "YoukuAlbumActivity";

    private MainHandler mHandler;

	private GridView mGridView = null;
    private CommonAlbumAdapter mAdapter = null;
    
    private final static int TASK_EPISODE		= 1;
    private final static int TASK_PLAYLINK		= 2;
    private final static int TASK_MORELIST		= 3;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH	= 2;

    private boolean search_mode = false;

    private Button btnReputation;
    private Button btnPopularity;
    private Button btnUpdate;

    private int album_page_index = 1;
    private int episode_page_index = 1;
    private int subpage_sort = 2; // 1-最新发布，2-最多播放
    private int search_orderby = 2; // orderby 1-综合排序 2-最新发布 3-最多播放
    private int search_page_index = 1;
    private int episode_page_incr = 1;
    private final static int page_size = 10;
    private final static int search_page_size = 20;

    private int mPlayerImpl;

    private List<Album> mAlbumList;
    private List<Episode> mEpisodeList;
    private YKUtil.FilterResult mFilterResult;

    private String mShowId;
    private String mVid;
    private String mPlayUrl;
    private String mTitle;
    private int mEpisodeIndex = -1;
    private YKUtil.ZGUrl mZGUrl;

    private String title;
    private int channel_id = -1;
    private String filter;
    private int get_filter;
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
        get_filter = intent.getIntExtra("get_filter", -1);
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
		
		setContentView(R.layout.activity_youku_episode);

        findViewById(R.id.btn_reputation).setOnClickListener(mClickListener);
        findViewById(R.id.btn_popularity).setOnClickListener(mClickListener);
        findViewById(R.id.btn_update).setOnClickListener(mClickListener);

		this.mGridView = (GridView) findViewById(R.id.grid_view);
		this.mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id) {
                // TODO Auto-generated method stub

                Map<String, Object> item = mAdapter.getItem(position);
                boolean bAlbum = (Boolean) item.get("is_album");
                if (bAlbum) {
                    mShowId = (String) item.get("show_id");
                    int count = (Integer) item.get("episode_total");
                    if (count < 10) {
                        new EPGTask().execute(TASK_EPISODE);
                        return;
                    }

                    Intent intent = new Intent(YoukuAlbumActivity.this, YoukuEpisodeActivity.class);
                    intent.putExtra("show_id", mShowId);
                    startActivity(intent);
                }
                else {
                    mVid = (String) item.get("vid");
                    if (mVid == null) {
                        Toast.makeText(YoukuAlbumActivity.this,
                                "video id is null", Toast.LENGTH_LONG).show();
                        return;
                    }

                    mTitle = (String) item.get("title");
                    new EPGTask().execute(TASK_PLAYLINK);
                }
            }

        });
		
		this.mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int position, long id) {
                // TODO Auto-generated method stub

                Map<String, Object> item = mAdapter.getItem(position);
                String description = (String) item.get("desc");
                new AlertDialog.Builder(YoukuAlbumActivity.this)
                        .setTitle("专辑介绍")
                        .setMessage(description)
                        .setPositiveButton("确定", null)
                        .show();
                return true;
            }
        });
		
		this.mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        mAdapter.setLoadImg(false);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        mAdapter.setLoadImg(true);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
                //LogUtil.debug(TAG, String.format("Java: onScroll first %d, visible %d, total %d",
                //        firstVisibleItem, visibleItemCount, totalItemCount));

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

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

	    if (search_key != null) {
            search_mode = true;
            new SetDataTask().execute(SET_DATA_SEARCH);
        }
	    else {
            search_mode = false;
            new SetDataTask().execute(SET_DATA_LIST);
        }
	}

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

            if (search_mode) {
                search_page_index = 1;
                // orderby 1-综合排序 2-最新发布 3-最多播放

                switch (v.getId()) {
                    case R.id.btn_reputation:
                        search_orderby = 1;
                        new SetDataTask().execute(SET_DATA_SEARCH);
                        break;
                    case R.id.btn_popularity:
                        search_orderby = 3;
                        new SetDataTask().execute(SET_DATA_SEARCH);
                        break;
                    case R.id.btn_update:
                        search_orderby = 2;
                        new SetDataTask().execute(SET_DATA_SEARCH);
                        break;
                    default:
                        Log.w(TAG, "Java unknown view id: " + v.getId());
                        break;
                }
            }
        }
    };

    private static class MainHandler extends Handler {
        private final static int MSG_EPISODE_DONE	    = 1;
        private final static int MSG_PLAYLINK_DONE	    = 2;
        private final static int MSG_MORELIST_DONE	    = 3;

        private final static int MSG_FAIL_GET_EPISODE	= 1001;

        private WeakReference<YoukuAlbumActivity> mWeakActivity;

        public MainHandler(YoukuAlbumActivity activity) {
            mWeakActivity = new WeakReference<YoukuAlbumActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            YoukuAlbumActivity activity = mWeakActivity.get();
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
                    intent.putExtra("player_impl", activity.mPlayerImpl);

                    activity.startActivity(intent);
                    break;
                case MSG_MORELIST_DONE:
                    activity.mAdapter.notifyDataSetChanged();
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
            Toast.makeText(this, "选集列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> title_list = new ArrayList<String>();

        for (int i=0;i<size;i++)
            title_list.add(mEpisodeList.get(i).getTitle());

        final String[] str_title_list = title_list.toArray(new String[size]);

        Dialog choose_episode_dlg = new AlertDialog.Builder(YoukuAlbumActivity.this)
                .setTitle("选集播放")
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
                .setNegativeButton("取消",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .create();
        choose_episode_dlg.show();
    }

    private HashMap<String, Object> fill_album_info(Album al) {

        HashMap<String, Object> AlbumInfo = new HashMap<String, Object>();

        Album detail_album;
        if (al.getEpisodeTotal() > 1) {
            // get more detail
            detail_album = YKUtil.getAlbumInfo(al.getShowId());
            if (detail_album == null)
                return null;
        }
        else {
            detail_album = al;
        }

        LogUtil.info(TAG, "album info: " + detail_album.toString());

        String vid = detail_album.getVid();
        if (vid == null)
            vid = detail_album.getShowId(); // tid: "XMTUxNTU0ODAzMg==",

        AlbumInfo.put("title", detail_album.getTitle());
        AlbumInfo.put("img_url", detail_album.getImgUrl());
        AlbumInfo.put("desc", detail_album.getDescription());
        AlbumInfo.put("tip", "播放: " + detail_album.getTotalVV());
        AlbumInfo.put("duration", detail_album.getStripe());
        AlbumInfo.put("show_id", detail_album.getShowId());
        AlbumInfo.put("total_vv", detail_album.getTotalVV());
        AlbumInfo.put("actor", detail_album.getActor());
        AlbumInfo.put("vid", vid);
        AlbumInfo.put("episode_total", detail_album.getEpisodeTotal());
        AlbumInfo.put("is_album", detail_album.getEpisodeTotal() > 1 ? true : false);
        return AlbumInfo;
    }

	private class EPGTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub

            loadingMore = false;

			if (!result) {
				LogUtil.error(TAG, "failed to get episode");
				Toast.makeText(YoukuAlbumActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
            int action = params[0];

            if (action == TASK_EPISODE) {
                mEpisodeList = YKUtil.getEpisodeList(mShowId, 1, page_size);
                if (mEpisodeList == null || mEpisodeList.isEmpty()) {
                    LogUtil.error(TAG, "Java: failed to call getEpisodeList()");
                    mHandler.sendEmptyMessage(MainHandler.MSG_FAIL_GET_EPISODE);
                    return false;
                }
                else if (episode_page_index == 1 && mEpisodeList.size() == 1) {
                    Episode ep = mEpisodeList.get(0);
                    mVid = ep.getVideoId();
                    mTitle = ep.getTitle();
                    mEpisodeIndex = -1;

                    mZGUrl = YKUtil.getPlayUrl2(mVid);
                    if (mZGUrl == null) {
                        LogUtil.error(TAG, "Java: failed to call getPlayUrl2()[one ep] vid: " + mVid);
                        return false;
                    }

                    mHandler.sendEmptyMessage(MainHandler.MSG_PLAYLINK_DONE);
                    return true;
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_EPISODE_DONE);
            }
			else if (action == TASK_PLAYLINK){
                if (mVid == null) {
                    LogUtil.error(TAG, "Java: mVid is null");
                    return false;
                }

                mZGUrl = YKUtil.getPlayUrl2(mVid);
                if (mZGUrl == null) {
                    LogUtil.error(TAG, "Java: failed to call getPlayUrl2()[playlink] vid: " + mVid);
                    return false;
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_PLAYLINK_DONE);
			}
			else if (action == TASK_MORELIST) {
                if (search_mode) {
                    search_page_index++;
                    mEpisodeList = YKUtil.soku(search_key, search_orderby, search_page_index);
                    if (mEpisodeList == null || mEpisodeList.isEmpty()) {
                        LogUtil.info(TAG, "Java: no more search result");
                        noMoreData = true;
                        return false;
                    }

                    int c = mEpisodeList.size();
                    if (c < search_page_size) {
                        noMoreData = true;
                        LogUtil.info(TAG, "Java: meet search end");
                    }
                    List<Map<String, Object>> listData = mAdapter.getData();
                    for (int i=0;i<c;i++) {
                        HashMap<String, Object> episode = new HashMap<String, Object>();
                        Episode ep = mEpisodeList.get(i);
                        episode.put("title", ep.getTitle());
                        episode.put("img_url", ep.getThumbUrl());
                        episode.put("desc", "N/A");
                        episode.put("onlinetime", "发布: " + ep.getOnlineTime());
                        episode.put("tip", "播放: " + ep.getTotalVV());
                        episode.put("duration", ep.getDuration());
                        episode.put("vid", ep.getVideoId());
                        episode.put("is_album", false);
                        episode.put("episode_total", 1);
                        listData.add(episode);
                    }
                }
                else {
                    album_page_index++;
                    mAlbumList = YKUtil.getAlbums(channel_id, filter, subpage_sort,
                            sub_channel_id, sub_channel_type,
                            album_page_index, page_size);
                    if (mAlbumList == null || mAlbumList.size() < page_size) {
                        if (mAlbumList == null)
                            LogUtil.info(TAG, "Java: no more album");
                        else
                            LogUtil.info(TAG, String.format("Java: no more album(meet end) %d %d",
                                    mAlbumList.size(), page_size));
                        noMoreData = true;
                        return false;
                    }

                    List<Map<String, Object>> listData = mAdapter.getData();

                    int c = mAlbumList.size();
                    for (int i=0;i<c;i++) {
                        Album al = mAlbumList.get(i);
                        HashMap<String, Object> album = fill_album_info(al);
                        if (album == null)
                            continue;

                        listData.add(album);
                    }
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
	
	private class SetDataTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {
		boolean show_filter = false;

		@Override
		protected void onPostExecute(List<Map<String, Object>> result) {
			// TODO Auto-generated method stub
			
			if (result == null) {
                LogUtil.error(TAG, "Java: failed to get sub channel");
				return;
			}
			
			if (mAdapter == null) {
                mAdapter = new CommonAlbumAdapter(YoukuAlbumActivity.this, result);
                mGridView.setAdapter(mAdapter);
            }
            else {
                List<Map<String, Object>> listData = mAdapter.getData();
                listData.clear();
                listData.addAll(result);
                mAdapter.notifyDataSetChanged();
            }

            if (show_filter) {
                LinearLayout layout_filter = (LinearLayout)YoukuAlbumActivity.this
                        .findViewById(R.id.layout_filter);
                layout_filter.setVisibility(View.VISIBLE);

                List<YKUtil.FilerGroup> fg_list = mFilterResult.mFilters;
                int size = fg_list.size();
                for (int i=0;i<size;i++) {
                    HorizontalTextListView htv = new HorizontalTextListView(
                            YoukuAlbumActivity.this, null);
                    final YKUtil.FilerGroup fg = fg_list.get(i);
                    final List<YKUtil.FilerType> ft = fg.mFilterList;
                    ArrayList<String> data_list = new ArrayList<String>();
                    for (int j=0;j<ft.size();j++) {
                        data_list.add(ft.get(j).mTitle);
                    }
                    htv.setList(data_list);
                    htv.setSelection(0);
                    htv.setOnItemClickListener(new HorizontalTextListView.OnItemClickListener() {
                        @Override
                        public boolean onItemClick(int position) {
                            //  genre:艺术|genre:曲艺
                            String filter_v = fg.mCat + ":" + ft.get(position).mValue;
                            if (filter == null || filter.isEmpty()) {
                                filter = filter_v;
                            }
                            else {
                                int pos = filter.indexOf(fg.mCat);
                                if (pos != -1) {
                                    // update
                                    int pos2 = filter.indexOf("|", pos);
                                    String prefix = null;
                                    if (pos2 > 0)
                                        prefix = filter.substring(pos2);

                                    if (pos == 0)
                                        filter = filter_v;
                                    else
                                        filter = filter.substring(0, pos - 1) + "|" + filter_v;
                                    if (prefix != null)
                                        filter += prefix;
                                }
                                else {
                                    // add
                                    filter += "|";
                                    filter += filter_v;
                                }
                            }
                            LogUtil.info(TAG, "set filter to: " + filter);
                            new SetDataTask().execute(SET_DATA_LIST);
                            return true;
                        }
                    });
                    layout_filter.addView(htv, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                }

                HorizontalTextListView htv = new HorizontalTextListView(
                        YoukuAlbumActivity.this, null);
                size = mFilterResult.mSortTypes.size();
                ArrayList<String> sort_list = new ArrayList<String>();
                for (int i=0;i<size;i++) {
                    sort_list.add(mFilterResult.mSortTypes.get(i).mTitle);
                }
                htv.setList(sort_list);
                htv.setSelection(0);
                htv.setOnItemClickListener(new HorizontalTextListView.OnItemClickListener() {
                    @Override
                    public boolean onItemClick(int position) {
                        subpage_sort = mFilterResult.mSortTypes.get(position).mValue;
                        LogUtil.info(TAG, "set subpage_sort to: " + subpage_sort);
                        new SetDataTask().execute(SET_DATA_LIST);
                        return true;
                    }
                });
                layout_filter.addView(htv, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

            }
		}
		
		@Override
		protected List<Map<String, Object>> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
            List<Map<String, Object>> items = null;

			if (action == SET_DATA_SEARCH) {
                /*mAlbumList = YKUtil.search(search_key, search_page_index, page_size);
                if (mAlbumList == null) {
                    LogUtil.error(TAG, "Java: failed to call subchannel()");
                    return false;
                }*/
                mEpisodeList = YKUtil.soku(search_key, search_orderby, search_page_index);
                if (mEpisodeList == null || mEpisodeList.isEmpty()) {
                    LogUtil.error(TAG, "Java: failed to call soku()");
                    return null;
                }

                int c = mEpisodeList.size();
                items = new ArrayList<Map<String, Object>>();
                for (int i=0;i<c;i++) {
                    HashMap<String, Object> episode = new HashMap<String, Object>();
                    Episode ep = mEpisodeList.get(i);
                    LogUtil.info(TAG, "episode info: " + ep.toString());
                    episode.put("title", ep.getTitle());
                    episode.put("img_url", ep.getThumbUrl());
                    episode.put("desc", "N/A");
                    episode.put("onlinetime", "发布: " + ep.getOnlineTime());
                    episode.put("tip", "播放: " + ep.getTotalVV());
                    episode.put("duration", ep.getDuration());
                    episode.put("vid", ep.getVideoId());
                    episode.put("is_album", false);
                    episode.put("episode_total", 1);
                    items.add(episode);
                }
			}
			else {
                mAlbumList = YKUtil.getAlbums(channel_id, filter, subpage_sort,
                        sub_channel_id, sub_channel_type,
                        album_page_index, page_size);
				if (mAlbumList == null || mAlbumList.isEmpty()) {
                    LogUtil.error(TAG, "Java: failed to call subchannel()");
					return null;
				}
                items = new ArrayList<Map<String, Object>>();
                int c = mAlbumList.size();
                for (int i=0;i<c;i++) {
                    Album al = mAlbumList.get(i);
                    HashMap<String, Object> episode = fill_album_info(al);
                    if (episode == null)
                        continue;

                    items.add(episode);
                }

                if (get_filter > 0) {
                    mFilterResult = YKUtil.getFilter(channel_id);
                    if (mFilterResult != null) {
                        show_filter = true;
                    }
                    get_filter = 0; // ONLY show once
                }
			}

			return items;
		}
	}

}
