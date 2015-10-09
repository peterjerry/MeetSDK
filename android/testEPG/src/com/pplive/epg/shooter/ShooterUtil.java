package com.pplive.epg.shooter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ShooterUtil {
	private static final String token = "dGDOG8s6ClrYVVWhZRc8G5u8gemYsixF";
	
	private static final String search_url_fmt = "http://api.makedie.me" +
			"/v1/sub/search?token=%s&q=%s&cnt=%d&pos=0";
	
	private static final String detail_url_fmt = "http://api.makedie.me" +
			"/v1/sub/detail?token=%s&id=%d";
	
	public static List<SearchItem> search(String key, int count) {
		String encoded_key;
		try {
			encoded_key = URLEncoder.encode(key, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		
		String url = String.format(search_url_fmt, token, encoded_key, count);
		System.out.println("Java: shooter search() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to list(): code " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			//String result = EntityUtils.toString(response.getEntity());
			String result = new String(EntityUtils.toString(
					response.getEntity()).getBytes("ISO-8859-1"), "utf-8");
			System.out.println("Java: shooter search() result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			if (status != 0) {
				return null;
			}
			
			JSONObject sub = root.getJSONObject("sub");
			JSONArray subs = sub.getJSONArray("subs");
			
			List<SearchItem> retList = new ArrayList<SearchItem>();
			for (int i=0;i<subs.length();i++) {
//				"native_name": "老大哥（美版） 第17季第26集\/Big Brother US S17E26",
//	            "videoname": "big.brother.us.s17e26.720p.hdtv.x264-bajskorv",
//	            "revision": 0,
//	            "subtype": "VobSub",
//	            "upload_time": "2015-08-21 22:11:00",
//	            "vote_score": 0,
//	            "id": 594897,
//	            "release_site": "人人影视YYeTs",
//	            "lang": {
//	                "langlist": {
//	                    "langdou": true,
//	                    "langkor": true
//	                },
//	                "desc": "韩  双语"
//	            }
				JSONObject item = subs.getJSONObject(i);
				
				if (!item.has("id"))
					continue;
				
				String native_name = item.getString("native_name");
				String videoname = item.getString("videoname");
				int id = item.getInt("id");
				String lang_desc = "N/A";
				SearchItem subItem = null;
				if (item.has("lang")) {
					JSONObject lang = item.getJSONObject("lang");
					lang_desc = lang.getString("desc");
					subItem = new SearchItem(native_name, videoname, id, lang_desc);
				}
				else {
					subItem = new SearchItem(native_name, videoname, id);
				}
				
				retList.add(subItem);
				System.out.println(String.format("Java: native_name %s, videoname %s, id %d, lang_desc %s",
						native_name, videoname, id, lang_desc));
			}
			
			return retList;
		}
		catch (ClientProtocolException e) {
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
	
	public static DetailItem detail(int id) {
		String url = String.format(detail_url_fmt, token, id);
		System.out.println("Java: shooter detail() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to list(): code " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = new String(EntityUtils.toString(
					response.getEntity()).getBytes("ISO-8859-1"), "utf-8");
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int status = root.getInt("status");
			if (status != 0) {
				return null;
			}
			
			JSONObject sub = root.getJSONObject("sub");
			String sub_result = sub.getString("result");
			if (!sub_result.equals("succeed"))
				return null;
			
			JSONArray subs = sub.getJSONArray("subs");
			if (subs.length() == 0)
				return null;
			
			JSONObject item = subs.getJSONObject(0);
/*			 "subs": {
	            "filename": "洛东江大决战.Does the Nak-Dong River Flow.1976.DVD.X264.AAC.HALFCDi.rar",
	            "native_name": "洛东江大决战\/Commando on the Nakdong River\/Does the Nak-Dong River Flow\/洛東江大決戦",
	            "id": 602333,
	            "revision": 0,
	            "upload_time": "2015-07-03 23:28:53",
	            "url": "http:\/\/file0.makedie.me\/download\/602333\/洛东江大决战.Does the Nak-Dong River Flow.1976.DVD.X264.AAC.HALFCDi.rar?_=1440338154&-=f45377d1fe35b283b3c20f4be0d1259a",
	            "size": 20180,
	            "videoname": "낙동강은 흐르는가",
	            "filelist": [{
	                "s": "52KB",
	                "f": "洛东江大决战.Does the Nak-Dong River Flow.1976.DVD.X264.AAC.HALFCDi.srt"
	            }],
	            "subtype": "VobSub",
	            "title": "洛东江大决战\/Commando on the Nakdong River\/Does the Nak-Dong River Flow\/洛東江 大決戦\/洛东江大决战.Does the Nak-Dong River Flow.1976.DVD.X264.AAC.HALFCDi",
	            "vote_score": 0,
	            "view_count": 118,
	            "release_site": "个人",
	            "producer": {
	                "uploader": "谢里登大 道",
	                "verifier": "谢里登大道",
	                "source": "校订翻译",
	                "producer": "chenchun8219"
	            },
	            "lang": {
	                "langlist": {
	                    "langdou": true
	                },
	                "desc": "双语"
	            }
	            "down_count": 5
	        }*/
			String filename = item.getString("filename");
			String native_name = item.getString("native_name");
			int sub_id = item.getInt("id");
			String sub_url = item.getString("url");
			/*sub_url = sub_url.replace("file0.makedie.me", "sub.makedie.me");
			sub_url = sub_url.replace("file1.makedie.me", "sub.makedie.me");
			sub_url = sub_url.replace("file2.makedie.me", "sub.makedie.me");*/
			sub_url = sub_url.replaceAll("file\\d{1}.makedie.me", "sub.makedie.me");
			int pos = sub_url.lastIndexOf("?");
			if (pos != -1)
				sub_url = sub_url.substring(0, pos);
			
			// 39906 -> 039906
			int pos1 = sub_url.indexOf("/download/");
			int pos2 = sub_url.indexOf("/", pos1 + "/download/".length());
			String val = sub_url.substring(pos1 + "/download/".length(), pos2);
			if (val.length() < 6)
				sub_url = sub_url.replaceFirst(val, String.format("%06d", Integer.valueOf(val)));
			
			List<String> arvList = null;
			if (item.has("filelist")) {
				if (item.get("filelist") instanceof JSONArray) {
					arvList = new ArrayList<String>();
					JSONArray filelist = item.getJSONArray("filelist");
					for (int i=0;i<filelist.length();i++) {
						JSONObject subfile = filelist.getJSONObject(i);
						String arv_filename = subfile.getString("f");
						arvList.add(arv_filename);
						System.out.println("arv file: " + arv_filename);
					}
				}
			}
			System.out.println(String.format("detail() id %d, filename %s, native_name %s, url %s",
					sub_id, filename, native_name, sub_url));
			
			return new DetailItem(sub_id, filename, native_name, sub_url, arvList);
		}
		catch (ClientProtocolException e) {
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
