package so.cym.crashhandlerdemo;

import so.cym.crashhandlerdemo.UploadLogTask.OnTask;

import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SendCrashActivity extends Activity implements OnTask {

	private static final String TAG = "SendCrashActivity";
	
	private Button mBtnSendCrash;
	private EditText mEtDesc;
	
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
		
		UploadLogTask task = new UploadLogTask(this);
		task.setOnTaskListener(this);
		task.execute(Util.upload_log_path, desc);
	}

	@Override
	public void onFinished() {
		// TODO Auto-generated method stub
		finish();
	}
	
}
