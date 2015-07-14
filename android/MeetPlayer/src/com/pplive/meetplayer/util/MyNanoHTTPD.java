package com.pplive.meetplayer.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.pplive.common.pptv.CDNItem;
import com.pplive.common.pptv.EPGUtil;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class MyNanoHTTPD extends NanoHTTPD{  
	private final static String TAG = "MyNanoHTTPD";
	
	private final static int ONE_KILOBYTE = 1024;
	private final static int ONE_MAGABYTE = (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE = (ONE_MAGABYTE * ONE_KILOBYTE);
	
	private int mPort = 8080;
	private String mRootDir;
	private Context mContext;
	private MimeTypeMap  mMimeTypeMap;
	
	private EPGUtil mEPG;
	private CDNItem mLiveitem;
	private String mLastM3u8;
	private int m_first_seg = 1;
	private int mVid;
	private long start_time = -1;
	
	private String block_url_fmt = "http://%s/live/" +
			"%s/" + // rid 074094e6c24c4ebbb4bf6a82f4ceabda
			"%d.block?ft=1&platform=android3" +
			"&type=phone.android.vip&sdk=1" +
			"&channel=162" + 
			"&vvid=41" +
			"&k=%s"; 
	
    public MyNanoHTTPD(Context ctx, int port, String wwwroot) {
    	super(port);
 
    	mContext 	= ctx;
    	mPort		= port;
    	mRootDir 	= wwwroot;
    	if (mRootDir == null || mRootDir.isEmpty())
    		mRootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    	
    	mMimeTypeMap = MimeTypeMap.getSingleton();
    	
    	mEPG = new EPGUtil();
    }  
      
    public MyNanoHTTPD(Context ctx, String hostName,int port){  
        super(hostName, port);
        
        mContext = ctx;
    }  

	public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
		Log.i(TAG, "Java: Method: " + method.toString());
		if (NanoHTTPD.Method.GET.equals(method)) {
			// get方式
			String queryParams = session.getQueryParameterString();
			Log.i(TAG, "params: " + queryParams);
			
			long from = 0;
			long to = -1;
			
			String uri = session.getUri();
			Log.i(TAG, "Java: uri: " + uri);
			
			Map<String, String> headers = session.getHeaders();
			Set<String> keys = headers.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = it.next();
				String value = headers.get(key);
				
				if (key.equals("range")) {
					// Range: bytes=500-999
					int pos;
					pos = value.indexOf("-");
					from = Long.valueOf(value.substring(6, pos));
					String info = String.format("Java: range %s(%d - ", value, from);
					if (pos != value.length() - 1) {
						to = Long.valueOf(value.substring(pos + 1, value.length()));
						info += to;
					}
					info += ")";
					Log.i(TAG, info);
				}
				
				//Log.d(TAG, String.format("Java: http header key %s, value %s", key, value));
			}
			
			try {
				if (uri.equals("/favicon.ico") || //favicon.ico
						uri.contains("/back.gif") ||
						uri.contains("/blank.gif") ||
						uri.contains("/folder.gif") ||
						uri.contains("/unknown.gif")) {
					Log.i(TAG, "Java: load resource: " + uri);
					int pos = uri.lastIndexOf("/");
					InputStream is = mContext.getAssets().open(uri.substring(pos + 1, uri.length()));
					return serveLocalFile(is, uri, 0, is.available(), false);
				}
				else if (uri.contains("/play.m3u8") && 
						queryParams != null && queryParams.contains("type=pplive3")) {
					return serveM3u8(uri, queryParams);
				}
				else if (uri.endsWith(".ts")) {
					return serveSegment(uri);
				}
				else {
					String filepath = mRootDir + uri;
					
					File file = new File(filepath);
					if (file.isDirectory())
						return serveList(uri, filepath);
					
					try {
						FileInputStream is = new FileInputStream(file);
						return serveLocalFile(is, filepath, from, to, headers.containsKey("range"));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "failed to open file: " + uri);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "failed to IOException: " + e.getMessage());
			}
		} else if (NanoHTTPD.Method.POST.equals(method)) {
			// post方式
		}
		
         return super.serve(session);  
     }
	
	private myResponse serveLocalFile(InputStream is, String path, 
			long from, long to, boolean range) {
		Log.i(TAG, "Java: GET file: " + path);
		
		try {
			int len = is.available();
			
			if (to == -1)
				to = len;

			if (len > 0) {
				Log.i(TAG, "before get mime_type: " + path);
				String extension = getExtension(path);
				Log.i(TAG, "before extension: " + extension);
				String mimeType;
				if (extension.isEmpty())
					mimeType = "application/octet-stream";
				else if (mMimeTypeMap.hasExtension(extension))
					mimeType = mMimeTypeMap.getMimeTypeFromExtension(extension);
				else
					mimeType = "application/octet-stream";
			    if (mimeType.contains("text"))
					mimeType += ";charset=utf-8";
				Log.i(TAG, "mime_type: " + mimeType);
				
				if (from > 0L) {
					Log.i(TAG, "Java: skip " + from);
					is.skip(from);
				}
				
				return new myResponse(Status.OK, mimeType, is, 
						from, to, range);
			}
			else {
				Log.w(TAG, "cannot get file size");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new myResponse(Status.BAD_REQUEST, "", null, 0, 0, false);
	}
	
	private myResponse serveList(String uri, String path) {
		// list folder
		Log.i(TAG, "Java: list folder: " + path);
		
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("html");
		mimeType += ";charset=utf-8";
		
		StringBuffer sb_html_context = new StringBuffer();
		sb_html_context.append(getAssetFileContext("head.txt"));
		
		String line_fmt = "<tr><td valign=\"top\"><img src=\"%s\" " +
				"alt=\"[%s]\">" +
				"</td><td><a href=\"%s\">%s</a></td>" +
				"<td align=\"right\">%s  </td><td align=\"right\">%s</td>" +
						"<td>&nbsp;</td></tr>";
		String tail_fmt = "<tr><th colspan=\"5\"><hr></th></tr></table>" +
				"<address>%s (Android Phone %s) Server at %s Port %d" +
				"</address></body></html>";
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		
		File folder = new File(path);
		File[] files = folder.listFiles();
		
		Arrays.sort(files, new FileComparator());
		
		if (files != null) {
			for (File onefile : files) {
				if (onefile.isHidden())
					continue;
				
				String altType = "DIR";
				if (!onefile.isDirectory()) {
					String filename = onefile.getName();
					String fileMimeType = null;
					int pos = filename.lastIndexOf(".");
					if (pos > 0) {
						String extension = filename.substring(pos + 1, filename.length());
						if (mMimeTypeMap.hasExtension(extension))
							fileMimeType = mMimeTypeMap.getMimeTypeFromExtension(extension);
					}
					
					if (fileMimeType == null)
						fileMimeType = "application/octet-stream";
														
					pos = fileMimeType.indexOf("/");
					altType = fileMimeType.substring(0, pos).toUpperCase();
				}
				
				String fileName = onefile.getName();
				long filesize = onefile.length();
				long modTime = onefile.lastModified();
				
				String icon = "unknown.gif";
				if (onefile.isDirectory()) {
					icon = "folder.gif";
				}
				
				String href;
				if (uri.equals("/"))
					href = fileName;
				else
					href = uri + "/" + fileName;
				
				sb_html_context.append(String.format(line_fmt, 
						icon, altType, href, fileName,
						dateFormat.format(new Date(modTime)),
						onefile.isDirectory() ? "-" : getFileSize(filesize)));
			}
		}
		
		String str_tail = String.format(tail_fmt, 
				Build.DEVICE, Build.VERSION.RELEASE, Util.getIpAddr(mContext), mPort);
		sb_html_context.append(str_tail);
		//sb_html_context.append(getAssetFileContext("tail.txt"));

		String str_html_context = sb_html_context.toString();
		int len = str_html_context.length();
		
		InputStream html_is = new ByteArrayInputStream(str_html_context.getBytes(/*"UTF-8"*/));
		return new myResponse(Status.OK, mimeType, html_is, 0, len, false);
	}
	
	private myResponse serveM3u8(String uri, String params) {
		// http://127.0.0.1:9006/play.m3u8?type=pplive3&playlink=300151
		// %3Fft%3D1%26bwtype%3D0%26platform%3Dandroid3%26type%3Dphone.android.vip
		// %26begin_time%3D1436716800%26end_time%3D1436722200
		
		Log.i(TAG, "Java: serveM3u8() params: " + params);
		if (mLastM3u8 == null || !mLastM3u8.equals(uri + params)) {
			Log.i(TAG, "Java: reset m3u8 segment time");
			m_first_seg = 1;
		}
		mLastM3u8 = uri + params;
		
		String decoded_params = null;
		try {
			decoded_params = URLDecoder.decode(params, "UTF-8");
			Log.i(TAG, "Java: decoded_params: " + decoded_params);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, false);
		}
		
		boolean isLive = true;
		long begin_time = 0;
		long end_time = 0;
		
		StringTokenizer st = new StringTokenizer(decoded_params, "&", false);
		while (st.hasMoreElements()) {
			String param = st.nextToken();
			Log.i(TAG, "Java: param: " + param);
			int pos;
			pos = param.indexOf("=");
			String key, value;
			if (pos > 0) {
				key = param.substring(0, pos);
				value = param.substring(pos + 1);
			}
			else {
				key = param;
				value = "N/A";
			}
			
			Log.i(TAG, "Java: key: " + key + " , value: " + value);
			if (key.equals("begin_time")) {
				begin_time = Long.valueOf(value);
				isLive = false;
			}
			else if (key.equals("end_time")) {
				end_time = Long.valueOf(value);
				
				Log.i(TAG, String.format("Java: begin_time %d, end_time %d", begin_time, end_time));
			}
			else if (key.equals("playlink")) {
				pos = value.indexOf("?");
				if (pos > 0)
					value = value.substring(0, pos);
				mVid = Integer.valueOf(value);
				Log.i(TAG, "Java: vid " + mVid);
			}
		}
		
		mLiveitem = mEPG.live_cdn(mVid);// 300156
		if (mLiveitem == null) {
			Log.e(TAG, "Java: failed to get mLiveitem");
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, false);
		}
		
        StringBuffer sb_m3u8_context = new StringBuffer();
		sb_m3u8_context.append("#EXTM3U\n");
		sb_m3u8_context.append("#EXT-X-TARGETDURATION:5\n");
		sb_m3u8_context.append("#EXT-X-MEDIA-SEQUENCE:0\n");
        
		if (isLive) {
			if (start_time == -1) {
				start_time = System.currentTimeMillis();
			}
			
			long elapsed_time = System.currentTimeMillis() - start_time;
			int offset_sec = (int)(elapsed_time / 1000) % 5;
			
			String item_st = mLiveitem.getST();
	        begin_time = new Date(item_st).getTime() / 1000;
	        begin_time -= 45; // second
	        begin_time -= 1800; // 1800 sec
	        begin_time -= (begin_time % 5);
	        Log.i(TAG, "Java: live begin_time " + begin_time);
	        
	        int count = 360;
			for (int i=0;i<count;i++) {
				sb_m3u8_context.append("#EXTINF:5,\n");
				String filename = String.format("%d.ts", begin_time + offset_sec + i * 5);
				sb_m3u8_context.append(filename);
				sb_m3u8_context.append("\n");
			}
		}
		else {
			int count = (int)(end_time - begin_time) / 5;
			for (int i=0;i<count;i++) {
				sb_m3u8_context.append("#EXTINF:5,\n");
				String filename = String.format("%d.ts", begin_time + i * 5);
				sb_m3u8_context.append(filename);
				sb_m3u8_context.append("\n");
			}
			
			sb_m3u8_context.append("#EXT-X-ENDLIST\n\n");
		}
		
		String str_m3u8_context = sb_m3u8_context.toString();
		int len = sb_m3u8_context.length();
		
		InputStream m3u8_is = new ByteArrayInputStream(str_m3u8_context.getBytes());
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u8");
		return new myResponse(Status.OK, mimeType, m3u8_is, 0, len, false);
	}
	
	private myResponse serveSegment(String uri) {
		Log.i(TAG, "Java serveSegment: " + uri);
		
		int time_stamp = Integer.valueOf(uri.substring(1, uri.length() - 3));
		Log.i(TAG, "Java: time_stamp " + time_stamp);
		
		byte[] in_flv = new byte[1048576];
		
		String httpUrl = String.format(block_url_fmt, mLiveitem.getHost(), mLiveitem.getRid(),
				time_stamp, mLiveitem.getK());
		Log.i(TAG, "Java: download flv segment: " + httpUrl);
		int in_size = httpUtil.httpDownloadBuffer(httpUrl, in_flv);
		byte[] out_ts = new byte[1048576];
		
		int out_size = MyFormatConverter.Convert(in_flv, in_size, out_ts, m_first_seg);
		Log.i(TAG, "Java: out_size " + out_size);
		m_first_seg = 0;
		
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("ts");
		ByteArrayInputStream is = new ByteArrayInputStream(out_ts); 
		return new myResponse(Status.OK, mimeType, is, 
				0, out_size, false);
	}
	
	private String getAssetFileContext(String filename) {
		String line = null;
		
		InputStream is = null;
		StringBuilder sb = new StringBuilder();
		try {
			is = mContext.getAssets().open(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));   
			while ((line = reader.readLine()) != null) {
				sb.append(line/* + "/n"*/);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return sb.toString();
	}
	
	private class myResponse extends Response {

		protected myResponse(IStatus status, String mimeType, InputStream data,
				long from, long to, boolean addrange) {
			super(status, mimeType, data, to - from);
			
			if (addrange || from > 0)
				this.addHeader("Content-Range", String.format("bytes %d-", from));
		}
		
	}
	
	private static String getExtension(final String path) {
	    String suffix = "";
	    final int idx = path.lastIndexOf(".");
	    if (idx > 0) {
	        suffix = path.substring(idx + 1, path.length());
	    }
	    return suffix;
	}
	
	private String getFileSize(long size) {
	    String strSize;
	    if (size < 0)
	    	return "N/A";
	    
	    if (size > ONE_GIGABYTE)
			strSize = String.format("%.3f GB",
					(double) size / (double) ONE_GIGABYTE);
	    else if (size > ONE_MAGABYTE)
			strSize = String.format("%.3f MB",
					(double) size / (double) ONE_MAGABYTE);
		else if (size > ONE_KILOBYTE)
			strSize = String.format("%.3f kB",
					(double) size / (double) ONE_KILOBYTE);
		else
			strSize = String.format("%d Byte", size);
		return strSize;
    }
	
	private class FileComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			if (f1.isFile() && f2.isDirectory())
				return 1;
			if (f2.isFile() && f1.isDirectory())
				return -1;
				
			String s1=f1.getName().toString().toLowerCase();
			String s2=f2.getName().toString().toLowerCase();
			return s1.compareTo(s2);
	    }
	}
}
      