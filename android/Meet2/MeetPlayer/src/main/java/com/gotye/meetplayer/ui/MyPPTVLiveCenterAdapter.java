package com.gotye.meetplayer.ui;

import java.util.List;

import com.gotye.common.pptv.LiveStream;
import com.gotye.meetplayer.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class MyPPTVLiveCenterAdapter extends BaseAdapter {
	private final static String TAG = "MyPPTVLiveCenterAdapter";
	
	private List<LiveStream> data = null;
	private Context context					= null;
	private LayoutInflater inflater 		= null;

	public MyPPTVLiveCenterAdapter(Context context, List<LiveStream> data) {
		super();
		
		this.context = context;
		this.data = data;

		inflater = LayoutInflater.from(context);
	}
	
	public List<LiveStream> getData() {
		return data;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return data.size();
	}

	@Override
	public LiveStream getItem(int position) {
		// TODO Auto-generated method stub
		if (position >= data.size())
			return null;
		
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	private class ViewHolder {
		public ImageView img_icon = null;
		public TextView title = null;
		public TextView start_time = null;
		public TextView end_time = null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例
		Log.d(TAG, "Java: getView() #" + position);
		
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.pptv_live, null);
			
			holder.img_icon = (ImageView) convertView.findViewById(R.id.tv_list_pic);
			holder.title = (TextView) convertView.findViewById(R.id.tv_title);
			holder.start_time = (TextView) convertView.findViewById(R.id.tv_now);
			holder.end_time = (TextView) convertView.findViewById(R.id.tv_next);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title和img设置内容
		LiveStream item = data.get(position);
		holder.img_icon.setImageResource(R.drawable.ic_launcher);
		holder.title.setText(item.title);
		holder.start_time.setText("开始时间: " + item.start_time);
		holder.end_time.setText("结束时间: " + item.end_time);
		
		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}
	
}
