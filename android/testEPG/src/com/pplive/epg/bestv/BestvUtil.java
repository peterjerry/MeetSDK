package com.pplive.epg.bestv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class BestvUtil {

	private final static String login_url = "http://bestcast.bbtv.cn:9080/login";
	
	private final static String session_url = "http://wechat.bestv.com.cn/" +
			"backwatching/backwatching_list.jsp?" +
			"type=menu&code=001562803068c97a74af0882cd1ed57D&state=123";
	
	private final static String getkey_url = "http://wechat.bestv.com.cn/LiveDoorChain/getkey?view=json";
	
	// 2 卫视, 3 上海
	private final static String find_channel_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindOnlineSrcChannels?view=json&OnlineStatus=%d";
	
	private final static String playinfo_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindSchedulePlayInfo?view=json&" +
			"channelCode=%s&tableName=src_schedule";

	
	private final static String scheduleplayinfo_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindSchedulePlayInfo?" +
			"view=json&channelCode=Umai:CHAN/%d@BESTV.SMG.SMG&tableName=src_schedule";
	
	private List<BestvChannel> mChannelList;
	private String mJSessionid;
	
	public BestvUtil() {
		mChannelList = new ArrayList<BestvChannel>();
	}
	
	public List<BestvChannel> getChannelList() {
		return mChannelList;
	}
	
	public BestvPlayInfo playInfo(String channelCode) {
		String url = String.format(playinfo_url_fmt, channelCode);
		System.out.println("Java: playInfo() url: " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: playInfo() result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
 			JSONObject root = (JSONObject) jsonParser.nextValue();
 			String businessCode = root.getString("businessCode");
 			if (!businessCode.equals("success")) {
 				System.out.println("Java: businessCode is not success");
 				return null;
 			}
 			
 			String count = root.getString("count");
			int c = Integer.valueOf(count);
			System.out.println("Java: key count: " + c);
			JSONArray resultSetArray = root.getJSONArray("resultSet");
			
			if (resultSetArray.length() > 0) {
				JSONObject resultSet = resultSetArray.getJSONObject(0);
				
				String now = resultSet.getString("nowPlay");
				String will = resultSet.getString("toPlay");
				return new BestvPlayInfo(now, will);
			}
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
	
	public boolean channel(int type) {
		/*if (mJSessionid == null) {
			mJSessionid = getJSessionId();
			if (mJSessionid == null) {
				System.out.println("failed to get jsessionId");
				return false;
			}
		}*/
		
		String url = String.format(find_channel_url_fmt, type);
		System.out.println("Java: getChannel() url: " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: getChannel() result: " + result.substring(0, 256));
			
			JSONTokener jsonParser = new JSONTokener(result);
 			JSONObject root = (JSONObject) jsonParser.nextValue();
 			String businessCode = root.getString("businessCode");
 			if (!businessCode.equals("success")) {
 				System.out.println("Java: businessCode is not success");
 				return false;
 			}
 			
 			String count = root.getString("count");
			int c = Integer.valueOf(count);
			System.out.println("Java: key count: " + c);
			JSONArray resultSetArray = root.getJSONArray("resultSet");
			
			mChannelList.clear();
			
			for (int i=0;i<resultSetArray.length();i++) {
				JSONObject resultSet = resultSetArray.getJSONObject(i);
				
				String title = resultSet.getString("CallSign");
				String abbr = resultSet.getString("ChannelAbbr");
				String code = resultSet.getString("ChannelCode");
				String id = resultSet.getString("ChannelID");
				String number = resultSet.getString("ChannelNumber");
				String ImgUrl = resultSet.getString("ImgUrl"); // img/pic/s_dycj
				String PlayUrl = resultSet.getString("PlayUrl"); // http://wx.live.bestvcdn.com.cn/live/program/live991/weixindycj/index.m3u8?se=weixin&ct=2
			
				String img_url_prefix = "http://wechat.bestv.com.cn/backwatching/";
				String img_url_surfix= "_logo.gif";
				String img_url = img_url_prefix + ImgUrl + img_url_surfix;
				BestvChannel channel = new BestvChannel(title, abbr, code, id, number, img_url, PlayUrl);
				
				mChannelList.add(channel);
				System.out.println("Java: add channel " + channel.toString());
			}
			
			return true;
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
			
		return false;
		
	}
	
	private String getJSessionId() {
		HttpGet request = new HttpGet(session_url);

		HttpResponse response;
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			
			response = httpclient.execute(request);
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result.substring(0, 64));
			
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			List<Cookie> cookies = httpclient.getCookieStore().getCookies();
			for (int i = 0; i < cookies.size(); i++) {
				Cookie co = cookies.get(i);
		        System.out.println(String.format("Java cookie: %s %s", co.getName(), co.getValue())); 
		        if (co.getName().equals("JSESSIONID")) {
		        	return co.getValue();
		        }
		    }
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	private boolean login() {
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		
		HttpPost httpPost = new HttpPost(login_url); 
		
		String strJsession = "JSESSIONID=" + mJSessionid + "; ";
        System.out.println("JSESSIONID: " + strJsession);
        
        httpPost.setHeader("Accept-Language", "zh-CN");
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
        httpPost.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
        httpPost.setHeader("Referer", "http://wechat.bestv.com.cn/backwatching/backwatchingdetail.jsp?" +
				"type=0&ChannelAbbr=typd");
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; " +
				"Android 4.4.4; zh-cn; MI 3W Build/KTU84P) " +
				"AppleWebKit/533.1 (KHTML, like Gecko)Version/4.0 " +
				"MQQBrowser/5.4 TBS/025411 Mobile Safari/533.1 " +
				"MicroMessenger/6.1.0.66_r1062275.542 NetType/WIFI");
        httpPost.setHeader("origin", "http://wechat.bestv.com.cn");
        httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        httpPost.setHeader("Accept-Encoding", "gzip");
        httpPost.setHeader("Cookie", 
        		"Hm_lvt_1e35bd6ba34fe8a6a160ab272fc892db=1427119654,1429530935; " +
        		strJsession + 
        		"lzstat_ss=3807293619_1_1432143406_3593490; " +
        		"lzstat_uv=1282489934416223788|3593490");
        
        try { 
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpPost);
            // getEntity()  
            HttpEntity httpEntity = response.getEntity();  
            if (httpEntity != null) {  
            	// 打印响应内容
                String result = EntityUtils.toString(httpEntity, "UTF-8");
                System.out.println("Java: response: " + result);
                
                if (response.getStatusLine().getStatusCode() != 200) {
                	System.out.println("Java: failed to get live key, response is not 200");
                	return false;
                }
                JSONTokener jsonParser = new JSONTokener(result);
    			JSONObject root = (JSONObject) jsonParser.nextValue();
    			String businessCode = root.getString("businessCode");
    			if (!businessCode.equals("success")) {
    				System.out.println("Java: failed to login, businessCode: " + businessCode);
    			}
    			
    			
    			String count = root.getString("count");
    			int c = Integer.valueOf(count);
    			System.out.println("Java: count: " + c);
    			JSONArray resultSetArray = root.getJSONArray("resultSet");
    			
    			for (int i=0;i<resultSetArray.length();i++) {
    				JSONObject resultSet = resultSetArray.getJSONObject(i);
    				
    			}
            }
            
            // 释放资源  
            //closeableHttpClient.close();  
            return true;
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        
		return false;
	}
	
	public BestvKey getLiveKey() {
		BestvKey key = null;
		
        UrlEncodedFormEntity entity;
        HttpResponse response;
        HttpEntity httpEntity;
        String sessionId = null;
        String openId = null;
        String token = null;
        
        try {  
            DefaultHttpClient client = new DefaultHttpClient();
            
            // step1
            // 创建HttpClientBuilder  
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();  
            // HttpClient  
            CloseableHttpClient closeableHttpClient = httpClientBuilder.build(); 
            HttpHost target = new HttpHost(session_url, 80, "http");
            // 依次是代理地址，代理端口号，协议类型  
            HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");  
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();    
            
            // step1 get sessionId
            HttpGet httpGetSession = new HttpGet(session_url);
            httpGetSession.setConfig(config); 
            
            httpGetSession.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; " +
    				"Android 4.4.4; zh-cn; MI 3W Build/KTU84P) " +
    				"AppleWebKit/533.1 (KHTML, like Gecko)Version/4.0 " +
    				"MQQBrowser/5.4 TBS/025411 Mobile Safari/533.1 " +
    				"MicroMessenger/6.1.0.66_r1062275.542 NetType/WIFI");
            httpGetSession.setHeader("Accept",
            		"application/vnd.wap.xhtml+xml, " +
            		"text/vnd.wap.wml, application/xhtml+xml, " +
            		"text/html, image/png, image/jpeg, image/gif, */*;q=0.1");
            httpGetSession.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
            httpGetSession.setHeader("Accept-Encoding", "gzip");
            httpGetSession.setHeader("Accept-Language", "zh-CN");

            httpGetSession.setHeader("Cookie", 
            		"Hm_lvt_1e35bd6ba34fe8a6a160ab272fc892db=1427119654,1429530935; " +
            		"lzstat_uv=1282489934416223788|3593490");
            
            response = closeableHttpClient.execute(target, httpGetSession);
            httpEntity = response.getEntity();  
            if (httpEntity != null) {
                if (response.getStatusLine().getStatusCode() != 200){
                	System.out.println("Java: failed to get sessionId, response is not 200");
                	return null;
                }
                
                List<Cookie> cookies = client.getCookieStore().getCookies();
    			for (int i = 0; i < cookies.size(); i++) {
    				Cookie co = cookies.get(i);
    				System.out.println(String.format("Java cookie: name %s, value %s", co.getName(), co.getValue())); 
    		        
    		        if (co.getName().equals("JSESSIONID")) {
    		        	sessionId = co.getValue();
    		        	System.out.println("Java: get sessionid " + sessionId);
    		        }
    		        else if (co.getName().equals("openid")) {
    		        	openId = co.getValue();
    		        	System.out.println("Java: get openId " + openId);
    		        }
    		    }
    			
    			if (sessionId == null || openId == null) {
    				System.out.println("Java: failed to find sessionid and openId");
    				return null;
    			}
            }
            
            String strJsession = "JSESSIONID=" + sessionId + "; ";
            System.out.println("Java: JSESSIONID: " + strJsession);
            
            // step2 login
            HttpPost httpPostLogin = new HttpPost(login_url);    
            
            httpPostLogin.setHeader("Accept-Language", "zh-CN");
            httpPostLogin.setHeader("X-Requested-With", "XMLHttpRequest");
            httpPostLogin.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
            httpPostLogin.setHeader("Referer", "http://wechat.bestv.com.cn/backwatching/" +
            		"backwatchingdetail.jsp?type=0&ChannelAbbr=xwzh");
            httpPostLogin.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; " +
    				"Android 4.4.4; zh-cn; MI 3W Build/KTU84P) " +
    				"AppleWebKit/533.1 (KHTML, like Gecko)Version/4.0 " +
    				"MQQBrowser/5.4 TBS/025411 Mobile Safari/533.1 " +
    				"MicroMessenger/6.1.0.66_r1062275.542 NetType/WIFI");
            httpPostLogin.setHeader("origin", "http://wechat.bestv.com.cn");
            httpPostLogin.setHeader("Accept", "*/*");
            httpPostLogin.setHeader("Accept-Encoding", "gzip");
            
            // 创建参数队列  
            List<NameValuePair> LoginParams = new ArrayList<NameValuePair>();   
            LoginParams.add(new BasicNameValuePair("client_id", openId));
            LoginParams.add(new BasicNameValuePair("platform", "ZHG_WEIXIN_BESTV_0001"));
            LoginParams.add(new BasicNameValuePair("client_name", "2"));
            LoginParams.add(new BasicNameValuePair("password", "1"));
            
            entity = new UrlEncodedFormEntity(LoginParams, "UTF-8");  
            httpPostLogin.setEntity(entity);  
            
            response = client.execute(httpPostLogin);
            
            // getEntity()  
            httpEntity = response.getEntity();  
            if (httpEntity != null) {  
            	// 打印响应内容
                String result = EntityUtils.toString(httpEntity, "UTF-8");
                System.out.println("Java: step2 response: " + result);
                
                if (response.getStatusLine().getStatusCode() != 200){
                	System.out.println("Java: failed to login, response is not 200");
                	return null;
                }
                
                JSONTokener jsonParser = new JSONTokener(result);
    			JSONObject root = (JSONObject) jsonParser.nextValue();
    			token = root.getString("token");
    			System.out.println("Java: token " + token);
    			
            }
            
            // step3 get key
            // 请求地址  
            HttpPost httpPost = new HttpPost(getkey_url);    
            
            httpPost.setHeader("Accept-Language", "zh-CN");
            httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
            httpPost.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
            httpPost.setHeader("Referer", "http://wechat.bestv.com.cn/backwatching/backwatchingdetail.jsp?" +
    				"type=0&ChannelAbbr=typd");
            httpPost.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; " +
    				"Android 4.4.4; zh-cn; MI 3W Build/KTU84P) " +
    				"AppleWebKit/533.1 (KHTML, like Gecko)Version/4.0 " +
    				"MQQBrowser/5.4 TBS/025411 Mobile Safari/533.1 " +
    				"MicroMessenger/6.1.0.66_r1062275.542 NetType/WIFI");
            httpPost.setHeader("origin", "http://wechat.bestv.com.cn");
            httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpPost.setHeader("Accept-Encoding", "gzip");
            httpPost.setHeader("Cookie", 
            		"Hm_lvt_1e35bd6ba34fe8a6a160ab272fc892db=1427119654,1429530935; " +
            		strJsession +
            		///"JSESSIONID=1F9595304B9315090C48F7B4C2864AC2; " +
            		"lzstat_ss=3807293619_1_1432143406_3593490; " +
            		"lzstat_uv=1282489934416223788|3593490");
    		
            // 创建参数队列  
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();  
            // 参数名为platform_account，值是o47fhtx0JJ7Hocxaz0XRHTDdutCI  
            formparams.add(new BasicNameValuePair("platform_account", "o47fhtx0JJ7Hocxaz0XRHTDdutCI"));
            formparams.add(new BasicNameValuePair("userid", "379704"));
            formparams.add(new BasicNameValuePair("type", "0"));
            
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");  
            httpPost.setEntity(entity);  
            
    		response = client.execute(httpPost);
            
            // getEntity()  
            httpEntity = response.getEntity();  
            if (httpEntity != null) {  
            	// 打印响应内容
                String result = EntityUtils.toString(httpEntity, "UTF-8");
                System.out.println("Java: step3 response: " + result);
                
                if (response.getStatusLine().getStatusCode() != 200){
                	System.out.println("Java: failed to get live key, response is not 200");
                	return null;
                }
    			
                JSONTokener jsonParser = new JSONTokener(result);
    			JSONObject root = (JSONObject) jsonParser.nextValue();
    			String businessCode = root.getString("businessCode");
    			if (!businessCode.equals("success")) {
    				System.out.println("Java: failed to get live key, businessCode: " + businessCode);
    				return null;
    			}
    			
    			String count = root.getString("count");
    			int c = Integer.valueOf(count);
    			System.out.println("Java: key count: " + c);
    			JSONArray resultSetArray = root.getJSONArray("resultSet");
    			
    			for (int i=0;i<resultSetArray.length();i++) {
    				JSONObject resultSet = resultSetArray.getJSONObject(i);
    				// "expiration_time": 1432150607702,
    	            // "live_key": "3910F85C71F674CECF7742D8C3054FAE330FC5A3A62C519E127559318202683D",
    	            // "update_time": "2015-05-20 17:36:47",
    	            // "x_user_id": 379704
    				int expiration_time = 0;
    				if (resultSet.has("expiration_time"))
    					expiration_time = resultSet.getInt("expiration_time");
    				String live_key = resultSet.getString("live_key");
    				String update_time = "N/A";
    				if (resultSet.has("update_time"))
    					update_time = resultSet.getString("update_time");
    				int x_user_id = 0;
    				if (resultSet.has("x_user_id"))
    					x_user_id = resultSet.getInt("x_user_id");
    				
    				if (i==0) {
    					key = new BestvKey(expiration_time, live_key, update_time, x_user_id);
    				}
    			}
            }
            
            // 释放资源  
            //closeableHttpClient.close();  
            return key;
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        
		return key;  
	}
}
