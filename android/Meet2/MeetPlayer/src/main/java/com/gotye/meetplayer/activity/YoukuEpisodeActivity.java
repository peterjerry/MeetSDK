package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.ImgUtil;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class YoukuEpisodeActivity extends AppCompatActivity {

    private final static String TAG ="YoukuEpisodeActivity";

    private String mShowId;
    private String mVid;
    private String mTitle;
    private int mEpisodeIndex; // base 0
    private int mPageIndex;
    private List<Episode> mEpisodeList;
    private boolean mbRevertEp = false;

    private int mPlayerImpl;

    private final static int page_size = 10;

    private ImageView mImgView;
    private TextView mTvStripe;
    private TextView mTvTotalVV;
    private TextView mTvDirector;
    private TextView mTVActor;
    private TextView mTvDesc;
    private GridView gridView;
    private ListView listView;

    private SimpleAdapter mAdapter = null;
    private boolean mbGridMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_youku_episode);

        Intent intent = getIntent();
        mShowId = intent.getStringExtra("show_id");
        mVid = intent.getStringExtra("vid");
        mTitle = intent.getStringExtra("title");

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

        this.mImgView = (ImageView)this.findViewById(R.id.img);
        this.mTvStripe = (TextView)this.findViewById(R.id.tv_stripe);
        this.mTvTotalVV = (TextView)this.findViewById(R.id.tv_total_vv);
        this.mTvDirector = (TextView)this.findViewById(R.id.tv_director);
        this.mTVActor = (TextView)this.findViewById(R.id.tv_actor);
        this.mTvDesc = (TextView)this.findViewById(R.id.tv_desc);
        this.gridView = (GridView) this.findViewById(R.id.grid_view);
        this.listView = (ListView) this.findViewById(R.id.listview);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id) {
                // TODO Auto-generated method stub
                Map<String, Object> item = (Map<String, Object>) mAdapter.getItem(position);
                int index = (Integer)item.get("index");
                mEpisodeIndex = index - 1;
                new PlayLinkTask().execute();
            }

        });

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> item = (Map<String, Object>) mAdapter.getItem(position);
                int index = (Integer)item.get("index");
                mEpisodeIndex = index - 1;
                new PlayLinkTask().execute();
            }
        });

        new SetDataTask().execute(mShowId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.episode_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.relate:
                Intent intent = new Intent(YoukuEpisodeActivity.this, YoukuAlbumActivity.class);
                intent.putExtra("title", mTitle);
                intent.putExtra("relate_vid", mVid);
                startActivity(intent);
                break;
            case R.id.display_mode:
                mbGridMode = !mbGridMode;
                if (mbGridMode) {
                    gridView.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                }
                else {
                    listView.setVisibility(View.VISIBLE);
                    gridView.setVisibility(View.GONE);
                }

                new SetDataTask().execute(mShowId);
                break;
            default:
                break;
        }

        return true;
    }

    private class PlayLinkTask extends AsyncTask<Integer, Integer, YKUtil.ZGUrl> {
        private String mTitle;

        @Override
        protected void onPostExecute(YKUtil.ZGUrl zgUrl) {
            // TODO Auto-generated method stub
            if (zgUrl == null) {
                Toast.makeText(YoukuEpisodeActivity.this, "获取视频播放地址失败",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            LogUtil.info(TAG, String.format(
                    Locale.US, "before add history: title %s, vid %s, show_id %s, ep_index %d",
                    mTitle, zgUrl.vid, mShowId, mEpisodeIndex));
            YKPlayhistoryDatabaseHelper.getInstance(YoukuEpisodeActivity.this)
                    .saveHistory(mTitle, zgUrl.vid, mShowId, mEpisodeIndex);

            Intent intent = new Intent(YoukuEpisodeActivity.this, PlayYoukuActivity.class);
            intent.putExtra("url_list", zgUrl.urls);
            intent.putExtra("duration_list", zgUrl.durations);
            intent.putExtra("title", mTitle);
            intent.putExtra("ft", 2);
            intent.putExtra("show_id", mShowId);
            intent.putExtra("vid", zgUrl.vid);
            intent.putExtra("episode_index", mEpisodeIndex);
            intent.putExtra("player_impl", mPlayerImpl);

            startActivity(intent);
        }

        @Override
        protected YKUtil.ZGUrl doInBackground(Integer... params) {
            // TODO Auto-generated method stub
            int page_index = ((mEpisodeIndex + 1/*convert to base 1*/) + 9) / 10;
            if (mEpisodeList == null || mEpisodeList.isEmpty() || mPageIndex != page_index) {
                LogUtil.info(TAG, String.format(Locale.US, "update page index: %d -> %d",
                        mPageIndex, page_index));
                mPageIndex = page_index;
                mEpisodeList = YKUtil.getEpisodeList(mShowId, mPageIndex, page_size);
                if (mEpisodeList == null || mEpisodeList.isEmpty())
                    return null;
            }

            int index = mEpisodeIndex - (mPageIndex - 1) * 10;
            if (index >= mEpisodeList.size()) {
                LogUtil.error(TAG, String.format(Locale.US,
                        "episode list index is invalid: %d.%d(mEpisodeIndex %d, mPageIndex %d)",
                        index, mEpisodeList.size(), mEpisodeIndex, mPageIndex));
                return null;
            }

            Episode ep = mEpisodeList.get(index);
            String vid = ep.getVideoId();
            mTitle = ep.getTitle();
            return YKUtil.getPlayUrl2(vid);
        }
    }

    private class SetDataTask extends AsyncTask<String, Integer, Boolean> {

        private Album mAlbum;
        private List<Episode> mEpList;

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (result) {
                if (mAlbum.getImgUrl() != null)
                    new SetPicTask().execute(mAlbum.getImgUrl());

                mTVActor.setText(String.format("主演: %s", mAlbum.getActor()));
                mTvDesc.setText(String.format("剧情介绍: %s", mAlbum.getDescription()));

                mTvTotalVV.setText(String.format("播放: %s", mAlbum.getTotalVV()));
                mTvStripe.setText(mAlbum.getStripe());

                if (mbGridMode) {
                    int size = mAlbum.getEpisodeTotal();
                    if (size > 30)
                        mbRevertEp = true;
                    else
                        mbRevertEp = false;
                    ArrayList<HashMap<String, Object>> dataList =
                            new ArrayList<HashMap<String, Object>>();
                    for (int i = 0; i < size; i++) {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        int ep_index;
                        if (mbRevertEp)
                            ep_index = size - i/*base 1*/;
                        else
                            ep_index = i + 1 /*base 1*/;
                        map.put("index", ep_index);
                        map.put("title", ep_index);
                        dataList.add(map);
                    }

                    String []from = new String[] {"index"};
                    int []to = new int[] {R.id.tv_title};
                    mAdapter = new SimpleAdapter(YoukuEpisodeActivity.this, dataList,
                            R.layout.gridview_episode, from, to);
                    gridView.setAdapter(mAdapter);
                }
                else {
                    ArrayList<HashMap<String, Object>> dataList =
                            new ArrayList<HashMap<String, Object>>();
                    int size = mEpList.size();
                    for (int i = 0; i < size; i++) {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("index", i + 1);
                        map.put("title", mEpList.get(i).getTitle());
                        dataList.add(map);
                    }

                    String []from = new String[] {"title"};
                    int []to = new int[] {R.id.tv_title};
                    mAdapter = new SimpleAdapter(YoukuEpisodeActivity.this, dataList,
                            R.layout.listview_episode, from, to);
                    listView.setAdapter(mAdapter);
                }
            }
            else {
                Toast.makeText(YoukuEpisodeActivity.this, "获取视频信息失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            String tid = params[0];
            mAlbum = YKUtil.getAlbumInfo(tid);
            if (!mbGridMode) {
                mEpList = new ArrayList<>();
                int page_index = 1;
                while (true) {
                    List<Episode> epList = YKUtil.getEpisodeList(
                            mAlbum.getShowId(), page_index++, page_size);
                    if (epList != null && !epList.isEmpty()) {
                        mEpList.addAll(epList);
                    }
                    else {
                        break;
                    }
                }

            }

            return (mAlbum != null);
        }
    }

    private class SetPicTask extends AsyncTask<String, Integer, Bitmap> {

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mImgView.setImageBitmap(bitmap);
            }
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            if (url != null && url.startsWith("http://"))
                return ImgUtil.getHttpBitmap(url);

            return null;
        }
    }
}
