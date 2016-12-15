package com.gotye.common.iqiyi;

import com.gotye.common.ZGUrl;
import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Album;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael.Ma on 2016/6/20.
 */
public class IqiyiUtil {
    private final static String TAG = "IqiyiUtil";

    private final static String api_url =
            "http://42.62.105.235/test/iqiyi/index.php?url=";

    public static ZGUrl getPlayZGUrl(String url) {
        return getPlayZGUrl(url, 2);
    }

    public static ZGUrl getPlayZGUrl(String url, int ft) {
        LogUtil.info(TAG, "getPlayZGUrl() url: " + url + " , ft " + ft);

        final String[] strType = new String[]{"fluent", "normal", "high", "super", "hd"};
        String encoded_url;

        try {
            encoded_url = URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        String http_url = api_url;
        http_url += encoded_url;
        http_url += "&type=";
        http_url += strType[ft];
        LogUtil.info(TAG, "getPlayZGUrl() api_url: " + http_url);

        String result = getHttpPage(http_url);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String vid = root.getString("vid");
            String tvid = root.getString("tvid");
            String title = root.getString("title");
            int duration = root.getInt("duration");
            JSONObject video = root.optJSONObject(strType[ft]);
            if (video == null) {
                LogUtil.error(TAG, "NOT found stream: " + strType[ft]);
                return null;
            }

            StringBuffer sbUrl = new StringBuffer();
            StringBuffer sbDuration = new StringBuffer();

            JSONArray urls = video.getJSONArray("url");
            for (int index = 0;index<urls.length();index++) {
                if (index > 0)
                    sbUrl.append(",");
                sbUrl.append(urls.getString(index));
            }
            JSONArray durations = video.getJSONArray("duration");
            for (int index = 0;index<durations.length();index++) {
                if (index > 0)
                    sbDuration.append(",");
                sbDuration.append(durations.getInt(index) / 1000);
            }

            return new ZGUrl(title, vid, "f4v", sbUrl.toString(), sbDuration.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getHttpPage(String url) {
        InputStream is = null;
        ByteArrayOutputStream os = null;

        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);// 设置超时的时间
            conn.setConnectTimeout(5000);// 设置链接超时的时间

            conn.connect();

            if (conn.getResponseCode() != 200) {
                LogUtil.error(TAG, "http response is not 200 " + conn.getResponseCode());
                return null;
            }

            // 获取响应的输入流对象
            is = conn.getInputStream();

            // 创建字节输出流对象
            os = new ByteArrayOutputStream();
            // 定义读取的长度
            int len = 0;
            // 定义缓冲区
            byte buffer[] = new byte[1024];
            // 按照缓冲区的大小，循环读取
            while ((len = is.read(buffer)) != -1) {
                // 根据读取的长度写入到os对象中
                os.write(buffer, 0, len);
            }

            // 返回字符串
            return new String(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
