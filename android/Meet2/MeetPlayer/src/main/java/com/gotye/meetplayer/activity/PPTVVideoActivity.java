package com.gotye.meetplayer.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.gotye.common.pptv.Catalog;
import com.gotye.common.pptv.Content;
import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.Episode;
import com.gotye.common.pptv.Module;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.sohu.PlaylinkSohu;
import com.gotye.common.util.LogUtil;
import com.gotye.db.PPTVPlayhistoryDatabaseHelper;
import com.gotye.db.PPTVPlayhistoryDatabaseHelper.ClipInfo;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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


public class PPTVVideoActivity extends AppCompatActivity {
	private final static String TAG = "PPTVVideoActivity";
	
	private final int EPG_ITEM_CATALOG			= 2;
	private final int EPG_ITEM_DETAIL			= 3;
	private final int EPG_ITEM_SEARCH			= 4;
	private final int EPG_ITEM_CONTENT_LIST	= 5;
	private final int EPG_ITEM_CONTENT_SURFIX	= 6;
	private final int EPG_ITEM_LIST			= 7;
	private final int EPG_ITEM_VIRTUAL_SOHU	= 8;
	private final int EPG_ITEM_VIRTUAL_LIST	= 9;
	private final int EPG_ITEM_CDN				= 11;
	private final int EPG_ITEM_FT				= 12;
	
	private final static int MSG_PLAYLINK_DONE					= 1001;
	private final static int MSG_FAIL_TO_GET_PLAYLINK				= 2001;
	private final static int MSG_FAIL_TO_CONTENTS_LIST			= 2002;
	private final static int MSG_FAIL_TO_CHANNEL_SELECT			= 2003; 
	
	private static final int MSG_EPG_CATALOG_DONE					= 502;
	private static final int MSG_EPG_DETAIL_DONE					= 503;
	private static final int MSG_EPG_SEARCH_DONE					= 504;
	private static final int MSG_EPG_CONTENT_LIST_DONE			= 505;
	private static final int MSG_EPG_CONTENT_SURFIX_DONE			= 506;
	private static final int MSG_EPG_LIST_DONE					= 507;
	private static final int MSG_FAIL_TO_CONNECT_EPG_SERVER		= 511;
	private static final int MSG_FAIL_TO_PARSE_EPG_RESULT			= 512;
	private static final int MSG_WRONG_PARAM						= 513;
	private static final int MSG_PUSH_CDN_CLIP					= 601;
	private static final int MSG_PLAY_CDN_URL						= 602;
	private static final int MSG_PLAY_CDN_FT						= 603;
	
	private EPGUtil mEPG;
	private List<Content> mEPGContentList 	= null;
	private List<Module> mEPGModuleList 	= null;
	private List<Catalog> mEPGCatalogList	= null;
	private List<PlayLink2> mEPGLinkList 	= null;
	private List<Episode> mVirtualLinkList	= null;
	private String mLink;
	private String mContentType;
	private String mEPGparam;
	private String mEPGtype;
	private boolean mListLive;
    private boolean mbVip = false;
	
	private ArrayAdapter<String> mAdapter;
	private boolean mContentSelected = false;
	private PlaylinkSohu mPlaylink;
	private long mAid;
	
	private String mEPGsearchKey;
	
	private PPTVPlayhistoryDatabaseHelper mHistoryDB;

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
                if (mContentSelected) {
                    String param = mEPGContentList.get(position).getParam();
                    if (param.startsWith("type="))
                        mContentType = "";
                    Intent intent = null;
                    if (mListLive) {
                        if (1 == position || 5 == position) // 体育直播 游戏直播
                            intent = new Intent(PPTVVideoActivity.this, PPTVLiveCenterActivity.class);
                        else
                            intent = new Intent(PPTVVideoActivity.this, PPTVLiveActivity.class);

                        int live_type;
                        if (1 == position) // 体育直播
                            live_type = 44;
                        else if (2 == position) // 卫视
                            live_type = 164;
                        else if (3 == position) // 地方台
                            live_type = 156;
                        else if (4 == position) // 电台
                            live_type = 210712;
                        else if (5 == position) // 游戏
                            live_type = 44;
                        else {
                            Toast.makeText(PPTVVideoActivity.this, "invalid live type: " + position, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (1 == position) // 体育直播
                            intent.putExtra("livecenter_id", "44");
                        else if (5 == position)
                            intent.putExtra("livecenter_id", "game");
                        else
                            intent.putExtra("live_type", live_type);
                    }
                    else {
                        intent = new Intent(PPTVVideoActivity.this, PPTVAlbumActivity.class);

                        intent.putExtra("epg_param", param);
                        intent.putExtra("epg_type", mContentType);
						intent.putExtra("epg_is_vip", mbVip);
                    }

                    startActivity(intent);
                    return;
                }
                else {
                    String title = mEPGModuleList.get(position).getTitle();
                    if (title.equals("直播"))
                        mListLive = true;
                    else
                        mListLive = false;

                    if (title.equals("VIP尊享"))
                        mbVip = true;
                    else
                        mbVip = false;

                    mLink = mEPGModuleList.get(position).getLink();
                    mContentType = "";
                    int pos = mLink.indexOf("type=");
                    if (pos != -1) {
                        mContentType = mLink.substring(pos, mLink.length());
                    }
                    new EPGTask().execute(EPG_ITEM_CONTENT_SURFIX);
                }
            }
        });

		Util.checkNetworkType(this);

		mHistoryDB = PPTVPlayhistoryDatabaseHelper.getInstance(this);
		
		mEPG = new EPGUtil();
		new EPGTask().execute(EPG_ITEM_CONTENT_LIST);
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
		LogUtil.info(TAG, "Java: onOptionsItemSelected " + id);

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
            case MSG_FAIL_TO_GET_PLAYLINK:
            	Toast.makeText(PPTVVideoActivity.this, "failed to get playlink",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_CONTENTS_LIST:
            	Toast.makeText(PPTVVideoActivity.this, "failed to do contents list",
            			Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_CHANNEL_SELECT:
            	Toast.makeText(PPTVVideoActivity.this, "failed to do channel select",
            			Toast.LENGTH_SHORT).show();
            	break;
            default:
            	Toast.makeText(PPTVVideoActivity.this, "invalid msg: " + msg.what,
            			Toast.LENGTH_SHORT).show();
            	break;
            }
        }
	};
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mContentSelected) {
			new EPGTask().execute(EPG_ITEM_CONTENT_LIST);
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
                    Util.writeSettings(PPTVVideoActivity.this, "search_keys", new_search_keys);
                }

                Intent intent = new Intent(PPTVVideoActivity.this, PPTVAlbumActivity.class);
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
						Util.writeSettings(PPTVVideoActivity.this, "search_keys", "");
						Toast.makeText(PPTVVideoActivity.this, "搜索记录已清空",
								Toast.LENGTH_SHORT).show();
					}
				})
                .setItems(keywords,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(
                                        PPTVVideoActivity.this, PPTVAlbumActivity.class);
                                intent.putExtra("search_key", keywords[whichButton]);
                                startActivity(intent);

                                dialog.dismiss();
                            }
                        })
                .create();
        choose_search_dlg.show();
    }
	
	private void popupRecentPlay() {
		final List<ClipInfo> infoList = mHistoryDB.getRecentPlay();
		
		if (infoList == null || infoList.size() == 0) {
			Toast.makeText(this, "pptv history is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> titleList = new ArrayList<String>();
		for (int i=0;i<infoList.size();i++) {
			titleList.add(infoList.get(i).mTitle);
		}
		
		final String[] str_title_list = titleList.toArray(new String[infoList.size()]);
		
		Dialog choose_recentplay_dlg = new AlertDialog.Builder(PPTVVideoActivity.this)
		.setTitle("Recent play")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				ClipInfo info = infoList.get(whichButton);
				int playlink = Integer.valueOf(info.mPlaylink);
				int ft = info.mFt;
				int bw_type = 3;
				
				short port = MediaSDK.getPort("http");
				String url = PlayLinkUtil.getPlayUrl(playlink, port, ft, bw_type, null);
				
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(PPTVVideoActivity.this,
						PPTVPlayerActivity.class);
                LogUtil.info(TAG, "to play uri: " + uri.toString());
				
				Toast.makeText(PPTVVideoActivity.this, 
						String.format("ready to play %s, 码流 %d",str_title_list[whichButton], ft), 
								Toast.LENGTH_SHORT).show();

				intent.setData(uri);
				intent.putExtra("title", info.mTitle);
				intent.putExtra("playlink", Integer.valueOf(info.mPlaylink));
				intent.putExtra("album_id", Integer.valueOf(info.mAlbumId));
				intent.putExtra("ft", ft);
				if (info.mEpisodeIndex >= 0)
					intent.putExtra("index", info.mEpisodeIndex);
				
				if (info.mLastPos > 0) {
					intent.putExtra("preseek_msec", info.mLastPos);
                    LogUtil.info(TAG, "Java: set preseek_msec " + info.mLastPos);
				}
		        
				startActivity(intent);
				
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
	
	private void popupHistory() {
		final List<ClipInfo> infoList = mHistoryDB.getPlayedClips();
		
		if (infoList == null || infoList.size() == 0) {
			Toast.makeText(this, "pptv history is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> titleList = new ArrayList<String>();
		for (int i=0;i<infoList.size();i++) {
			titleList.add(infoList.get(i).mTitle);
		}
		
		final String[] str_title_list = titleList.toArray(new String[infoList.size()]);
		
		Dialog choose_history_dlg = new AlertDialog.Builder(PPTVVideoActivity.this)
		.setTitle("Play history")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				ClipInfo info = infoList.get(whichButton);
				int playlink = Integer.valueOf(info.mPlaylink);
				int ft = info.mFt;
				int bw_type = 3;
				
				short port = MediaSDK.getPort("http");
				String url = PlayLinkUtil.getPlayUrl(playlink, port, ft, bw_type, null);
				
				Uri uri = Uri.parse(url);
                LogUtil.info(TAG, "to play uri: " + uri.toString());
				
				Toast.makeText(PPTVVideoActivity.this, 
						String.format("ready to play %s, 码流 %d",str_title_list[whichButton], ft), 
								Toast.LENGTH_SHORT).show();
				
				Intent intent = new Intent(PPTVVideoActivity.this, PPTVPlayerActivity.class);
				intent.setData(uri);
				intent.putExtra("title", info.mTitle);
				intent.putExtra("playlink", Integer.valueOf(info.mPlaylink));
				intent.putExtra("album_id", Integer.valueOf(info.mAlbumId));
				intent.putExtra("episode_idx", info.mEpisodeIndex);
				intent.putExtra("ft", ft);
				
				if (info.mLastPos > 0) {
					intent.putExtra("preseek_msec", info.mLastPos);
                    LogUtil.info(TAG, "Java: set preseek_msec " + info.mLastPos);
				}
		        
				startActivity(intent);
				
				dialog.dismiss();
			}
		})
		.setNeutralButton("Clear",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					mHistoryDB.clearHistory();
					Toast.makeText(PPTVVideoActivity.this, "pptv playlink history was clear", 
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
        protected List<String> doInBackground(Integer... params) {
        	int action = params[0];
        	int id = -1;
        	if (params.length > 1)
        		id = params[1];
        	
        	List<String> items = new ArrayList<String>();
        	
        	if (EPG_ITEM_CATALOG == action) {
        		if (!mEPG.catalog(id)) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return null;
        		}
        		
    			/*mEPGCatalogList = mEPG.getCatalog();
    			if (mEPGCatalogList.size() == 0)
    				return false;*/
				
    			mhandler.sendEmptyMessage(MSG_EPG_CATALOG_DONE);
        	}
        	else if (EPG_ITEM_SEARCH == action) {
        		if (params.length < 2) {
        			mhandler.sendEmptyMessage(MSG_WRONG_PARAM);
        			return null;
				}
        		
        		int start_page = params[1];
        		int count = params[2];
        		
        		if (!mEPG.search(mEPGsearchKey, 0, 0/* 0-只正片，1-非正片，-1=不过滤*/, start_page, count)) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
        			return null;
    			}
        		
    			/*mEPGLinkList = mEPG.getLink();
    			if (mEPGLinkList.size() == 0)
    				return false;*/

    			mhandler.sendEmptyMessage(MSG_EPG_SEARCH_DONE);
        	}
        	else if (EPG_ITEM_CONTENT_LIST == action) {
        		if (!mEPG.contents_list()) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_CONNECT_EPG_SERVER);
        			return null;
    			}
        		
    			mEPGModuleList = mEPG.getModule();
    			if (mEPGModuleList.size() == 0)
    				return null;
    			
    			for (int i=0;i<mEPGModuleList.size();i++)
    				items.add(mEPGModuleList.get(i).getTitle());
    			
    			mContentSelected = false;
        	}
        	else if (EPG_ITEM_CONTENT_SURFIX == action) {
        		if (mLink == null || mLink.isEmpty() || !mEPG.contents(mLink)) {
        			mhandler.sendEmptyMessage(MSG_FAIL_TO_PARSE_EPG_RESULT);
        			return null;
        		}
        		
    			mEPGContentList = mEPG.getContent();
    			if (mEPGContentList.size() == 0)
    				return null;
    			
    			for (int i=0;i<mEPGContentList.size();i++)
    				items.add(mEPGContentList.get(i).getTitle());
    			
    			mContentSelected = true;
        	}
        	else {
                LogUtil.warn(TAG, "Java: EPGTask invalid action: " + action);
        	}
        	
        	return items;
        }
        
    	@Override
        protected void onPostExecute(List<String> result) {
    		progDlg.dismiss();
    		
    		if (result != null && result.size() > 0) {
				if (mAdapter == null) {
					mAdapter = new ArrayAdapter<String>(
							PPTVVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
					PPTVVideoActivity.this.mListView.setAdapter(mAdapter);
				}
				else {
					mAdapter.clear();
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
					PPTVVideoActivity.this.mListView.setSelection(0);
				}
			}
        }

        @Override
        protected void onPreExecute() {
        	progDlg = new ProgressDialog(PPTVVideoActivity.this);
			progDlg.setMessage("数据请求中...");
			progDlg.setCancelable(true);
			progDlg.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {		
        }
	}
}
