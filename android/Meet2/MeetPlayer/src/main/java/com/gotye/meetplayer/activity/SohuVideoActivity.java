package com.gotye.meetplayer.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.gotye.common.sohu.ChannelSohu;
import com.gotye.common.sohu.PlaylinkSohu;
import com.gotye.common.sohu.SohuUtil;
import com.gotye.common.sohu.SubChannelSohu;
import com.gotye.common.sohu.PlaylinkSohu.SohuFtEnum;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

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
import android.text.TextUtils;
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


public class SohuVideoActivity extends AppCompatActivity {
	private final static String TAG = "SohuVideoActivity";
	
	private final static int EPG_TASK_LIST_CHANNEL		= 1;
	private final static int EPG_TASK_SELECT_CHANNEL		= 2;
	private final static int EPG_TASK_GET_PLAYLINK		= 3;
	private final static int EPG_TASK_GET_VIDEOINFO		= 4;
	
	private final static int MSG_PLAYLINK_DONE			= 1001;
	private final static int MSG_FAIL_TO_GET_PLAYLINK		= 2001;
	private final static int MSG_FAIL_TO_CHANNEL_LIST		= 2002;
	private final static int MSG_FAIL_TO_CHANNEL_SELECT	= 2003; 
	
	private SohuUtil mEPG;
	private List<ChannelSohu> mChannelList;
	private List<SubChannelSohu> mSubChannelList;
	private String mChannelId;
	private ArrayAdapter<String> mAdapter;
	private boolean mSubChannelSelected = false;
	private PlaylinkSohu mPlaylink;
	private long mAid;
	
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
				if (!TextUtils.isEmpty(mChannelId) && mChannelId.equals("1000021000")) {
					// 电视剧
					Intent intent = new Intent(SohuVideoActivity.this, SohuEpisodeActivity.class);
					intent.putExtra("channelId", mChannelId);
					startActivity(intent);
					return;
				}

				if (mSubChannelSelected) {
					Intent intent = new Intent(SohuVideoActivity.this, SohuEpisodeActivity.class);
					intent.putExtra("sub_channel_id", mSubChannelList.get(position).mSubChannelId);
					startActivity(intent);
					return;
				}

				mChannelId = mChannelList.get(position).mChannelId;

				new EPGTask().execute(EPG_TASK_SELECT_CHANNEL);
			}
		});
		
		mEPG = new SohuUtil();

		Util.checkNetworkType(this);

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
            case R.id.search_history:
                popupSearchHistory();
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
        			Toast.makeText(SohuVideoActivity.this, "no stream available", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
        		Intent intent = new Intent(SohuVideoActivity.this, PlaySohuActivity.class);
        		intent.putExtra("url_list", strUrl);
        		intent.putExtra("duration_list", mPlaylink.getDuration(ft));
        		intent.putExtra("title", mPlaylink.getTitle());
        		startActivity(intent);
            	break;
            case MSG_FAIL_TO_GET_PLAYLINK:
            	Toast.makeText(SohuVideoActivity.this, "failed to get playlink",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_CHANNEL_LIST:
            	Toast.makeText(SohuVideoActivity.this, "failed to do channel list",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_CHANNEL_SELECT:
            	Toast.makeText(SohuVideoActivity.this, "failed to do channel select",
            			Toast.LENGTH_SHORT).show();
            	break;
            default:
            	Toast.makeText(SohuVideoActivity.this, "invalid msg: " + msg.what,
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
		final String search_keys = Util.readSettings(this, "search_keys");
		String keyword = search_keys;
		int pos = search_keys.indexOf("|");
		if (pos > 0) {
			keyword = search_keys.substring(0, pos);
		}
		inputKey.setText(keyword);
		inputKey.setHint("输入搜索关键词");

		builder = new AlertDialog.Builder(this);
		builder.setTitle("输入搜索关键词").setIcon(android.R.drawable.ic_dialog_info).setView(inputKey)
				.setNegativeButton("取消", null);
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				mEPGsearchKey = inputKey.getText().toString();
				boolean bDuplicated = false;
				StringTokenizer st = new StringTokenizer(search_keys, "|", false);
				while (st.hasMoreElements()) {
					String keyword = st.nextToken();
					if (keyword.equals(mEPGsearchKey)) {
						bDuplicated = true;
						break;
					}
				}
				if (!bDuplicated) {
					String new_search_keys = mEPGsearchKey;
					new_search_keys += "|";
					new_search_keys += search_keys;
					Util.writeSettings(SohuVideoActivity.this, "search_keys", new_search_keys);
				}

				Intent intent = new Intent(SohuVideoActivity.this, SohuEpisodeActivity.class);
				intent.putExtra("search_key", mEPGsearchKey);
				startActivity(intent);

				dialog.dismiss();
			}
		});
		builder.show();
	}

    private void popupSearchHistory() {
        String search_keys = Util.readSettings(this, "search_keys");
        StringTokenizer st = new StringTokenizer(search_keys, "|", false);
        List<String> key_list = new ArrayList<String>();
        while (st.hasMoreElements()) {
            String keyword = st.nextToken();
            key_list.add(keyword);
        }

        if (key_list.isEmpty()) {
            Toast.makeText(this, "搜索记录为空", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] keywords = key_list.toArray(new String[key_list.size()]);

        Dialog choose_search_dlg = new AlertDialog.Builder(this)
                .setTitle("选择搜索关键词")
                .setNegativeButton("取消", null)
				.setNeutralButton("清空记录", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Util.writeSettings(SohuVideoActivity.this, "search_keys", "");
						Toast.makeText(SohuVideoActivity.this, "搜索记录已清空",
								Toast.LENGTH_SHORT).show();
					}
				})
                .setItems(keywords,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(
                                        SohuVideoActivity.this, SohuEpisodeActivity.class);
                                intent.putExtra("search_key", keywords[whichButton]);
                                startActivity(intent);

                                dialog.dismiss();
                            }
                        })
                .create();
        choose_search_dlg.show();
    }
	
	private void popupHistory() {
		final String key = "SohuPlayHistory";
		final String regularEx = ",";
		String value = Util.readSettings(SohuVideoActivity.this, key);
		
		List<String> TitleList = new ArrayList<String>();
		final List<String> videoInfoList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, regularEx, false);
        while (st.hasMoreElements()) {
        	String token = st.nextToken();
        	int pos = token.indexOf("|");
        	if (pos != -1) {
        		TitleList.add(token.substring(0, pos));
        		videoInfoList.add(token.substring(pos + 1));
        	}
        }
		
        int size = TitleList.size();
        if (size == 0) {
        	Toast.makeText(SohuVideoActivity.this, "no sohu playlink history", Toast.LENGTH_SHORT).show();
        	return;
        }
        
		final String[] str_title_list = (String[])TitleList.toArray(new String[size]);
		
		Dialog choose_history_dlg = new AlertDialog.Builder(SohuVideoActivity.this)
		.setTitle("Select video")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
        		String link_info = videoInfoList.get(whichButton);
        		int pos1, pos2;
        		
        		int vid;
        		long aid;
        		int site;
        		
        		pos1 = link_info.indexOf("|");
        		if (pos1 == -1) {
        			vid = Integer.valueOf(link_info);
        			aid = -1;
        			site= -1;
        		}
        		else {
        			vid = Integer.valueOf(link_info.substring(0, pos1));
        			pos2 = link_info.indexOf("|", pos1 + 1);
	        		aid = Long.valueOf(link_info.substring(pos1 + 1, pos2));
	        		site = Integer.valueOf(link_info.substring(pos2 + 1));
        		}
        		
        		Log.i(TAG, String.format("ready to play %s(vid %d, aid %d, site %d)", 
						str_title_list[whichButton], vid, aid, site));
				if (aid > 1000000000000L) {
					mAid = aid;
					new EPGTask().execute(EPG_TASK_GET_VIDEOINFO, vid, site);
				}
				else {
					new EPGTask().execute(EPG_TASK_GET_PLAYLINK, vid);
				}

				dialog.dismiss();
			}
		})
		.setNeutralButton("Clear",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					Util.writeSettings(SohuVideoActivity.this, key, "");
					Toast.makeText(SohuVideoActivity.this, "sohu playlink history was clear", 
							Toast.LENGTH_LONG).show();
			}})
		.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
			}})
		.create();
		
		choose_history_dlg.show();
	}
	
	private class EPGTask extends AsyncTask<Integer, Integer, List<String>> {

		private ProgressDialog progDlg = null;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progDlg = new ProgressDialog(SohuVideoActivity.this);
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
							SohuVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
					SohuVideoActivity.this.mListView.setAdapter(mAdapter);
				}
				else {
					mAdapter.clear();
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
					SohuVideoActivity.this.mListView.setSelection(0);
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
				
				if (!mEPG.channel_list()) {
					Log.e(TAG, "Java: failed to call channel_list():");
					mhandler.sendEmptyMessage(MSG_FAIL_TO_CHANNEL_LIST);
					return null;
				}
				
				mChannelList = mEPG.getChannelList();
				
				Items = new ArrayList<String>();
	
				for (int i=0;i<mChannelList.size();i++) {
					Items.add(mChannelList.get(i).mTitle);
				}
			}
			else if (action == EPG_TASK_SELECT_CHANNEL) {
				if (!mEPG.channel_select(mChannelId)) {
					Log.e(TAG, "Java: failed to call channel_select() channel_id: " + mChannelId);
					mhandler.sendEmptyMessage(MSG_FAIL_TO_CHANNEL_SELECT);
					return null;
				}
				
				mSubChannelList = mEPG.getSubChannelList();
	
				Items = new ArrayList<String>();
				
				for (int i=0;i<mSubChannelList.size();i++) {
					Items.add(mSubChannelList.get(i).mTitle);
				}
				
				mSubChannelSelected = true;
			}
			else {
				if (action == EPG_TASK_GET_PLAYLINK) {
					if (params.length < 2)
						return null;
					
					int vid = params[1];
					mPlaylink = mEPG.playlink_pptv(vid, 0);
					if (mPlaylink == null) {
						Log.e(TAG, "Java: failed to call playlink_pptv() vid: " + vid);
						mhandler.sendEmptyMessage(MSG_FAIL_TO_GET_PLAYLINK);
						return null;
					}
					
					mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);
				}
				else if (action == EPG_TASK_GET_VIDEOINFO) {
					if (params.length < 3)
						return null;
					
					int vid = params[1];
					int site = params[2];
					
					mPlaylink = mEPG.video_info(site, vid, mAid);
					if (mPlaylink == null) {
						Log.e(TAG, "Java: failed to call video_info() vid: " + vid);
						mhandler.sendEmptyMessage(MSG_FAIL_TO_GET_PLAYLINK);
						return null;
					}
					
					mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);
				}
			}
			
			return Items;
		}
		
	}

}
