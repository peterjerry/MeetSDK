package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.common.pptv.Catalog;
import com.pplive.common.pptv.Content;
import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.Episode;
import com.pplive.common.pptv.Module;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.VirtualChannelInfo;
import com.pplive.common.sohu.ChannelSohu;
import com.pplive.common.sohu.PlaylinkSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.SubChannelSohu;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.Util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


public class PPTVVideoActivity extends ListActivity {
	private final static String TAG = "PPTVVideoActivity";
	
	private final int EPG_ITEM_FRONTPAGE		= 1;
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
	
	private static final int MSG_EPG_FRONTPAGE_DONE				= 501;
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
	
	private ArrayAdapter<String> mAdapter;
	private boolean mContentSelected = false;
	private PlaylinkSohu mPlaylink;
	private long mAid;
	
	private String mEPGsearchKey;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		mEPG = new EPGUtil();
		new EPGTask().execute(EPG_ITEM_CONTENT_LIST);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode: " + keyCode);
		
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
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
        			Toast.makeText(PPTVVideoActivity.this, "no stream available", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
        		Intent intent = new Intent(PPTVVideoActivity.this, PlaySohuActivity.class);
        		intent.putExtra("url_list", strUrl);
        		intent.putExtra("duration_list", mPlaylink.getDuration(ft));
        		intent.putExtra("title", mPlaylink.getTitle());
        		startActivity(intent);
            	break;
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
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		//super.onListItemClick(l, v, position, id);
		
		if (mContentSelected) {
			String param = mEPGContentList.get(position).getParam();
			if (param.startsWith("type="))
				mContentType = "";
			Intent intent = new Intent(PPTVVideoActivity.this, PPTVEpisodeActivity.class);
    		intent.putExtra("epg_param", param);
    		intent.putExtra("epg_type", mContentType);
    		String info = String.format("Java: ready to start PPTVEpisodeActivity param: %s, type: %s",
    				param, mContentType);
    		Log.i(TAG, info);
    		//Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    		
    		startActivity(intent);
			return;
		}
		else {
			mLink = mEPGModuleList.get(position).getLink();
			mContentType = "";
			int pos = mLink.indexOf("type=");
			if (pos != -1) {
				mContentType = mLink.substring(pos, mLink.length());
			}
			new EPGTask().execute(EPG_ITEM_CONTENT_SURFIX);
		}
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
            	Util.writeSettings(PPTVVideoActivity.this, "last_searchkey", mEPGsearchKey);

            	Intent intent = new Intent(PPTVVideoActivity.this, SohuEpisodeActivity.class);
        		intent.putExtra("search_key", mEPGsearchKey);
        		startActivity(intent);
        		
        		dialog.dismiss();
             }
        });
        builder.show();
	}
	
	private void popupHistory() {
		final String key = "PPTVPlayHistory";
		final String regularEx = ",";
		String value = Util.readSettings(PPTVVideoActivity.this, key);
		
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
        	Toast.makeText(PPTVVideoActivity.this, "no sohu playlink history", Toast.LENGTH_SHORT).show();
        	return;
        }
        
		final String[] str_title_list = (String[])TitleList.toArray(new String[size]);
		
		Dialog choose_history_dlg = new AlertDialog.Builder(PPTVVideoActivity.this)
		.setTitle("Select video")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
        		

				dialog.dismiss();
			}
		})
		.setNeutralButton("Clear",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
					Util.writeSettings(PPTVVideoActivity.this, key, "");
					Toast.makeText(PPTVVideoActivity.this, "sohu playlink history was clear", 
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
        		Log.w(TAG, "Java: EPGTask invalid action: " + action);
        	}
        	
        	return items;
        }
        
    	@Override
        protected void onPostExecute(List<String> result) {
    		if (result != null && result.size() > 0) {
				if (mAdapter == null) {
					mAdapter = new ArrayAdapter<String>(
							PPTVVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
					PPTVVideoActivity.this.setListAdapter(mAdapter);
				}
				else {
					mAdapter.clear();
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
				}
			}
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {		
        }
	}
	
	private class EPGTask2 extends AsyncTask<Integer, Integer, List<String>> {

		@Override
        protected void onPostExecute(List<String> result) {
			if (result != null && result.size() > 0) {
				if (mAdapter == null) {
					mAdapter = new ArrayAdapter<String>(
							PPTVVideoActivity.this, android.R.layout.simple_expandable_list_item_1, result);
					PPTVVideoActivity.this.setListAdapter(mAdapter);
				}
				else {
					mAdapter.clear();
					mAdapter.addAll(result);
					mAdapter.notifyDataSetChanged();
				}
			}
        }
		
		@Override
		protected List<String> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
