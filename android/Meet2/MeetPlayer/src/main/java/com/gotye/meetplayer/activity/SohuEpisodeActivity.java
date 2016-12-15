package com.gotye.meetplayer.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gotye.common.sohu.AlbumSohu;
import com.gotye.common.sohu.EpisodeSohu;
import com.gotye.common.sohu.PlaylinkSohu;
import com.gotye.common.sohu.PlaylinkSohu.SohuFtEnum;
import com.gotye.common.sohu.SohuUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.CommonAlbumAdapter;
import com.gotye.meetplayer.util.Util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;


public class SohuEpisodeActivity extends AppCompatActivity {
	private final static String TAG = "SohuEpisodeActivity";
	
	private GridView gridView = null;  
    private CommonAlbumAdapter adapter = null;
    
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
	private int selected_vid		= -1;
    private int selected_site		= -1;
    private int selected_index		= -1;
    private String search_key;
    
    boolean noMoreData = false;
    boolean loadingMore = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		Intent intent = getIntent();
		sub_channel_id = intent.getIntExtra("sub_channel_id", -1);
		if (intent.hasExtra("search_key"))
			search_key = intent.getStringExtra("search_key");
		if (sub_channel_id == -1 && search_key == null) {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_sohu_episode);  
		
		this.gridView = (GridView) findViewById(R.id.grid_view);
		this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

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
						// " last_count - 1" fix 709 case
						ep_page_index = (last_count + 9) / page_size;
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
		
		this.gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v,
					int position, long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				String description = (String)item.get("desc");
				new AlertDialog.Builder(SohuEpisodeActivity.this)
					.setTitle("专辑介绍")
					.setMessage(description)
					.setPositiveButton("确定", null)
					.show();
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
				//LogUtil.debug(TAG, String.format("Java: onScroll first %d, visible %d, total %d",
				//		firstVisibleItem, visibleItemCount, totalItemCount));
				
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
            	
            	SohuFtEnum ft = SohuFtEnum.SOHU_FT_ORIGIN;
            	String strUrl = mPlaylink.getUrl(ft);
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SohuFtEnum.SOHU_FT_SUPER;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SohuFtEnum.SOHU_FT_HIGH;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SohuFtEnum.SOHU_FT_NORMAL;
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
                intent.putExtra("vid", selected_vid);
        		intent.putExtra("site", selected_site);
        		intent.putExtra("ft", ft.value());
				intent.putExtra("player_impl", 1/*HW_SYSTEM*/);
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
    				episode.put("desc", al.getDescription());
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
		.setNeutralButton("Page", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						popupSelectPage(ep_page_index);
						
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
	
	private void popupSelectPage(int default_page) {
		AlertDialog.Builder builder;
		
		final EditText inputKey = new EditText(this);
    	inputKey.setText(String.valueOf(default_page));
		inputKey.setHint("select episode page");
		
        builder = new AlertDialog.Builder(this);
        builder.setTitle("input page number").setIcon(android.R.drawable.ic_dialog_info).setView(inputKey)
                .setNegativeButton("Cancel", null);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
            	ep_page_index = Integer.valueOf(inputKey.getText().toString());
            	new SohuEpgTask().execute(TASK_EPISODE, selected_aid);
        		dialog.dismiss();
             }
        });
        builder.show();
	}
	
	private class SohuEpgTask extends AsyncTask<Long, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			
			if (!result) {
				LogUtil.error(TAG, "failed to get episode");
				Toast.makeText(SohuEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Long... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			LogUtil.info(TAG, "Java: SohuEpgTask action " + action);
			
			if (action == TASK_EPISODE) {
				long aid = params[1];
				if (!mEPG.episode(aid, ep_page_index, page_size)) {
					LogUtil.error(TAG, "Java: failed to call episode()");
					mhandler.sendEmptyMessage(MSG_NO_MORE_EPISODE);
					return true;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_PLAYLINK){
				if (params.length < 3) {
					LogUtil.error(TAG, "Java: TASK_PLAYLINK params.lenght is invalid: " + params.length);
					return false;
				}
				
				long aid 	= params[1];
				long vid 	= params[2];
				
				mPlaylink = mEPG.playlink_pptv((int)vid, 0);
				if (mPlaylink == null) {
					LogUtil.error(TAG, "Java: failed to call playlink_pptv() vid: " + vid);
					return false;
				}
				
				Util.add_sohuvideo_history(SohuEpisodeActivity.this, 
						mPlaylink.getTitle(), (int)vid, -1, -1);

				selected_vid = (int)vid;
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
				if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
					LogUtil.error(TAG, "Java: failed to call morelist() morelist " + mMoreList);
					noMoreData = true;
					return false;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MORELIST_DONE);
			}
			else if (action == TASK_VIDEO_INFO) {
				if (params.length < 4) {
					LogUtil.error(TAG, "Java: TASK_VIDEO_INFO params.lenght is invalid: " + params.length);
					return false;
				}
				
				long aid 	= params[1];
				long vid 	= params[2];
				long site	= params[3];
				mPlaylink = mEPG.video_info((int)site, (int)vid, aid);
				if (mPlaylink == null) {
					LogUtil.error(TAG, "Java: failed to call video_info() vid: " + vid);
					return false;
				}
				
				Util.add_sohuvideo_history(SohuEpisodeActivity.this, 
						mPlaylink.getTitle(), (int)vid, aid, (int)site);

				selected_vid = (int)vid;
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
			}
			else {
				LogUtil.error(TAG, "Java: invalid action type: " + action);
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
				LogUtil.error(TAG, "Java: failed to get sub channel");
				return;
			}
			
			adapter = new CommonAlbumAdapter(SohuEpisodeActivity.this, data2);
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
					LogUtil.error(TAG, "Java: failed to call subchannel()");
					return false;
				}
				
				mMoreList = mEPG.getMoreList();
				if (mMoreList != null && !mMoreList.isEmpty()) {
					if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
						LogUtil.error(TAG, "Java: failed to call morelist()");
						return false;
					}
				}
				else {
					LogUtil.warn(TAG, "Java: morelist param is empty");
				}
				
				mAlbumList = mEPG.getAlbumList();
			}
						  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				AlbumSohu al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl(true));
				episode.put("desc", al.getDescription());
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
