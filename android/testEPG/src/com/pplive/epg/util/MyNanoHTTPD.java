package com.pplive.epg.util;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.pplive.epg.pptv.CDNItem;
import com.pplive.epg.pptv.EPGUtil;
import com.pplive.epg.pptv.NativeMedia;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class MyNanoHTTPD extends NanoHTTPD {
	private int mPort = 8080;
	private String mRootDir;
	
	private String mBDUSS;
	
	private EPGUtil mEPG;
	private List<CDNItem> mLiveItemList;
	private CDNItem mLiveItem;
	private String mLastM3u8;
	private int m_first_seg = 1;
	private int mVid;
	private int mFt = 1;
	private boolean mIsLive = true;
	private long start_time;
	
	private String block_url_fmt = "http://%s/live/" +
			"%s/" + // rid 074094e6c24c4ebbb4bf6a82f4ceabda
			"%d.block?ft=%d&platform=android3" +
			"&type=phone.android.vip&sdk=1" +
			"&channel=162" + 
			"&vvid=41" +
			"&k=%s"; 
	
    public MyNanoHTTPD(int port, String wwwroot) {
    	super(port);
 
    	mPort		= port;
    	mRootDir 	= wwwroot;
    	if (mRootDir == null || mRootDir.isEmpty())
    		mRootDir = "./";
    	
    	mEPG = new EPGUtil();
    }  
      
    public MyNanoHTTPD(String hostName, int port){  
        super(hostName, port);
    }  
    
    public void setBDUSS(String BDUSS) {
    	mBDUSS = BDUSS;
    }

	public Response serve(IHTTPSession session) {
		Method method = session.getMethod();
		System.out.println("Java: Method: " + method.toString());
		if (NanoHTTPD.Method.GET.equals(method)) {
			// get方式
			String queryParams = session.getQueryParameterString();
			System.out.println("Java: params: " + queryParams);
			
			long from	= -1; // not SET
			long to		= -1; // not SET
			
			String uri = session.getUri();
			System.out.println("Java: uri: " + uri);
			
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
					System.out.println(info);
				}
				
				//Log.d(TAG, String.format("Java: http header key %s, value %s", key, value));
			}
			
			if (uri.contains("rest/2.0/pcs")) {
				return servePCS(uri, queryParams, from, to);
			}
			else if (uri.contains("/play.m3u8") && 
					queryParams != null && queryParams.contains("type=pplive3")) {
				return serveM3u8(uri, queryParams, from, to);
			}
			else if (uri.endsWith(".ts") && 
					queryParams != null && queryParams.contains("type=pplive3")) {
				return serveSegment(uri, from, to);
			}
		} else if (NanoHTTPD.Method.POST.equals(method)) {
			// post方式
		}
		
         return super.serve(session);  
     }
	
	private myResponse servePCS(String uri, String params, long from, long to) {
		System.out.println(String.format("Java: servePCS() uri %s, params: %s, from %d, to %d",
				uri, params, from, to));
		
		String httpUrl = null;
		boolean bNewApi = false;
		if (params.contains("&new_api=1")) {
			httpUrl = "http://c.pcs.baidu.com" + uri + "?" + params;
			bNewApi = true;
		}
		else {
			httpUrl = "https://pcs.baidu.com" + uri + "?" + params;
		}
		
		System.out.println("Java: httpUrl " + httpUrl);
		
		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
		}

		try {
			URLConnection conn = url.openConnection();
			
			if (bNewApi) {
				if (mBDUSS == null) {
					System.out.println("BDUSS is not set!");
					return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
				}
				
				conn.setRequestProperty("Cookie", 
					"BDUSS=" + mBDUSS);
			}
			
			if (from != -1) {
				// Range: bytes=500-999
				String range = String.format("bytes=%d-", from);
				if (to != -1)
					range += String.valueOf(to);
				conn.setRequestProperty("RANGE", range);
			}
			
			long contentLength = Long.parseLong(conn.getHeaderField("Content-Length"));
			// conn.getContentLength() can only support less than 2G file size
			System.out.println(String.format("Java: conn type %s, len %s", 
					conn.getContentType(), contentLength));
			InputStream inStream = conn.getInputStream();
			
			IStatus stat = Status.OK;
			if (from != -1)
				stat = Status.PARTIAL_CONTENT;
			
			return new myResponse(stat, conn.getContentType(), 
					inStream, from, to, contentLength);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
	}
	
	private myResponse serveM3u8(String uri, String params, long from, long to) {
		// http://127.0.0.1:9006/play.m3u8?type=pplive3&playlink=300151
		// %3Fft%3D1%26bwtype%3D0%26platform%3Dandroid3%26type%3Dphone.android.vip
		// %26begin_time%3D1436716800%26end_time%3D1436722200
		
		System.out.println("Java: serveM3u8() params: " + params);
		if (mLastM3u8 == null || !mLastM3u8.equals(uri + params)) {
			System.out.println("Java: reset m3u8 segment time");
			m_first_seg = 1;
		}
		mLastM3u8 = uri + params;
		
		String decoded_params = null;
		try {
			decoded_params = URLDecoder.decode(params, "UTF-8");
			System.out.println("Java: decoded_params: " + decoded_params);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
		}
		
		long begin_time = 0;
		long end_time = 0;
		
		mIsLive = true;
		
		StringTokenizer st = new StringTokenizer(decoded_params, "&", false);
		int param_count = 0;
		while (st.hasMoreElements()) {
			String param = st.nextToken();
			System.out.println(String.format("Java: param #%d: %s",
					param_count, param));
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
			
			//System.out.println("Java: key: " + key + " , value: " + value);
			if (key.equals("begin_time")) {
				begin_time = Long.valueOf(value);
				mIsLive = false;
			}
			else if (key.equals("end_time")) {
				end_time = Long.valueOf(value);
				
				System.out.println(String.format("Java: begin_time %d, end_time %d", begin_time, end_time));
			}
			else if (key.equals("playlink")) {
				pos = value.indexOf("?");
				if (pos > 0)
					value = value.substring(0, pos);
				mVid = Integer.valueOf(value);
				System.out.println("Java: vid " + mVid);
			}
			else if (key.equals("ft")) {
				mFt = Integer.valueOf(value);
				System.out.println("Java: ft " + mFt);
			}
			
			param_count++;
		}
		
		mLiveItemList = mEPG.live_cdn(mVid);// 300156
		if (mLiveItemList == null || mLiveItemList.size() == 0) {
			System.out.println("Java: failed to get mLiveitem");
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
		}
		
		int size = mLiveItemList.size();
		mLiveItem = null;
		for(int i=0;i<size;i++) {
			CDNItem liveItem = mLiveItemList.get(i);
			if (Integer.valueOf(liveItem.getFT()) == mFt) {
				mLiveItem = liveItem;
				System.out.println("Java: found ft steam " + mFt);
				break;
			}
		}
		
		if (mLiveItem == null) {
			System.out.println("Java: failed to find ft stream " + mFt);
			return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
		}
		
        StringBuffer sb_m3u8_context = new StringBuffer();
		sb_m3u8_context.append("#EXTM3U\n");
		sb_m3u8_context.append("#EXT-X-TARGETDURATION:5\n");
        
		if (mIsLive) {
			String item_st = mLiveItem.getST();
	        begin_time = new Date(item_st).getTime() / 1000;
	        System.out.println("Java: live begin_time(origin) " + begin_time);
	        begin_time -= 45; // second live lag
	        begin_time -= (5 * (360 + 3)); // 1800 sec
	        begin_time -= (begin_time % 5);
	        System.out.println("Java: live begin_time(final) " + begin_time);
	        
	        process_live_seg(begin_time);

	        // must use 1,2,3... index
	        // cannot use segment time_stamp, otherwise will cause seek stuck
	        sb_m3u8_context.append(String.format("#EXT-X-MEDIA-SEQUENCE:%d\n", (begin_time - start_time) / 5));
			
	        int count = 360 + 3;
	        
			for (int i=0;i<count;i++) {
				sb_m3u8_context.append("#EXTINF:5,\n");
				String filename = String.format("%d.ts?type=pplive3", begin_time + i * 5);
				sb_m3u8_context.append(filename);
				sb_m3u8_context.append("\n");
			}
		}
		else {
			sb_m3u8_context.append("#EXT-X-MEDIA-SEQUENCE:1\n");
			
			int count = (int)(end_time - begin_time) / 5;
			for (int i=0;i<count;i++) {
				sb_m3u8_context.append("#EXTINF:5,\n");
				String filename = String.format("%d.ts?type=pplive3", begin_time + i * 5);
				sb_m3u8_context.append(filename);
				sb_m3u8_context.append("\n");
			}
			
			sb_m3u8_context.append("#EXT-X-ENDLIST\n\n");
		}
		
		String str_m3u8_context = sb_m3u8_context.toString();
		int len = sb_m3u8_context.length();

		InputStream m3u8_is = new ByteArrayInputStream(str_m3u8_context.getBytes());
		if (from > 0) {
			try {
				m3u8_is.skip(from);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new myResponse(Status.BAD_REQUEST, null, null, 0, 0, 0);
			}
			
			len -= from;
		}
		String mimeType = "application/vnd.apple.mpegurl";
		return new myResponse(Status.OK, mimeType, m3u8_is, from, to, len);
	}
	
	private void process_live_seg(long time_stamp) {
		if (m_first_seg == 0)
			return;
		
		System.out.println("Java: process_live_seg() " + time_stamp);
		
		byte[] in_flv = new byte[1048576];
		
		String httpUrl = String.format(block_url_fmt, mLiveItem.getHost(), mLiveItem.getRid(),
				time_stamp, mFt, mLiveItem.getKey());
		System.out.println("Java: download live first flv segment: " + httpUrl);
		int in_size = httpUtil.httpDownloadBuffer(httpUrl, 1400, in_flv);
		byte[] out_ts = new byte[1048576];
		
		int out_size = NativeMedia.Convert(in_flv, in_size, out_ts, 1, m_first_seg);
		System.out.println("Java: live first flv out_size " + out_size);
		
        start_time = time_stamp;
		m_first_seg = 0;
	}
	
	private myResponse serveSegment(String uri, long from, long to) {
		System.out.println("Java serveSegment: " + uri);
		
		int time_stamp = Integer.valueOf(uri.substring(1, uri.length() - 3));
		System.out.println("Java: time_stamp " + time_stamp);
		
		byte[] in_flv = new byte[1048576];
		
		String httpUrl = String.format(block_url_fmt, mLiveItem.getHost(), mLiveItem.getRid(),
				time_stamp, mFt, mLiveItem.getKey());
		System.out.println("Java: download flv segment: " + httpUrl);
		int in_size = httpUtil.httpDownloadBuffer(httpUrl, 1400, in_flv);
		byte[] out_ts = new byte[1048576];
		
		int out_size = NativeMedia.Convert(in_flv, in_size, out_ts, 1, m_first_seg);
		System.out.println("Java: out_size " + out_size);
		m_first_seg = 0;
		
		String mimeType = "video/MP2T";
		ByteArrayInputStream is = new ByteArrayInputStream(out_ts);
		
		IStatus stat = Status.OK;
		if (from > 0) {
			is.skip(from);
			out_size -= from;
			stat = Status.PARTIAL_CONTENT;
		}
		return new myResponse(stat, mimeType, is, 
				from, to, out_size);
	}
	
	private class myResponse extends Response {

		protected myResponse(IStatus status, String mimeType, InputStream data,
				long from, long to, long totalbytes) {
			super(status, mimeType, data, totalbytes);
			
			System.out.println(String.format("Java: myResponse() from %d, to %d, totalbytes %d",
					from, to, totalbytes));
			if (from != -1) {
				String strRange = String.format("bytes %d-", from);
				if (to != -1)
					strRange += String.valueOf(to);
				strRange += "/";
				strRange += String.valueOf(from + totalbytes);
				this.addHeader("Content-Range", strRange);
				System.out.println("Java: add header Content-Range: " + strRange);
			}
		}
		
	}
}
      
