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

	public static final int LEVEL_SYSTEM					= 1;
	public static final int LEVEL_SOFTWARE_SD				= 2;
	public static final int LEVEL_SOFTWARE_HD1			= 3;
	public static final int LEVEL_SOFTWARE_HD2			= 4;
	public static final int LEVEL_SOFTWARE_BD				= 5;
	
	@Deprecated
	// use LEVEL_SYSTEM
	public static final int LEVEL_HARDWARE				= 1;
	@Deprecated
	// use LEVEL_SOFTWARE_SD
	public static final int LEVEL_SOFTWARE_LIUCHANG		= 2;
	@Deprecated
	// use LEVEL_SOFTWARE_HD1
	public static final int LEVEL_SOFTWARE_GAOQING		= 3;
	@Deprecated
	// use LEVEL_SOFTWARE_HD2
	public static final int LEVEL_SOFTWARE_CHAOQING		= 4;
	@Deprecated
	// use LEVEL_SOFTWARE_BD
	public static final int LEVEL_SOFTWARE_LANGUANG		= 5;
	
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
		
		boolean retPlayer = FFMediaPlayer.initPlayer(path);
		boolean retParser = SimpleSubTitleParser.initParser(path);
		return (retPlayer && retParser);
	}

	public static boolean checkCompatibility(Surface surface) {
		return checkCompatibility(COMPATIBILITY_HARDWARE_DECODE, surface);
	}

	public static boolean checkCompatibility(int checkWhat, Surface surface) {
		if (AppRootDir == null) {
		    LogUtils.error("MeetSDK.AppRootDir is null.");
			throw new IllegalArgumentException("MeetSDK.AppRootDir is null.");
		}
		
		return false;
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
		/*
		if (holder == null || uri == null) {

			return false;
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

		boolean isOMXSurface = isOMXSurface(path);
		setSurfaceType(holder, isOMXSurface);
		return isOMXSurface;
		*/
	}

	@Deprecated
	public interface SurfaceTypeDecider {
		public boolean isOMXSurface();
	}

	@Deprecated
	public static void setSurfaceType(SurfaceHolder holder, SurfaceTypeDecider decider) {
		setSurfaceType(holder, decider.isOMXSurface());
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
			bitmap = getThumbnailFromDiskCache(UrlUtil.encode(filePath));
			if (bitmap != null) {
				
				return bitmap;
			} else {
				bitmap = Build.VERSION.SDK_INT >= 8 ? 
						ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MICRO_KIND) : null;
				
				bitmap = bitmap == null ?
						MeetPlayerHelper.createVideoThumbnail(filePath, kind) : bitmap;
			}
			
			if (bitmap != null) {
				addThumbnailToDiskCache(UrlUtil.encode(filePath), bitmap);
			}
		} catch (LinkageError e) {
			LogUtils.error("LinkageError", e);
		}

		return bitmap;
	}
	
	public synchronized static Bitmap createImageThumbnail(ContentResolver cr, int origId, String filePath, int kind) {

        Bitmap bitmap = null;

        bitmap = getThumbnailFromDiskCache(UrlUtil.encode(filePath));
        if (null != bitmap) {
            return bitmap;
        } 
        
        bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr, origId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
        if (null != bitmap) {
            addThumbnailToDiskCache(UrlUtil.encode(filePath), bitmap);
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
            LogUtils.error("IOException", e);
		} finally {
			
		}
		
		return sDiskCache;
	}

	private static void addThumbnailToDiskCache(String key, Bitmap bitmap) {
		DiskLruCache cache = getThumbnailDiskLruCache();

		try {
			if (cache != null) {
			    LogUtils.debug("addThumbnailToDiskCache");
				
				DiskLruCache.Editor editor = cache.edit(key);
				
				if (editor != null) {
					OutputStream os = editor.newOutputStream(0);
					editor.commit();
					
					boolean ret = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
					LogUtils.debug("bitmap compress: " + ret);
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
		    LogUtils.debug("getThumbnailFromDiskCache");
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
	
	public static boolean setLogPath()
    {
		String DEFAULT_LOGFILE = "/pptv/tmp/outputlog.log";
        return LogUtils.init(Environment.getExternalStorageDirectory() + DEFAULT_LOGFILE, 
        		Environment.getExternalStorageDirectory() + "/pptv/tmp/");
    }
	
    public static boolean setLogPath(String logfile)
    {
        return LogUtils.init(logfile, Environment.getExternalStorageDirectory() + "/pptv/tmp/");
    }
    
    public static boolean setLogPath(String logfile, String tempPath)
    {
        return LogUtils.init(logfile, tempPath);
    }

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
}
