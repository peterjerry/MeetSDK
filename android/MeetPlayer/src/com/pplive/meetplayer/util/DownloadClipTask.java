package com.pplive.meetplayer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.util.Log;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.db.MediaStoreDatabaseHelper;
import com.pplive.meetplayer.R;
import com.pplive.sdk.MediaSDK;

/**
 * @param[0] playlink
 * @param[1] ft 码流
 * @param[2] savepath 保存路径
 *
 */
public class DownloadClipTask extends AsyncTask<String, Integer, Boolean> {
	private static final String TAG = "DownloadClipTask";
	
	private Context mContext;
	private boolean mIsP2PDownload = false;
	protected String mTitle;
	private String mPlaylink;
	private String mFt;
	protected String mSavePath;
	protected long mFileSize = 0L;
	protected long mDownloadedSize = 0L;
	private boolean interrupted = false;
	
	private EPGUtil mEPG;
	
	public DownloadClipTask(Context ctx, String title, boolean isP2P) {
		mContext 		= ctx;
		mTitle			= title;
		mIsP2PDownload 	= isP2P;
		
		mEPG = new EPGUtil();
	}
	
	public void interrupt() {
		interrupted = true;
	}
	
	private void saveMedia() {
		MediaInfo info = MeetSDK.getMediaDetailInfo(mSavePath);
		if (info != null) {
			MediaStoreDatabaseHelper db = MediaStoreDatabaseHelper.getInstance(mContext);
			db.saveMediaInfo(mSavePath, mTitle, info);
		}
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		if (params.length < 3) {
			Log.e(TAG, "invalid param count: " + params.length);
			return false;
		}
		
		mPlaylink		= params[0];
		mFt				= params[1];
		mSavePath		= params[2];
		
		publishProgress(0, 0);

		if (mIsP2PDownload) {
			//playcode:ppvod2:///23832333?ft=1&bwtype=3&platform=android3
			//&type=phone.android.download.vip&sv=4.1.3&p2p.source=7&bighead=true&p2p.level=1
			String playcode = String.format("%s?ft=%s&bwtype=3&platform=android3" +
					"&type=phone.android.download.vip&sv=4.1.3", mPlaylink, mFt);
			playcode = "ppvod2:///" + playcode + "&p2p.source=7&bighead=true&p2p.level=1";
			Log.i(TAG, String.format("Java: playcode %s, mSavePath %s", playcode, mSavePath));
			// TODO Auto-generated method stub
			long handle = -1;
			try {
				handle = MediaSDK.downloadOpen(playcode, "mp4", mSavePath, 
					new MediaSDK.Download_Callback() {

						@Override
						public void invoke(long result) {
							// TODO Auto-generated method stub
							Log.i(TAG, "Java: MediaSDK invoke " + result);
							
							/**
	                         * sdk 正常关闭回调，handle <= 0不回调
	                         * 0：成功
	                         * 5：取消操作，调用了close
	                         */
						}
				
				});
			}
			catch (Throwable e) {
	            e.printStackTrace();
	            return false;
	        }
			
			// open失败
	        if (handle == 0 || handle == -1) {
	        	Log.e(TAG, "Java: failed to open download session");
	            return false;
	        }
	        
	        Log.i(TAG, "Java: download handle: " + handle);
			
			while (true) {
	            MediaSDK.Download_Statistic stat = new MediaSDK.Download_Statistic();
	            long resultCode = -1;
	            try {
	                resultCode = MediaSDK.getDownloadInfo(handle, stat);
	                Log.i(TAG, String.format("Java: download stat: %d/%d, speed %d kB/s", 
	                		stat.finish_size, stat.total_size, stat.speed / 1024));
	                if (stat.total_size > 0 && stat.speed > 0)
	                	publishProgress((int)(stat.finish_size * 100 / stat.total_size), stat.speed / 1024);
	            }
	            catch (Throwable e) {
	                // Log.v(TAG, "getDownloadInfo");
	                // addLog("getDownloadInfo\n");
	                e.printStackTrace();
	                MediaSDK.downloadClose(handle);
	                return false;
	            }
	            
	            if (resultCode != 0) {
	                // 下载出错
	                Log.e(TAG, "Java: download error: " + resultCode);
	                MediaSDK.downloadClose(handle);
	                return false;
	            }
	            
	            if (stat.total_size > 0 && stat.finish_size >= stat.total_size) {
	            	Log.i(TAG, String.format("Java: download done! %s %s", 
	            			stat.finish_size, stat.total_size));
	            	break;
	            }
	            
	            if (stat.total_size > 0 && mFileSize == 0)
	            	mFileSize = stat.total_size;
	            if (stat.finish_size > 0)
	            	mDownloadedSize = stat.finish_size;
	            
	            if (interrupted) {
					Log.w(TAG, "interrupted by user");
					MediaSDK.downloadClose(handle);
					File f = new File(mSavePath);
					f.delete();
					return false;
				}
	            
	            try {
	                Thread.sleep(300);
	            }
	            catch (InterruptedException e) {
	                
	            }
			}
			
			MediaSDK.downloadClose(handle);
			saveMedia();
			
			return true;
		}
		else {
			String download_url = mEPG.getCDNUrl(mPlaylink, mFt, false, false);
			if (download_url == null) {
				Log.e(TAG, "failed to get cdn url");
				return false;
			}
			
			URL url = null;
			try {
				url = new URL(download_url);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			try {
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setReadTimeout(3000);

                // 2016.1.18 Michael.Ma added to fix cannot get Content-Length problem
                //conn.setRequestProperty("RANGE", "bytes=0-");

				InputStream inStream = conn.getInputStream();
				FileOutputStream fs = new FileOutputStream(mSavePath);

                mFileSize = -1;
                if (conn.getHeaderField("Content-Length") != null)
				    mFileSize = Long.parseLong(conn.getHeaderField("Content-Length"));
				
				int byteread = 0;
				byte[] buffer = new byte[1024];
				
				long total_start = System.currentTimeMillis();
				long start = total_start;
				while ((byteread = inStream.read(buffer)) != -1) {
					mDownloadedSize += byteread;
					fs.write(buffer, 0, byteread);
					
					if (interrupted) {
						Log.w(TAG, "interrupted by user");
						File f = new File(mSavePath);
						f.delete();
						return false;
					}
					
					long curr = System.currentTimeMillis();
					if (curr - start > 500) {
						int speed = (int)(mDownloadedSize / (curr - total_start));
                        int progress = 0; // 0 - 100
                        if (mFileSize != -1)
                            progress = (int)(mDownloadedSize * 100 / mFileSize);
						publishProgress(progress, speed/* kB/sec */);
						start = curr;
					}
				}

				Log.i(TAG, "Java: total file size: " + mDownloadedSize);
				saveMedia();
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		return false;
	}
	
}
