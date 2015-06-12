package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.pplive.common.sohu.AlbumSohu;
import com.pplive.common.sohu.EpisodeSohu;
import com.pplive.common.sohu.PlaylinkSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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


public class SohuEpisodeActivity extends Activity {
	private final static String TAG = "SohuEpisodeActivity";
	
	private GridView gridView = null;  
    private MySohuEpAdapter adapter = null;
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_PLAYLINK_DONE	= 2;
    private final static int MSG_MORELIST_DONE	= 3;
    private final static int MSG_NO_MORE_EPISODE	= 11;
    
    private final static long TASK_EPISODE		= 1L;
    private final static long TASK_PLAYLINK		= 2L;
    private final static long TASK_MORELIST		= 3L;
    private final static long TASK_VIDEO_INFO		= 4L;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH		= 2;
    
    private final static int page_size = 10;
    private int album_page_index = 1;
    private int ep_page_index = 1;
    private int ep_page_incr;
    private int search_page_index = 1;
    
    private List<Map<String, Object>> data2;
    
    private SohuUtil mEPG;
    private List<AlbumSohu> mAlbumList;
    private List<EpisodeSohu> mEpisodeList;
    private String mMoreList;
    private PlaylinkSohu mPlaylink;
    private int sub_channel_id		= -1;
    private long selected_aid		= -1;
    private long selected_site		= -1;
    private int selected_index		= -1;
    private String search_key;
    
    boolean noMoreData = false;
    boolean loadingMore = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_sohu_episode);  
		
		Intent intent = getIntent();
		sub_channel_id = intent.getIntExtra("sub_channel_id", -1);
		if (intent.hasExtra("search_key"))
			search_key = intent.getStringExtra("search_key");
		if (sub_channel_id == -1 && search_key == null) {
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
				selected_site = (Integer)item.get("site");
				
				if ((Boolean)item.get("is_album")) {
					selected_aid = (Long)item.get("aid");
					int last_count = (Integer)item.get("last_count");
					if (last_count > 30) {
						// " last_count - 1" fix 50 / 10 case
						ep_page_index = (last_count - 1) / page_size + 1;
						ep_page_incr = -1;
					}
					else {
						ep_page_index = 1;
						ep_page_incr = 1;
					}
					
					new SohuEpgTask().execute(TASK_EPISODE, selected_aid);
				}
				else {
					long aid	= (Long)item.get("aid");
					int vid		= (Integer)item.get("vid");
					
					if (aid > 1000000000000L)
						new SohuEpgTask().execute(TASK_VIDEO_INFO, aid, (long)vid, (long)selected_site);
					else
						new SohuEpgTask().execute(TASK_PLAYLINK, aid, (long)vid);
				}
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
		        	if (!loadingMore && mMoreList != null && !mMoreList.isEmpty()) {
		        		loadingMore = true;
		                new SohuEpgTask().execute(TASK_MORELIST);
		        	}
		        }
			}
		});
		
	    mEPG = new SohuUtil();
	    
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
            	if (mEpisodeList.size() == 1 && ep_page_index == 1) {
            		EpisodeSohu ep = mEpisodeList.get(0);
					selected_index = 0;
					new SohuEpgTask().execute(TASK_PLAYLINK, ep.mAid, (long)ep.mVid);
					
					return;
            	}
            	
            	popupSelectEpisodeDlg();
            	break;
            case MSG_PLAYLINK_DONE:
            	
            	SOHU_FT ft = SOHU_FT.SOHU_FT_ORIGIN;
            	String strUrl = mPlaylink.getUrl(ft);
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_SUPER;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_HIGH;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_NORMAL;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			Toast.makeText(SohuEpisodeActivity.this, "no stream available", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
        		Intent intent = new Intent(SohuEpisodeActivity.this, PlaySohuActivity.class);
        		intent.putExtra("url_list", strUrl);
        		intent.putExtra("duration_list", mPlaylink.getDuration(ft));
        		intent.putExtra("title", mPlaylink.getTitle());
        		intent.putExtra("index", (ep_page_index - 1) * page_size + selected_index);
        		intent.putExtra("aid", selected_aid);
        		startActivity(intent);
            	break;
            case MSG_MORELIST_DONE:
            	List<Map<String, Object>> listData = adapter.getData();
            	
    			int c = mAlbumList.size();
    			for (int i=0;i<c;i++) {
    				HashMap<String, Object> episode = new HashMap<String, Object>();
    				AlbumSohu al = mAlbumList.get(i);
    				
    				episode.put("title", al.getTitle());
    				episode.put("img_url", al.getImgUrl(true));
    				episode.put("tip", al.getTip());
    				episode.put("aid", al.getAid());
    				episode.put("vid", al.getVid());
    				episode.put("site", al.getSite());
    				episode.put("is_album", al.isAlbum());
    				episode.put("last_count", al.getLastCount());
    				listData.add(episode);
    			}
            	
            	adapter.notifyDataSetChanged();
            	break;
            case MSG_NO_MORE_EPISODE:
            	Toast.makeText(SohuEpisodeActivity.this, "No more episode", Toast.LENGTH_SHORT).show();
            	break;
            default:
            	break;
            }
        }
	};
	
	private void popupSelectEpisodeDlg() {
		int size = mEpisodeList.size();
		if (size == 0) {
			Toast.makeText(this, "episode list is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> title_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			title_list.add(mEpisodeList.get(i).mTitle);
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);
		
		Dialog choose_episode_dlg = new AlertDialog.Builder(SohuEpisodeActivity.this)
		.setTitle("Select episode")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				long aid	= mEpisodeList.get(whichButton).mAid;
				int vid		= mEpisodeList.get(whichButton).mVid;
				selected_index = whichButton;
				
				if (aid > 1000000000000L)
					new SohuEpgTask().execute(TASK_VIDEO_INFO, aid, (long)vid, (long)selected_site);
				else
					new SohuEpgTask().execute(TASK_PLAYLINK, aid, (long)vid);
				
				dialog.dismiss();
			}
		})
		.setPositiveButton("More...", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						ep_page_index += ep_page_incr;
						if (ep_page_index > 0)
							new SohuEpgTask().execute(TASK_EPISODE, selected_aid);
						else
							Toast.makeText(SohuEpisodeActivity.this, "No more episode", Toast.LENGTH_SHORT).show();
						
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
		String value = Util.readSettings(SohuEpisodeActivity.this, key);
		
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
        
		Util.writeSettings(SohuEpisodeActivity.this, key, sb.toString());
	}
	
	private class SohuEpgTask extends AsyncTask<Long, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "failed to get episode");
				Toast.makeText(SohuEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Long... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			Log.i(TAG, "Java: SohuEpgTask action " + action);
			
			if (action == TASK_EPISODE) {
				long aid = params[1];
				if (!mEPG.episode(aid, ep_page_index, page_size)) {
					Log.e(TAG, "Java: failed to call episode()");
					mhandler.sendEmptyMessage(MSG_NO_MORE_EPISODE);
					return true;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_PLAYLINK){
				if (params.length < 3) {
					Log.e(TAG, "Java: TASK_PLAYLINK params.lenght is invalid: " + params.length);
					return false;
				}
				
				long aid 	= params[1];
				long vid 	= params[2];
				
				mPlaylink = mEPG.playlink_pptv((int)vid, 0);
				if (mPlaylink == null) {
					Log.e(TAG, "Java: failed to call playlink_pptv() vid: " + vid);
					return false;
				}
				
				add_video_history(mPlaylink.getTitle(), (int)vid, -1, -1);
				
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
				if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
					Log.e(TAG, "Java: failed to call morelist() morelist " + mMoreList);
					noMoreData = true;
					return false;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MORELIST_DONE);
			}
			else if (action == TASK_VIDEO_INFO) {
				if (params.length < 4) {
					Log.e(TAG, "Java: TASK_VIDEO_INFO params.lenght is invalid: " + params.length);
					return false;
				}
				
				long aid 	= params[1];
				long vid 	= params[2];
				long site	= params[3];
				mPlaylink = mEPG.video_info((int)site, (int)vid, aid);
				if (mPlaylink == null) {
					Log.e(TAG, "Java: failed to call video_info() vid: " + vid);
					return false;
				}
				
				add_video_history(mPlaylink.getTitle(), (int)vid, aid, (int)site);
				
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
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
			
			adapter = new MySohuEpAdapter(SohuEpisodeActivity.this, data2);
		    gridView.setAdapter(adapter);  
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
				if (!mEPG.search(search_key, search_page_index, page_size))
					return false;
				
				mAlbumList = mEPG.getSearchItemList();
			}
			else {
				if (!mEPG.subchannel(sub_channel_id, page_size, 1)) {
					Log.e(TAG, "Java: failed to call subchannel()");
					return false;
				}
				
				mMoreList = mEPG.getMoreList();
				if (mMoreList != null && !mMoreList.isEmpty()) {
					if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
						Log.e(TAG, "Java: failed to call morelist()");
						return false;
					}
				}
				else {
					Log.w(TAG, "Java: morelist param is empty");
				}
				
				mAlbumList = mEPG.getAlbumList();
			}
						  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			Log.i(TAG, "Java album size: " + c);
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				AlbumSohu al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl(true));
				episode.put("tip", al.getTip());
				episode.put("aid", al.getAid());
				episode.put("vid", al.getVid());
				episode.put("site", al.getSite());
				episode.put("is_album", al.isAlbum());
				episode.put("last_count", al.getLastCount());
				data2.add(episode);
			}
			
			return true;
		}
	}  
}
