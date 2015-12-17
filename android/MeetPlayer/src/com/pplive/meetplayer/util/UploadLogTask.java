package com.pplive.meetplayer.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.pplive.common.util.LogUtil;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class UploadLogTask extends AsyncTask<String, Integer, Boolean> {
	private final static String TAG = "UploadLogTask";
	
	private Context mContext;
	
	public UploadLogTask(Context ctx) {
		this.mContext = ctx;
	}
	@Override
	protected void onPostExecute(Boolean result) {
		// TODO Auto-generated method stub
		if (result) {
			Toast.makeText(mContext, "log uploaded to iloveaya", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		// TODO Auto-generated method stub
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
			String filename = "meetplayer_" + df.format(new Date()) + ".zip";
			LogUtil.info(TAG, "Java: log zipfile name: " + filename);
			
			LogcatHelper.getInstance().zipLogFiles(filename);
			
			HttpPostUtil u = new HttpPostUtil("http://www.iloveyaya.zz.vc/upload.php");
			u.addFileParameter(
					"file", 
					new File(mContext.getCacheDir() + File.separator + filename));
			u.addTextParameter("tag", "chinese");
			byte[] b = u.send();
			String result = new String(b);
			LogUtil.info(TAG, "Java: HttpPostUtil result: " + result);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
    			e.printStackTrace();
    	}
			
		return false;
	}
		
}
