package com.pplive.meetplayer.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pplive.db.MediaStoreDatabaseHelper;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.util.Log;

public class MyMediaService extends Service {
	private final static String TAG = "MyMediaService";
	private final static String OBSERVE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	
	private FileObserver mFileObserver;
	private static MediaStoreDatabaseHelper mediaDB;
	
	private static final Map<String, String> sMimeTypeMap;
	private static final Pattern sRegMimeType;
	
	static {
		final String extensions[] = { "264", "h264", "265", "h265", 
				"3g2", "3gp", "3gp2", "3gpp", "3gpp2", "3p2", "amv", "asf", "avi", "dat",
				"dir", "divx", "dlx", "dv", "dv4", "dvr", "dvr-ms", "dvx", "dxr", "evo", 
				"f4p", "f4v", "flv", "gvi",
				"hdmov", "ivf", "ivr", "k3g", 
				"m1v", "m21", "m2t", "m2ts", "m2v", "m3u", "m3u8", "m4e", "m4v", "mj2",
				"mjp", "mjpg", "mkv", "mmv", "mnv", "mod", "moov", 
				"mov", "movie", "mp21", "mp2v", "mp4", "mp4v",
				"mpc", "mpe", "mpeg", "mpeg4", "mpg", "mpg2", 
				"mpv", "mpv2", "mts", "mpegts", "m2ts", "mtv", "mve", "mxf", 
				"nsv", "nuv", "ogg","ogm", "ogv", "ogx", 
				"pgi", "ppl", "pva", "qt", "qtm", 
				"r3d", "rm", "rmvb", "roq", "rv", "svi", "trp", "ts", 
				"vc1", "vcr", "vfw", "vid", "vivo", "vob", 
				"vp3", "vp6", "vp7", "vp8", "vp9", "vro", "webm", "wm", "wmv", "wtv",
				"xvid", "yuv" };

		sMimeTypeMap = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		sb.append("^(.*)[.](");
		for (int index = 0; index < extensions.length; ++index) {
			addMimeType(extensions[index]);
			
			sb.append(String.format(index == 0 ? "%s" : "|%s", extensions[index]));
		}
		sb.append(")$");
		sRegMimeType = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	private static void addMimeType(String extension) {
		sMimeTypeMap.put(extension, "video/" + extension);
	}

	private static String getMimeType(String filename) {
		if (filename == null) {
			return "video/unknown";
		}

		Matcher matcher = sRegMimeType.matcher(filename);
		return matcher.find() ? sMimeTypeMap.get(matcher.group(2).toLowerCase()) : "video/unknown";
	}
	
	@Override  
    public void onCreate() {  
        Log.i(TAG, "Java: MyMediaService onCreate");  
        super.onCreate();
        
        mediaDB = MediaStoreDatabaseHelper.getInstance(getApplicationContext());
        
        if (null == mFileObserver) {  
            mFileObserver = new RecursiveFileObserver(OBSERVE_PATH, 
            		FileObserver.CLOSE_WRITE | FileObserver.CREATE |
            		FileObserver.DELETE | FileObserver.DELETE_SELF |
            		FileObserver.MOVE_SELF | FileObserver.MOVED_FROM | FileObserver.MOVED_TO);  
            mFileObserver.startWatching(); //开始监听  
            Log.i(TAG, "Java: start to monitor folder " + OBSERVE_PATH);
        }  
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.i(TAG, String.format("Java: MyMediaService onStartCommand() %d %d", flags, startId));
		
		return Service.START_STICKY;
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

    	Log.i(TAG, "Java: MyMediaService onDestroy()");
    	if (null != mFileObserver)
    		mFileObserver.stopWatching(); //停止监听 
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static void deleteMedia(String path) {
		mediaDB.deleteMedia(path);
		Log.i(TAG, "Java: delete media file: " + path);
	}
	
	private static void addMedia(String path) {
		MediaInfo info = MeetSDK.getMediaDetailInfo(path);
		if (info != null) {
			String title = path;
			int pos = title.lastIndexOf("/");
			if (pos > 0)
				title = title.substring(pos + 1);
			
			mediaDB.saveMedia(path, title, info);
			Log.i(TAG, "Java: save new media file: " + path);
		}
	}
	
	static class  RecursiveFileObserver extends FileObserver {  
		/** Only modification events */  
	    public static int CHANGES_ONLY = CREATE | DELETE | CLOSE_WRITE  
	            | DELETE_SELF | MOVE_SELF | MOVED_FROM | MOVED_TO;
	    
	    private List<SingleFileObserver> mObservers;  
	    private String mPath;  
	    private int mMask; 
	    
        //mask:指定要监听的事件类型，默认为FileObserver.ALL_EVENTS  
        public RecursiveFileObserver(String path, int mask) {  
        	super(path, mask);  
            mPath = path;  
            mMask = mask;
        }  
  
        public RecursiveFileObserver(String path) {  
        	super(path, ALL_EVENTS);
        }  
  
        @Override  
        public void startWatching() {  
            if (mObservers != null)  
                return;  
      
            mObservers = new ArrayList<SingleFileObserver>();  
            Stack<String> stack = new Stack<String>();  
            stack.push(mPath);  
      
            while (!stack.isEmpty()) {  
                String parent = (String)stack.pop();  
                mObservers.add(new SingleFileObserver(parent, mMask));  
                File path = new File(parent);  
                File[] files = path.listFiles();  
                if (null == files)  
                    continue;  
                for (File f : files) {  
                    if (f.isDirectory() && !f.getName().equals(".")  
                            && !f.getName().equals("..")) {  
                        stack.push(f.getPath());  
                    }  
                }  
            }  
      
            for (int i = 0; i < mObservers.size(); i++) {  
                SingleFileObserver sfo = mObservers.get(i);  
                sfo.startWatching();  
            }  
        };  
      
        @Override  
        public void stopWatching() {  
            if (mObservers == null)  
                return;  
      
            for (int i = 0; i < mObservers.size(); i++) {  
                SingleFileObserver sfo = (SingleFileObserver) mObservers.get(i);  
                sfo.stopWatching();  
            }  
              
            mObservers.clear();  
            mObservers = null;  
        };  
        
		@Override
		public void onEvent(int event, String path) {
			final int action = event & FileObserver.ALL_EVENTS;
			switch (action) {
			case FileObserver.ACCESS:
				Log.i(TAG, "Java: event: 文件或目录被访问, path: " + path);
				break;
			case FileObserver.ATTRIB:  
	            Log.i(TAG, "Java: event: 文件或目录属性修改, path: " + path);  
	            break;  
	        case FileObserver.CLOSE_NOWRITE:  
	        	Log.i(TAG, "Java: event: 文件或目录读关闭, path: " + path);  
	            break;  
			case FileObserver.CREATE:
				Log.i(TAG, "Java: event: 文件或目录被创建, path: " + path);
				break;
			case FileObserver.DELETE:
				Log.i(TAG, "Java: event: 文件或目录被删除, path: " + path);
				if (sRegMimeType.matcher(path).find())
					deleteMedia(path);
				break;
			case FileObserver.OPEN:
				Log.i(TAG, "Java: event: 文件或目录被打开, path: " + path);
				break;
			case FileObserver.MODIFY:
				Log.i(TAG, "Java: event: 文件或目录被修改, path: " + path);
				break;
			case FileObserver.MOVE_SELF:  
				Log.i(TAG, "Java: event: 文件或目录自移动, path: " + path);
	            break;  
	        case FileObserver.MOVED_FROM:  
	        	Log.i(TAG, "Java: event: 文件或目录移动自, path: " + path);
	        	if (sRegMimeType.matcher(path).find())
	        		deleteMedia(path);
	            break;  
	        case FileObserver.MOVED_TO:  
	        	Log.i(TAG, "Java: event: 文件或目录移动至, path: " + path);
	        	if (sRegMimeType.matcher(path).find())
	        		addMedia(path);
	            break;  
			case FileObserver.CLOSE_WRITE:
				Log.i(TAG, "Java: event: 文件或目录写关闭, path: " + path);
				if (sRegMimeType.matcher(path).find())
					addMedia(path);
				break;
			default:
				Log.w(TAG, "Java: event: " + action + " , path: " + path);
				break;
			}
		}
		
		/** 
	     * Monitor single directory and dispatch all events to its parent, with full 
	     * path. 
	     */  
	    class SingleFileObserver extends FileObserver {  
	        String mPath;  
	  
	        public SingleFileObserver(String path) {  
	            this(path, ALL_EVENTS);  
	            mPath = path;  
	        }  
	  
	        public SingleFileObserver(String path, int mask) {  
	            super(path, mask);  
	            mPath = path;  
	        }  
	  
	        @Override  
	        public void onEvent(int event, String path) {  
	            String newPath = mPath + "/" + path;  
	            RecursiveFileObserver.this.onEvent(event, newPath);  
	        }  
	    }  

	}
}
