package com.gotye.meetplayer.ui;

import java.util.List;
import java.util.Map;

import com.gotye.common.util.PicCacheUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.ImgUtil;

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

public class MySohuEpAdapter extends BaseAdapter {
	private final static String TAG = "MySohuEpAdapter";
	
	private List<Map<String, Object>> data = null;
	private Context context					= null;
	private LayoutInflater inflater 		= null;

	public MySohuEpAdapter(Context context, List<Map<String, Object>> data) {
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
		public TextView title			= null;
		public ImageView img			= null;
		public TextView onlinetime		= null;
		public TextView tip				= null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例
		Log.d(TAG, "Java: getView() #" + position);
		
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.gridview_item, null);
			
			holder.title = (TextView) convertView.findViewById(R.id.gridview_title);
			holder.onlinetime = (TextView) convertView.findViewById(R.id.gridview_onlinetime);
			holder.tip = (TextView) convertView.findViewById(R.id.gridview_tip);
			holder.img = (ImageView) convertView.findViewById(R.id.gridview_img);

			RelativeLayout.LayoutParams params = (LayoutParams) holder.img.getLayoutParams();
			params.height = dip2px(context, 200/* dip */);
			holder.img.setLayoutParams(params);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// 为holder中的title tip和img设置内容
		Map<String, Object> item = data.get(position);
		String title = (String) item.get("title");
		String onlinetime = "";
		if (item.containsKey("onlinetime"))
			onlinetime = (String) item.get("onlinetime");
		String tip = (String) item.get("tip");
		String img_url = (String) item.get("img_url");
		holder.title.setText(title);
		holder.onlinetime.setText(onlinetime);
		holder.tip.setText(tip);

		if (img_url != null && img_url.startsWith("http://")) {
			String key = PicCacheUtil.hashKeyForDisk(img_url);
			Bitmap bmp = PicCacheUtil.getThumbnailFromDiskCache(key);
			if (bmp != null) {
				holder.img.setImageBitmap(bmp);
				//LogUtil.info(TAG, "set http bitmap from getThumbnailFromDiskCache");
			}
			else {
				holder.img.setTag(img_url);
				holder.img.setImageResource(R.drawable.loading);
				new LoadPicTask().execute(holder, img_url);
			}
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
	
	private class LoadPicTask extends AsyncTask<Object, Integer, Bitmap> {

		private ViewHolder mHolder;
		private String mImgUrl;
		
		@Override
		protected void onPostExecute(Bitmap bmp) {
			if (mHolder == null || bmp == null) {
				Log.e(TAG, "Java: failed to get http image " + mImgUrl);
				return;
			}
			
			if (mHolder.img.getTag() != null && mHolder.img.getTag().equals(mImgUrl)) {
                mHolder.img.setImageBitmap(bmp);
            }
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			mHolder = (ViewHolder)params[0];
			mImgUrl = (String)params[1];
			
			Bitmap bmp = ImgUtil.getHttpBitmap(mImgUrl);
			if (bmp == null) {
				Log.e(TAG, "Java: failed to getHttpBitmap " + mImgUrl);
				return null;
			}
			
			String key = PicCacheUtil.hashKeyForDisk(mImgUrl);
			PicCacheUtil.addThumbnailToDiskCache(key, bmp);
			return bmp;
		}
	}
}
