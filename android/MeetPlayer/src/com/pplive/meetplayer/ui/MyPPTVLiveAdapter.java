package com.pplive.meetplayer.ui;

import java.util.List;
import java.util.Map;

import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.util.PicCacheUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.ui.widget.AsyncImageView;
import com.pplive.meetplayer.util.ImgUtil;

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

public class MyPPTVLiveAdapter extends BaseAdapter {
	private final static String TAG = "MyPPTVLiveAdapter";
	
	private List<PlayLink2> data = null;
	private Context context					= null;
	private LayoutInflater inflater 		= null;

	public MyPPTVLiveAdapter(Context context, List<PlayLink2> data) {
		super();
		
		this.context = context;
		this.data = data;

		inflater = LayoutInflater.from(context);
	}
	
	public List<PlayLink2> getData() {
		return data;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return data.size();
	}

	@Override
	public PlayLink2 getItem(int position) {
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
		public TextView nowplay = null;
		public TextView willplay = null;
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
			holder.nowplay = (TextView) convertView.findViewById(R.id.tv_now);
			holder.willplay = (TextView) convertView.findViewById(R.id.tv_next);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title和img设置内容
		PlayLink2 item = data.get(position);
		holder.img_icon.setImageResource(R.drawable.ic_launcher);
		holder.title.setText(item.getTitle());
		
		String nowplay, willplay;
		String desc = item.getDescription();
		int pos = desc.indexOf("|");
		if (pos == -1)
			nowplay = willplay = "N/A";
		else {
			nowplay = desc.substring(0, pos);
			willplay = desc.substring(pos + 1, desc.length());
		}
		holder.nowplay.setText("当前播放: " + nowplay);
		holder.willplay.setText("即将播放: " + willplay);
		
		String img_url = item.getImgUrl();
		if (img_url.startsWith("http://")) {
			new LoadInfoTask().execute(holder, img_url);
		}
		
		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}
	
	
	private class LoadInfoTask extends AsyncTask<Object, Integer, Bitmap> {

		ViewHolder mHolder;
		
		@Override
		protected void onPostExecute(Bitmap bmp) {
			if (mHolder == null || bmp == null)
				return;
			
			ImageView thumb		= mHolder.img_icon;
			thumb.setImageBitmap(bmp);
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			mHolder = (ViewHolder)params[0];
			String img_url = (String)params[1];
			String key = PicCacheUtil.hashKeyForDisk(img_url);
			Bitmap bmp = PicCacheUtil.getThumbnailFromDiskCache(key);
			if (bmp == null) {
				bmp = ImgUtil.getHttpBitmap(img_url);
				if (bmp == null) {
					Log.e(TAG, "Java: failed to getHttpBitmap " + img_url);
					return null;
				}
				
				PicCacheUtil.addThumbnailToDiskCache(key, bmp);
			}
			
			return bmp;
		}
	}
}
