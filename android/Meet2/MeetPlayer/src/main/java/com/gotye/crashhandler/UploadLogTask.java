package com.gotye.crashhandler;

import java.io.File;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.gotye.meetplayer.MeetApplication;

public class UploadLogTask extends AsyncTask<String, Integer, String> {
	private final static String TAG = "UploadLogTask";
	
	protected Context mContext;
	protected TaskListener mListener;
	
	public UploadLogTask(Context ctx) {
		this.mContext = ctx;
	}
	
	public interface TaskListener {
		void onFinished(String msg, int code);

		void onEror(String msg, int code);
	}
	
	public void setOnTaskListener(TaskListener listener) {
		mListener = listener;
	}

	@Override
	protected void onPostExecute(String result) {
		// TODO Auto-generated method stub
        //mProgDlg.dismiss();
        if (mListener != null) {
            if (result != null)
                mListener.onFinished("成功将崩溃信息 " + result + " 发送到服务器，感谢您的反馈", 0);
            else
                mListener.onEror("发送崩溃信息失败", -1);
        }
	}
	
	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Java: begin to send crash log to server");
		
		String filename = params[0];
		String desc = params[1];
		File f = new File(filename);
		int retry = 3;
		String log_filename = null;
		while (retry > 0) {
			log_filename = UploadUtil2.uploadFile(f, desc, MeetApplication.UPLOAD_CRASH_LOG_URL);
			if (log_filename != null) {
				f.delete();
				break;
			}
			
			retry--;
		}

		if (log_filename != null)
			Log.i(TAG, "Java: upload crash log " + log_filename + " successfully");
		
		return log_filename;
	}
		
}
