package so.cym.crashhandlerdemo;

import java.io.File;


import com.gotye.meetplayer.MeetApplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadLogTask extends AsyncTask<String, Integer, String> {
	private final static String TAG = "UploadLogTask";
	
	private Context mContext;
	private OnTask mOnTaskListener;
	
	public UploadLogTask(Context ctx) {
		this.mContext = ctx;
	}
	
	public interface OnTask {
		abstract void onFinished(); 
	};
	
	public void setOnTaskListener(OnTask listener) {
		mOnTaskListener = listener;
	}
	
	@Override
	protected void onPostExecute(String result) {
		// TODO Auto-generated method stub
		if (result != null) {
			Toast.makeText(mContext, 
					"成功将崩溃信息 " + result + " 发送到服务器，感谢您的反馈", 
					Toast.LENGTH_SHORT).show();
			if (mOnTaskListener != null)
				mOnTaskListener.onFinished();
		}
		else {
			Toast.makeText(mContext, "发送崩溃信息失败", Toast.LENGTH_SHORT).show();
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
			log_filename = UploadUtil2.uploadFile(f, desc,  
					MeetApplication.UPLOAD_URL);
			if (log_filename != null) {
				f.delete();
				break;
			}
			
			retry--;
		}

		if (log_filename != null)
			Log.i(TAG, "Java: upload crash log " + log_filename + " successfully");
		
		return log_filename;
		
		/*try {
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
    	}*/
	}
		
}
