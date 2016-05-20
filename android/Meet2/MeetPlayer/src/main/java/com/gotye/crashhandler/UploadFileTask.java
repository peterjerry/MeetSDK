package com.gotye.crashhandler;

import android.content.Context;

import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.MeetApplication;

import java.io.File;

public class UploadFileTask extends UploadLogTask {
	private final static String TAG = "UploadFileTask";

	public UploadFileTask(Context ctx) {
		super(ctx);
	}

    @Override
    protected void onPostExecute(String result) {
        if (mListener != null) {
            if (result != null)
                mListener.onFinished("成功将dump文件 " + result + " 发送到服务器，感谢您的反馈", 0);
            else
                mListener.onError("发送崩溃信息失败", -1);
        }
    }

    @Override
	protected String doInBackground(String... params) {
        String filename = params[0];
        File f = new File(filename);

        int retry = 3;
        String save_filename = null;
        while (retry > 0) {
            save_filename = UploadUtil.uploadFile(f, MeetApplication.UPLOAD_DUMP_URL);
            if (save_filename != null) {
                f.delete();
                LogUtil.info(TAG, "crash zip file " + f.getName() + " was deleted");
                break;
            }

            retry--;
        }

        return save_filename;
	}
}
