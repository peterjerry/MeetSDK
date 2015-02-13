package com.pplive.meetplayer.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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
	
	private final static String frontpage_url = "http://mtbu.api.pptv.com/v4/module?lang=zh_cn&platform=aphone"
			+ "&appid=com.pplive.androidphone&appver=4.1.3"
			+ "&appplt=aph&userLevel=0";
	
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
	
	private ArrayList<Catalog> mCatalogList;
	private ArrayList<PlayLink2> mPlayLinkList;
	private ArrayList<Navigator> mNavList;
	
	public EPGUtil() {
		mCatalogList = new ArrayList<Catalog>();
		mPlayLinkList = new ArrayList<PlayLink2>();
		mNavList = new ArrayList<Navigator>();
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
	
	public boolean frontpage(int catalog_index) {
		Log.i(TAG, "frontpage: " + frontpage_url);
		
		HttpGet request = new HttpGet(frontpage_url);
		
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
			
			mCatalogList.clear();
			
			if (-1 == catalog_index) {
				for (int i=0;i<modules.length();i++) {
					JSONObject programs = modules.getJSONObject(i);
					JSONObject data = programs.getJSONObject("data");
					if (!data.isNull("title")) {
						Catalog c = new Catalog(i, programs.getString("id"), data.getString("title"));
						mCatalogList.add(c);
					}
				}
			}
			else {
				JSONObject programs = modules.getJSONObject(catalog_index);
				JSONObject data = programs.getJSONObject("data");
				JSONArray data_array = data.getJSONArray("dlist");
				
				for (int i=0;i<data_array.length();i++){  
	                JSONObject cell = data_array.getJSONObject(i);
	                Catalog c = new Catalog(cell.getString("title"), cell.getString("link"));
					mCatalogList.add(c);
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
	
	public boolean selection(String vid) {
		String url = String.format(detail_url_fmt, vid, vid);
		System.out.println(url);
		
		boolean ret = false;
		
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
    	String link_duration = v.getChild("duration").getText();
    	String link_duration_sec = v.getChild("durationSecond").getText();
    	int duration_sec = Integer.valueOf(link_duration_sec);
    	String link_resolution = v.getChild("resolution").getText();
    	
    	String link_content = v.getChild("content").getText();
    	
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
        	Element source = playlink2.getChild("source");
	    	String src_mark = source.getAttributeValue("mark");
	    	String src_res = source.getAttributeValue("resolution");
	    	if(src_res != null && !src_res.isEmpty())
	    		link_resolution = src_res; // overwrite
	    	PlayLink2 l = new PlayLink2(link_title, link_id, link_content, 
	    			src_mark, link_director, link_act,
	    			link_year, link_area,
	    			link_resolution, duration_sec);
	    	mPlayLinkList.add(l);
        }
        
        return true;
	}
	
	public boolean search(String s_key, String s_type, String s_contenttype, String s_count) {
		String url = String.format(search_url_fmt, s_key, s_type, s_contenttype, s_count);
		System.out.println(url);

		boolean ret = false;
		
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
