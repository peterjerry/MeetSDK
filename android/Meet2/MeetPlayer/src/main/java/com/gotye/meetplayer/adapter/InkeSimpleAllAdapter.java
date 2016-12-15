package com.gotye.meetplayer.adapter;

import java.util.List;
import java.util.Map;

import com.gotye.meetplayer.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

public class InkeSimpleAllAdapter extends MeetAdapter {

	public InkeSimpleAllAdapter(Context context, List<Map<String, Object>> data,
                                int resourceId) {
		super(context, data, resourceId);
	}

	private class ViewHolder {
		public ImageView avatar			= null;
		public TextView title			= null;
		public TextView uid				= null;
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
			convertView = inflater.inflate(id, null);

			holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
			holder.title = (TextView) convertView.findViewById(R.id.title);
			holder.uid = (TextView) convertView.findViewById(R.id.uid);
			holder.location = (TextView) convertView.findViewById(R.id.location);
			holder.online_users = (TextView) convertView.findViewById(R.id.online_users);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String uid = "映客ID: " + (Integer) item.get("uid");
		String location = (String) item.get("location");
		String online_users = "观看人数: " + (Integer) item.get("online_users");
		String img_url = (String) item.get("img_url");
		holder.title.setText(title);
		holder.uid.setText(uid);
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

}
