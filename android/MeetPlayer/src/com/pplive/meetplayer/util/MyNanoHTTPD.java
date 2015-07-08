package com.pplive.meetplayer.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import android.content.Context;
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
	
	private String mRootDir;
	private Context mContext;
	private MimeTypeMap  mMimeTypeMap;
	
    public MyNanoHTTPD(Context ctx, int port, String wwwroot) {
    	super(port);
 
    	mContext = ctx;
    	mRootDir = wwwroot;
    	if (mRootDir == null || mRootDir.isEmpty())
    		mRootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    	
    	mMimeTypeMap = MimeTypeMap.getSingleton();
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
			
			InputStream is = null;
			long len = 0;
			String uri = session.getUri();
			Log.i(TAG, "Java: uri: " + uri);
			String filepath = mRootDir + uri;
			Log.i(TAG, "Java: GET file: " + filepath);
			
			try {
				if (uri.equals("/favicon.ico") || //favicon.ico
						uri.contains("/back.gif") ||
						uri.contains("/blank.gif") ||
						uri.contains("/folder.gif") ||
						uri.contains("/unknown.gif")) {
					Log.i(TAG, "Java: load resource: " + uri);
					int pos = uri.lastIndexOf("/");
					is = mContext.getAssets().open(uri.substring(pos + 1, uri.length()));
				}
				else {
					File file = new File(filepath);
					if (file.isDirectory()) {
						// list folder
						Log.i(TAG, "Java: list folder " + file.getAbsolutePath());
						
						String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("html");
						mimeType += ";charset=utf-8";
						
						StringBuffer sb_html_context = new StringBuffer();
						sb_html_context.append(getAssetFileContext("head.txt"));
						
						String line_fmt = "<tr><td valign=\"top\"><img src=\"%s\" " +
								"alt=\"[%s]\">" +
								"</td><td><a href=\"%s\">%s</a></td>" +
								"<td align=\"right\">%s  </td><td align=\"right\">%s</td>" +
										"<td>&nbsp;</td></tr>";
						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss");
						
						File folder = new File(filepath);
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
						
						sb_html_context.append(getAssetFileContext("tail.txt"));

						String str_html_context = sb_html_context.toString();
						len = str_html_context.length();
						
						InputStream html_is = new ByteArrayInputStream(str_html_context.getBytes(/*"UTF-8"*/));
						return new myResponse(Status.OK, mimeType, html_is, len);
					}
					else { // is a file
						is = new FileInputStream(file);
					}
				}
				
				len = is.available();

				if (len > 0) {
					Log.i(TAG, "before get mime_type: " + filepath);
					String extension = getExtension(filepath);
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
					
					return new myResponse(Status.OK, mimeType, is, len);
				}
				else {
					Log.w(TAG, "cannot get file size");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "failed to open file: " + filepath);
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
				long totalBytes) {
			super(status, mimeType, data, totalBytes);
			// TODO Auto-generated constructor stub
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
	
	class FileComparator implements Comparator<File> {
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
      