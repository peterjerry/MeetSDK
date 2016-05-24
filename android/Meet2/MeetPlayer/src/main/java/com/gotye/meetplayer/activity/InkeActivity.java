package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.gotye.common.util.LogUtil;
import com.gotye.common.util.httpUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.MeetAdapter;
import com.gotye.meetplayer.adapter.InkeHomePageAdapter;
import com.gotye.meetplayer.adapter.InkeSimpleAllAdapter;
import com.gotye.meetplayer.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InkeActivity extends AppCompatActivity {

    private final static String TAG = "InkeActivity";

    private ListView mLvCreator;
    private GridView mGvCreator;
    private MeetAdapter mAdapter;

    private int type = LoadTask.LIST_SIMPLEALL;

    private ViewPager pager;
    private PagerTabStrip tabStrip;
    private ArrayList<View> viewContainter;
    private ArrayList<String> titleContainer;
    private View viewSimpleAll, viewHomePage;

    private List<Map<String, String>> nsList;

    private String gettop_api_url = "http://service5.ingkee.com/api/live/gettop" +
            "?lc=3000000000001604" +
            "&cv=IK2.5.00_Android" +
            "&cc=TG36078" +
            "&uid=67302632" +
            "&sid=E0rgPgFY8Vi0bQ69RjMuQaybuIQi3i3" +
            "&devi=99000558796818" +
            "&imsi=" +
            "&icc=" +
            "&conn=WIFI" +
            "&vv=1.0.2-201511261613.android" +
            "&aid=3dc3324f515d553" +
            "&proto=3" +
            "&count=10";

    private String simpleall_api_url = "http://service5.ingkee.com/api/live/simpleall" +
            "?lc=3000000000001802" +
            "&cv=IK2.6.00_Android" +
            "&cc=TG36001" +
            "&ua=YuLongCoolpad8297-C00" +
            "&uid=67302632" +
            "&sid=E0rgPgFY8Vi0bQ69RjMuQaybuIQi3i3" +
            "&devi=99000558796818" +
            "&imsi=" +
            "&icc=" +
            "&conn=WIFI" +
            "&vv=1.0.2-201601131421.android" +
            "&aid=3dc3324f515d553" +
            "&osversion=android_19" +
            "&proto=3" +
            "&multiaddr=1";

    private String homepage_api_url = "http://service5.ingkee.com/api/live/homepage_new" +
            "?lc=3000000000003002" +
            "&cv=IK2.6.10_Android" +
            "&cc=TG36001" +
            "&ua=YuLongCoolpad8297-C00" +
            "&uid=67302632" +
            "&sid=E0rgPgFY8Vi0bQ69RjMuQaybuIQi3i3" +
            "&devi=99000558796818" +
            "&imsi=" +
            "&icc=" +
            "&conn=WIFI" +
            "&vv=1.0.2-201601131421.android" +
            "&aid=3dc3324f515d553" +
            "&osversion=android_19" +
            "&proto=3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inke);

        viewContainter = new ArrayList<View>();
        titleContainer = new ArrayList<String>();

        pager = (ViewPager) this.findViewById(R.id.viewpager);
        tabStrip = (PagerTabStrip) this.findViewById(R.id.tabstrip);
        //取消tab下面的长横线
        tabStrip.setDrawFullUnderline(false);
        //设置tab的背景色
        tabStrip.setBackgroundColor(this.getResources().getColor(R.color.fancy_blue));
        //设置当前tab页签的下划线颜色
        tabStrip.setTabIndicatorColor(this.getResources().getColor(R.color.red));
        tabStrip.setTextSpacing(200);

        viewSimpleAll = LayoutInflater.from(this).inflate(R.layout.activity_inke_simpleall, null);
        viewHomePage = LayoutInflater.from(this).inflate(R.layout.activity_inke_homepage, null);
        //viewpager开始添加view
        viewContainter.add(viewSimpleAll);
        viewContainter.add(viewHomePage);
        //页签项
        titleContainer.add("热门主播");
        titleContainer.add("新鲜出炉");

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
                mAdapter = null;
                if (position == 0) {
                    new LoadTask().execute(LoadTask.LIST_SIMPLEALL);
                }
                else {
                    new LoadTask().execute(LoadTask.LIST_HOMEPAGE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mLvCreator = (ListView) viewSimpleAll.findViewById(R.id.lv_creator);
        mGvCreator = (GridView) viewHomePage.findViewById(R.id.gv_creator);

        mLvCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (Map<String, Object>)mLvCreator.getItemAtPosition(position);
                String play_url = (String) map.get("play_url");
                //play_url = ndsTranslate(play_url);
                Intent intent = new Intent(InkeActivity.this, InkePlayerActivity.class);
                intent.putExtra("play_url", play_url);
                startActivity(intent);
            }
        });

        mGvCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (Map<String, Object>)mGvCreator.getItemAtPosition(position);
                String play_url = (String) map.get("play_url");
                //play_url = ndsTranslate(play_url);
                Intent intent = new Intent(InkeActivity.this, InkePlayerActivity.class);
                intent.putExtra("play_url", play_url);
                startActivity(intent);
            }
        });

        mGvCreator.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        Util.checkNetworkType(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] ns = new String[]{
                        "pull.a8.com", "pull2.a8.com",
                        "pull3.a8.com", "pull4.a8.com",
                        "pull5.a8.com", "pull6.a8.com",
                        "pull7.a8.com", "pull8.a8.com",
                        "pull9.a8.com", "pull99.a8.com",
                };

                nsList = new ArrayList<>();
                for (int i=0;i<ns.length;i++) {
                    String []ips = Util.nsLookup(ns[i]);
                    if (ips != null && ips.length > 0) {
                        Map<String, String> item = new HashMap<>();
                        item.put("ns", ns[i]);
                        item.put("ip", ips[0]);
                        nsList.add(item);
                        LogUtil.info(TAG, String.format(Locale.US,
                                "ns_add ip binding : %s -> %s", ns[i], ips[0]));
                    }
                }
            }
        }).start();

        new LoadTask().execute(LoadTask.LIST_SIMPLEALL);
    }

    private String ndsTranslate(String url) {
        String retUrl = url;
        for (int i=0;i<nsList.size();i++) {
            Map<String, String> item = nsList.get(i);
            String ns = item.get("ns");
            String ip = item.get("ip");
            if (url.contains(ns)) {
                retUrl = url.replace(ns, ip);
                LogUtil.info(TAG, "ns_replace: final_url: " + retUrl);
                break;
            }
        }

        return retUrl;
    }

    private class LoadTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {

        private final static int LIST_SIMPLEALL = 1;
        private final static int LIST_HOMEPAGE = 2;

        private int action;

        @Override
        protected void onPostExecute(List<Map<String, Object>> result) {
            if (result != null) {
                if (action == LIST_SIMPLEALL) {
                    if (mAdapter == null) {
                        mAdapter = new InkeSimpleAllAdapter(InkeActivity.this, result,
                                R.layout.inke_item);
                        mLvCreator.setAdapter(mAdapter);
                    } else {
                        List<Map<String, Object>> dataList = mAdapter.getData();
                        dataList.clear();
                        dataList.addAll(result);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                else {
                    if (mAdapter == null) {
                        mAdapter = new InkeHomePageAdapter(InkeActivity.this, result,
                                R.layout.inke_gridview_item);
                        mGvCreator.setAdapter(mAdapter);
                    } else {
                        List<Map<String, Object>> dataList = mAdapter.getData();
                        dataList.clear();
                        dataList.addAll(result);
                        mAdapter.notifyDataSetChanged();
                    }
                }

            } else {
                Toast.makeText(InkeActivity.this, "加载数据失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Integer... params) {
            String result;
            String url;

            action = params[0];
            if (action == LIST_SIMPLEALL)
                url = simpleall_api_url;
            else
                url = homepage_api_url;

            LogUtil.info(TAG, "inke api: " + url);
            result = httpUtil.getHttpPage(url);
            if (result == null)
                return null;

            try {
                JSONTokener jsonParser = new JSONTokener(result);
                JSONObject root = (JSONObject) jsonParser.nextValue();
                if (root.getInt("dm_error") != 0)
                    return null;

                JSONArray lives = root.getJSONArray("lives");
                int size = lives.length();
                List<Map<String, Object>> listData = new ArrayList<Map<String, Object>>();

                for (int i = 0; i < size; i++) {
                    JSONObject live = lives.getJSONObject(i);

//                    {
//                        creator: {
//                            id: 16789339,
//                            nick: "陈蕊儿lvy",
//                            portrait: "NDU1MTQxNDU5OTk3NDA0.jpg"
//                        },
//                        id: "1461302915861745",
//                        name: "",
//                        city: "上海市",
//                        share_addr: "http://live.a8.com/s/?uid=16789339&liveid=1461302915861745&ctime=1461302915",
//                        stream_addr: "http://pull.a8.com/live/1461302915861745.flv",
//                        version: 0,
//                        slot: 2,
//                        optimal: 0,
//                        online_users: 7173,
//                        group: 0
//                    },

                    String city = live.getString("city");
                    String id = live.getString("id");
                    JSONObject creator = live.getJSONObject("creator");
                    String portrait = creator.getString("portrait");
                    String nick = creator.getString("nick");
                    String title = nick;
                    if (!live.getString("name").isEmpty())
                        title = live.getString("name");
                    String share_addr = live.getString("share_addr");
                    String play_url = live.getString("stream_addr");
                    int online_users = live.optInt("online_users");

                    String img_url;
                    if (portrait.startsWith("http://"))
                        img_url = portrait;
                    else
                        img_url = "http://img.meelive.cn/" + portrait;
                    String encoded_img_url = URLEncoder.encode(img_url, "UTF-8");
                    String show_url = "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage";
                    show_url += "?url=";
                    show_url += encoded_img_url;
                    show_url += "&w=360&h=360&s=80&c=0&o=0;";

                    HashMap<String, Object> mapLive = new HashMap<String, Object>();
                    mapLive.put("title", title);
                    mapLive.put("img_url", show_url);
                    mapLive.put("play_url", play_url);
                    mapLive.put("location", city);
                    mapLive.put("share_addr", share_addr);
                    mapLive.put("online_users", online_users);
                    listData.add(mapLive);
                }

                return listData;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}
