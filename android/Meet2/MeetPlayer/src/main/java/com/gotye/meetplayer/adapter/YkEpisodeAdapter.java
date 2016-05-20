package com.gotye.meetplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.util.List;
import java.util.Map;

public class YkEpisodeAdapter extends MeetAdapter {
	public YkEpisodeAdapter(Context context, List<Map<String, Object>> data, int resource) {
		super(context, data, resource);
	}

	private class ViewHolder {
		public TextView title = null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例

		ViewHolder holder;
		if (convertView == null) {
			convertView = inflater.inflate(id, null);
            holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.tv_title);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Map<String, Object> item = data.get(position);

        String title = String.valueOf(item.get("title"));
        holder.title.setText(title);

        if (item.containsKey("company")) {
            String company = (String)item.get("company");
            if (company.equals("youku")) {
                String vid = (String)item.get("vid");
                if (vid != null) {
                    int pos = YKPlayhistoryDatabaseHelper.getInstance(context)
                            .getLastPlayedPosition(vid);
                    if (pos > 0) {
                        holder.title.setTextColor(Color.RED);
                    }
                    else {
                        holder.title.setTextColor(Color.BLACK);
                    }

                }
            }
        }

		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

}
