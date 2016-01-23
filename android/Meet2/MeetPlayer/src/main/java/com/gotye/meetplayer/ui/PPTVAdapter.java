package com.gotye.meetplayer.ui;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gotye.meetsdk.MeetSDK;
import android.provider.MediaStore.Video.Thumbnails;

import com.gotye.meetplayer.R;

public class PPTVAdapter extends BaseAdapter {

	private final static String TAG = "PPTVAdapter";
	
	private Context mContext;
	private int mResource;
	private ImageView mView = null;
	private List<Map<String, Object>> mData;
	private LayoutInflater mInflater = null;
	
	public PPTVAdapter(Context context, List<Map<String, Object>> data, int resource) {
        // TODO Auto-generated constructor stub
		this.mContext = context;
		this.mResource = resource;
		this.mData = data;
		this.mInflater = LayoutInflater.from(context);
    }
	
	public void updateData(List<Map<String, Object>> data) {
		this.mData = data;
	}

	static class ViewHolder
    {
        public TextView tv_title;
        public TextView tv_desc;
        public TextView tv_ft;
        public TextView tv_duration;
        public TextView tv_resolution;
    }
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mData.size();
	}

	@Override
	public Map<String, Object> getItem(int index) {
		// TODO Auto-generated method stub
		return mData.get(index);
	}

	@Override
	public long getItemId(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;
        if (convertView == null) {
        	holder = new ViewHolder();
        	convertView = mInflater.inflate(R.layout.pptv_list, null);
        	
        	//private final String[] from = { "title", "desc", "ft", "duration", "resolution" };
        	
        	//private final int[] to = { R.id.tv_title, R.id.tv_description, 
        	//		R.id.tv_ft, R.id.tv_duration, R.id.resolution};
        	
        	holder.tv_title = (TextView)convertView.findViewById(R.id.tv_title);
        	holder.tv_desc = (TextView)convertView.findViewById(R.id.tv_description);
        	holder.tv_ft = (TextView)convertView.findViewById(R.id.tv_ft);
        	holder.tv_duration = (TextView)convertView.findViewById(R.id.tv_duration);
        	holder.tv_resolution = (TextView)convertView.findViewById(R.id.tv_resolution);
        	
            convertView.setTag(holder);
        }
        else {
        	holder = (ViewHolder)convertView.getTag();
        }
        
    	Map<String, Object> item = mData.get(position);
	    String title = (String)item.get("title");
	    String desc = (String)item.get("desc");
	    int ft = (Integer)item.get("ft");
	    String duration = (String)item.get("duration");
	    String resolution = (String)item.get("resolution");
		
		holder.tv_title.setText(title);
		holder.tv_desc.setText(desc);
		holder.tv_ft.setText(String.valueOf(ft));
		holder.tv_duration.setText(duration);
		holder.tv_resolution.setText(resolution);

		return convertView;
	}
}