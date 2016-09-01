package com.gotye.meetplayer.adapter;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.os.AsyncTask;

import com.gotye.meetsdk.MeetSDK;
import com.gotye.meetplayer.R;

public class MyAdapter extends SimpleAdapter {

	private final static String TAG = "MyAdapter";
	private ImageView mView = null;
	private boolean mScrolling = false;
	
	public MyAdapter(Context context,  
            List<? extends Map<String, ?>> data, int resource,  
            String[] from, int[] to) {  
        super(context, data, resource, from, to);  
        // TODO Auto-generated constructor stub  
    }
	
	public void SetScrolling(boolean isScrolling) {
		mScrolling = isScrolling;
	}
    
	@Override
    public void setViewImage(ImageView view, String value) {
		Log.d(TAG, "Java: setViewImage: " + value);
		
		// fix xiaomi 3 crash when scrolling
		//super.setViewImage(view, value);
		
		mView = view;
				
		//new ThumbNailTask().execute(value); // async task will cause thumbnail mismatch	
		
		if (mScrolling)
			view.setImageResource(R.mipmap.clip);
		else
			((ImageView)view).setImageBitmap(MeetSDK.createVideoThumbnail(value, Thumbnails.MICRO_KIND));
		
		// system thumnnail
        /*Bitmap bitmap = getVideoThumb(value);
        if (bitmap != null)
			view.setImageBitmap(bitmap);	
		else
			view.setImageResource(R.drawable.clip);*/
	}
	
	class ThumbNailTask extends AsyncTask<String, Integer, Bitmap>{  
        //后面尖括号内分别是参数（例子里是线程休息时间），进度(publishProgress用到)，返回值 类型  
          
        @Override  
        protected void onPreExecute() {  
            //第一个执行方法  
            super.onPreExecute();  
        }  
          
        @Override  
        protected Bitmap doInBackground(String... params) {  
            //第二个执行方法,onPreExecute()执行完后执行  
			Log.d(TAG, "Java: to get thumbnail: " + params[0]);
            return MeetSDK.createVideoThumbnail(params[0], Thumbnails.MICRO_KIND); 
        }  
  
        @Override  
        protected void onProgressUpdate(Integer... progress) {  
            //这个函数在doInBackground调用publishProgress时触发，虽然调用时只有一个参数  
            //但是这里取到的是一个数组,所以要用progesss[0]来取值  
            //第n个参数就用progress[n]来取值  
            //tv.setText(progress[0]+"%");  
            super.onProgressUpdate(progress);  
        }  
  
        @Override  
        protected void onPostExecute(Bitmap result) {  
            //doInBackground返回时触发，换句话说，就是doInBackground执行完后触发  
            //这里的result就是上面doInBackground执行后的返回值，所以这里是"执行完毕"  
			if(result == null) {
				Log.e(TAG, "Java: failed to get thumbnail");
			}
			else
				mView.setImageBitmap(result);	
            super.onPostExecute(result);  
        }  
    }  

    public Bitmap getVideoThumb(String path){
        return ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MICRO_KIND);
    }
}