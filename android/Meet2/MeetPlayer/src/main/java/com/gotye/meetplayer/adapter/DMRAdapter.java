package com.gotye.meetplayer.adapter;

import android.content.Context;
import android.media.Image;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gotye.meetplayer.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.List;
import java.util.Map;

public class DMRAdapter extends MeetAdapter {

	public DMRAdapter(Context context, List<Map<String, Object>> data,
					  int resourceId) {
        super(context, data, resourceId);
	}

	private class ViewHolder {
		public ImageView icon = null;
        public TextView title = null;
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

			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.title = (TextView) convertView.findViewById(R.id.title);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String icon_url = (String) item.get("icon");

        holder.title.setText(title);

        if (icon_url != null && icon_url.startsWith("http://")) {
            holder.icon.setImageResource(R.drawable.dlna);
            holder.icon.setTag(icon_url);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.dlna)         // 加载开始默认的图片
                    .showImageForEmptyUri(R.drawable.dlna) //url爲空會显示该图片，自己放在drawable里面的
                    .showImageOnFail(R.drawable.dlna)      //加载图片出现问题，会显示该图片
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .imageScaleType(ImageScaleType.NONE)
                    .build();
            ImageLoader.getInstance().displayImage(icon_url, holder.icon, options);
        }

		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

}
