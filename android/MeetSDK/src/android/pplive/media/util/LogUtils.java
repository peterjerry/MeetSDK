package android.pplive.media.util;

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

import android.pplive.media.MeetSDK;
import android.util.Log;

public class LogUtils {

    static final String TAG = "MeetSDK_LogUtils";

    private static final int EXCEPTION_STACK_INDEX = 5;

    private static String outputfile;

    private static String logpath;

    private static String infopath;

    private static int fileLimit = 100 * 1024;

    private static long offset = 0;

    private static BufferedRandomAccessFile braf = null;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static boolean inited;

    private LogUtils() {
    }

    public static boolean init(String logfile, String tempPath) {
    	Log.i(TAG, String.format("Java: init() logfile %s, tempPath %s", logfile, tempPath));
    	
        outputfile = logfile;
        infopath = tempPath + "/deviceinfo";
        logpath = tempPath + "/player.log";
        boolean hasLogPath = IoUtil.makeParentPath(outputfile);
        boolean hasTempPath = IoUtil.makePath(tempPath);
        inited = hasLogPath && hasTempPath;
        return inited;
    }

    public static void logDeviceInfo() {
        if (!inited) {
        	Log.w(TAG, "log is not inited");
            return;
        }
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(infopath)));
            StringBuffer sb = new StringBuffer();
            sb.append("MeetSDK version:").append(MeetSDK.getVersion()).append("\n");
			sb.append("MeetSDK native_version:").append(MeetSDK.getNativeVersion()).append("\n");
            sb.append("Android version:").append(DeviceInfoUtil.getSystemVersion()).append("\n");
            sb.append("CPU cores:").append(DeviceInfoUtil.getCpuCoresNum()).append("\n");
            sb.append("CPU Freq:").append(DeviceInfoUtil.getCpuFreq()).append("\n");
            sb.append("Memory:").append(DeviceInfoUtil.getTotalMemory()).append("\n");
            sb.append("Manufacturer:").append(DeviceInfoUtil.getManufacturer()).append("\n");
            sb.append("Model:").append(DeviceInfoUtil.getModel()).append("\n");
            sb.append("Device:").append(DeviceInfoUtil.getDevice()).append("\n");
            sb.append("Brand:").append(DeviceInfoUtil.getBrand()).append("\n");
            bw.write(sb.toString());
            bw.close();
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    public static void makeUploadLog() {
        if (!inited) {
        	Log.w(TAG, "log is not inited");
            return;
        }
        
        logDeviceInfo();
        try {
        	Log.i(TAG, "Java: begin to write log file: " + outputfile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile)));
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infopath)));
            String line = "";
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.write('\n');
            }
            br.close();
            File logfile = new File(logpath);
            if (logfile.exists()) {
                bw.write("-----------------\n");
                br = new BufferedReader(new InputStreamReader(new FileInputStream(logfile)));
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.write('\n');
                }
                br.close();
            }
            bw.flush();
            bw.close();
            
            braf.flush();
            braf.close();
            braf = null;
            logfile.delete();
            Log.i(TAG, "Java: end write log file");
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    public static void verbose(String msg) {
        log(Log.VERBOSE, getTag(), msg);
    }

    public static void debug(String msg) {
        log(Log.DEBUG, getTag(), msg);
    }

    public static void info(String msg) {
        log(Log.INFO, getTag(), msg);
    }

    public static void warn(String msg) {
        log(Log.WARN, getTag(), msg);
    }

    public static void error(String msg) {
        log(Log.ERROR, getTag(), msg);
    }

    public static void error(String msg, Throwable tr) {
        error(msg + ": " + Log.getStackTraceString(tr));
    }

    private static void log(int level, String tag, String msg) {
        if (level >= Log.DEBUG) {
            writeFile(String.format("%s %s %s: %s", SDF.format(new Date()), getLevelString(level), tag, msg));
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

    public static void nativeLog(int level, String tag, String msg) {
        log(level, tag, msg);
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

        return "";
    }

    public static String getTag() {
        return getTag(EXCEPTION_STACK_INDEX);
    }

    public static String getTag(int stackLevel) {
        try {
            StackTraceElement[] stack_trace = Thread.currentThread().getStackTrace();
            if (stack_trace == null || stack_trace.length <= stackLevel) {
                return "***";
            }

            StackTraceElement element = stack_trace[stackLevel];
            String className = element.getClassName();

            int index = className.lastIndexOf(".");
            if (index > 0) {
                className = className.substring(index + 1);
            }

            return className + "_" + element.getMethodName() + "_" + element.getLineNumber();

        } catch (Throwable e) {
            e.printStackTrace();
            return "***";
        }
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
}
