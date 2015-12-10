package android.pplive.media;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.pplive.media.config.Config;
import android.pplive.media.player.FFMediaExtractor;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.player.TrackInfo;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.MeetPlayerHelper;
import android.pplive.media.player.FFMediaPlayer;
import android.pplive.media.player.PlayerPolicy;
import android.pplive.media.subtitle.SimpleSubTitleParser;
import android.pplive.media.util.DiskLruCache;
import android.pplive.media.util.LogUtils;
import android.pplive.media.util.UrlUtil;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;

public final class MeetSDK {

	@SuppressWarnings("unused")
	private static final String TAG = "pplive/MeetSDK";

	public static final int COMPATIBILITY_HARDWARE_DECODE = 1;
	public static final int COMPATIBILITY_SOFTWARE_DECODE = 2;

	public static final int LEVEL_SYSTEM				= 1;
	public static final int LEVEL_SOFTWARE_SD			= 2;
	public static final int LEVEL_SOFTWARE_HD1			= 3;
	public static final int LEVEL_SOFTWARE_HD2			= 4;
	public static final int LEVEL_SOFTWARE_BD			= 5;
	
	private static String libPath = "";
	private static boolean libLoaded = false;
	
	private static String AppRootDir = null;
	private static String PPBoxLibName = null;
	private static int status = 0;
	
	@Deprecated
	static public void setAppRootDir(String dir) {
		AppRootDir = dir;
	}
	
	@Deprecated
	static public String getAppRootDir() {
		return AppRootDir;
	}
	
	@Deprecated
	static public void setPPBoxLibName(String libname) {
		PPBoxLibName = libname;
	}
	
	@Deprecated
	static public String getPPBoxLibName() {
		return PPBoxLibName;
	}
    
	public static boolean initSDK(Context ctx) {
		return initSDK(ctx, "");
	}
	
	public static boolean initSDK(Context ctx, String path) {
		AppRootDir = "/data/data/" + ctx.getPackageName() + "/";
		
		if (!load_lib(path))
			return false;
		
		boolean retPlayer = FFMediaPlayer.initPlayer(path);
		boolean retExtrator = FFMediaExtractor.initExtrator();
		boolean retParser = SimpleSubTitleParser.initParser(path);
		return (retPlayer && retExtrator && retParser);
	}
	
	private static boolean load_lib(String path) {
		if (libLoaded)
			return true;
		
		try {
			if (path != null) {
				libPath = path;
				
				if (!libPath.equals("") && !libPath.endsWith("/"))
					libPath += "/";
			}
			
			String so_name = "meet";
			
			if (libPath != null && !libPath.equals("")) {
				String full_name;
				full_name = libPath + "lib" + so_name + ".so";
				libLoaded = load_lib_local(full_name);
				
				// 2015.5.29 guoliangma added to fix launcher failed to load so
				if (!libLoaded) {
					LogUtils.warn("failed to load set load-path so, try load so in system path");
					libLoaded = load_lib_system(so_name);
				}
			}
			else {
				libLoaded = load_lib_system(so_name);
			}
			
			if (!libLoaded) {
				LogUtils.error("failed to load meet so");
				return false;
			}
			
			libLoaded = true;
		}
		catch (Throwable t) {
			t.printStackTrace();
			LogUtils.error("failed to initPlayer: " + t.toString());
		}
		
		return libLoaded;
	}
	
	private static boolean load_lib_system(String lib_name) {
		try {
			LogUtils.info("System.loadLibrary() try load " + lib_name);
			System.loadLibrary(lib_name);
			LogUtils.info("System.loadLibrary() " + lib_name + " loaded!");
			return true;
		}
		catch (Throwable t) {
			t.printStackTrace();
			LogUtils.error("failed to load system library meet: " + t.toString());
		}
		
		return false;
	}
	
	private static boolean load_lib_local(String path_name) {
		try {
			LogUtils.info("System.load() try load: " + path_name);
			System.load(path_name);
			LogUtils.info("System.load() " + path_name + " loaded!");
			return true;
		}
		catch (Throwable t) {
			t.printStackTrace();
			LogUtils.error("failed to load local library meet: " + t.toString());
		}
		
		return false;
	}

	@Deprecated
	public static boolean checkCompatibility(Surface surface) {
		return true;
	}

	@Deprecated
	public static boolean checkCompatibility(int checkWhat, Surface surface) {
		return true;
	}
	
	public static boolean SupportSoftDecode() {
		return FFMediaPlayer.native_supportSoftDecode();
	}

	public static int checkSoftwareDecodeLevel() {
		if (AppRootDir == null) {
			LogUtils.error("MeetSDK.AppRootDir is null.");
			throw new IllegalArgumentException("MeetSDK.AppRootDir is null.");
		}

		int decodeLevel = LEVEL_SYSTEM;
		try {
			decodeLevel = MeetPlayerHelper.checkSoftwareDecodeLevel();
		} catch (LinkageError e) {
			LogUtils.error("LinkageError", e);
		}
		
		return decodeLevel;
	}
	
	public static int getCpuArchNumber() {
		if (AppRootDir == null) {
		    LogUtils.error("MeetSDK.AppRootDir is null.");
			throw new IllegalArgumentException("MeetSDK.AppRootDir is null.");
		}

		int archNum = 0;
		try {
			archNum = MeetPlayerHelper.getCpuArchNumber();
		} catch (LinkageError e) {
			LogUtils.error("LinkageError", e);
		}
		return archNum;
	}

	public static String getVersion() {
		return Config.getVersion();
	}
	
	public static String getNativeVersion() {
		return FFMediaPlayer.native_getVersion();
	}

	@Deprecated
	public static String getBestCodec(String appPath) {
		String codec = null;
		try {
			codec = MeetPlayerHelper.getBestCodec(appPath);
		} catch (LinkageError e) {
			LogUtils.error("LinkageError", e);
		}
		
		return codec;
	}
	
	public static MediaInfo getMediaInfo(String filePath) {
		return getMediaInfo(new File(filePath));
	}
	
	public static MediaInfo getMediaInfo(File mediaFile) {
		MediaInfo info = null;

		try {
			if (mediaFile != null && mediaFile.exists()) {
				info = MeetPlayerHelper.getMediaInfo(mediaFile.getAbsolutePath());
			}
		} catch (LinkageError e) {
            LogUtils.error("LinkageError", e);
        }

		return info;
	}
	
	public static MediaInfo getMediaDetailInfo(String url) {
		MediaInfo info = null;
		
		try {
			info = MeetPlayerHelper.getMediaDetailInfo(url);
		} catch (LinkageError e) {
            LogUtils.error("LinkageError", e);
        }

		return info;
	}
	
	public static MediaInfo getMediaDetailInfo(File mediaFile) {
		MediaInfo info = null;

		try {
			if (mediaFile != null && mediaFile.exists()) {
				info = MeetPlayerHelper.getMediaDetailInfo(mediaFile.getAbsolutePath());
			}
		} catch (LinkageError e) {
			LogUtils.error("LinkageError", e);
		}

		return info;
	}

	public static String Uri2String(Context ctx, Uri uri) {

		if (null == ctx || null == uri) {
			return null;
		}

		String schema = uri.getScheme();
		String path = null;

		if ("content".equalsIgnoreCase(schema)) {
			String[] proj = { MediaStore.Video.Media.DATA };
			Cursor cursor = ctx.getContentResolver().query(uri, proj, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
				path = cursor.getString(column_index);
				cursor.close();
			}
		} else if ("file".equalsIgnoreCase(schema)) {
			path = uri.getPath();
		} else {
			path = uri.toString();
		}
		return path;
	}

	public static void setPlayerPolicy(String xml) {
		PlayerPolicy.setPlayerPolicy(xml);
	}
	
	public static DecodeMode getPlayerType(Uri uri) {
		return PlayerPolicy.getDeviceCapabilities(uri);
	}
	
	public static DecodeMode getPlayerType(String path) {
		return PlayerPolicy.getDeviceCapabilities(path);
	}
	
	@Deprecated
	public static boolean isOMXSurface(String url) {

		if (url == null) {
			return false;
		}

		boolean isOMXSurface = false;

		if (url.startsWith("/")) {
			try {
				if(("f16ref".equalsIgnoreCase(android.os.Build.BOARD) ||
					"AML8726-M3".equalsIgnoreCase(android.os.Build.BOARD) ||
					"ppbox".equalsIgnoreCase(android.os.Build.BOARD) ) &&
					(url.endsWith(".mp4") ||
					url.endsWith(".mkv") ||
					url.endsWith(".rmvb") ||
					url.endsWith(".rm") ||
					url.endsWith(".flv") ||
					url.endsWith(".3gp") ||
					url.endsWith(".asf") ||
					url.endsWith(".mpg") ||
					url.endsWith(".ts") ||
					url.endsWith(".m2ts") ||
					url.endsWith(".mov"))) {

					isOMXSurface = true;
				} else if (MeetPlayerHelper.checkSoftwareDecodeLevel() == LEVEL_SYSTEM) {

					isOMXSurface = true;
				} else if (url.endsWith(".mp4") || url.endsWith(".3gp")) {

					MediaInfo info = MeetSDK.getMediaDetailInfo(new File(url));

					if (null != info) {
						LogUtils.info("info.Width:" + info.getWidth());
						LogUtils.info("info.Height:" + info.getHeight());
						LogUtils.info("info.FormatName:" + info.getFormatName());
						LogUtils.info("info.VideoCodecName:" + info.getVideoCodecName());
						
						ArrayList<TrackInfo> audioChannelsInfo =  info.getAudioChannelsInfo();
						if (audioChannelsInfo != null) {
							for(int i=0;i<audioChannelsInfo.size();i++) {
								LogUtils.info(String.format("info.Audio #%d CodecName: ", i, audioChannelsInfo.get(i).getCodecName()));
							}
						}
						
						ArrayList<TrackInfo> subtitleChannelsInfo =  info.getSubtitleChannelsInfo();
						if (subtitleChannelsInfo != null) {
							for(int i=0;i<subtitleChannelsInfo.size();i++) {
								LogUtils.info(String.format("info.Subtitle #%d CodecName: ", i, subtitleChannelsInfo.get(i).getCodecName()));
							}
						}
						
						if ("h264".equals(info.getVideoCodecName())) {
							if (audioChannelsInfo.size() == 0 || "aac".equals(audioChannelsInfo.get(0).getCodecName()))
								isOMXSurface = true;
						}
					}
				}
			} catch (LinkageError e) {
	            LogUtils.error("LinkageError", e);
        	}
			
		} else {
			isOMXSurface = UrlUtil.isPPTVPlayUrl(url);
		}

		return isOMXSurface;
	}

	@Deprecated
	public static boolean isOMXSurface(Context ctx, Uri uri) {
		if (null == ctx || null == uri) {
			return false;
		}

		String path = Uri2String(ctx, uri);

		return isOMXSurface(path);
	}

	@Deprecated
	public static boolean setSurfaceType(Context ctx, SurfaceHolder holder, Uri uri) {
		return false;
	}

	@Deprecated
	public interface SurfaceTypeDecider {
		public boolean isOMXSurface();
	}

	@Deprecated
	public static void setSurfaceType(SurfaceHolder holder, SurfaceTypeDecider decider) {
	}

	@Deprecated
	public static void setSurfaceType(SurfaceHolder holder, boolean isOMXSurface) {
	}

	/**
	 * @param filePath
	 *            the path of video file
	 * @param kind
	 *            could be MINI_KIND or MICRO_KIND
	 * 
	 * @return Bitmap, or null on failures
	 */
	public synchronized static Bitmap createVideoThumbnail(String filePath, int kind) {
		LogUtils.info("createVideoThumbnail: " + filePath);
		
		Bitmap bitmap = null;

		try {
			String key = UrlUtil.hashKeyForDisk(filePath);
			bitmap = getThumbnailFromDiskCache(key);
			if (bitmap != null) {
				LogUtils.info("createVideoThumbnail from diskcache: " + filePath);
				return bitmap;
			}
			
			if (Build.VERSION.SDK_INT >= 8)
				bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MICRO_KIND);
			
			if (bitmap != null) {
				LogUtils.info("createVideoThumbnail from ThumbnailUtils: " + filePath);
			}
			else {
				bitmap = MeetPlayerHelper.createVideoThumbnail(filePath, kind);
				LogUtils.info("createVideoThumbnail from ffmpeg: " + filePath);
			}
			
			if (bitmap != null)
				addThumbnailToDiskCache(key, bitmap);
		} catch (LinkageError e) {
			e.printStackTrace();
			LogUtils.error("LinkageError", e);
		}

		return bitmap;
	}
	
	public synchronized static Bitmap createImageThumbnail(ContentResolver cr, int origId, String filePath, int kind) {

        Bitmap bitmap = null;
        
        String key = UrlUtil.hashKeyForDisk(filePath);
        bitmap = getThumbnailFromDiskCache(key);
        if (null != bitmap) {
            return bitmap;
        } 
        
        bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr, origId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
        if (null != bitmap) {
            addThumbnailToDiskCache(key, bitmap);
        }

        return bitmap;
    }

	public static File ThumbCacheDir = null; 

	private static File sCacheDir = new File(Environment.getExternalStorageDirectory(), "pptv" + File.separator + ".thumbnails");
	private static DiskLruCache sDiskCache = null;

	private synchronized static DiskLruCache getThumbnailDiskLruCache() {

		try {
			if (null != ThumbCacheDir) {
				sCacheDir = ThumbCacheDir;
			}

			if (sDiskCache == null || !sCacheDir.exists()) {
				sDiskCache = DiskLruCache.open(sCacheDir, 1 /* appVersion */, 1 /* valueCount */, 4 * 1024 * 1024 /* maxSize */);
			} 
		} catch (IOException e) {
            LogUtils.error("failed to open DiskLruCache: IOException", e);
		} finally {
			
		}
		
		return sDiskCache;
	}

	private static void addThumbnailToDiskCache(String key, Bitmap bitmap) {
		DiskLruCache cache = getThumbnailDiskLruCache();

		try {
			if (cache != null) {
			    LogUtils.debug("Java: addThumbnailToDiskCache() " + key);
				
				DiskLruCache.Editor editor = cache.edit(key);
				
				if (editor != null) {
					OutputStream os = editor.newOutputStream(0);
					editor.commit();
					
					LogUtils.info("bitmap before compress");
					boolean ret = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
					LogUtils.info("bitmap after compress: " + ret);
				}
				
			}

		} catch (IOException e) {
            LogUtils.error("IOException", e);
		} finally {
			
		}
	}

	private static Bitmap getThumbnailFromDiskCache(String key) {
		Bitmap bitmap = null;
		DiskLruCache cache = getThumbnailDiskLruCache();

		try {
		    LogUtils.debug("Java: getThumbnailFromDiskCache() " + key);
			if (cache != null && cache.get(key) != null) {
				DiskLruCache.Snapshot snapshot = cache.get(key);
				InputStream is = snapshot.getInputStream(0);
				bitmap = BitmapFactory.decodeStream(is);
				
				snapshot.close();
			}
		} catch (IOException e) {
            LogUtils.error("IOException", e);
		} finally {
			if (bitmap != null) {
				LogUtils.debug("bitmap is not null.");
			} else {
			    LogUtils.debug("bitmap is null.");
			}
		}

		return bitmap;
	}
	
	/**
	 * logfile 使用默认文件名 Environment.getExternalStorageDirectory() + "/pptv/tmp/outputlog.log"
	 * tempPath 使用默认路径 Environment.getExternalStorageDirectory() + "/pptv/tmp/"
	 * @return 是否成功
	 */
	public static boolean setLogPath()
    {
		String DEFAULT_LOGFILE = "/pptv/tmp/outputlog.log";
        return LogUtils.init(Environment.getExternalStorageDirectory() + DEFAULT_LOGFILE, 
        		Environment.getExternalStorageDirectory() + "/pptv/tmp/");
    }
	
    /**
     * @param logfile 调用makePlayerlog() 时生成log文件的路径
     * tempPath 使用默认路径 Environment.getExternalStorageDirectory() + "/pptv/tmp/"
     * @return 是否成功
     */
    public static boolean setLogPath(String logfile)
    {
        return LogUtils.init(logfile, Environment.getExternalStorageDirectory() + "/pptv/tmp/");
    }
    
    /**
     * @param logfile 调用makePlayerlog() 时生成log文件的路径
     * @param tempPath 保存deviceinfo, player.log 等日志文件的临时路径
     * @return
     */
    public static boolean setLogPath(String logfile, String tempPath)
    {
        return LogUtils.init(logfile, tempPath);
    }

    /** @brief 生成 播放器日志 该文件由 setLogPath() 的tempPath目录下的player.log内容而生成
     * 
     */
    public static void makePlayerlog()
    {
        LogUtils.makeUploadLog();
    }

    @Deprecated
    public static void setPlayerStatus(int code)
    {
        status = code;
    }

    @Deprecated
    public static int getPlayerStatus()
    {
        return status;
    }
    
	private MeetSDK() {}
	
	/**
	 * @param [in]in_flv 输入flv文件内容
	 * @param [in]in_size 输入flv文件大小
	 * @param [out]out_ts 输出mpegts文件内容
	 * @param process_timestamp 是否处理时间戳
	 * @param first_seg 是否是第一个分段(仅当 process_timestamp为1有效)
	 * @return
	 */
	public static native int Convert(byte[] in_flv, int in_size, byte[] out_ts, 
			int process_timestamp, int first_seg);
}
