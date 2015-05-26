package com.pplive.epg.sohu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.vst.ProgramVst;

public class SohuUtil {
	private static final String API_URL = "http://api.tv.sohu.com/video/playinfo/" +
			"%d.json?api_key=9854b2afa779e1a6bff1962447a09dbd&plat=6&sver=3.1&partner=47&c=2&sid=%d";
	
	private static final String TOPIC_LIST_URL_FMT = "http://api.tv.sohu.com/v4/personal/tv/individuation.json?" +
			"plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&uid=9354d3e14fdc4aa4999aff3790dab635" +
			"&sver=4.7.1&cat=9008&page=%d&page_size=%d&sysver=4.2.2&partner=340";
	
	private static final String EPISODE_DISC_URL_FMT = "http://api.tv.sohu.com/v4/album/info/%d.json?" +
			"area_code=42&plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.7.1&sysver=4.2.2&partner=340";
	
	private static final String EPISODE_DETAIL_URL_FMT = "http://api.tv.sohu.com/v4/album/videos/" +
			"%d.json?order=0&site=1&with_trailer=1&plat=6" +
			"&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&page=%d&page_size=%d&sysver=4.2.2&partner=340";
	
	private static final String SEARCH_URL_FMT = "http://api.tv.sohu.com/v4/search/all.json?" +
			"pgc=1&area_code=42&plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&uid=9354d3e14fdc4aa4999aff3790dab635" +
			"&pay=1&sver=4.7.1" +
			"&key=%s&page=%d&page_size=%d&ds=&sysver=4.2.2&type=1&partner=340&all=1";
	
	private List<TopicSohu> mTopicList;
	private List<AlbumSohu> mAlbumList;
	private List<EpisodeSohu> mEpisodeList;
	private List<AlbumSohu> mSearchItemList;
	
	public SohuUtil() {
		mTopicList = new ArrayList<TopicSohu>();
		mAlbumList = new ArrayList<AlbumSohu>();
		mEpisodeList = new ArrayList<EpisodeSohu>();
		mSearchItemList = new ArrayList<AlbumSohu>();
	}
	
	public List<TopicSohu> getTopicList() {
		return mTopicList;
	}
	
	public List<AlbumSohu> getAlbumList() {
		return mAlbumList;
	}
	
	public List<EpisodeSohu> getEpisodeList() {
		return mEpisodeList;
	}
	
	public List<AlbumSohu> getSearchItemList() {
		return mSearchItemList;
	}
	
	public boolean getTvList() {
		return false;
	}
	
	public boolean search(String key, int page_index, int page_size) {
		String url = String.format(SEARCH_URL_FMT, key, page_index, page_size);
		System.out.println("Java: SohuUtil search() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			String output = result;
			if (output.length() > 256)
				output = output.substring(0, 256);
			System.out.println("Java: result: " + output);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray items = data.getJSONArray("items");
			
			mSearchItemList.clear();
			for (int i=0;i<items.length();i++) {
				JSONObject item = items.getJSONObject(i);
				
				
				if (item.has("album_name")) {
					String picUrl = item.getString("ver_high_pic");
					int aid = item.getInt("aid");
					int	cid = item.getInt("cid");
					String album_name = item.getString("album_name");
					
					if (item.has("videos")) {
						JSONArray videos = item.getJSONArray("videos");
						for(int j=0;j<videos.length();j++) {
							JSONObject v = videos.getJSONObject(j);
							int vid = v.getInt("vid");
							
						}
					}
					
					AlbumSohu a = new AlbumSohu(album_name, picUrl, aid, cid);
					mSearchItemList.add(a);
				}
			}
			
			return true;
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
		
		return false;
	}
	
	public boolean episode(int id, int page_index, int page_size) {
		String url = String.format(EPISODE_DETAIL_URL_FMT, id, page_index, page_size);
		System.out.println("Java: SohuUtil episode() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			String output = result;
			if (output.length() > 64)
				output = output.substring(0, 64);
			System.out.println("Java: result: " + output);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			int count = data.getInt("count");
			int page = data.getInt("page");
			if (!data.has("videos"))
				return false;
			
			JSONArray videoList = data.getJSONArray("videos");
			
			mEpisodeList.clear();
			for (int i=0;i<videoList.length();i++) {
				JSONObject episode = videoList.getJSONObject(i);
				
				String title = episode.getString("video_name");
				String picUrl = episode.getString("hor_high_pic");
				int aid = episode.getInt("aid");
				int	vid = episode.getInt("vid");
				String playurl = episode.getString("download_url");
				
				EpisodeSohu e = new EpisodeSohu(title, picUrl, aid, vid, playurl);
				mEpisodeList.add(e);
			}
			
			return true;
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
		
		return false;
	}
	
	public boolean album(int topic_id, int page_index, int page_size) {
		String url = String.format(TOPIC_LIST_URL_FMT, page_index, page_size);
		System.out.println("Java: SohuUtil album() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			int count = data.getInt("count");
			JSONArray topicList = data.getJSONArray("topic");
			
			for (int i=0;i<topicList.length();i++) {
				JSONObject topic = topicList.getJSONObject(i);
				int tid = topic.getInt("tid");
				if (tid == topic_id) {
					JSONArray albums = topic.getJSONArray("albums");
					
					mAlbumList.clear();
					
					for (int k=0;k<albums.length();k++) {
						JSONObject album = albums.getJSONObject(k);
						
						String name = album.getString("album_name");
						String tip = album.getString("tip");
						int aid = album.getInt("aid");
						int cid = album.getInt("cid");
						String picUrl = album.getString("hor_high_pic");
						AlbumSohu a = new AlbumSohu(name + "(" + tip + ")", picUrl, aid, cid);
						mAlbumList.add(a);
					}

					break;
				}
			}
			
			return true;
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
		
		return false;
	}
	
	public boolean topic(int page_index, int page_size) {
		String url = String.format(TOPIC_LIST_URL_FMT, page_index, page_size);
		System.out.println("Java: SohuUtil topic() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			int count = data.getInt("count");
			JSONArray topicList = data.getJSONArray("topic");
			
			mTopicList.clear();
			
			for (int i=0;i<topicList.length();i++) {
				JSONObject topic = topicList.getJSONObject(i);
				int tid = topic.getInt("tid");
				String topic_name = topic.getString("topic_name");
				
				TopicSohu t = new TopicSohu(topic_name, tid);
				mTopicList.add(t);
			}
			
			return true;
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
		
		return false;
	}
	
	public PlaylinkSohu getPlayLink(int vid, int sid) {
		String url = String.format(API_URL, vid, sid);
		System.out.println("Java: SohuUtil getPlayLink " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return null;
			}
			
			JSONObject data = root.getJSONObject("data");
			String normal_url = data.getString("url_nor_mp4");
			String tv_name = data.getString("tv_name");
			String high_url = data.getString("url_high_mp4");
			
			JSONArray clipsDuration_nor = data.getJSONArray("clipsDuration_nor");
			StringBuffer sbNormal = new StringBuffer();
			for (int k=0;k<clipsDuration_nor.length();k++) {
				double du = clipsDuration_nor.getDouble(k);
				sbNormal.append((int)(du * 1000));
				sbNormal.append(",");
				System.out.println(String.format("Java: segment #%d %.3f sec", k, du));
			}
			
			JSONArray clipsDuration_high = data.getJSONArray("clipsDuration_high");
			StringBuffer sbHigh = new StringBuffer();
			for (int k=0;k<clipsDuration_high.length();k++) {
				double du = clipsDuration_high.getDouble(k);
				sbHigh.append((int)(du * 1000));
				sbHigh.append(",");
				System.out.println(String.format("Java: segment #%d %.3f sec", k, du));
			}
			
			return new PlaylinkSohu(tv_name, normal_url, high_url, sbNormal.toString(), sbHigh.toString());
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
