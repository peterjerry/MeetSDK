package com.pplive.meetplayer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
	
	private final static String mtbu_url = "http://mtbu.api.pptv.com/v4/module?lang=zh_cn&platform=aphone&" + 
			"appid=com.pplive.androidphone&appver=4.1.3&appplt=aph&userLevel=0&";// + 
			//"channel=@SHIP.TO.31415926PI@&location=app%3A%2F%2Faph.pptv.com%2Fv4%2Fhome";
	
	private final static String detail_url_fmt = "http://epg.api.pptv.com/detail.api?auth=d410fafad87e7bbf6c6dd62434345818&canal=" + 
			"@SHIP.TO.%sPI@&userLevel=0&appid=com.pplive.androidphone&appver=4.1.3" + 
			"&appplt=aph&vid=%s&series=1&virtual=1&ver=2&platform=android3"; // 8022983
	
	private final int MAX_TITLE_LEN = 16;
	
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
	
	public boolean getCategory() {
		Log.i(TAG, "java: getCategory()");
		
		boolean ret = false;
		
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(mtbu_url);
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
	
	public boolean initEPG(int type) {
		boolean ret = false;
		
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(mtbu_url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				//Log.i(TAG, "Java: mtbu result: " + result.replaceAll("\r", ""));
				JSONTokener jsonParser = new JSONTokener(result);
				JSONObject item = (JSONObject) jsonParser.nextValue();
				JSONArray modules = item.getJSONArray("modules");
				JSONObject programs = modules.getJSONObject(type);
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
	
	private boolean getDetail(String url) {
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(url);
		try {
			httpResponse = new DefaultHttpClient().execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(httpResponse.getEntity());
				//Log.d(TAG, "Java: mtbu detail result: " + result);
				parsexml(result);
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
	
	
	void parsexml(String str) {
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
						Log.i(TAG, "Java: mtbu added " + new_episode.toString());
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
}
