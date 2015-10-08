package com.pplive.epg.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
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

public class LrcDownloadUtil {
	private static String API_GECIME_LYRIC_URL = "http://geci.me/api/lyric/";
	private static String API_GECIME_LYRIC_ATRIST = "http://geci.me/api/artist/";
	private static String API_BAIDU_LYRIC_URL = "http://box.zhangmen.baidu.com" +
			"/x?op=12" +
			"&count=1" +
			"&title="; //%B2%BB%B5%C3%B2%BB%B0%AE$$%C5%CB%E7%E2%B0%D8$$$$";
	private static String BAIDU_LRC_URL_FMT = "http://box.zhangmen.baidu.com/bdlrc/%d/%d.lrc";
	
	public static List<LrcData> getLyc(String song_name) {
		return getLyc(song_name, null);
	}
	
	public static List<LrcData> getLyc(String song_name, String artist) {
		System.out.println(String.format("Java: getLyc(): song %s, artist %s", song_name, artist));
		
		String encoded_str;
		try {
			encoded_str = URLEncoder.encode(song_name, "UTF-8");
			String url = API_GECIME_LYRIC_URL + encoded_str;
			
			if (artist != null) {
				encoded_str = URLEncoder.encode(artist, "UTF-8");
				url += "/";
				url += encoded_str;
			}
			
			System.out.println("Java: getLyc(): " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: return status is not 200 " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int count = root.getInt("count");
			JSONArray lyrics = root.getJSONArray("result");
			int num = lyrics.length();
			
			List<LrcData> lrcList = new ArrayList<LrcData>();
			
			for (int i=0;i<num;i++) {
				JSONObject lyc = lyrics.getJSONObject(i);
				String lrc_path = lyc.getString("lrc");
				String lrc_song = lyc.getString("song");
				int artist_id = lyc.getInt("artist_id");
				int sid = lyc.getInt("sid");
				
				LrcData lrc = new LrcData(artist_id, "", sid, 0, lrc_song, lrc_path);
				lrcList.add(lrc);
				
				System.out.println(String.format("Java: get lrc_path: %s, lrc_song: %s, artist_id: %d, sid %d",
						lrc_path, lrc_song, artist_id, sid));
			}
			
			return lrcList;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
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
	
	public static String getBaiduLyc(String song_name, String artist) {
		System.out.println(String.format("Java: getBaiduLyc(): song %s, artist %s", song_name, artist));
		
		String encoded_str;
		try {
			encoded_str = URLEncoder.encode(song_name, "GB2312");
			String url = API_BAIDU_LYRIC_URL + encoded_str;
			
			if (artist != null) {
				encoded_str = URLEncoder.encode(artist, "GB2312");
				url += "$$";
				url += encoded_str;
				url += "$$$$";
			}
			
			System.out.println("Java: getLyc(): " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: return status is not 200 " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = new String(EntityUtils.toString(
					response.getEntity()).getBytes("gb2312"), "utf-8");
			System.out.println("Java: result: " + result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);
	        Document doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			String count = root.getChildText("count");
			int c = Integer.valueOf(count);
			if (c == 0) {
				System.out.println("Java: lrc count is zero");
				return null;
			}
			
			List<Element> xml_url_list = root.getChildren("url");
			if (xml_url_list == null || xml_url_list.size() == 0) {
				System.out.println("Java: failed to get xml_url_list");
				return null;
			}
			
			Element first_item = xml_url_list.get(0);
			String lrcid = first_item.getChildText("lrcid");
			int id = Integer.valueOf(lrcid);
			if (id ==0) {
				System.out.println("Java: error lrcid is 0");
				return null;
			}
			
			String lrc_url = String.format(BAIDU_LRC_URL_FMT, id / 100, id);
			System.out.println("Java: get lrc_url " + lrc_url);
			return lrc_url;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
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
	
	public static String getArtist(int artist_id) {
		try {
			String url = API_GECIME_LYRIC_ATRIST + String.valueOf(artist_id);
			System.out.println("Java: getArtist() " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200) {
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int count = root.getInt("count");
			int code = root.getInt("code");
			if (code != 0) {
				System.out.println("Java: code is wrong " + code);
				return null;
			}
			
			JSONObject artist = root.getJSONObject("result");
			return artist.getString("name");
		} catch (MalformedURLException e) {
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
