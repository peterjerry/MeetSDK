package com.gotye.meetplayer.adapter;

import java.util.List;
import java.util.Map;

import com.gotye.common.util.LogUtil;
import com.gotye.common.util.PicCacheUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.ImgUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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

public class InkeAdapter extends BaseAdapter {
	private final static String TAG = "InkeAdapter";
	
	private List<Map<String, Object>> data = null;
	private Context context					= null;
	private LayoutInflater inflater 		= null;

	public InkeAdapter(Context context, List<Map<String, Object>> data) {
		super();
		
		this.context = context;
		this.data = data;

		inflater = LayoutInflater.from(context);
	}
	
	public List<Map<String, Object>> getData() {
		return data;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return data.size();
	}

	@Override
	public Map<String, Object> getItem(int position) {
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
		public ImageView avatar			= null;
		public TextView title			= null;
		public TextView location		= null;
		public TextView online_users	= null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例
		//Log.d(TAG, "Java: getView() #" + position);
		
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.inke_item, null);

			holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
			holder.title = (TextView) convertView.findViewById(R.id.title);
			holder.location = (TextView) convertView.findViewById(R.id.location);
			holder.online_users = (TextView) convertView.findViewById(R.id.online_users);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String location = (String) item.get("location");
		String online_users = "观看人数: " + (Integer) item.get("online_users");
		String img_url = (String) item.get("img_url");
		holder.title.setText(title);
		holder.location.setText(location);
		holder.online_users.setText(online_users);

		if (img_url != null && img_url.startsWith("http://")) {
			//String key = PicCacheUtil.hashKeyForDisk(img_url);
			//Bitmap bmp = PicCacheUtil.getThumbnailFromDiskCache(key);

            holder.avatar.setTag(img_url);
            holder.avatar.setImageResource(R.drawable.loading);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.avatar_loading)         // 加载开始默认的图片
                    .showImageForEmptyUri(R.drawable.avatar_error) //url爲空會显示该图片，自己放在drawable里面的
                    .showImageOnFail(R.drawable.avatar_error)      //加载图片出现问题，会显示该图片
                    .displayer(new RoundedBitmapDisplayer(5))  //图片圆角显示，值为整数
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();
            ImageLoader.getInstance().displayImage(img_url, holder.avatar, options);
		}
		
		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

	public int dip2px(Context context, float dipValue) {
		float m = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * m + 0.5f);
	}

	public int px2dip(Context context, float pxValue) {
		float m = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / m + 0.5f);
	}

}
