package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.Toast;


public class PPTVEpisodeActivity extends Activity {
	private final static String TAG = "PPTVEpisodeActivity";
	
	private Button btnReputation;
	private Button btnPopularity;
	private Button btnUpdate;
	private CheckBox cbIsCatalog;
	private Button btnFt;
	private GridView gridView = null;
    private MySohuEpAdapter adapter = null;
    
    private final static int ONE_KILOBYTE 	= 1024;
	private final static int ONE_MAGABYTE 	= (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE 	= (ONE_MAGABYTE * ONE_KILOBYTE);
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_MOREDATA_DONE	= 2;
    private final static int MSG_PLAY_CDN_FT		= 3;
    private final static int MSG_NO_MORE_EPISODE	= 11;
    private final static int MSG_FAIL_TO_DETAIL	= 12;
    private final static int MSG_FAIL_TO_GET_FT	= 13;
    
    private final static int TASK_DETAIL			= 1;
    private final static int TASK_MORELIST		= 2;
    private final static int TASK_MORESEARCH		= 3;
    private final static int TASK_ITEM_FT			= 4;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH		= 2;
    
    private final static int page_size = 10;
    private int album_page_index = 1;
    private int ep_page_index = 1;
    private int ep_page_incr;
    private int search_page_index = 1;
    
    private EPGUtil mEPG;
    private String epg_param;
    private String epg_type;
    private String episode_title;
    private int episode_index;
    private int selected_episode = -1;
    // 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
    private String epg_order = "order=t";
    private String search_key;
    private List<PlayLink2> mAlbumList;
    private List<PlayLink2> mEpisodeList;
    private boolean is_catalog = false;
    
    private boolean interrupted = false;
    
    private boolean noMoreData = false;
    private boolean loadingMore = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		Intent intent = getIntent();
		if (intent.hasExtra("epg_param")) {
			epg_param = intent.getStringExtra("epg_param");
			epg_type = intent.getStringExtra("epg_type");
		}
		else if (intent.hasExtra("search_key"))
			search_key = intent.getStringExtra("search_key");
		else {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_pptv_episode);
		
		this.btnReputation = (Button) findViewById(R.id.btn_reputation);
		this.btnPopularity = (Button) findViewById(R.id.btn_popularity);
		this.btnUpdate = (Button) findViewById(R.id.btn_update);
		this.cbIsCatalog = (CheckBox) findViewById(R.id.cb_is_catalog);
		this.btnFt = (Button) findViewById(R.id.btn_ft);
		
		this.btnReputation.setOnClickListener(mClickListener);
		this.btnPopularity.setOnClickListener(mClickListener);
		this.btnUpdate.setOnClickListener(mClickListener);
		this.cbIsCatalog.setOnClickListener(mClickListener);
		
		this.btnFt.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] ft = {"流畅", "高清", "超清", "蓝光"};
				
				Dialog choose_ft_dlg = new AlertDialog.Builder(PPTVEpisodeActivity.this)
				.setTitle("select download ft")
				.setSingleChoiceItems(ft, Integer.parseInt(btnFt.getText().toString()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btnFt.setText(Integer.toString(whichButton));
							dialog.dismiss();
						}
					})
				.create();
				choose_ft_dlg.show();	
			}
		});
		
		this.gridView = (GridView) findViewById(R.id.grid_view);
		this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				String vid = (String)item.get("vid");
				new PPTVEpgTask().execute(TASK_DETAIL, Integer.valueOf(vid));
			}
			
		});
		
		this.gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v,
					int position, long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				popupMoreDialog(item);
				return true;
			}
		});
		
		this.gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
		            int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				Log.i(TAG, String.format("Java: onScroll first %d, visible %d, total %d", 
						firstVisibleItem, visibleItemCount, totalItemCount));
				
				int lastInScreen = firstVisibleItem + visibleItemCount;
		        if (totalItemCount > 0 && lastInScreen == totalItemCount && !noMoreData) {
		        	if (!loadingMore) {
		        		loadingMore = true;
		        		if (search_key != null)
		        			new PPTVEpgTask().execute(TASK_MORESEARCH);
		        		else
		        			new PPTVEpgTask().execute(TASK_MORELIST);
		        	}
		        }
			}
		});
		
	    mEPG = new EPGUtil();
	    
	    if (search_key != null)
	    	new SetDataTask().execute(SET_DATA_SEARCH);
	    else
	    	new SetDataTask().execute(SET_DATA_LIST);
	    
	    if (Util.startP2PEngine(this) == false) {
			Toast.makeText(this, "failed to start p2p engine", 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private void popupMoreDialog(final Map<String, Object> item) {
    	final String[] action = {"详情", "下载"};
		Dialog choose_action_dlg = new AlertDialog.Builder(this)
		.setTitle("选择操作")
		.setItems(action, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					if (whichButton == 0) {
						String title = (String)item.get("title");
						String description = (String)item.get("desc");
						String info = description;
						if (info == null)
							info = title;
						new AlertDialog.Builder(PPTVEpisodeActivity.this)
							.setTitle("专辑介绍")
							.setMessage(info)
							.setPositiveButton("确定", null)
							.show();
					}
					else if (whichButton == 1) {
						String vid = (String) item.get("vid");
						String title = (String)item.get("title");
						String save_path = Environment.getExternalStorageDirectory().getAbsolutePath() + 
								"/test2/" + title + ".mp4";
						
						new PPBoxDownloadTask().execute(vid, save_path, title);
					}
				}
			})
		.setNegativeButton("取消", null)
		.create();
		choose_action_dlg.show();
    }
	
	private View.OnClickListener mClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			album_page_index = 1;
			// 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
			
			switch (v.getId()) {  
            case R.id.btn_reputation:  
                epg_order = "order=g";
                new SetDataTask().execute(SET_DATA_LIST);
                break;            
            case R.id.btn_popularity:  
            	epg_order = "order=t";
            	new SetDataTask().execute(SET_DATA_LIST);
                break;    
            case R.id.btn_update:
            	epg_order = "order=n";
            	new SetDataTask().execute(SET_DATA_LIST);
                break;
            case R.id.cb_is_catalog:
            	is_catalog = !is_catalog;
            	new SetDataTask().execute(SET_DATA_LIST);
            	break;
            default:
            	Log.w(TAG, "Java unknown view id: " + v.getId());
                break;  
			}
		}
	};
	
	private Handler mhandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_EPISODE_DONE:
            	if (mEpisodeList.size() == 1) {
            		String vid = mEpisodeList.get(0).getId();
            		episode_title = mEpisodeList.get(0).getTitle();
            		int playlink = Integer.valueOf(vid);
            		if (playlink < 300000) { // vod
            			new PPTVEpgTask().execute(TASK_ITEM_FT, playlink);
            		}
            		else { // live
            			play_video(1, 1);
            		}
            			
					return;
            	}
            	
            	popupSelectEpisodeDlg();
            	break;
            case MSG_MOREDATA_DONE:
            	List<Map<String, Object>> listData = adapter.getData();
            	
    			int c = mAlbumList.size();
    			for (int i=0;i<c;i++) {
    				HashMap<String, Object> episode = new HashMap<String, Object>();
    				PlayLink2 al = mAlbumList.get(i);
    				
    				episode.put("title", al.getTitle());
    				episode.put("img_url", al.getImgUrl());
    				episode.put("tip", al.getVideoCount() + "集");
    				episode.put("vid", al.getId());
    				episode.put("desc", al.getDescription());
    				listData.add(episode);
    			}
            	
            	adapter.notifyDataSetChanged();
            	break;
            case MSG_PLAY_CDN_FT:
            	play_video(msg.arg1, msg.arg2);
            	break;
            case MSG_NO_MORE_EPISODE:
            	Toast.makeText(PPTVEpisodeActivity.this, "No more episode", Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_GET_FT:
            	Toast.makeText(PPTVEpisodeActivity.this, "Failed to get ft", Toast.LENGTH_SHORT).show();
            	break;
            default:
            	break;
            }
        }
	};
	
	private void play_video(int ft, int best_ft) {
		String vid = mEpisodeList.get(0).getId();
		
		String info = String.format("ready to play video %s, playlink: %s, ft: %d", 
				episode_title, vid, ft);
		Log.i(TAG, info);
		Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
		
		short port = MediaSDK.getPort("http");
		int playlink = Integer.valueOf(vid);
		String url = PlayLinkUtil.getPlayUrl(playlink, port, ft, 3, null);
		
		Uri uri = Uri.parse(url);
		
		/* method 1
		 * Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setComponent(new ComponentName("com.pplive.tvduck", "com.pplive.tvduck.PlayerActivity"));
		intent.setClassName("com.pplive.tvduck", "com.pplive.tvduck.PlayerActivity");
        intent.putExtra(Intent.ACTION_VIEW, uri);
        intent.setData(uri);*/
        
        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.putExtra(Intent.ACTION_VIEW, uri);
        Intent wrapperIntent = Intent.createChooser(intent, "选择播放器");
        startActivity(wrapperIntent);*/
		
        // method 3
        Intent intent = new Intent(PPTVEpisodeActivity.this,
        		PPTVPlayerActivity.class);
		Log.i(TAG, "to play uri: " + uri.toString());

		intent.setData(uri);
		intent.putExtra("title", episode_title);
		intent.putExtra("ft", ft);
		intent.putExtra("best_ft", best_ft);
		intent.putExtra("vid", selected_episode);
		intent.putExtra("index", episode_index);
        
		startActivity(intent);
	}
	
	private void popupSelectEpisodeDlg() {
		int size = mEpisodeList.size();
		if (size == 0) {
			Toast.makeText(this, "episode list is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> title_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			title_list.add(mEpisodeList.get(i).getTitle());
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);
		
		Dialog choose_episode_dlg = new AlertDialog.Builder(PPTVEpisodeActivity.this)
		.setTitle("Select episode")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				String vid = mEpisodeList.get(whichButton).getId();
				episode_title = mEpisodeList.get(whichButton).getTitle();
				episode_index = whichButton;
				new PPTVEpgTask().execute(TASK_DETAIL, Integer.valueOf(vid));
				dialog.dismiss();
			}
		})
		.setPositiveButton("More...", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						ep_page_index += ep_page_incr;
						if (ep_page_index > 0) {
							//
						}
						else
							Toast.makeText(PPTVEpisodeActivity.this, "No more episode", Toast.LENGTH_SHORT).show();
						
						dialog.dismiss();
					}
				})
		.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
			}})
		.create();
		choose_episode_dlg.show();
	}
	
	private class PPTVEpgTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "failed to get episode");
				Toast.makeText(PPTVEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			Log.i(TAG, "Java: PPTVEpgTask action " + action);
			
			if (action == TASK_DETAIL) {
				int vid = params[1];
				if (!mEPG.detail(String.valueOf(vid))) {
					Log.e(TAG, "Java: failed to call detail()");
					mhandler.sendEmptyMessage(MSG_FAIL_TO_DETAIL);
					return false;
				}
				
				mEpisodeList = mEPG.getLink();
				if (mEpisodeList.size() > 1)
					selected_episode = vid;
				
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
				if (!mEPG.list(epg_param, epg_type, album_page_index, epg_order, page_size, is_catalog)) {
					Log.e(TAG, "Java: failed to call list() more");
					noMoreData = true;
					return false;
				}
				
				mAlbumList = mEPG.getLink();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MOREDATA_DONE);
			}
			else if (action == TASK_MORESEARCH) {
				album_page_index++;
				if (!mEPG.search(search_key, 0, 0, album_page_index, page_size)) {
					Log.e(TAG, "Java: failed to call search() more");
					noMoreData = true;
					return false;
				}
				
				mAlbumList = mEPG.getLink();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MOREDATA_DONE);
			}
			else if (action == TASK_ITEM_FT) {
        		Log.i(TAG, "Java: EPGTask start to getCDNUrl");
        		
        		int vid = params[1];
        		int []ft_list = mEPG.getAvailableFT(String.valueOf(vid));
        		if (ft_list == null || ft_list.length == 0) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		int ft = -1;
        		for (int i=ft_list.length - 1;i>=0;i--) {
        			if (ft_list[i] >= 0 && ft_list[i] < 4) {
        				ft = ft_list[i];
        				break;
        			}
        		}
        		
        		if (ft == -1) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		Util.add_pptvvideo_history(PPTVEpisodeActivity.this, episode_title, vid, ft);
        		
        		Message msg = mhandler.obtainMessage(MSG_PLAY_CDN_FT, ft, ft);
    	        msg.sendToTarget();
        	}
			else {
				Log.e(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {
		
		@Override
		protected void onPostExecute(List<Map<String, Object>> result) {
			// TODO Auto-generated method stub
			if (result == null) {
				Log.e(TAG, "Java: failed to get data");
				return;
			}
			
			if (adapter == null) {
				adapter = new MySohuEpAdapter(PPTVEpisodeActivity.this, result);
				gridView.setAdapter(adapter);
			}
			else {
				List<Map<String, Object>> listData = adapter.getData();
				listData.clear();
				listData.addAll(result);
				adapter.notifyDataSetChanged();
			}
		}
		
		@Override
		protected List<Map<String, Object>> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
				if (!mEPG.search(search_key, 0, 0, album_page_index, page_size))
					return null;
				
				mAlbumList = mEPG.getLink();
			}
			else {
				if (!mEPG.list(epg_param, epg_type, album_page_index, epg_order, page_size, is_catalog))
					return null;
				
				mAlbumList = mEPG.getLink();
			}
			
			List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			Log.i(TAG, "Java album size: " + c);
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				PlayLink2 al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl());
				episode.put("tip", al.getVideoCount() + "集");
				episode.put("vid", al.getId());
				episode.put("desc", al.getDescription());
				items.add(episode);
			}
			
			return items;
		}
	}  
	
	public void interrupt() {
		interrupted = true;
	}
	
	private class PPBoxDownloadTask extends AsyncTask<String, Integer, Boolean> {
		private String mTitle;
		private String mSavePath;
		private long mFileSize = 0;
		private long mDownloadedSize = 0;
		
		ProgressDialog progressDialog;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressDialog = new ProgressDialog(PPTVEpisodeActivity.this);
			progressDialog.setTitle("下载中...");
			progressDialog.setMessage("文件\n下载进度: ");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(100);
			progressDialog.setIndeterminate(false);
			progressDialog.setCancelable(true);
	    	progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					// TODO Auto-generated method stub
					interrupt();
					Toast.makeText(getApplicationContext(), 
							"文件 " + mSavePath + " 下载取消", Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}
			});
	    	progressDialog.show();
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			progressDialog.dismiss();
			
			if (result) {
				Toast.makeText(PPTVEpisodeActivity.this, 
						String.format("file %s saved to %s(size %s)", 
								mTitle, mSavePath, getFileSize(mFileSize)),
						Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(PPTVEpisodeActivity.this, "failed to download file " + mTitle, 
						Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			if (params.length < 3)
				return false;
			
			String vid		= params[0];
			mSavePath		= params[1];
			mTitle			= params[2];
			
			publishProgress(0, 0);
			
			if (!mEPG.detail(String.valueOf(vid))) {
				Log.e(TAG, "Java: failed to call detail()");
				return false;
			}
			
			mEpisodeList = mEPG.getLink();
			if (mEpisodeList == null || mEpisodeList.size() == 0) {
				Log.e(TAG, "Java: mEpisodeList is empty");
				return false;
			}
			
			PlayLink2 link = mEpisodeList.get(0);
			String playlink = link.getId();
			int ft= Integer.parseInt(btnFt.getText().toString());
			
			//playcode:ppvod2:///23832333?ft=1&bwtype=3&platform=android3
			//&type=phone.android.download.vip&sv=4.1.3&p2p.source=7&bighead=true&p2p.level=1
			String playcode = String.format("%s?ft=%d&bwtype=3&platform=android3" +
					"&type=phone.android.download.vip&sv=4.1.3", playlink, ft);
			playcode = "ppvod2:///" + playcode + "&p2p.source=7&bighead=true&p2p.level=1";
			Log.i(TAG, String.format("Java: playcode %s, mSavePath %s", playcode, mSavePath));
			// TODO Auto-generated method stub
			long handle = -1;
			try {
				handle = MediaSDK.downloadOpen(playcode, "mp4", mSavePath, 
					new MediaSDK.Download_Callback() {

						@Override
						public void invoke(long result) {
							// TODO Auto-generated method stub
							Log.i(TAG, "Java: MediaSDK invoke " + result);
							
							/**
                             * sdk 正常关闭回调，handle <= 0不回调
                             * 0：成功
                             * 5：取消操作，调用了close
                             */
						}
				
				});
			}
			catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
			
			// open失败
            if (handle == 0 || handle == -1) {
            	Log.e(TAG, "Java: failed to open download session");
                return false;
            }
            
            Log.i(TAG, "Java: download handle: " + handle);
			
			while (true) {
                MediaSDK.Download_Statistic stat = new MediaSDK.Download_Statistic();
                long resultCode = -1;
                try {
                    resultCode = MediaSDK.getDownloadInfo(handle, stat);
                    Log.i(TAG, String.format("Java: download stat: %d/%d, speed %d kB/s", 
                    		stat.finish_size, stat.total_size, stat.speed / 1024));
                    if (stat.total_size > 0 && stat.speed > 0)
                    	publishProgress((int)(stat.finish_size * 100 / stat.total_size), stat.speed / 1024);
                }
                catch (Throwable e) {
                    // Log.v(TAG, "getDownloadInfo");
                    // addLog("getDownloadInfo\n");
                    e.printStackTrace();
                    MediaSDK.downloadClose(handle);
                    return false;
                }
                
                if (resultCode != 0) {
                    // 下载出错
                    Log.e(TAG, "Java: download error: " + resultCode);
                    MediaSDK.downloadClose(handle);
                    return false;
                }
                
                if (stat.total_size > 0 && stat.finish_size >= stat.total_size) {
                	Log.i(TAG, String.format("Java: download done! %s %s", 
                			stat.finish_size, stat.total_size));
                	break;
                }
                
                if (stat.total_size > 0 && mFileSize == 0)
                	mFileSize = stat.total_size;
                if (stat.finish_size > 0)
                	mDownloadedSize = stat.finish_size;
                
                if (interrupted) {
					Log.w(TAG, "interrupted by user");
					MediaSDK.downloadClose(handle);
					return false;
				}
                
                try {
                    Thread.sleep(300);
                }
                catch (InterruptedException e) {
                    
                }
			}
			
			MediaSDK.downloadClose(handle);
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			int progress = values[0];
			double speed = (double)values[1] / 1000.0f;
			progressDialog.setMessage(
					String.format("%s\n下载进度: %d%%\n下载速度 %.3f MB/sec %s/%s", 
							mSavePath, progress, speed, 
							getFileSize(mDownloadedSize), getFileSize(mFileSize)));
			progressDialog.setProgress(progress);	
		}
	}
	
	private String getFileSize(long size) {
	    String strSize;
	    if (size < 0)
	    	return "N/A";
	    
	    if (size > ONE_GIGABYTE)
			strSize = String.format("%.3f GB",
					(double) size / (double) ONE_GIGABYTE);
	    else if (size > ONE_MAGABYTE)
			strSize = String.format("%.3f MB",
					(double) size / (double) ONE_MAGABYTE);
		else if (size > ONE_KILOBYTE)
			strSize = String.format("%.3f kB",
					(double) size / (double) ONE_KILOBYTE);
		else
			strSize = String.format("%d Byte", size);
		return strSize;
    }
}

