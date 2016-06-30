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

/**
 * Created by Michael.Ma on 2016/5/3.
 */
public class InkeUtil {
    private final static String TAG = "InkeUtil";

    private final static String api_surfix =
            "?lc=3000000000003002" +
                    "&cv=IK2.6.10_Android" +
                    "&cc=TG36001" +
                    "&ua=YuLongCoolpad8297-C00" +
                    "&uid=67302632" +
                    "&sid=E0rgPgFY8Vi0bQ69Rj8qXbS7uIQi3i3" +
                    "&devi=99000558796818" +
                    "&imsi=" +
                    "&icc=" +
                    "&conn=WIFI" +
                    "&vv=1.0.2-201601131421.android" +
                    "&aid=3dc3324f515d553" +
                    "&osversion=android_19" +
                    "&proto=3";

    private final static String simpleall_api_url =
            "http://service5.ingkee.com/api/live/simpleall" +
                    api_surfix + "&multiaddr=1";

    private final static String homepage_api_url =
            "http://service5.ingkee.com/api/live/homepage_new" +
                    api_surfix;

    private final static String prepare_publish_api =
            "http://service5.ingkee.com/api/live/pre" +
                    api_surfix +
                    "&multiaddr=1";

    private final static String keepalive_publish_api =
            "http://service5.ingkee.com/api/live/keepalive" +
                    api_surfix;

    private final static String users_publish_api =
            "http://service5.ingkee.com/api/live/users" +
                    api_surfix +
                    "&count=%d" +
                    "&id=%s" + // 1466038853474424
                    "&start=0";

    private final static String start_publish_api =
            "http://service5.ingkee.com/api/live/start" +
                    api_surfix;

    private final static String update_publish_api =
            "http://service5.ingkee.com/api/live/update" +
                    api_surfix;

    private final static String stop_publish_api =
            "http://service5.ingkee.com/api/live/stop" +
                    api_surfix;

    private final static String search_api =
            "http://service5.ingkee.com/api/user/search" +
                    api_surfix +
                    "&count=10" +
                    "&start=0" + // offset
                    "&keyword=%d"; // uid 67302632

    private final static String now_publish_api =
            "http://service5.ingkee.com/api/live/now_publish" +
                    api_surfix +
                    "&id=%d&multiaddr=1"; // uid 67302632

    public static class LiveInfo {
        public int mUserId;
        public String mTitle;
        public String mImage;
        public String mPlayUrl;
        public String mLocation;
        public String mShareAddr;
        public int mOnlineUsers;

        private LiveInfo() {

        }

        public LiveInfo(int uid, String title, String image, String playUrl, String location,
                        String shareAddr, int onlineUsers) {
            this.mUserId = uid;
            this.mTitle = title;
            this.mImage = image;
            this.mPlayUrl = playUrl;
            this.mLocation = location;
            this.mShareAddr = shareAddr;
            this.mOnlineUsers = onlineUsers;
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
        public String mId;
        public int mRoomId;
        public String mPushUrl;
        public String mStreamUrl;
        public String mShareUrl;

        public String mCity;
        public int mCreator;
        public String mImage;

        public PublishResult(String id, int room_id,
                             String push_url, String stream_url, String share_url) {
            this(id, room_id, push_url, stream_url, share_url, null, 0, null);
        }

        public PublishResult(String id, int room_id,
                             String push_url, String stream_url, String share_url,
                             String city, int creator, String image) {
            this.mId = id;
            this.mRoomId = room_id;
            this.mPushUrl = push_url;
            this.mStreamUrl = stream_url;
            this.mShareUrl = share_url;

            this.mCity = city;
            this.mCreator = creator;
            this.mImage = image;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format(Locale.US,
                    "id: %s, roomId: %d, push_url: %s, stream_url: %s, share_url: %s",
                    mId, mRoomId, mPushUrl, mStreamUrl, mShareUrl));
            if (mCity != null) {
                sb.append(", city: ");
                sb.append(mCity);
            }
            if (mCreator != 0) {
                sb.append(", creator: ");
                sb.append(mCreator);
            }
            if (mImage != null) {
                sb.append(", image: ");
                sb.append(mImage);
            }

            return sb.toString();
        }
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

                list.add(new LiveInfo(uid, title, show_url, play_url,
                        city, share_addr, online_users));
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

            return new PublishResult(id, room_id, publish_addr, stream_addr, share_addr);
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

    public static List<UserInfo> search(int uid) {
        LogUtil.info(TAG, "search() uid: " + uid);

        String url = String.format(Locale.US, search_api, uid);

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

            /*
            dm_error=0
            error_msg=操作成功
            live
                city=上海市
                creator=67302632
                id=1466039078583477
                image=MzMyMDYxNDY2MDM4NzA4.jpg
                ...
                publish_addr=rtmp://istream2.a8.com/live/1466039078583477
                room_id=351205311
                share_addr=http://live2.a8.com/s/?uid=67302632&liveid=1466039078583477&ctime=1466039078
                ...
                stream_addr=http://pull2.a8.com/live/1466039078583477.flv
            */

            JSONObject live = root.getJSONObject("live");
            if (live == null)
                return null;

            String stream_addr = root.getString("stream_addr");
            String id = root.getString("id");
            int room_id = root.getInt("room_id");
            String publish_addr = root.getString("publish_addr");
            String share_addr = root.getString("share_addr");
            return new PublishResult(id, room_id, publish_addr, stream_addr, share_addr);
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
            String publish_addr = live_info.getString("publish_addr");
            int room_id = live_info.getInt("room_id");
            String share_addr = live_info.getString("share_addr");
            String stream_addr = live_info.getString("stream_addr");

            return new PublishResult(
                    live_id, room_id, publish_addr, stream_addr, share_addr,
                    city, creator, image);
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
