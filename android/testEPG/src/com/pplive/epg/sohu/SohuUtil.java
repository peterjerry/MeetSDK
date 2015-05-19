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
				double du = (double)clipsDuration_nor.get(k);
				sbNormal.append((int)(du * 1000));
				sbNormal.append(",");
				System.out.println(String.format("Java: segment #%d %.3f sec", k, du));
			}
			
			JSONArray clipsDuration_high = data.getJSONArray("clipsDuration_high");
			StringBuffer sbHigh = new StringBuffer();
			for (int k=0;k<clipsDuration_high.length();k++) {
				double du = (double)clipsDuration_high.get(k);
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
