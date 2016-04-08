package com.gotye.meetplayer.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;
import com.gotye.common.youku.Episode;
import com.gotye.common.youku.YKUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.ui.widget.HorizontalTextListView;
import com.gotye.meetplayer.util.ImgUtil;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YoukuEpisodeActivity extends AppCompatActivity {

    private final static String TAG ="YoukuEpisodeActivity";

    private String mShowId;
    private String mTitle;
    private int  mEpisodeIndex;

    private int mPlayerImpl;

    private final static int page_size = 10;

    private ImageView mImgView;
    private TextView mTvDirector;
    private TextView mTVActor;
    private TextView mTvDesc;
    private GridView gridView;

    private int screen_width, screen_height;
    private boolean isTVbox = false;

    private SimpleAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_youku_album);

        Intent intent = getIntent();
        mShowId = intent.getStringExtra("show_id");

        mImgView = (ImageView)this.findViewById(R.id.img);
        mTvDirector = (TextView)this.findViewById(R.id.tv_director);
        mTVActor = (TextView)this.findViewById(R.id.tv_actor);
        mTvDesc = (TextView)this.findViewById(R.id.tv_desc);

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

        this.gridView = (GridView) findViewById(R.id.grid_view);
        this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id) {
                // TODO Auto-generated method stub
                LogUtil.info(TAG, "Java: onItemClick() " + position);

                Map<String, Object> item = (Map<String, Object>) mAdapter.getItem(position);
                mTitle = (String)item.get("title");
                mEpisodeIndex = position;
                String vid = (String)item.get("vid");
                new PlayLinkTask().execute(vid);
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
            }
        });

        new SetDataTask().execute(mShowId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen_width	= dm.widthPixels;
        screen_height	= dm.heightPixels;
        LogUtil.info(TAG, String.format("Java: screen %d x %d", screen_width, screen_height));

        if (screen_width > screen_height)
            isTVbox = true;
        else
            isTVbox = false;
    }

    private class PlayLinkTask extends AsyncTask<String, Integer, YKUtil.ZGUrl> {

        @Override
        protected void onPostExecute(YKUtil.ZGUrl zgUrl) {
            // TODO Auto-generated method stub
            if (zgUrl == null) {
                Toast.makeText(YoukuEpisodeActivity.this, "获取视频播放地址失败",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(YoukuEpisodeActivity.this, PlayYoukuActivity.class);
            intent.putExtra("url_list", zgUrl.urls);
            intent.putExtra("duration_list", zgUrl.durations);
            intent.putExtra("title", mTitle);
            intent.putExtra("ft", 2);
            intent.putExtra("show_id", mShowId);
            intent.putExtra("index", mEpisodeIndex);
            intent.putExtra("player_impl", mPlayerImpl);

            startActivity(intent);
        }

        @Override
        protected YKUtil.ZGUrl doInBackground(String... params) {
            // TODO Auto-generated method stub
            String vid = params[0];
            return YKUtil.getPlayUrl2(vid);
        }
    }

    private class SetDataTask extends AsyncTask<String, Integer, Boolean> {
        private Album mAlbum;
        private List<Episode> mEpList;
        private Bitmap mBitmap;

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (result) {
                mTVActor.setText("主演: " + mAlbum.getActor());
                mTvDesc.setText("剧情介绍: " + mAlbum.getDescription());

                if (mBitmap != null)
                    mImgView.setImageBitmap(mBitmap);

                int size = mEpList.size();
                ArrayList<HashMap<String, Object>> dataList =
                        new ArrayList<HashMap<String, Object>>();
                for (int i = 0; i < size; i++) {
                    Episode ep = mEpList.get(i);
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("index", i + 1 /*base 1*/);
                    map.put("title", ep.getTitle());
                    map.put("vid", ep.getVideoId());
                    dataList.add(map);
                }

                mAdapter = new SimpleAdapter(YoukuEpisodeActivity.this, dataList,
                        R.layout.gridview_episode, new String[] { "index" }, new int[] {
                        R.id.tv_title });
                gridView.setAdapter(mAdapter);
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
            if (mAlbum == null)
                return false;

            String url = mAlbum.getImgUrl();
            if (url != null && url.startsWith("http://"))
                mBitmap = ImgUtil.getHttpBitmap(url);

            int index = 1;
            mEpList = new ArrayList<Episode>();
            while (true) {
                List<Episode> epList =  YKUtil.getEpisodeList(tid, index, page_size);
                if (epList == null || epList.isEmpty())
                    break;

                mEpList.addAll(epList);
                if (epList.size() < 10)
                    break;

                index++;
            }

            return true;
        }
    }
}
