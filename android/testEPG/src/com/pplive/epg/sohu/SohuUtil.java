package com.pplive.epg.sohu;

import java.io.IOException;
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

public class SohuUtil {
	private static final String PLAYINFO_URL = "http://api.tv.sohu.com/video/playinfo/" +
			"%d.json?api_key=9854b2afa779e1a6bff1962447a09dbd&plat=6&sver=3.1&partner=47&c=2&sid=%d";
	
	private static final String CHANNEL_LIST_URL = "http://api.tv.sohu.com/v5/mobile/channel/list.json?" +
			"plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.7.1&sysver=4.2.2&partner=340";
	
	private static final String SUBCHANNEL_URL_FMT = "http://api.tv.sohu.com/v5/mobile/channelPageData/list.json" +
			"?plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.7.1" +
			"&page_size=%d&offset=%d&sysver=4.2.2&sub_channel_id=%d&partner=340&cursor=0";
	
	private static final String SEARCH_CHANNEL_SURFIX_FMT = "&plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&page_size=%d&offset=%d&sysver=4.2.2&partner=340";
	
	private static final String COLUMN_URL = "http://api.tv.sohu.com/v4/mobile/column/list.json?" +
			"cate_code=9006&plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&sysver=4.2.2&partner=340";
	
	private static final String VIDEO_INFO_URL_FMT = "http://api.tv.sohu.com/v4/video/info/" +
			"%d.json?" +
			"site=%d" +
			"&plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&sysver=4.2.2&partner=340&aid=%d";
	
	private static final String TOPIC_LIST_URL_FMT = "http://api.tv.sohu.com/v4/personal/tv/individuation.json?" +
			"plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&uid=9354d3e14fdc4aa4999aff3790dab635" +
			"&sver=4.7.1&cat=9008&page=%d&page_size=%d&sysver=4.2.2&partner=340";
	
	private static final String ALBUM_INFO_URL_FMT = "http://api.tv.sohu.com/v4/album/info/" +
			"%d.json?" +
			"area_code=42&plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.7.1&sysver=4.2.2&partner=340";
	
	private static final String EPISODE_URL_FMT = "http://api.tv.sohu.com/v4/album/videos/" +
			"%d.json?order=0&site=1&with_trailer=1&plat=6" +
			"&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&page=%d&page_size=%d&sysver=4.2.2&partner=340";
	
	private static final String SEARCH_URL_FMT = "http://api.tv.sohu.com/v4/search/all.json?" +
			"pgc=1&area_code=42&plat=6&poid=1" +
			"&api_key=9854b2afa779e1a6bff1962447a09dbd&uid=9354d3e14fdc4aa4999aff3790dab635" +
			"&pay=1&sver=4.7.1" +
			"&key=%s&page=%d&page_size=%d&ds=&sysver=4.2.2&type=1&partner=340&all=1";
	
	private static final String LIVE_URL_FMT = "http://api.tv.sohu.com/tv_live/live/liveBroadcast.json?" +
			"n=%d&plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&sysver=4.2.2&p=1&partner=340";
	
	private static final String LIVE_DETAIL_URL_FMT = "http://api.tv.sohu.com/tv_live/live/LiveDetail.json?" +
			"n=%d&plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd" +
			"&sver=4.7.1&sysver=4.2.2&p=1&partner=340&tvId=%d";
	
	private List<LiveChannelSohu> mLiveChannelList;
	private List<ChannelSohu> mChannelList;
	private List<SubChannelSohu> mSubChannelList;
	private List<CategorySohu> mCategoryList;
	private List<TopicSohu> mTopicList;
	private List<AlbumSohu> mAlbumList;
	private List<EpisodeSohu> mEpisodeList;
	private List<AlbumSohu> mSearchItemList;
	private String mMoreListPrefix;
	
	public SohuUtil() {
		mLiveChannelList	= new ArrayList<LiveChannelSohu>();
		mChannelList		= new ArrayList<ChannelSohu>();
		mSubChannelList		= new ArrayList<SubChannelSohu>();
		mCategoryList		= new ArrayList<CategorySohu>();
		mTopicList 			= new ArrayList<TopicSohu>();
		mAlbumList 			= new ArrayList<AlbumSohu>();
		mEpisodeList 		= new ArrayList<EpisodeSohu>();
		mSearchItemList 	= new ArrayList<AlbumSohu>();
	}
	
	public List<CategorySohu> getCateList() {
		return mCategoryList;
	}
	
	public List<LiveChannelSohu> getLiveChannelList() {
		return mLiveChannelList;
	}
	
	public List<ChannelSohu> getChannelList() {
		return mChannelList;
	}
	
	public List<SubChannelSohu> getSubChannelList() {
		return mSubChannelList;
	}
	
	public String getMoreList() {
		return mMoreListPrefix;
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
	
	public boolean live(int num) {
		String url = String.format(LIVE_URL_FMT, num);
		System.out.println("Java: SohuUtil live() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to cate() %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray list = data.getJSONArray("list");
			int count = data.getInt("count");
			int c = list.length();
			
			mLiveChannelList.clear();
			for (int i=0;i<c;i++) {
				JSONObject live_channel = list.getJSONObject(i);
				
				/*
				"soonTime":"2015-06-05 13:59:00.0",
				"tvId":74,
				"ipLimit":0,
				"icoBigPic":"http://i2.itc.cn/20130105/2dd1_c980a5c3_6efb_743a_cdef_796a798442b1_1.png",
				"flashLiveUrl":"http://gslb.tv.sohu.com/live?cid=3&ver=seg&ckey=fMIg7Eo-XzM8&prot=3&sig=gAy5gRhyl6FvB7HpXlfLxQ..",
				"icoSmallPic":"http://i2.itc.cn/20120119/2cea_6d1489c8_3604_3af9_1887_d58c9eba318c_4.png",
				"soon":"下午剧场：中国刑警803 23",
				"nowTime":"2015-06-05 13:17:00.0",
				"enName":"dragontv",
				"now":"下午剧场：中国刑警803 22",
				"name":"东方卫视",
				"action":0,
				"liveUrl":"http://gslb.tv.sohu.com/live?cid=3&ver=seg&ckey=fMIg7Eo-XzM8&type=hls&prot=1&sig=gAy5gRhyl6FvB7HpXlfLxQ..&type=hls",
				live_channel.getString("");
				*/
				
				int tvId = live_channel.getInt("tvId");
				String title = live_channel.getString("name");
				String icon_big = live_channel.getString("icoBigPic");
				String icon_small = live_channel.getString("icoSmallPic");
				String now_play = live_channel.getString("now");
				String will_play = live_channel.getString("soon");
				String now_time = live_channel.getString("nowTime");
				String will_time = live_channel.getString("soonTime");
				String live_url = live_channel.getString("liveUrl");
				
				LiveChannelSohu lc = new LiveChannelSohu(tvId, title, now_play, will_play, 
						icon_small, icon_big, live_url);
				mLiveChannelList.add(lc);
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
	
	public boolean live_detail(int tv_id) {
		String url = String.format(LIVE_DETAIL_URL_FMT, tv_id);
		System.out.println("Java: SohuUtil live_detail() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to cate() %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray menu = data.getJSONArray("menu");
			int count = data.getInt("count");
			int c = menu.length();
			
			for (int i=0;i<c;i++) {
				JSONObject prog = menu.getJSONObject(i);
				
				
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
	
	public boolean channel_select(String channelId) {
		System.out.println("Java: SohuUtil cate() channelId " + channelId);
		System.out.println("Java: SohuUtil cate() " + CHANNEL_LIST_URL);
		
		HttpGet request = new HttpGet(CHANNEL_LIST_URL);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to cate() %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray cateCodes = data.getJSONArray("cateCodes");
			int count = data.getInt("count");
			
			mSubChannelList.clear();
			for (int i=0;i<count;i++) {
				JSONObject cate = cateCodes.getJSONObject(i);
				
				/* live channle has no id
				{
					"icon":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_live_4.7.1.png",
					"name":"直播",
					"cid":9002,
					"cate_code":9002,
					"icon_selected":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_live_4.7.1_selected.png"
				}*/
				
				if (cate.has("channeled") && cate.getString("channeled").equals(channelId)) {
					JSONArray sub_channels = cate.getJSONArray("sub_channel");
					int count_sub = sub_channels.length();
					for (int j=0;j<count_sub;j++) {
						JSONObject sub_c = sub_channels.getJSONObject(j);
						
						/*
						"name":"推荐",
						"load_more":1,
						"sub_channel_type":0,
						"sub_channel_id":1000000
						 */
						String title = sub_c.getString("name");
						int sub_channel_type = sub_c.getInt("sub_channel_type");
						int sub_channel_id = sub_c.getInt("sub_channel_id");
						
						SubChannelSohu sub = new SubChannelSohu(title, sub_channel_type, sub_channel_id);
						mSubChannelList.add(sub);
					}
					
					return true;
				}	
			}
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
	
	public boolean cate_search(String cate_url) {
		System.out.println("Java: SohuUtil cate() " + cate_url);
		
		HttpGet request = new HttpGet(cate_url);
		
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
			JSONArray categorys = data.getJSONArray("categorys");
			
			mCategoryList.clear();
			int count = categorys.length();
			for (int i=0;i<count;i++) {
				JSONObject item = categorys.getJSONObject(i);
				
				/*
				 "cate_name":"排序",
	            "cate_alias":"o",
	            "default_keys":"-1"
				 */
				String cate_name = item.getString("cate_name");
				String alias = item.getString("cate_alias");
				String default_keys = item.getString("default_keys");
				
				JSONArray cates = item.getJSONArray("cates");
				int c2 = cates.length();
				for (int j=0;j<c2;j++) {
					JSONObject cate = cates.getJSONObject(j);
					/*
					"name":"新上架",
                    "search_key":"1"
                    */
					String title = cate.getString("name");
					String search_key = cate.getString("search_key");
					
					CategorySohu ca = new CategorySohu(title, cate_name, alias, search_key, default_keys);
					mCategoryList.add(ca);
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
	
	public AlbumSohu album_info(long aid) {
		String url = String.format(ALBUM_INFO_URL_FMT, aid);
		System.out.println("Java: SohuUtil album_info() " + url);
		
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
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return null;
			}
			
			JSONObject video = root.getJSONObject("data");
			
			return getAlbum(video, 0, "");
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
	
	public boolean channel_list() {
		System.out.println("Java: SohuUtil channel_list() " + CHANNEL_LIST_URL);
		
		HttpGet request = new HttpGet(CHANNEL_LIST_URL);
		
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
			JSONArray cateCodes = data.getJSONArray("cateCodes");
			int count = data.getInt("count");
			
			mChannelList.clear();
			for (int i=0;i<count;i++) {
				JSONObject cate = cateCodes.getJSONObject(i);
				
				/*
				"channeled":"1000021000",
				"icon":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_episode_4.7.1.png",
				"name":"电视剧",
				"cate_api":"http://api.tv.sohu.com/v4/category/teleplay.json?",
				"cid":2,
				"cate_code":101,
				"icon_selected":"http://tv.sohu.com/upload/clientapp/channelicon/gphone/channel_icon_episode_4.7.1_selected.png",
				*/
				
				String channelId = getNodeString(cate, "channeled");
				String iconUrl = cate.getString("icon");
				String title = cate.getString("name");
				if (!cate.has("cate_api")) {
					System.out.println("Java: no cate_api");
					continue;
				}
				
				String cate_api = getNodeString(cate, "cate_api");
				int cid = cate.getInt("cid");
				int cate_code = cate.getInt("cate_code");

				final String cate_surfix = "?&plat=6&poid=1" +
						"&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.7.1" +
						"&sysver=4.2.2&sub_channel_id=1000000&partner=340";
				ChannelSohu c = new ChannelSohu(title, channelId, iconUrl, cate_api + cate_surfix, 
						cid, cate_code);
				mChannelList.add(c);
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
	
	public PlaylinkSohu video_info(int site, int vid, long aid) {
		String url = String.format(VIDEO_INFO_URL_FMT, vid, site, aid);
		System.out.println("Java: SohuUtil video_info() " + url);
		
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
			String tv_name = data.getString("video_name");
			
			String normal_url = getNodeString(data, "url_nor_mp4");
			String high_url = getNodeString(data, "url_high_mp4");
			
			String clipsDuration_nor = getNodeString(data, "clips_duration_nor");
			String clipsDuration_high = getNodeString(data, "clips_duration_high");
			
			String clipsSize_nor = getNodeString(data, "clips_size_nor");
			String clipsSize_high = getNodeString(data, "clips_size_high");

			return new PlaylinkSohu(tv_name, normal_url, high_url, 
					clipsDuration_nor, clipsDuration_high,
					clipsSize_nor, clipsSize_high);
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
	
	public boolean morelist(String morelist_prefix, int page_size, int page_offset) {
		String url = morelist_prefix + String.format(SEARCH_CHANNEL_SURFIX_FMT, page_size, page_offset);
		System.out.println("Java: SohuUtil morelist() " + url);
		
		int subId = -1;
		int pos = url.indexOf("subId=");
		if (pos != -1) {
			int pos2 = url.indexOf("&", pos);
			String strSub = url.substring(pos + 6, pos2);
			subId = Integer.valueOf(strSub);
		}
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to subchannel() %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray videos = data.getJSONArray("videos");
			int count = data.getInt("count");
			
			mAlbumList.clear();
			int c = videos.length();
			
			if (c == 0)
				return false;
			
			for (int j=0;j<c;j++) {
				JSONObject video = videos.getJSONObject(j);
				
				String album_name = video.getString("album_name");
				String video_name = video.getString("video_name");
				
				int is_album = video.getInt("is_album");
				String album_title = album_name;
				if (is_album == 0)
					album_title = video_name;
				//if ((subId >= 50 && subId < 60) || subId > 200) {
				
				String second_cate_name = getNodeString(video, "second_cate_name");
				int site = video.getInt("site");
				int v_count = video.getInt("total_video_count");
				int last_count = video.getInt("latest_video_count");
				
				double score = video.getDouble("score");
				double douban_score = 0.0f;
				if (video.has("douban_score"))
					douban_score = video.getDouble("douban_score");
				
				String desc = "N/A";
				if (video.has("album_desc"))
					desc = video.getString("album_desc");
				else if (video.has("tv_desc"))
					desc = video.getString("tv_desc");
				
				long aid = video.getLong("aid");
				// 2015.9.6 vid key is removed
				int vid = getNodeInt(video, "vid");
				int cid = video.getInt("cid");
				String hori_pic_url = getNodeString(video, "hor_high_pic");
				String vert_pic_url = getNodeString(video, "ver_high_pic");
				String area = "N/A";
				if (video.has("area"))
					area = video.getString("area");
				String year = "N/A";
				if (video.has("year"))
					year = video.getString("year");
				
				String main_actor = "N/A";
				if (video.has("main_actor"))
					main_actor = video.getString("main_actor");
					
				String director = "N/A";
				if (video.has("director"))
					video.getString("director");
				
				String tip = getNodeString(video, "tip");
				String score_tip = getNodeString(video, "score_tip");
				int duration_sec = 0;
				if (video.has("time_length"))
					duration_sec = video.getInt("time_length");
				
				AlbumSohu album = new AlbumSohu(0, "", 
						album_title, second_cate_name, v_count, last_count, is_album == 1? true: false,
						aid, vid, cid, desc, tip, site, 
						score, douban_score, score_tip, 
						director, main_actor, 
						year, area,
						hori_pic_url, vert_pic_url, "",
						duration_sec);
				
				mAlbumList.add(album);
			}
			
			return true;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean subchannel(int sub_channel_id, int page_size, int page_offset) {
		String url = String.format(SUBCHANNEL_URL_FMT, page_size, page_offset, sub_channel_id);
		System.out.println("Java: SohuUtil subchannel() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to subchannel() %d %s", status, statusText));
				return false;
			}
			
			JSONObject data = root.getJSONObject("data");
			JSONArray columns = data.getJSONArray("columns");
			
			mAlbumList.clear();
			int column_size = columns.length();
			
			if (column_size == 0)
				return false;
			
			for (int i=0;i<column_size;i++) {
				JSONObject column = columns.getJSONObject(i);
				int column_id = column.getInt("column_id");
				String column_name = getNodeString(column, "name");
				
				// more_list=http://api.tv.sohu.com/v4/search/channel/sub.json?subId=200&
				mMoreListPrefix = getNodeString(column, "more_list");
				
				if (!column.has("video_list")) {
					System.out.println("Java: no video_list");
					continue;
				}
				
				JSONArray video_list = column.getJSONArray("video_list");
				int c = video_list.length();
				for (int j=0;j<c;j++) {
					JSONObject video = video_list.getJSONObject(j);
					
					AlbumSohu album = getAlbum(video, column_id, column_name);
					if (album != null)
						mAlbumList.add(album);
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
	
	public boolean channel_xxx(int sub_channel_id, int page_size, int page_offset) {
		String url = String.format(SUBCHANNEL_URL_FMT, page_size, page_offset, sub_channel_id);
		System.out.println("Java: SohuUtil channel() " + url);
		
		HttpGet request = new HttpGet(CHANNEL_LIST_URL);
		
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
			JSONArray categorys = data.getJSONArray("categorys");
			
			mCategoryList.clear();
			int count = categorys.length();
			for (int i=0;i<count;i++) {
				JSONObject item = categorys.getJSONObject(i);
				
				/*
				 "cate_name":"排序",
	            "cate_alias":"o",
	            "default_keys":"-1"
				 */
				String cate_name = item.getString("cate_name");
				String alias = item.getString("cate_alias");
				String default_keys = item.getString("default_keys");
				
				JSONArray cates = item.getJSONArray("cates");
				int c2 = cates.length();
				for (int j=0;j<c2;j++) {
					JSONObject cate = cates.getJSONObject(j);
					/*
					"name":"新上架",
                    "search_key":"1"
                    */
					String title = cate.getString("name");
					String search_key = cate.getString("search_key");
					
					CategorySohu ca = new CategorySohu(title, cate_name, alias, search_key, default_keys);
					mCategoryList.add(ca);
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
	
	public boolean column() {
		System.out.println("Java: SohuUtil column() " + COLUMN_URL);
		
		HttpGet request = new HttpGet(COLUMN_URL);
		
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
			JSONArray columns = data.getJSONArray("columns");
			int count = data.getInt("count");
			
			mChannelList.clear();
			for (int i=0;i<count;i++) {
				JSONObject column = columns.getJSONObject(i);
				
				/*
				"channeled":"1000130001",
				"column_type":4,
				"column_id":128,
				"jump_cate_code":0,
				"layout_type":101,
				"more_list":"http://api.tv.sohu.com/v4/search/stream/6.json?channeled=1000130001&page_size=20&",
				"content_size":0,
				"more_list_layout_type":1,
				"name":"精选"
				*/
				
				String channelId = column.getString("channeled");
				int column_type = column.getInt("column_type");
				int column_id = column.getInt("column_id");
				String title = column.getString("name");
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
				
				AlbumSohu al = getAlbum(item, 0, "");
				if (al != null)
					mSearchItemList.add(al);
				
				/*if (item.has("album_name")) {
					String picUrl = item.getString("ver_high_pic");
					long aid = item.getLong("aid");
					int	cid = item.getInt("cid");
					
					String album_name = item.getString("album_name");
					String video_name = getNodeString(item, "video_name");
					int is_album = item.getInt("is_album");
					String album_title = album_name;
					if (is_album == 0)
						album_title = video_name;
					
					String cate = getNodeString(item, "second_cate_name");
					
					String main_actor = getNodeString(item, "main_actor");
					int v_count = item.getInt("total_video_count");
					int last_count = item.getInt("latest_video_count");
					
					int site = -1;
					
					if (item.has("videos")) {
						JSONArray videos = item.getJSONArray("videos");
						for (int j=0;j<videos.length();j++) {
							JSONObject v = videos.getJSONObject(j);
							int video_vid = v.getInt("vid");
							
							if (site == -1 && v.has("site"))
								site = v.getInt("site");
						}
					}
					
					AlbumSohu a = new AlbumSohu(album_title, cate, main_actor,
							aid, v_count, last_count, 
							is_album == 1 ? true : false, site);
					
					mSearchItemList.add(a);
				}*/
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
	
	public boolean episode(long aid, int page_index, int page_size) {
		String url = String.format(EPISODE_URL_FMT, aid, page_index, page_size);
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
				long video_aid = episode.getLong("aid");
				int	video_vid = episode.getInt("vid");
				
				String playurl = "";
				if (episode.has("download_url"))
					playurl = episode.getString("download_url");
				
				EpisodeSohu e = new EpisodeSohu(title, picUrl, video_aid, video_vid, playurl);
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
	
	/*public boolean album_detail(int aid, int vid, int page_index, int page_size) {
		String url = String.format(ALBUM_DETAIL_URL_FMT, aid, vid, page_index, page_size);
		System.out.println("Java: SohuUtil album_detail() " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to album_detail %d %s", status, statusText));
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
				int video_aid = episode.getInt("aid");
				int	video_id = episode.getInt("vid");
				String playurl = episode.getString("download_url");
				
				EpisodeSohu e = new EpisodeSohu(title, picUrl, video_aid, video_id, playurl);
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
	}*/

	/*public boolean album(int topic_id, int page_index, int page_size) {
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
						int vid = album.getInt("vid");
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
	}*/
	
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
	
	public PlaylinkSohu playlink_pptv(int vid, int sid) {
		String url = String.format(PLAYINFO_URL, vid, sid);
		System.out.println("Java: SohuUtil getPlayLink " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			String strRes = result;
			if (strRes.length() > 64)
				strRes = strRes.substring(0, 64);
			System.out.println("Java: result: " + strRes);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			String statusText = root.getString("statusText");
			if (status != 200) {
				System.out.println(String.format("Java: failed to get url %d %s", status, statusText));
				return null;
			}
			
			JSONObject data = root.getJSONObject("data");
			String tv_name = getNodeString(data, "tv_name");
			
			String normal_url	= getNodeString(data, "url_nor_mp4");
			String high_url		= getNodeString(data, "url_high_mp4");
			
			String StrNorDuration = "";
			if (data.has("clipsDuration_nor")) {
				JSONArray clipsDuration_nor = data.getJSONArray("clipsDuration_nor");
				StringBuffer sbNormal = new StringBuffer();
				for (int k=0;k<clipsDuration_nor.length();k++) {
					double du = clipsDuration_nor.getDouble(k);
					sbNormal.append(du);
					sbNormal.append(",");
					System.out.println(String.format("Java: segment #%d %.3f sec", k, du));
				}
				StrNorDuration = sbNormal.toString();
			}
			
			String StrHighDuration = "";
			if (data.has("clipsDuration_high")) {
				JSONArray clipsDuration_high = data.getJSONArray("clipsDuration_high");
				StringBuffer sbHigh = new StringBuffer();
				for (int k=0;k<clipsDuration_high.length();k++) {
					double du = clipsDuration_high.getDouble(k);
					sbHigh.append((int)(du));
					sbHigh.append(",");
					System.out.println(String.format("Java: segment #%d %.3f sec", k, du));
				}
				StrHighDuration = sbHigh.toString();
			}
			
			String StrNorBytes = "";
			if (data.has("clipsBytes_nor")) {
				JSONArray clipsSize_nor = data.getJSONArray("clipsBytes_nor");
				StringBuffer sbNor = new StringBuffer();
				for (int k=0;k<clipsSize_nor.length();k++) {
					int size = clipsSize_nor.getInt(k);
					sbNor.append(size);
					sbNor.append(",");
					
					String unit = " byte";
					double unified_size = size;
					if (unified_size > 1024) {
						unified_size /= (double)1024;
						unit = "k byte";
					}
					if (unified_size > 1024) {
						unified_size /= (double)1024;
						unit = "M byte";
					}
					System.out.println(String.format("Java: normal segment #%d %.3f%s", 
							k, unified_size, unit));
				}
				StrNorBytes = sbNor.toString();
			}
			
			String StrHighBytes = "";
			if (data.has("clipsBytes_high")) {
				JSONArray clipsSize_high = data.getJSONArray("clipsBytes_high");
				StringBuffer sbHigh = new StringBuffer();
				for (int k=0;k<clipsSize_high.length();k++) {
					int size = clipsSize_high.getInt(k);
					sbHigh.append(size);
					sbHigh.append(",");
					
					String unit = " byte";
					double unified_size = size;
					if (unified_size > 1024) {
						unified_size /= (double)1024;
						unit = "k byte";
					}
					if (unified_size > 1024) {
						unified_size /= (double)1024;
						unit = "M byte";
					}
					System.out.println(String.format("Java: high segment #%d %.3f%s", 
							k, unified_size, unit));
				}
				StrHighBytes = sbHigh.toString();
			}
			
			return new PlaylinkSohu(tv_name, normal_url, high_url, 
					StrNorDuration, StrHighDuration, 
					StrNorBytes, StrHighBytes);
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
	
	private AlbumSohu getAlbum(JSONObject video, int column_id, String column_name) {
		try {
			/*  movie
			"hor_w16_pic":"http://photocdn.sohu.com/tvmobile/20140727/1503/mvrs_album_5064833_640_360.jpg",
			"second_cate_name":"战争片;剧情片;动作片",
			"score":8.9,
			"douban_score":6.6,
			"aid":5064833,
			"hor_w8_pic":"http://photocdn.sohu.com/tvmobile/20140727/1503/mvrs_album_5064833_320_180.jpg",
			"tip":"8.9分",
			"time_length":6526,
			"ver_high_pic":"http://photocdn.sohu.com/20120821/vrsa_ver5064833_pic26.jpg",
			"main_actor":"黛安·克鲁格,杰曼·翰苏,伯努瓦·马吉梅,拉斐尔·佩尔索纳",
			"area":"法国",
			"score_tip":"8.9分",
			"total_video_count":1,
			"hor_w6_pic":"http://photocdn.sohu.com/tvmobile/20140727/1503/mvrs_album_5064833_240_135.jpg",
			"year":"2011",
			"album_name":"特种部队",
			"c":"http://photocdn.sohu.com/tvmobile/20140121/2210/13873596308631209.jpg",
			"vid":780014,
			"video_big_pic":"http://img.m.tv.sohu.com/mvrs/13877882956201673.jpg",
			"hor_big_pic":"http://photocdn.sohu.com/tvmobile/20140121/2210/13873596309811082.jpg",
			"play_count":70799555,
			"recommend_tip":"特种兵救女主玩命逃亡",
			"director":"斯蒂芬·莱波贾",
			"cate_code":"100;100101;100108;100106",
			"cid":1,
			"latest_video_count":1,
			"album_desc":"一名法国战地记者女记者Elsa（黛安·克鲁格 饰）和她的同事在巴基斯坦采访期间遭到塔利班绑架。塔利班将被捕获的Elsa相关视频放倒了网络上，并声称在规定时间内，她将在摄像机面前被处死，全世界的人民都可以通过网络看到全过程。在Elsa被行刑之前，一支由特种部队成员组成的救援小组被法国军方派来营救她。面对着极其残暴的塔利班亡命之徒，救援小组不畏艰险在第一时间就救出了Elsa，却与总部失去联系，不得不开始一场更为险恶的保卫战。一场在塔利班追捕者和救援小组之间的拉锯逃亡战不可避免的展开了。塔利班设下各种陷阱，要将救援小组一网打尽，而救援小组只有一个目标：成功营救Elsa逃离此地。",
			"ver_big_pic":"http://photocdn.sohu.com/20120821/vrsab_ver5064833.jpg",
			"video_name":"特种部队",
			
			tv series
			"hor_w16_pic":"http://photocdn.sohu.com/tvmobile/20150319/085851/album_5359902_640_360.jpg",
			"fee_month":0,
			"latest_video_count_tip":"更新至42集",
			"second_cate_name":"武侠剧;古装剧;言情剧;悬疑剧",
			"is_original_code":0,
			"score":7.6,
			"douban_score":3.8,
			"aid":5359902,
			"hor_w8_pic":"http://photocdn.sohu.com/tvmobile/20150319/085852/album_5359902_320_180.jpg",
			"tip":"更新至42集",
			"time_length":2341,
			"ver_high_pic":"http://photocdn.sohu.com/tvmobile/20150319/084853/album_5359902_240_330.jpg",
			"main_actor":"张翰,陈伟霆,杨洋,茅子俊,何晟铭,张钧甯,吴映洁,贾青",
			"area":"内地剧",
			"score_tip":"7.6分",
			"total_video_count":48,
			"hor_w6_pic":"http://photocdn.sohu.com/tvmobile/20150319/085850/album_5359902_240_135.jpg",
			"data_type":1,
			"year":"2015",
			"program_id":18468,
			"album_name":"少年四大名捕（2015）",
			"hor_high_pic":"http://photocdn.sohu.com/20150312/vrsa_hor5359902_e86H6_pic25.jpg",
			"vid":2391784,
			"video_big_pic":"http://photocdn.sohu.com/20150526/vrs1654342_jJbe4_pic6.jpg",
			"site":1,
			"hor_big_pic":"http://photocdn.sohu.com/20130515/vrsab_hor5359902.jpg",
			"play_count":456767211,
			"mobileLimit":0,
			"director":"黄俊文 ,梁胜权",
			"cate_code":"101;101105;101106;101104;101112",
			"cid":2,
			"latest_video_count":42,
			"album_desc":"宋徽宗时，神侯府总管诸葛正我率手下四大名捕冷血、无情、追命、铁手四人护卫京城。冷血救下少女楚离陌，诸葛正我欣赏离陌，留府培养。冷血和离陌因此结下一生一世的缘分，二人性格不合，误会不断，但却冤家路窄。楚离陌执行任务时失踪，冷血在内心开始审视自己对其情感，外出寻找离陌，二人历尽辛苦走到一起，遭遇仇人陷害，处于困境时互不抛弃，终于两颗心走到一起。王爷安世耿一心排除异己制造混乱，四大名捕侦破安世耿所制造的玉玺案、驱狐案，安世耿视四大名捕为死敌。女神捕姬遥花暗恋冷血，对离陌心生妒忌，后又遭安世耿利用，一起对付四大名捕。冷血众人和楚离陌联手，对付安世耿。姬遥花迷途知返，关键时刻刺杀安世耿，取得诸葛正我的宽恕。楚离陌和冷血终成眷属，其他三位名捕也有自己的归宿，姬遥花也走出爱的阴影。",
			"fee":0,
			"ver_big_pic":"http://photocdn.sohu.com/tvmobile/20150319/084852/album_5359902_120_165.jpg",
			"season":1,
			"video_name":"少年四大名捕（2015）第42集",
			"publish_time":"2015-05-27",
			"ver_w12_pic":"http://photocdn.sohu.com/tvmobile/20150319/084853/album_5359902_480_660.jpg",
			"is_album":1
			*/
			
			String album_name = getNodeString(video, "album_name");
			String video_name = getNodeString(video, "video_name");
			if (album_name.isEmpty() && video_name.isEmpty()) {
				System.out.println("Java: album and video name are all empty");
				return null;
			}
			
			int is_album = 0;
			if (video.has("is_album"))
				is_album = video.getInt("is_album");
			String album_title = album_name;
			if (is_album == 0)
				album_title = video_name;
			
			String second_cate_name = getNodeString(video, "second_cate_name");
			int site = -1;
			if (video.has("site"))
				site = video.getInt("site");
			int v_count = 0;
			if (video.has("total_video_count"))
				v_count = video.getInt("total_video_count");
			int last_count = 0;
			if (video.has("latest_video_count"))
				last_count = video.getInt("latest_video_count");
			
			double score = 0.0f;
			if (video.has("score"))
				score = video.getDouble("score");
			double douban_score = 0.0f;
			if (video.has("douban_score"))
				douban_score = video.getDouble("douban_score");
			
			String desc = "N/A";
			if (video.has("album_desc"))
				desc = video.getString("album_desc");
			else if (video.has("tv_desc"))
				desc = video.getString("tv_desc");
			
			if (!video.has("aid")) {
				System.out.println("Java: video has no aid");
				return null;
			}
			
			long aid = video.getLong("aid");
			int vid = 0;
			if (video.has("vid"))
				vid = video.getInt("vid");
			int cid = video.getInt("cid");
			String hori_pic_url = getNodeString(video, "hor_high_pic");
			String vert_pic_url = getNodeString(video, "ver_high_pic");
			String area = "N/A";
			if (video.has("area"))
				area = video.getString("area");
			String year = "N/A";
			if (video.has("year"))
				year = video.getString("year");
			
			String main_actor = "N/A";
			if (video.has("main_actor"))
				main_actor = video.getString("main_actor");
				
			String director = "N/A";
			if (video.has("director"))
				video.getString("director");
			
			String tip = getNodeString(video, "tip");
			String score_tip = getNodeString(video, "score_tip");
			int duration_sec = 0;
			if (video.has("time_length"))
				duration_sec = video.getInt("time_length");
			
			// for search result
			if (video.has("videos")) {
				JSONArray videos = video.getJSONArray("videos");
				for (int j=0;j<videos.length();j++) {
					JSONObject v = videos.getJSONObject(j);
					int video_vid = v.getInt("vid");
					
					if (site == -1 && v.has("site"))
						site = v.getInt("site");
				}
			}
			
			return new AlbumSohu(column_id, column_name, 
					album_title, second_cate_name, v_count, last_count, is_album == 1 ? true : false,
					aid, vid, cid, desc, tip, site, 
					score, douban_score, score_tip, 
					director, main_actor, 
					year, area,
					hori_pic_url, vert_pic_url, "",
					duration_sec);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String getNodeString(JSONObject node, String key) {
		String strRet = "";
		if (node.has(key)) {
			try {
				strRet = node.getString(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return strRet;
	}
	
	private int getNodeInt(JSONObject node, String key) {
		int retVal = 0;
		if (node.has(key)) {
			try {
				retVal = node.getInt(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return retVal;
	}
	
}
