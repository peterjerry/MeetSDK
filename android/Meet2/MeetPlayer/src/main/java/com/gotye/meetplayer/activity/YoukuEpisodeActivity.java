package com.gotye.meetplayer.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.MeetAdapter;
import com.gotye.meetplayer.adapter.YkEpisodeAdapter;
import com.gotye.meetplayer.util.ImgUtil;
import com.gotye.meetplayer.util.Util;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private MeetAdapter mGvAdapter, mLvAdapter;

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
                Map<String, Object> item = (Map<String, Object>) mGvAdapter.getItem(position);
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
                Map<String, Object> item = (Map<String, Object>) mLvAdapter.getItem(position);
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
                break;
            default:
                break;
        }

        return true;
    }

    private class PlayLinkTask extends AsyncTask<Integer, Integer, YKUtil.ZGUrl> {
        private String mTitle;
        private ProgressDialog mProgressDlg;

        @Override
        protected void onPreExecute() {
            mProgressDlg = new ProgressDialog(YoukuEpisodeActivity.this);
            mProgressDlg.setMessage("播放地址解析中...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.show();
        }

        @Override
        protected void onPostExecute(YKUtil.ZGUrl zgUrl) {
            // TODO Auto-generated method stub
            mProgressDlg.dismiss();

            if (zgUrl == null) {
                Toast.makeText(YoukuEpisodeActivity.this, "获取视频播放地址失败",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            LogUtil.info(TAG, String.format(
                    Locale.US, "before add history: title %s, vid %s, show_id %s, ep_index %d",
                    mTitle, zgUrl.vid, mShowId, mEpisodeIndex));
            YKPlayhistoryDatabaseHelper dbHelper = YKPlayhistoryDatabaseHelper.getInstance(YoukuEpisodeActivity.this);
            int pos = dbHelper.getLastPlayedPosition(zgUrl.vid);
            if (pos < 0)
                dbHelper.saveHistory(mTitle, zgUrl.vid, mShowId, mEpisodeIndex);

            Intent intent = new Intent(YoukuEpisodeActivity.this, PlayYoukuActivity.class);
            intent.putExtra("url_list", zgUrl.urls);
            intent.putExtra("duration_list", zgUrl.durations);
            intent.putExtra("title", mTitle);
            intent.putExtra("ft", 2);
            intent.putExtra("show_id", mShowId);
            intent.putExtra("vid", zgUrl.vid);
            intent.putExtra("episode_index", mEpisodeIndex);
            intent.putExtra("player_impl", mPlayerImpl);
            if (pos > 0) {
                intent.putExtra("preseek_msec", pos);
                LogUtil.info(TAG, "set preseek_msec: " + pos);
            }

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
            return YKUtil.getPlayZGUrl(YoukuEpisodeActivity.this, vid);
        }
    }

    private class SetDataTask extends AsyncTask<String, Integer, Boolean> {

        private Album mAlbum;
        private List<Episode> mEpList;

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (!result) {
                Toast.makeText(YoukuEpisodeActivity.this, "获取视频信息失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            LogUtil.info(TAG, "onProgressUpdate(): " + progress);

            if (progress == 30) {
                if (mAlbum.getImgUrl() != null) {
                    DisplayImageOptions options = new DisplayImageOptions.Builder()
                            .showImageOnLoading(R.drawable.loading)         // 加载开始默认的图片
                            .showImageForEmptyUri(R.drawable.loading) //url爲空會显示该图片，自己放在drawable里面的
                            .showImageOnFail(R.drawable.loading_error)      //加载图片出现问题，会显示该图片
                            .displayer(new RoundedBitmapDisplayer(5))  //图片圆角显示，值为整数
                            .cacheInMemory(true)
                            .cacheOnDisk(true)
                            .build();
                    ImageLoader.getInstance().displayImage(
                            mAlbum.getImgUrl(), mImgView, options);
                }

                mTvDirector.setText(String.format("导演: " + mAlbum.getDirector()));
                mTVActor.setText(String.format("主演: %s", mAlbum.getActor()));
                mTvDesc.setText(String.format("剧情介绍: %s", mAlbum.getDescription()));

                mTvTotalVV.setText(String.format("播放: %s", mAlbum.getTotalVV()));
                mTvStripe.setText(mAlbum.getStripe());

                int size = mAlbum.getEpisodeTotal();

                String stripe = mAlbum.getStripe();
                // 更新至36集 剧集
                // 更新至20160430 综艺
                //[\d]*
                if (stripe != null && stripe.contains("更新至") && stripe.endsWith("集")) {
                    Pattern pattern = Pattern.compile("[1-9]\\d*");
                    Matcher matcher = pattern.matcher(stripe);
                    if (matcher.find()) {
                        size = Integer.valueOf(matcher.group());
                        LogUtil.info(TAG, "episode size updated to " + size);
                    }
                }

                if (size > 30)
                    mbRevertEp = true;
                else
                    mbRevertEp = false;
                List<Map<String, Object>> dataList =
                        new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    int ep_index;
                    if (mbRevertEp)
                        ep_index = size - i/*base 1*/;
                    else
                        ep_index = i + 1 /*base 1*/;
                    map.put("index", ep_index);
                    map.put("title", ep_index);
                    map.put("company", "youku");
                    dataList.add(map);
                }

                mGvAdapter = new YkEpisodeAdapter(YoukuEpisodeActivity.this, dataList,
                        R.layout.gridview_episode);
                gridView.setAdapter(mGvAdapter);
            }
            else if (progress > 30) {
                // grid view
                // update episode info
                //String []from = new String[] {"title"};
                //int []to = new int[] {R.id.tv_title};
                //mAdapter = new SimpleAdapter(YoukuEpisodeActivity.this, dataList,
                //        R.layout.listview_episode, from, to);
                int size;

                size = mEpList.size();
                for (int i = 0; i < size; i++) {
                    int ep_index;
                    if (mbRevertEp)
                        ep_index = mGvAdapter.getCount() - 1 - i/*base 0*/;
                    else
                        ep_index = i/*base 0*/;

                    Map<String, Object> map = mGvAdapter.getItem(ep_index);
                    map.put("vid", mEpList.get(i).getVideoId());
                }
                mGvAdapter.notifyDataSetChanged();

                // list view
                ArrayList<Map<String, Object>> dataList =
                        new ArrayList<>();
                size = mEpList.size();
                int start = 0;
                if (mLvAdapter != null) {
                    start = mLvAdapter.getCount();
                }
                for (int i = start; i < size; i++) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("index", i + 1);
                    map.put("title", mEpList.get(i).getTitle());
                    map.put("vid", mEpList.get(i).getVideoId());
                    map.put("company", "youku");
                    dataList.add(map);
                }

                if (mLvAdapter == null) {
                    mLvAdapter = new YkEpisodeAdapter(YoukuEpisodeActivity.this, dataList,
                            R.layout.listview_episode);
                    listView.setAdapter(mLvAdapter);
                }
                else {
                    List<Map<String, Object>> data = mLvAdapter.getData();
                    data.addAll(dataList);
                    mLvAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            String tid = params[0];
            int progress = 0;

            mAlbum = YKUtil.getAlbumInfo(tid);
            if (mAlbum == null) {
                LogUtil.error(TAG, "failed to get album info");
                return false;
            }

            progress = 30;
            publishProgress(progress);

            mEpList = new ArrayList<>();
            int page_index = 1;
            while (true) {
                List<Episode> epList = YKUtil.getEpisodeList(
                        mAlbum.getShowId(), page_index++, page_size);
                if (epList != null && !epList.isEmpty()) {
                    mEpList.addAll(epList);
                    progress++;
                    publishProgress(progress);
                }
                else {
                    break;
                }
            }

            publishProgress(100);
            return true;
        }
    }
}
