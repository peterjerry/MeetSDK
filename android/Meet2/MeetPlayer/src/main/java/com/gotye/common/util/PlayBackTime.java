package com.gotye.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gotye.meetplayer.R;

public class PlayBackTime {
	private final static String TAG = "PlayBackTime";

	private Context mContext;
	private long mStartTimeSec = -1;
	private int mDurationSec = 0;

	public PlayBackTime(Context ctx) {
		mContext = ctx;
	}

	public boolean isPlayback() {
		return (mDurationSec > 0);
	}

	public void setLive() {
		mDurationSec = 0;
	}

	public String getBestvTimeStr() {
		if (mDurationSec == 0 || mStartTimeSec == -1)
			return null;

		return String.format("&starttime=%d&endtime=%d",
				mStartTimeSec, mStartTimeSec + mDurationSec * 60);
	}

	public String getPPTVTimeStr() {
		if (mDurationSec == 0 || mStartTimeSec == -1)
			return null;

		String PlayerLinkSurfix = String.format("&begin_time=%d&end_time=%d",
				mStartTimeSec, mStartTimeSec + mDurationSec * 60);
		try {
			PlayerLinkSurfix = URLEncoder.encode(PlayerLinkSurfix, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Java: failed to gen PPTV PlayerLinkSurfix");
			return null;
		}

		Log.i(TAG, "Java: PPTV PlayerLinkSurfix final: " + PlayerLinkSurfix);
		return PlayerLinkSurfix;
	}

	public boolean setPlaybackTime() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		View view = View.inflate(mContext, R.layout.date_time_dialog, null);
		final AppCompatSpinner spinnerDate = (AppCompatSpinner) view.findViewById(R.id.spinner_date);
		final TimePicker timePicker = (android.widget.TimePicker) view.findViewById(R.id.time_picker);
		final AppCompatSpinner spinnerDuration = (AppCompatSpinner) view.findViewById(R.id.spinner_duration);

		Calendar cal = Calendar.getInstance();
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); // yyyy-MM-dd HH:mm:ss

		List<String> dateList = new ArrayList<String>();
		for (int i=0;i<7;i++) {
			cal.setTime(date);
			String dateString = formatter.format(date);
			dateList.add(dateString);

			cal.add(Calendar.DAY_OF_MONTH, -1);//把日期往后增加一天.整数往后推,负数往前移动
			date = cal.getTime();
		}

		DateAdapter dateAdapter = new DateAdapter(mContext, dateList);
		spinnerDate.setAdapter(dateAdapter);

		final int[] duration_min = new int[]{0, 10, 20, 30, 60, 120, 180};

		ArrayAdapter<CharSequence> sourceAdapter = ArrayAdapter.createFromResource(
				mContext, R.array.duration_array, R.layout.spinner_text/*android.R.layout.simple_spinner_item*/);
		sourceAdapter.setDropDownViewResource(R.layout.spinner_text);
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

		timePicker.setIs24HourView(true);
		if (mStartTimeSec != -1) {
			cal.setTimeInMillis(mStartTimeSec * 1000);
			timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
			timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		}
		else {
			cal.setTime(new Date());
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

		builder.setTitle("设置回看");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				int pos = spinnerDate.getSelectedItemPosition();
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DAY_OF_MONTH, -pos);

				cal.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);

				mStartTimeSec = cal.getTimeInMillis() / 1000;

				// step2
				pos = spinnerDuration.getSelectedItemPosition();
				mDurationSec = duration_min[pos];
				if (mDurationSec == 0) {
					Toast.makeText(mContext, String.format("时长为0，切换到直播模式"), Toast.LENGTH_SHORT).show();
					return;
				}

				Log.i(TAG, String.format("start_time %d sec, duration %d min", mStartTimeSec, mDurationSec));

				dialog.cancel();

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				String startTimestr = formatter.format(cal.getTime());
				Toast.makeText(mContext, String.format("切换为 回看 模式 开始 %s, 时长 %d min",
						startTimestr, mDurationSec), Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mDurationSec = 0;
				Toast.makeText(mContext, String.format("切换到直播模式"), Toast.LENGTH_SHORT).show();
			}

		});

		Dialog dialog = builder.create();
		dialog.show();
		return true;
	}

}
