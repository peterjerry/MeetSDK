package com.pplive.common.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.pplive.media.util.BufferedRandomAccessFile;
import android.text.TextUtils;
import android.util.Log;

public class LogUtil {
	
	private static final String TAG = "LogUtil";
	
    private static final SimpleDateFormat SDF = 
    		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static String outputfile;

    private static String logpath;

    private static String infopath;

    private static int fileLimit = 100 * 1024;

    private static long offset = 0;

    private static BufferedRandomAccessFile braf = null;
    
    private static boolean inited;
    
    public static boolean init(String logfile, String tempPath) {
    	Log.i(TAG, String.format("Java: init() logfile %s, tempPath %s", logfile, tempPath));
    	
        outputfile = logfile;
        infopath = tempPath + "/deviceinfo";
        logpath = tempPath + "/meetplayer.log";
        boolean hasLogPath = makeParentPath(outputfile);
        boolean hasTempPath = makePath(tempPath);
        inited = hasLogPath && hasTempPath;
        
        return inited;
    }
	
	public static void verbose(String TAG, String msg) {
        log(Log.VERBOSE, TAG, msg);
    }

    public static void debug(String TAG, String msg) {
        log(Log.DEBUG, TAG, msg);
    }

    public static void info(String TAG, String msg) {
        log(Log.INFO, TAG, msg);
    }

    public static void warn(String TAG, String msg) {
        log(Log.WARN, TAG, msg);
    }

    public static void error(String TAG, String msg) {
        log(Log.ERROR, TAG, msg);
    }
    
	private static void log(int level, String tag, String msg) {
        if (level >= Log.DEBUG) {
            writeFile(String.format("%s [%s] %s: %s", 
            		SDF.format(new Date()), getLevelString(level), tag, msg));
        }

        if (level == Log.ERROR)
			Log.e(tag, msg);
		else if(level == Log.WARN)
			Log.w(tag, msg);
		else if(level == Log.INFO)
			Log.i(tag, msg);
		else if(level == Log.DEBUG)
			Log.d(tag, msg);
    }
	
	public static synchronized void writeFile(String msg) {
        if (!inited) {
            return;
        }

        try {
            if (braf == null) {
            	// open/create log file
                braf = new BufferedRandomAccessFile(logpath, "rw");
                try {
                    offset = braf.length();
                    if (offset >= fileLimit) {
                        String firstline = braf.readLine();
                        int index = firstline.indexOf('#');
                        offset = (index == -1) ? 0 : Integer.parseInt(firstline.substring(0, index));
                    }
                } catch (Exception e) {
                	e.printStackTrace();
                    offset = 0;
                }
            }
            
            msg += '\n';
            try {
                braf.seek(offset);
                braf.write(msg.getBytes());
                offset = (offset + msg.length()) >= fileLimit ? 0 : offset + msg.length();
                braf.seek(0);
                braf.write((offset + "#").getBytes());
                braf.flush();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
	
    private static String getLevelString(int level) {

        switch (level) {
        case Log.VERBOSE:
            return "V";
        case Log.DEBUG:
            return "D";
        case Log.INFO:
            return "I";
        case Log.WARN:
            return "W";
        case Log.ERROR:
            return "E";
        }

        return "U";
    }
    
    static boolean makePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        } else
            return true;
    }

    static boolean makeParentPath(String filename) {
        if (TextUtils.isEmpty(filename)) {
            return false;
        }
        File file = new File(filename);
        return makePath(file.getParentFile().getAbsolutePath());
    }
}
