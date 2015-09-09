package com.pplive.meetplayer.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.R;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

public class PPTVLiveActivity extends Activity {
	private final static String TAG = "PPTVLiveActivity";
	private Button btnLive;
	private Button btnPlayback;
	private ListView lv_tvlist;
	
	private EPGUtil mEPG;
	private List<PlayLink2>mEPGLinkList;
	private String mLinkSurfix = null;
	
	private MyPPTVLiveAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_pptv_live);
		
		this.btnLive = (Button)this.findViewById(R.id.btn_live);
		this.btnPlayback = (Button)this.findViewById(R.id.btn_playback);
		this.lv_tvlist = (ListView)this.findViewById(R.id.lv_tvlist);
		
		this.btnLive.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mLinkSurfix = null;
				Toast.makeText(PPTVLiveActivity.this, "切换为 直播 模式", Toast.LENGTH_SHORT).show();
			}
		});
		
		this.btnPlayback.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setPlaybackTime();
			}
		});
		
		this.lv_tvlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				PlayLink2 link = mAdapter.getItem(position);
				String playlink = link.getId();
				short http_port = MediaSDK.getPort("http");
				String play_url = PlayLinkUtil.getPlayUrl(
						Integer.valueOf(playlink), http_port, 1, 3, mLinkSurfix);
				
				Intent intent = new Intent(PPTVLiveActivity.this,
		        		PPTVPlayerActivity.class);
				Uri uri = Uri.parse(play_url);
				Log.i(TAG, "to play uri: " + uri.toString());

				intent.setData(uri);
				intent.putExtra("title", link.getTitle());
				//intent.putExtra("ft", 1);
				//intent.putExtra("best_ft", 3);
		        
				startActivity(intent);
			}
		});
		
		Intent intent = getIntent();
		int live_type = intent.getIntExtra("live_type", 0);
		
		mEPG = new EPGUtil();
		if (live_type == 0)
			Toast.makeText(this, "live_type 未获取", Toast.LENGTH_SHORT).show();
		else
			new EPGTask().execute(live_type);
	}
	
	private boolean setPlaybackTime() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.date_time_dialog, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
        final TimePicker timePicker = (android.widget.TimePicker) view.findViewById(R.id.time_picker);
        final EditText etDuration = (EditText) view.findViewById(R.id.et_duration);
        builder.setView(view); 

        Calendar cal = Calendar.getInstance(); 
        cal.setTimeInMillis(System.currentTimeMillis()); 
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null); 

        timePicker.setIs24HourView(true); 
        timePicker.setCurrentHour(18/*cal.get(Calendar.HOUR_OF_DAY)*/); 
        timePicker.setCurrentMinute(30/*cal.get(Calendar.MINUTE)*/); 
 
        builder.setTitle("选择开始时间"); 
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() { 

            @Override 
            public void onClick(DialogInterface dialog, int which) { 

            	int year, month, day, hour, min;
            	year = datePicker.getYear();
            	month = datePicker.getMonth();
            	day  = datePicker.getDayOfMonth();
            	hour = timePicker.getCurrentHour();
            	min = timePicker.getCurrentMinute();
            	
            	String strHour = String.format("%02d", hour);
                String strMin = String.format("%02d", min);
                
            	StringBuffer sb = new StringBuffer(); 
                sb.append(String.format("%d-%02d-%02d",  
                        year, month, day)); 
                sb.append(" ");
                sb.append(strHour).append(":").append(strMin); 
                
                String strTime;
                strTime = String.format("%d-%02d-%02d %02d:%02d",
                		datePicker.getYear(),
                        datePicker.getMonth(), 
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());

                long StartTimeSec;
                int DurationSec;
                // step1
                GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min, 0);
                StartTimeSec = gc.getTimeInMillis() / 1000;
            	
            	// step2
            	String strDuration =  etDuration.getText().toString();
            	DurationSec = Integer.parseInt(strDuration);
            	
            	if (DurationSec == 0) {
            		mLinkSurfix = null;
            		Toast.makeText(PPTVLiveActivity.this, 
                    		String.format("时长为0，切换到直播模式"), Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	Log.i(TAG, String.format("start_time %d sec, duration %d min", StartTimeSec, DurationSec));
            	
            	mLinkSurfix = String.format("&begin_time=%d&end_time=%d", 
                		StartTimeSec, StartTimeSec + DurationSec * 60);
                try {
                	mLinkSurfix = URLEncoder.encode(mLinkSurfix, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                Log.i(TAG, "Java: mPlayerLinkSurfix final: " + mLinkSurfix);
            	
                dialog.cancel();
                Toast.makeText(PPTVLiveActivity.this, 
                		String.format("切换为 回看 模式 开始 %s, 时长 %d min", 
                				sb.toString(), DurationSec), 
                		Toast.LENGTH_SHORT).show();
            } 
        });
        
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mLinkSurfix = null;
				Toast.makeText(PPTVLiveActivity.this, 
                		String.format("切换到直播模式"), Toast.LENGTH_SHORT).show();
			}
        	
        });
        
        Dialog dialog = builder.create(); 
        dialog.show();
        return true;
    }
	
	private class EPGTask extends AsyncTask<Integer, Integer, List<PlayLink2>> {
		
		@Override
		protected void onPostExecute(List<PlayLink2> result) {
			// TODO Auto-generated method stub
			if (result == null) {
				Toast.makeText(getApplicationContext(),"获取列表失败", Toast.LENGTH_SHORT).show();
			}
			else {
				mAdapter = new MyPPTVLiveAdapter(PPTVLiveActivity.this, result);
				lv_tvlist.setAdapter(mAdapter);
			}
		}
		
		@Override
		protected List<PlayLink2> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			if (!mEPG.live(1, 200, params[0]))
				return null;
			
			else
				return mEPG.getLink();
		}
	}
}
