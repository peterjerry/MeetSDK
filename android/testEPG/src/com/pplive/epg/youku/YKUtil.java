package com.pplive.epg.youku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.util.Base64Util;
import com.pplive.epg.util.CryptAES;

public class YKUtil {
	
	private final static String YOUKU_SECURITY_FMT = 
			"http://play.youku.com/play/get.json?vid=%s&ct=12";
	
	private final static String apiurl = 
			"http://i.play.api.3g.youku.com/common/v3/play" +
    		"?audiolang=1" +
    		"&guid=7066707c5bdc38af1621eaf94a6fe779" +
    		"&ouid=3151cdbf1449478fad97c27cd5fa755b2fff49fa" +
    		"&ctype=%s" + // param0
    		"&did=%s" + // param1
    		"&language=guoyu" +
    		"&local_point=0" +
    		"&brand=apple" +
    		"&vdid=924690F1-A141-446B-A5E5-4A5A778BB4F5" +
    		"&local_time=0" +
    		"&os=ios" +
    		"&point=1" +
    		"&os_ver=7.1.2" +
    		"&id=d1d065eafb1411e2a705" +
    		"&deviceid=0f607264fc6318a92b9e13c65db7cd3c" +
    		"&ver=3.9.4" +
    		"&format=1,3,6,7" +
    		"&network=WIFI" +
    		"&btype=iPad4,1" +
    		"&vid=%s" + // param2
    		"&pid=87c959fb273378eb" +
    		"&local_vid=%s"; // param3
	
	private final static String m3u8_url_fmt = 
			"http://pl.youku.com/playlist/m3u8" +
			"?ts=%s" + // useless?
			"&keyframe=1" +
			"&vid=%s" +
			"&type=hd2" +
			"&sid=%s" +
			"&token=%s" +
			"&oip=%s" +
			"&did=%s" + // useless?
			"&ctype=%s" +
			"&ev=1" +
			"&ep=%s";
	
	private final static String m3u8_url_fmt2 = 
			"http://pl.youku.com/playlist/m3u8" +
			"?ctype=12" +
			"&ep=%s" +
			"&ev=1" +
			"&keyframe=1" +
			"&oip=%s" +
			"&sid=%s" +
			"&token=%s" +
			"&type=%s" +
			"&vid=%s";
	
	public static void main(String[] args) { 
        
		String url = "http://v.youku.com/v_show/id_XMTQ5NDg3NjQzNg==.html?from=s1.8-1-1.1";
		
		String m3u8Url = getPlayUrl(url);
		if (m3u8Url == null)
			return;
		
		System.out.println("Java: toUrl " + m3u8Url);
		
        //String keyStr = "UITN25LMUQC436IM";  
 
        //String plainText = "this is a string will be AES_Encrypt";
	         
        //String encText = CryptAES.AES_Encrypt(keyStr, plainText);
        //String decString = CryptAES.AES_Decrypt(keyStr, encText); 
         
        //System.out.println(encText); 
        //System.out.println(decString);
	}
	
	public static String getPlayUrl(String youku_url) {
		System.out.println("Java: youku_url " + youku_url);
		
		String vid = getVid(youku_url);
		if (vid == null)
			return null;
		
		System.out.println("Java: vid " + vid);
		
		String ctype = "20";//指定播放ID 20
        String did = md5(ctype + "," + vid);
        long sec = System.currentTimeMillis() / 1000;
        String tm = String.valueOf(sec);
        
        String api_url = String.format(apiurl, ctype, did, vid, vid);
		System.out.println("Java: api_url " + api_url);
		
		HttpGet request = new HttpGet(api_url);
		
		HttpResponse response;
		try {
			request.setHeader("User-Agent", 
					"Youku HD;3.9.4;iPhone OS;7.1.2;iPad4,1");
			request.setHeader("Encoding", "gzip, deflate");
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			String data = root.getString("data");
			String dec_data = yk_jsondecode(data);
			//System.out.println("Java: dec_data " + dec_data);
			
			JSONTokener jsonDataParser = new JSONTokener(dec_data);
			JSONObject data_root = (JSONObject) jsonDataParser.nextValue();
			JSONObject sid_data = data_root.getJSONObject("sid_data");
			String sid = sid_data.getString("sid");
			String oip = sid_data.getString("oip");
			String token = sid_data.getString("token");
			
			String tmp = String.format("%s_%s_%s", sid, vid, token);
			String ep = getEP(tmp, ctype);
			
			return String.format(m3u8_url_fmt,
					tm, vid, sid, token, oip, did, ctype, ep);
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
	
	public static String getPlayUrl2(String youku_url) {
		String vid = getVid(youku_url);
		if (vid == null)
			return null;
		
		SecurityParam security = getSecurity(vid);
		if (security == null)
			return null;
		
		System.out.println("security.mEncryptString " + security.mEncryptString);
		EpParam ep = getEP2(vid, security.mEncryptString);
		
		String final_url = String.format(m3u8_url_fmt2, 
				ep.mEp, security.mIp, ep.mSid, ep.mToken, "hd2", vid);
		return final_url;
	}
	
	private static String getVid(String url) {
		String strRegex = "(?<=id_)(\\w+)";
		Pattern pattern = Pattern.compile(strRegex);
		Matcher match = pattern.matcher(url);
		boolean result = match.find();
		if (result)
			return url.substring(match.start(), match.end());
		
		return null;
	}
	
	private static String getEP(String sk, String ctype) {
        String pkey = "b45197d21f17bb8a"; //21
        if (ctype.equals("20"))
        	pkey = "9e3633aadde6bfec"; //20
        else if (ctype.equals("30"))
        	pkey = "197d21f1722361ac"; //30
        
		try {
			return URLEncoder.encode(
					CryptAES.AES_Encrypt(pkey, sk), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return null;
	}
	
	private static SecurityParam getSecurity(String vid) {
		String url = String.format(YOUKU_SECURITY_FMT, vid);
		System.out.println("Java: getSecurity " + url);
		
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
			JSONObject security = 
					root.getJSONObject("data").getJSONObject("security");
			
			String encrypt_string = security.getString("encrypt_string");
			long ip = security.getLong("ip");
			System.out.println("Java: encrypt_string " + encrypt_string + 
					", ip " + String.valueOf(ip));
			return new SecurityParam(encrypt_string, String.valueOf(ip));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static String myEncoder(String a, byte[] c, boolean isToBase64)
	{
		String result = "";
		byte []tmp = new byte[1024];
		int offset = 0;
		int f = 0, h = 0, q = 0;
		int[] b = new int[256];
		for (int i = 0; i < 256; i++)
			b[i] = i;
		
		while (h < 256) {
			f = (f + b[h] + a.charAt(h % a.length())) % 256;
			int temp = b[h];
			b[h] = b[f];
			b[f] = temp;
			h++;
		}
		
		f = 0;
		h = 0;
		q = 0;
		while (q < c.length)
		{
			h = (h + 1) % 256;
			f = (f + b[h]) % 256;
			int temp = b[h];
			b[h] = b[f];
			b[f] = temp;
			byte[] bytes = new byte[] { (byte)(c[q] ^ b[(b[h] + b[f]) % 256]) };
			tmp[offset++] = bytes[0];
			result += new String(bytes);
			q++;
		}
		
		if (isToBase64) {
			byte []data = new byte[offset];
			System.arraycopy(tmp, 0, data, 0, offset);
			result = Base64Util.encode(data);
		}
		
		return result;
	}
	
	private static EpParam getEP2(String vid, String ep) {
		String template1 = "becaf9be";
		String template2 = "bf7e5f01";
		
		byte []bytes = Base64Util.decode(ep);
		String temp = myEncoder(template1, bytes, false);
		System.out.println("temp " + temp);
		String[] part = temp.split("_");
		String sid = part[0];
		String token = part[1];
		String whole = String.format("%s_%s_%s", sid, vid, token);
		System.out.println("whole " + whole);
		String tmp = myEncoder(template2, whole.getBytes(), true);
		try {
			String ep_new = URLEncoder.encode(tmp, "UTF-8");
			System.out.println("ep_new " + ep_new);
			return new EpParam(ep_new, token, sid);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static class EpParam {
		public String mEp;
		public String mToken;
		public String mSid;
		
		public EpParam(String ep, String token, String sid) {
			mEp = ep;
			mToken = token;
			mSid = sid;
		}
	}
	
	private static class SecurityParam {
		/*security: {
			encrypt_string: "NgXVSAodJrvZ0PnF9+JxA4T3vBFu1wzKXhk=", // ep
			ip: 3031238944 // oip
		},*/
		
		public String mEncryptString;
		public String mIp;
		
		public SecurityParam(String encryptString, String ip) {
			mEncryptString = encryptString;
			mIp = ip;
		}
	}
	
	private static String yk_jsondecode(String json) {
		return CryptAES.AES_Decrypt("qwer3as2jin4fdsa", json);
    }
	
	private static String md5(String string) {

	    byte[] hash;

	    try {
	        hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
	    } catch (NoSuchAlgorithmException e) {
	        throw new RuntimeException("Huh, MD5 should be supported?", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new RuntimeException("Huh, UTF-8 should be supported?", e);
	    }

	    StringBuilder hex = new StringBuilder(hash.length * 2);
	    for (byte b : hash) {
	        if ((b & 0xFF) < 0x10)
	        	hex.append("0");

	        hex.append(Integer.toHexString(b & 0xFF));
	    }

	    return hex.toString();
	}
}
