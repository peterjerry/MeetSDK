package com.gotye.meetplayer.adapter;

import java.util.List;
import java.util.Map;

import com.gotye.common.util.LogUtil;
import com.gotye.common.util.PicCacheUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.ImgUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
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

public class CommonAlbumAdapter extends BaseAdapter {
	private final static String TAG = "CommonAlbumAdapter";
	
	private List<Map<String, Object>> data = null;
	private Context context					= null;
	private LayoutInflater inflater 		= null;

	public CommonAlbumAdapter(Context context, List<Map<String, Object>> data) {
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
		public TextView duration		= null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		// 获得holder以及holder对象中tv和img对象的实例
		//Log.d(TAG, "Java: getView() #" + position);
		
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.gridview_item, null);
			
			holder.title = (TextView) convertView.findViewById(R.id.gridview_title);
			holder.onlinetime = (TextView) convertView.findViewById(R.id.gridview_onlinetime);
			holder.tip = (TextView) convertView.findViewById(R.id.gridview_tip);
			holder.duration = (TextView) convertView.findViewById(R.id.gridview_duration);
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
		String duration = "";
		if (item.containsKey("duration"))
			duration = (String) item.get("duration");
		holder.duration.setText(duration);

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

		if (img_url != null && img_url.startsWith("http://")) {
			//String key = PicCacheUtil.hashKeyForDisk(img_url);
			//Bitmap bmp = PicCacheUtil.getThumbnailFromDiskCache(key);

            holder.img.setImageResource(R.drawable.loading);
            holder.img.setTag(img_url);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.loading)         // 加载开始默认的图片
                    .showImageForEmptyUri(R.drawable.loading) //url爲空會显示该图片，自己放在drawable里面的
                    .showImageOnFail(R.drawable.loading_error)      //加载图片出现问题，会显示该图片
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
					.imageScaleType(ImageScaleType.NONE)
                    .build();
            ImageLoader.getInstance().displayImage(img_url, holder.img, options);
		}
		
		// 注意 默认为返回null,必须得返回convertView视图
		return convertView;
	}

	private int dip2px(Context context, float dipValue) {
		float m = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * m + 0.5f);
	}

	private int px2dip(Context context, float pxValue) {
		float m = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / m + 0.5f);
	}
}
