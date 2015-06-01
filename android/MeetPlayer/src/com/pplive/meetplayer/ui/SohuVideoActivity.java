package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.List;

import com.pplive.common.sohu.ChannelSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.SubChannelSohu;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.Util;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


public class SohuVideoActivity extends ListActivity {
	private final static String TAG = "SohuVideoActivity";
	
	private final static int EPG_TASK_LIST_CHANNEL		= 1;
	private final static int EPG_TASK_SELECT_CHANNEL		= 2;
	
	private List<String> mItems; 
	
	private SohuUtil mEPG;
	private List<ChannelSohu> mChannelList;
	private List<SubChannelSohu> mSubChannelList;
	private String mChannelId;
	private ArrayAdapter<String> mAdapter;
	boolean mSubChannelSelected = false;
	
	private String mEPGsearchKey;
	
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
    public boolean onCreateOptionsMenu(Menu menu) {  
        MenuInflater menuInflater = new MenuInflater(getApplication());  
        menuInflater.inflate(R.menu.sohu_menu, menu);  
        return super.onCreateOptionsMenu(menu);  
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		Log.i(TAG, "Java: onOptionsItemSelected " + id);
		
		switch (id) {
		case R.id.search:
			popupSearch();
			break;
		default:
			Log.w(TAG, "unknown menu id " + id);
			break;
		}
		
		return true;
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
	
	private void popupSearch() {
		AlertDialog.Builder builder;
		
		final EditText inputKey = new EditText(this);
    	String last_key = Util.readSettings(this, "last_searchkey");
    	Log.i(TAG, "Java last_key: " + last_key);
    	inputKey.setText(last_key);
		inputKey.setHint("input search key");
		
        builder = new AlertDialog.Builder(this);
        builder.setTitle("input key").setIcon(android.R.drawable.ic_dialog_info).setView(inputKey)
                .setNegativeButton("Cancel", null);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
            	mEPGsearchKey = inputKey.getText().toString();
            	Log.i(TAG, "Java save last_key: " + mEPGsearchKey);
            	Util.writeSettings(SohuVideoActivity.this, "last_searchkey", mEPGsearchKey);

            	Intent intent = new Intent(SohuVideoActivity.this, SohuEpisodeActivity.class);
        		intent.putExtra("search_key", mEPGsearchKey);
        		startActivity(intent);
             }
        });
        builder.show();
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
