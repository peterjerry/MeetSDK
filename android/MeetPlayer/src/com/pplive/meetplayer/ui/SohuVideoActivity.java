package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.List;

import com.pplive.common.sohu.ChannelSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.SubChannelSohu;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class SohuVideoActivity extends ListActivity {
	private final static String TAG = "SohuVideoActivity";
	
	private final static int EPG_TASK_LIST_CHANNEL		= 1;
	private final static int EPG_TASK_SELECT_CHANNEL		= 2;
	
	List<String> mItems; 
	
	SohuUtil mEPG;
	List<ChannelSohu> mChannelList;
	List<SubChannelSohu> mSubChannelList;
	String mChannelId;
	ArrayAdapter<String> mAdapter;
	boolean mSubChannelSelected = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		//this.setTheme(android.R.style.Theme_Black);
		
		mEPG = new SohuUtil();
		mItems = new ArrayList<String>();
		
		new FillChannelTask().execute(EPG_TASK_LIST_CHANNEL);
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mSubChannelSelected) {
			new FillChannelTask().execute(EPG_TASK_LIST_CHANNEL);
			return;
		}
		
		super.onBackPressed();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		//super.onListItemClick(l, v, position, id);
		
		if (mSubChannelSelected) {
			Intent intent = new Intent(SohuVideoActivity.this, SohuEpisodeActivity.class);
    		intent.putExtra("sub_channel_id", mSubChannelList.get(position).mSubChannelId);
    		startActivity(intent);
			return;
		}
		
		mChannelId = mChannelList.get(position).mChannelId;
		
		new FillChannelTask().execute(EPG_TASK_SELECT_CHANNEL);
	}
	
	private class FillChannelTask extends AsyncTask<Integer, Integer, List<String>> {

		@Override
        protected void onPostExecute(List<String> result) {
			if (result != null && result.size() > 0) {
				mAdapter = new ArrayAdapter<String>(
						SohuVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
				SohuVideoActivity.this.setListAdapter(mAdapter);
			}
        }
		
		@Override
		protected List<String> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			mItems.clear();
			
			if (action == EPG_TASK_LIST_CHANNEL) {
				mSubChannelSelected = false;
				
				if (!mEPG.channel_list())
					return null;
				
				mChannelList = mEPG.getChannelList();
				
	
				for (int i=0;i<mChannelList.size();i++) {
					mItems.add(mChannelList.get(i).mTitle);
				}
			}
			else if (action == EPG_TASK_SELECT_CHANNEL) {
				if (!mEPG.channel_select(mChannelId))
					return null;
				
				mSubChannelList = mEPG.getSubChannelList();
				List<String> items = new ArrayList<String>();
	
				for (int i=0;i<mSubChannelList.size();i++) {
					mItems.add(mSubChannelList.get(i).mTitle);
				}
				
				mSubChannelSelected = true;
			}
			
			return mItems;
		}
		
	}

}
