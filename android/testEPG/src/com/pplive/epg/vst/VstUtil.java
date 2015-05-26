package com.pplive.epg.vst;

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

public class VstUtil {
	String tvlist_url = "http://live.91vst.com/tvlist";
	
	private List<ProgramVst> mProgramList;

	public VstUtil() {
		mProgramList = new ArrayList<ProgramVst>();
	}
	
	public List<ProgramVst> getProgramList() {
		return mProgramList;
	}
	
	public boolean program_list() {
		System.out.println("Java: program_list " + tvlist_url);
		
		HttpGet request = new HttpGet(tvlist_url);
		
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
			int tvnum = root.getInt("tvnum");
			int uptime = root.getInt("uptime");
			JSONArray types = root.getJSONArray("type");
			for (int i=0;i<types.length();i++) {
				JSONObject type = types.getJSONObject(i);
				String id = type.getString("id");
				String name = type.getString("name");
				System.out.println(String.format("Java: id %s, name %s", id, name));
			}
			
			mProgramList.clear();
			
			JSONArray programs = root.getJSONArray("live");
			for (int j=0;j<programs.length();j++) {
				JSONObject program = programs.getJSONObject(j);
				
				String prog_id = program.getString("id");
				String prog_name = program.getString("name");
				String prog_area = program.getString("area");
				String prog_quality = program.getString("quality");
				String itemid = program.getString("itemid");
				int pos = itemid.indexOf(",");
				String id = null;
				if (pos == -1)
					id = itemid;
				else
					id = itemid.substring(0, pos);
				
				String url_list = program.getString("urllist");
		        StringTokenizer st;
		        
		        /*
		         * Java: id 1, name 中央频道
		        Java: id 2, name 卫视频道
		        Java: id 9, name 乐视轮播
		        Java: id 3, name 体育竞技
		        Java: id 4, name 影视娱乐
		        Java: id 5, name 新闻资讯
		        Java: id 6, name 少儿教育
		        Java: id 11, name 财经频道
		        Java: id 7, name 省级频道
		        Java: id 10, name 高清频道
		        Java: id 12, name 剧场专区
		        Java: id 8, name 用户分享
		        */
		        
		        if (prog_area.equals("上海") || id.equals("1") || id.equals("2") || id.equals("4") || 
		        		id.equals("5") || id.equals("10")) {
			        List<String> urlList = new ArrayList<String>(); 
			        st = new StringTokenizer(url_list, "#", false);
					while (st.hasMoreElements()) {
						String url = st.nextToken();
						System.out.println("Java: play url: " + url);
						urlList.add(url);
					}
					
					ProgramVst prog = new ProgramVst(Integer.valueOf(prog_id), 
							prog_name, prog_area, prog_quality, urlList);
					mProgramList.add(prog);
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
}
