package com.gotye.common.youku;

import android.content.Context;
import android.os.Environment;

import com.gotye.common.ZGUrl;
import com.gotye.common.util.Base64Util;
import com.gotye.common.util.CryptAES;
import com.gotye.common.util.LogUtil;
import com.gotye.common.util.httpUtil;
import com.gotye.meetplayer.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class YKUtil {
	private final static String TAG = "YKUtil";

    private final static String YOUKU_USER_AGENT =
            "Youku HD;3.9.4;iPhone OS;7.1.2;iPad4,1";

    private final static String YOUKU_USER_AGENT2 =
            "User-Agent: Mozilla/5.0 (iPod; CPU iPhone OS_5_1_1 like Mac OS X) " +
                    "AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 " +
                    "Mobile/9B206 Safari/7534.48.3";

    private final static String DESKTOP_CHROME_USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/46.0.2490.86 Safari/537.36";

    private final static String youku_page =
            "http://v.youku.com/v_show/id_%s.html?x";

    private final static String youku_channel_api =
            "http://api.mobile.youku.com/layout/android5_0/channel/" +
                    "tags?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI";

    private final static String youku_catalog_api =
            "http://api.mobile.youku.com/layout/android/channel/" +
                    "subtabs?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI" +
                    "&cid=%d";

    private final static String youku_subpage_api =
            "http://api.mobile.youku.com/layout/v_5/android/" +
                    "channel/subpage?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    //"&_t_=1459479510" +
                    //"&e=md5" +
                    //"&_s_=1c024ab0b0c0de314a425423e8ab021e" +
                    "&network=WIFI" +
                    "&cid=%d" +
                    "&sub_channel_id=%d" +
                    "&sub_channel_type=%d" +
                    "&ob=%d" + // 2 sort 1-最新发布, 2-最多播放
                    "&show_game_information=1" +
                    "&pg=%d" + // index from 1
                    "&pz=%d"; // page size

    private final static String youku_filter_api =
            "http://api.mobile.youku.com/layout/android3_0/" +
                    "channel/filter?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "ver=5.4.4" +
                    //"&_t_=1459481528
                    // &e=md5
                    // &_s_=cb0d373a00025401298039dc5f721d73
                    "&network=WIFI" +
                    "&cid=%d"; // 86

    private final static String youku_album_info_api =
            "http://api.mobile.youku.com/layout/android5_0/" +
                    "play/detail?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI" +
                    "&format=" +
                    "&id=%s" +
                    "&area_code=1";

    private final static String youku_episode_api =
            "http://api.mobile.youku.com/shows/" +
                    "%s/reverse/videos" +
                    "?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI" +
                    "&fields=vid|titl|lim|is_new|pv" +
                    "&pg=%d" +
                    "&pz=%d" +
                    "&area_code=1";

    private final static String youku_search_api =
            "http://api.appsdk.soku.com/u/s?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&_t_=1457661257" +
                    "&e=md5" +
                    "&_s_=5f8a9e3cf9f057cd5bc886d9b4d3897d" +
                    "&network=WIFI" +
                    "&format=&cid=&seconds=0&seconds_end=0" +
                    "&ob=0" +
                    "&pg=%d" +
                    "&pz=%d" +
                    "&aaid=145766125936696352155" +
                    "&brand=Coolpad" +
                    "&btype=Coolpad+8297-C00" +
                    "&sdkver=6&kref=2&area_code=1" +
                    "&keyword=";

    private final static String apiurl =
            "http://i.play.api.3g.youku.com/common/v3/play" +
                    "?audiolang=1" +
                    "&guid=7066707c5bdc38af1621eaf94a6fe779" +
                    "&ouid=3151cdbf1449478fad97c27cd5fa755b2fff49fa" +
                    "&ctype=%s" + // param0
                    "&did=%s" + // param1
                    "&language=guoyu" +
                    "&local_point=0" +
                    "&brand=apple" +
                    "&vdid=924690F1-A141-446B-A5E5-4A5A778BB4F5" +
                    "&local_time=0" +
                    "&os=ios" +
                    "&point=1" +
                    "&os_ver=7.1.2" +
                    "&id=d1d065eafb1411e2a705" +
                    "&deviceid=0f607264fc6318a92b9e13c65db7cd3c" +
                    "&ver=3.9.4" +
                    "&format=1,3,6,7" +
                    "&network=WIFI" +
                    "&btype=iPad4,1" +
                    "&vid=%s" + // param2
                    "&pid=87c959fb273378eb" +
                    "&local_vid=%s"; // param3

    private final static String api2url =
            "http://play.youku.com/play/get.json" +
                    "?vid=%s" +
                    "&ct=%s";

    private final static String youku_soku_api =
            "http://www.soku.com/search_video/q_";

    private final static String m3u8_url_fmt =
            "http://pl.youku.com/playlist/m3u8" +
                    "?ts=%s" + // useless?
                    "&keyframe=1" +
                    "&vid=%s" +
                    "&sid=%s" +
                    "&token=%s" +
                    "&oip=%s" +
                    "&did=%s" + // useless?
                    "&ctype=%s" +
                    "&ev=1" +
                    "&ep=%s" +
                    "&type=%s";

    private final static String m3u8_url_fmt2 =
            "http://pl.youku.com/playlist/m3u8" +
                    "?ctype=12" +
                    "&ep=%s" +
                    "&ev=1" +
                    "&keyframe=1" +
                    "&oip=%s" +
                    "&sid=%s" +
                    "&token=%s" +
                    "&vid=%s" +
                    "&type=%s";

    private final static String relate_api =
            "http://api.mobile.youku.com/common/shows/relate" +
                    "?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI" +
                    "&id=%s" +
                    "&apt=3" +
                    "&pg=%d" +
                    "&md=1" +
                    "&pz=20";

    public static final int API_METHOD_V3_PLAY     = 1;
    public static final int API_METHOD_GET_JSON    = 2;

    private static String YKss = null;
    private static int api_method = API_METHOD_V3_PLAY;

    public static void setApiMethod(int method) {
        if (method != 1 && method !=2 ) {
            LogUtil.error(TAG, "NOT supported method: " + method);
        }
    }

    public static String getPlayYoukuUrl(String youku_url) {
        LogUtil.info(TAG, "Java: getPlayYoukuUrl: " + youku_url);

        String vid = getVid(youku_url);
        if (vid == null)
            return null;
        if (api_method == API_METHOD_V3_PLAY)
            return getPlayUrl(vid);
        else
            return getPlayUrl2(vid);
    }

    public static ZGUrl getPlayZGUrl(Context context, String vid) {
        return getPlayZGUrl(context, vid, 2);
    }

    public static ZGUrl getPlayZGUrl(Context context, String vid, int ft) {
        if (YKss == null) {
            // try get ykss from storage

            // load valid YKss
            YKss = Util.readSettings(context, "ykss");
            if (!YKss.isEmpty())
                LogUtil.info(TAG, "ykss loaded: " + YKss);

            // always try to get kyss, maybe cannot get because last ykss is still valid
            String new_ykss = getYKss(null, vid);
            if (new_ykss != null) {
                YKss = new_ykss;
                Util.writeSettings(context, "ykss", YKss);
                LogUtil.info(TAG, "ykss updated: " + YKss);
            }
        }

        String m3u8_url;
        if (api_method == API_METHOD_V3_PLAY)
            m3u8_url = getPlayUrl(vid, ft);
        else
            m3u8_url = getPlayUrl2(vid, ft);
        if (m3u8_url == null) {
            LogUtil.error(TAG, "Java: failed to get m3u8 url, vid: " + vid);
            return null;
        }

        LogUtil.info(TAG, "m3u8_url: " + m3u8_url);

        String Cookies = "__ysuid=" + YKUtil.getPvid(3) +
                ";xreferrer=http://www.youku.com/" +
                ";ykss=" + YKss;
        LogUtil.info(TAG, "Cookies: " + Cookies);
        byte []buffer = new byte[65536 * 4];
        int content_size = httpUtil.httpDownloadBuffer(m3u8_url, Cookies, 0, buffer);
        if (content_size <= 0) {
            LogUtil.error(TAG, "Java: failed to download m3u8 file: " + m3u8_url);
            return null;
        }
        byte []m3u8_context = new byte[content_size];
        System.arraycopy(buffer, 0, m3u8_context, 0, content_size);

        String str_m3u8;
        try {
            str_m3u8 = new String(m3u8_context, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LogUtil.error(TAG, "failed to convert m3u8_context to string");
            return null;
        }

        //return YKUtil.getZGUrls(vid);
        return YKUtil.parseM3u8(vid, str_m3u8);
    }

    private static String getStreamType(int ft) {
        //type=mp4（高清）可替换为type=flv（标清）、type=hd2（超清）、type=hd3（1080p）
        String stream_type = "flv";
        switch (ft) {
            case 0:
                stream_type = "flv";
                break;
            case 1:
                stream_type = "mp4";
                break;
            case 2:
                stream_type = "hd2";
                break;
            case 3:
                stream_type = "hd3";
                break;
            default:
                break;
        }

        return stream_type;
    }

    private static String getPlayUrl(String vid) {
        return getPlayUrl(vid, 2);
    }

	private static String getPlayUrl(String vid, int ft) {
        LogUtil.info(TAG, String.format(Locale.US,
                "Java: getPlayUrl() vid %s, ft %d", vid, ft));
		
		String ctype = "20";//指定播放ID 20
        String did = md5(ctype + "," + vid);
        long sec = System.currentTimeMillis() / 1000;
        String tm = String.valueOf(sec);
        
        String api_url = String.format(apiurl, ctype, did, vid, vid);
        LogUtil.info(TAG, "youku api_url: " + api_url);
        String result = getHttpPage(api_url, YOUKU_USER_AGENT2, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String data = root.getString("data");
            String dec_data = yk_jsondecode(data);

            JSONTokener jsonDataParser = new JSONTokener(dec_data);
            JSONObject data_root = (JSONObject) jsonDataParser.nextValue();
            JSONObject sid_data = data_root.getJSONObject("sid_data");
            String sid = sid_data.getString("sid");
            String oip = sid_data.getString("oip");
            String token = sid_data.getString("token");

            String tmp = String.format("%s_%s_%s", sid, vid, token);
            String ep = getEP(tmp, ctype);

            //type=mp4（高清）可替换为type=flv（标清）、type=hd2（超清）、type=hd3（1080p）
            String stream_type = getStreamType(ft);
            return String.format(m3u8_url_fmt,
                    tm, vid, sid, token, oip, did, ctype, ep, stream_type);

        } catch (JSONException e1) {
            e1.printStackTrace();
        }
		
		return null;
	}

    private static String getPlayUrl2(String vid) {
        return getPlayUrl2(vid, 2);
    }

    private static String getPlayUrl2(String vid, int ft) {
        LogUtil.info(TAG, String.format(Locale.US,
                "Java: getPlayUrl2() vid %s, ft %d", vid, ft));

        String ctype = "12";//指定播放ID 12
        String api_url = String.format(api2url, vid, ctype);
        LogUtil.info(TAG, "youku api2_url: " + api_url);
        //'Referer: http://v.youku.com/v_show/'.$vid.'.html?x',$cookie));
        String refer = "http://v.youku.com/v_show/" + vid + ".html?x";
        String result = getHttpPage(api_url, YOUKU_USER_AGENT2, true, refer);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

            //$ep=$rs['data']['security']['encrypt_string'];
            //$ip=$rs['data']['security']['ip'];
            //$videoid=$rs['data']['id'];
            JSONObject data = root.getJSONObject("data");
            JSONObject security = data.getJSONObject("security");
            String ep = security.getString("encrypt_string");
            String ip = security.getString("ip");
            String videoid = data.getString("id");

            // list($sid, $token)=explode('_',yk_e('becaf9be', base64_decode($ep)));
            String tmp = myEncoder("becaf9be", Base64Util.decode(ep), false);
            LogUtil.info(TAG, "myEncoder result: " + tmp);
            String []tmp2 = tmp.split("_");
            String sid = tmp2[0];
            String token = tmp2[1];

            String tmp3 = String.format("%s_%s_%s", sid, videoid, token);

            // 解析后返回的url末尾的type=mp4（高清）
// 可替换为type=flv（标清）、type=hd2（超清）、type=hd3（1080p）
//            "http://pl.youku.com/playlist/m3u8" +
//                    "?ctype=12" +
//                    "&ep=%s" +
//                    "&ev=1" +
//                    "&keyframe=1" +
//                    "&oip=%s" +
//                    "&sid=%s" +
//                    "&token=%s" +
//                    "&vid=%s" +
//                    "&type=%s";

            //$ep=urlencode(base64_encode(yk_e('bf7e5f01',$sid.'_'.$videoid.'_'.$token)));
            String tmp4 = myEncoder("bf7e5f01", tmp3.getBytes(), true);

            //type=mp4（高清）可替换为type=flv（标清）、type=hd2（超清）、type=hd3（1080p）
            String stream_type = getStreamType(ft);

            try {
                String ep_new = URLEncoder.encode(tmp4, "UTF-8");
                return String.format(m3u8_url_fmt2,
                        ep_new, ip, sid, token, videoid, stream_type);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    private static String myEncoder(String a, byte[] c, boolean isToBase64) {
        String result = "";
        byte []tmp = new byte[1024];
        int offset = 0;
        int f = 0, h = 0, q = 0;
        int[] b = new int[256];
        for (int i = 0; i < 256; i++)
            b[i] = i;

        while (h < 256) {
            f = (f + b[h] + a.charAt(h % a.length())) % 256;
            int temp = b[h];
            b[h] = b[f];
            b[f] = temp;
            h++;
        }

        f = 0;
        h = 0;
        q = 0;
        while (q < c.length)
        {
            h = (h + 1) % 256;
            f = (f + b[h]) % 256;
            int temp = b[h];
            b[h] = b[f];
            b[f] = temp;
            byte[] bytes = new byte[] { (byte)(c[q] ^ b[(b[h] + b[f]) % 256]) };
            tmp[offset++] = bytes[0];
            result += new String(bytes);
            q++;
        }

        if (isToBase64) {
            byte []data = new byte[offset];
            System.arraycopy(tmp, 0, data, 0, offset);
            result = Base64Util.encode(data);
        }

        return result;
    }

    public static ZGUrl getZGUrls(String vid) {
        // ApiKey：66c15f2b1b2bd90244dbcc84290395f8
        // http://zg.yangsifa.com/video?url=[播放地址]&hd=[清晰度]&apikey=[自己的ApiKey]
        // v.youku.com/v_show/id_
        String api_key = "66c15f2b1b2bd90244dbcc84290395f8";
        String api_url = "http://zg.yangsifa.com/video?url=v.youku.com/v_show/id_%s==.html&hd=3";
        String url = String.format(api_url, vid, api_key);
        LogUtil.info(TAG, "getZGUrls() url: " + url);
        String result = getHttpPage(url, null, false);
        if (result == null)
            return null;

        try {
//			hd: "gq",
//			exe: "mp4",
//			vtype: "youku",
//			urls: [
//			{
//			url: "http://zg.yangsifa.com/link?url=dbf4LLph7Dxyi7t0EuaiWUYjFk6weeJigT--opEsTx24hzvGp2UEM-K0TGSehsxn29xfsUj2DRKfOkZrMQ"
//			},
//			{
//			url: "http://zg.yangsifa.com/link?url=8248iR0SztTHeaZjC1HZuv9x4G-qSq0ycVcYb1Q2BdNjvxoBqfUiAqf3PPPX0fLSfasv0R1O84Y59fmxtg"
//			},
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            JSONObject pc = root.getJSONObject("pc");
            String hd = pc.getString("hd");
            String file_type = pc.getString("exe");
            String vtype = pc.getString("vtype");
            JSONArray urls = pc.getJSONArray("urls");
            int size = urls.length();
            StringBuffer sbUrl = new StringBuffer();
            for (int i=0;i<size;i++) {
                if (i > 0)
                    sbUrl.append(",");

                JSONObject item = urls.getJSONObject(i);
                String item_url = item.getString("url");
                LogUtil.info(TAG, String.format(Locale.US,
                        "getZGUrls() url seg #%d %s", i, item_url));
                sbUrl.append(item_url);
            }

            JSONObject mobile = root.getJSONObject("mobile");
            String mobile_hd = mobile.getString("hd");
            String mobile_file_type = mobile.getString("exe");
            String mobile_vtype = mobile.getString("vtype");
            String mobile_url = mobile.getString("url");

            // get duration list
            String m3u8_url = getPlayUrl2(vid);
            LogUtil.info(TAG, "m3u8_url: " + m3u8_url);
            byte []buffer = new byte[65536 * 10];
            int content_size = httpUtil.httpDownloadBuffer(m3u8_url, 0, buffer);
            if (content_size < 0) {
                LogUtil.error(TAG, "failed to download m3u8_context");
                return null;
            }

            byte []m3u8_context = new byte[content_size];
            System.arraycopy(buffer, 0, m3u8_context, 0, content_size);

            ZGUrl tmp = parseM3u8(vid, new String(m3u8_context));
            return new ZGUrl(vid, file_type, sbUrl.toString(), tmp.durations);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static List<Channel> getChannel() {
        System.out.println("getChannel: " + youku_channel_api);

        String result = getHttpPage(youku_channel_api, null, false);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String status = root.getString("status");
            if (!status.equals("success"))
                return null;

            JSONArray channel_nav = root.getJSONArray("channel_nav");
            int size = channel_nav.length();
            List<Channel> channelList = new ArrayList<Channel>();
            for (int i=0;i<size;i++) {
//            	{
//            		channel_id: 1001,
//            		is_youku_channel: 0,
//            		content_type: 1,
//            		title: "排行榜"
//            		},


                JSONObject channel = channel_nav.getJSONObject(i);
                int channel_id = channel.getInt("channel_id");
                String title = channel.getString("title");

                channelList.add(new Channel(title, channel_id));
            }

            return channelList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static List<Catalog> getCatalog(int channel_id) {
        System.out.println("getCatalog() channel_id: " + channel_id);
        String url = String.format(youku_catalog_api, channel_id);

        LogUtil.info(TAG, "getCatalog() url: " + url);
        String result = getHttpPage(url, YOUKU_USER_AGENT2, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();

            JSONArray results = root.getJSONArray("results");
            int size = results.length();
            List<Catalog> catalogList = new ArrayList<Catalog>();
            for (int i=0;i<size;i++) {
//            	{
//            		filter: "area:大陆|tv_genre:|pay_kind:|releaseyear:",
//            		sub_channel_type: 3,
//            		image_state: 1,
//            		display_type: 0,
//            		title: "大陆剧",
//            		highlight: 0,
//            		sub_channel_id: 2
//            		},

                JSONObject item = results.getJSONObject(i);
                String filter = null;
                if (item.has("filter"))
                    filter = item.getString("filter");
                String title = item.getString("title");
                int sub_channel_type = item.getInt("sub_channel_type");
                int sub_channel_id = item.getInt("sub_channel_id");

                catalogList.add(new Catalog(title, filter,
                        sub_channel_type, sub_channel_id));
            }

            return catalogList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static class SortType {
        public String mTitle;
        public int mValue;

        public SortType(String title, int value) {
            this.mTitle = title;
            this.mValue = value;
        }

        @Override
         public String toString() {
            return String.format("sort_type: title %s, value %d", mTitle, mValue);
        }
    }

    public static class FilerType {
        public String mTitle;
        public String mValue;

        public FilerType(String title, String value) {
            this.mTitle = title;
            this.mValue = value;
        }

        @Override
        public String toString() {
            return String.format("filter_type: title %s, value %s", mTitle, mValue);
        }
    }

    public static class FilerGroup {
        public String mTitle;
        public String mCat;
        public List<FilerType> mFilterList;

        public FilerGroup(String title, String cat, List<FilerType> filter_list) {
            this.mTitle         = title;
            this.mCat           = cat;
            this.mFilterList    = filter_list;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("filter_group: title ");
            sb.append(mTitle);
            sb.append(", cat ");
            sb.append(mCat);
            sb.append(", filter_list ");
            sb.append(mFilterList.toString());
            return sb.toString();
        }
    }

    public static class FilterResult {
        public List<FilerGroup> mFilters;
        public List<SortType> mSortTypes;

        public FilterResult(List<FilerGroup> filter_list, List<SortType> sort_type_list) {
            this.mFilters = filter_list;
            this.mSortTypes = sort_type_list;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i=0;i<mFilters.size();i++) {
                if (i > 0)
                    sb.append("\n");

                sb.append("filter_group: #");
                sb.append(i);
                sb.append(" " + mFilters.get(i).toString());
            }

            for (int i=0;i<mSortTypes.size();i++) {
                sb.append("\nsort_type: #");
                sb.append(i);
                sb.append(" " + mSortTypes.get(i).toString());
            }
            return sb.toString();
        }
    }

    public static FilterResult getSearchFilter() {
        List<FilerType> ft_duration = new ArrayList<>();
        //0- 不限，1->0-10min, 2->10-30min, 3->30-60min, 4->60min+
        ft_duration.add(new FilerType("不限", "0"));
        ft_duration.add(new FilerType("0-10分钟", "1"));
        ft_duration.add(new FilerType("10-30分钟", "2"));
        ft_duration.add(new FilerType("30-60分钟", "3"));
        ft_duration.add(new FilerType("60分钟以上", "4"));

        List<FilerType> publish_time = new ArrayList<>();
        // limitdate 1->1天, 7->1周, 31->1月， 365->1年
        publish_time.add(new FilerType("不限", "0"));
        publish_time.add(new FilerType("1天", "1"));
        publish_time.add(new FilerType("1周", "7"));
        publish_time.add(new FilerType("1月", "31"));
        publish_time.add(new FilerType("1年", "365"));

        List<FilerType> resolution_ft = new ArrayList<>();
        // hd 0-不限,1-高清,6-超清,7-1080p
        resolution_ft.add(new FilerType("不限", "0"));
        resolution_ft.add(new FilerType("高清", "1"));
        resolution_ft.add(new FilerType("超清", "6"));
        resolution_ft.add(new FilerType("1080p", "7"));

        List<FilerGroup> filter_list = new ArrayList<>();
        filter_list.add(new FilerGroup("时长", "lengthtype", ft_duration));
        filter_list.add(new FilerGroup("发布时间", "limitdate", publish_time));
        filter_list.add(new FilerGroup("画质", "hd", resolution_ft));

        List<SortType> sort_type_list = new ArrayList<>();
        // orderby 1-综合排序 2-最新发布 3-最多播放
        sort_type_list.add(new SortType("综合排序", 1));
        sort_type_list.add(new SortType("最新发布", 2));
        sort_type_list.add(new SortType("最多播放", 3));

        return new FilterResult(filter_list, sort_type_list);
    }

    public static FilterResult getFilter(int channel_id) {
        //2-最多播放, 4-最具争议, 6-最多收藏, 5-最广传播, 1-最新发布
        String url = String.format(youku_filter_api, channel_id);
        LogUtil.info(TAG, "getFilter() url " + url);

        String result = getHttpPage(url, DESKTOP_CHROME_USER_AGENT, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            if (root.has("status")) {
                String status = root.getString("status");
                if (!status.equals("success"))
                    return null;
            }

            JSONObject results = root.getJSONObject("results");
            JSONArray filter = results.getJSONArray("filter");
            int count = filter.length();
            List<FilerGroup> filtergroupList = new ArrayList<FilerGroup>();
            for (int i=0;i<count;i++) {
                JSONObject item = filter.getJSONObject(i);
                String title = item.getString("title");
                String cat = item.getString("cat");
                JSONArray items = item.getJSONArray("items");
                int c = items.length();
                List<FilerType> filterList = new ArrayList<FilerType>();
                for (int j=0;j<c;j++) {
                    JSONObject f = items.getJSONObject(j);
                    String f_title = f.getString("title");
                    String f_value = f.getString("value");
                    filterList.add(new FilerType(f_title, f_value));
                }

                filtergroupList.add(new FilerGroup(title, cat, filterList));
            }

            JSONArray sort = results.getJSONArray("sort");
            count = sort.length();
            List<SortType> sortList = new ArrayList<SortType>();
            for (int i=0;i<count;i++) {
                JSONObject item = sort.getJSONObject(i);
                String title = item.getString("title");
                int value = item.getInt("value");
                sortList.add(new SortType(title, value));
            }

            return new FilterResult(filtergroupList, sortList);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static List<Album> getAlbums(
            int channel_id , String filter, int sort,
            int sub_channel_id, int sub_channel_type,
            int page, int page_size) {

        String url = String.format(Locale.US, youku_subpage_api,
                channel_id, sub_channel_id, sub_channel_type,
                sort, page, page_size);
        if (filter != null && !filter.isEmpty()) {
            try {
                String encoded_filter = URLEncoder.encode(filter, "UTF-8");
                url += "&filter=";
                url += encoded_filter;
            } catch (UnsupportedEncodingException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
                return null;
            }
        }

        LogUtil.info(TAG, "getAlbums() url " + url);
        String result = getHttpPage(url, DESKTOP_CHROME_USER_AGENT, false);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            if (root.has("status")) {
                String status = root.getString("status");
                if (!status.equals("success"))
                    return null;
            }

            // sub_channel_id=242&sub_channel_type=1 电视剧 精选
            // sub_channel_id=2&sub_channel_type=3 电视剧 大陆

            List<Album> albumList = new ArrayList<Album>();
            if (sub_channel_type == 1/*精选*/) {
                JSONArray boxes = root.getJSONArray("boxes");
                int size = boxes.length();
                for (int i=0;i<size;i++) {
                    JSONObject box = boxes.getJSONObject(i);
                    String box_title = "";
                    if (box.has("title"))
                        box_title = box.getString("title");
                    JSONArray cells = box.getJSONArray("cells");
                    for (int j=0;j<cells.length();j++) {
                        JSONObject cell = cells.getJSONObject(j);
                        JSONArray contents = cell.getJSONArray("contents");
                        for (int k=0;k<contents.length();k++) {
                            JSONObject content = contents.getJSONObject(k);
//                            {
//                                subtitle: "8.5",
//                                        img: "http://r4.ykimg.com/050C0000570342EE67BC3D03F80969CE",
//                                    title: "铁血战狼",
//                                    url: "",
//                                    paid: 0,
//                                    stripe: "更新至20集",
//                                    tid: "c31d55bc6f3411e5b692",
//                                    is_vv: 0,
//                                    type: "2"
//                            },
                            String title = box_title + " " + content.getString("title");
                            String stripe = content.getString("stripe");
                            String subtitle = content.getString("subtitle");
                            String img_url = content.getString("img");
                            String tid = content.getString("tid");
                            int is_vv = content.getInt("is_vv");
                            int type = content.getInt("type");
                            String total_vv = "";
                            if (is_vv > 0)
                                total_vv = subtitle;
                            int total_ep = 1;
                            if (type == 2)
                                total_ep = 12345678; // guess
                            albumList.add(new Album(title, tid, stripe,
                                    img_url, total_vv, total_ep));
                        }
                    }
                }
            }
            else {
                JSONArray results = root.getJSONArray("results");
                int size = results.length();
                for (int i=0;i<size;i++) {
//              {
//            	pay_type: [ ],
//            	subtitle: "9.7",
//            	img: "http://r3.ykimg.com/050E0000569861D467BC3C411601597D",
//            	title: "女医明妃传",
//            	paid: 0,
//            	stripe: "50集全",
//            	tid: "7e51f7b0864e11e38b3f",
//            	is_vv: 0,
//            	type: 2
//            	},

//                {
//                    subtitle: "177.5万",
//                    img: "http://r3.ykimg.com/0542040856FA0B0C6A0A49045E9340C2",
//                    title: "第20160329期 吴奇隆刘诗诗婚后首现身 华晨宇否认与邓紫棋恋情",
//                    paid: 0,
//                    stripe: "27:51",
//                    tid: "XMTUxNTU0ODAzMg==",
//                    is_vv: 1,
//                    type: 1
//                },

                    JSONObject item = results.getJSONObject(i);
                    String title = item.getString("title");
                    String stripe = item.getString("stripe");
                    String img_url = item.getString("img");
                    String tid = item.getString("tid");
                    int is_vv = item.getInt("is_vv");
                    int type = item.getInt("type");
                    String total_vv = "";
                    if (is_vv > 0 && item.has("subtitle")) {
                        total_vv = item.getString("subtitle");
                    }
                    int total_ep = 1;
                    if (type == 2)
                        total_ep = 12345678; // guess
                    albumList.add(new Album(title, tid, stripe,
                            img_url, total_vv, total_ep));
                }
            }

            return albumList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static Album getAlbumInfo(String tid) {
        String url = String.format(youku_album_info_api, tid);

        LogUtil.info(TAG, "getAlbumInfo() url: " + url);
        String result = getHttpPage(url, YOUKU_USER_AGENT2, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String status = root.getString("status");
            if (!status.equals("success"))
                return null;

            JSONObject detail = root.getJSONObject("detail");
            String director = "N/A";
            if (detail.has("director")) {
                JSONArray item_director = detail.getJSONArray("director");
                int size = item_director.length();
                StringBuffer sb = new StringBuffer();
                for (int i=0;i<size;i++) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(item_director.getString(i));
                }
                director = sb.toString();
            }
            String actor = "N/A";
            if (detail.has("performer")) {
                JSONArray performer = detail.getJSONArray("performer");
                int size = performer.length();
                StringBuffer sb = new StringBuffer();
                for (int i=0;i<size;i++) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(performer.getString(i));
                }
                actor = sb.toString();
            }
            String showid = null;
            if (detail.has("showid"))
                showid = detail.getString("showid");
            String title = detail.getString("title");
            String stripe_bottom = detail.getString("stripe_bottom");
            String username = detail.getString("username");
            String img = detail.getString("img");
            String desc = detail.getString("desc");
            String total_vv = detail.getString("total_vv_fmt");
            String stripe = detail.getString("stripe_bottom");
            String showdate = "N/A";
            if (detail.has("showdate"))
                showdate = detail.getString("showdate");
            String videoid = null;
            if (detail.has("videoid"))
                videoid = detail.getString("videoid");
            int episode_total = 0;
            if (detail.has("episode_total"))
                episode_total = detail.getInt("episode_total");

            return new Album(title, showid,
                    stripe, img, total_vv,
                    showdate, desc, director, actor, videoid, episode_total);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static List<Episode> getEpisodeList(String showId, int page, int page_size) {
        String url = String.format(youku_episode_api, showId, page, page_size);

        LogUtil.info(TAG, "getEpisodeList() url: " + url);
        String result = getHttpPage(url, YOUKU_USER_AGENT2, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String status = root.getString("status");
            if (!status.equals("success"))
                return null;

            JSONArray results = root.getJSONArray("results");
            int size = results.length();
            List<Episode> epList = new ArrayList<Episode>();
            for (int i=0;i<size;i++) {
                JSONObject item = results.getJSONObject(i);
//            	{
//            		total_pv: 63499520,
//            		show_videostage: 1,
//            		title: "女医明妃传 01",
//            		is_new: false,
//            		videoid: "XMTQ3MDg4Mjk4MA==",
//            		streamtypes: [
//            		"hd2",
//            		"flvhd",
//            		"hd",
//            		"3gphd",
//            		"hd3"
//            		],
//            		show_videoseq: 1,
//            		total_pv_fmt: "6350.0万",
//            		limit: 0,
//            		publicType: 0
//            	},
                String title = item.getString("title");
                String videoId = item.getString("videoid");
                JSONArray streamtypes = item.getJSONArray("streamtypes");
                List<String> StrmTypeList = new ArrayList<String>();
                for (int j=0;j<streamtypes.length();j++) {
                    StrmTypeList.add(streamtypes.getString(j));
                }
                String total_pv_fmt = item.getString("total_pv_fmt");

                epList.add(new Episode(title, videoId, StrmTypeList));
            }

            return epList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static MixResult soku(String keyword, String filter, int orderby, int page) {
        String url = null;
        try {
            String encoded_keyword = URLEncoder.encode(keyword, "UTF-8");
            url = youku_soku_api + encoded_keyword;
            // orderby 1-综合排序 2-最新发布 3-最多播放
            // page base 1
            // hd 0-不限,1-高清,6-超清,7-1080p
            // lengthtype 0- 不限，1->0-10min, 2->10-30min, 3->30-60min, 4->60min+
            // limitdate 1->1天, 7->1周, 31->1月， 365->1年
            url += "?page=";
            url += page;
            if (orderby > 0) {
                url += "&orderby=";
                url += orderby;
            }
            if (filter != null) {
                url += filter;
            }
            /*url += "&hd=";
            url += hd;
            url += "&lengthtype=";
            url += lengthtype;*/
            //综合排序 最新发布 最多播放 &od=0,1,2
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        LogUtil.info(TAG, "soku() url: " + url);
        String html = getHttpPage(url, DESKTOP_CHROME_USER_AGENT, false);
        if (html == null) {
            LogUtil.error(TAG, "failed to get http page: " + url);
            return null;
        }

        //Document doc = Jsoup.connect(url).timeout(5000).get();
        Document doc = Jsoup.parse(html);

        List<Album> albumList = null;
        List<Episode> epList = new ArrayList<Episode>();

        // get s_dir
        Elements s_dir = doc.getElementsByClass("s_dir");
        int size = s_dir.size();
        if (size > 0) {
            Elements lis = doc.getElementsByClass("s_link");
            albumList = new ArrayList<Album>();
            for (int j = 0;j<lis.size();j++) {
                Element item = lis.get(j).getElementsByTag("a").first();
                String title = item.attr("_log_title");
                String href = item.attr("href");
                String showid = item.attr("_iku_showid");
                albumList.add(new Album(title, showid, null,
                        null, null, 123456));
            }
        }

        // get video
        Elements test = doc.getElementsByClass("v");
        if (test == null || test.isEmpty()) {
            LogUtil.error(TAG, "failed to get v tag");
            return null;
        }

        size = test.size();
        for (int i = 0; i < size; i++) {
            Element v = test.get(i);
            Element v_thumb = v.getElementsByClass("v-thumb").first();
            Element v_link = v.getElementsByClass("v-link").first();
            // fix me!
            // solve http://www.soku.com/detail/show/XMTEwMTU5Mg==?siteId=14
            if (v_link == null)
                continue;

            Element v_meta = v.getElementsByClass("v-meta").first();
            Element video = v_link.getElementsByTag("a").first();
            String thumb_url = v_thumb.child(0).attr("src");
            String duration = v_thumb.getElementsByClass("v-thumb-tagrb")
                    .first().child(0).text();
            String title = video.attr("title");
            String href = video.attr("href");
            String vid = video.attr("_log_vid");
            String vv = v_meta.getElementsByClass("v-meta-entry")
                    .first().child(1).child(1).text();
            String online_time = v_meta.getElementsByClass("v-meta-entry")
                    .first().child(1).child(2).text();
            epList.add(new Episode(title, vid, thumb_url,
                    online_time, vv, duration, null));
        }

        return new MixResult(albumList, epList);
    }

    public static class MixResult {
        public List<Album> mALbumList;
        public List<Episode> mEpisodeList;

        public MixResult(List<Album> albumList, List<Episode> epList) {
            this.mALbumList = albumList;
            this.mEpisodeList = epList;
        }
    }

    public static MixResult relate(String vid, int page) {
        LogUtil.info(TAG, String.format(Locale.US, "relate() vid %s, page %d", vid, page));

        String url = String.format(Locale.US, relate_api, vid, page);
        LogUtil.info(TAG, "relate() url: " + url);
        String result = getHttpPage(url, YOUKU_USER_AGENT2, false);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String status = root.getString("status");
            if (!status.equals("success"))
                return null;

//            pubdate: "2016-01-15 16:14:48",
//            total_vv: 70892,
//            duration: 8509,
//            dct: "96",
//            ver: "A",
//            total_comment: 107,
//            img: "http://r1.ykimg.com/054204085697780D6A0A4804C63C2FEB",
//            title: "火星救援",
//            format: [
//            4,
//            5,
//            6
//            ],
//            videoid: "XMTQ0NTI3Mzk0OA==",
//            source: 120,
//            state: 3,
//            cats: "电影",
//            publicType: 0,
//            duration_fmt: "141:49",
//            img_hd: "http://r1.ykimg.com/054104085697780D6A0A4804C63C2FEB",
//            username: "焦亦丝",
//            tags: [
//            "电影",
//            "火星救援"
//            ],
//            clickLogUrl: "",
//            paid: 0,
//            total_down: 1,
//            link: "http://v.youku.com/v_show/id_XMTQ0NTI3Mzk0OA==.html",
//            total_up: 410,
//            ord: "0",
//            algInfo: "1cf001-2-1cf007-2-1lr1-2-1dtype-2h-1api-2F0107-1reqid-2F146104103506237U",
//            desc: "",
//            dma: "306",
//            stripe_bottom: "播放：7.1万",
//            cid: 96,
//            mm: "0",
//            userid: "UMzE2NTUwNTAzNg==",
//            stripe_bottom_fmt: "141:49",
//            total_vv_fmt: "7.1万",
//            total_fav: 0,
//            reputation: 8.1186461960806,
//            limit: 1,
//            req_id: "146104103506237U",

//            showid: "cbfc2f22962411de83b1",
//            dma: "1101",
//            total_vv_fmt: "446.6万",
//            clickLogUrl: "",
//            total_vv: 4465916,
//            ord: "0",
//            dct: "97",
//            pk_odshow: "13449",
//            ver: "A",
//            stripe_bottom: "8集全",
//            img: "http://r1.ykimg.com/050B0000541BBA1267379F65020B5888",
//            title: "武松",
//            mm: "0",
//            show_vthumburl: "http://r3.ykimg.com/050D0000541BBA3367379F18480DC8DB",
//            videoid: "XNTI0NTQ3OTAw",
//            stripe_bottom_fmt: "8集全",
//            algInfo: "1cf001-2-1dtype-2h-1api-2F0070-1reqid-2F1461042026169DX0",
//            reputation: 8.471,
//            req_id: "1461042026169DX0",
//            show_vthumburl_hd: "http://r3.ykimg.com/050E0000541BBA3367379F18480DC8DB",
//            series_update: "8集全",
//            duration_fmt: "00:00",
//            img_hd: "http://r1.ykimg.com/050C0000541BBA1267379F65020B5888",

            JSONArray results = root.getJSONArray("results");
            int size = results.length();
            List<Album> albumList = new ArrayList<Album>();
            List<Episode> epList = new ArrayList<Episode>();

            for (int i=0;i<size;i++) {
                JSONObject item = results.getJSONObject(i);
                if (item.has("showid")) {
                    // album
                    String showid = item.getString("showid");
                    String total_vv_fmt = item.getString("total_vv_fmt");
                    int total_vv = item.getInt("total_vv");
                    String stripe_bottom = item.getString("stripe_bottom");
                    String img_url = item.getString("img");
                    String title=  item.getString("title");
                    String videoid = item.getString("videoid");
                    double reputation = item.getDouble("reputation");
                    String series_update = item.getString("series_update");
                    albumList.add(new Album(title, showid, stripe_bottom,
                            img_url, total_vv_fmt, 123456));
                }
                else {
                    String pubdate = item.getString("pubdate");
                    int total_vv = item.getInt("total_vv");
                    int duration = item.getInt("duration");
                    String img_url = item.getString("img");
                    String title=  item.getString("title");
                    JSONArray formats = item.getJSONArray("format");
                    int []format = new int[formats.length()];
                    for(int j=0;j<formats.length();j++)
                        format[j] = formats.getInt(j);
                    String videoid = item.getString("videoid");
                    String duration_fmt = item.getString("duration_fmt");
                    String link = item.getString("link");
                    String stripe_bottom = item.getString("stripe_bottom");
                    String total_vv_fmt = item.getString("total_vv_fmt");
                    epList.add(new Episode(title, videoid, img_url,
                            pubdate, total_vv_fmt, duration_fmt, null));
                }
            }

            return new MixResult(albumList, epList);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    @Deprecated
    public static List<Album> search(String keyword,
                                     int page, int page_size) {
        LogUtil.info(TAG, "Java: search() keyword " + keyword);

        String url;
        try {
            String encoded_keyword = URLEncoder.encode(keyword, "UTF-8");
            url = String.format(youku_search_api, page, page_size) +
                    encoded_keyword;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        LogUtil.info(TAG, "Java: search() url: " + url);
        String result = getHttpPage(url, YOUKU_USER_AGENT2, false);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            JSONArray results = root.getJSONArray("results");
            int size = results.length();
            List<Album> albumList = new ArrayList<Album>();
            for(int i=0;i<size;i++) {
                JSONObject item = results.getJSONObject(i);
                if (!item.has("showid"))
                    continue;

                String title = item.getString("title");
                String showid = item.getString("showid");
                String videoId = item.getString("videoid");
                String img_url = item.getString("img");
                String pubdate = item.getString("pubdate");
                albumList.add(new Album(title, showid, pubdate,
                        img_url, null, 1));
            }

            return albumList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    private static String getHttpPage(String url,
                                      String UserAgent,
                                      boolean setEncoding) {
        return getHttpPage(url, UserAgent, setEncoding, null);
    }

	private static String getHttpPage(String url,
                                      String UserAgent,
                                      boolean setEncoding,
                                      String refer) {
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
            URL realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间
            if (UserAgent != null)
                conn.setRequestProperty("User-Agent", UserAgent);
			if (setEncoding)
				conn.setRequestProperty("Encoding", "gzip, deflate");
            if (refer != null)
                conn.setRequestProperty("Referer", refer);

            if (YKss != null) {
                String Cookies = "__ysuid=" + YKUtil.getPvid(3) +
                        ";xreferrer=http://www.youku.com/" +
                        ";ykss=" + YKss;
                conn.setRequestProperty("Cookie", Cookies);
            }

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

    public static String generateM3u8(String m3u8_context) {
        List<String> UrlList = new ArrayList<String>();
        List<Integer> DurationList = new ArrayList<Integer>();

        // http://[^?]+\?ts_start=0
        String regSeg = "http://[^?]+\\?ts_start=0";
        Pattern patternSeg = Pattern.compile(regSeg);
        Matcher matcherSeg = patternSeg.matcher(m3u8_context);

        // #EXT.+
        String regExt = "#EXT.+";
        Pattern patternExt = Pattern.compile(regExt);
        Matcher matcherExt = patternExt.matcher(m3u8_context);

        while (matcherSeg.find()) {
            String url = matcherSeg.group(0);
            int pos = url.lastIndexOf(".ts?ts_start=0");

            UrlList.add(url.substring(0, pos));
        }

        float duration = 0f;
        while (matcherExt.find()) {
            String line = matcherExt.group(0);
            if (line.startsWith("#EXTINF")) {
                //#EXTINF:11.72,
                int pos1, pos2;
                pos1 = line.indexOf(":");
                pos2 = line.lastIndexOf(",");
                String tmp = line.substring(pos1 + 1, pos2);
                if (pos1 < 0 || pos2 < 0)
                    continue;

                boolean isFloat = tmp.matches("[\\d]+\\.[\\d]+");
                if (isFloat)
                    duration += Float.parseFloat(tmp);
                else
                    duration += (float)Integer.parseInt(tmp);
            }
            else if (line.contains("#EXT-X-DISCONTINUITY") || line.contains("#EXT-X-ENDLIST")) {
                DurationList.add((int)duration);
                duration = 0f;
            }
        }

        for (int i=0;i<UrlList.size();i++) {
            LogUtil.info(TAG, String.format("url #%d %s", i, UrlList.get(i)));
        }
        int total_duration = 0;
        for (int i=0;i<DurationList.size();i++) {
            total_duration += DurationList.get(i);
            LogUtil.info(TAG, String.format("duration #%d %d", i, DurationList.get(i)));
        }
        LogUtil.info(TAG, String.format("video total duration %d sec(%d min)",
                total_duration, total_duration / 60));

        StringBuffer sb = new StringBuffer();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-TARGETDURATION:200\n");
        sb.append("#EXT-X-VERSION:3\n");
        for (int i=0;i<UrlList.size();i++) {
            sb.append("#EXTINF:");
            sb.append(DurationList.get(i));
            sb.append(",\n");
            sb.append(UrlList.get(i));
            sb.append("\n");
            sb.append("#EXT-X-DISCONTINUITY\n");
        }
        sb.append("#EXT-X-ENDLIST\n");
        LogUtil.info(TAG, "m3u8 context: " + sb.toString());

        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/youku.m3u8";
            FileOutputStream fout = new FileOutputStream(path);
            byte [] bytes = sb.toString().getBytes();
            fout.write(bytes);
            fout.close();
            LogUtil.info(TAG, "write m3u8 context size " + bytes.length);
            return path;
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static ZGUrl parseM3u8(String vid, String m3u8_context) {
        LogUtil.info(TAG, String.format(Locale.US, "parseM3u8() vid: %s, m3u8_len %d", vid, m3u8_context.length()));

        StringBuffer sbUrl = new StringBuffer();
        StringBuffer sbDuration = new StringBuffer();

        // http://[^?]+\?ts_start=0
        String regSeg = "http://[^?]+\\?ts_start=0";
        Pattern patternSeg = Pattern.compile(regSeg);
        Matcher matcherSeg = patternSeg.matcher(m3u8_context);

        // #EXT.+
        String regExt = "#EXT.+";
        Pattern patternExt = Pattern.compile(regExt);
        Matcher matcherExt = patternExt.matcher(m3u8_context);

        int index = 0;
        while (matcherSeg.find()) {
            String url = matcherSeg.group(0);
            int pos = url.lastIndexOf(".ts?ts_start=0");

            if (index > 0)
                sbUrl.append(",");
            sbUrl.append(url.substring(0, pos));
            index++;
        }

        index = 0;
        float duration = 0f;
        while (matcherExt.find()) {
            String line = matcherExt.group(0);
            if (line.startsWith("#EXTINF")) {
                //#EXTINF:11.72,
                int pos1, pos2;
                pos1 = line.indexOf(":");
                pos2 = line.lastIndexOf(",");
                String tmp = line.substring(pos1 + 1, pos2);
                if (pos1 < 0 || pos2 < 0)
                    continue;

                boolean isFloat = tmp.matches("[\\d]+\\.[\\d]+");
                if (isFloat)
                    duration += Float.parseFloat(tmp);
                else
                    duration += (float)Integer.parseInt(tmp);
            }
            else if (line.contains("#EXT-X-DISCONTINUITY") || line.contains("#EXT-X-ENDLIST")) {
                if (index > 0)
                    sbDuration.append(",");
                sbDuration.append((int)duration);
                duration = 0f;
                index++;
            }
        }

        if (sbUrl.toString().isEmpty()|| sbDuration.toString().isEmpty()) {
            LogUtil.error(TAG, "url list is null: " + m3u8_context);
            return null;
        }

        return new ZGUrl(vid, "flv", sbUrl.toString(), sbDuration.toString());
    }

	public static String getVid(String url) {
		String strRegex = "(?<=id_)(\\w+)";
		Pattern pattern = Pattern.compile(strRegex);
		Matcher match = pattern.matcher(url);
		boolean result = match.find();
		if (result)
			return url.substring(match.start(), match.end());
		
		return null;
	}
	
	private static String getEP(String sk, String ctype) {
        String pkey = "b45197d21f17bb8a"; //21
        if (ctype.equals("20"))
        	pkey = "9e3633aadde6bfec"; //20
        else if (ctype.equals("30"))
        	pkey = "197d21f1722361ac"; //30
        
		try {
			return URLEncoder.encode(
					CryptAES.AES_Encrypt(pkey, sk), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return null;
	}
	
	private static String yk_jsondecode(String json) {
		return CryptAES.AES_Decrypt("qwer3as2jin4fdsa", json);
    }
	
	private static String md5(String string) {

	    byte[] hash;

	    try {
	        hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("Huh, MD5 should be supported?", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new RuntimeException("Huh, UTF-8 should be supported?", e);
	    }

	    StringBuilder hex = new StringBuilder(hash.length * 2);
	    for (byte b : hash) {
	        if ((b & 0xFF) < 0x10)
	        	hex.append("0");

	        hex.append(Integer.toHexString(b & 0xFF));
	    }

	    return hex.toString();
	}

    public static String getPvid(int len) {
        String[] randchar = new String[]{
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
                "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

        int i = 0;
        String r = "";
        long seconds = System.currentTimeMillis();
        /*Calendar cal = java.util.Calendar.getInstance();
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        long seconds = cal.getTimeInMillis();*/
        for (i = 0; i < len; i++) {
            int v = new Random().nextInt(100);
            int index = (int)(v * Math.pow(10, 6)) % randchar.length;
            r += randchar[index];
        }

        return String.valueOf(seconds) + r;
    }

    public static class YKssResult {
        public String m_ykss;
        public long m_exp_time;

        public YKssResult(String ykss, long exp_time) {
            this.m_ykss = ykss;
            this.m_exp_time = exp_time;
        }
    }

    private static String getYKss(String httpUrl, String vid) {
        if (httpUrl == null && vid == null) {
            LogUtil.error(TAG, "getYKss() invalid params");
            return null;
        }

        String html_url = httpUrl;
        if (html_url == null)
            html_url = String.format(youku_page, vid);
        LogUtil.info(TAG, "Java: getYKss() " + html_url);

        try {
            URL url = new URL(html_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Map<String, List<String>> map = conn.getHeaderFields();
            List<String> cookie_list = map.get("Set-Cookie");
            if (cookie_list != null) {
                LogUtil.info(TAG, "Set-Cookie: " + cookie_list.toString());
                // Set-Cookie: [ykss=a6bb21573338bd086f08658d; path=/; domain=.youku.com, u=deleted; expires=Wed, 29-Apr-2015 07:28:37 GMT; path=/; domain=.youku.com]

                String ykss;
                int start, end;

                for (int i = 0; i < cookie_list.size(); i++) {
                    String c = cookie_list.get(i);
                    LogUtil.info(TAG, "cookie c: " + c);

                    if (c.contains("ykss=")) {
                        start = c.indexOf("ykss=");
                        end = c.indexOf(";", start);

                        if (end == -1)
                            ykss = c.substring(start + 5);
                        else
                            ykss = c.substring(start + 5, end);

                        return ykss;
                    }
                    else if (c.contains("expires=")) {
                        String expire_str;
                        start = c.indexOf("expires=");
                        end = c.indexOf(";", start);
                        if (end == -1)
                            expire_str = c.substring(start + 8);
                        else
                            expire_str = c.substring(start + 8, end);
                        /*DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
                        try {
                            expire_time = df.parse(expire_str).getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            LogUtil.error(TAG, "ParseException: " + e.toString());
                            return null;
                        }*/
                    }
                }

                LogUtil.error(TAG, "Set-Cookie: ykss NOT found");
            }
            else {
                LogUtil.error(TAG, "Set-Cookie is empty");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            LogUtil.error(TAG, e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.error(TAG, e.toString());
        }

        return null;
    }
}
