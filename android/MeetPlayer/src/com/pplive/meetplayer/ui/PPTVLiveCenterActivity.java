package com.pplive.meetplayer.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.pplive.common.pptv.CDNItem;
import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.LiveStream;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.service.MyHttpService;
import com.pplive.meetplayer.util.Util;
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
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class PPTVLiveCenterActivity extends Activity {
	private final static String TAG = "PPTVLiveCenterActivity";
	private final static int MAX_DAY = 5;
	
	private final static String live_m3u8_url_fmt = "http://%s/live/%d/%d/" + // interval/delay/
			"%s.m3u8" +
			"?type=phone.android.vip&sdk=1&channel=162&vvid=41" +
			"&k=%s"; // NOT support start_time and end_time, ONLY live
	
	private final static String ACTION_LIVE_CENTER = "live_center";
	private final static String ACTION_LIVE_FT = "live_ft";
	
	private TextView tvDay;
	private Button btnLive;
	private Button btnPlayback;
	private Button btnNextDay;
	private Button btnBwType;
	private ListView lv_tvlist;
	
	private EPGUtil mEPG;
	private String mLiveId;
	private int mBwType = 0;
	private String mLinkSurfix = null;
	private int dayOffset = 0;
	
	private MyPPTVLiveCenterAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_pptv_livecenter);
		
		this.tvDay = (TextView)this.findViewById(R.id.tv_day);
		this.btnLive = (Button)this.findViewById(R.id.btn_live);
		this.btnPlayback = (Button)this.findViewById(R.id.btn_playback);
		this.btnNextDay = (Button)this.findViewById(R.id.btn_nextday);
		this.btnBwType = (Button)this.findViewById(R.id.btn_bw_type);
		this.lv_tvlist = (ListView)this.findViewById(R.id.lv_tvlist);
		
		this.btnLive.setOnClickListener(mOnClickListener);
		this.btnPlayback.setOnClickListener(mOnClickListener);
		this.btnNextDay.setOnClickListener(mOnClickListener);
		this.btnBwType.setOnClickListener(mOnClickListener);
		
		this.lv_tvlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				LiveStream liveStrm = mAdapter.getItem(position);
				new EPGTask().execute(ACTION_LIVE_FT, liveStrm.title, liveStrm.channelID);
			}
		});
		
		this.lv_tvlist.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				LiveStream strm = mAdapter.getItem(position);
				if (strm != null)
					Toast.makeText(PPTVLiveCenterActivity.this, "channel_id: " + strm.channelID, 
							Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		mBwType = Util.readSettingsInt(this, "live_bwtype");
		
		Intent intent = getIntent();
		mLiveId = intent.getStringExtra("livecenter_id");
		
		mEPG = new EPGUtil();
		if (mLiveId == null)
			Toast.makeText(this, "live_type 未获取", Toast.LENGTH_SHORT).show();
		else
			new EPGTask().execute(ACTION_LIVE_CENTER, mLiveId, updateTime());
	}
	
	private View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			int id = v.getId();
			switch(id) {
			case R.id.btn_live:
				mLinkSurfix = null;
				Toast.makeText(PPTVLiveCenterActivity.this, "切换为 直播 模式", Toast.LENGTH_SHORT).show();
				break;
			case R.id.btn_playback:
				setPlaybackTime();
				break;
			case R.id.btn_nextday:
				dayOffset++;
				if (dayOffset > MAX_DAY)
					dayOffset = -1;
				
				new EPGTask().execute(ACTION_LIVE_CENTER, mLiveId, updateTime());
				break;
			case R.id.btn_bw_type:
				final String[] bw_type = {"P2P", "MYHTTP", "M3U8"};
				int sel = Util.readSettingsInt(PPTVLiveCenterActivity.this, "live_bwtype");

				Dialog choose_bw_type_dlg = new AlertDialog.Builder(PPTVLiveCenterActivity.this)
				.setTitle("select bw_type")
				.setSingleChoiceItems(bw_type, sel, /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							mBwType = whichButton;
							btnBwType.setText(bw_type[mBwType]);
							Util.writeSettingsInt(PPTVLiveCenterActivity.this, "live_bwtype", mBwType);
							Toast.makeText(PPTVLiveCenterActivity.this, 
									"switch bw_type to " + bw_type[mBwType], 
									Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					})
				.create();
				choose_bw_type_dlg.show();
				break;
			default:
				break;
			}
		}
		
	};
	
	private String updateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		Date today = new Date();
		c.setTime(today);
		c.add(Calendar.DAY_OF_MONTH, dayOffset);//把日期往后增加一天.整数往后推,负数往前移动 
		Date day = c.getTime();
		String strDay = sdf.format(day);
		
		SimpleDateFormat sdfWeekend = new SimpleDateFormat("E");
		String strWeekend = sdfWeekend.format(day);
		this.tvDay.setText(strDay + " " + strWeekend);
		return strDay;
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
                
                String strTime = String.format("%d-%02d-%02d %02d:%02d",
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
            		Toast.makeText(PPTVLiveCenterActivity.this, 
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
                Toast.makeText(PPTVLiveCenterActivity.this, 
                		String.format("切换为 回看 模式 开始 %s, 时长 %d min", 
                				sb.toString(), DurationSec), 
                		Toast.LENGTH_SHORT).show();
            } 
        });
        
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mLinkSurfix = null;
				Toast.makeText(PPTVLiveCenterActivity.this, 
                		String.format("切换到直播模式"), Toast.LENGTH_SHORT).show();
			}
        	
        });
        
        Dialog dialog = builder.create(); 
        dialog.show();
        return true;
    }
	
	private class EPGTask extends AsyncTask<String, Integer, Boolean> {
		private List<LiveStream> strmList;
		private List<CDNItem> itemList;
		private String title;
		private int vid;
		private String action;
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (result) {
				if (ACTION_LIVE_CENTER.equals(action)) {
					mAdapter = new MyPPTVLiveCenterAdapter(
							PPTVLiveCenterActivity.this, mEPG.getLiveStrm());
					lv_tvlist.setAdapter(mAdapter);
				}
				else if (ACTION_LIVE_FT.equals(action)) {
					int best_ft = 0;
					int index = 0;
					int count = itemList.size();
					for (int i = 0;i<count;i++) {
						CDNItem item = itemList.get(i);
						int ft = Integer.valueOf(item.getFT());
						if (ft > best_ft) {
							best_ft = ft;
							index = i;
						}
					}
					
					CDNItem LiveItem = itemList.get(index);
					String play_url = null;
					
					if (mBwType == 0 || mBwType == 1) {
						short http_port = MediaSDK.getPort("http");
						if (mBwType == 1)
							http_port = (short)MyHttpService.getPort();
						
						play_url = PlayLinkUtil.getPlayUrl(
								vid, http_port, best_ft/*ft*/, 3/*bw_type*/, mLinkSurfix);
					}
					else if (mBwType == 2) {
						play_url = String.format(live_m3u8_url_fmt, 
								LiveItem.getHost(), 
								LiveItem.getInterval(), LiveItem.getDelay(), 
								LiveItem.getRid(), LiveItem.getKey());
					}
					else {
						Toast.makeText(PPTVLiveCenterActivity.this, "invalid bw_type " + mBwType, Toast.LENGTH_SHORT).show();
						return;
					}
					
					Intent intent = new Intent(PPTVLiveCenterActivity.this,
			        		PPTVPlayerActivity.class);
					Uri uri = Uri.parse(play_url);
					Log.i(TAG, "to play uri: " + uri.toString());

					intent.setData(uri);
					intent.putExtra("title", title);
					intent.putExtra("ft", best_ft);
					intent.putExtra("best_ft", best_ft);
					if (mLinkSurfix == null)
						intent.putExtra("do_retry", 1);
					
					Toast.makeText(PPTVLiveCenterActivity.this, 
							String.format("start to play %s, playlink %d, ft %d, size %d x %d, bitrate %d",
									title, vid, best_ft, 
									LiveItem.getWidth(), LiveItem.getHeight(), LiveItem.getBitrate()),
							Toast.LENGTH_SHORT).show();
			        
					startActivity(intent);
				}
			}
			else {
				Toast.makeText(getApplicationContext(),"获取列表失败", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			if (params.length < 3)
				return false;
			
			action = params[0];
			if (ACTION_LIVE_CENTER.equals(action)) {
				return mEPG.live_center(params[1], params[2]);
			}
			else if (ACTION_LIVE_FT.equals(action)) {
				title = params[1];
				
				vid = Integer.valueOf(params[2]);
				return ((itemList = mEPG.live_cdn(vid)) != null);
			}
			else {
				Log.e(TAG, "Java: unknown action: " + action);
				return false;
			}
		}
	}
}
