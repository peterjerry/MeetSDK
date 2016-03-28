package com.pplive.epg.youku;

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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.util.Base64Util;
import com.pplive.epg.util.CryptAES;
import com.pplive.epg.util.httpUtil;

public class YKUtil {
	
	private final static String YOUKU_SECURITY_FMT = 
			"http://play.youku.com/play/get.json?vid=%s&ct=12";
	
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
	
	private final static String m3u8_url_fmt2 = 
			"http://pl.youku.com/playlist/m3u8" +
			"?ctype=12" +
			"&ep=%s" +
			"&ev=1" +
			"&keyframe=1" +
			"&oip=%s" +
			"&sid=%s" +
			"&token=%s" +
			"&type=%s" +
			"&vid=%s";
	
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
	
	private final static String youku_get_episode_api = 
			"http://api.mobile.youku.com/shows/%s/" +
			"reverse/videos" +
			"?pid=6f81431b00e5b30a" +
			"&guid=0b902709fdaba50d69ce66911d4a56e8" +
			"&ver=5.4.4" +
			"&network=WIFI" +
			"&fields=vid|titl|lim|is_new|pv" +
			"&pg=%d" +
			"&pz=%d" +
			"&area_code=1";
	
	private final static String youku_search_api = 
			"http://api.appsdk.soku.com/u/s?" +
			"pid=6f81431b00e5b30a" +
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
			"&keyword=" ;//%E7%81%AB%E6%98%9F%E6%95%91%E6%8F%B4
	
	public static void main(String[] args) { 
        
		//String url = "http://v.youku.com/v_show/id_XMTQ5NDg3NjQzNg==.html?from=s1.8-1-1.1";
		
		List<Channel> channelList = getChannel();
		if (channelList == null)
			return;
		
		for (int i=0;i<channelList.size();i++) {
			System.out.println(
					String.format("#%d %s", i, channelList.get(i).toString()));
		}
		
		Channel c = channelList.get(1); // 1电视剧 2电影15游戏
		List<Catalog> catlogList = getCatalog(c.getChannelId());
		if (catlogList == null)
			return;
		
		for (int i=0;i<catlogList.size();i++) {
			System.out.println(
					String.format("#%d %s", i, catlogList.get(i).toString()));
		}
		
		Catalog cat = catlogList.get(2); // 1全部 动作
		List<Album> alList = getAlbums(
				c.getChannelId(), cat.getFilter(), 
				cat.getSubChannelId(), cat.getSubChannelType(), 1);
		//List<Album> alList = search("女医明妃传", 1, 5);
		if (alList == null)
			return;
		
		for (int i=0;i<alList.size();i++)
			System.out.println(alList.get(i).toString());
		
		Album al = alList.get(3);
		al = getAlbumInfo(al.getShowId());
		if (al == null)
			return;
		
		System.out.println("album info: " + al.toString());
		
		List<Episode> list = getEpisodeList(al.getShowId(), 1, 100);
		if (list != null) {
			for (int i=0;i<list.size();i++)
				System.out.println(list.get(i).toString());
		}
		
		//getZGUrls(list.get(3).getVideoId());
		
		String m3u8Url = getPlayUrl_vid(list.get(3).getVideoId());
		if (m3u8Url == null) {
			System.out.println("failed to get m3u8");
			return;
		}
		
		System.out.println("Java: toUrl " + m3u8Url);
		byte []buffer = new byte[65536];
		if (!httpUtil.httpDownload(m3u8Url, "1.m3u8")) {
			System.out.println("failed to download m3u8");
			return;
		}
		
		int content_size = httpUtil.httpDownloadBuffer(m3u8Url, 0, buffer);
		if (content_size < 0) {
			System.out.println("failed to download m3u8_context");
			return;
		}
		
		byte []m3u8_context = new byte[content_size];
		System.arraycopy(buffer, 0, m3u8_context, 0, content_size);
		parseM3u8(new String(m3u8_context));
		
        //String keyStr = "UITN25LMUQC436IM";  
 
        //String plainText = "this is a string will be AES_Encrypt";
	         
        //String encText = CryptAES.AES_Encrypt(keyStr, plainText);
        //String decString = CryptAES.AES_Decrypt(keyStr, encText); 
         
        //System.out.println(encText); 
        //System.out.println(decString);
	}
	
	public static List<Album> search(String keyword, 
			int page, int page_size) {
		System.out.println("search() keyword " + keyword);
		
		String url = null;
		try {
			String encoded_keyword = URLEncoder.encode(keyword, "UTF-8");
			url = String.format(youku_search_api, page, page_size) + 
					encoded_keyword;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		
		System.out.println("search() url: " + url);
		String result = getHttpPage(url, false, false);
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
		
		System.out.println("getCatalog() url: " + url);
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
			int channel_id, String filter, 
			int sub_channel_id, int sub_channel_type,
			int page) {
		
		final int page_size = 30;
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
		
		System.out.println("getAlbums " + url);
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
		System.out.println("getAlbumInfo() tid: " + tid);
		String url = String.format(youku_album_info_api, tid);
		
		System.out.println("getAlbumInfo() url: " + url);
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
            String subtitle = detail.getString("username");
            String img = detail.getString("img");
            String desc = detail.getString("desc");
            String total_vv = detail.getString("total_vv_fmt");
            String stripe = detail.getString("stripe_bottom");
            String showdate = detail.getString("showdate");
            
            if (subtitle != null) {
            	title += "(";
            	title += subtitle;
            	title += ")";
            }
            return new Album(title, videoid, stripe,
            		showid, img, total_vv, 
            		showdate, desc, actor);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
		
        return null;
	}
	
	public static List<Episode> getEpisodeList(
			String showId, int page, int page_size) {		
		String url = String.format(youku_get_episode_api, 
				showId, page, page_size);
		System.out.println("getEpisodeList() url: " + url);
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
	
	public static String getPlayUrl_vid(String vid) {
		System.out.println("Java: vid " + vid);
		
		String ctype = "20";//指定播放ID 20
        String did = md5(ctype + "," + vid);
        long sec = System.currentTimeMillis() / 1000;
        String tm = String.valueOf(sec);
        
        String api_url = String.format(apiurl, ctype, did, vid, vid);
		System.out.println("Java: api_url " + api_url);
		
		String result = getHttpPage(api_url, true, true);
		if (result == null)
			return null;
			
		try {
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			String data = root.getString("data");
			String dec_data = yk_jsondecode(data);
			//System.out.println("Java: dec_data " + dec_data);
			
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
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
                System.out.println("http response is not 200 " + conn.getResponseCode());
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
	
	public static String getPlayUrl(String youku_url) {
		System.out.println("Java: youku_url " + youku_url);
		
		String vid = getVid(youku_url);
		if (vid == null)
			return null;
		
		return getPlayUrl_vid(vid);
	}
	
	private static ZGUrl parseM3u8(String m3u8_context) {
		List<String> urlList = new ArrayList<String>();
		List<Integer> durationList = new ArrayList<Integer>();
		
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
            urlList.add(url.substring(0, pos));
        }
        
        float duration_f = 0;
        int duration = 0;
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
        			duration_f += Float.parseFloat(tmp);
        		else
        			duration_f += (float)Integer.parseInt(tmp);
        	}
        	else if (line.contains("#EXT-X-DISCONTINUITY")) {
        		duration = (int)duration_f;
        		durationList.add(duration);
        		duration_f = 0;
        	}
        }
        
        if (urlList.isEmpty())
        	return null;
        
        return new ZGUrl("flv", urlList, durationList);
    }
	
	public static class ZGUrl {
		public String file_type;
		public List<String> file_list;
		public List<Integer> duration_list;
		
		public ZGUrl(String type, List<String>file_list, List<Integer>duration_list) {
			this.file_type = type;
			this.file_list = file_list;
			this.duration_list = duration_list;
		}
	}
	
	public static ZGUrl getZGUrls(String vid) {
        // ApiKey：66c15f2b1b2bd90244dbcc84290395f8
        // http://zg.yangsifa.com/video?url=[播放地址]&hd=[清晰度]&apikey=[自己的ApiKey]
		// v.youku.com/v_show/id_
		String api_key = "66c15f2b1b2bd90244dbcc84290395f8";
		String api_url = "http://zg.yangsifa.com/video?url=v.youku.com/v_show/id_%s.html&hd=3";
        String url = String.format(api_url, vid, api_key);
        System.out.println("getZGUrls() url: " + url);
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
            List<String> urlList = new ArrayList<String>();
            List<Integer> durationList = new ArrayList<Integer>();
            for (int i=0;i<size;i++) {
            	JSONObject item = urls.getJSONObject(i);
            	String item_url = item.getString("url");
            	System.out.println("item_url " + item_url);
            	urlList.add(item_url);
            	durationList.add(300);
            }
            
            JSONObject mobile = root.getJSONObject("mobile");
            String mobile_hd = mobile.getString("hd");
            String mobile_file_type = mobile.getString("exe");
            String mobile_vtype = mobile.getString("vtype");
            String mobile_url = mobile.getString("url");
            System.out.println("mobile_url " + mobile_url);
            
            return new ZGUrl(file_type, urlList, durationList);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        return null;
    }
	
	public static String getPlayUrl2(String youku_url) {
		String vid = getVid(youku_url);
		if (vid == null)
			return null;
		
		SecurityParam security = getSecurity(vid);
		if (security == null)
			return null;
		
		System.out.println("security.mEncryptString " + security.mEncryptString);
		EpParam ep = getEP2(vid, security.mEncryptString);
		
		String final_url = String.format(m3u8_url_fmt2, 
				ep.mEp, security.mIp, ep.mSid, ep.mToken, "hd2", vid);
		return final_url;
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
	
	private static SecurityParam getSecurity(String vid) {
		String url = String.format(YOUKU_SECURITY_FMT, vid);
		System.out.println("Java: getSecurity " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONObject security = 
					root.getJSONObject("data").getJSONObject("security");
			
			String encrypt_string = security.getString("encrypt_string");
			long ip = security.getLong("ip");
			System.out.println("Java: encrypt_string " + encrypt_string + 
					", ip " + String.valueOf(ip));
			return new SecurityParam(encrypt_string, String.valueOf(ip));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static String myEncoder(String a, byte[] c, boolean isToBase64)
	{
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
	
	private static EpParam getEP2(String vid, String ep) {
		String template1 = "becaf9be";
		String template2 = "bf7e5f01";
		
		byte []bytes = Base64Util.decode(ep);
		String temp = myEncoder(template1, bytes, false);
		System.out.println("temp " + temp);
		String[] part = temp.split("_");
		String sid = part[0];
		String token = part[1];
		String whole = String.format("%s_%s_%s", sid, vid, token);
		System.out.println("whole " + whole);
		String tmp = myEncoder(template2, whole.getBytes(), true);
		try {
			String ep_new = URLEncoder.encode(tmp, "UTF-8");
			System.out.println("ep_new " + ep_new);
			return new EpParam(ep_new, token, sid);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static class EpParam {
		public String mEp;
		public String mToken;
		public String mSid;
		
		public EpParam(String ep, String token, String sid) {
			mEp = ep;
			mToken = token;
			mSid = sid;
		}
	}
	
	private static class SecurityParam {
		/*security: {
			encrypt_string: "NgXVSAodJrvZ0PnF9+JxA4T3vBFu1wzKXhk=", // ep
			ip: 3031238944 // oip
		},*/
		
		public String mEncryptString;
		public String mIp;
		
		public SecurityParam(String encryptString, String ip) {
			mEncryptString = encryptString;
			mIp = ip;
		}
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
