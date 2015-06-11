package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;


public class PPTVEpisodeActivity extends Activity {
	private final static String TAG = "PPTVEpisodeActivity";
	
	private GridView gridView = null;  
    private MySohuEpAdapter adapter = null;
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_MORELIST_DONE	= 2;
    private final static int MSG_PLAY_CDN_FT		= 3;
    private final static int MSG_NO_MORE_EPISODE	= 11;
    private final static int MSG_FAIL_TO_DETAIL	= 12;
    private final static int MSG_FAIL_TO_GET_FT	= 13;
    
    private final static int TASK_DETAIL			= 1;
    private final static int TASK_MORELIST		= 2;
    private final static int TASK_ITEM_FT			= 3;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH		= 2;
    
    private final static int page_size = 10;
    private int album_page_index = 1;
    private int ep_page_index = 1;
    private int ep_page_incr;
    private int search_page_index = 1;
    
    private List<Map<String, Object>> data2;
    
    private EPGUtil mEPG;
    private String epg_param;
    private String epg_type;
    private String search_key;
    private List<PlayLink2> mAlbumList;
    private List<PlayLink2> mEpisodeList;
    
    boolean noMoreData = false;
    boolean loadingMore = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_sohu_episode);  
		
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
		
		gridView = (GridView) findViewById(R.id.grid_view);
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				String vid = (String)item.get("vid");
				new PPTVEpgTask().execute(TASK_DETAIL, Integer.valueOf(vid));
			}
			
		});
		
		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			
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
	}
	
	private Handler mhandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_EPISODE_DONE:
            	if (mEpisodeList.size() == 1) {
            		String vid = mEpisodeList.get(0).getId();
            		new PPTVEpgTask().execute(TASK_ITEM_FT, Integer.valueOf(vid));
            		
					return;
            	}
            	
            	popupSelectEpisodeDlg();
            	break;
            case MSG_MORELIST_DONE:
            	List<Map<String, Object>> listData = adapter.getData();
            	
    			int c = mAlbumList.size();
    			for (int i=0;i<c;i++) {
    				HashMap<String, Object> episode = new HashMap<String, Object>();
    				PlayLink2 al = mAlbumList.get(i);
    				
    				episode.put("title", al.getTitle());
    				episode.put("img_url", al.getImgUrl());
    				episode.put("tip", al.getVideoCount() + "集");
    				episode.put("vid", al.getId());
    				listData.add(episode);
    			}
            	
            	adapter.notifyDataSetChanged();
            	break;
            case MSG_PLAY_CDN_FT:
            	play_video(msg.arg1);
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
	
	private void play_video(int ft) {
		String playlink = mEpisodeList.get(0).getId();
		
		String info = String.format("ready to play video playlink: %s, ft: %d", playlink, ft);
		Log.i(TAG, info);
		Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
		
		short port = MediaSDK.getPort("http");
		String url = PlayLinkUtil.getPlayUrl(Integer.valueOf(playlink), port, ft, 3, null);
		
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(PPTVEpisodeActivity.this,
				VideoPlayerActivity.class);
		Log.i(TAG, "to play uri: " + uri.toString());

		intent.setData(uri);
		intent.putExtra("impl", 0);
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
	
	private void add_video_history(String title, int vid, long aid, int site) {
		String key = "SohuPlayHistory";
		String regularEx = ",";
		final int save_max_count = 10;
		String value = Util.readSettings(PPTVEpisodeActivity.this, key);
		
		List<String> playHistoryList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, regularEx, false);
        while (st.hasMoreElements()) {
        	String token = st.nextToken();
        	playHistoryList.add(token);
        }
        
        String new_video = String.format("%s|%d|%d|%d", title, vid, aid, site);
        
        int count = playHistoryList.size();
        StringBuffer sb = new StringBuffer();
        int start = count - save_max_count + 1;
        if (start < 0)
        	start = 0;
        
        boolean isNewVideo = true;
        for (int i = start; i<count ; i++) {
        	String item = playHistoryList.get(i);
        	if (new_video.contains(item) && isNewVideo)
        		isNewVideo = false;
        	
        	sb.append(item);
        	sb.append(regularEx);
        }
        
        if (isNewVideo)
        	sb.append(new_video);
        else
        	Log.i(TAG, String.format("Java %s already in history list", new_video));
        
		Util.writeSettings(PPTVEpisodeActivity.this, key, sb.toString());
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
					return true;
				}
				
				mEpisodeList = mEPG.getLink();
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
				if (!mEPG.list(epg_param, epg_type, album_page_index, "order=t", page_size)) {
					Log.e(TAG, "Java: failed to call list() more");
					noMoreData = true;
					return false;
				}
				
				mAlbumList = mEPG.getLink();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MORELIST_DONE);
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
        		
        		Message msg = mhandler.obtainMessage(MSG_PLAY_CDN_FT, ft, 0);
    	        msg.sendToTarget();
        	}
			else {
				Log.e(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "Java: failed to get sub channel");
				return;
			}
			
			adapter = new MySohuEpAdapter(PPTVEpisodeActivity.this, data2);
		    gridView.setAdapter(adapter);  
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
				
			}
			else {
				if (!mEPG.list(epg_param, epg_type, album_page_index, "order=t", page_size))
					return false;
				
				mAlbumList = mEPG.getLink();
			}
						  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			Log.i(TAG, "Java album size: " + c);
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				PlayLink2 al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl());
				episode.put("tip", al.getVideoCount() + "集");
				episode.put("vid", al.getId());
				data2.add(episode);
			}
			
			return true;
		}
	}  
}

