package com.gotye.meetplayer.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.util.LogUtil;
import com.gotye.db.PPTVPlayhistoryDatabaseHelper;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.adapter.CommonAlbumAdapter;
import com.gotye.meetplayer.ui.widget.DirChooserDialog;
import com.gotye.meetplayer.ui.widget.DirChooserDialog.onOKListener;
import com.gotye.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.Toast;


public class PPTVAlbumActivity extends AppCompatActivity {
	private final static String TAG = "PPTVAlbumActivity";
	
	private Button btnReputation;
	private Button btnPopularity;
	private Button btnUpdate;
	private CheckBox cbIsCatalog;
	private Button btnFt;
	private GridView gridView = null;
    private CommonAlbumAdapter adapter = null;
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_MOREDATA_DONE	= 2;
    private final static int MSG_PLAY_CDN_FT		= 3;
    private final static int MSG_NO_MORE_EPISODE	= 11;
    private final static int MSG_FAIL_TO_DETAIL	= 12;
    private final static int MSG_FAIL_TO_GET_FT	= 13;
    
    private final static int TASK_DETAIL			= 1;
    private final static int TASK_MORELIST		= 2;
    private final static int TASK_MORESEARCH		= 3;
    private final static int TASK_ITEM_FT			= 4;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH		= 2;
    
    private final static int page_size = 10;
    private int album_page_index = 1;
    
    private EPGUtil mEPG;
    private String epg_param;
    private String epg_type;
    private String episode_title;
    private int episode_index;
    private int selected_album_id = -1;
    private String album_id;
    private String album_title;
    private String album_img_url;
    // 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
    private String epg_order = "order=t";
    private String search_key;
    private String epg_signature;
    private String epg_cdn_url;
    private boolean epg_is_vip = false;
    private List<PlayLink2> mAlbumList;
    private List<PlayLink2> mEpisodeList;
    private boolean is_catalog = false;

    private boolean noMoreData = false;
    private boolean loadingMore = false;

    private boolean mbPopSelEp = true;
    private String mDownloadLocalFolder;
    
    private PPTVPlayhistoryDatabaseHelper mHistoryDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Intent intent = getIntent();
		if (intent.hasExtra("epg_param")) {
			epg_param = intent.getStringExtra("epg_param");
			epg_type = intent.getStringExtra("epg_type");
            epg_is_vip = intent.getBooleanExtra("epg_is_vip", false);
            LogUtil.info(TAG, "Java: vip is " + (epg_is_vip ? "ON" : "OFF"));
		}
		else if (intent.hasExtra("search_key")) {
            search_key = intent.getStringExtra("search_key");
        }
		else {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_pptv_album);
		
		this.btnReputation = (Button) findViewById(R.id.btn_reputation);
		this.btnPopularity = (Button) findViewById(R.id.btn_popularity);
		this.btnUpdate = (Button) findViewById(R.id.btn_update);
		this.cbIsCatalog = (CheckBox) findViewById(R.id.cb_is_catalog);
		this.btnFt = (Button) findViewById(R.id.btn_ft);
		
		this.btnReputation.setOnClickListener(mClickListener);
		this.btnPopularity.setOnClickListener(mClickListener);
		this.btnUpdate.setOnClickListener(mClickListener);
		this.cbIsCatalog.setOnClickListener(mClickListener);
		
		this.btnFt.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				final String[] ft = {"流畅", "高清", "超清", "蓝光"};
				
				Dialog choose_ft_dlg = new AlertDialog.Builder(PPTVAlbumActivity.this)
				.setTitle("select download ft")
				.setSingleChoiceItems(ft, Integer.parseInt(btnFt.getText().toString()), /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							btnFt.setText(Integer.toString(whichButton));
							dialog.dismiss();
						}
					})
				.create();
				choose_ft_dlg.show();	
			}
		});
		
		this.gridView = (GridView) findViewById(R.id.grid_view);
		this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				album_id = (String)item.get("vid");
                album_title = (String)item.get("title");
                album_img_url = (String)item.get("img_url");
				
				if (mbPopSelEp) {
					new PPTVEpgTask().execute(TASK_DETAIL, Integer.valueOf(album_id));
				}
				else {
					Intent intent = new Intent(PPTVAlbumActivity.this, MeetViewActivity.class);
					intent.putExtra("album_id", album_id);
					startActivity(intent);
				}	
			}
			
		});
		
		this.gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v,
					int position, long id) {
				// TODO Auto-generated method stub
				
				Map<String, Object> item = adapter.getItem(position);
				popupMoreDialog(item);
				return true;
			}
		});
		
		this.gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				switch (scrollState) {
					case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
					case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						break;
					case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
						break;
					default:
						break;
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
		            int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				//LogUtil.debug(TAG, String.format("Java: onScroll first %d, visible %d, total %d",
				//		firstVisibleItem, visibleItemCount, totalItemCount));
				
				int lastInScreen = firstVisibleItem + visibleItemCount;
		        if (totalItemCount > 0 && lastInScreen == totalItemCount && !noMoreData) {
		        	if (!loadingMore) {
		        		loadingMore = true;
		        		if (search_key != null)
		        			new PPTVEpgTask().execute(TASK_MORESEARCH);
		        		else
		        			new PPTVEpgTask().execute(TASK_MORELIST);
		        	}
		        }
			}
		});
		
		this.gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View v,
					int position, long id) {
				// TODO Auto-generated method stub
				Map<String, Object> item = adapter.getItem(position);
				String title = (String)item.get("title");
				setTitle(title);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				setTitle(getResources().getString(R.string.title_activity_pptv_video));
			}
		});
		
	    mEPG = new EPGUtil();
	    
	    if (search_key != null)
	    	new SetDataTask().execute(SET_DATA_SEARCH);
	    else
	    	new SetDataTask().execute(SET_DATA_LIST);
	    
	    if (!Util.startP2PEngine(this)) {
			Toast.makeText(this, "failed to start p2p engine", 
					Toast.LENGTH_SHORT).show();
		}
	    
	    String folder = Util.readSettings(this, "download_folder");
	    if (folder.isEmpty())
		    mDownloadLocalFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + 
					"/test2";
	    else
	    	mDownloadLocalFolder = folder;
	    
	    mHistoryDB = PPTVPlayhistoryDatabaseHelper.getInstance(this);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		Util.writeSettings(this, "download_folder", mDownloadLocalFolder);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {  
        MenuInflater menuInflater = new MenuInflater(getApplication());  
        menuInflater.inflate(R.menu.pptv_ep_menu, menu);  
        return super.onCreateOptionsMenu(menu);  
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		LogUtil.info(TAG, "Java: onOptionsItemSelected " + id);
		
		switch (id) {
		case R.id.pop_sel_ep:
			mbPopSelEp = !mbPopSelEp;
			item.setChecked(mbPopSelEp);
			break;
		case R.id.p2p_download:
			break;
		case R.id.set_download_folder:  
	        DirChooserDialog dlg = new DirChooserDialog(PPTVAlbumActivity.this,
	        		DirChooserDialog.TypeOpen, null, mDownloadLocalFolder);  
	        dlg.setTitle("Choose dst file dir");
	        dlg.setOnOKListener(new onOKListener(){

				@Override
				public void saveFolder(String folder) {
					// TODO Auto-generated method stub
					File f = new File(folder);
					if (!f.exists() && !f.mkdir())
						Toast.makeText(PPTVAlbumActivity.this, "Failed to create new folder " + folder,
								Toast.LENGTH_SHORT).show();
					else {
						mDownloadLocalFolder = folder;
						Toast.makeText(PPTVAlbumActivity.this,
							"Download folder save as: " + mDownloadLocalFolder, Toast.LENGTH_SHORT).show();
					}
				}
	        	
	        });
	       
	        dlg.show();
			break;
		default:
			LogUtil.warn(TAG, "unknown menu id " + id);
			break;
		}
		
		return true;
    }
	
	private void popupMoreDialog(final Map<String, Object> item) {
    	final String[] action = {"detail", "download"};
		Dialog choose_action_dlg = new AlertDialog.Builder(this)
		.setTitle("select action")
		.setItems(action, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					if (whichButton == 0) {
						String title = (String)item.get("title");
						String description = (String)item.get("desc");
						String info = description;
						if (info == null)
							info = title;
						new AlertDialog.Builder(PPTVAlbumActivity.this)
							.setTitle("Album description")
							.setMessage(info)
							.setPositiveButton("OK", null)
							.show();
					}
					else if (whichButton == 1) {
						String vid = (String) item.get("vid");
						String title = (String)item.get("title");
						String save_path = mDownloadLocalFolder + "/" + title + ".mp4";
					}
				}
			})
		.setNegativeButton("Cancel", null)
		.create();
		choose_action_dlg.show();
    }
	
	private View.OnClickListener mClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			album_page_index = 1;
			// 最受好评, param: order=g|最高人气, param: order=t|最新更新, param: order=n
			
			switch (v.getId()) {  
            case R.id.btn_reputation:  
                epg_order = "order=g";
                new SetDataTask().execute(SET_DATA_LIST);
                break;            
            case R.id.btn_popularity:  
            	epg_order = "order=t";
            	new SetDataTask().execute(SET_DATA_LIST);
                break;    
            case R.id.btn_update:
            	epg_order = "order=n";
            	new SetDataTask().execute(SET_DATA_LIST);
                break;
            case R.id.cb_is_catalog:
            	is_catalog = !is_catalog;
            	new SetDataTask().execute(SET_DATA_LIST);
            	break;
            default:
				LogUtil.warn(TAG, "Java unknown view id: " + v.getId());
                break;  
			}
		}
	};
	
	private Handler mhandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_EPISODE_DONE:
            	if (mEpisodeList.size() == 1) {
            		String vid = mEpisodeList.get(0).getId();
            		episode_title = mEpisodeList.get(0).getTitle();
            		int playlink = Integer.valueOf(vid);
        			if (playlink >= 300000 && playlink <= 400000) { // live
            			play_video(1, 1);
            		}
            		else { // vod
            			new PPTVEpgTask().execute(TASK_ITEM_FT, playlink);
            		}

					return;
            	}
				else if (mEpisodeList.size() > 20) {
					Intent intent = new Intent(PPTVAlbumActivity.this, PPTVEpisodeActivity.class);
					intent.putExtra("album_id", album_id);
					intent.putExtra("title", album_title);
                    intent.putExtra("img_url", album_img_url);
					startActivity(intent);

					return;
				}
            	
            	popupSelectEpisodeDlg();
            	break;
            case MSG_MOREDATA_DONE:
            	List<Map<String, Object>> listData = adapter.getData();
            	
    			int c = mAlbumList.size();
    			for (int i=0;i<c;i++) {
    				HashMap<String, Object> episode = new HashMap<String, Object>();
    				PlayLink2 al = mAlbumList.get(i);
    				
    				episode.put("title", al.getTitle());
    				episode.put("img_url", al.getImgUrl());
    				episode.put("onlinetime", al.getOnlineTime());
    				episode.put("tip", "评分 " + al.getMark());
    				episode.put("vid", al.getId());
    				episode.put("desc", al.getDescription());
    				listData.add(episode);
    			}
            	
            	adapter.notifyDataSetChanged();
            	break;
            case MSG_PLAY_CDN_FT:
            	play_video(msg.arg1, msg.arg2);
            	break;
            case MSG_NO_MORE_EPISODE:
            	Toast.makeText(PPTVAlbumActivity.this, "No more episode", Toast.LENGTH_SHORT).show();
            	break;
            case MSG_FAIL_TO_GET_FT:
            	Toast.makeText(PPTVAlbumActivity.this, "Failed to get ft", Toast.LENGTH_SHORT).show();
            	break;
            default:
            	break;
            }
        }
	};
	
	private void play_video(int ft, int best_ft) {
		String vid = mEpisodeList.get(0).getId();
		
		String info = String.format(Locale.US,
                "ready to play video %s, playlink: %s, ft: %d",
				episode_title, vid, ft);
		LogUtil.info(TAG, info);
		Toast.makeText(this, info, Toast.LENGTH_SHORT).show();

        short port = MediaSDK.getPort("http");
        int playlink = Integer.valueOf(vid);

        Uri uri;
        if (epg_is_vip) {
            uri = Uri.parse(epg_cdn_url);
            LogUtil.info(TAG, "use cdn url to play vip video: " + epg_cdn_url);
        }
        else {
            String url = PlayLinkUtil.getPlayUrl(playlink, port, ft, 3, null);

            uri = Uri.parse(url);
        }
		
		/* method 1
		 * Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setComponent(new ComponentName("com.pplive.tvduck", "com.pplive.tvduck.PlayerActivity"));
		intent.setClassName("com.pplive.tvduck", "com.pplive.tvduck.PlayerActivity");
        intent.putExtra(Intent.ACTION_VIEW, uri);
        intent.setData(uri);*/
        
        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.putExtra(Intent.ACTION_VIEW, uri);
        Intent wrapperIntent = Intent.createChooser(intent, "选择播放器");
        startActivity(wrapperIntent);*/
		
        // method 3
		int last_pos = mHistoryDB.getLastPlayedPosition(String.valueOf(playlink));
		
        Intent intent = new Intent(PPTVAlbumActivity.this,
        		PPTVPlayerActivity.class);
		LogUtil.info(TAG, "to play uri: " + uri.toString());

		intent.setData(uri);
		intent.putExtra("title", episode_title);
		intent.putExtra("playlink", playlink);
		intent.putExtra("album_id", selected_album_id);
		intent.putExtra("ft", ft);
		intent.putExtra("best_ft", best_ft);
		intent.putExtra("index", episode_index);
        intent.putExtra("is_vip", epg_is_vip);
        if (epg_signature != null)
            intent.putExtra("sig", epg_signature);
		
		if (last_pos > 0) {
			intent.putExtra("preseek_msec", last_pos);
			LogUtil.info(TAG, "Java: set preseek_msec " + last_pos);
		}
        
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
		
		final String[] str_title_list = title_list.toArray(new String[size]);
		
		Dialog choose_episode_dlg = new AlertDialog.Builder(PPTVAlbumActivity.this)
		.setTitle("Select episode")
		.setItems(str_title_list,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String vid = mEpisodeList.get(whichButton).getId();
                        episode_title = mEpisodeList.get(whichButton).getTitle();
                        episode_index = whichButton;
                        new PPTVEpgTask().execute(TASK_DETAIL, Integer.valueOf(vid));
                        dialog.dismiss();
                    }
                })
		.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
		.create();
		choose_episode_dlg.show();
	}
	
	private class PPTVEpgTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			
			if (!result) {
				LogUtil.error(TAG, "failed to get episode");
				Toast.makeText(PPTVAlbumActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			
			if (action == TASK_DETAIL) {
				int vid = params[1];
				if (!mEPG.detail(String.valueOf(vid))) {
					LogUtil.error(TAG, "Java: failed to call detail()");
					mhandler.sendEmptyMessage(MSG_FAIL_TO_DETAIL);
					return false;
				}
				
				mEpisodeList = mEPG.getLink();
				if (mEpisodeList.size() > 1)
					selected_album_id = vid;
				
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_MORELIST) {
				album_page_index++;
				if (!mEPG.list(epg_param, epg_type, album_page_index, epg_order, page_size, is_catalog)) {
					LogUtil.error(TAG, "Java: failed to call list() more");
					noMoreData = true;
					return false;
				}
				
				mAlbumList = mEPG.getLink();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MOREDATA_DONE);
			}
			else if (action == TASK_MORESEARCH) {
				album_page_index++;
				if (!mEPG.search(search_key, 0, 0, album_page_index, page_size)) {
					LogUtil.error(TAG, "Java: failed to call search() more");
					noMoreData = true;
					return false;
				}
				
				mAlbumList = mEPG.getLink();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MOREDATA_DONE);
			}
			else if (action == TASK_ITEM_FT) {
				LogUtil.info(TAG, "Java: EPGTask start to getCDNUrl");
        		
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
        		
        		Util.add_pptvvideo_history(PPTVAlbumActivity.this, episode_title,
						String.valueOf(vid), album_id, episode_index, ft);
                if (epg_is_vip) {
                    // http://114.80.186.137:80/128bf5c96ab61002f4be2ad309820758.mp4
                    // ?w=1&key=54a3ba32b506b80c6c9f875e25f9ce99
                    // &k=07f8e9fa6a99dd9f1f9a8d11f9fc0825-6eae-1459144870
                    // &type=phone.android.vip&vvid=877a4382-f0e4-49ed-afea-8d59dbd11df1
                    // &sv=4.1.3&platform=android3&ft=2&accessType=wifi
                    epg_cdn_url = mEPG.getCDNUrl(String.valueOf(vid), String.valueOf(ft),
                            false, false);
                    int pos1, pos2;
                    String rid = null;

                    pos2 = epg_cdn_url.indexOf(".mp4");
                    if (pos2 != -1) {
                        pos1 = epg_cdn_url.lastIndexOf("/", pos2);

                        if (pos1 != -1) {
                            rid = epg_cdn_url.substring(pos1 + 1, pos2);
                            LogUtil.info(TAG, "rid=" + rid);
                        }
                    }

                    pos1 = epg_cdn_url.indexOf("&key=");
                    pos2 = epg_cdn_url.indexOf("&sv=");
                    if (pos1 != -1 && pos2 != -1 && rid != null)
                        epg_signature = epg_cdn_url.substring(pos1, pos2) + "&rid=" + rid;
                    else
                        epg_signature = null;
                }
                else {
                    epg_signature = null;
                }

				Message msg = mhandler.obtainMessage(MSG_PLAY_CDN_FT, ft, ft);
    	        msg.sendToTarget();
        	}
			else {
				LogUtil.error(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, List<Map<String, Object>>> {
		
		@Override
		protected void onPostExecute(List<Map<String, Object>> result) {
			// TODO Auto-generated method stub
			
			if (result == null) {
				LogUtil.error(TAG, "Java: failed to get data");
				return;
			}
			
			if (adapter == null) {
				adapter = new CommonAlbumAdapter(PPTVAlbumActivity.this, result);
				gridView.setAdapter(adapter);
			}
			else {
				List<Map<String, Object>> listData = adapter.getData();
				listData.clear();
				listData.addAll(result);
				adapter.notifyDataSetChanged();
			}
		}
		
		@Override
		protected List<Map<String, Object>> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
				if (!mEPG.search(search_key, 0, 0, album_page_index, page_size))
					return null;
				
				mAlbumList = mEPG.getLink();
			}
			else {
				if (!mEPG.list(epg_param, epg_type, album_page_index, epg_order, page_size, is_catalog))
					return null;
				
				mAlbumList = mEPG.getLink();
			}
			
			List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				PlayLink2 al = mAlbumList.get(i);

				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl());
				episode.put("onlinetime", al.getOnlineTime());
				episode.put("tip", "评分 " + al.getMark());
				episode.put("vid", al.getId());
				episode.put("desc", al.getDescription());
				items.add(episode);
			}
			
			return items;
		}
	}  
	
}

