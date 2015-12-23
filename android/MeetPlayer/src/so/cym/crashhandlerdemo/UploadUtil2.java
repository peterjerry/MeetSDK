package so.cym.crashhandlerdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class UploadUtil2 {

	private static final String TAG = "UploadUtil2";
	
	private static final int TIME_OUT = 10 * 1000; // 10 sec
	private static final String CHARSET = "UTF-8";

	public static String uploadFile(File file, String desc, String requestUrl){
		try {
			BufferedReader bf = new BufferedReader(new FileReader(file));
			StringBuffer sb = new StringBuffer();
			String content = null;
			while(true) {
				content = bf.readLine();
				if (content == null)
					break;
				
				sb.append(content.trim());
				sb.append("\n");
			}
			
			String stacktrace = sb.toString();
			//Log.d(TAG, "stacktrace: " + stacktrace);
			bf.close();
			
			DefaultHttpClient client = new DefaultHttpClient();
			// 请求超时
            client.getParams().setParameter(
            		CoreConnectionPNames.CONNECTION_TIMEOUT, TIME_OUT);
            // 读取超时
            client.getParams().setParameter(
            		CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
            
			HttpPost localHttpPost = new HttpPost(requestUrl);
	        List<BasicNameValuePair> localArrayList = new ArrayList<BasicNameValuePair>();
	        localArrayList.add(new BasicNameValuePair("package_name", AppInfo.APP_PACKAGE));
	        localArrayList.add(new BasicNameValuePair("package_version", AppInfo.APP_VERSION));
	        localArrayList.add(new BasicNameValuePair("phone_model", AppInfo.PHONE_MODEL));
	        localArrayList.add(new BasicNameValuePair("android_version", AppInfo.ANDROID_VERSION));
	        localArrayList.add(new BasicNameValuePair("stacktrace", stacktrace));
	        localArrayList.add(new BasicNameValuePair("description", desc));
	        
	        localHttpPost.setEntity(
					new UrlEncodedFormEntity(localArrayList, CHARSET));
	        HttpResponse response = client.execute(localHttpPost);
	        int code = response.getStatusLine().getStatusCode();
	        if (code != 200) {
	        	Log.e(TAG, "failed to post crash log: " + code);
	        	return null;
	        }
	        
	        
        	String result = EntityUtils.toString(response.getEntity());
        	Log.i(TAG, "post result: " + result);
        	JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			return root.getString("log_filename");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
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
