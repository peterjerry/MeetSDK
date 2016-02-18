package com.gotye.meetplayer.ui;

import java.util.List;

import com.gotye.common.pptv.EPGUtil;
import com.gotye.common.pptv.PlayLink2;
import com.gotye.common.pptv.PlayLinkUtil;
import com.gotye.common.util.PlayBackTime;
import com.gotye.meetplayer.R;
import com.gotye.meetplayer.service.MyHttpService;
import com.gotye.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class PPTVLiveActivity extends AppCompatActivity {
	private final static String TAG = "PPTVLiveActivity";
	
	private Button btnLive;
	private Button btnPlayback;
	private Button btnBwType;
	private ListView lv_tvlist;
	
	private EPGUtil mEPG;
	private List<PlayLink2>mEPGLinkList;
	private PlayBackTime mPlaybackTime;
	private int mBwType = 0;
	
	private MyPPTVLiveAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_pptv_live);
		
		this.btnLive = (Button)this.findViewById(R.id.btn_live);
		this.btnPlayback = (Button)this.findViewById(R.id.btn_playback);
		this.btnBwType = (Button)this.findViewById(R.id.btn_bw_type);
		this.lv_tvlist = (ListView)this.findViewById(R.id.lv_tvlist);
		
		this.btnLive.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mPlaybackTime.setLive();
				Toast.makeText(PPTVLiveActivity.this, "切换为 直播 模式", Toast.LENGTH_SHORT).show();
			}
		});
		
		this.btnPlayback.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mPlaybackTime.setPlaybackTime();
			}
		});
		
		this.btnBwType.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final String[] bw_type = {"P2P", "MYHTTP", "M3U8"};
				int sel = Util.readSettingsInt(PPTVLiveActivity.this, "live_bwtype");

				Dialog choose_bw_type_dlg = new AlertDialog.Builder(PPTVLiveActivity.this)
				.setTitle("select bw_type")
				.setSingleChoiceItems(bw_type, sel, /*default selection item number*/
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int whichButton){
							mBwType = whichButton;
							btnBwType.setText(bw_type[mBwType]);
							Util.writeSettingsInt(PPTVLiveActivity.this, "live_bwtype", mBwType);
							Toast.makeText(PPTVLiveActivity.this, 
									"switch bw_type to " + bw_type[mBwType], 
									Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					})
				.create();
				choose_bw_type_dlg.show();	
			}
		});
		
		this.lv_tvlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				PlayLink2 link = mAdapter.getItem(position);
				String playlink = link.getId();
				String play_url = null;
				
				if (mBwType == 0 || mBwType == 1) {
					short http_port = MediaSDK.getPort("http");
					if (mBwType == 1)
						http_port = (short)MyHttpService.getPort();
					
					play_url = PlayLinkUtil.getPlayUrl(
							Integer.valueOf(playlink), http_port, 1, 3, 
							mPlaybackTime.getPPTVTimeStr());
				}
				else {
					Toast.makeText(PPTVLiveActivity.this, "invalid bw_type " + mBwType, Toast.LENGTH_SHORT).show();
					return;
				}
					
				
				Intent intent = new Intent(PPTVLiveActivity.this,
		        		PPTVPlayerActivity.class);
				Uri uri = Uri.parse(play_url);
				Log.i(TAG, "to play uri: " + uri.toString());

				intent.setData(uri);
				intent.putExtra("title", link.getTitle());
				//intent.putExtra("ft", 1);
				//intent.putExtra("best_ft", 3);
		        
				startActivity(intent);
			}
		});
		
		mBwType = Util.readSettingsInt(this, "live_bwtype");
		mPlaybackTime = new PlayBackTime(this);
		
		Intent intent = getIntent();
		int live_type = intent.getIntExtra("live_type", 0);
		
		mEPG = new EPGUtil();
		if (live_type == 0)
			Toast.makeText(this, "live_type 未获取", Toast.LENGTH_SHORT).show();
		else
			new EPGTask().execute(live_type);
	}
	
	private class EPGTask extends AsyncTask<Integer, Integer, List<PlayLink2>> {
		
		@Override
		protected void onPostExecute(List<PlayLink2> result) {
			// TODO Auto-generated method stub
			if (result == null) {
				Toast.makeText(getApplicationContext(),"获取列表失败", Toast.LENGTH_SHORT).show();
			}
			else {
				mAdapter = new MyPPTVLiveAdapter(PPTVLiveActivity.this, result);
				lv_tvlist.setAdapter(mAdapter);
			}
		}
		
		@Override
		protected List<PlayLink2> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			if (!mEPG.live(1, 200, params[0]))
				return null;
			
			else
				return mEPG.getLink();
		}
	}
}
