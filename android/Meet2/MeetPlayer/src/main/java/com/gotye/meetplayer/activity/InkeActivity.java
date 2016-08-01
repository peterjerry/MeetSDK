package com.gotye.meetplayer.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.gotye.common.inke.InkeUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.MeetAdapter;
import com.gotye.meetplayer.adapter.InkeHomePageAdapter;
import com.gotye.meetplayer.adapter.InkeSimpleAllAdapter;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InkeActivity extends AppCompatActivity {

    private final static String TAG = "InkeActivity";

    private ListView mLvCreator;
    private ListView mLvFollowCreator;
    private GridView mGvCreator;
    private MeetAdapter mAdapter;

    private int list_type = LoadTask.LIST_SIMPLEALL;

    private ViewPager pager;
    private PagerTabStrip tabStrip;
    private ArrayList<View> viewContainter;
    private ArrayList<String> titleContainer;
    private View viewSimpleAll, viewFollow, viewHomePage;

    private List<Map<String, String>> nsList;

    private int mUid;

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
        viewFollow = LayoutInflater.from(this).inflate(R.layout.activity_inke_simpleall, null);
        viewHomePage = LayoutInflater.from(this).inflate(R.layout.activity_inke_homepage, null);
        //viewpager开始添加view
        viewContainter.add(viewSimpleAll);
        viewContainter.add(viewFollow);
        viewContainter.add(viewHomePage);
        //页签项
        titleContainer.add("热门主播");
        titleContainer.add("个人关注");
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
                // reset adapter
                mAdapter = null;

                if (position == 0) {
                    list_type = LoadTask.LIST_SIMPLEALL;
                }
                else if (position == 1) {
                    list_type = LoadTask.LIST_GET_FOLLOW;
                }
                else {
                    list_type = LoadTask.LIST_HOMEPAGE;
                }

                new LoadTask().execute(list_type);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mLvCreator = (ListView) viewSimpleAll.findViewById(R.id.lv_creator);
        mLvFollowCreator = (ListView) viewFollow.findViewById(R.id.lv_creator);
        mGvCreator = (GridView) viewHomePage.findViewById(R.id.gv_creator);

        mLvCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPlay(position, list_type);
            }
        });

        mLvFollowCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPlay(position, list_type);
            }
        });

        mGvCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startPlay(position, list_type);
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

        /*new Thread(new Runnable() {
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
        }).start();*/

        new LoadTask().execute(LoadTask.LIST_SIMPLEALL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.inke_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.search_creator:
                popupSearch();
            default:
                break;
        }

        return true;
    }

    private void startPlay(int position, int list_type) {
        Map<String, Object> map;
        if (list_type == LoadTask.LIST_SIMPLEALL) {
            map = (Map<String, Object>) mLvCreator.getItemAtPosition(position);
        }
        else if (list_type == LoadTask.LIST_GET_FOLLOW) {
            map = (Map<String, Object>)mLvFollowCreator.getItemAtPosition(position);
        }
        else {
            map = (Map<String, Object>) mGvCreator.getItemAtPosition(position);
        }

        String creator_name = (String)map.get("title");
        int uid = (Integer)map.get("uid");
        String play_url = (String) map.get("play_url");
        String rid = (String) map.get("rid");
        int slot = (Integer) map.get("slot");
        play_url += "?type=gotyelive";
        //play_url = ndsTranslate(play_url);
        Intent intent = new Intent(InkeActivity.this, InkePlayerActivity.class);
        intent.putExtra("play_url", play_url);
        intent.putExtra("creator_name", creator_name);
        intent.putExtra("uid", uid);
        intent.putExtra("rid", rid);
        intent.putExtra("slot", slot);
        intent.putExtra("simpleall", list_type != LoadTask.LIST_HOMEPAGE);
        startActivity(intent);
    }

    private void popupSearch() {
        AlertDialog.Builder builder;

        final EditText inputUid = new EditText(this);
        inputUid.setText("67302632");
        inputUid.setHint("输入映客ID号");
        inputUid.setInputType(InputType.TYPE_CLASS_NUMBER);

        builder = new AlertDialog.Builder(this);
        builder.setTitle("映客播客搜索")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputUid)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String text = inputUid.getText().toString();
                        new InkeTask().execute(InkeTask.ACTION_SEARCH, Integer.valueOf(text));
                        dialog.dismiss();
                    }
                });
        builder.show();
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

    private class InkeTask extends AsyncTask<Integer, Integer, Boolean> {
        private int action;
        private List<InkeUtil.UserInfo> userList;
        private InkeUtil.PublishResult publishResult;

        public final static int ACTION_SEARCH       = 1;
        public final static int ACTION_NOWPUBLISH   = 2;
        public final static int ACTION_FOLLOW       = 3;

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (ACTION_SEARCH == action) {
                    String[] str_title_list = new String[userList.size()];
                    for (int i=0;i<userList.size();i++) {
                        InkeUtil.UserInfo info = userList.get(i);
                        str_title_list[i] = info.mNickName + "(" + info.mUid + ")";
                    }

                    Dialog choose_episode_dlg = new AlertDialog.Builder(InkeActivity.this)
                            .setTitle("主播信息")
                            .setItems(str_title_list,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            InkeUtil.UserInfo info = userList.get(whichButton);
                                            new InkeTask().execute(ACTION_NOWPUBLISH, info.mUid);
                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton("取消", null)
                            .create();
                    choose_episode_dlg.show();
                }
                else if (action == ACTION_NOWPUBLISH) {
                    LogUtil.info(TAG, "publish info: " + publishResult.toString());

                    String play_url = publishResult.mStreamUrl;
                    play_url += "?type=gotyelive";
                    Intent intent = new Intent(InkeActivity.this, InkePlayerActivity.class);
                    intent.putExtra("play_url", play_url);
                    intent.putExtra("creator_name", publishResult.mName);
                    intent.putExtra("uid", publishResult.mCreator);
                    intent.putExtra("rid", publishResult.mId);
                    intent.putExtra("slot", publishResult.mSlot);
                    intent.putExtra("simpleall", true);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(InkeActivity.this,
                            "关注成功",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                if (action == ACTION_NOWPUBLISH) {
                    Dialog follow_dlg = new AlertDialog.Builder(InkeActivity.this)
                            .setTitle("主播信息")
                            .setMessage("直播未开始，是否关注主播？")
                            .setPositiveButton("关注", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new InkeTask().execute(ACTION_FOLLOW, mUid);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .create();
                    follow_dlg.show();
                }
                else {
                    Toast.makeText(InkeActivity.this,
                            (action == ACTION_SEARCH ? "搜索" : "关注") + "失败",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            action = params[0];
            int uid = params[1];
            if (ACTION_SEARCH == action) {
                userList = InkeUtil.search(uid);
                return (userList != null && userList.size() > 0);
            }
            else if (ACTION_NOWPUBLISH == action) {
                mUid = uid; // save for follow

                publishResult = InkeUtil.getNowPublish(uid);
                return (publishResult != null);
            }
            else {
                return InkeUtil.follow(uid);
            }
        }
    }

    private class LoadTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {

        private final static int LIST_SIMPLEALL     = 1;
        private final static int LIST_GET_FOLLOW    = 2;
        private final static int LIST_HOMEPAGE      = 3;

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
                else if (action == LIST_GET_FOLLOW) {
                    if (mAdapter == null) {
                        mAdapter = new InkeSimpleAllAdapter(InkeActivity.this, result,
                                R.layout.inke_item);
                        mLvFollowCreator.setAdapter(mAdapter);
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
            List<InkeUtil.LiveInfo> list;

            action = params[0];
            if (action == LIST_SIMPLEALL)
                list = InkeUtil.simpleall();
            else if (action == LIST_GET_FOLLOW)
                list = InkeUtil.getFollow();
            else
                list = InkeUtil.homepage();
            if (list == null || list.isEmpty())
                return null;

            List<Map<String, Object>> listData = new ArrayList<>();
            for (int i=0;i<list.size();i++) {
                InkeUtil.LiveInfo info = list.get(i);

                HashMap<String, Object> mapLive = new HashMap<>();
                mapLive.put("uid", info.mUserId);
                mapLive.put("rid", info.mRoomId);
                mapLive.put("title", info.mTitle);
                mapLive.put("img_url", info.mImage);
                mapLive.put("play_url", info.mPlayUrl);
                mapLive.put("location", info.mLocation);
                mapLive.put("share_addr", info.mShareAddr);
                mapLive.put("online_users", info.mOnlineUsers);
                mapLive.put("slot", info.mSlot);
                listData.add(mapLive);
            }

            return listData;
        }
    }

}
