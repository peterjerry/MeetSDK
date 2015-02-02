package com.pplive.meetplayer.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.TrackInfo;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.pplive.meetplayer.R;

public class ListMediaUtil {

	private final static String TAG = "ListMediaUtil";
	
	private final static int ONE_MAGABYTE = 1048576;
	private final static int ONE_KILOBYTE = 1024;
	
	private List<Map<String, Object>> mClipList = null;
	private Context mContext;
	private String mUrl;
	
	public List<Map<String, Object>> getList() {
		return mClipList;
	}
	
	public ListMediaUtil(Context ctx) {
		mContext = ctx;
		mClipList = new ArrayList<Map<String, Object>>();
	}

	private String GetFileExt(String path) {
		/*File f = new File(path);  
		String fileName = f.getName(); 
		String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
		return prefix;*/
		
		String file_ext;
		int pos;
		pos = path.lastIndexOf(".");
		if (pos == -1) {
			file_ext = "N/A";
		}
		else {
			file_ext = path.substring(pos + 1);
		}
		
		return file_ext;
	}
	
	private String GetFileName(String path) {
		int pos1, pos2;
		pos1 = path.lastIndexOf('/');
		pos2 = path.lastIndexOf(".");
		if (pos2 == -1)
			pos2 = path.length();
		
		String filename;
		filename = path.substring(pos1 + 1, pos2);
		
		return filename;
	}
	
	private String GetFileFolder(String path) {
		String file_folder;
		int pos1, pos2;
		pos1 = path.lastIndexOf('/');
		pos2 = path.lastIndexOf('/', pos1 - 1);
		if (pos2 == -1) {
			file_folder = "N/A";
		}
		else {
			file_folder = path.substring(pos2 + 1, pos1);
		}
		
		return file_folder;
	}
	
	private String msecToString(long msec)
	{
		long msec_, sec, minute, hour, tmp;
		msec_ = msec % 1000;
		sec = msec / 1000;
		
		// sec = 3710
		tmp = sec % 3600; // 110
		hour = sec / 3600; // 1
		sec = tmp % 60; // 50
		minute = tmp / 60; // 1
		return String.format("%02d:%02d:%02d:%03d", hour, minute, sec, msec_);
	}
	
	private String QueryMediaInfo(String path) {
		MediaInfo info = MeetSDK.getMediaDetailInfo(path);
		
		StringBuffer sbMediaInfo = new StringBuffer();

		sbMediaInfo.append("ext:");
		sbMediaInfo.append(GetFileExt(path));
		
		if (info == null) {
			Log.w(TAG, "video: " + path + " cannot get media info");
			return sbMediaInfo.toString();
		}
		sbMediaInfo.append(", f:");
		String strFormat = info.getFormatName();
		if (strFormat.length() > 6)
			strFormat = strFormat.substring(0, 6);
		sbMediaInfo.append(strFormat);
		
		if (info.getVideoCodecName() != null) {
			sbMediaInfo.append(", v:");
			sbMediaInfo.append(info.getVideoCodecName());
		}
		
		ArrayList<TrackInfo> audiolist = info.getAudioChannelsInfo();
		if (audiolist.size() > 0) {
			sbMediaInfo.append(", a:");
		}
		for (int i=0;i<audiolist.size();i++) {
			TrackInfo item = audiolist.get(i);
			sbMediaInfo.append(item.getCodecName());
			sbMediaInfo.append("(");
			
			if(item.getTitle() != null) {
				sbMediaInfo.append(item.getTitle());
			}
			else if(item.getLanguage() != null) {
				sbMediaInfo.append(item.getLanguage());
			}
			else {
				sbMediaInfo.append("默认");
			}
			sbMediaInfo.append(")|");
		}
		ArrayList<TrackInfo> subtitlelist = info.getSubtitleChannelsInfo();
		if (subtitlelist.size() > 0) {
			sbMediaInfo.append(", s:");
		}
		for (int i=0;i<subtitlelist.size();i++) {
			TrackInfo item = subtitlelist.get(i);
			sbMediaInfo.append(item.getCodecName());
			sbMediaInfo.append("(");
			
			if(item.getTitle() != null) {
				sbMediaInfo.append(item.getTitle());
			}
			else if(item.getLanguage() != null) {
				sbMediaInfo.append(item.getLanguage());
			}
			else {
				sbMediaInfo.append("默认");
			}
			sbMediaInfo.append(")|");
		}
		
		return sbMediaInfo.toString();
	}
	
	private boolean ListMediaInfoHttp() {
		Log.i(TAG, "Java: ListMediaInfoHttp");
		
		HTTPListUtil lister = new HTTPListUtil();
		if (!lister.ListHTTPList(mUrl)) {
			Log.e(TAG, "Java: failed to list http server");
			return false;
		}
		
		HashMap<String, Object> parent_folder = new HashMap<String, Object>();
		parent_folder.put("filename", "..");
		parent_folder.put("filesize", "N/A");
		parent_folder.put("resolution", "N/A");
		parent_folder.put("fullpath", mUrl);
		parent_folder.put("thumb", R.drawable.folder);
		mClipList.add(parent_folder);
		
		List<URL> FileList = lister.getFile();
		List<URL> FolderList = lister.getFolder();
		
		for (int i = 0; i < FolderList.size(); i++) {
			URL url = (URL)FolderList.get(i);
			String clip_fullpath = url.toString();
			String folder = GetFileFolder(clip_fullpath);
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("filename", folder);
			map.put("mediainfo", "N/A");
			map.put("folder", mUrl);
			map.put("filesize", "N/A");
			map.put("modify", "N/A");
			map.put("resolution", "N/A");
			map.put("fullpath", clip_fullpath);
			map.put("thumb", R.drawable.folder);

			Log.i(TAG, "folder: " + folder + " added to list");
			mClipList.add(map);
		}
		
		for (int i = 0; i < FileList.size(); i++) {
			URL url = (URL)FileList.get(i);
			String clip_fullpath = url.toString();
			String filename = GetFileName(clip_fullpath);
			MediaInfo info = MeetSDK.getMediaDetailInfo(clip_fullpath);
			String string_res = "N/A";
			if (info != null) {
				String.format("%dx%d %s", 
					info.getWidth(), info.getHeight(), msecToString(info.getDuration()));
			}
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("filename", filename);
			map.put("mediainfo", QueryMediaInfo(clip_fullpath));
			map.put("folder", mUrl);
			map.put("filesize", "N/A");
			map.put("modify", "N/A");
			map.put("resolution", string_res);
			map.put("fullpath", clip_fullpath);
			map.put("thumb", R.drawable.http);

			Log.i(TAG, "video: " + filename + " added to list");
			mClipList.add(map);
		}
		
		return true;
	}
	
	private boolean ListMediaInfoFolder() {
		// add parent folder ".." line
		HashMap<String, Object> parent_folder = new HashMap<String, Object>();
		parent_folder.put("filename", "..");
		parent_folder.put("mediainfo", "N/A");
		parent_folder.put("folder", "N/A");
		parent_folder.put("filesize", "N/A");
		parent_folder.put("resolution", "N/A");
		parent_folder.put("fullpath", "..");
		parent_folder.put("thumb", R.drawable.folder);
		mClipList.add(parent_folder);
				
		File folder = new File(mUrl);
		File[] files = folder.listFiles();
		if (files == null) {
			Log.i(TAG, "folder is empty");
			return true;
		}

		Arrays.sort(files, new FileComparator());
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		for (File file : files) {
			String fileName = file.getName();
			if (fileName.endsWith("srt") || fileName.endsWith("ass"))
				continue;
			
			if (file.isFile() && !file.isHidden())
			{
				long modTime = file.lastModified();

				String filesize;
				if (file.length() > ONE_MAGABYTE)
					filesize = String.format("%.3f MB",
							(float) file.length() / (float) ONE_MAGABYTE);
				else if (file.length() > ONE_KILOBYTE)
					filesize = String.format("%.3f kB",
							(float) file.length() / (float) ONE_KILOBYTE);
				else
					filesize = String.format("%d Byte", file.length());

				MediaInfo info = MeetSDK.getMediaDetailInfo(file);

				if (info != null) {
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("filename", file.getName());
					map.put("mediainfo", QueryMediaInfo(file.getAbsolutePath()));
					map.put("folder", GetFileFolder(file.getAbsolutePath()));
					map.put("filesize", filesize);
					map.put("modify", dateFormat.format(new Date(modTime)));
					
					String string_res = String.format("%dx%d %s", 
							info.getWidth(), info.getHeight(), msecToString(info.getDuration()));
					map.put("resolution", string_res);
					Log.i(TAG, "Java: media info: " + string_res);
					
					map.put("fullpath", file.getAbsolutePath());
					map.put("thumb", file.getAbsolutePath()/*R.drawable.clip*/);

					int index = fileName.lastIndexOf(".");
					String s;
					if (index != -1)
						s = fileName.substring(0, index).toString();
					else
						s = fileName;
					
					Log.i(TAG, "video: " + fileName + " added to list");
					mClipList.add(map);
				}
				else {
					Log.w(TAG, "video: " + fileName + " cannot get media info");
				}
			}
			else if(file.isDirectory() && !file.isHidden()) {
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("filename", file.getName());
				map.put("mediainfo", "N/A");
				map.put("folder", "N/A");
				map.put("filesize", "N/A");
				map.put("modify", "N/A");
				map.put("resolution", "N/A");
				map.put("fullpath", file.getAbsolutePath());
				map.put("thumb", R.drawable.folder);

				int index = fileName.lastIndexOf(".");
				String s;
				if (index != -1)
					s = fileName.substring(0, index).toString();
				else
					s = fileName;
				
				Log.i(TAG, "folder: " + fileName + " added to list");
				mClipList.add(map);
			}
		}
		
		return true;
	}
	
	private boolean ListMediaInfoMediaStore() {
		Log.i(TAG, "Java: ListMediaInfoMediaStore()");
		
		String[] thumbColumns = new String[]{
				MediaStore.Video.Thumbnails.DATA,
				MediaStore.Video.Thumbnails.VIDEO_ID
		};
		
		String[] mediaColumns = new String[]{
				MediaStore.Video.Media.DATA,
				MediaStore.Video.Media._ID,
				MediaStore.Video.Media.TITLE,
				MediaStore.Video.Media.MIME_TYPE,
				MediaStore.Video.Media.DURATION,
				MediaStore.Video.Media.SIZE
		};
		
		ContentResolver cr = mContext.getContentResolver();  //cr.query
        Cursor cur = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumns,  
                null, null, null);  
        if (cur == null) {
        	Toast.makeText(mContext, "no cursor", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		final int displayNameId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
		final int dataId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
		final int durationId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
		final int sizeId = cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			String title = cur.getString(displayNameId);
			long duration = cur.getLong(durationId);
			long size = cur.getLong(sizeId);
			String path = cur.getString(dataId);
			
			// bypass pptv folder
			if (path.indexOf("/pptv/") != -1)
				continue;
			
			File file = new File(path);
			if (!file.exists()) {
				Log.w(TAG, "failed to open file: " + path);
				continue;
			}
			
			Log.i(TAG, String.format("title: %s, path %s, duration: %d, size %d", title, path, duration, size));
			
			String filesize;
			if (size > ONE_MAGABYTE)
				filesize = String.format("%.3f MB",
						(float) size / (float) ONE_MAGABYTE);
			else if (size > ONE_KILOBYTE)
				filesize = String.format("%.3f kB",
						(float) size / (float) ONE_KILOBYTE);
			else
				filesize = String.format("%d Byte", size);
				
			long modTime = file.lastModified();
			
			MediaInfo info = MeetSDK.getMediaDetailInfo(file);
			
			String string_res;

			if (info != null) {
				string_res = String.format("%dx%d %s", 
						info.getWidth(), info.getHeight(), msecToString(info.getDuration()));
				Log.d(TAG, "Java: media info: " + string_res);

				int index = path.lastIndexOf(".");
				String s;
				if (index != -1)
					s = path.substring(0, index).toString();
				else
					s = path;
			}
			else {
				string_res = "N/A";
				Log.w(TAG, "video: " + path + " cannot get media info");
			}
			
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("filename", title);
			map.put("mediainfo", QueryMediaInfo(path));
			map.put("folder", GetFileFolder(path));
			map.put("filesize", filesize);
			map.put("modify", dateFormat.format(new Date(modTime)));
			map.put("resolution", string_res);
			map.put("fullpath", path);
			map.put("thumb", path);
			
			Log.i(TAG, "video: " + title + " added to list");
			mClipList.add(map);
		}
		
		return true;
	}
	
	public boolean ListMediaInfo(String url) {
		Log.i(TAG, "Java: ListMidiaInfo " + url);
		
		mUrl = url;
		mClipList.clear();
		
		if (mUrl == null || mUrl.equals(""))
			return ListMediaInfoMediaStore();
		else if(mUrl.startsWith("http://"))
			return ListMediaInfoHttp();
		else
			return ListMediaInfoFolder();
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
	
	class FilePathComparator implements Comparator<String> {
		@Override
		public int compare(String path1, String path2) {
				
			String s1=path1.toLowerCase();
			String s2=path2.toLowerCase();
			return s1.compareTo(s2);
	    }
	}
	
}
