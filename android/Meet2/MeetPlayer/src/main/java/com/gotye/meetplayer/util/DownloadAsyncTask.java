package com.gotye.meetplayer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

import com.gotye.common.util.LogUtil;

public class DownloadAsyncTask extends AsyncTask<String, Integer, Boolean> {

    private static final String TAG = "DownloadAsyncTask";

    protected static final String MSG_DOWNLOAD_SUCCESS = "download successfully";
    protected static final String MSG_DOWNLOAD_FAILED = "failed to download!!!";

    private boolean mbDownloadFile = true;
    protected boolean mInterrupted = false;

    public void interrupt() {
        mInterrupted = true;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        LogUtil.info(TAG, "doInBackground");

        String url = params[0];
        String path = params[1];

        boolean ret = false;
        int bytesum = 0;
        int byteread = 0;

        File file = null;
        FileOutputStream fs = null;
        InputStream inStream = null;

        URL httpUrl = null;
        try {
            httpUrl = new URL(url);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setInstanceFollowRedirects(true);

            inStream = conn.getInputStream();
            file = new File(path);
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                LogUtil.error(TAG, "cannot create parent folder: " + file.getParentFile().getAbsolutePath());
                return false;
            }

            fs = new FileOutputStream(file);

            long totalSize = Long.parseLong(conn.getHeaderField("Content-Length"));

            long total_start_time = System.currentTimeMillis();
            long start_time = System.currentTimeMillis();

            byte[] buffer = new byte[4096];
            while ((byteread = inStream.read(buffer)) != -1) {
                if (mInterrupted) {
                    LogUtil.warn(TAG, "interrupted by user");
                    break;
                }

                bytesum += byteread;
                fs.write(buffer, 0, byteread);

                // calc speed
                long current_time = System.currentTimeMillis();
                long elapsed_time = current_time - start_time;

                if (elapsed_time > 1000) { // 1sec
                    long total_elapsed_time = current_time - total_start_time;
                    int speed = (int)(bytesum / total_elapsed_time); // kB/s
                    int pct = (int)((bytesum * 100) / totalSize);
                    publishProgress(pct, speed);

                    start_time = current_time;
                }
            }

            ret = !mInterrupted;

            if (!mInterrupted)
                LogUtil.info(TAG, "Java: total file size: " + bytesum);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                LogUtil.info(TAG, "before close inStream");
                if (inStream != null) {
                    inStream.close();
                }

                LogUtil.info(TAG, "before close fs");
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                LogUtil.info(TAG, "after close fs");
            } catch (IOException e) {
                LogUtil.warn(TAG, "IOException: " + e.toString());
            }

            // Clean
            if (!ret && file != null) {
                LogUtil.info(TAG, "delete interrupted download file: " + file.getAbsolutePath());
                file.delete();
            }

            //client.getConnectionManager().shutdown();
        }

        LogUtil.info(TAG, "task end");
        return ret;
    }

}
