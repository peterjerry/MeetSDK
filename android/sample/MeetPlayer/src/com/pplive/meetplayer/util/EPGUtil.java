package com.pplive.meetplayer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class EPGUtil {
	private final static String TAG = "EPGUtil";
	
	public final static int LIST_FRONTPAGE_CATALOG = -101;
	public final static int LIST_MAIN_CATALOG = -102;
	
	//mtbu -> epg.api -> play.api
	
	private final static String catalog_url = "http://mtbu.api.pptv.com/v4/module?lang=zh_cn"
			+ "&platform=aphone&appid=com.pplive.androidphone&appver=4.1.3"
			+ "&appplt=aph&userLevel=0&channel=@SHIP.TO.31415926PI@"
			+ "&location=app%3A%2F%2Faph.pptv.com%2Fv4%2Fcate";
	// cate/live
	// cate/
	// module data dlist tags param=ntags=动作:catalog|&order=n
	
	private final static String frontpage_url = "http://mtbu.api.pptv.com/v4/module?lang=zh_cn&platform=aphone"
			+ "&appid=com.pplive.androidphone&appver=4.1.3"
			+ "&appplt=aph&userLevel=0";
	
	private final static String detail_url_fmt = "http://epg.api.pptv.com/detail.api?auth=d410fafad87e7bbf6c6dd62434345818&canal=" + 
			"@SHIP.TO.%sPI@&userLevel=0&appid=com.pplive.androidphone&appver=4.1.3" + 
			"&appplt=aph&vid=%s&series=1&virtual=1&ver=2&platform=android3"; // 8022983
	
	private final static String boxplay_prefix = "http://play.api.pptv.com/boxplay.api?" + 
			"platform=android3&type=phone.android.vip&sv=4.0.1&param=";
	
	private final static String boxplay_fmt = "&sdk=1&channel=162&content=need_drag" + 
			"&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=41" +
			"&id=%s&ft=1&k_ver=1.1.0";
	
	private final int MAX_TITLE_LEN = 16;
	private final int HOST_PORT = 80;
	
	private List<Map<String, Object>> mCategoryList = null;
	private List<Map<String, Object>> mClipList = null;
	
	public List<Map<String, Object>> getCategoryList() {
		return mCategoryList;
	}
	
	public List<Map<String, Object>> getClipList() {
		return mClipList;
	}
	
	public	final static int EPG_TYPE_NEWS		 	= 3; // PP出品
	public	final static int EPG_TYPE_HOT_TV		= 4; // 同步剧场
	public	final static int EPG_TYPE_HOT_MOVIE	= 5; // 热播电影
	public	final static int EPG_TYPE_VIP_MOVIE	= 6; // 会员电影
	public	final static int EPG_TYPE_CARTOON		= 7; // 卡通动漫
	
	public boolean getCategory(int id) {
		Log.i(TAG, "java: getCategory()");
		
		boolean ret = false;
		
		HttpResponse httpResponse = null;
		String url;
		if (LIST_FRONTPAGE_CATALOG == id)
			url = frontpage_url;
		else if (LIST_MAIN_CATALOG == id)
			url = catalog_url;
		else
			url = "xxx";
		
		HttpGet httpGet = new HttpGet(url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				JSONTokener jsonParser = new JSONTokener(result);
				JSONObject item = (JSONObject) jsonParser.nextValue();
				JSONArray modules = item.getJSONArray("modules");
				
				mCategoryList = new ArrayList<Map<String, Object>>();
				
				for (int i=0;i<modules.length();i++) {
					JSONObject programs = modules.getJSONObject(i);
					JSONObject data = programs.getJSONObject("data");
					if (!data.isNull("title")) {
						HashMap<String, Object> new_category = new HashMap<String, Object>();
						new_category.put("index", i);
						new_category.put("title", data.getString("title"));
						mCategoryList.add(new_category);
						//Log.d(TAG, "java: getCategory added " + new_category.toString());
					}
				}
				ret = true;
				
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {  
			e.printStackTrace();
		}  
		
		return ret;
	}
	
	public boolean getEPGClips(int catalog_index) {
		boolean ret = false;
		
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(frontpage_url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				//Log.i(TAG, "Java: mtbu result: " + result.replaceAll("\r", ""));
				JSONTokener jsonParser = new JSONTokener(result);
				JSONObject item = (JSONObject) jsonParser.nextValue();
				JSONArray modules = item.getJSONArray("modules");
				JSONObject programs = modules.getJSONObject(catalog_index);
				JSONObject data = programs.getJSONObject("data");
				JSONArray data_array = data.getJSONArray("dlist");
				
				mClipList = new ArrayList<Map<String, Object>>();
				
				for (int i=0;i<data_array.length();i++){  
	                JSONObject jsonobject = data_array.getJSONObject(i);  
	                Log.i(TAG, String.format("Java: mtbu data_array #%d id: %s", i, jsonobject.getString("title")));
	                String link = jsonobject.getString("link");
	                Log.i(TAG, String.format("Java: mtbu data_array #%d link: %s", i, link));
	                int pos1, pos2;
	                pos1 = link.indexOf("&vid=");
	                pos2 = link.indexOf("&sid=");
	                if (pos1 == -1 || pos2 == -1)
	                	continue;
	                
	                String vid = link.substring(pos1 + 5, pos2);
	                Log.i(TAG, String.format("Java: mtbu data_array #%d vid: %s", i, vid));
	                String detail_url = String.format(detail_url_fmt, vid, vid);
	                Log.i(TAG, "Java: mtbu get detail: " + detail_url);
	                getDetail(detail_url);
				}
				ret = true;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException ex) {  
		    // 异常处理代码  
		}  
		
		return ret;
	}
	
	public String getCDNUrl(String link, String ft) {
		Log.i(TAG, String.format("java: getCDNUrl() %s", link));
		
		String user_type = null;
		try {
			user_type = URLEncoder.encode("userType=1", "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String boxplay_part2 = String.format(boxplay_fmt, link);
		StringBuffer sbBoxPlayUrl = new StringBuffer();
		sbBoxPlayUrl.append(boxplay_prefix);
		sbBoxPlayUrl.append(user_type);
		sbBoxPlayUrl.append(boxplay_part2);
		Log.i(TAG, "Java key url " + sbBoxPlayUrl.toString());
		HttpGet httpGet = new HttpGet(sbBoxPlayUrl.toString());
		
		HttpResponse httpResponse = null;

		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				return parseCdnUrlxml(result, ft, false);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private boolean getDetail(String url) {
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				//Log.d(TAG, "Java: mtbu detail result: " + result);
				parseClipxml(result);
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	
	void parseClipxml(String str) {
        try {
        	InputStream in_withcode;
        	in_withcode = new ByteArrayInputStream(str.getBytes("UTF-8"));
    		XmlPullParser parser = Xml.newPullParser();
			parser.setInput(in_withcode, "UTF-8");
			String main_title = null;

			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					//Log.d(TAG, "Java: mtbu START_DOCUMENT");
					break;
				case XmlPullParser.START_TAG:
					//Log.d(TAG, "Java: mtbu START_TAG " + parser.getName());
					//if (parser.getName().equals("video_list2")) {
					if (parser.getName().equals("title")) {
						main_title = parser.nextText();
						if (main_title.length() > MAX_TITLE_LEN)
							main_title = main_title.substring(0, MAX_TITLE_LEN);
					}
					else if (parser.getName().equals("playlink2")) {
						HashMap<String, Object> new_episode = new HashMap<String, Object>();
						String link = parser.getAttributeValue(null, "id");
						String sub_title = parser.getAttributeValue(null, "title");
						String duration = parser.getAttributeValue(null, "duration");
						new_episode.put("title", main_title + "(" + sub_title + ")"); // episode2
						new_episode.put("link", link); // playlink
						new_episode.put("duration", duration); // minute
						mClipList.add(new_episode);
						Log.i(TAG, "Java: epg clip added " + new_episode.toString());
					}
					break;
				case XmlPullParser.END_TAG:
					//Log.d(TAG, "Java: mtbu END_TAG " + parser.getName());
					break;
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String parseCdnUrlxml(String str, String ft, boolean m3u8) {
		Log.i(TAG, "Java: epg parseCdnUrlxml " + str.replace("\n", ""));
		String url = null;
		
        try {
        	InputStream in_withcode;
        	in_withcode = new ByteArrayInputStream(str.getBytes("UTF-8"));
    		XmlPullParser parser = Xml.newPullParser();
			parser.setInput(in_withcode, "UTF-8");

			int eventType = parser.getEventType();
			boolean bFound = false;
			
			String host = null;
			String st = null;
			String key = null;
			String rid = null;
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					if (parser.getName().equals("item")) {
						String tmp = parser.getAttributeValue(null, "ft");
						if (tmp != null && tmp.equals(ft)) {
							rid = parser.getAttributeValue(null, "rid");
						}
					}
					if (parser.getName().equals("dt")) {
						String tmp = parser.getAttributeValue(null, "ft");
						if (tmp != null && tmp.equals(ft)) {
							Log.i(TAG, "Java: epg ft=1");
							bFound = true;
						}
					}
					if (bFound) {
						if (parser.getName().equals("sh") && null == host) { // sh main, bh backup
							host = parser.nextText();
							Log.i(TAG, "Java: epg host: " + host);
						}
						
						if (parser.getName().equals("st") && null == st) {
							st = parser.nextText();
							Log.i(TAG, "Java: epg st: " + st);
						}
						
						if (parser.getName().equals("key") && null == key) {
							key = parser.nextText();
							Log.i(TAG, "Java: epg key: " + key);
						}
						
						if (host != null && st != null && key != null)
							break;
					}
					
					break;
				case XmlPullParser.END_TAG:
					break;
				}
				eventType = parser.next();
			}
			
			if (host == null || st == null || key == null)
				return null;
			
			if (!host.contains("http://"))
	            url = "http://" + host;
	        else
	            url = host;
			if (!host.contains(":"))
				url += ":" + HOST_PORT;
			
			url += "/";
			if (m3u8)
				url += rid.replaceFirst(".mp4", ".m3u8");
			else
				url += rid;
	        
			url += "?w=" + 1 + "&key=" + Key.getKey(new Date(st).getTime());
			url += "&k=" + key;
			url += "&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1"
					+ "&sv=4.1.3&platform=android3";
			url += "&ft=" + ft;
			url += "&accessType=wifi";
			Log.i(TAG, "Java: epg final cdn url: " + url);
			
			return url;
			
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return null;
	}
}
