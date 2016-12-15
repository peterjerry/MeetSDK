package com.gotye.meetplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gotye.db.PPTVPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;

import java.util.List;
import java.util.Map;

public class PPTVEpisodeAdapter extends MeetAdapter {
	public PPTVEpisodeAdapter(Context context, List<Map<String, Object>> data, int resource) {
		super(context, data, resource);
	}

	private class ViewHolder {
		public TextView title = null;
		public TextView desc = null;
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
            holder.desc = (TextView) convertView.findViewById(R.id.tv_desc);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Map<String, Object> item = data.get(position);

        String title = String.valueOf(item.get("title")); // force convert to String(fix Integer case)
        holder.title.setText(title);

        String desc = (String)item.get("desc");
        if (!TextUtils.isEmpty(desc))
            holder.desc.setText(desc);

        if (item.containsKey("company")) {
            String company = (String)item.get("company");
            if (company.equals("pptv")) {
                String playlink = (String)item.get("playlink");
                if (playlink != null) {
                    int pos = PPTVPlayhistoryDatabaseHelper.getInstance(context)
                            .getLastPlayedPosition(playlink);
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
