package com.pplive.common.pptv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


import android.util.Log;

public class EPGUtil { 
	private final static String TAG = "EPGUtil";
	
	private final int HOST_PORT = 80;
	
	private final static String frontpage_url = "http://mtbu.api.pptv.com/v4/module?lang=zh_cn&platform=aphone"
			+ "&appid=com.pplive.androidphone&appver=4.1.3"
			+ "&appplt=aph&userLevel=0";
	
	private final static String catalog_url_prefix = "http://mtbu.api.pptv.com/v4/module"
			+ "?lang=zh_cn&platform=aphone&appid=com.pplive.androidphone"
			+ "&appver=4.1.3&appplt=aph&userLevel=0&channel=@SHIP.TO.31415926PI@";
	
	private final static String ppi = "&ppi=1:eyJpbmZvIjp7ImMiOjEsInMiOjIsImFjIjpbMiwyMDA0M10sImV" +
			"oIjoiMjAxNTA0MDIwMCJ9LCJkcyI6Ik1Dd0NGQkd0SjZnelpSUWJ4dUJZdCtlUnhCWXJ2Nm1iQ" +
			"WhRZGNJUU9YWjEvN2o1WThaaWpXZG5hU3hRd2l3PT0ifQ";
	
	private final static String catalog_url_prefix2 = "http://mtbu.api.pptv.com/v4/module" +
			"?lang=zh_cn&platform=aphone&appid=com.pplive.androidphone" +
			"&appver=5.0.3&appplt=aph&userLevel=1" +
			ppi + 
			"&channel=82";
	
	private final static String search_url_fmt = "http://so.api.pptv.com/search_smart.api"
			+ "?auth=d410fafad87e7bbf6c6dd62434345818"
			+ "&appver=4.1.3&canal=@SHIP.TO.31415926PI@"
			+ "&userLevel=0&hasVirtual=1"
			+ "&k=%s"
			+ "&conlen=0&shownav=1"
			+ "&type=%d"
			+ "&mode=all"
			+ "&contentype=%d"// 0-只正片，1-非正片，-1=不过滤
			+ "&s=%d"
			+ "&c=%d" // display item count
			+ "&ver=2&platform=android3";
	
	private final static String detail_url_fmt = "http://epg.api.pptv.com/detail.api"
			+ "?auth=d410fafad87e7bbf6c6dd62434345818"
			+ "&canal=@SHIP.TO.%s"
			+ "PI@&userLevel=0&appid=com.pplive.androidphone&appver=4.1.3"
			+ "&appplt=aph"
			+ "&vid=%s" // 8022983
			+ "&series=1&virtual=1&ver=2&platform=android3";
	
	private final static String list_url_prefix_fmt = "http://epg.api.pptv.com/list.api"
			+ "?auth=d410fafad87e7bbf6c6dd62434345818"
			+ "&appver=4.1.3&canal=@SHIP.TO.31415926PI@"
			+ "&userLevel=0&virtual=1&platform=android3"
			+ "&s=%d" // start page index
			+ "&%s" // order=xx 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
			+ "&c=%d" // count
			//+ "&vt=3,21" // 21 -> 3,21
			+ "&ver=2";
	
	private final static String live_url_fmt = "http://epg.api.pptv.com/live-list.api?"
			+ "auth=d410fafad87e7bbf6c6dd62434345818&userLevel=0"
			+ "&s=%d"
			+ "&c=%d"
			+ "&platform=android3&vt=4"
			+ "&type=%d" // 156 地方台, 164 卫视
			+ "&nowplay=1"
			+ "&appid=com.pplive.androidphone&appver=4.1.3&appplt=aph";
	
	private final static String boxplay_prefix = "http://play.api.pptv.com/boxplay.api?" + 
			"platform=android3&type=phone.android.vip&sv=4.0.1&param=";
	
	private final static String boxplay_fmt = "&sdk=1&channel=162&content=need_drag" + 
			"&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=41" +
			"&id=%s&ft=1&k_ver=1.1.0";
	
	private final static String getvchannel_fmt = "http://epg.api.pptv.com/getvchannel?" +
			"platform=android3&pagesize=%d&infoid=%d&siteid=%d&nowpage=%d";
	
	private List<Content> mContentList;
	private List<Module> mModuleList;
	private List<Catalog> mCatalogList;
	private List<PlayLink2> mPlayLinkList;
	private List<Navigator> mNavList;
	
	private List<Episode> mVirtualPlayLinkList;
	private List<VirtualChannelInfo> mVchannelInfoList;
	
	public EPGUtil() {
		mContentList 		= new ArrayList<Content>();
		mModuleList 		= new ArrayList<Module>();
		mCatalogList 		= new ArrayList<Catalog>();
		mPlayLinkList 		= new ArrayList<PlayLink2>();
		mNavList			= new ArrayList<Navigator>();
		
		mVirtualPlayLinkList= new ArrayList<Episode>();
		mVchannelInfoList	= new ArrayList<VirtualChannelInfo>();
	}
	
	public List<Content> getContent() {
		return mContentList;
	}
	
	public List<Module> getModule() {
		return mModuleList;
	}
	
	public List<Catalog> getCatalog() {
		return mCatalogList;
	}
	
	public List<PlayLink2> getLink() {
		return mPlayLinkList;
	}
	
	public List<VirtualChannelInfo> getVchannelInfo() {
		return mVchannelInfoList;
	}
	
	public List<Navigator> getNav() {
		return mNavList;
	}
	
	public List<Episode> getVirtualLink() {
		return mVirtualPlayLinkList;
	}
	
	public boolean live(int start_page, int count, int type) {
		String url = String.format(live_url_fmt, start_page, count, type);
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        mPlayLinkList.clear();
	       
	        List<Element> v_list = root.getChildren("v");
			Iterator<Element> it2 = v_list.iterator();
	        while (it2.hasNext()){   
	        	Element v  = (Element) it2.next();
	        	String title = v.getChild("title").getText();
	        	String vid = v.getChild("vid").getText();
	        	String nowplay = v.getChild("nowplay").getText();
	        	System.out.println(String.format("title: %s, id: %s, nowplay: %s", title, vid, nowplay));
	        	PlayLink2 l = new PlayLink2(title, vid, nowplay);
	        	mPlayLinkList.add(l);
	        }
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	public boolean contents_list() {
		String cate;
		try {
			String sur = "app://aph.pptv.com/v4/cate";
			cate = URLEncoder.encode(sur, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = catalog_url_prefix2 + "&location=" + cate;
		
		Log.i(TAG, "Java: epg contents_list " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject item = (JSONObject) jsonParser.nextValue();
			JSONArray modules = item.getJSONArray("modules");
			
			mModuleList.clear();
			
			JSONObject program = modules.getJSONObject(0).getJSONObject("data");
			JSONArray contents = program.getJSONArray("dlist");
			for (int i=0;i<contents.length();i++) {
				JSONObject c = contents.getJSONObject(i);
				Module new_mod = new Module(i, c.getString("title"), c.getString("target"), c.getString("link"));
				mModuleList.add(new_mod);
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
		
		return true;
	}
	
	public boolean contents(String surfix) {
		
		String cate;
		try {
			String sur = "app://aph.pptv.com/v4/cate";
			if(surfix != null && !surfix.isEmpty())
				sur = surfix;
			cate = URLEncoder.encode(sur, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = catalog_url_prefix + "&location=" + cate;
		
		Log.i(TAG, "Java: epg contents " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject item = (JSONObject) jsonParser.nextValue();
			JSONArray modules = item.getJSONArray("modules");
			
			mContentList.clear();
			
			int module_index = 0;
			if ("app://aph.pptv.com/v4/cate/sports?type=5".equals(surfix))
				module_index = 1; // hard code
			
			JSONObject program = modules.getJSONObject(module_index).getJSONObject("data");
			JSONArray contents = program.getJSONArray("dlist");
			for (int i=0;i<contents.length();i++) {
				JSONObject obj = contents.getJSONObject(i);
				
				JSONArray tags = obj.getJSONArray("tags");
				if (tags == null)
					continue;
				
				for (int j=0;j<tags.length();j++) {
					JSONObject c = tags.getJSONObject(j);
					Content new_content = new Content(
							c.getString("id"), c.getString("title"), c.getString("param"), c.getString("position"));
					mContentList.add(new_content);
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
		
		return true;
	}
	
	public boolean frontpage() {
		Log.i(TAG, "Java: epg url " + frontpage_url);
		HttpGet request = new HttpGet(frontpage_url);
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				Log.e(TAG, "failed to connect to epg server");
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject item = (JSONObject) jsonParser.nextValue();
			JSONArray modules = item.getJSONArray("modules");
			
			mModuleList.clear();
			
			for (int i=0;i<modules.length();i++) {
				JSONObject programs = modules.getJSONObject(i);
				JSONObject data = programs.getJSONObject("data");
				if (!data.isNull("title")) {
					String target = "";
					String link = "";
					if (!data.isNull("target"))
						target = data.getString("target");
					if (!data.isNull("link"))
						link = data.getString("link");
					Module c = new Module(i, data.getString("title"), target, link);
					mModuleList.add(c);
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
		
		return true;
	}
	
	public boolean catalog(int index) {
		Log.i(TAG, "Java: epg url " + frontpage_url);
		HttpGet request = new HttpGet(frontpage_url);
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				Log.e(TAG, "failed to connect to epg server");
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject item = (JSONObject) jsonParser.nextValue();
			JSONArray modules = item.getJSONArray("modules");
			
			mCatalogList.clear();
			
			JSONObject programs = modules.getJSONObject(index);
			JSONObject data = programs.getJSONObject("data");
			JSONArray data_array = data.getJSONArray("dlist");
			
			for (int i=0;i<data_array.length();i++){  
                JSONObject collection = data_array.getJSONObject(i);
                Catalog c = new Catalog(collection.getString("title"), 
                		collection.getString("target"), collection.getString("link"));
				mCatalogList.add(c);
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
		
		return true;
	}
	
	public boolean list(String param, String type, 
			int start_page, String order, int count) {
		String encoded_param;
		int pos = param.indexOf('=');
		if (pos == -1)
			encoded_param = param;
		else {
			String tmp = param.substring(pos + 1, param.length());
			try {
				encoded_param = param.substring(0, pos + 1) + URLEncoder.encode(tmp, "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			Log.i(TAG, "Java: epg encoded param " + encoded_param);
		}
		
		String url = String.format(list_url_prefix_fmt, 
				start_page, order, count);
		
		if (type != null && !type.isEmpty()) {
			url += "&";
			url += type;
		}
		
		url += "&";
		url += encoded_param;
		url += "&appid=com.pplive.androidphone&appplt=aph";
		url += ppi;
		Log.i(TAG, "Java epg list() url: " + url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		//request.setHeader("User-Agent", 
		//		"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");  
		HttpResponse response;
		
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			Log.i(TAG, "Java epg result " + result.replace("\n", ""));
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        int total_count = Integer.valueOf(root.getChild("count").getText());
	        int page_count = Integer.valueOf(root.getChild("page_count").getText());
	        int countInPage = Integer.valueOf(root.getChild("countInPage").getText());
	        int page = Integer.valueOf(root.getChild("page").getText()); // == input param s
	        Log.i(TAG, 
	        	String.format("total_count %d, page_count %d, countInPage %d, page %d",
	        	total_count, page_count, countInPage, page));
	        
	        mPlayLinkList.clear();
	        
	        List<Element> v_list = root.getChildren("v");
			Iterator<Element> it2 = v_list.iterator();
	        while (it2.hasNext()){   
	        	Element v  = (Element) it2.next();
	        	ret = add_v(v);
	        	if (!ret)
	        		break;
	        }
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return ret;
	}
	
	public boolean virtual_channel(String title, int info_id, int page_size, int site_id, int now_page) {
		//http://epg.api.pptv.com/getvchannel?platform=android3&pagesize=200&infoid=1056&siteid=3&nowpage=4
			
		String url = String.format(getvchannel_fmt, page_size, info_id, site_id, now_page);
		Log.i(TAG, "Java: virtual_channel() " + url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200) {
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        mVirtualPlayLinkList.clear();
	        ret = add_episode(root, title);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return ret;
	}
	
	public boolean detail(String vid) {
		String url = String.format(detail_url_fmt, vid, vid);
		Log.i(TAG, "Java: epg url " + url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				Log.e(TAG, "failed to connect to epg server");
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			Log.d(TAG, "Java: epg result " + result.replace("\n", ""));
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        mPlayLinkList.clear();
	        
	        ret = add_v(root);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return ret;
	}
	
	private boolean add_episode(Element v, String main_title) {
		List<Element> sites = v.getChildren("site");
		Element site= sites.get(0);
		String siteid = site.getAttributeValue("siteid");
		
		List<Element> episodes = site.getChildren("episode");
		for (int i=0;i<episodes.size();i++) {
			Element episode = episodes.get(i);
			String vid = episode.getAttributeValue("vid");
			String title = episode.getAttributeValue("title");
			String extid = episode.getAttributeValue("extid");
			
			String final_title = main_title + "(" + title + ")";
			
			Episode e = new Episode(Integer.valueOf(siteid), final_title, vid, extid);
			mVirtualPlayLinkList.add(e);
		}
		
		return true;
	}
	
	private boolean add_v(Element v) {
		String link_title  = v.getChild("title").getText();
    	String link_id = v.getChild("vid").getText();
    	String link_director = v.getChild("director").getText();
    	String link_act = v.getChild("act").getText();
    	String link_year = v.getChild("year").getText();
    	String link_area = v.getChild("area").getText();
    	
    	String str_du;
    	int duration_sec;
    	Element du = v.getChild("durationSecond");
    	if (du != null) {
    		str_du = du.getText();
    		duration_sec = Integer.valueOf(str_du);
    	}
    	else {
    		du = v.getChild("duration");
    		str_du = du.getText();
    		duration_sec = Integer.valueOf(str_du) * 60;
    	}

    	String link_resolution = v.getChild("resolution").getText();
    	
    	String link_description = "";
    	Element content = v.getChild("content");
    	if (content != null)
    		link_description = content.getText();
    	
    	//virtual channel
    	List<Element> virtuals = v.getChildren("virtual");
    	if (virtuals.size() > 1) {
        	String infoId = v.getChild("infoId").getText();

	    	Element virtual = virtuals.get(1);
	    	
    		List<Element> sites = virtual.getChildren("site");
    		if (sites.size() > 0) {
    			mVchannelInfoList.clear();
    			
    			boolean found_sohu = false;
    			for (int c=0;c<sites.size();c++) {
		        	Element site = sites.get(c);
		        	String logo_url = site.getAttributeValue("logo");
		        	String title = site.getAttributeValue("title");
		        	String siteid = site.getAttributeValue("siteid");
		        	String total = site.getAttributeValue("total");
		        	String mode = site.getAttributeValue("mode");
		        	List<Element> link_vlist = site.getChildren("episode");
		        	
		        	if (siteid.equals("3") /*sohu*/)
		        		found_sohu = true;
		        	
		        	VirtualChannelInfo vInfo = new VirtualChannelInfo(link_title, logo_url, 
		        			Integer.valueOf(siteid), Integer.valueOf(infoId), 
		        			Integer.valueOf(total), Integer.valueOf(mode));
		        	mVchannelInfoList.add(vInfo);
		        	
		        	Log.i(TAG, String.format("Java: virtual channel info_id %s, title: %s, siteid %s, total %s",
		        			infoId, title, siteid, total));
    			}

    			if (!found_sohu) {
    				Log.w(TAG, "Java: sohu virtual channel was NOT found");
    				return false;
    			}
    			
	        	return true;
    		}
    	}
    	
    	List<Element> linklist = null;
        Element video_list2 = v.getChild("video_list2");
        if (video_list2 != null)
        	linklist = video_list2.getChildren("playlink2");
        else
        	linklist = v.getChildren("playlink2");
        
        if (linklist.size() < 1)
        	return false;
        
        for (int i=0;i<linklist.size();i++) {
        	Element playlink2 = linklist.get(i);
        	String id = playlink2.getAttributeValue("id");
        	if (id != null && !id.isEmpty())
        		link_id = id; // overwrite
        	
        	String ext_title;
        	if (linklist.size() == 1)
        		ext_title = "";
        	else
        		ext_title = playlink2.getAttributeValue("title");
        	
        	Element source = playlink2.getChild("source");
	    	String src_mark = source.getAttributeValue("mark");
	    	String src_res = source.getAttributeValue("resolution");
	    	if(src_res != null && !src_res.isEmpty())
	    		link_resolution = src_res; // overwrite
	    	PlayLink2 l = new PlayLink2(link_title, ext_title, link_id, link_description, 
	    			src_mark, link_director, link_act,
	    			link_year, link_area,
	    			link_resolution, duration_sec);
	    	mPlayLinkList.add(l);
        }
        
        //Log.d(TAG, "mPlayLinkList size " + mPlayLinkList.size());
        return true;
	}
	
	public int[] getAvailableFT(String link) {
		Log.i(TAG, String.format("java: getAvailableFT() %s", link));
		
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
		Log.i(TAG, "Java: getAvailableFT() url " + sbBoxPlayUrl.toString());

		HttpGet request = new HttpGet(sbBoxPlayUrl.toString());
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(response.getEntity());
				return parseCdnUrlxml_ft(result);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private int[] parseCdnUrlxml_ft(String xml) {
		Log.i(TAG, "Java: epg parseCdnUrlxml " + xml.replace("\n", ""));
		
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(xml);  
        Document doc;
		try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			ArrayList<CDNrid> ridList = new ArrayList<CDNrid>();
			ArrayList<CDNItem> itemList = new ArrayList<CDNItem>();
			
			Element channel = root.getChild("channel");
			Element file = channel.getChild("file");
			if (file == null)
				return null;
			
			List<Element> item_list = file.getChildren("item");
			
			int[] ft = new int[item_list.size()];
			for (int i=0;i<item_list.size();i++) {
				String rid_ft = item_list.get(i).getAttributeValue("ft");
				String rid_rid = item_list.get(i).getAttributeValue("rid");
				CDNrid rid = new CDNrid(rid_ft, rid_rid);
				ridList.add(rid);
				ft[i] = Integer.valueOf(rid_ft);
			}
			
			return ft;
		}
		catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return null;
	}
	
	public String getCDNUrl(String link, String ft, boolean is_m3u8, boolean noVideo) {
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
		Log.i(TAG, "Java: epg url " + sbBoxPlayUrl.toString());

		HttpGet request = new HttpGet(sbBoxPlayUrl.toString());
		
		HttpResponse response;
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() == 200) {
				String result = EntityUtils.toString(response.getEntity());
				return parseCdnUrlxml(result, ft, is_m3u8, noVideo);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String parseCdnUrlxml(String xml, String ft, boolean is_m3u8, boolean novideo) {
		Log.i(TAG, "Java: epg parseCdnUrlxml " + xml.replace("\n", ""));
		String url = null;
		
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(xml);  
        Document doc;
		try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			ArrayList<CDNrid> ridList = new ArrayList<CDNrid>();
			ArrayList<CDNItem> itemList = new ArrayList<CDNItem>();
			
			Element file = root.getChild("channel").getChild("file");
			List<Element> item_list = file.getChildren("item");
			for (int i=0;i<item_list.size();i++) {
				String rid_ft = item_list.get(i).getAttributeValue("ft");
				String rid_rid = item_list.get(i).getAttributeValue("rid");
				CDNrid rid = new CDNrid(rid_ft, rid_rid);
				ridList.add(rid);
			}
			
			List<Element> dt = root.getChildren("dt");
			for (int i=0;i<dt.size();i++) {
				Element d = dt.get(i);
				
				String d_ft = d.getAttributeValue("ft");
				String d_sh = d.getChild("sh").getText(); // main server
				String d_bh = d.getChild("bh").getText(); // backup server
				String d_st = d.getChild("st").getText(); // server time
				String d_key = d.getChild("key").getText();
				
				CDNItem item = new CDNItem(d_ft, d_sh, d_st, d_bh, d_key);
				itemList.add(item);
			}
			
			// generate cdn url
			for (int i=0;i<itemList.size();i++) {
				CDNItem item = itemList.get(i);

				if (item.getFT().equals(ft)) {
					Log.i(TAG, "Java: epg start to gen cdn url");
					
					String host = item.getHost();
					if (!host.contains("http://"))
			            url = "http://" + host;
			        else
			            url = host;
					if (!host.contains(":"))
						url += ":" + HOST_PORT;
					
					url += "/";
					
					String rid = "";
					for (int j=0;j<ridList.size();j++) {
						CDNrid r = ridList.get(j);
						if (r.m_ft.equals(ft)) {
							rid = r.m_rid;
							break;
						}
					}

					if (novideo/*force use m3u8*/ || is_m3u8)
						url += rid.replaceFirst(".mp4", ".m3u8");
					else
						url += rid;
			        
					url += "?w=" + 1 + "&key=" + item.getK();
					url += "&k=" + item.getKey();
					if (novideo)
						url += "&video=false";
					url += "&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1"
							+ "&sv=4.1.3&platform=android3";
					url += "&ft=" + ft;
					//url += "&accessType=wifi";
					
					Log.i(TAG, "Java: epg final cdn url: " + url);
					break;
				}
			}
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return url;
	}
	
	public boolean search(String s_key, int s_type, int s_contenttype, int s_start_page, int s_count) {
		String url = String.format(search_url_fmt, s_key, s_type, s_contenttype, s_start_page, s_count);
		Log.i(TAG, "Java: epg url " + url);

		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = new DefaultHttpClient().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				Log.e(TAG, "failed to connect to epg server");
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			Log.d(TAG, "Java: epg result " + result.replace("\n", ""));
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        // get nav info
	        Element nav = root.getChild("nav");
	        if (nav != null) {
		        List<Element> typelist = nav.getChildren("type");
		        
		        mNavList.clear();
		        
		        Iterator<Element> it = typelist.iterator();
		        while (it.hasNext()){   
		        	Element nav_type  = (Element) it.next();
		        	String name = nav_type.getAttributeValue("name");
		        	String id = nav_type.getAttributeValue("id");
		        	int count = Integer.valueOf(nav_type.getAttributeValue("count"));
		        	
		        	Navigator n = new Navigator(name, id, count);
		        	mNavList.add(n);
		        }
	        }
	        
	        // get video info
	        mPlayLinkList.clear();
	        
			List<Element> v_list = root.getChildren("v");
			Iterator<Element> it2 = v_list.iterator();
	        while (it2.hasNext()){   
	        	Element v  = (Element) it2.next();
	        	ret = add_v(v);
	        	if (!ret)
	        		break;
	        }
	        
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
}