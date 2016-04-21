package com.gotye.crashhandler;

import com.gotye.common.util.LogUtil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class UploadUtil {

	private static final String TAG = "UploadUtil";

	private static final int TIME_OUT = 10 * 1000; // 10 sec
	private static final String CHARSET = "utf-8";

	public static String uploadFile(File file, String requestUrl){
		String result = null;
		String  BOUNDARY =  UUID.randomUUID().toString();  //边界标识   随机生成
		String PREFIX = "--" ;
		String LINE_END = "\r\n";
		String CONTENT_TYPE = "multipart/form-data";   //内容类型
		try{
			URL url = new URL(requestUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(TIME_OUT);
			conn.setConnectTimeout(TIME_OUT);
			conn.setDoInput(true);  //允许输入流
			conn.setDoOutput(true); //允许输出流
			conn.setUseCaches(false);  //不允许使用缓存
			conn.setRequestMethod("POST");  //请求方式
			conn.setRequestProperty("Charset", CHARSET);  //设置编码
			conn.setRequestProperty("connection", "keep-alive");
			conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);

			if (file == null)
				return null;

			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			StringBuffer sb = new StringBuffer();
			sb.append(PREFIX);
			sb.append(BOUNDARY);
			sb.append(LINE_END);
			/**
			 * 这里重点注意：
			 * name里面的值为服务器端需要key   只有这个key 才可以得到对应的文件
			 * filename是文件的名字，包含后缀名的   比如:abc.png
			 */

			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String str_time = format.format((new Date()));
			String log_filename = String.format("%s_%s_%s_%s",
					AppInfo.PHONE_MODEL, AppInfo.APP_PACKAGE, str_time, file.getName());

			// name=file is hardcode in upload.php
			sb.append("Content-Disposition: form-data; name=\"file\"; filename=\""
					+ log_filename + "\"" + LINE_END);
			sb.append("Content-Type: application/octet-stream; charset="+CHARSET+LINE_END);
			sb.append(LINE_END);
			dos.write(sb.toString().getBytes());
			InputStream is = new FileInputStream(file);
			byte[] bytes = new byte[4096];
			int len = 0;
			while ((len = is.read(bytes))!=-1) {
				dos.write(bytes, 0, len);
				//Log.d(TAG, "Java: write " + len);
			}
			is.close();
			dos.write(LINE_END.getBytes());
			byte[] end_data = (PREFIX+BOUNDARY+PREFIX+LINE_END).getBytes();
			dos.write(end_data);
			dos.flush();

			/**
			 * 获取响应码  200=成功
			 * 当响应成功，获取响应的流
			 */
			int res = conn.getResponseCode();
			LogUtil.info(TAG, "Java: response code: " + res);
			if (res != 200) {
				return null;
			}

			LogUtil.info(TAG, "Java: request success");
			InputStream input =  conn.getInputStream();
			StringBuffer sb1= new StringBuffer();
			int ss ;
			while ((ss=input.read())!=-1) {
				sb1.append((char)ss);
			}
			result = sb1.toString();
			LogUtil.debug(TAG, "result: " + result);

			return log_filename;
		}catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
