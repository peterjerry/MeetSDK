package com.gotye.common.inke;

import android.util.Log;

import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Michael.Ma on 2016/5/3.
 */
public class InkeUtil {
    private final static String TAG = "InkeUtil";

    private final static String inke_api_host =
            "http://service.ingkee.com/api";

    private final static String api_surfix =
            "?lc=3000000000005859" +
                    "&cv=IK2.9.50_Android" +
                    "&cc=TG36008" +
                    "&ua=XiaomiMI3W" +
                    "&uid=65286584" +
                    "&id=65286584" +
                    "&sid=20AQA35wxSrd7yjC04Veoz6xkHZVq1oVSmBDi2F9ZrI50B7Mssi3" +
                    "&devi=864690028530395" +
                    "&imsi=" +
                    "&icc=" +
                    "&conn=WIFI" +
                    "&vv=1.0.3-2016060211417.android" +
                    "&aid=8d48c729f639b554" +
                    "&osversion=android_19" +
                    "&proto=4" +
                    "&smid=DuO7bpGwHmL8lq1hV5Jaq7oEtB1KooGaK3Xqf9" +
                    "+gBThJPIf7lOm05DKxchLLUQ9VXZutYmrxfsie/SBtqFY/xusA";

    private final static String get_follow_api_url =
            inke_api_host + "/live/homepage" +
                    api_surfix + "&type=1&multiaddr=1";

    private final static String simpleall_api_url =
            inke_api_host + "/live/simpleall" +
                    api_surfix + "&multiaddr=1";

    private final static String homepage_api_url =
            inke_api_host + "/live/homepage_new" +
                    api_surfix;

    private final static String prepare_publish_api =
            inke_api_host + "/live/pre" +
                    api_surfix +
                    "&multiaddr=1";

    private final static String keepalive_publish_api =
            inke_api_host + "/live/keepalive" +
                    api_surfix;

    private final static String users_publish_api =
            inke_api_host + "/live/users" +
                    api_surfix +
                    "&count=%d" +
                    "&id=%s" + // 1466038853474424
                    "&start=0";

    private final static String start_publish_api =
            inke_api_host + "/live/start" +
                    api_surfix;

    private final static String update_publish_api =
            inke_api_host + "/live/update" +
                    api_surfix;

    private final static String stop_publish_api =
            inke_api_host + "/live/stop" +
                    api_surfix;

    private final static String now_publish_api =
            inke_api_host + "/live/now_publish" +
                    api_surfix +
                    "&id=%d&multiaddr=1"; // uid 67302632

    private final static String search_api =
            inke_api_host + "/user/search" +
                    api_surfix +
                    "&count=10" +
                    "&start=0" + // offset
                    "&keyword=%d"; // uid 67302632

    private final static String follow_api =
            inke_api_host + "/user/relation/follow" +
                    api_surfix;

    private final static String unfollow_api =
            inke_api_host + "/user/relation/unfollow" +
                    api_surfix;

    private final static String relation_api =
            inke_api_host + "/user/relation/relation" +
                    api_surfix +
                    "&id=%d";

    private final static String chat_iplist_api =
            inke_api_host + "/chat/iplist" +
                    api_surfix;

    private final static String account_token_api =
            inke_api_host + "/user/account/token" +
                    api_surfix;

    private final static String live_share_pc_api =
            "http://webapi.busi.inke.com/web/live_share_pc" +
                    "?uid=%d&id=%s";

    public static class LiveInfo {
        public int mUserId;
        public String mRoomId;
        public String mTitle;
        public String mImage;
        public String mPlayUrl;
        public String mLocation;
        public String mShareAddr;
        public int mOnlineUsers;
        public int mSlot;

        private LiveInfo() {

        }

        public LiveInfo(int uid, String room_id, String title,
                        String image, String playUrl, String location,
                        String shareAddr, int onlineUsers, int slot) {
            this.mUserId        = uid;
            this.mRoomId        = room_id;
            this.mTitle         = title;
            this.mImage         = image;
            this.mPlayUrl       = playUrl;
            this.mLocation      = location;
            this.mShareAddr     = shareAddr;
            this.mOnlineUsers   = onlineUsers;
            this.mSlot          = slot;
        }
    }

    public static class UserInfo {
        public int mUid;
        public String mNickName;
        public String mLocation;
        public String mImage;

        private UserInfo() {

        }

        public UserInfo(int uid, String nickname, String location, String image) {
            this.mUid = uid;
            this.mNickName = nickname;
            this.mLocation = location;
            this.mImage = image;
        }
    }

    public static class PublishResult {
        public String mId; // id: "1468545869377486",
        public int mRoomId; // 432541305
        public String mPushUrl;
        public String mStreamUrl;
        public String mShareUrl;
        public int mSlot;

        public String mCity;
        public int mCreator;
        public String mName;
        public String mImage;

        public PublishResult(String id, int room_id,
                             String push_url, String stream_url, String share_url, int slot) {
            this(id, room_id,
                    push_url, stream_url, share_url, slot,
                    null, 0, null, null);
        }

        public PublishResult(String id, int room_id,
                             String push_url, String stream_url, String share_url, int slot,
                             String city, int creator, String name, String image) {
            this.mId        = id;
            this.mRoomId    = room_id;
            this.mPushUrl   = push_url;
            this.mStreamUrl = stream_url;
            this.mShareUrl  = share_url;
            this.mSlot      = slot;

            this.mCity      = city;
            this.mCreator   = creator;
            this.mName      = name;
            this.mImage     = image;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format(Locale.US,
                    "id: %s, roomId: %d, push_url: %s, stream_url: %s, share_url: %s, slot %d",
                    mId, mRoomId, mPushUrl, mStreamUrl, mShareUrl, mSlot));
            if (mCity != null) {
                sb.append(", city: ");
                sb.append(mCity);
            }
            if (mCreator != 0) {
                sb.append(", creator: ");
                sb.append(mCreator);
            }
            if (mName != null) {
                sb.append(", name: ");
                sb.append(mName);
            }
            if (mImage != null) {
                sb.append(", image: ");
                sb.append(mImage);
            }

            return sb.toString();
        }
    }

    public static List<LiveInfo> getFollow() {
        return getLiveInfo(get_follow_api_url);
    }

    public static List<LiveInfo> simpleall() {
        return getLiveInfo(simpleall_api_url);
    }

    public static List<LiveInfo> homepage() {
        return getLiveInfo(homepage_api_url);
    }

    private static List<LiveInfo> getLiveInfo(String url) {
        LogUtil.info(TAG, "getLiveInfo(): " + url);
        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to getLiveInfo url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            if (root.getInt("dm_error") != 0)
                return null;

            JSONArray lives = root.getJSONArray("lives");
            int size = lives.length();
            List<LiveInfo> list = new ArrayList<>();

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
                int uid = creator.getInt("id");
                String portrait = creator.getString("portrait");
                String nick = creator.getString("nick");
                String title = nick;
                if (!live.getString("name").isEmpty())
                    title = live.getString("name");
                String share_addr = live.getString("share_addr");
                String play_url = live.getString("stream_addr");
                int slot = live.getInt("slot"); // for chatroom
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
                show_url += "&w=360&h=360&s=80&c=0&o=0";

                list.add(new LiveInfo(uid, id, title, show_url, play_url,
                        city, share_addr, online_users, slot));
            }

            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static PublishResult getPublishUrl() {
        LogUtil.info(TAG, "getPublishUrl(): " + prepare_publish_api);

        String result = Util.getHttpPage(prepare_publish_api);
        if (result == null) {
            LogUtil.error(TAG, "failed to get publish url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "高峰期间4级以下用户不能直播，早8-晚6点直播无限制！看直播、送礼物都可升级解锁直播哦！",
//            dm_error: 10001

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to getPublishUrl(): " + error_msg);
                return null;
            }

            /*
            dm_error=0
            error_msg=操作成功
            id=1466038853474424
            link=0
            publish_addr=rtmp://istream5.a8.com/live/1466038853474424
            room_id=351201120
            share_addr=http://live5.a8.com/s/?uid=65286584&liveid=1466038853474424&ctime=1466038853
            slot: 3,
            stream_addr=http://pull5.a8.com/live/1466038853474424.flv
            */
            String stream_addr = root.getString("stream_addr");
            String id = root.getString("id");
            int room_id = root.getInt("room_id");
            String publish_addr = root.getString("publish_addr");
            String share_addr = root.getString("share_addr");
            int slot = root.getInt("slot");

            return new PublishResult(id, room_id,
                    publish_addr, stream_addr, share_addr, slot);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int getUsers(String id) {
        LogUtil.info(TAG, "getUsers(): id: " + id);

        String url = String.format(Locale.US, users_publish_api, 20, id);

        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to get users url");
            return -1;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "高峰期间4级以下用户不能直播，早8-晚6点直播无限制！看直播、送礼物都可升级解锁直播哦！",
//            dm_error: 10001

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to getUsers(): " + error_msg);
                return -1;
            }

            /*
            dm_error=0
            error_msg=操作成功
            total=19
            users
            */
            int total = root.getInt("total");
            JSONArray users = root.getJSONArray("users");
            int count = users.length();
            for (int i=0;i<count;i++) {
                JSONObject user = users.getJSONObject(i);

                // nick=小梵子
                // portrait=http://img.meelive.cn/dmimg00/M00/00/15/wKgy9FQhCZuIWA7cAAE96DN4obAAAAkOQB8aeYAAT4A637.jpg
                String nick = user.getString("nick");
                String portrait = user.getString("portrait");
                //LogUtil.info(TAG, String.format(Locale.US,
                //        "inke live user #%d: %s, %s", i, nick, portrait));
            }
            return total;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static List<String> chatList(int slot) {
        LogUtil.info(TAG, "chatList(): " + chat_iplist_api);

        String result = Util.getHttpPage(chat_iplist_api);
        if (result == null) {
            LogUtil.error(TAG, "failed to get charList url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "高峰期间4级以下用户不能直播，早8-晚6点直播无限制！看直播、送礼物都可升级解锁直播哦！",
//            dm_error: 10001

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to chatList(): " + error_msg);
                return null;
            }

            JSONArray cfg = root.getJSONArray("cfg");
            int size = cfg.length();
            List<String> ipList = new ArrayList<>();
            for (int i=0;i<size;i++) {
                JSONObject c = cfg.getJSONObject(i);
                LogUtil.info(TAG, "slot info: " + c.toString());

                // addr=60.205.82.52:81|60.205.82.49:81
                // slot=2

                String host_addr = c.getString("addr");
                int host_slot = c.getInt("slot");

                if (host_slot != slot)
                    continue;

                String []ip_addrs = host_addr.split("\\|");
                for (int j=0;j<ip_addrs.length;j++) {
                    String url = ip_addrs[j];
                    if (!url.startsWith("http://"))
                        url = "http://" + ip_addrs[j];
                    ipList.add(url);
                }
                //int index = host_addr.indexOf("|");
                //ipList.add("http://" + host_addr.substring(0, index));
                //ipList.add("http://" + host_addr.substring(index + 1, host_addr.length()));
            }

            return ipList;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String chatServer(String host) {
        LogUtil.info(TAG, "chatServer(): " + host);

        String url = host + "/socket.io/1/" + api_surfix;
        LogUtil.info(TAG, "chatServer url: " + url);
        //Map<String, String> headers = UserAccountTokenManager.ins().getRequestHeaders("/socket.io/1/");
        String result = Util.postHttpPage(url, null);
        if (result == null) {
            LogUtil.error(TAG, "failed to call chatServer()");
            return null;
        }

        // tPMGQcMob7fpF1EV5toY:60:60:websocket
        LogUtil.info(TAG, "chatServer() result: " + result);
        int index = result.indexOf(":");
        if (index < 0) {
            LogUtil.error(TAG, "failed to find websocket token");
            return null;
        }

        String token = result.substring(0, index);
        LogUtil.info(TAG, "websocket token: " + token);
        String websocket_url = host + "/socket.io/1/websocket/" + token + api_surfix;
        return websocket_url;
    }

    public static String chatServer2(String uid, String id) {
        LogUtil.info(TAG, String.format(Locale.US, "chatServer2(): %s %s", uid, id));

        String url = String.format(live_share_pc_api, Integer.valueOf(uid), id);
        LogUtil.info(TAG, "chatServer2 url: " + url);
        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to call chatServer2()");
            return null;
        }

//        {
//            error_code: 0,
//                    message: "ok",
//                data: {
//            media_info: {
//                nick: "三八蔓",
//                        portrait: "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage?url=http%3A%2F%2Fimg.meelive.cn%2FMTQ3MjU3OTk4ODQ1MyM3MTIjanBn.jpg&w=100&h=100&s=80&c=0&o=0",
//                        inke_id: 6543260,
//                        level: 60,
//                        gender: 0,
//                        gender_img: "/Public/images/gender-0.png",
//                        area: "宁波市",
//                        description: "看心情播",
//                        level_img: "http://img.meelive.cn/MjczMTExNDM1OTIyMTE2.jpg"
//            },
//            file: {
//                status: 1,
//                record_url: "rtmp://pull99.a8.com/live/1472709665633273",
//                online_users: 18691,
//                title: "你丑你先睡，我美我直播！正在直播，快来一起看~",
//                pic: "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage?url=http%3A%2F%2Fimg.meelive.cn%2FMTQ3MjU3OTk4ODQ1MyM3MTIjanBn.jpg&w=520&h=836&s=80&c=0&o=0",
//                city: "宁波市"
//            },
//            records: [ ],
//            status: 1,
//            portrait: "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage?url=http%3A%2F%2Fimg.meelive.cn%2FMTQ3MjU3OTk4ODQ1MyM3MTIjanBn.jpg&w=100&h=100&s=80&c=0&o=0",
//            point: 24239807,
//            sio_ip: "101.201.40.64:81",
//            liveid: "1472709665633273",
//            live_uid: "6543260",
//            is_follow: 0,
//            weibo_url: "https://api.weibo.com/oauth2/authorize?client_id=2444295379&redirect_uri=http%3A%2F%2Fopen.inke.tv%2Fweibo%2Flogin_pc&response_type=code",
//            weixin_url: "https://open.weixin.qq.com/connect/qrconnect?appid=wx9b1840345c3386de&redirect_uri=http%3A%2F%2Fvideo.inke.com%2Fweixin%2Flogin_pc%2Fuid%3D6543260%26id%3D1472709665633273%2F&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect",
//            sec: "525b1da5d9fdb6247fb245eca27e2066",
//            view_uid: "",
//            nonce: "djzVCGC3Be",
//            time: 1472710637,
//            sio_md5: "",
//            view_nick: "",
//            token: "",
//            token_time: "",
//            origin: null,
//            is_login: 0
//        }
//        }


//        {
//            record_url: "http://record2.a8.com/mp4/1472623404847989.mp4",
//                    online_users: 58455,
//                city: "长春市",
//                title: "你丑你先睡，我美我直播！嘉。正在直播，快来一起看~",
//                img: "http://img.meelive.cn/MzI5NjYxNDYwMDQ4OTM0.jpg",
//                pic: "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage?url=http%3A%2F%2Fimg.meelive.cn%2FMzI5NjYxNDYwMDQ4OTM0.jpg&w=346&h=260&s=80&c=0&o=0",
//                liveid: "1472623404847989"
//        },

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

            int error_code = root.getInt("error_code");
            if (error_code != 0) {
                String message = root.getString("message");
                LogUtil.error(TAG, "failed to chatServer2, error_msg: " + message);
                return null;
            }

            JSONObject data = root.getJSONObject("data");
            JSONObject media_info = data.getJSONObject("media_info");
            String nick = media_info.getString("nick");
            String portrait = media_info.getString("portrait");
            int inke_id = media_info.getInt("inke_id");

            JSONObject file = data.getJSONObject("file");
            int status = file.getInt("status");
            String record_url = file.getString("record_url");
            int online_users = file.getInt("online_users");
            String title = file.getString("title");
            String pic = file.getString("title");
            String city = file.getString("city");

            JSONArray records = data.optJSONArray("records");
            if (records != null && records.length() > 0) {
                for (int i=0;i<records.length();i++) {
                    JSONObject rec = records.getJSONObject(i);
                    String rec_record_url = rec.getString("record_url");
                    int rec_online_users = rec.getInt("online_users");
                    String rec_liveid = rec.getString("liveid");
                    LogUtil.info(TAG, String.format(Locale.US,
                            "rec %s, users %d, liveid %s",
                            rec_record_url, rec_online_users, rec_liveid));
                }
            }

            String sio_ip = data.getString("sio_ip");
            String liveid = data.getString("liveid");
            String live_uid = data.getString("live_uid");
            String sec = data.getString("sec");
            String nonce = data.getString("nonce");
            long time = data.getLong("time");

            // http://101.201.40.173:81/socket.io/1/
            // ?uid=
            // &place=room
            // &sid=1
            // &roomid=1472709367929772
            // &token=
            // &time=1472711317
            // &nonce=AtaAOYDzsK
            // &sec=f4a28c987fd612e8b8aebe5c87351677
            // &t=1472711317457

            String get_token_url = "http://" + sio_ip + "/socket.io/1/" +
                    "?uid=" +
                    "&place=room" +
                    "&sid=1" +
                    "&roomid=" + liveid +
                    "&token=" +
                    "&time=" + time +
                    "&nonce=" + nonce +
                    "&sec=" + sec +
                    "&t=" + time;
            LogUtil.info(TAG, "get_token_url: " + get_token_url);

            result = Util.getHttpPage(get_token_url);
            if (result == null) {
                LogUtil.error(TAG, "failed to get sio_token");
                return null;
            }

            //WKq73Pi7_dSkY3_sItRY:60:60:websocket
            LogUtil.info(TAG, "sio_token result: " + result);
            int index = result.indexOf(":");
            if (index < 0) {
                LogUtil.error(TAG, "failed to find websocket token");
                return null;
            }

            String token = result.substring(0, index);
            LogUtil.info(TAG, "websocket token: " + token);

            // ws://101.201.40.173:81/socket.io/1/websocket/WKq73Pi7_dSkY3_sItRY
            // ?uid=
            // &place=room
            // &sid=1
            // &roomid=1472709367929772
            // &token=
            // &time=1472711317
            // &nonce=AtaAOYDzsK
            // &sec=f4a28c987fd612e8b8aebe5c87351677

            String sio_url = "ws://" + sio_ip + "/socket.io/1/websocket/" + token +
                    "?uid=" +
                    "&place=room" +
                    "&sid=1" +
                    "&roomid=" + liveid +
                    "&token=" +
                    "&time=" + time +
                    "&nonce=" + nonce +
                    "&sec=" + sec;
            LogUtil.info(TAG, "sio_url: " + sio_url);
            return sio_url;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getToken(String secret) {
        String url = account_token_api;

        // {"time":1472708405,"sec":"853dd7b4669aab59d638c2c9a056ddae"}
        JSONObject json = new JSONObject();
        try {
            json.put("time", System.currentTimeMillis() / 1000);
            json.put("sec", secret);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        LogUtil.info(TAG, "post data: " + json.toString());
        String result = Util.postHttpPage(url, json.toString());
        if (result == null) {
            LogUtil.error(TAG, "failed to call chatServer()");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "请求参数错误",
//            dm_error: 499
//            expire=1800
            // md5=44c925659397a459cfd4ee9dc6f40f32
            // timestamp=1472708402
            // token=187f67af4e6e96fb1ae2dde6d7267bba1b3abd1b7bc3c3d09b86bbbe
            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to getToken , error_msg: " + error_msg);
                return null;
            }

            return root.getString("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean follow(int uid) {
        return follow_impl(uid, true);
    }

    public static boolean unfollow(int uid) {
        return follow_impl(uid, false);
    }

    private static boolean follow_impl(int uid, boolean follow) {
        String action = follow ? "follow" : "unfollow";

        LogUtil.info(TAG, "follow_impl(): " + uid + " , action: " + action);

        //{"id":7197535}
        JSONObject json = new JSONObject();
        try {
            json.put("id", uid);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        LogUtil.info(TAG, "post data: " + json.toString());
        String api_url = follow ? follow_api : unfollow_api;
        String result = Util.postHttpPage(api_url, json.toString());
        if (result == null) {
            LogUtil.error(TAG, "failed to " + action);
            return false;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "请求参数错误",
//            dm_error: 499
            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to " + action + " , error_msg: " + error_msg);
                return false;
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String relation(int uid) {
        LogUtil.info(TAG, "relation() uid: " + uid);

        String url = String.format(Locale.US, relation_api, uid);

        LogUtil.info(TAG, "relation url: " + url);
        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to get relation url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

            //dm_error=0
            //error_msg=操作成功
            //relation=following

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to relation(): " + error_msg);
                return null;
            }

            String relation = root.getString("relation");
            return relation;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<UserInfo> search(int uid) {
        LogUtil.info(TAG, "search() uid: " + uid);

        String url = String.format(Locale.US, search_api, uid);
        LogUtil.info(TAG, "search() url: " + url);

        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to get search url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "高峰期间4级以下用户不能直播，早8-晚6点直播无限制！看直播、送礼物都可升级解锁直播哦！",
//            dm_error: 10001

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to getUsers(): " + error_msg);
                return null;
            }

            /*
            dm_error=0
            error_msg=操作成功
            start=0
            total=1
            users
                relation=null
                user
                    birth=
                    description=陪伴是最长情的告白 @hedonghong
                    emotion=恋爱中
                    ...
                    id=49467302
                    level=15
                    location=杭州市
                    nick=Inke49467302
                    portrait=OTYwODQxNDY1OTI1NDg5.jpg
                    ...
            */

            int total = root.getInt("total");
            if (total == 0)
                return null;

            JSONArray users = root.getJSONArray("users");
            int count = users.length();

            List<UserInfo> list = new ArrayList<>();
            for (int i=0;i<count;i++) {
                JSONObject userItem = users.getJSONObject(i);
                String relation = userItem.getString("relation");
                JSONObject user = userItem.getJSONObject("user");

                int id = user.getInt("id");
                String location = user.getString("location");
                String portrait = user.getString("portrait");
                String nick = user.getString("nick");

                String img_url;
                if (portrait.startsWith("http://"))
                    img_url = portrait;
                else
                    img_url = "http://img.meelive.cn/" + portrait;
                String encoded_img_url;
                try {
                    encoded_img_url = URLEncoder.encode(img_url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    continue;
                }
                String show_url = "http://image.scale.a8.com/imageproxy2/dimgm/scaleImage";
                show_url += "?url=";
                show_url += encoded_img_url;
                show_url += "&w=360&h=360&s=80&c=0&o=0";

                list.add(new UserInfo(id, nick, location, show_url));
            }

            return list;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static PublishResult getNowPublish(int uid) {
        LogUtil.info(TAG, "getNowPublish() uid: " + uid);

        String url = String.format(Locale.US, now_publish_api, uid);
        LogUtil.info(TAG, "getNowPublish() url: " + url);

        String result = Util.getHttpPage(url);
        if (result == null) {
            LogUtil.error(TAG, "failed to get nowpublish url");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "高峰期间4级以下用户不能直播，早8-晚6点直播无限制！看直播、送礼物都可升级解锁直播哦！",
//            dm_error: 10001

            int dm_error = root.getInt("dm_error");
            String error_msg = root.optString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to getUsers(): " + error_msg);
                return null;
            }

            //{"dm_error":0,
            // "error_msg":"操作成功",
            // "live":{
            //      "city":"吉林市",
            //      "creator":9885779,
            //      "id":"1468547197099174",
            //      "image":"",
            //      "name":"我来告别的��",
            //      "pub_stat":1,
            //      "room_id":432541305,
            //      "share_addr":"http://live6.a8.com/s/?uid=9885779&liveid=1468547197099174&ctime=1468547197",
            //      "slot":6,
            //      "status":1,
            //      "stream_addr":"http://pull99.a8.com/live/1468547197099174.flv",
            //      "version":0,
            //      "link":0,
            //      "optimal":0,
            //      "multi":0,"rotate":0}}

            JSONObject live = root.optJSONObject("live");
            if (live == null) {
                LogUtil.error(TAG, "failed to find live node");
                return null;
            }

            String city = live.getString("city");
            int creator = live.getInt("creator");
            String name = live.getString("name");
            String stream_addr = live.getString("stream_addr");
            String id = live.getString("id");
            int room_id = live.getInt("room_id");
            String share_addr = live.getString("share_addr");
            int slot = live.getInt("slot");

            return new PublishResult(id, room_id,
                    "", stream_addr, share_addr, slot,
                    city, creator, name, "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean startPublishUrl(String id) {
        LogUtil.info(TAG, "startPublishUrl(): " + id);

        JSONObject live = new JSONObject();
        try {
            live.put("city", "重庆市");
            live.put("id", id);
            live.put("location", "蜀中区");
            live.put("name", "Joker");
            live.put("relate_id", "");
            live.put("stat", "pub");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String json = live.toString();
        LogUtil.info(TAG, "json data: " + json);
        String result = Util.postHttpPage(start_publish_api, json);
        if (result == null) {
            LogUtil.error(TAG, "failed to start publish");
            return false;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "请求参数错误",
//            dm_error: 499String stream_addr = root.getString("stream_addr");
            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to start publish, error_msg: " + error_msg);
                return false;
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static PublishResult updatePublishUrl(String id) {
        LogUtil.info(TAG, "updatePublishUrl(): " + id);

        JSONObject live = new JSONObject();
        try {
            live.put("city", "重庆市");
            live.put("id", id);
            live.put("image", "Mjc1NjYxNDY2MDM4NDg0.jpg");
            live.put("multiaddr", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String json = live.toString();
        LogUtil.info(TAG, "json data: " + json);
        String result = Util.postHttpPage(update_publish_api, json);
        if (result == null) {
            LogUtil.error(TAG, "failed to update publish");
            return null;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "请求参数错误",
//            dm_error: 499String stream_addr = root.getString("stream_addr");
            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to start publish, error_msg: " + error_msg);
                return null;
            }

            /*
            city=上海市
            creator=65286584
            id=1466038853474424
            image=Mjc1NjYxNDY2MDM4NDg0.jpg
            name=
            other_addr=
            pub_stat=1
            publish_addr=rtmp://istream5.a8.com/live/1466038853474424
            room_id=351201120
            share_addr=http://live5.a8.com/s/?uid=65286584&liveid=1466038853474424&ctime=1466038853
            slot=3
            status=1
            stream_addr=http://pull5.a8.com/live/1466038853474424.flv
            version=0
            */

            JSONObject live_info = root.getJSONArray("live").getJSONObject(0);
            String city = live_info.getString("city");
            int creator = live_info.getInt("creator");
            String live_id = live_info.getString("id");
            String image = live_info.getString("image");
            String name = live_info.getString("name");
            String publish_addr = live_info.getString("publish_addr");
            int room_id = live_info.getInt("room_id");
            String share_addr = live_info.getString("share_addr");
            String stream_addr = live_info.getString("stream_addr");
            int slot = live_info.getInt("slot");

            return new PublishResult(
                    live_id, room_id, publish_addr, stream_addr, share_addr, slot,
                    city, creator, name, image);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void stopPublishUrl(String id) {
        LogUtil.info(TAG, "stopPublishUrl(): " + id);

        JSONObject live = new JSONObject();
        try {
            live.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String json = live.toString();
        LogUtil.info(TAG, "json data: " + json);

        String result = Util.postHttpPage(stop_publish_api, json);
        if (result == null) {
            LogUtil.error(TAG, "failed to stop publish");
            return;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

//            error_msg: "请求参数错误",
//            dm_error: 499String stream_addr = root.getString("stream_addr");
            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to stop publish, error_msg: " + error_msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean keepalivePublish(long id) {
        JSONObject live = new JSONObject();
        try {
            live.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String result = Util.postHttpPage(keepalive_publish_api, live.toString());
        if (result == null) {
            LogUtil.error(TAG, "failed to get publish url");
            return false;
        }

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

            //error_msg: "请求参数错误",
            //dm_error: 499

            int dm_error = root.getInt("dm_error");
            String error_msg = root.getString("error_msg");
            if (dm_error != 0) {
                LogUtil.error(TAG, "failed to keep alive: " + error_msg);
                return false;
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }
}
