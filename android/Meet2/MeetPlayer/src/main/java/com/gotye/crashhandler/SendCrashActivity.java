package com.gotye.crashhandler;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

/**
 * 
 * @author hzcaoyanming
 *
 * 发送crash的activity。该activity是在崩溃后自动重启的。
 */
public class SendCrashActivity extends AppCompatActivity
		implements UploadLogTask.TaskListener {

	private static final String TAG = "SendCrashActivity";

	private Button mBtnSendCrash;
	private EditText mEtDesc;

    private ProgressDialog mProgDlg;
	
	/**
	 * localFileUrl
	 * 本地log文件的存放地址
	 */
	private static String localFileUrl = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_crash);

		this.mEtDesc = (EditText) this.findViewById(R.id.et_crashinfo);
	}

	public void sendCrash(View view){
		String desc = mEtDesc.getText().toString();
		if (desc.isEmpty())
			desc = "通用错误";

        mProgDlg = new ProgressDialog(this);
        mProgDlg.setTitle("系统管理");
        mProgDlg.setMessage("日志上传中...");
		mProgDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgDlg.setCancelable(true);
        mProgDlg.show();

		UploadLogTask task = new UploadLogTask(this);
		task.setOnTaskListener(this);
		task.execute(Util.upload_log_path, desc);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send_crash, menu);
		return true;
	}

	@Override
	public void onFinished(String msg, int code) {
        if (mProgDlg != null)
            mProgDlg.dismiss();

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
	}

	@Override
	public void onEror(String msg, int code) {
        if (mProgDlg != null)
            mProgDlg.dismiss();

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}
