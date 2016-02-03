package com.gotye.meetplayer.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.gotye.meetplayer.R;

public class TimePickerActivity extends Activity implements OnTouchListener {
	private EditText etStartTime; 
    private EditText etDuration; 
    private Button btnBack;
    private long start_time_sec;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timepicker);
        
        Intent intent = getIntent();
        
        etStartTime = (EditText) this.findViewById(R.id.et_start_time); 
        etDuration = (EditText) this.findViewById(R.id.et_duration); 
        btnBack = (Button) this.findViewById(R.id.btn_back);
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); // yyyy-MM-dd HH:mm
        Date curDate = new Date(System.currentTimeMillis());
        String strDay = formatter.format(curDate);
        SharedPreferences sharedata = getSharedPreferences("last_select_time", 0);  
        int last_hour = sharedata.getInt("hour", 18);
        int last_min = sharedata.getInt("min", 30);
        String strTime = String.format(" %02d:%02d", last_hour, last_min);
        etStartTime.setText(strDay + strTime/*2014-11-08 17:12"*/);
           
        etStartTime.setOnTouchListener(this); 
        etDuration.setOnTouchListener(this); 
        btnBack.setOnTouchListener(this); 
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN) { 
			   
            AlertDialog.Builder builder = new AlertDialog.Builder(this); 
            View view = View.inflate(this, R.layout.date_time_dialog, null); 
            final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker); 
            final TimePicker timePicker = (android.widget.TimePicker) view.findViewById(R.id.time_picker); 
            builder.setView(view); 
   
            Calendar cal = Calendar.getInstance(); 
            cal.setTimeInMillis(System.currentTimeMillis()); 
            datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null); 
   
            timePicker.setIs24HourView(true);
            SharedPreferences sharedata = getSharedPreferences("last_select_time", 0);  
            int last_hour = sharedata.getInt("hour", 18);
            int last_min = sharedata.getInt("min", 30);
            timePicker.setCurrentHour(last_hour/*cal.get(Calendar.HOUR_OF_DAY)*/); 
            timePicker.setCurrentMinute(last_min/*cal.get(Calendar.MINUTE)*/); 
   
            if (v.getId() == R.id.et_start_time) { 
                final int inType = etStartTime.getInputType(); 
                etStartTime.setInputType(InputType.TYPE_NULL); 
                etStartTime.onTouchEvent(event); 
                etStartTime.setInputType(inType); 
                etStartTime.setSelection(etStartTime.getText().length()); 
                   
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
                    	
                    	// save hour and minute
                    	SharedPreferences.Editor sharedata = getSharedPreferences("last_select_time", 0).edit();  
                    	sharedata.putInt("hour", hour);
                    	sharedata.putInt("min", min);
                    	sharedata.commit();
                    	
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
   
                        etStartTime.setText(sb); 
                        etDuration.requestFocus(); 
                        
                        GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min, 0);
                    	start_time_sec = gc.getTimeInMillis() / 1000;
                           
                        dialog.cancel(); 
                    } 
                }); 
                
                Dialog dialog = builder.create(); 
                dialog.show(); 
                return true;
            }
            
            if (v.getId() == R.id.btn_back) {
            	Intent intent = getIntent();
            	intent.putExtra("start_time", etStartTime.getText().toString());
            	intent.putExtra("start_time_sec", start_time_sec);
            	
            	int duration;
            	String strDuration = etDuration.getText().toString();
            	duration = Integer.parseInt(strDuration);
            	intent.putExtra("duration_min", duration);
            	
            	setResult(1, intent);
            	finish();
            	return true;
            }
        } 
   
        return false; 
    } 
}