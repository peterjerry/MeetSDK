package com.pplive.meetplayer.ui;

import java.util.List;
import java.util.Map;

import com.pplive.common.util.PicCacheUtil;
import com.pplive.meetplayer.R;
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
import android.widget.TextView;

public class MySohuEpAdapter extends BaseAdapter {
	private final static String TAG = "MySohuEpAdapter";
	
	private List<Map<String, Object>> data = null;
	private Context context = null;
	private LayoutInflater inflater = null;

	public MySohuEpAdapter(Context context, List<Map<String, Object>> data) {
		super();
		this.data = data;
		this.context = context;

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

		TextView title			= null;
		ImageView img			= null;
		TextView tip			= null;
		boolean imgShowed		= false;

		public TextView getTitle() {
			return title;
		}

		public void setTitle(TextView title) {
			this.title = title;
		}
		
		public TextView getTip() {
			return tip;
		}

		public void setTip(TextView tip) {
			this.tip = tip;
		}

		public ImageView getImg() {
			return img;
		}

		public void setImg(ImageView img) {
			this.img = img;
		}

		public void setShowed(boolean isShowed) {
			this.imgShowed = isShowed;
		}
		
		public boolean isShowed() {
			return imgShowed;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例
		ViewHolder holder;
		if (convertView == null) {

			convertView = inflater.inflate(R.layout.gridview_item, null);
			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.gridview_title);
			holder.tip = (TextView) convertView.findViewById(R.id.gridview_tip);
			holder.img = (ImageView) convertView.findViewById(R.id.gridview_img);

			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String tip = (String) item.get("tip");
		String img_url = (String) item.get("img_url");
		holder.getTitle().setText(title);
		holder.getTip().setText(tip);
		
		ImageView iv = holder.getImg();
		
		//if (!holder.isShowed()) {
			iv.setImageResource(R.drawable.clip); // set default picture
			new LoadPicTask().execute(holder, img_url);
		//}
		
		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

	private class LoadPicTask extends AsyncTask<Object, Integer, Bitmap> {

		private ViewHolder mHolder;
		private String img_url;
		
		@Override
		protected void onPostExecute(Bitmap bmp) {
			if (mHolder == null || bmp == null) {
				Log.e(TAG, "Java: failed to get http image " + img_url);
				return;
			}
			
			ImageView thumb	= mHolder.getImg();
			thumb.setImageBitmap(bmp);
			mHolder.setShowed(true);
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			mHolder = (ViewHolder)params[0];
			img_url = (String)params[1];
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
