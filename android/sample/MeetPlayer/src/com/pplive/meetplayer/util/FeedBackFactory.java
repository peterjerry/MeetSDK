package com.pplive.meetplayer.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.JsonReader;
import android.widget.Toast;

/**
 * @author Michael
 * @email youjikong@pptv.com
 * @time 2014��9��11�� ����7:26:59
 * @description:
 */
public class FeedBackFactory {
    private static final String dns = "http://1174290910.813293274.fastweb.pb.cachecn.net/fast_tools/display_ldns_diag_client.php";
    private static final String boxplay = "http://play.api.pptv.com/boxplay.api?platform=atv&type=tv.android&sv=2.5.1&sdk=1&channel=161&content=need_drag&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=906485874&id=17320980&ft=0&k_ver=1.1.0.7459";
    private static final String boxplayip = "http://112.65.200.121/boxplay.api?platform=atv&type=tv.android&sv=2.5.1&sdk=1&channel=161&content=need_drag&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=906485874&id=17320980&ft=0&k_ver=1.1.0.7459";
    private static final String webcdn = "http://play.api.webcdn.pptv.com/boxplay.api?platform=atv&type=tv.android&sv=2.5.1&sdk=1&channel=161&content=need_drag&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=906485874&id=17320980&ft=0&k_ver=1.1.0.7459";
    private static final String ip138 = "http://www.ip138.com/";

    private static final String URL = "http://172.16.10.137/crashapi/api/crashreport/launcher";
	//"http://crash.ott.pptv.com/api/crashreport/launcher";

    public static final String SOURCE_CRASH = "1";
    public static final String SOURCE_PLAY_FAILED = "2";
    public static final String SOURCE_USER_FEED = "3";

    public static final String TYPE_CRASH = "1";
    public static final String TYPE_CAN_NOT_PLAY = "2";
    public static final String TYPE_PLAY_SLOW = "3";
    public static final String TYPE_LOADING_SLOW = "4";
    public static final String TYPE_CAN_NOT_LOGIN = "5";
    public static final String TYPE_OTHER_PROBLEM = "6";
	public static Context sContext;

    private boolean needCheckDNS;
    
    private static final String TAG = "CrashReport";

    // �������
    private String source;
    private String devToken;
    private boolean isVip;

    // ��ѡ����
    private String crashId;
    private String type;
    private String pageUrl;
    private String userId;
    private String version;
    private String flash;
    private String devName;
    private String channelId;
    private boolean isLocal = false;

    // ��ǰ���Լ���ɵĲ���
    private String time;
    private String md5;
    private File zipFile;

    public FeedBackFactory(String source, String devToken, boolean isVip,
            boolean needCheckDNS) {
        this(source, devToken, isVip, needCheckDNS, "", TYPE_OTHER_PROBLEM, "",
                "", "", "", "", "" , false);
    }

    public FeedBackFactory(String source, String devToken, boolean isVip,
            boolean needCheckDNS, String crashId, String type, String pageUrl,
            String userId, String version, String flash, String devName,
            String channelId, boolean isLocal) {
        super();
        this.source = source;
        this.devToken = devToken;
        this.isVip = isVip;
        this.needCheckDNS = needCheckDNS;
        this.crashId = crashId;
        this.type = type;
        this.pageUrl = pageUrl;
        this.userId = userId;
        this.version = version;
        this.flash = flash;
        this.devName = devName;
        this.channelId = channelId;
        this.isLocal = isLocal;
        crashId = "";
        time = "";
        md5 = "";
        zipFile = null;
    }

    public void asyncFeedBack() {
        new FeebBackTask().execute();
    }

    private class FeebBackTask extends AsyncTask<Void, Float, String> {

        private int progress = 0;

        @Override
        protected String doInBackground(Void... params) {
            prepareFeedBackData();
            return feedBack();
        }

        @Override
        protected void onPostExecute(String result) {
			Toast.makeText(sContext, "crash reported, id: " + result, Toast.LENGTH_LONG).show();
        }

        private void prepareFeedBackData() {
            setProgress(5);
            
            if (TextUtils.isEmpty(crashId)) {
                crashId = "";
            }
            Log.i(TAG, "michael crash id-->" + crashId);
            if (TextUtils.isEmpty(time)) {
                time = System.currentTimeMillis() + "";
            }
            if (TextUtils.isEmpty(md5)) {
                String md5Key = crashId + "" + source + "" + type + devToken
                        + version + channelId + time
                        + "Z9pKnqggIwIm1qfsyw4";
                md5 = DigestUtils.md5Hex(md5Key);
                Log.i("", "michael md5 s:" + md5Key);
                Log.i("", "michael md5 r:" + md5);
            }
            if (zipFile == null) {
                zipFile = LogcatHelper.getInstance().zipLogFiles("");
            }
            setProgress(40);
        }

        private String feedBack() {
            String ret = "";

            Bundle param = new Bundle();
            Map<String, String> map = new HashMap<String, String>();
            map.put("source", source);
            map.put("devtoken", devToken);
            map.put("isvip", isVip ? "1" : "0");
            map.put("type", type);
            map.put("pageurl", pageUrl);
            map.put("userid", userId);
            map.put("version", version);
            map.put("flash", flash);
            map.put("devname", devName);
            map.put("channelid", channelId);
            map.put("md5", md5);
            map.put("crashid", crashId);
            map.put("time", time);
            Set<String> keys = map.keySet();
            String value;
            for (String key : keys) {
                value = map.get(key);
                if (!TextUtils.isEmpty(value)) {
                    param.putString(key, value);
                }
            }
            
            //����ǲ��԰汾����������־
            if(isLocal){
            	setProgress(99);
            	return ret;
            }
            
            Log.i(TAG, "michael post param: " + param.toString());
            String response = LogReportHandler.post(URL, param, zipFile);
            setProgress(80);
            Log.i(TAG, "michael crash upload reponse-->" + response);
            if (!TextUtils.isEmpty(response)) {
                try {
                    ByteArrayInputStream stream = new ByteArrayInputStream(
                            response.getBytes(HTTP.UTF_8));
                    InputStreamReader reader = new InputStreamReader(stream,
                            HTTP.UTF_8);
                    JsonReader json = new JsonReader(reader);
                    if (json != null && json.hasNext()) {
                        json.beginObject();
                        String name;
                        while (json.hasNext()) {
                            name = json.nextName();
                            if ("errcode".equals(name)) {
                                value = json.nextString();
                                if ("0".equals(value)) {
                                    // 上传成功,清空log文件
                                    LogcatHelper.getInstance().clearLogFiles();
                                }
                            } else if ("crashid".equals(name)) {
                                value = json.nextLong() + "";
                                ret = value;
                            } else {
                                json.skipValue();
                            }
                        }
                        json.endObject();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setProgress(99);
            return ret;
        }

        private void setProgress(int p) {
            progress = p;
        }
    }
}
