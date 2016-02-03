package com.gotye.meetplayer.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.gotye.meetplayer.R;

public class AsyncImageView extends ImageView {

	static final String TAG = AsyncImageView.class.getSimpleName();

	protected static final DisplayImageOptions DEFALUT_DISPLAY_OPTIONS = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.clip)
			.showImageOnFail(R.drawable.clip_error)
			.showImageOnLoading(R.drawable.clip).cacheOnDisk(true)
			.bitmapConfig(Bitmap.Config.RGB_565).build();
	
	private static final ImageLoadingListener mListener = new ImageLoadingListener() {
		
		@Override
		public void onLoadingStarted(String imageUri, View v) {
			// TODO Auto-generated method stub
			Log.w(TAG, "onLoadingStarted[imageUri: " + imageUri + ", v: " + v + "]");
		}
		
		@Override
		public void onLoadingFailed(String imageUri, View v, FailReason reason) {
			// TODO Auto-generated method stub
			Log.w(TAG, "onLoadingFailed[imageUri: " + imageUri + ", v: " + v + ", FailReason: " + reason);
		}
		
		@Override
		public void onLoadingComplete(String imageUri, View v, Bitmap bmp) {
			// TODO Auto-generated method stub
			Log.w(TAG, "onLoadingComplete[imageUri: " + imageUri + ", v: " + v + ", Bitmap: " + bmp + "]");
		}
		
		@Override
		public void onLoadingCancelled(String imageUri, View v) {
			// TODO Auto-generated method stub
			Log.w(TAG, "onLoadingCancelled[imageUri: " + imageUri + ", v: " + v + "]");
		}
	};

	protected ImageLoader mImageLoader = ImageLoader.getInstance();

	public AsyncImageView(Context context) {
		super(context);
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setImageAsync(String imageUri) {
		setImageAsync(imageUri, DEFALUT_DISPLAY_OPTIONS, null);
	}

	public void setImageAsync(String imageUri, DisplayImageOptions options) {
		setImageAsync(imageUri, options, null);
	}

	public void setImageAsync(String imageUri, ImageLoadingListener listener) {
		setImageAsync(imageUri, DEFALUT_DISPLAY_OPTIONS, listener);
	}

	public void setImageAsync(String imageUri, DisplayImageOptions options,
			ImageLoadingListener listener) {

		if (listener == null) {
			listener = mListener;
		}
		mImageLoader.displayImage(imageUri, this, options, listener);
	}
}
