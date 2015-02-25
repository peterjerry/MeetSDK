package com.pplive.epg;

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
			//+ "&location=app%3A%2F%2Faph.pptv.com%2Fv4%2Fcate";
	
	private final static String search_url_fmt = "http://so.api.pptv.com/search_smart.api"
			+ "?auth=d410fafad87e7bbf6c6dd62434345818"
			+ "&appver=4.1.3&canal=@SHIP.TO.31415926PI@"
			+ "&userLevel=0&hasVirtual=1"
			+ "&k=%s"
			+ "&conlen=0&shownav=1"
			+ "&type=%s"
			+ "&mode=all"
			+ "&contentype=%s"// 0-只正片，1-非正片，-1=不过滤
			+ "&c=%s" // display item count
			+ "&s=1&ver=2&platform=android3";
	
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
			+ "&s=1"
			+ "&order=t" // 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
			+ "&c=%d" // list count number
			+ "&vt=3,21" // 21 -> 3,21
			+ "&ver=2"; // &type=1
	
	private final static String live_url_fmt = "http://epg.api.pptv.com/live-list.api?"
			+ "auth=d410fafad87e7bbf6c6dd62434345818&userLevel=0"
			+ "&c=%d"
			+ "&s=1&platform=android3&vt=4"
			+ "&type=%d" // 156
			+ "&nowplay=1"
			+ "&appid=com.pplive.androidphone&appver=4.1.3&appplt=aph";
	
	private final static String boxplay_prefix = "http://play.api.pptv.com/boxplay.api?" + 
			"platform=android3&type=phone.android.vip&sv=4.0.1&param=";
	
	private final static String boxplay_fmt = "&sdk=1&channel=162&content=need_drag" + 
			"&auth=55b7c50dc1adfc3bcabe2d9b2015e35c&vvid=41" +
			"&id=%s&ft=1&k_ver=1.1.0";
	
	private ArrayList<Content> mContentList;
	private ArrayList<Module> mModuleList;
	private ArrayList<Catalog> mCatalogList;
	private ArrayList<PlayLink2> mPlayLinkList;
	private ArrayList<Navigator> mNavList;
	
	public EPGUtil() {
		mContentList = new ArrayList<Content>();
		mModuleList = new ArrayList<Module>();
		mCatalogList = new ArrayList<Catalog>();
		mPlayLinkList = new ArrayList<PlayLink2>();
		mNavList = new ArrayList<Navigator>();
	}
	
	public ArrayList<Content> getContent() {
		return mContentList;
	}
	
	public ArrayList<Module> getModule() {
		return mModuleList;
	}
	
	public ArrayList<Catalog> getCatalog() {
		return mCatalogList;
	}
	
	public ArrayList<PlayLink2> getLink() {
		return mPlayLinkList;
	}
	
	public ArrayList<Navigator> getNav() {
		return mNavList;
	}
	
	public boolean live(int count, int type) {
		String url = String.format(live_url_fmt, count, type);
		System.out.println(url);
		
		HttpGet request = new HttpGet(url);
		
		boolean ret = false;
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
	        	System.out.println(String.format("title: %s, id: %s, nowplay: %s", title, vid, nowplay));
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
			
			JSONObject program = modules.getJSONObject(0).getJSONObject("data");
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
	
	public boolean list(String param, String type) {
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
		
		String url = String.format(list_url_prefix_fmt, 30);
		if (type != null && !type.isEmpty()) {
			url += "&";
			url += type;
		}
		
		url += "&";
		url += encoded_param;
		url += "&appid=com.pplive.androidphone&appplt=aph";
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
        
        return true;
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

					if (is_m3u8)
						url += rid.replaceFirst(".mp4", ".m3u8");
					else
						url += rid;
			        
					url += "?w=" + 1 + "&key=" + item.getK();
					url += "&k=" + item.getKey();
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
	
	public boolean search(String s_key, String s_type, String s_contenttype, String s_count) {
		String url = String.format(search_url_fmt, s_key, s_type, s_contenttype, s_count);
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
