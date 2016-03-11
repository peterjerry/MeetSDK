package com.gotye.meetplayer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.gotye.common.youku.Catalog;
import com.gotye.common.youku.Channel;
import com.gotye.common.youku.YKUtil;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class YoukuVideoActivity extends AppCompatActivity {
	private final static String TAG = "YoukuVideoActivity";
	
	private final static int EPG_TASK_LIST_CHANNEL		= 1;
	private final static int EPG_TASK_SELECT_CATALOG	= 2;
	private final static int EPG_TASK_GET_PLAYLINK		= 3;
	private final static int EPG_TASK_GET_VIDEOINFO		= 4;
	
	private final static int MSG_PLAYLINK_DONE			= 1001;
	private final static int MSG_FAIL_TO_GET_PLAYURL	= 2001;
	private final static int MSG_FAIL_TO_CHANNEL_LIST	= 2002;
	private final static int MSG_FAIL_TO_SELECT_CATALOG	= 2003;

	private List<Channel> mChannelList;
	private List<Catalog> mCatalogList;
	private int mChannelId;
	private ArrayAdapter<String> mAdapter;
	private boolean mSubChannelSelected = false;

	private String mEPGsearchKey;

	private ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mListView = new ListView(this);
		setContentView(mListView);

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mSubChannelSelected) {
					Intent intent = new Intent(YoukuVideoActivity.this, YoukuEpisodeActivity.class);
                    intent.putExtra("title", mCatalogList.get(position).getTitle());
                    intent.putExtra("channel_id", mChannelId);
                    intent.putExtra("filter", mCatalogList.get(position).getFilter());
					intent.putExtra("sub_channel_id", mCatalogList.get(position).getSubChannelId());
                    intent.putExtra("sub_channel_type", mCatalogList.get(position).getSubChannelType());
					startActivity(intent);
				}
                else {
                    mChannelId = mChannelList.get(position).getChannelId();
                    new EPGTask().execute(EPG_TASK_SELECT_CATALOG);
                }
			}
		});
		
		new EPGTask().execute(EPG_TASK_LIST_CHANNEL);
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
		case R.id.play_history:
			popupHistory();
			break;
		default:
			Log.w(TAG, "unknown menu id " + id);
			break;
		}
		
		return true;
    }
	
	private Handler mhandler = new Handler(){  
		  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_FAIL_TO_GET_PLAYURL:
            	Toast.makeText(YoukuVideoActivity.this, "failed to get playlink",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_CHANNEL_LIST:
            	Toast.makeText(YoukuVideoActivity.this, "failed to do channel list",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_SELECT_CATALOG:
            	Toast.makeText(YoukuVideoActivity.this, "failed to select catalog",
            			Toast.LENGTH_SHORT).show();
            	break;
            default:
            	Toast.makeText(YoukuVideoActivity.this, "invalid msg: " + msg.what,
            			Toast.LENGTH_SHORT).show();
            	break;
            }
        }
	};
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mSubChannelSelected) {
			new EPGTask().execute(EPG_TASK_LIST_CHANNEL);
			return;
		}
		
		super.onBackPressed();
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
            	Util.writeSettings(YoukuVideoActivity.this, "last_searchkey", mEPGsearchKey);

            	Intent intent = new Intent(YoukuVideoActivity.this, SohuEpisodeActivity.class);
        		intent.putExtra("search_key", mEPGsearchKey);
        		startActivity(intent);
        		
        		dialog.dismiss();
             }
        });
        builder.show();
	}
	
	private void popupHistory() {

	}
	
	private class EPGTask extends AsyncTask<Integer, Integer, List<String>> {

		private ProgressDialog progDlg = null;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progDlg = new ProgressDialog(YoukuVideoActivity.this);
			progDlg.setMessage("数据请求中...");
			progDlg.setCancelable(true);
			progDlg.show();
		}
		
		@Override
        protected void onPostExecute(List<String> result) {
			progDlg.dismiss();
			
			if (result != null && result.size() > 0) {
				if (mAdapter == null) {
					mAdapter = new ArrayAdapter<String>(
							YoukuVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
					YoukuVideoActivity.this.mListView.setAdapter(mAdapter);
				}
				else {
					mAdapter.clear();
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
					YoukuVideoActivity.this.mListView.setSelection(0);
				}
			}
        }
		
		@Override
		protected List<String> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			List<String> Items = null;
			
			if (action == EPG_TASK_LIST_CHANNEL) {
				mSubChannelSelected = false;

				mChannelList = YKUtil.getChannel();
				if (mChannelList == null) {
					Log.e(TAG, "Java: failed to call getChannel():");
					mhandler.sendEmptyMessage(MSG_FAIL_TO_CHANNEL_LIST);
					return null;
				}
				
				Items = new ArrayList<String>();
	
				for (int i=0;i<mChannelList.size();i++) {
					Items.add(mChannelList.get(i).getTitle());
				}
			}
			else if (action == EPG_TASK_SELECT_CATALOG) {
				mCatalogList = YKUtil.getCatalog(mChannelId);
				if (mCatalogList == null) {
					Log.e(TAG, "Java: failed to call getCatalog() channel_id: " + mChannelId);
					mhandler.sendEmptyMessage(MSG_FAIL_TO_SELECT_CATALOG);
					return null;
				}
	
				Items = new ArrayList<String>();
				
				for (int i=0;i<mCatalogList.size();i++) {
					Items.add(mCatalogList.get(i).getTitle());
				}
				
				mSubChannelSelected = true;
			}
			
			return Items;
		}
		
	}

}
