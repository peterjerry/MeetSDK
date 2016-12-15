package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.DMSItemAdapter;
import com.gotye.meetplayer.adapter.MeetAdapter;
import com.gotye.meetplayer.util.IDlnaCallback;
import com.gotye.meetplayer.util.Util;
import com.pplive.dlna.DLNASdk;
import com.pplive.dlna.DLNASdkDMSItemInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMSExplorerActivity extends AppCompatActivity {

    private final static String TAG = "DMSExplorerActivity";

    private DLNASdk mSDK;
    private DLNASdk.DLNASdkInterface mDLNAcallback;
    private String mDeviceUUid;
    private String mDeviceName;
    private List<DLNASdkDMSItemInfo> mVideoList, mAudioList, mPicList;

    private ViewPager pager;
    private PagerTabStrip tabStrip;
    private ArrayList<View> viewContainter;
    private ArrayList<String> titleContainer;

    private MeetAdapter mAdapter;
    private ListView mCurrLv;
    private List<DLNASdkDMSItemInfo> mCurrList;

    private int mPlayerImpl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmsexplorer);

        Intent intent = getIntent();
        mDeviceUUid = intent.getStringExtra("dms_uuid");
        mDeviceName = intent.getStringExtra("dms_name");

        setTitle(mDeviceName);

        initSDK();

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");
        if (mPlayerImpl == 0)
            mPlayerImpl = 2;

        viewContainter = new ArrayList<>();
        titleContainer = new ArrayList<>();

        pager = (ViewPager) this.findViewById(R.id.viewpager);
        tabStrip = (PagerTabStrip) this.findViewById(R.id.tabstrip);
        //取消tab下面的长横线
        tabStrip.setDrawFullUnderline(false);
        //设置tab的背景色
        tabStrip.setBackgroundColor(this.getResources().getColor(R.color.fancy_blue));
        //设置当前tab页签的下划线颜色
        tabStrip.setTabIndicatorColor(this.getResources().getColor(R.color.red));
        tabStrip.setTextSpacing(200);

        for (int i=0;i<3;i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.page_listview, null);
            ListView listview = (ListView) view.findViewById(R.id.lv);
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Map<String, Object> item = mAdapter.getItem(position);
                    String title = (String)item.get("title");
                    String url = (String)item.get("url");
                    play_video(title, url);
                }
            });
            viewContainter.add(view);
        }
        //页签项
        titleContainer.add("视频");
        titleContainer.add("音频");
        titleContainer.add("图片");

        pager.setAdapter(new PagerAdapter() {

            //viewpager中的组件数量
            @Override
            public int getCount() {
                return viewContainter.size();
            }
            //滑动切换的时候销毁当前的组件
            @Override
            public void destroyItem(ViewGroup container, int position,
                                    Object object) {
                container.removeView(viewContainter.get(position));
            }
            //每次滑动的时候生成的组件
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(viewContainter.get(position));
                return viewContainter.get(position);
            }

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0 == arg1;
            }

            @Override
            public int getItemPosition(Object object) {
                return super.getItemPosition(object);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titleContainer.get(position);
            }
        });

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrLv = (ListView) viewContainter.get(position).findViewById(R.id.lv);

                if (position == 0) {
                    mCurrList = mVideoList;
                }
                else if (position == 1) {
                    mCurrList = mAudioList;
                }
                else {
                    mCurrList = mPicList;
                }

                if (mCurrList == null || mCurrList.isEmpty()) {
                    Toast.makeText(DMSExplorerActivity.this, "文件列表为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateList();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mCurrLv = (ListView) viewContainter.get(0).findViewById(R.id.lv);
        mCurrList = mVideoList;
        if (mCurrList == null || mCurrList.isEmpty()) {
            Toast.makeText(DMSExplorerActivity.this, "文件列表为空",
                    Toast.LENGTH_SHORT).show();
        }

        updateList();
    }

    private void updateList() {
        List<Map<String, Object>> listData = new ArrayList<>();
        for (int i=0;i<mCurrList.size();i++) {
            DLNASdkDMSItemInfo info = mCurrList.get(i);

            HashMap<String, Object> mapItem = new HashMap<>();
            mapItem.put("url", info.downloadUrl);
            mapItem.put("title", info.titleName);
            mapItem.put("type", info.fileType);
            mapItem.put("size", info.fileSize);
            mapItem.put("filesize", Util.getFileSize(info.fileSize));
            listData.add(mapItem);
        }

        if (mAdapter == null) {
            mAdapter = new DMSItemAdapter(DMSExplorerActivity.this, listData,
                    R.layout.dms_item);
            mCurrLv.setAdapter(mAdapter);
        }
        else {
            List<Map<String, Object>> dataList = mAdapter.getData();
            dataList.clear();
            dataList.addAll(listData);
            mCurrLv.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void play_video(String title, String url) {
        Intent intent  = new Intent(DMSExplorerActivity.this, VideoPlayerActivity.class);
        intent.setData(Uri.parse(url));
        intent.putExtra("player_impl", mPlayerImpl);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void initSDK() {
        mDLNAcallback = new DLNASdk.DLNASdkInterface() {
            @Override
            public void OnDeviceAdded(String uuid, String firendname, String logourl, int devicetype) {

            }

            @Override
            public void OnDeviceRemoved(String uuid, int devicetype) {

            }

            @Override
            public void OnLogPrintf(String msg) {

            }

            @Override
            public boolean OnConnect(String uuid, String requestName) {
                return false;
            }

            @Override
            public void OnConnectCallback(String uuid, int state) {

            }

            @Override
            public void OnDisConnect(String uuid) {

            }

            @Override
            public void OnDisConnectCallback(String uuid, boolean isTimeout) {

            }

            @Override
            public void OnRemoveTransportFile(String uuid, String transportuuid) {

            }

            @Override
            public void OnRemoveTransportFileCallback(String uuid, String transportuuid, boolean isTimeout) {

            }

            @Override
            public void OnAddTransportFile(String uuid, String transportuuid, String fileurl, String filename, String thumburl) {

            }

            @Override
            public void OnAddTransportFileCallback(String uuid, String transportuuid, int state) {

            }

            @Override
            public int OnSetURI(String url, String urltitle, String remoteip, int mediatype) {
                return 0;
            }

            @Override
            public void OnPlay() {
            }

            @Override
            public void OnPause() {
            }

            @Override
            public void OnStop() {
            }

            @Override
            public void OnSeek(long position) {
            }

            @Override
            public void OnSetVolume(long volume) {

            }

            @Override
            public void OnSetMute(boolean mute) {

            }

            @Override
            public void OnVolumeChanged(String uuid, long lVolume) {
            }

            @Override
            public void OnMuteChanged(String uuid, boolean bMute) {

            }

            @Override
            public void OnPlayStateChanged(String uuid, String state) {
            }

            @Override
            public void OnPlayUrlChanged(String uuid, String url) {
            }

            @Override
            public void OnContainerChanged(String uuid, String item_id, String update_id) {

            }

            @Override
            public void OnGetCaps(String uuid, String caps) {

            }

            @Override
            public void OnSetUrl(String uuid, long error) {

            }

            @Override
            public void OnBrowse(boolean success, String uuid, String objectid, long count, long total, DLNASdkDMSItemInfo[] filelists) {
                if (filelists != null && filelists.length > 0) {
                    for (int i = 0;i<filelists.length;i++) {
                        DLNASdkDMSItemInfo info = filelists[i];
                        LogUtil.info(TAG, "info #" + i + " : " + info.toString());
                        if (!info.isDirectory) {
                            LogUtil.info(TAG, "file: " + info.titleName +
                                    ", url: " + info.downloadUrl +
                                    ", filesize: " + info.fileSize +
                                    ", filetype: " + info.fileType);

                            String url = info.downloadUrl.toLowerCase();
                            if (url.endsWith(".mp4") ||
                                    url.endsWith(".avi") ||
                                    url.endsWith(".wmv") ||
                                    url.endsWith(".mov"))
                            {
                                info.fileType = 1; // video
                                mVideoList.add(info);
                            }
                            else if (url.endsWith(".mp3") ||
                                    url.endsWith(".wav") ||
                                    url.endsWith(".ogg") ||
                                    url.endsWith(".amr") ||
                                    url.endsWith(".flac") ||
                                    url.endsWith(".ape"))
                            {
                                info.fileType = 2; // audio
                                mAudioList.add(info);
                            }
                            else if (url.endsWith(".jpeg") ||
                                    url.endsWith(".jpg") ||
                                    url.endsWith(".bmp") ||
                                    url.endsWith(".png"))
                            {
                                info.fileType = 3; // image
                                mPicList.add(info);
                            }

                            if (mCurrList != null && mCurrList.size() % 5 == 0) {
                                DMSExplorerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateList();
                                    }
                                });
                            }
                        }
                        else {
                            String objId = info.objectId;
                            LogUtil.info(TAG, "browse: " + objId);

                            mSDK.Browse(mDeviceUUid, objId, 0, 50, false);
                        }
                    }
                }
            }
        };

        mVideoList = new ArrayList<>();
        mAudioList = new ArrayList<>();
        mPicList = new ArrayList<>();

        IDlnaCallback callback = IDlnaCallback.getInstance();
        callback.setCallback(mDLNAcallback);

        LogUtil.info(TAG, "browse: 0");

        mSDK = DLNASdk.getInstance();
        mSDK.Browse(mDeviceUUid, "0"/*root*/, 0, 10, true);
    }
}
