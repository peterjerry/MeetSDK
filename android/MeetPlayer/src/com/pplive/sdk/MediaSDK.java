package com.pplive.sdk;

import java.util.Properties;
import java.nio.ByteBuffer;

public class MediaSDK
{

    public static final int LEVEL_ERROR = 0;
    public static final int LEVEL_ALARM = 1;
    public static final int LEVEL_EVENT = 2;
    public static final int LEVEL_INFOR = 3;
    public static final int LEVEL_DEBUG = 4;
    public static final int LEVEL_DEBUG1 = 5;
    public static final int LEVEL_DEBUG2 = 6;
    
    public static String libPath = ".";
    public static String logPath = ".";
    public static String libName = null;
    public static String dumpPath = "";
    public static String cpuArch = "";
    public static boolean logOn = true;
    public static int logLevel = LEVEL_DEBUG;

    public static class Download_Statistic
    {
        public long total_size;

        public long finish_size;

        public int speed;

        @Override
        public String toString()
        {
            return "Download_Statistic [total_size=" + total_size + ", finish_size=" + finish_size + ", speed=" + speed
                    + "]";
        }

    }
    
    public static class Upload_Statistic
    {
        public int time;
        public int remaining_time;
    }
    
    public static class Download_Result
    {
        public int his_max_speed;
        public int cur_max_speed;
        public int bwtype;
        public int speed_limit;
        public String error_code;
        public String reason;
        public String cur_cdn;
        public String main_cdn;
        public String bakup_cdn;
    }
    
    public static class Play_Statistic
    {
        public int cdn_speed;
        public int p2p_speed;
        public int sn_speed;
    }

    public static interface Download_Callback
    {
        public void invoke(
            long errno);
    }
    
    public static String jni_lib_name()
    {
    	if (libName != null)
    		return libName;
    	
        String name;
        Properties props = System.getProperties();
        String osArch = props.getProperty("os.arch");
        if(cpuArch != null && cpuArch.length() != 0) {
            osArch = cpuArch;
        }
        System.out.println(osArch);
        if (osArch != null && osArch.contains("x86"))
        {
            name = "ppbox_jni-android-x86-gcc44-mt-1.1.0";
        } 
        else if (osArch != null && osArch.contains("mips"))
        {
            name = "ppbox_jni-mips-android-gcc44-mt-1.1.0";
        }
        else if (osArch != null && osArch.contains("i386"))
        {
            name = "ppbox_jni-linux-x86-gcc44-mt-1.1.0";
        }
        else if (osArch != null && osArch.contains("86"))
        {
            name = "ppbox_jni-android-x86-gcc44-mt-1.1.0";
        }
        else if (osArch != null && osArch.contains("aarch64"))
        {
            name = "ppbox_jni-arm-android-r10d-gcc49-mt-1.1.0";
        }
        else
        {
            name = "ppbox_jni-armandroid-r4-gcc44-mt-1.1.0";
        }
        return name;
    }
    
    public static String ppbox_lib_name()
    {
        String name;
        Properties props = System.getProperties();
        String osArch = props.getProperty("os.arch");
        if(cpuArch != null && cpuArch.length() != 0) {
            osArch = cpuArch;
        }
        System.out.println(osArch);
        if (osArch != null && osArch.contains("x86"))
        {
            name = "ppbox-android-x86-gcc44-mt-1.1.0";
        } 
        else if (osArch != null && osArch.contains("mips"))
        {
            name = "ppbox-mips-android-gcc44-mt-1.1.0";
        }
        else if (osArch != null && osArch.contains("i386"))
        {
            name = "ppbox-linux-x86-gcc44-mt-1.1.0";
        }
        else if (osArch != null && osArch.contains("86"))
        {
            name = "ppbox-android-x86-gcc44-mt-1.1.0";
        }
        /*else if (osArch != null && osArch.contains("aarch64"))
        {
            name = "ppbox-arm-android-r10d-gcc49-mt-1.1.0";
        }*/
        else
        {
            name = "ppbox-armandroid-r4-gcc44-mt-1.1.0";
        }
        return name;
    }
    
    private static boolean load_by_local(String name)
    {   
        try 
        {
            String full_name = libPath + "/lib" + name + ".so";
            System.load(full_name);
        }
        catch (Throwable e) 
        {
            return false;
        }
        return true;
    }
    
    private static boolean load_by_system(String name)
    { 
        try 
        {
            System.loadLibrary(name);
        }
        catch (Throwable e) 
        {
            return false;
        }
        return true;
    }

    //-------------------------------------------------------------------------
    // startP2PEngine/startP2PEngineEx
    private native static long startP2PEngineImpl(
        String gid, 
        String pid, 
        String auth,
        String params);
    public static long startP2PEngineEx(
        String gid, 
        String pid, 
        String auth,
        String params)
    {
        String name = jni_lib_name();
        if(g_init || load_by_local(name) || load_by_system(name))
        {
            g_init = true;
            return startP2PEngineImpl(gid,pid,auth,params);
        }
        else
        {
            return -1;
        }
    }
    public static long startP2PEngine(
        String gid, 
        String pid, 
        String auth)
    {
        return startP2PEngineEx(gid,pid,auth,"");
    }

    //-------------------------------------------------------------------------
    // stopP2PEngine
    private native static long stopP2PEngineImpl();
    public static long stopP2PEngine()
    {
        if (!g_init)
            return -1;
        return stopP2PEngineImpl();
    }

    private native static void sendDacImpl(int type, String lparam, String rparam);

    public static void sendDac(int type, String lparam, String rparam)
    {
        if (!g_init)
            return;
        sendDacImpl(type,lparam,rparam);
    } 

    //-------------------------------------------------------------------------
    // downloadOpen
    private native static long downloadOpenImpl(
        String playlink, 
        String format, 
        String save_filename, 
        Download_Callback callback);
    // 返回打开下载用例的句柄
    public static long downloadOpen(
        String playlink, 
        String format, 
        String save_filename, 
        Download_Callback callback)
    {
        if (!g_init)
            return -1;
        return downloadOpenImpl(playlink, format, save_filename, callback);
    }

    //-------------------------------------------------------------------------
    // downloadClose
    private native static void downloadCloseImpl(
        long handle);
    public static void downloadClose(
        long handle)
    {
        if (!g_init)
            return;
        downloadCloseImpl(handle);
    }

    //-------------------------------------------------------------------------
    // getPPBoxVersion
    private native static String getPPBoxVersionImpl();
    public static String getPPBoxVersion()
    {
    if (!g_init)
            return "";
        return getPPBoxVersionImpl();
    }

    //-------------------------------------------------------------------------
    // getDownloadInfo
    private native static long getDownloadInfoImpl(
        long handle, 
        Download_Statistic stat);
    public static long getDownloadInfo(
        long handle, 
        Download_Statistic stat)
    {
        if (!g_init)
            return -1;
        return getDownloadInfoImpl(handle, stat);
    }
     
    //-------------------------------------------------------------------------
    // getDownloadResult
    private native static long getDownloadResultImpl(
    long handle, 
    Download_Result stat);
    public static long getDownloadResult(
        long handle, 
        Download_Result stat)
    {
       if (!g_init)
            return -1;
        return getDownloadResultImpl(handle, stat);
    }
    
    //-------------------------------------------------------------------------
    // base64EncodeByKey
    private native static String base64EncodeByKeyImpl(
        String str, 
        String key);
    public static String base64EncodeByKey(
        String str, 
        String key)
    {
       if (!g_init)
            return "";
        return base64EncodeByKeyImpl(str, key);
    }
    
    //-------------------------------------------------------------------------
    private native static String base64EncodeImpl(
        String str);
    public static String base64Encode(
        String str)
    {
       if (!g_init)
            return "";
        return base64EncodeImpl(str);
    }

    private native static int mergeMoiveImpl(
        String source,
        String format,
        String dest);
    public static int mergeMoive(
        String source,
        String format,
        String dest)
    {
       if (!g_init)
            return -1;
        return mergeMoiveImpl(source,format,dest);
    }

    //-------------------------------------------------------------------------
    private native static short getPortImpl(
        String str);
    public static short getPort(
        String str)
    {
       if (!g_init)
            return -1;
        return getPortImpl(str);
    }

    //-------------------------------------------------------------------------
    private native static long getPlayInfoImpl(
        String name, 
        Play_Statistic stat);
    public static long getPlayInfo(
        String name, 
        Play_Statistic stat)
    {
       if (!g_init)
            return -1;
        return getPlayInfoImpl(name,stat);
    }

    //-------------------------------------------------------------------------
    private native static void setPlayLevelImpl(
        String name, 
        int level);
    public static void setPlayLevel(
        String name, 
        int level)
     {
        if (!g_init)
            return;
         setPlayLevelImpl(name, level);
     }
        
    //-------------------------------------------------------------------------
    private native static void setPlayInfoImpl(
        String name, 
        String type, 
        String info);
    //设置 play xml,  name is play id    
    public static void setPlayInfo(
        String name, 
        String type, 
        String info)
    {
       if (!g_init)
            return;
        setPlayInfoImpl(name, type, info);
    }
    
    //-------------------------------------------------------------------------
    private native static void setDownloadBufferSizeImpl(
        int length);
    public static void setDownloadBufferSize(
        int length)
    {
       if (!g_init)
            return;
        setDownloadBufferSizeImpl(length);
    }

    //-------------------------------------------------------------------------
    private native static void setConfigImpl(
        String module, 
        String section, 
        String key, 
        String value);
    public static void setConfig(
        String module, 
        String section, 
        String key, 
        String value)
    {
       String name = jni_lib_name();
       if(g_init || load_by_local(name) || load_by_system(name))
       {
           g_init = true;
           setConfigImpl(module, section, key, value);
       }
        
    }
        
    //-------------------------------------------------------------------------
    private native static void setPlayerBufferTimeImpl(
        int time);
    public static void setPlayerBufferTime(
        int time)
    {
       if (!g_init)
            return;
        setPlayerBufferTimeImpl(time);
    }

    //-------------------------------------------------------------------------
    private native static void setCurPlayerTimeImpl(
        int time);
    public static void setCurPlayerTime(
        int time)
    {
       if (!g_init)
            return;
        setCurPlayerTimeImpl(time);
    }
    
    //-------------------------------------------------------------------------
    private native static void setStatusImpl(
        String main_type, 
        String sub_type, 
        String value);
    public static void setStatus(
        String main_type, 
        String sub_type, 
        String value)
    {
       if (!g_init)
            return;
        setStatusImpl(main_type,sub_type,value);
    }
    
    //---------------------rtmp上传 -----------
    
    public static class CaptureConfigData
    {
        public int stream_count;   //流个数
        public int thread_count;       // 线程个数 1 表示单线程序
        public int sort_type;       // 1  需要排序, 0不需要排序
    };
    
    public static class StreamInfo
    {
        public int time_scale;        //时间换算 1 秒 = time_scale 个分隔
        public int bitrate;           //bit率  可以填为 0
        public int __union0;     // 视频: width            音频: channel_count
        public int __union1;     // 视频: height           音频: sample_size
        public int __union2;     // 视频: frame_rate       音频: sample_rate
        public int __union3;     //填0
        public int format_size;  //编码参数config 大小
        public ByteBuffer format_buffer;  //编码参数
    };
    
    
    public static class Sample
    {
        public int itrack;            //表示音/视类型  0 视频   1 音频
        public int flags;             // 1 关键帧    0  非关键帧
        public long time;              // decode time
        public int composite_time_delta;    //baseline为0
        public int size;                 //帧大小
        public ByteBuffer buffer;        //帧数据
    };
    
    public native static long CaptureOpen(
			        String playlink,
					String format,
					String outUrl,
					Download_Callback callback);

	public native static long CaptureInit(long handle, CaptureConfigData config);
    
	public native static long CaptureSetStream(long handle, int index, StreamInfo info);
	
	public native static void CapturePutSample(long handle, Sample sample);
	
	public native static void CaptureDestroy(long handle);
	
	public native static long CaptureStatInfo(long handle,Upload_Statistic stat);

    static boolean callback = false;
    private static boolean g_init = false;
    public static void main(String[] argv)
    {
        startP2PEngine(argv[0], argv[1], argv[2]);
        if (argv.length == 6) {
            downloadOpen(argv[3], argv[4], argv[5], new Download_Callback() {
                public void invoke(long errno) {
                    System.out.println("Download_Callback: " + errno);
                    callback = true;
                }
            });
            while (!callback)
                try { Thread.currentThread().sleep(1000); } catch (Exception e) {}
        }
        stopP2PEngine();
    }
}
