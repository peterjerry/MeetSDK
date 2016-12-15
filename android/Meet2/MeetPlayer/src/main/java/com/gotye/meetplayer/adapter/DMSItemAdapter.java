package com.gotye.meetplayer.adapter;

import android.content.Context;
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

public class DMSItemAdapter extends MeetAdapter {

	public DMSItemAdapter(Context context, List<Map<String, Object>> data,
						  int resourceId) {
        super(context, data, resourceId);
	}

	private class ViewHolder {
		public TextView title = null;
        public TextView filesize = null;
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

			holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.filesize = (TextView) convertView.findViewById(R.id.filesize);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String filesize = (String) item.get("filesize");

        holder.title.setText(title);
		holder.filesize.setText(filesize);

		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

}
