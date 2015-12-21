package com.pplive.epg.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class MyNanoHTTPD extends NanoHTTPD {
	private int mPort = 8080;
	private String mRootDir;
	
	private String mBDUSS;
	
    public MyNanoHTTPD(int port, String wwwroot) {
    	super(port);
 
    	mPort		= port;
    	mRootDir 	= wwwroot;
    	if (mRootDir == null || mRootDir.isEmpty())
    		mRootDir = "./";
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
      
