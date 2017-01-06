package com.pplive.common.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogUtil {
	
	private static final String TAG = "LogUtil";

    private static final int VERBOSE    = 1;
	private static final int DEBUG      = 2;
    private static final int INFO       = 3;
    private static final int WARN       = 4;
    private static final int ERROR      = 5;


    private static final SimpleDateFormat SDF = 
    		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
    
    private static String outputfile;

    private static String logpath;

    private static String infopath;

    private static int fileLimit = 100 * 1024; // 100k

    private static long offset = 0;

    private static BufferedRandomAccessFile braf = null;
    
    private static boolean inited;
    
    public static boolean init(String logfile, String tempPath) {
    	System.out.println(String.format("Java: init() logfile %s, tempPath %s", logfile, tempPath));
    	
        outputfile = logfile;
        infopath = tempPath + "/deviceinfo";
        logpath = tempPath + "/bibo.log";
        boolean hasLogPath = makeParentPath(outputfile);
        boolean hasTempPath = makePath(tempPath);
        inited = hasLogPath && hasTempPath;
        
        return inited;
    }
	
	public static void verbose(String TAG, String msg) {
        log(VERBOSE, TAG, msg);
    }

    public static void debug(String TAG, String msg) {
        log(DEBUG, TAG, msg);
    }

    public static void info(String TAG, String msg) {
        log(INFO, TAG, msg);
    }

    public static void warn(String TAG, String msg) {
        log(WARN, TAG, msg);
    }

    public static void error(String TAG, String msg) {
        log(ERROR, TAG, msg);
    }
    
	private static void log(int level, String tag, String msg) {
        if (level >= INFO) {
            writeFile(String.format("%s [%s] %s: %s", 
            		SDF.format(new Date()), getLevelString(level), tag, msg));
        }

        System.out.println(String.format("%s [%s] %s: %s",
                SDF.format(new Date()), getLevelString(level), tag, msg));
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
        case VERBOSE:
            return "V";
        case DEBUG:
            return "D";
        case INFO:
            return "I";
        case WARN:
            return "W";
        case ERROR:
            return "E";
        }

        return "U";
    }
    
    static private boolean makePath(String path) {
        if (isEmpty(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        } else
            return true;
    }

    static private boolean makeParentPath(String filename) {
        if (isEmpty(filename)) {
            return false;
        }
        File file = new File(filename);
        return makePath(file.getParentFile().getAbsolutePath());
    }

    static private boolean isEmpty(String str) {
        return (str == null) || (str.trim().length() == 0) || (str.trim().equals(""));
    }
}
