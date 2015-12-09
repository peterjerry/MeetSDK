package com.pplive.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pplive.meetplayer.R;

public class PlayBackTime {
	private final static String TAG = "PlayBackTime";
	
	private Context mContext;
	private long mStartTimeSec = -1;
	private int mDurationSec = 60;
	private String mPlaylinkSurfix;
	
	public PlayBackTime(Context ctx) {
		mContext = ctx;
	}
	
	public long getStartTime() {
		return mStartTimeSec;
	}
	
	public int getDuration() {
		return mDurationSec;
	}
	
	public String getPlaylinkSurfix() {
		return mPlaylinkSurfix;
	}
	
	public void setLive() {
		mDurationSec = 0;
		mPlaylinkSurfix = null;
	}
	
	public void setPlaybackTime() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext); 
        View view = View.inflate(mContext, R.layout.date_time_dialog, null); 
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker); 
        final TimePicker timePicker = (TimePicker) view.findViewById(R.id.time_picker);
        final Spinner spinnerDuration = (Spinner) view.findViewById(R.id.spinner_duration);
        
        final int[] duration_min = new int[]{0, 10, 20, 30, 60, 120, 180};
        
        ArrayAdapter<CharSequence> sourceAdapter = ArrayAdapter.createFromResource(
        		mContext, R.array.duration_array, android.R.layout.simple_spinner_item);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(sourceAdapter);
        spinnerDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String name = parent.getItemAtPosition(pos).toString();
                Log.i(TAG, "onItemSelected " + name);
            }

            public void onNothingSelected(AdapterView parent) {
                Log.i(TAG, "onNothingSelected");
            }

        });
        
        builder.setView(view); 

        Calendar cal = Calendar.getInstance(); 
        timePicker.setIs24HourView(true);
        if (mStartTimeSec != -1) {
            cal.setTimeInMillis(mStartTimeSec * 1000); 
        	timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY)); 
	        timePicker.setCurrentMinute(cal.get(Calendar.MINUTE)); 
        }
        else {
        	cal.setTimeInMillis(System.currentTimeMillis()); 
            datePicker.init(
            		cal.get(Calendar.YEAR), 
            		cal.get(Calendar.MONTH), 
            		cal.get(Calendar.DAY_OF_MONTH), 
            		null);
	        timePicker.setCurrentHour(18/*cal.get(Calendar.HOUR_OF_DAY)*/); 
	        timePicker.setCurrentMinute(30/*cal.get(Calendar.MINUTE)*/); 
        }
        
        // default duration
        if (mDurationSec == 0)
        	mDurationSec = 60;
        
        int pos = -1;
    	for (int i=0;i<duration_min.length;i++) {
    		if (mDurationSec == duration_min[i]) {
    			pos = i;
    			break;
    		}
    	}
    	
    	if (pos != -1)
    		spinnerDuration.setSelection(pos);
 
        builder.setTitle("select start time"); 
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() { 

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
                
                // step1
                GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min, 0);
                mStartTimeSec = gc.getTimeInMillis() / 1000;
            	
            	// step2
                int pos = spinnerDuration.getSelectedItemPosition();
            	mDurationSec = duration_min[pos];
            	
            	Log.i(TAG, String.format("start_time %d sec, duration %d min", mStartTimeSec, mDurationSec));
            	
            	if (mDurationSec == 0) {
            		mPlaylinkSurfix = null;
            		Toast.makeText(mContext, String.format("duration is 0, toggle to LIVE mode"), 
            				Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	mPlaylinkSurfix = String.format("&begin_time=%d&end_time=%d", 
                		mStartTimeSec, mStartTimeSec + mDurationSec * 60);
                try {
                	mPlaylinkSurfix = URLEncoder.encode(mPlaylinkSurfix, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                Log.i(TAG, "Java: mPlayerLinkSurfix final: " + mPlaylinkSurfix);
                
                dialog.cancel();
                Toast.makeText(mContext, String.format("toggle to playback mode start %s, duration %d min", 
                				sb.toString(), mDurationSec), 
                		Toast.LENGTH_SHORT).show();
            } 
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mPlaylinkSurfix = null;
				mDurationSec = 0;
				Toast.makeText(mContext, String.format("toggle to LIVE mode"), 
						Toast.LENGTH_SHORT).show();
			}
        	
        });
        
        Dialog dialog = builder.create(); 
        dialog.show();
    }
	
}
