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

import com.gotye.common.util.LogUtil;
import com.gotye.common.youku.Catalog;
import com.gotye.common.youku.Channel;
import com.gotye.common.youku.YKUtil;
import com.gotye.db.YKPlayhistoryDatabaseHelper;
import com.gotye.db.YKPlayhistoryDatabaseHelper.ClipInfo;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private int mPlayerImpl;
    private String mTitle;
    private String mShowId;
    private int mEpisodeIndex = -1;
    private int mLastPos;

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
                    Catalog cat = mCatalogList.get(position);
					Intent intent = new Intent(YoukuVideoActivity.this, YoukuAlbumActivity.class);
                    intent.putExtra("title", cat.getTitle());
                    intent.putExtra("channel_id", mChannelId);
                    if (cat.getTitle().equals("全部"))
                        intent.putExtra("get_filter", 1);
					intent.putExtra("filter", cat.getFilter());
					intent.putExtra("sub_channel_id", cat.getSubChannelId());
                    intent.putExtra("sub_channel_type", cat.getSubChannelType());
					startActivity(intent);
				}
                else {
                    mChannelId = mChannelList.get(position).getChannelId();
                    new EPGTask().execute(EPG_TASK_SELECT_CATALOG);
                }
			}
		});

        mPlayerImpl = Util.readSettingsInt(this, "PlayerImpl");

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
		switch (id) {
			case R.id.search:
				popupSearch();
				break;
			case R.id.search_history:
				popupSearchHistory();
				break;
            case R.id.clean_search_history:
                Util.writeSettings(YoukuVideoActivity.this, "search_keys", "");
                Toast.makeText(this, "搜索记录已清空", Toast.LENGTH_SHORT).show();
                break;
			case R.id.play_history:
				popupHistory();
				break;
			case R.id.recent_play:
                popupRecentPlay();
				break;
			default:
				LogUtil.warn(TAG, "unknown menu id " + id);
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
                    Util.writeSettings(YoukuVideoActivity.this, "search_keys", new_search_keys);
                }

            	Intent intent = new Intent(YoukuVideoActivity.this, YoukuAlbumActivity.class);
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
                .setItems(keywords,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(YoukuVideoActivity.this, YoukuAlbumActivity.class);
                                intent.putExtra("search_key", keywords[whichButton]);
                                startActivity(intent);

                                dialog.dismiss();
                            }
                        })
                .create();
        choose_search_dlg.show();
    }
	
	private void popupHistory() {
        final YKPlayhistoryDatabaseHelper historyDB = YKPlayhistoryDatabaseHelper.getInstance(this);

        final List<ClipInfo> infoList = historyDB.getPlayedClips();

        if (infoList == null || infoList.isEmpty()) {
            Toast.makeText(this, "播放记录为空", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> titleList = new ArrayList<String>();
        for (int i=0;i<infoList.size();i++) {
            titleList.add(infoList.get(i).mTitle);
        }

        final String[] str_title_list = titleList.toArray(new String[infoList.size()]);

        Dialog choose_history_dlg = new AlertDialog.Builder(this)
                .setTitle("播放记录")
                .setItems(str_title_list,
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ClipInfo info = infoList.get(whichButton);
                                mTitle = info.mTitle;
                                mShowId = info.mShowId;
                                mEpisodeIndex = info.mEpisodeIndex;
                                new PlayLinkTask().execute(info.mVideoId);

                                dialog.dismiss();
                            }
                        })
                .setNeutralButton("Clear",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton){
                                historyDB.clearHistory();
                                Toast.makeText(YoukuVideoActivity.this,
                                        "播放记录已清空",
                                        Toast.LENGTH_LONG).show();
                            }})
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton){
                            }})
                .create();

        choose_history_dlg.show();
	}

    private void popupRecentPlay() {
        final YKPlayhistoryDatabaseHelper historyDB = YKPlayhistoryDatabaseHelper.getInstance(this);

        final List<ClipInfo> infoList = historyDB.getRecentPlay();

        if (infoList == null || infoList.isEmpty()) {
            Toast.makeText(this, "没有最近播放记录", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> titleList = new ArrayList<String>();
        for (int i=0;i<infoList.size();i++) {
            titleList.add(infoList.get(i).mTitle);
        }

        final String[] str_title_list = titleList.toArray(new String[infoList.size()]);

        Dialog choose_recentplay_dlg = new AlertDialog.Builder(this)
                .setTitle("最近播放")
                .setItems(str_title_list,
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ClipInfo info = infoList.get(whichButton);
                                mTitle          = info.mTitle;
                                mShowId         = info.mShowId;
                                mEpisodeIndex   = info.mEpisodeIndex;
                                mLastPos        = info.mLastPos;

                                new PlayLinkTask().execute(info.mVideoId);

                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int whichButton){
                            }})
                .create();

        choose_recentplay_dlg.show();
    }

    private class PlayLinkTask extends AsyncTask<String, Integer, YKUtil.ZGUrl> {
        @Override
        protected void onPostExecute(YKUtil.ZGUrl zgUrl) {
            // TODO Auto-generated method stub
            if (zgUrl == null) {
                Toast.makeText(YoukuVideoActivity.this, "获取视频播放地址失败",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(YoukuVideoActivity.this, PlayYoukuActivity.class);
            intent.putExtra("url_list", zgUrl.urls);
            intent.putExtra("duration_list", zgUrl.durations);
            intent.putExtra("title", mTitle);
            intent.putExtra("ft", 2);
            intent.putExtra("show_id", mShowId);
            intent.putExtra("vid", zgUrl.vid);
            intent.putExtra("episode_index", mEpisodeIndex);
            intent.putExtra("player_impl", mPlayerImpl);

            if (mLastPos > 0) {
                intent.putExtra("preseek_msec", mLastPos);
                LogUtil.info(TAG, "Java: set preseek_msec " + mLastPos);
            }

            startActivity(intent);
        }

        @Override
        protected YKUtil.ZGUrl doInBackground(String... params) {
            // TODO Auto-generated method stub
            String vid = params[0];
            return YKUtil.getPlayUrl2(vid);
        }
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
				if (mChannelList == null || mChannelList.isEmpty()) {
					LogUtil.error(TAG, "Java: failed to call getChannel():");
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
				if (mCatalogList == null || mCatalogList.isEmpty()) {
					LogUtil.error(TAG, "Java: failed to call getCatalog() channel_id: " + mChannelId);
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
