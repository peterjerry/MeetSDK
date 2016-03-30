package com.gotye.common.youku;

import android.os.Environment;

import com.gotye.common.util.CryptAES;
import com.gotye.common.util.LogUtil;
import com.gotye.common.util.httpUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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

    private final static String youku_album_api =
            "http://api.mobile.youku.com/layout/v_5/android/" +
                    "channel/subpage?pid=6f81431b00e5b30a" +
                    "&guid=0b902709fdaba50d69ce66911d4a56e8" +
                    "&ver=5.4.4" +
                    "&network=WIFI" +
                    "&cid=%d" +
                    "&sub_channel_id=%d" +
                    "&sub_channel_type=%d" +
                    "&ob=2" +
                    "&show_game_information=1" +
                    "&pg=%d" + // index from 1
                    "&pz=%d"; // page size

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

    private final static String youku_soku_api =
            "http://www.soku.com/search_video/q_";

    private final static String m3u8_url_fmt =
            "http://pl.youku.com/playlist/m3u8" +
                    "?ts=%s" + // useless?
                    "&keyframe=1" +
                    "&vid=%s" +
                    "&type=hd2" +
                    "&sid=%s" +
                    "&token=%s" +
                    "&oip=%s" +
                    "&did=%s" + // useless?
                    "&ctype=%s" +
                    "&ev=1" +
                    "&ep=%s";

    public static String getPlayUrl(String youku_url, String vid) {
        LogUtil.info(TAG, String.format("Java: getPlayUrl %s, vid %s",
                youku_url == null ? "N/A" : youku_url, vid == null ? "N/A" : vid));

        String vid_ = vid;
        if (vid_ == null && youku_url != null) {
            vid_ = getVid(youku_url);
            if (vid_ == null)
                return null;
        }

        return getPlayUrl(vid_);
    }

	public static String getPlayUrl(String vid) {
        LogUtil.info(TAG, "Java: getPlayUrl() vid " + vid);
		
		String ctype = "20";//指定播放ID 20
        String did = md5(ctype + "," + vid);
        long sec = System.currentTimeMillis() / 1000;
        String tm = String.valueOf(sec);
        
        String api_url = String.format(apiurl, ctype, did, vid, vid);
        String result = getHttpPage(api_url, true, true);
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

            return String.format(m3u8_url_fmt,
                    tm, vid, sid, token, oip, did, ctype, ep);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
		
		return null;
	}

    public static class ZGUrl {
        public String file_type;
        public String urls;
        public String durations;

        public ZGUrl(String type, String urls, String durations) {
            this.file_type = type;
            this.urls = urls;
            this.durations = durations;
        }
    }

    public static ZGUrl getZGUrls(String vid) {
        // ApiKey：66c15f2b1b2bd90244dbcc84290395f8
        // http://zg.yangsifa.com/video?url=[播放地址]&hd=[清晰度]&apikey=[自己的ApiKey]
        // v.youku.com/v_show/id_
        String api_key = "66c15f2b1b2bd90244dbcc84290395f8";
        String api_url = "http://zg.yangsifa.com/video?url=v.youku.com/v_show/id_%s==.html&hd=3";
        String url = String.format(api_url, vid, api_key);
        LogUtil.info(TAG, "getZGUrls() url: " + url);
        String result = getHttpPage(url, false, false);
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
                LogUtil.info(TAG, String.format("getZGUrls() url seg #%d %s", i, item_url));
                sbUrl.append(item_url);
            }

            JSONObject mobile = root.getJSONObject("mobile");
            String mobile_hd = mobile.getString("hd");
            String mobile_file_type = mobile.getString("exe");
            String mobile_vtype = mobile.getString("vtype");
            String mobile_url = mobile.getString("url");

            // get duration list
            String m3u8_url = getPlayUrl(vid);
            LogUtil.info(TAG, "m3u8_url: " + m3u8_url);
            byte []buffer = new byte[65536 * 10];
            int content_size = httpUtil.httpDownloadBuffer(m3u8_url, 0, buffer);
            if (content_size < 0) {
                LogUtil.error(TAG, "failed to download m3u8_context");
                return null;
            }

            byte []m3u8_context = new byte[content_size];
            System.arraycopy(buffer, 0, m3u8_context, 0, content_size);

            ZGUrl tmp = parseM3u8(new String(m3u8_context));
            return new ZGUrl(file_type, sbUrl.toString(), tmp.durations);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static List<Channel> getChannel() {
        System.out.println("getChannel: " + youku_channel_api);

        String result = getHttpPage(youku_channel_api, false, false);
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
        String result = getHttpPage(url, true, true);
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
                String filter = "";
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

    public static List<Album> getAlbums(
            int channel_id ,String filter,
            int sub_channel_id, int sub_channel_type,
            int page, int page_size) {

        String url = String.format(youku_album_api,
                channel_id, sub_channel_id, sub_channel_type, page, page_size);
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
        String result = getHttpPage(url, true, true);
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

            List<Album> albumList = new ArrayList<Album>();

            if (channel_id == 95 /*music*/) {
                JSONArray boxes = root.getJSONArray("boxes");
                int size = boxes.length();
                for (int i=0;i<size;i++) {
                    JSONObject item = boxes.getJSONObject(i);
                    JSONArray cells = item.getJSONArray("cells");
                    int size2 = cells.length();
                    for (int j=0;j<size2;j++) {
                        JSONObject cell = cells.getJSONObject(j);
                        JSONArray contents = cell.getJSONArray("contents");
                        int size3 = contents.length();
                        for (int k=0;k<size3;k++) {
                            JSONObject song = contents.getJSONObject(k);
//                            {
//                                subtitle: "495",
//                                        img: "http://r4.ykimg.com/0541010156E2270E641DA4FA18A017DC",
//                                    title: "Where Went Yesterday 剧情版",
//                                    url: "",
//                                    paid: 0,
//                                    stripe: "05:20",
//                                    tid: "XMTQ5NjQ1NTc1Ng==",
//                                    is_vv: 1,
//                                    type: "1"
//                            },
                            String title = song.getString("title");
                            String tid = song.getString("tid");
                            String stripe = song.getString("stripe");
                            albumList.add(new Album(title, tid, stripe));
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

                    JSONObject item = results.getJSONObject(i);
                    String title = item.getString("title");
                    String stripe = item.getString("stripe");
                    String tid = item.getString("tid");
                    albumList.add(new Album(title, tid, stripe));
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

        LogUtil.debug(TAG, "getAlbumInfo() url: " + url);
        String result = getHttpPage(url, true, true);
        if (result == null)
            return null;

        try {
            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            String status = root.getString("status");
            if (!status.equals("success"))
                return null;

            JSONObject detail = root.getJSONObject("detail");
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
            String sub_title = detail.getString("username");
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

            if (sub_title != null && !sub_title.isEmpty()) {
                title += "(";
                title += sub_title;
                title += ")";
            }
            return new Album(title, showid,
                    stripe, img, total_vv,
                    showdate, desc, actor, videoid, episode_total);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static List<Episode> getEpisodeList(String showId, int page, int page_size) {
        String url = String.format(youku_episode_api, showId, page, page_size);

        LogUtil.info(TAG, "getEpisodeList() url: " + url);
        String result = getHttpPage(url, true, true);
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

    public static List<Episode> soku(String keyword, int orderby, int page) {
        String url = null;
        try {
            String encoded_keyword = URLEncoder.encode(keyword, "UTF-8");
            url = youku_soku_api + encoded_keyword;
            // orderby 1-综合排序 2-最新发布 3-最多播放
            // page base 1
            // hd 0-不限,1-高清,6-超清,7-1080p
            // lengthtype 0- 不限，1->0-10min, 2->10-30min, 3->30-60min, 4->60min+
            url += "?page=";
            url += page;
            url += "&orderby=";
            url += orderby;
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
        String html = getHttpPage(url, false, false);
        if (html == null) {
            LogUtil.error(TAG, "failed to get http page: " + url);
            return null;
        }

        //Document doc = Jsoup.connect(url).timeout(5000).get();
        Document doc = Jsoup.parse(html);
        Elements test = doc.getElementsByClass("v");
        if (test == null || test.isEmpty()) {
            LogUtil.error(TAG, "failed to get v tag");
            return null;
        }

        int size = test.size();
        List<Episode> epList = new ArrayList<Episode>();
        for (int i = 0; i < size; i++) {
            Element v = test.get(i);
            Element v_thumb = v.child(0);
            Element v_link = v.child(1);
            Element v_meta = v.child(3);
            Element video = v_link.child(0);
            String thumb_url = v_thumb.child(0).attr("src");
            String title = video.attr("title");
            String href = video.attr("href");
            String vid = video.attr("_log_vid");
            String vv = v_meta.child(1).child(1).child(1).text();
            String online_time = v_meta.child(1).child(2).child(1).text();
            epList.add(new Episode(title, vid, thumb_url,
                    online_time, vv, null));
        }

        return epList;
    }

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
        String result = getHttpPage(url, true, false);
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
                String pubdate = item.getString("pubdate");
                albumList.add(new Album(title, showid, pubdate));
            }

            return albumList;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        return null;
    }

	private static String getHttpPage(String url, boolean setUserAgent, boolean setEncoding) {
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
            URL realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间
			if (setUserAgent) {
				conn.setRequestProperty("User-Agent",
						"Youku HD;3.9.4;iPhone OS;7.1.2;iPad4,1");
			}
            else {
                conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.3; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/46.0.2490.86 Safari/537.36");
            }
			if (setEncoding)
				conn.setRequestProperty("Encoding", "gzip, deflate");

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

    public static ZGUrl parseM3u8(String m3u8_context) {
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

        if (!sbUrl.toString().isEmpty() && !sbDuration.toString().isEmpty())
            return new ZGUrl("flv", sbUrl.toString(), sbDuration.toString());

        return null;
    }

	private static String getVid(String url) {
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
}
