package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pplive.common.sohu.AlbumSohu;
import com.pplive.common.sohu.EpisodeSohu;
import com.pplive.common.sohu.PlaylinkSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;
import com.pplive.meetplayer.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


public class SohuEpisodeActivity extends Activity {
	private final static String TAG = "SohuEpisodeActivity";
	
	private GridView 						gridView = null;  
    private MySohuEpAdapter 				adapter = null;  
    private SimpleAdapter					adapter1 = null;  
    private List<HashMap<String,Object>>	list = null;  
    private HashMap<String,Object> 			map = null;  
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_PLAYLINK_DONE	= 2;
    
    private final static int TASK_EPISODE			= 1;
    private final static int TASK_PLAYLINK		= 2;
    
    private final static int page_size = 10;
    private int page_index = 1;
    
    private List<Map<String, Object>> data2;
    
    SohuUtil mEPG;
    List<AlbumSohu> mAlbumList;
    List<EpisodeSohu> mEpisodeList;
    PlaylinkSohu mPlaylink;
    int sub_channel_id = -1;
    int selected_aid = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_sohu_episode);  
		
		Intent intent = getIntent();
		sub_channel_id = intent.getIntExtra("sub_channel_id", -1);
		if (sub_channel_id == -1) {
			Toast.makeText(this, "failed to get sub_channel_id", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		gridView = (GridView) findViewById(R.id.grid_view);
		
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int numColumns = screen_width / 256;
		gridView.setNumColumns(numColumns);
		
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				//reset page_index
				page_index = 1;
				
				AlbumSohu al = mAlbumList.get(position);
				int aid = al.getAid();
				selected_aid = aid;
				new EpisodeTask().execute(TASK_EPISODE, aid);
			}
			
		});
		
	    list = new ArrayList<HashMap<String,Object>>();  
	    
	    mEPG = new SohuUtil();
	    
        new SetDataTask().execute();
	}
	
	private Handler mhandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_EPISODE_DONE:
            	popupSelectEpisodeDlg();
            	break;
            case MSG_PLAYLINK_DONE:
            	Intent intent = new Intent(SohuEpisodeActivity.this, PlaySohuActivity.class);
        		intent.putExtra("url_list", mPlaylink.getUrl(SOHU_FT.SOHU_FT_HIGH));
        		intent.putExtra("duration_list", mPlaylink.getDuration(SOHU_FT.SOHU_FT_HIGH));
        		intent.putExtra("title", mPlaylink.getTitle());
        		intent.putExtra("info_id", -1);
        		intent.putExtra("index", -1);
        		startActivity(intent);
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
				int aid = mEpisodeList.get(whichButton).mAid;
				int vid = mEpisodeList.get(whichButton).mVid;
				new EpisodeTask().execute(TASK_PLAYLINK, aid, vid);
				dialog.dismiss();
			}
		})
		.setPositiveButton("More...", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						page_index++;
						new EpisodeTask().execute(TASK_EPISODE, selected_aid);
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
	
	private class EpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Toast.makeText(SohuEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			if (action == TASK_EPISODE) {
				int aid = params[1];
				if (!mEPG.episode(aid, page_index, page_size))
					return false;
				
				mEpisodeList = mEPG.getEpisodeList();
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_PLAYLINK){
				int aid = params[1];
				int vid = params[2];
				mPlaylink = mEPG.detail(vid, aid);
				if (mPlaylink == null)
					return false;
				
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
			}

			return true;
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Toast.makeText(SohuEpisodeActivity.this, "failed to get sub channel", 
						Toast.LENGTH_SHORT).show();
				return;
			}
			
			adapter = new MySohuEpAdapter(SohuEpisodeActivity.this, data2);
		    gridView.setAdapter(adapter);  
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			if (!mEPG.subchannel(sub_channel_id, /*page_size*/100, page_index))
				return false;
			
			mAlbumList = mEPG.getAlbumList();
			  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				AlbumSohu al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl(true));
				data2.add(episode);
			}
			
			return true;
		}
	}  
}
