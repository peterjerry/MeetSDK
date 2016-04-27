package com.gotye.meetplayer.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.gotye.common.util.httpUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.InkeAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InkeActivity extends AppCompatActivity {

    private ListView mLvCreator;
    private Button mBtnToggle;
    private InkeAdapter mAdapter;

    private int type = 0;

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

        mBtnToggle = (Button) this.findViewById(R.id.btn_toggle);
        mLvCreator = (ListView) this.findViewById(R.id.lv_creator);

        mLvCreator.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = mAdapter.getItem(position);
                String play_url = (String)map.get("play_url");
                Intent intent = new Intent(InkeActivity.this, InkePlayerActivity.class);
                intent.putExtra("play_url", play_url);
                startActivity(intent);
            }
        });

        mBtnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type == 0) {
                    type = 1;
                    mBtnToggle.setText(getResources().getString(R.string.inke_hotest));
                }
                else {
                    type = 0;
                    mBtnToggle.setText(getResources().getString(R.string.inke_newest));
                }

                new LoadTask().execute(type);
            }
        });

        new LoadTask().execute(type);
    }

    private class LoadTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {

        @Override
        protected void onPostExecute(List<Map<String, Object>> result) {
            if (result != null) {
                if (mAdapter == null) {
                    mAdapter = new InkeAdapter(InkeActivity.this, result);
                    mLvCreator.setAdapter(mAdapter);
                }
                else {
                    List<Map<String, Object>> dataList = mAdapter.getData();
                    dataList.clear();
                    dataList.addAll(result);
                    mAdapter.notifyDataSetChanged();
                }

            }
            else {
                Toast.makeText(InkeActivity.this, "加载数据失败", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Integer... params) {
            String result;

            if (params[0] == 0)
                result = httpUtil.getHttpPage(simpleall_api_url);
            else
                result = httpUtil.getHttpPage(homepage_api_url);
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

                for (int i=0;i<size;i++) {
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
                    int online_users = live.getInt("online_users");

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
