package com.gotye.meetplayer.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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

import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.db.PPTVPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.MeetAdapter;
import com.gotye.meetplayer.adapter.PPTVEpisodeAdapter;
import com.gotye.meetplayer.util.Util;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.pplive.sdk.MediaSDK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PPTVEpisodeActivity extends AppCompatActivity {

    private final static String TAG ="PPTVEpisodeActivity";

    private EPGUtil mEPG;
    private String mAlbumId;
    private String mTitle;
    private String mImgUrl;
    private int mEpisodeIndex; // base 0
    private String mVid;
    private String mEpisodeTitle;

    private PPTVPlayhistoryDatabaseHelper mHistoryDB;

    private List<PlayLink2> mEpisodeList;
    private boolean mbRevertEp = false;

    private int mPlayerImpl;

    private final static int PAGE_SIZE = 10;
    private final static int REVERT_LIST_MIN_SIZE = 30;

    private ImageView mImgView;
    private TextView mTvStripe;
    private TextView mTvTotalVV;
    private TextView mTvDirector;
    private TextView mTVActor;
    private TextView mTvDesc;
    private GridView gridView;
    private ListView listView;

    private MeetAdapter mGvAdapter;

    private boolean mbGridMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_pptv_episode);

        Intent intent = getIntent();
        mAlbumId = intent.getStringExtra("album_id");
        mTitle = intent.getStringExtra("title");
        mImgUrl = intent.getStringExtra("img_url");

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

        this.mImgView = (ImageView)this.findViewById(R.id.img);
        this.mTvStripe = (TextView)this.findViewById(R.id.tv_stripe);
        this.mTvTotalVV = (TextView)this.findViewById(R.id.tv_total_vv);
        this.mTvDirector = (TextView)this.findViewById(R.id.tv_director);
        this.mTVActor = (TextView)this.findViewById(R.id.tv_actor);
        this.mTvDesc = (TextView)this.findViewById(R.id.tv_desc);
        this.gridView = (GridView) this.findViewById(R.id.grid_view);
        this.listView = (ListView) this.findViewById(R.id.listview);

        if (mImgUrl != null) {
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.loading)         // 加载开始默认的图片
                .showImageForEmptyUri(R.drawable.loading) //url爲空會显示该图片，自己放在drawable里面的
                .showImageOnFail(R.drawable.loading_error)      //加载图片出现问题，会显示该图片
                .displayer(new RoundedBitmapDisplayer(5))  //图片圆角显示，值为整数
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
            ImageLoader.getInstance().displayImage(mImgUrl, mImgView, options);
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id) {
                // TODO Auto-generated method stub
                Map<String, Object> item = mGvAdapter.getItem(position);
                int index = (Integer)item.get("index");
                mEpisodeIndex = index - 1;
                mVid = mEpisodeList.get(mEpisodeIndex).getId();
                mEpisodeTitle = mEpisodeList.get(mEpisodeIndex).getTitle();
                new EPGTask().execute(EPGTask.TASK_PLAYLINK, Integer.parseInt(mVid), mEpisodeIndex);
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

        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> item = mGvAdapter.getItem(position);
                int index = (Integer)item.get("index");
                mEpisodeIndex = index - 1;
                mVid = mEpisodeList.get(mEpisodeIndex).getId();
                mEpisodeTitle = mEpisodeList.get(mEpisodeIndex).getTitle();
                new EPGTask().execute(EPGTask.TASK_DETAIL, Integer.parseInt(mVid), mEpisodeIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mHistoryDB = PPTVPlayhistoryDatabaseHelper.getInstance(this);

        mEPG = new EPGUtil();

        if (!TextUtils.isEmpty(mAlbumId)) {
            new EPGTask().execute(EPGTask.TASK_LIST_EP, Integer.parseInt(mAlbumId));
        }
        else {
            Toast.makeText(this, "专辑ID为空", Toast.LENGTH_SHORT).show();
        }
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

    private class EPGTask extends AsyncTask<Integer, Integer, Boolean> {

        private static final int TASK_LIST_EP   = 1;
        private static final int TASK_PLAYLINK  = 2;
        private static final int TASK_DETAIL    = 3;

        private int action;
        private String info;

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (!result) {
                Toast.makeText(PPTVEpisodeActivity.this,
                        "获取" + (action == TASK_LIST_EP ? "视频列表" : "播放地址") + "失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (action == TASK_LIST_EP) {
                if (mEpisodeList == null || mEpisodeList.isEmpty()) {
                    Toast.makeText(PPTVEpisodeActivity.this, "视频列表为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Map<String, Object>> dataList = new ArrayList<>();
                int size = mEpisodeList.size();

                if (size > REVERT_LIST_MIN_SIZE)
                    mbRevertEp = true;
                else
                    mbRevertEp = false;

                for (int i = 0; i < size; i++) {
                    HashMap<String, Object> map = new HashMap<String, Object>();

                    int ep_index;
                    if (mbRevertEp)
                        ep_index = size - i/*base 1*/;
                    else
                        ep_index = i + 1 /*base 1*/;
                    String playlink = mEpisodeList.get(ep_index - 1).getId();

                    map.put("index", ep_index);
                    map.put("title", ep_index);
                    map.put("playlink", playlink);
                    map.put("company", "pptv");
                    dataList.add(map);
                }

                mGvAdapter = new PPTVEpisodeAdapter(PPTVEpisodeActivity.this, dataList,
                        R.layout.gridview_episode);
                gridView.setAdapter(mGvAdapter);
            }
            else if (action == TASK_PLAYLINK) {
                Toast.makeText(PPTVEpisodeActivity.this, info, Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            // TODO Auto-generated method stub
            action = params[0];

            int vid = params[1];
            if (!mEPG.detail(String.valueOf(vid))) {
                LogUtil.error(TAG, "Java: failed to call detail()");
                return false;
            }

            List<PlayLink2> ep_list = mEPG.getLink();

            if (action == TASK_PLAYLINK || action == TASK_DETAIL) {
                if (ep_list.isEmpty()) {
                    LogUtil.error(TAG, "failed to get episode playlink");
                    return false;
                }

                int episode_index = params[2];
                PlayLink2 ep = ep_list.get(0);

                String video_id = ep.getId();
                String episode_title = ep.getTitle();

                if (action == TASK_DETAIL) {
                    final String desc = ep.getDescription();
                    PPTVEpisodeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvDesc.setText("剧情介绍: " + desc);
                        }
                    });
                }
                else {
                    int []ft_list = mEPG.getAvailableFT(video_id);
                    if (ft_list == null || ft_list.length == 0) {
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
                        return false;
                    }

                    Util.add_pptvvideo_history(PPTVEpisodeActivity.this, episode_title,
                            String.valueOf(vid), mAlbumId, episode_index, ft);

                    info = String.format(Locale.US,
                            "ready to play video %s, playlink: %s, ft: %d",
                            episode_title, video_id, ft);
                    LogUtil.info(TAG, info);

                    short port = MediaSDK.getPort("http");

                    Uri uri;
                    String url = PlayLinkUtil.getPlayUrl(vid, port, ft, 3, null);
                    uri = Uri.parse(url);

                    int last_pos = mHistoryDB.getLastPlayedPosition(String.valueOf(vid));

                    Intent intent = new Intent(PPTVEpisodeActivity.this,
                            PPTVPlayerActivity.class);
                    LogUtil.info(TAG, "to play uri: " + uri.toString());

                    intent.setData(uri);
                    intent.putExtra("title", episode_title);
                    intent.putExtra("playlink", vid);
                    intent.putExtra("album_id", Integer.parseInt(mAlbumId));
                    intent.putExtra("ft", ft);
                    intent.putExtra("best_ft", ft);
                    intent.putExtra("index", episode_index);

                    if (last_pos > 0) {
                        intent.putExtra("preseek_msec", last_pos);
                        LogUtil.info(TAG, "Java: set preseek_msec " + last_pos);
                    }

                    startActivity(intent);
                }
            }
            else {
                mEpisodeList = new ArrayList<>();
                mEpisodeList.addAll(ep_list);
            }

            return true;
        }
    }
}
