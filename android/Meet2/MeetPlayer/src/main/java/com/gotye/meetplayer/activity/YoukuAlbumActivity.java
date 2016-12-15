package com.gotye.meetplayer.activity;

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
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gotye.common.ZGUrl;
import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.CommonAlbumAdapter;
import com.gotye.meetplayer.ui.widget.HorizontalTextListView;
import com.gotye.meetplayer.util.Util;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private final static int SET_DATA_RELATE	= 3;

    private boolean search_mode = false;

    private int album_page_index = 1;
    private int episode_page_index = 1;
    private int subpage_sort = 2; // 1-最新发布，2-最多播放
    private final static int page_size = 10;

    private int mPlayerImpl;

    private List<Album> mAlbumList;
    private List<Episode> mEpisodeList;
    private YKUtil.FilterResult mFilterResult;

    private String mShowId;
    private String mVid;
    private String mPlayUrl;
    private String mTitle;
    private int mEpisodeIndex = -1;
    private ZGUrl mZGUrl;

    private String title;
    private int channel_id = -1;
    private String filter;
    private int get_filter;
    private int sub_channel_id = -1;
    private int sub_channel_type = -1;

    private String search_key;
    private int search_orderby = -1; // orderby 1-综合排序 2-最新发布 3-最多播放
    private int search_page_index = 1;
    private String search_filter;
    private boolean search_get_filter;
    private final static int search_page_size = 20;
    private int search_index; // index in page

    private String relate_vid;
    
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
        else if (intent.hasExtra("relate_vid")) {
            relate_vid = intent.getStringExtra("relate_vid");
            setTitle(getResources().getString(R.string.title_activity_youku_video) +
                    "  " + title + " 相关");
        }
        else {
            setTitle(getResources().getString(R.string.title_activity_youku_video) +
                    "  " + title);
        }

		if (sub_channel_id == -1 && search_key == null && relate_vid == null) {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
            finish();
			return;
		}
		
		setContentView(R.layout.activity_youku_album);

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
                    String vid = (String) item.get("vid");
                    String title = (String) item.get("title");
                    int count = (Integer) item.get("episode_total");
                    if (count < 10) {
                        new EPGTask().execute(TASK_EPISODE);
                        return;
                    }

                    Intent intent = new Intent(YoukuAlbumActivity.this, YoukuEpisodeActivity.class);
                    intent.putExtra("show_id", mShowId);
                    intent.putExtra("vid", vid);
                    intent.putExtra("title", title);
                    startActivity(intent);
                }
                else {
                    mVid = (String) item.get("vid");
                    if (mVid == null) {
                        Toast.makeText(YoukuAlbumActivity.this,
                                "视频ID为空", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (search_key != null) {
                        search_index = position % search_page_size;
                        LogUtil.info(TAG, String.format(Locale.US,
                                "set search_index to: %d(pos %d, page_size %d)",
                                search_index, position, search_page_size));
                    }

                    mTitle = (String) item.get("title");
                    new EPGTask().execute(TASK_PLAYLINK);
                }
            }

        });
		
		this.mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
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
                if (relate_vid == null &&
                        totalItemCount > 0 && lastInScreen == totalItemCount && !noMoreData) {
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
            search_get_filter = true;
            new SetDataTask().execute(SET_DATA_SEARCH);
        }
        else if (relate_vid != null) {
            search_mode = false;
            new SetDataTask().execute(SET_DATA_RELATE);
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

                    YKPlayhistoryDatabaseHelper dbHelper =
                            YKPlayhistoryDatabaseHelper.getInstance(activity);
                    int pos = dbHelper.getLastPlayedPosition(activity.mZGUrl.vid);
                    if (pos < 0) {
                        dbHelper.saveHistory(activity.mTitle,
                                activity.mZGUrl.vid, activity.mShowId,
                                activity.mEpisodeIndex);
                    }

                    Intent intent = new Intent(activity, PlayYoukuActivity.class);
                    intent.putExtra("url_list", activity.mZGUrl.urls);
                    intent.putExtra("duration_list", activity.mZGUrl.durations);
                    intent.putExtra("title", activity.mTitle);
                    intent.putExtra("ft", 2);
                    intent.putExtra("show_id", activity.mShowId);
                    intent.putExtra("vid", activity.mZGUrl.vid);
                    intent.putExtra("page_index", activity.episode_page_index);
                    intent.putExtra("episode_index", activity.mEpisodeIndex);
                    intent.putExtra("player_impl", activity.mPlayerImpl);

                    if (activity.search_key != null) {
                        intent.putExtra("search_key", activity.search_key);
                        intent.putExtra("search_filter", activity.search_filter);
                        intent.putExtra("search_orderby", activity.search_orderby);
                        intent.putExtra("search_page_index", activity.search_page_index);
                        intent.putExtra("search_index", activity.search_index);
                    }

                    if (pos > 0) {
                        intent.putExtra("preseek_msec", pos);
                        LogUtil.info(TAG, "set preseek_msec: " + pos);
                    }

                    activity.startActivity(intent);
                    break;
                case MSG_MORELIST_DONE:
                    activity.mAdapter.notifyDataSetChanged();
                    break;
                case MSG_FAIL_GET_EPISODE:
                    Toast.makeText(activity, "获取分集失败", Toast.LENGTH_SHORT).show();
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

    private HashMap<String, Object> fill_album_info(Album album) {

        HashMap<String, Object> AlbumInfo = new HashMap<String, Object>();

        String vid = album.getVid();
        if (vid == null)
            vid = album.getShowId(); // tid: "XMTUxNTU0ODAzMg==",

        AlbumInfo.put("title", album.getTitle());
        AlbumInfo.put("img_url", album.getImgUrl());
        AlbumInfo.put("show_id", album.getShowId());
        AlbumInfo.put("tip", album.getStripe());
        AlbumInfo.put("vid", vid);
        AlbumInfo.put("episode_total", album.getEpisodeTotal());
        AlbumInfo.put("is_album", album.getEpisodeTotal() > 1);
        AlbumInfo.put("company", "youku");
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

                    mZGUrl = YKUtil.getPlayZGUrl(YoukuAlbumActivity.this, mVid);
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

                mZGUrl = YKUtil.getPlayZGUrl(YoukuAlbumActivity.this, mVid);
                if (mZGUrl == null) {
                    LogUtil.error(TAG, "Java: failed to call getPlayUrl2()[playlink] vid: " + mVid);
                    return false;
                }

                mHandler.sendEmptyMessage(MainHandler.MSG_PLAYLINK_DONE);
			}
			else if (action == TASK_MORELIST) {
                if (search_mode) {
                    List<Map<String, Object>> listData = mAdapter.getData();

                    search_page_index++;
                    YKUtil.MixResult result = YKUtil.soku(
                            search_key, search_filter, search_orderby, search_page_index);
                    if (result == null || result.mEpisodeList == null ||
                            result.mEpisodeList.isEmpty()) {
                        LogUtil.info(TAG, "Java: no more search result");
                        noMoreData = true;
                        return false;
                    }

                    mEpisodeList = result.mEpisodeList;
                    int c = mEpisodeList.size();
                    if (c < search_page_size) {
                        noMoreData = true;
                        LogUtil.info(TAG, "Java: meet search end");
                    }
                    for (int i=0;i<c;i++) {
                        HashMap<String, Object> episode = new HashMap<String, Object>();
                        Episode ep = mEpisodeList.get(i);
                        episode.put("title", ep.getTitle());
                        episode.put("img_url", ep.getThumbUrl());
                        episode.put("desc", "N/A");
                        episode.put("onlinetime", String.format("发布: %s",
                                ep.getOnlineTime() == null ? "" : ep.getOnlineTime()));
                        episode.put("tip", String.format("播放: %s",
                                ep.getTotalVV() == null ? "" : ep.getTotalVV()));
                        episode.put("duration", ep.getDuration());
                        episode.put("vid", ep.getVideoId());
                        episode.put("is_album", false);
                        episode.put("episode_total", 1);
                        episode.put("company", "youku");
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
                            LogUtil.info(TAG, String.format(Locale.US,
                                    "Java: no more album(meet end) %d %d",
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
                            if (search_mode) {
                                String filter_v = fg.mCat + "=" + ft.get(position).mValue;
                                if (search_filter == null || search_filter.isEmpty()) {
                                    search_filter = "&" + filter_v;
                                }
                                else {
                                    int pos = search_filter.indexOf(fg.mCat);
                                    if (pos != -1) {
                                        // update
                                        int pos2 = search_filter.indexOf("&", pos);
                                        String prefix = null;
                                        if (pos2 > 0)
                                            prefix = search_filter.substring(pos2);

                                        if (pos == 0)
                                            search_filter = "&" + filter_v;
                                        else
                                            search_filter = search_filter.substring(0, pos - 1) + "&" + filter_v;
                                        if (prefix != null)
                                            search_filter += prefix;
                                    }
                                    else {
                                        // add
                                        search_filter += "&";
                                        search_filter += filter_v;
                                    }
                                }
                                LogUtil.info(TAG, "set search_filter to: " + search_filter);

                                search_page_index = 1;
                                noMoreData = false;
                                new SetDataTask().execute(SET_DATA_SEARCH);
                            }
                            else {
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

                                album_page_index = 1;
                                noMoreData = false;
                                new SetDataTask().execute(SET_DATA_LIST);
                            }

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
                        if (search_mode) {
                            search_page_index = 1;
                            // orderby 1-综合排序 2-最新发布 3-最多播放
                            switch (position) {
                                case 0:
                                default:
                                    search_orderby = 1;
                                    break;
                                case 1:
                                    search_orderby = 2;
                                    break;
                                case 2:
                                    search_orderby = 3;
                                    break;
                            }

                            new SetDataTask().execute(SET_DATA_SEARCH);
                        }
                        else {
                            subpage_sort = mFilterResult.mSortTypes.get(position).mValue;
                            LogUtil.info(TAG, "set subpage_sort to: " + subpage_sort);
                            album_page_index = 1;
                            noMoreData = false;
                            new SetDataTask().execute(SET_DATA_LIST);
                        }
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

                items = new ArrayList<Map<String, Object>>();

                YKUtil.MixResult result = YKUtil.soku(search_key, search_filter, search_orderby, search_page_index);
                if (result == null) {
                    LogUtil.error(TAG, "Java: failed to call soku()");
                    return null;
                }

                mAlbumList = result.mALbumList;
                mEpisodeList = result.mEpisodeList;
                if (mAlbumList != null && !mAlbumList.isEmpty()) {
                    int c = mAlbumList.size();
                    for (int i=0;i<c;i++) {
                        Album al = mAlbumList.get(i);
                        HashMap<String, Object> albumMap = fill_album_info(al);
                        if (albumMap == null)
                            continue;

                        items.add(albumMap);
                    }
                }

                if (mEpisodeList == null || mEpisodeList.isEmpty()) {
                    LogUtil.error(TAG, "Java: soku() episode list is null");
                    return null;
                }

                int c = mEpisodeList.size();
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
                    episode.put("company", "youku");
                    items.add(episode);
                }

                if (search_get_filter) {
                    mFilterResult = YKUtil.getSearchFilter();
                    show_filter = true;
                    search_get_filter = false;
                }
			}
			else if (action == SET_DATA_LIST) {
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
                    HashMap<String, Object> albumMap = fill_album_info(al);
                    if (albumMap == null)
                        continue;

                    items.add(albumMap);
                }

                if (get_filter > 0) {
                    mFilterResult = YKUtil.getFilter(channel_id);
                    if (mFilterResult != null) {
                        show_filter = true;
                    }
                    get_filter = 0; // ONLY show once
                }
			}
            else if (action == SET_DATA_RELATE) {
                YKUtil.MixResult result = YKUtil.relate(relate_vid, album_page_index);
                if (result == null) {
                    LogUtil.error(TAG, "failed to call relate() " + relate_vid);
                    return null;
                }

                items = new ArrayList<Map<String, Object>>();

                mAlbumList = result.mALbumList;
                mEpisodeList = result.mEpisodeList;

                if (mAlbumList == null && mEpisodeList == null) {
                    LogUtil.error(TAG, "Java: failed to call relate()");
                    return null;
                }
                if (mAlbumList != null && !mAlbumList.isEmpty()) {
                    int c = mAlbumList.size();
                    for (int i=0;i<c;i++) {
                        Album al = mAlbumList.get(i);
                        HashMap<String, Object> albumMap = fill_album_info(al);
                        if (albumMap == null)
                            continue;

                        items.add(albumMap);
                    }
                }

                int c = mEpisodeList.size();
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
                    episode.put("company", "youku");
                    items.add(episode);
                }
            }
            else {
                LogUtil.error(TAG, "invalid action: " + action);
                return null;
            }

			return items;
		}
	}

}
