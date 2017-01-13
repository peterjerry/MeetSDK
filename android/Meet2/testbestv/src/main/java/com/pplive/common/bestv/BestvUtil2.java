package com.pplive.common.bestv;

import com.pplive.common.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Michael.Ma on 2016/7/19.
 */
public class BestvUtil2 {
    private final static String TAG ="BestvUtil2";

    public final static int CHANNEL_ID_CCTV         = 1;
    public final static int CHANNEL_ID_WEISHI       = 2;
    public final static int CHANNEL_ID_SHANGHAI     = 4;
    public final static int CHANNEL_ID_RADIO        = 5;


    private final static String UserAgent =
            "Mozilla/5.0(Linux;U;Android4.4.4;zh-cn;HM1SBuild/KTU84P)" +
                    "AppleWebKit/533.1(KHTML,likeGecko)Version/4.0MobileSafari/533.1";

    private final static String init_api_surfix =
            "bn=Xiaomi" +
                    "&mn=HM1S" +
                    "&os=Android4.4.4" +
                    "&ut=mac&mac=28e31f5c2506" +
                    "&rs=720x1280" +
                    "&net=WIFI" +
                    "&mnc=46001" +
                    "&pnm=com.bestv.app" +
                    "&anm=BesTV" +
                    "&ua=" + UserAgent +
                    "&lt=1" +
                    "&lct=31.282104,121.437172";

    private final static String category_api =
        "https://bestvapi.bestv.cn/video/tv_category?token=";

    private final static String channel_api =
            "https://bestvapi.bestv.cn/video/tv_list?cid=%d&token=";

    private final static String detail_api =
            "https://bestvapi.bestv.cn/video/tv_detail?tid=%d&token=";

    private static String mToken;

    public static class BestvCategory {
        public int id;
        public String title;

        public BestvCategory(int id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    public static class BestvChannel {
        public int tid;
        public String title;
        public String icon;
        public String snapshot;
        public String nowPlay;
        public String willPlay;

        public BestvChannel(int tid, String title, String icon,
                           String snapshot, String nowPlay, String willPlay) {
            this.tid = tid;
            this.title = title;
            this.icon = icon;
            this.snapshot = snapshot;
            this.nowPlay = nowPlay;
            this.willPlay = willPlay;
        }
    }

    public static class BestvProgramList {
        public String channel_name;
        public String day;
        public List<BestvProgram> programs;

        public BestvProgramList(String name, String day, List<BestvProgram> programs) {
            this.channel_name   = name;
            this.day            = day;
            this.programs       = programs;
        }
    }

    public static class BestvProgram {
        public String title;
        public String time;
        public String url;
        public boolean isNow;

        public BestvProgram(String title, String time, String url, boolean isNow) {
            this.title  = title;
            this.time   = time;
            this.url    = url;
            this.isNow  = isNow;
        }
    }

    public static List<BestvCategory> getCategory() {
        LogUtil.info(TAG, "getCategory()");

        String url = category_api + mToken;
        LogUtil.info(TAG, "getCategory(): " + url);
        String result = getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to getCategory()");
            return null;
        }

        try {
            JSONObject json = new JSONObject(result);
            JSONArray data = json.getJSONArray("data");
            List<BestvCategory> catList = new ArrayList<>();
            for (int i=0;i<data.length();i++) {
                JSONObject cat = data.getJSONObject(i);
                int id = cat.getInt("id");
                String title = cat.getString("name");
                catList.add(new BestvCategory(id, title));
            }

            return catList;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<BestvChannel> getChannel(int cid) {
        LogUtil.info(TAG, "getChannel() cid: " + cid);

        String url = String.format(Locale.US,
                channel_api, cid) + mToken;
        LogUtil.info(TAG, "getChannel(): " + url);
        String result = getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to getChannel()");
            return null;
        }

        try {
            JSONObject json = new JSONObject(result);
            JSONObject data = json.getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            List<BestvChannel> channelList = new ArrayList<>();
            for (int i=0;i<list.length();i++) {
                JSONObject chn = list.getJSONObject(i);

                // icon=http://mimgs.bestv.cn/bestvapp/20151021/562722a28372a.png
                // name=CCTV-1
                // pic=http://mimgs.bestv.cn/bestvapp/20151019/5624cdfb7b362.jpg
                // tid=1
                // title=风云年代(38)
                // title2=风云年代(39)
                int tid = chn.getInt("tid");
                String title = chn.getString("name");
                String icon = chn.getString("icon");
                String snapshot = chn.getString("pic");
                String nowPlay = chn.getString("title");
                String willPlay = chn.getString("title2");
                channelList.add(new BestvChannel(tid, title, icon, snapshot, nowPlay, willPlay));
            }

            return channelList;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String detail(int tid) {
        LogUtil.info(TAG, "detail() tid: " + tid);

        String url = String.format(Locale.US, detail_api, tid) + mToken;
        LogUtil.info(TAG, "detail(): " + url);
        String result = getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to detail()");
            return null;
        }

        try {
            JSONObject json = new JSONObject(result);
            JSONObject data = json.getJSONObject("data");

            // playurl=http://bst.mobile.live.bestvcdn.com.cn/live/program/live991/
            // weixinxwzh/live.m3u8?se=weixin&ct=2
            // &_fk=74D7C4565EA17DADE32A3E3E9098D2248EBBDFDF3E96A904AFB924EDDD4E1409
            String playUrl = data.getString("playurl");

            // ct=2,3 640x480 400k
            // ct=4 720x540 1.3m
            return playUrl;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<BestvProgramList> getPrograms(int tid) {
        LogUtil.info(TAG, "getPrograms() tid: " + tid);

        String url = String.format(Locale.US, detail_api, tid) + mToken;
        LogUtil.info(TAG, "getPrograms(): " + url);
        String result = getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to detail()");
            return null;
        }

//        code: 0,
//                error: "",
//                total: 0,
//                data: {
//                    name: "电视剧频道",
//                    playurl: "http://bst.mobile.live.bestvcdn.com.cn/live/program/live991/weixindsjpd/live.m3u8?se=weixin&ct=2&_fk=850A8A02D56B0339627002695DC963CFCB4C02E32F544B52E4310D07E4C9D02D",
//                    tvs: [
//            {
//                day: "08-14 星期天",
//                isToday: 0,
//                list: [
//                {
//                    time: "00:00",
//                    title: "电视剧频道",
//                    playurl: "http://bst.mobile.live.bestvcdn.com.cn/live/program/live991/weixindsjpd/index.m3u8?se=weixin&ct=2&starttime=1471104000&endtime=1471105980&_fk=850A8A02D56B0339627002695DC963CFCB4C02E32F544B52E4310D07E4C9D02D",
//                    isNow: 0
//                },

        try {
            JSONObject json = new JSONObject(result);
            int code = json.getInt("code");
            if (code != 0) {
                String error = json.getString("error");
                LogUtil.error(TAG, "failed to getPrograms(): " + error);
                return null;
            }

            JSONObject data = json.getJSONObject("data");
            String name = data.getString("name");
            String live_url = data.getString("playurl");
            JSONArray tvs = data.getJSONArray("tvs");

            int size = tvs.length();
            if (size > 0) {
                List<BestvProgramList> day_list = new ArrayList<>();

                for (int i=0;i<size;i++) {
                    JSONObject tv = tvs.getJSONObject(i);

                    String day = tv.getString("day");
                    JSONArray list = tv.getJSONArray("list");
                    List<BestvProgram> prog_list = new ArrayList<>();
                    for (int j=0;j<list.length();j++) {
                        JSONObject prog = list.getJSONObject(j);
                        boolean isNow = (prog.getInt("isNow") > 0);
                        String playurl= prog.getString("playurl");
                        String title = prog.getString("title");
                        String time = prog.getString("time");

                        prog_list.add(new BestvProgram(title, time, playurl, isNow));
                    }

                    day_list.add(new BestvProgramList(name, day, prog_list));
                }

                return day_list;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getToken() {

        String request_params =
                "app=android" +
                        "&channelid=a6d547d2-5ac8-40cc-a70c-71dd0a9e84c3" +
                        "&timestamp=" + System.currentTimeMillis() / 1000L +
                        "&" + init_api_surfix;
        request_params = sortParams(request_params);
        String magic_key = "45e245eef810a7194cad416858a04ba8";

        try {
            byte[] encoded_data = encodeParams(request_params.getBytes(), magic_key.getBytes());
            String signature = convertReadable(encoded_data).toLowerCase();
            LogUtil.info(TAG, "signature: " + signature);

            String post_url = "https://bestvapi.bestv.cn/app/init?" +
                    request_params +
                    "&signature=" + signature;

            JSONObject json = new JSONObject();
            json.put("device_id", "865624023658835");

            LogUtil.info(TAG, "post url: " + post_url);
            LogUtil.info(TAG, "post data: " + json.toString());

            String result = postBestvHttpPage(post_url, json.toString());
            if (result == null) {
                LogUtil.error(TAG, "failed to post");
                return null;
            }

            JSONObject root = new JSONObject(result);
            int errorcode = root.getInt("errorcode");
            if (errorcode != 0) {
                String message = root.optString("message");
                LogUtil.error(TAG, String.format(Locale.US,
                        "failed to init bestv api: %d(msg: %s)",
                        errorcode, message));
                return null;
            }
            // token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VJZCI6Ijg2NDY5MDAyODUzMDM5NSIsInRpbWVzdGFtcCI6MTQ2ODg5MjI4MiwiY2hhbm5lbElkIjoiYTZkNTQ3ZDItNWFjOC00MGNjLWE3MGMtNzFkZDBhOWU4NGMzIn0.AKz9HAa-rip4vqgI962xYNq_fOE1HLUldDogCYCKw9Q
            mToken = root.getString("token");
            LogUtil.info(TAG, "bestv token: " + mToken);
            return mToken;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.error(TAG, "Exception: " + e.getMessage());
        }

        return null;
    }

    private static String sortParams(String params) {
        String[] param_items = params.split("&");
        Arrays.sort(param_items);
        StringBuilder sb = new StringBuilder();

        for (int i=0;i<param_items.length;i++) {
            String item = param_items[i];
            String[] tmp = item.split("=");
            if (i > 0)
                sb.append("&");
            if (tmp.length >= 2 && !isEmpty(tmp[1])) {
                sb.append(item);
            }
        }

        return sb.toString();
    }

    private static byte[] encodeParams(byte[] bytes1, byte[] bytes2) throws Exception {
        SecretKeySpec key = new SecretKeySpec(bytes2, "HmacSHA256");
        Mac localMac = Mac.getInstance(key.getAlgorithm());
        localMac.init(key);
        return localMac.doFinal(bytes1);
    }

    private static String convertReadable(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (int i=0;i<bytes.length;i++) {
            String str = Integer.toHexString(bytes[i] & 0xFF);
            if (str.length() == 1) {
                sb.append('0');
            }
            sb.append(str.toUpperCase());
        }

        return sb.toString();
    }

    private static String postBestvHttpPage(String url, String params) {
        InputStream is = null;
        ByteArrayOutputStream os = null;

        try {
            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();

            conn.setRequestProperty("User-Agent", UserAgent);
            conn.setRequestProperty("app", "android");
            conn.setRequestProperty("channel", "standard");
            conn.setRequestProperty("release", "1");
            conn.setRequestProperty("version", "2.1.4");

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            if (params != null) {
                byte[] bypes = params.getBytes();
                conn.getOutputStream().write(bypes);// 输入参数
            }
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

    private static boolean isEmpty(String str) {
        return (str == null) || (str.trim().length() == 0) || (str.trim().equals(""));
    }

    public static String getHttpPage(String url) {
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
            LogUtil.error(TAG, "IOException: " + e.getMessage());
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
