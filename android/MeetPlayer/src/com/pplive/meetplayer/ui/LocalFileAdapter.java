package com.pplive.meetplayer.ui;

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

import android.pplive.media.MeetSDK;
import android.provider.MediaStore.Video.Thumbnails;

import com.pplive.meetplayer.R;

public class LocalFileAdapter extends BaseAdapter {

	private final static String TAG = "LocalFileAdapter";
	
	private Context mContext;
	private int mResource;
	private List<Map<String, Object>> mData;
	private LayoutInflater mInflater = null;
	
	public LocalFileAdapter(Context context, List<Map<String, Object>> data, int resource) {
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
        public ImageView thumb;
        public TextView tv_filename;
        public TextView tv_mediainfo;
        public TextView tv_folder;
        public TextView tv_filesize;
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
        	convertView = mInflater.inflate(R.layout.sd_list, null);
        	
        	holder.thumb = (ImageView)convertView.findViewById(R.id.iv_thumb);
        	holder.tv_filename = (TextView)convertView.findViewById(R.id.tv_filename);
        	holder.tv_mediainfo = (TextView)convertView.findViewById(R.id.tv_mediainfo);
        	holder.tv_folder = (TextView)convertView.findViewById(R.id.tv_folder);
        	holder.tv_filesize = (TextView)convertView.findViewById(R.id.tv_filesize);
        	holder.tv_resolution = (TextView)convertView.findViewById(R.id.tv_resolution);
        	
            convertView.setTag(holder);
        }
        else {
        	holder = (ViewHolder)convertView.getTag();
        }
        	
    	Map<String, Object> item = mData.get(position);
	    String file_name = (String)item.get("filename");
	    String media_info = (String)item.get("mediainfo");
	    String folder = (String)item.get("folder");
	    String filesize = (String)item.get("filesize");
	    String resolution = (String)item.get("resolution");
		String fullpath = (String)item.get("fullpath");
		
		Object obj = item.get("thumb") ;
		if (obj instanceof Integer) {
			int thumb_id = (Integer)obj;
			holder.thumb.setImageResource(thumb_id);
		}
		else {
			Bitmap bm = MeetSDK.createVideoThumbnail(fullpath, Thumbnails.MICRO_KIND);
			if (bm == null)
				holder.thumb.setImageResource(R.drawable.clip);
			else
				holder.thumb.setImageBitmap(bm);
		}
		
		holder.tv_filename.setText(file_name);
		holder.tv_mediainfo.setText(media_info);
		holder.tv_folder.setText(folder);
		holder.tv_filesize.setText(filesize);
		holder.tv_resolution.setText(resolution);

		return convertView;
	}
}