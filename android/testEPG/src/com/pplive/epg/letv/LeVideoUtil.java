package com.pplive.epg.letv;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LeVideoUtil {
	private final static String search_api_url = "http://search.lekan.letv.com" +
			"/lekan/apisuggest_json.so?wd=";
	
	private final static String detail_api_fmt = "http://api.mob.app.letv.com" +
			"/play/tabs?cid=0&pid=0&vid=%d&devid=6f176b8a7fcd6eb2aa58bed383ee8fbb" +
			"&pcode=010110065&version=5.9.3";
	
	private final static String cdn_url_api_fmt = "http://api.mob.app.letv.com" +
			"/play?tm=%d" +
			"&playid=0" +
			"&tss=ios" +
			"&pcode=010110065" +
			"&version=5.9.3" +
			"&vid=%d";
	
	
	public int search(String key) {
		String url = null;
		try {
			String encoded_path = URLEncoder.encode(key, "utf-8");

			url = search_api_url + encoded_path;
			System.out.println("ready to search(): " + url);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("status is not 200: " + response.getStatusLine().getStatusCode());
				return -1;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONArray clips = (JSONArray) jsonParser.nextValue();
			if (clips == null || clips.length() == 0)
				return -1;
			
			int count = clips.length();
			int ret_vid = -1;
			for (int i=0;i<count;i++) {
				JSONObject c = clips.getJSONObject(i);
				int aid = c.getInt("aid");
				String name = c.getString("name");
				String vid = c.getString("vid");
				System.out.println(String.format("clip #%d: aid: %d, name: %s, vid:  %s",
						i, aid, name, vid));
				
				if (ret_vid == -1)
					ret_vid = Integer.valueOf(vid);
			}
			
			return ret_vid;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public VideoInfo detail(int vid) {
		String url = String.format(detail_api_fmt, vid);
		System.out.println("ready to detail(): " + url);
	
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONObject header = root.getJSONObject("header");
			if (!header.getString("status").equals("1"))
				return null;
			
			JSONObject body = root.getJSONObject("body");
			JSONObject videoInfo = body.getJSONObject("videoInfo");
//			"videoInfo":{
//				"vid":"1059958",
//				"pid":"34730",
//				"cid":"1",
//				"nameCn":"越光宝盒",
//				"subTitle":"最新癫狂爆笑巨作",
//				"singer":"",
//				"releaseDate":"2010-03-18",
//				"style":"",
//				"playMark":"0",
//				"guest":"",
//				"type":"3",
//				"btime":"0",
//				"etime":"0",
//				"duration":"5366",
//				"mid":"628252",
//				"episode":"",
//				"porder":"1",
//				"pay":"0",
//				"download":"1",
//				"picAll":{
//				"120*90":"http://i2.letvimg.com/lc03_yunzhuanma/201506/10/17/42/3e2e61370b501635d69339d4fa2ceca2_628252/thumb/2_120_90.jpg",
//				"200*150":"http://i2.letvimg.com/lc03_yunzhuanma/201506/10/17/42/3e2e61370b501635d69339d4fa2ceca2_628252/thumb/2_200_150.jpg",
//				"320*200":"http://i2.letvimg.com/lc03_yunzhuanma/201506/10/17/42/3e2e61370b501635d69339d4fa2ceca2_628252/thumb/2_320_200.jpg"
//				},
//				"play":"1",
//				"openby":"",
//				"jump":"0",
//				"jumptype":"",
//				"jumplink":"",
//				"isDanmaku":"1",
//				"brList":[
//				"mp4_180",
//				"mp4_1000",
//				"mp4_350",
//				"mp4_1300"
//				],
//				"videoTypeKey":"180001",
//				"videoType":"0001",
//				"videoTypeName":"正片",
//				"controlAreas":"",
//				"disableType":"1",
//				"watchingFocus":{
//				},
//				"cornerMark":"01:29:26"
//				},
			String info_vid = videoInfo.getString("vid");
			String info_pid = videoInfo.getString("pid");
			int info_cid = videoInfo.getInt("cid");
			String info_title = videoInfo.getString("nameCn");
			String info_subtitle = videoInfo.getString("subTitle");
			String info_duration = videoInfo.getString("duration");
			System.out.println(String.format("vid %s, pid %s, cid %d, duration %s",
					info_vid, info_pid, info_cid, info_duration));
			JSONArray brList = videoInfo.getJSONArray("brList");
			int br_count = brList.length();
			List<String>info_brlist = new ArrayList<String>();
			for (int i=0;i<br_count;i++) {
				info_brlist.add(brList.getString(i));
			}
			
			return new VideoInfo(
					Integer.valueOf(info_vid), Integer.valueOf(info_cid), Integer.valueOf(info_pid),
					info_title, info_subtitle, 
					Integer.valueOf(info_duration), info_brlist);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String cdn_url(int vid, String streamDesc) {
		String url = String.format(cdn_url_api_fmt, System.currentTimeMillis() / 1000, vid);
		System.out.println("ready to cdn_url(): " + url);
	
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONObject header = root.getJSONObject("header");
			if (!header.getString("status").equals("1"))
				return null;
			
			JSONObject body = root.getJSONObject("body");
			JSONObject videofile = body.getJSONObject("videofile");
			JSONObject infos = videofile.getJSONObject("infos");
			JSONObject stream = infos.getJSONObject(streamDesc);
			String mainUrl = stream.getString("mainUrl");
			
			System.out.println("mp4 url: " + mainUrl);
			return mainUrl;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String play_url(String url) {
		System.out.println("ready to play_url(): " + url);
	
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			if (status != 200)
				return null;
			
			JSONArray nodelist = root.getJSONArray("nodelist");
			if (nodelist == null || nodelist.length() == 0)
				return null;
			
			JSONObject node = nodelist.getJSONObject(0);
			String name = node.getString("name");
			String play_url = node.getString("location");
			System.out.println("name: " + name + " , play_url: " + play_url);
			return play_url;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
