package com.pplive.meetplayer.ui;

import java.util.List;
import java.util.Map;

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

		private TextView title			= null;
		private ImageView img			= null;
		private TextView tip			= null;
		private boolean imgDownloaded	= false;

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

		public void setImg(ImageView img) {
			this.img = img;
		}
		
		public ImageView getImg() {
			return img;
		}
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
			
			holder.setTitle((TextView) convertView.findViewById(R.id.gridview_title));
			holder.setTip((TextView) convertView.findViewById(R.id.gridview_tip));
			holder.setImg((ImageView) convertView.findViewById(R.id.gridview_img));
			
			 RelativeLayout.LayoutParams params = (LayoutParams) holder.getImg().getLayoutParams();
			 params.height	= dip2px(context, 200/*dip*/);
			 holder.getImg().setLayoutParams(params);  
			
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

		if (img_url != null && img_url.startsWith("http://")) {
			holder.getImg().setTag(img_url);
			holder.getImg().setImageResource(R.drawable.loading);
			new LoadPicTask().execute(holder, img_url);
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
			
			ImageView thumb	= mHolder.getImg();
			if (thumb.getTag() != null && thumb.getTag().equals(mImgUrl))
				thumb.setImageBitmap(bmp);
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			mHolder = (ViewHolder)params[0];
			mImgUrl = (String)params[1];
			
			String key = PicCacheUtil.hashKeyForDisk(mImgUrl);
			Bitmap bmp = PicCacheUtil.getThumbnailFromDiskCache(key);
			if (bmp == null) {
				bmp = ImgUtil.getHttpBitmap(mImgUrl);
				if (bmp == null) {
					Log.e(TAG, "Java: failed to getHttpBitmap " + mImgUrl);
					return null;
				}
				
				PicCacheUtil.addThumbnailToDiskCache(key, bmp);
			}
			
			return bmp;
		}
	}
}
