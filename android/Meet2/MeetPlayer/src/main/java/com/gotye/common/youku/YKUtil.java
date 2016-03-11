package com.gotye.common.youku;

import com.gotye.common.util.CryptAES;
import com.gotye.common.util.LogUtil;

import java.io.ByteArrayOutputStream;
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

            if (sub_channel_type == 1) {
                return null;
                //todo
                //JSONArray boxes = root.getJSONArray("boxes");
            }

            JSONArray results = root.getJSONArray("results");
            int size = results.length();
            List<Album> albumList = new ArrayList<Album>();
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
            String showid = detail.getString("showid");
            String videoid = detail.getString("videoid");
            String title = detail.getString("title");
            String sub_title = detail.getString("username");
            String img = detail.getString("img");
            String desc = detail.getString("desc");
            String total_vv = detail.getString("total_vv_fmt");
            String stripe = detail.getString("stripe_bottom");
            String showdate = detail.getString("showdate");
            int episode_total = 0;
            if (detail.has("episode_total"))
                episode_total = detail.getInt("episode_total");

            if (sub_title != null && !sub_title.isEmpty()) {
                title += "(";
                title += sub_title;
                title += ")";
            }
            return new Album(title, videoid, showid,
                    stripe, img, total_vv,
                    showdate, desc, actor, episode_total);
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

	private static String getHttpPage(String url, boolean setUserAgent, boolean setEncoding) {
		URL realUrl = null;
		InputStream is = null;
		ByteArrayOutputStream os = null;

		try {
			realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5000);// 设置超时的时间
			conn.setConnectTimeout(5000);// 设置链接超时的时间
			if (setUserAgent) {
				conn.setRequestProperty("User-Agent",
						"Youku HD;3.9.4;iPhone OS;7.1.2;iPad4,1");
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
