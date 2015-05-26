package com.pplive.epg.letv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipException;

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

import com.pplive.epg.util.ZipUtil;
import com.pplive.epg.util.httpUtil;

public class LetvUtil {
	String iploop_url = "http://int.dpool.sina.com.cn/iplookup/iplookup.php?format=json";
	
	String play_list_url_fmt = "http://st.live.letv.com/live/playlist/%s.json"; // lb_1080P
	
	String context_json_url = "http://121.201.14.53/apk_api/itv_json_v5.zip";
	
	String live_url_fmt = "http://live.gslb.letv.com/gslb?stream_id=%s&tag=live" +
			"&ext=m3u8&sign=live_tv&platid=10&splatid=1002&tm=10&format=2&expect=3&must=%s";
	
	String recommend_url_fmt = "http://g3com.cp21.ott.cibntv.net/r?format=2&stream_id=%s";
	
	private String mJsonContext;
	private List<Programlb> mProgramList;
	private List<ProgramItemlb> mProgramItemList;
	private List<PlayLinkLb> mPlaylinkList;
	private List<StreamIdLb> mStreamIdList;

	public LetvUtil() {
		update_json();
		
		mProgramList = new ArrayList<Programlb>();
		mProgramItemList = new ArrayList<ProgramItemlb>();
		mPlaylinkList = new ArrayList<PlayLinkLb>();
		mStreamIdList = new ArrayList<StreamIdLb>();
	}
	
	public List<PlayLinkLb> getPlaylinkList() {
		return mPlaylinkList;
	}
	
	public List<ProgramItemlb> getProgramItemList() {
		return mProgramItemList;
	}
	
	public List<Programlb> getProgramList() {
		return mProgramList;
	}
	
	public List<StreamIdLb> getStreamIdList() {
		return mStreamIdList;
	}
	
	boolean load_json(String path) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(path));
			String data = "";
			StringBuffer sb = new StringBuffer();
			while ((data = br.readLine()) != null) {
				sb.append(data);
			}
			
			mJsonContext = sb.toString();
			br.close();
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean update_json() {
		String zip_save_path = "itv_json_v5.zip";
		String json_save_path = "itv_json_v5.php";
		
		File json_file = new File(json_save_path);
		if (json_file.exists()) {
			System.out.println("Java: json file already exists!");
			return load_json(json_save_path);
		}
		
		boolean ret = httpUtil.httpDownload(context_json_url, "itv_json_v5.zip");
		if (ret == false) {
			System.out.println("Java: failed to download json");
			return false;
		}
		
		File file = new File(zip_save_path);
		try {
			if (ZipUtil.upZipFile(file, json_save_path) != 0) {
				System.out.println("Java: failed to unzip json");
				return false;
			}
			
			return load_json(json_save_path);
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean play_list(String epg_id) {
		String url;
		url = String.format(play_list_url_fmt, epg_id);
		System.out.println("Java: play_list " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			};
			
			String result = new String(EntityUtils.toString(
					response.getEntity()).getBytes("ISO-8859-1"), "utf-8");
			System.out.println("Java: result: " + result.substring(0, 256));
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			String channel_name = root.getString("channelname");
			String date = root.getString("date");
			
			Iterator<String> keyIter = root.keys();
			String key;
			mStreamIdList.clear();
			while (keyIter.hasNext()) {
				key = (String)keyIter.next();
				if (key.contains("live_url")) {
					String strm_url = root.getString(key);
					int begin = strm_url.indexOf("?stream_id=");
					int end = strm_url.lastIndexOf("&");
					String stream_id = strm_url.substring(begin + "?stream_id=".length(), end);
					StreamIdLb strm = new StreamIdLb(stream_id, strm_url);
					mStreamIdList.add(strm);
					System.out.println(String.format("Java: stream_id %s, strm_url %s", 
							stream_id, strm_url));
				}
			}
			
			System.out.println(String.format("Java: channel_name %s, date %s", 
					channel_name, date));
			
			mProgramItemList.clear();
			JSONArray contents = root.getJSONArray("content");
			for (int i=0;i<contents.length();i++) {
				JSONObject prog = contents.getJSONObject(i);
				String id = prog.getString("id");
				String playtime = prog.getString("seektime");
				String title = prog.getString("title");
				String viewpic = prog.getString("viewPic");
				
				ProgramItemlb progItem = new ProgramItemlb(Integer.valueOf(id), title, playtime, viewpic);
				mProgramItemList.add(progItem);
				System.out.println("Java: prog " + progItem.toString());
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
	
	public boolean context() {
		
		try {
			JSONTokener jsonParser = new JSONTokener(mJsonContext);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int tvnum = root.getInt("tvnum");
			System.out.println("Java: tvnum: " + tvnum);
			JSONArray typelist = root.getJSONArray("type");
			for (int k=0;k<typelist.length();k++) {
				JSONObject type = typelist.getJSONObject(k);
				int type_id = type.getInt("id");
				String type_name = type.getString("name");
				System.out.println(String.format("#%d %s", type_id, type_name));
			}
			
			JSONArray live = root.getJSONArray("live");
			
			mProgramList.clear();
			
			for (int i=0;i<live.length();i++) {
				JSONObject program = live.getJSONObject(i);
				String id = program.getString("id");
				String name = program.getString("name");
				String area = program.getString("area");
				String epgid = program.getString("epgid");
				if (epgid.contains("letv-lb_")) {
					epgid = epgid.substring(5); // lb_xxxx
					String stream_id;
					if (epgid.contains("1080P"))
						stream_id = epgid + "_1080p3m";
					else
						stream_id = epgid + "_1300";
					
					Programlb prog = new Programlb(Integer.valueOf(id), name, area, epgid, stream_id);
					mProgramList.add(prog);
					System.out.println("Java: Programlb " + prog.toString());
					
				}
			}
			
			return true;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String recommend(String stream_id) {
		String url;
		url = String.format(recommend_url_fmt, stream_id);
		System.out.println("Java: recommend " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));

			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        String desc = root.getChildText("desc");
	        String perfect = root.getChildText("perfect");
	       
	        System.out.println("Java: desc: " + desc + " , perfect " + perfect);
	        
	        StringTokenizer st;
	        
	        st = new StringTokenizer(perfect, ",", false);
			if (st.hasMoreElements()) { // just get the first one
				String best_id = st.nextToken();
				System.out.println("Java: best_id: " + best_id);
				return best_id;
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
		
		return null;
	}
	
	public boolean live(String stream_id, String must) {
		String live_url = String.format(live_url_fmt, stream_id, must);
		System.out.println("Java: live " + live_url);
		
		HttpGet request = new HttpGet(live_url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));

			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);  
	        Document doc = builder.build(returnQuote);
	        Element root = doc.getRootElement();
	        
	        mPlaylinkList.clear();
	        List<Element> nodelist = root.getChild("nodelist").getChildren("node");
	        for (int i=0;i<nodelist.size();i++) {
	        	String name = nodelist.get(i).getAttributeValue("name");
	        	String play_link = nodelist.get(i).getText();
	        	System.out.println(String.format("Java: node#%d name: %s, playlink: %s",
	        			i, name, play_link));
	        	
	        	PlayLinkLb link = new PlayLinkLb(name, play_link);
	        	mPlaylinkList.add(link);
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
}
