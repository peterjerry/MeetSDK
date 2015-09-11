package com.pplive.epg.pptv;

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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class EPGUtil {
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
			+ "&conlen=0"
			+ "&shownav=1"
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
			//+ "&vt=3,21" // 21 -> 3,21 视频, param: vt=3, 合集, param: vt=21,22
			+ "&ver=2";
	
	private final static String live_url_fmt = "http://epg.api.pptv.com/live-list.api?"
			+ "auth=d410fafad87e7bbf6c6dd62434345818&userLevel=0"
			+ "&s=%d"
			+ "&c=%d"
			+ "&platform=android3&vt=4"
			+ "&type=%d" // 156 地方台, 164 卫视
			+ "&nowplay=1"
			+ "&appid=com.pplive.androidphone&appver=4.1.3&appplt=aph";
	
	private final static String live_cdn_url_fmt = "http://play.api.pptv.com/boxplay.api?" +
			"ft=1" +
			"&platform=android3" +
			"&type=phone.android.vip" +
			"&sdk=1" +
			"&channel=162" +
			"&vvid=41" +
			"&auth=55b7c50dc1adfc3bcabe2d9b2015e35c" +
			"&id=%d";
	
	private final static String boxplay_prefix = "http://play.api.pptv.com/boxplay.api?" + 
			"platform=android3&type=phone.android.vip&sv=4.0.1&param=";
	
	private final static String boxplay_fmt = "&sdk=1&channel=162&content=need_drag" + 
			"&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=41" +
			"&id=%s&ft=1&k_ver=1.1.0";
	
	private final static String getvchannel_fmt = "http://epg.api.pptv.com/getvchannel?" +
			"platform=android3&pagesize=%d&infoid=%d&siteid=%d&nowpage=%d";
	
	private final static String live_center_fmt = "http://livecenter.pptv.com" +
			"/api/v1/collection?" +
			"auth=d410fafad87e7bbf6c6dd62434345818&platform=android3" +
			"&id=%s" + // 44 -> sports, game -> game
			"&start=%s" + // 2015-09-08
			"&appid=com.pplive.androidphone" +
			"&appver=5.2.1&appplt=aph&recommend=0";
	
	private List<Content> mContentList;
	private List<Module> mModuleList;
	private List<Catalog> mCatalogList;
	private List<PlayLink2> mPlayLinkList;
	private List<Navigator> mNavList;
	private List<LiveStream> mLiveStrmList;
	
	private List<Episode> mVirtualPlayLinkList;
	private String mStrInfoId;
	
	public EPGUtil() {
		mContentList 			= new ArrayList<Content>();
		mModuleList 			= new ArrayList<Module>();
		mCatalogList 			= new ArrayList<Catalog>();
		mPlayLinkList			= new ArrayList<PlayLink2>();
		mVirtualPlayLinkList 	= new ArrayList<Episode>();
		mNavList 				= new ArrayList<Navigator>();
		mLiveStrmList			= new ArrayList<LiveStream>();
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
	
	public List<Episode> getVirtualLink() {
		return mVirtualPlayLinkList;
	}
	
	public List<Navigator> getNav() {
		return mNavList;
	}
	
	public List<LiveStream> getLiveStrm() {
		return mLiveStrmList;
	}
	
	public String getInfoId() {
		return mStrInfoId;
	}
	
	public CDNItem live_cdn(int vid) {
		String url = String.format(live_cdn_url_fmt, vid);
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			return parseLiveCdnUrlxml(result);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private CDNItem parseLiveCdnUrlxml(String xml) {
		System.out.println("Java: epg parseLiveCdnUrlxml \n" + xml.replace("\n", ""));
		
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(xml);  
        Document doc;
		try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			Element channel = root.getChild("channel");
			Element stream = channel.getChild("stream");
			/*
			 * <stream delay="45" interval="5" jump="1800" cft="1">
			 * <item rid="677a6d8e5f264fe9b5b87553c8e033e2" bitrate="412" width="480" height="360" ft="1" syncid="399" vip="0" protocol="live2" format="h264" />
			 * </stream>
			 */
			String delay = stream.getAttributeValue("delay");
			String interval = stream.getAttributeValue("interval");
			
			Element strm_item = stream.getChild("item");
			String rid = strm_item.getAttributeValue("rid");
			String bitrate = strm_item.getAttributeValue("bitrate");
			String width = strm_item.getAttributeValue("width");
			String height = strm_item.getAttributeValue("height");
			String ft = strm_item.getAttributeValue("ft");
			String format = strm_item.getAttributeValue("format");
			
			System.out.println(String.format("Java: stream_info delay %s, interval %s, " +
					"rid %s, bitrate %s, width %s, height %s, ft %s, format %s",
					delay, interval, rid, bitrate, width, height, ft, format));

			Element d = root.getChildren("dt").get(0);
			
			String d_sh = d.getChild("sh").getText(); // main server
			String d_bh = d.getChild("bh").getText(); // backup server
			String d_st = d.getChild("st").getText(); // server time
			String d_key = d.getChild("key").getText();
			
			CDNItem item = new CDNItem(ft, Integer.valueOf(width), Integer.valueOf(height), 
					d_sh, d_st, d_bh, d_key, rid);	
			return item;
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
	
	public boolean live(int start_page, int count, int type) {
		String url = String.format(live_url_fmt, start_page, count, type);
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
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
	        	
	        	Element img = v.getChild("imgurl");
		    	String imgUrl = "";
		    	if (img != null)
		    		imgUrl = img.getText();
		    	
	        	System.out.println(String.format("title: %s, id: %s, nowplay: %s", title, vid, nowplay));
	        	PlayLink2 l = new PlayLink2(title, vid, nowplay, imgUrl);
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
		
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
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
		
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
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
		System.out.println(frontpage_url);
		
		HttpGet request = new HttpGet(frontpage_url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
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
					Module c = new Module(i, data.getString("title"), /*data.getString("target")*/"", data.getString("link"));
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
	
	public boolean catalog(int catalog_index) {
		if (catalog_index < 0)
			return false;
		
		System.out.println(frontpage_url);
		HttpGet request = new HttpGet(frontpage_url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject item = (JSONObject) jsonParser.nextValue();
			JSONArray modules = item.getJSONArray("modules");
			
			mCatalogList.clear();
			
			JSONObject programs = modules.getJSONObject(catalog_index);
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
		}
		System.out.println("encoded param " + encoded_param);
		
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
		System.out.println("Java epg list() url: " + url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        int total_count = Integer.valueOf(root.getChild("count").getText());
	        int page_count = Integer.valueOf(root.getChild("page_count").getText());
	        int countInPage = Integer.valueOf(root.getChild("countInPage").getText());
	        int page = Integer.valueOf(root.getChild("page").getText()); // == input param s
	        System.out.println(
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
	
	public boolean virtual_channel(int info_id, int page_size, int site_id, int now_page) {
		//http://epg.api.pptv.com/getvchannel?platform=android3&pagesize=200&infoid=1056&siteid=3&nowpage=4
			
		String url = String.format(getvchannel_fmt, page_size, info_id, site_id, now_page);
		System.out.println("Java: virtual_channel() url: " + url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        mVirtualPlayLinkList.clear();
	        ret = add_episode(root);
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
		System.out.println(url);
		
		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = HttpClients.createDefault().execute(request);
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
	
	private boolean add_episode(Element v) {
		List<Element> sites = v.getChildren("site");
		Element site= sites.get(0);
		String siteid = site.getAttributeValue("siteid");
		
		List<Element> episodes = site.getChildren("episode");
		for (int i=0;i<episodes.size();i++) {
			Element episode = episodes.get(i);
			String vid = episode.getAttributeValue("vid");
			String title = episode.getAttributeValue("title");
			String extid = episode.getAttributeValue("extid");
			
			Episode e = new Episode(Integer.valueOf(siteid), title, vid, extid);
			mVirtualPlayLinkList.add(e);
		}
		
		return true;
	}
	
	private String getXMLNode(Element e, String key) {
		Element node = e.getChild(key);
		if (node == null)
			return null;
		
		return node.getText();
	}
	
	private boolean add_v(Element v) {
		String link_title  = v.getChild("title").getText();
    	String link_id = v.getChild("vid").getText();
    	String link_director = getXMLNode(v, "director");
    	String link_act = getXMLNode(v, "act");
    	String link_year = getXMLNode(v, "year");
    	String link_area = getXMLNode(v, "area");
    	/*String link_director = v.getChild("director").getText();
    	String link_act = v.getChild("act").getText();
    	String link_year = v.getChild("year").getText();
    	String link_area = v.getChild("area").getText();*/
    	
    	String str_du;
    	int duration_sec;
    	Element du = v.getChild("durationSecond");
    	if (du != null) {
    		str_du = du.getText();
    		duration_sec = Integer.valueOf(str_du);
    	}
    	else {
    		str_du = getXMLNode(v, "duration");
    		if (str_du == null)
    			duration_sec = 0;
    		else
    			duration_sec = Integer.valueOf(str_du) * 60;
    	}
    	
    	String link_resolution = v.getChild("resolution").getText();
    	
    	String link_description = "";
    	Element content = v.getChild("content");
    	if (content != null)
    		link_description = content.getText();
    	
    	List<Element> linklist = null;
    	
    	//virtual channel
    	List<Element> virtuals = v.getChildren("virtual");
    	if (virtuals.size() > 1) {
        	String strInfoId = v.getChild("infoId").getText();

	    	Element virtual = virtuals.get(1);
	    	
    		List<Element> sites = virtual.getChildren("site");
    		if (sites.size() > 0) {
	        	for(int k=0;k<sites.size();k++) {
	    			Element site = sites.get(k);
		        	String title = site.getAttributeValue("title");
		        	String total = site.getAttributeValue("total");
		        	List<Element> link_vlist = site.getChildren("episode");
		        	mStrInfoId = strInfoId;
		        	
		        	System.out.println(String.format("Java: virtual channel infoid %s, title: %s, total %s",
		        			strInfoId, title, total));
	        	}

	        	return true;
    		}
    	}
    	
        Element video_list2 = v.getChild("video_list2");
        if (video_list2 != null)
        	linklist = video_list2.getChildren("playlink2");
        else
        	linklist = v.getChildren("playlink2");
        
        if (linklist.size() < 1) {
        	System.out.println("Java: both real playlink and virtual channel NOT found!");
        	return false;
        }
        
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
	    	
	    	Element img = playlink2.getChild("imgurl");
	    	String imgUrl = "";
	    	if (img != null)
	    		imgUrl = img.getText();
	    	
	    	PlayLink2 l = new PlayLink2(link_title, ext_title, link_id, link_description, 
	    			src_mark, link_director, link_act,
	    			link_year, link_area,
	    			link_resolution, duration_sec, imgUrl);
	    	mPlayLinkList.add(l);
        }
        
        return true;
	}
	
	public int[] getAvailableFT(String link) {
		System.out.println("Java: getAvailableFT() " + link);
		
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
		System.out.println("Java: epg url " + sbBoxPlayUrl.toString());

		HttpGet request = new HttpGet(sbBoxPlayUrl.toString());
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
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
		System.out.println("Java: epg parseCdnUrlxml " + xml.replace("\n", ""));
		
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(xml);  
        Document doc;
		try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			ArrayList<CDNstream> ridList = new ArrayList<CDNstream>();
			ArrayList<CDNItem> itemList = new ArrayList<CDNItem>();
			
			Element file = root.getChild("channel").getChild("file");
			List<Element> item_list = file.getChildren("item");
			
			int[] ft = new int[item_list.size()];
			for (int i=0;i<item_list.size();i++) {
				String rid_ft = item_list.get(i).getAttributeValue("ft");
				String rid_rid = item_list.get(i).getAttributeValue("rid");
				CDNstream rid = new CDNstream(rid_ft, rid_rid);
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
		System.out.println("getCDNUrl() " + link);
		
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
		System.out.println(sbBoxPlayUrl.toString());

		HttpGet request = new HttpGet(sbBoxPlayUrl.toString());
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
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
		System.out.println("parseCdnUrlxml " + xml.replace("\n", ""));
		String url = null;
		
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(xml);  
        Document doc;
		try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			ArrayList<CDNstream> strmList = new ArrayList<CDNstream>();
			ArrayList<CDNItem> itemList = new ArrayList<CDNItem>();
			
			Element file = root.getChild("channel").getChild("file");
			List<Element> item_list = file.getChildren("item");
			/*<file cur="1">
			  <item rid="902ad08f97cc2efa521ef6942a34e464.mp4" bitrate="337" vip="0" ft="0" filesize="321628975" width="480" height="216" format="h264" /> 
			  <item rid="923b1ccda4d404a11b591ad24bf7cece.mp4" bitrate="518" vip="0" ft="1" filesize="494892624" width="720" height="320" format="h264" /> 
			  <item rid="60118991ffd3a28e0f91e0184f78860f.mp4" bitrate="1047" vip="0" ft="2" filesize="999054567" width="1280" height="572" format="h264" /> 
			  <item rid="9fda672c9e06768ce923554e53011d45.mp4" bitrate="2255" vip="1" ft="3" filesize="2152478843" width="1920" height="856" format="h264" /> 
			  <item rid="d439e51f236d4b1d3131b90dc38e5865.mp4" bitrate="6108" vip="1" ft="4" filesize="5828167836" width="1920" height="856" format="h264" /> 
			  </file>*/

			for (int i=0;i<item_list.size();i++) {
				Element strm_item = item_list.get(i);
				String rid_ft = strm_item.getAttributeValue("ft");
				String rid_rid = strm_item.getAttributeValue("rid");
				String bitrate = strm_item.getAttributeValue("bitrate");
				String width = strm_item.getAttributeValue("width");
				String height = strm_item.getAttributeValue("height");
				String format = strm_item.getAttributeValue("format");
				CDNstream strm = new CDNstream(rid_ft, rid_rid, 
						format, Integer.valueOf(width), Integer.valueOf(height), Integer.valueOf(bitrate));
				strmList.add(strm);
			}
			
			List<Element> dt = root.getChildren("dt");
			for (int i=0;i<dt.size();i++) {
				Element d = dt.get(i);
				
				String d_ft = d.getAttributeValue("ft");
				String d_sh = d.getChild("sh").getText(); // main server
				String d_bh = d.getChild("bh").getText(); // backup server
				String d_st = d.getChild("st").getText(); // server time
				String d_key = d.getChild("key").getText();
				
				CDNItem item = new CDNItem(d_ft, 0, 0, 
						d_sh, d_st, d_bh, d_key, strmList.get(i).m_rid);
				itemList.add(item);
			}
			
			// generate cdn url
			for (int i=0;i<itemList.size();i++) {
				CDNItem item = itemList.get(i);

				if (item.getFT().equals(ft)) {
					String host = item.getHost();
					if (!host.contains("http://"))
			            url = "http://" + host;
			        else
			            url = host;
					if (!host.contains(":"))
						url += ":" + HOST_PORT;
					
					url += "/";
					
					String rid = "";
					for (int j=0;j<strmList.size();j++) {
						CDNstream r = strmList.get(j);
						if (r.m_ft.equals(ft)) {
							rid = r.m_rid;
							break;
						}
					}

					if (novideo/*force use m3u8*/ || is_m3u8)
						url += rid.replaceFirst(".mp4", ".m3u8");
					else
						url += rid;
					
					url += "?w=" + 1 + "&key=" + item.generateK();
					url += "&k=" + item.getKey();
					if (novideo)
						url += "&video=false";
					url += "&type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1"
							+ "&sv=4.1.3&platform=android3";
					url += "&ft=" + ft;
					url += "&accessType=wifi";

					
					System.out.println("epg final cdn url: " + url);
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
		System.out.println(url);

		boolean ret = false;
		
		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println(result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        // get nav info
	        Element nav = root.getChild("nav");
	        if(nav != null) {
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
		        	
		        	String nav_info = String.format("%s, id %s, count %s", name, id, count);
		        	System.out.println(nav_info);
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
	
	
	/**
	 * @param id
	 * @param start_time format 2015-09-04
	 * @return
	 */
	public boolean live_center(String id, String start_time) {
		String url = String.format(live_center_fmt, id, start_time);
		
		System.out.println("Java: epg live_center() " + url);

		HttpGet request = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to connect to epg server");
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			//System.out.println("Java: epg result " + result.replace("\n", ""));
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element collections = doc.getRootElement();
	        Element collection = collections.getChild("collection");
	        
	        Element sections = collection.getChild("sections");
	        if (sections == null)
	        	return false;
	        
	        List<Element> section = sections.getChildren("section");
	        if (section == null || section.size() == 0)
	        	return false;
	        
	        mLiveStrmList.clear();
	        
	        for (int i=0;i<section.size();i++) {
	        	Element program = section.get(i);
	        	String prog_title = program.getChildText("title");
	        	String prog_id = program.getChildText("id");
	        	String prog_start_time = program.getChildText("start_time");
	        	String prog_end_time = program.getChildText("end_time");
	        	
	        	Element streams = program.getChild("streams");
	        	Element stream = streams.getChild("stream");
	        	String channel_id = stream.getChildText("channel_id");
	        	
	        	mLiveStrmList.add(new LiveStream(prog_id, prog_title, channel_id, 
	        			prog_start_time, prog_end_time, null));
	        	
	        	System.out.println(String.format("Java: title %s, id %s, start %s, end %s, channel_id %s",
	        			prog_title, prog_id, prog_start_time, prog_end_time, channel_id));
	        }

	        return true;
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
		
		return false;
	}
}
