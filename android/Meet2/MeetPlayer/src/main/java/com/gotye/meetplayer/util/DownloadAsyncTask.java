package com.gotye.meetplayer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadAsyncTask extends AsyncTask<String, Integer, Boolean> {
	
	private static final String TAG = "DownloadAsyncTask";
	
	protected static final String MSG_DOWNLOAD_SUCCESS = "Download Success!!!";
	protected static final String MSG_DOWNLOAD_FAILED = "Download Failed!!!";
	
	@Override
	protected Boolean doInBackground(String... params) {
		Log.i(TAG, "doInBackground");
	
		String url = params[0];
		String path = params[1];
		
		boolean ret = false;
		OutputStream os = null;
		InputStream is = null;

		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = null;

		File file = new File(path);
		if (file.getParentFile().exists() || !file.getParentFile().exists()
				&& file.getParentFile().mkdirs()) {
			try {
				// File Stream
				os = new FileOutputStream(file);

				// Http
				httpGet = new HttpGet(url);
				HttpResponse rep = client.execute(httpGet);

				int status = rep.getStatusLine().getStatusCode();

				if (status >= HttpStatus.SC_OK
						&& status <= HttpStatus.SC_MULTIPLE_CHOICES) {
					
					HttpEntity entity = rep.getEntity();
					is = entity.getContent();
					
					long totalSize = entity.getContentLength();
					long totalReadSize = 0;

					byte[] buf = new byte[1024 * 1024]; // buffer size: 1MB;
					int readSize = 0;
					while ((readSize = is.read(buf)) > 0) {
						os.write(buf, 0, readSize);
						totalReadSize += readSize;
						publishProgress((int)((totalReadSize * 100) / totalSize));
					}

					ret = true;
				} else {
					Log.e(TAG, rep.getStatusLine().toString());
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.toString());
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			} finally {
				try {
					if (os != null) {
						os.close();
						os = null;
					}

					if (is != null) {
						is.close();
						os = null;
					}
				} catch (IOException e) {
					Log.w(TAG, e.toString());
				}

				// Clean
				if (!ret && file != null) {
					file.delete();
				}
				
				client.getConnectionManager().shutdown();
				
				Log.i(TAG, ret ? MSG_DOWNLOAD_SUCCESS : MSG_DOWNLOAD_FAILED);
			}
		}

		return ret;
	}

}
