package com.pplive.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import libcore.io.DiskLruCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class PicCacheUtil {
	private final static String TAG = "PicCacheUtil";
	
	private static Context sContext = null;
	private static File ThumbCacheDir = null; 

	private static File sCacheDir = new File(Environment.getExternalStorageDirectory(), "pptv" + File.separator + ".thumbnails");
	private static DiskLruCache sDiskCache = null;
	
	public void setContext(Context ctx) {
		sContext = ctx;
	}
	
	private File getDiskCacheDir(String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = sContext.getExternalCacheDir().getPath() + "/pptv" + File.separator + ".thumbnails";
		} else {
			cachePath = sContext.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}
	
	public static String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	
	public synchronized static DiskLruCache getThumbnailDiskLruCache() {

		try {
			if (null != ThumbCacheDir) {
				sCacheDir = ThumbCacheDir;
			}

			if (sDiskCache == null || !sCacheDir.exists()) {
				sDiskCache = DiskLruCache.open(sCacheDir, 
						1 /* appVersion */,
						1 /* valueCount */,
						4 * 1024 * 1024 /* maxSize */);
			} 
		} catch (IOException e) {
            Log.e(TAG, "IOException" + e);
		} finally {
			
		}
		
		return sDiskCache;
	}

	public static void addThumbnailToDiskCache(String key, Bitmap bitmap) {
		DiskLruCache cache = getThumbnailDiskLruCache();

		try {
			if (cache != null) {
			    Log.d(TAG, "addThumbnailToDiskCache");
				
				DiskLruCache.Editor editor = cache.edit(key);
				
				if (editor != null) {
					OutputStream os = editor.newOutputStream(0);
					editor.commit();
					
					boolean ret = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
					Log.d(TAG, "bitmap compress: " + ret);
				}
				
			}

		} catch (IOException e) {
			Log.e(TAG, "IOException" + e);
		} finally {
			
		}
	}

	public static Bitmap getThumbnailFromDiskCache(String key) {
		Bitmap bitmap = null;
		DiskLruCache cache = getThumbnailDiskLruCache();

		try {
			Log.d(TAG, "getThumbnailFromDiskCache");
			if (cache != null && cache.get(key) != null) {
				DiskLruCache.Snapshot snapshot = cache.get(key);
				InputStream is = snapshot.getInputStream(0);
				bitmap = BitmapFactory.decodeStream(is);
				
				snapshot.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "IOException" + e);
		} finally {
			if (bitmap != null) {
				Log.d(TAG, "bitmap is not null.");
			} else {
				Log.d(TAG, "bitmap is null.");
			}
		}

		return bitmap;
	}
}
