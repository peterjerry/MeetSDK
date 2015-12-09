package com.pplive.meetplayer.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.Episode;
import com.pplive.common.sohu.AlbumSohu;
import com.pplive.common.sohu.EpisodeSohu;
import com.pplive.common.sohu.PlaylinkSohu;
import com.pplive.common.sohu.PlaylinkSohu.SohuFtEnum;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.meetplayer.R;
import com.pplive.meetplayer.media.FragmentMp4MediaPlayerV2;
import com.pplive.meetplayer.ui.widget.MyMediaController;
import com.pplive.meetplayer.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.pplive.media.player.MediaController.MediaPlayerControl;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PlaySohuActivity extends Activity implements Callback {
	private final static String TAG = "PlaySohuActivity";
	
	private RelativeLayout mLayout;
	private SurfaceView mView;
	private SurfaceHolder mHolder;
	private FragmentMp4MediaPlayerV2 mPlayer;
	private MyMediaController mController;
	private MyMediaPlayerControl mMediaPlayerControl;
	private ProgressBar mBufferingProgressBar;
	private TextView mTextViewFileName;
	
	private String mUrlListStr;
	private String mDurationListStr;
	private String mTitle;
	private int mInfoId, mIndex;
	private long mAid;
	private int mSite = -1;
	private int mFt;
	
	private int mVideoWidth, mVideoHeight;
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;
	
	private boolean mIsBuffering = false;
	
	private EPGUtil mEPG;
	private List<Episode> mVirtualLinkList;
	
	private SohuUtil mSohu;
	private List<EpisodeSohu> mEpisodeList;
	private int sohu_page_index = -1;
	private int sohu_episode_cnt = -1;
	final private int sohu_page_size = 10;
	
	private boolean mSwichingEpisode = false;
	
	/* 记录上一次按返回键的时间 */
    private long backKeyTime = 0L;
	
	private final static int LIST_PPTV = 1;
	private final static int LIST_SOHU = 2;
	
	private final static int MEDIA_CONTROLLER_TIMEOUT = 3000;
	
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private MediaPlayer.OnErrorListener	 mOnErrorListener;
	private MediaPlayer.OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdate;
	private MediaPlayer.OnInfoListener mOnInfoListener;
	
	public static final int SCREEN_FIT = 0; // 自适应
    public static final int SCREEN_STRETCH = 1; // 铺满屏幕 
    public static final int SCREEN_FILL = 2; // 放大裁切
    public static final int SCREEN_CENTER = 3; // 原始大小
    
    private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};
	
	private int mDisplayMode = SCREEN_FIT;
	
	private final static int MSG_PLAY_NEXT_EPISODE 		= 1;
	private final static int MSG_FADE_OUT_TV_FILENAME		= 2;
	private final static int MSG_SHOW_MEDIA_CONTROLLER	= 3;
	
	private final static int MSG_INVALID_EPISODE_INDEX	= 101;
	private final static int MSG_FAIL_TO_GET_PLAYLINK		= 102;
	private final static int MSG_FAIL_TO_GET_STREAM		= 103;
	private final static int MSG_FAIL_TO_GET_ALBUM_INFO	= 104;
	
	private final static String url_list = "http://data.vod.itc.cn/?" +
			"new=/49/197/T9vx2eIRoGJa8v2svlzxkN.mp4&vid=1913402&ch=tv" +
			"&cateCode=115;115102;115103;115105&plat=6&mkey=lZzltZEl0zZ8BypeOS30vK03Trnxwtvj&prod=app," +
			"http://data.vod.itc.cn/?new=/208/249/k0qpKxLzSiu8jYs1996riF.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=sVCsCgJXiDnjxX_2LgPqycXUPkxWsOgb&prod=app," +
			"http://data.vod.itc.cn/?new=/95/201/DFpUuRsTRdmhqZqwkNfD5B.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=wK4roB-c4dClMKzMM5io8AkcU1woYZRK&prod=app," +
			"http://data.vod.itc.cn/?new=/94/253/XNegXt7MRBqTC7zfbSV9MB.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=m2lq1NlURrit46JngILRgC0-K7oIDT3I&prod=app," +
			"http://data.vod.itc.cn/?new=/241/134/TmaV7dUCTom7m9F5396l0D.mp4" +
			"&vid=1913402&ch=tv&cateCode=115;115102;115103;115105&plat=6" +
			"&mkey=kK4jgq0w6aS7b7z_Mm7h9GnS4QbxUfnx&prod=app";
	
	private final static String duration_list = "300.12,300.04,300.04,300.04,219.011";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_frag_mp4_player);
		
		mLayout 				= (RelativeLayout) findViewById(R.id.main_layout);
		mView 					= (SurfaceView) findViewById(R.id.player_view);
		mController 			= (MyMediaController) findViewById(R.id.video_controller);
		mBufferingProgressBar 	= (ProgressBar) findViewById(R.id.progressbar_buffering);
		mTextViewFileName 		= (TextView) findViewById(R.id.tv_filename);
		
		mMediaPlayerControl = new MyMediaPlayerControl();
		mController.setMediaPlayer(mMediaPlayerControl);
		
		mTextViewFileName.setTextColor(Color.RED);
		mTextViewFileName.setTextSize(24);
		
		SurfaceHolder holder = mView.getHolder();
		holder.addCallback(this);
		
		m_playlink_list = new ArrayList<String>();
		m_duration_list = new ArrayList<Integer>();
		
		Intent intent = getIntent();
		if (intent.hasExtra("url_list") && intent.hasExtra("duration_list")) {
			mUrlListStr			= intent.getStringExtra("url_list");
			mDurationListStr	= intent.getStringExtra("duration_list");
			mTitle				= intent.getStringExtra("title");
			mInfoId				= intent.getIntExtra("info_id", -1);
    		mIndex				= intent.getIntExtra("index", -1);
    		
    		// for sohu
    		mAid				= intent.getLongExtra("aid", -1); 
    		mSite				= intent.getIntExtra("site", -1);
    		mFt					= intent.getIntExtra("ft", 0);
    		
    		Log.i(TAG, "Java: mDurationListStr " + mDurationListStr);
		}
		else {
			Log.w(TAG, "Java: use test url and duration list");
			
			mUrlListStr 		= url_list;
			mDurationListStr	= duration_list;
			mInfoId				= -1;
    		mIndex				= -1;
		}
		
		mOnInfoListener = new MediaPlayer.OnInfoListener() {
			
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				Log.i(TAG, "Java: onInfo what " + what + " , extra " + extra);
				
				if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
					Log.i(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_START");
					mIsBuffering = true;
					mBufferingProgressBar.setVisibility(View.VISIBLE);
					Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_START");
				}
				else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
					Log.i(TAG, "Java: onInfo MEDIA_INFO_BUFFERING_END");
					mIsBuffering = false;
					mBufferingProgressBar.setVisibility(View.GONE);
					Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_END");
				}
				else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
					Log.i(TAG, "Java: onInfo MEDIA_INFO_VIDEO_RENDERING_START");
				}
				
				return true;
			}
		};
		
		mOnBufferingUpdate = new MediaPlayer.OnBufferingUpdateListener() {
			
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int pct) {
				// TODO Auto-generated method stub
				Log.i(TAG, "Java: onBufferingUpdate " + pct);
			}
		};
		
		mOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
			
			@Override
			public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
				// TODO Auto-generated method stub
				mVideoWidth		= w;
				mVideoHeight	= h;
				
				mHolder.setFixedSize(w, h);
				toggleDisplayMode(mDisplayMode, false);
			}
		};
		
		mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Log.i(TAG, "Java: onPrepared()");
				
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
				mp.start();
				
				if (!mController.isShowing())
					mController.show(MEDIA_CONTROLLER_TIMEOUT);
			}
		};
		
		mOnErrorListener = new MediaPlayer.OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int error, int extra) {
				// TODO Auto-generated method stub
				Log.e(TAG, "Java: onError what " + error + " , extra " + extra);
				
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
				Toast.makeText(PlaySohuActivity.this, "Error " + error + " , extra " + extra,
						Toast.LENGTH_SHORT).show();
				finish();
				
				return true;
			}
		};
		
		mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				if (mInfoId != -1) {
					new NextEpisodeTask().execute(LIST_PPTV, 1);
	        		return;
				}
				else if (mAid != -1) {
					new NextEpisodeTask().execute(LIST_SOHU, 1);
	        		return;
				}
				
				Toast.makeText(PlaySohuActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
				finish();
			}
		};

		mSohu = new SohuUtil();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		int incr;
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			if (!mController.isShowing()) {
				if (mPlayer != null) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
							keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
						if (!mSwichingEpisode) {
							mSwichingEpisode = true;
							
							if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
								incr = -1;
							else
								incr = 1;
	
							if (mInfoId != -1) {
								new NextEpisodeTask().execute(LIST_PPTV, incr);
							}
							else if (mAid != -1) {
								new NextEpisodeTask().execute(LIST_SOHU, incr);
							}
						}
					}
					else if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
							keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						if (mDisplayMode == SCREEN_FIT)
							mDisplayMode = SCREEN_STRETCH;
						else
							mDisplayMode = SCREEN_FIT;
						toggleDisplayMode(mDisplayMode, true);
					}
					else {
						mTextViewFileName.setVisibility(View.VISIBLE);
						Message msg = mHandler.obtainMessage(MSG_FADE_OUT_TV_FILENAME);
						mHandler.removeMessages(MSG_FADE_OUT_TV_FILENAME);
			            mHandler.sendMessageDelayed(msg, MEDIA_CONTROLLER_TIMEOUT);
			            
						mController.show(MEDIA_CONTROLLER_TIMEOUT * 2);
					}
				}
				
				return true;
			}

			if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode
					|| KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
				if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode)
					incr = 1;
				else
					incr = -1;

				int pos = mPlayer.getCurrentPosition();
				int step = mPlayer.getDuration() / 100 + 1000;
				Log.i(TAG, String.format("Java pos %d, step %s", pos, step));
				if (step > 30000)
					step = 30000;
				pos += (incr * step);
				if (pos > mPlayer.getDuration())
					pos = mPlayer.getDuration();
				else if (pos < 0)
					pos = 0;
				mPlayer.seekTo(pos);
				
				mController.show(MEDIA_CONTROLLER_TIMEOUT * 2);
			} else if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode
					|| KeyEvent.KEYCODE_DPAD_UP == keyCode) {
				// todo
			}

			return true;
		case KeyEvent.KEYCODE_BACK:
			if (mController.isShowing()) {
				mController.hide();
			}
			else if ((System.currentTimeMillis() - backKeyTime) > 2000) {
				Toast.makeText(PlaySohuActivity.this,
						"press another time to exit", Toast.LENGTH_SHORT)
						.show();
				backKeyTime = System.currentTimeMillis();
			} else {
				onBackPressed();
			}
			
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onStop();
		
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (mPlayer != null && event.getAction() == MotionEvent.ACTION_UP) {
			if (mController.isShowing())
				mController.hide();
			else
				mController.show(MEDIA_CONTROLLER_TIMEOUT);
		}
		
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		mHolder = sh;
		
		buildPlaylinkList();
		setupMediaPlayer();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder sh) {
		// TODO Auto-generated method stub
		
	}
	
	private Handler mHandler = new Handler(){ 
		@Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
			case MSG_PLAY_NEXT_EPISODE:
				setupMediaPlayer();
				break;
			case MSG_FADE_OUT_TV_FILENAME:
				mTextViewFileName.setVisibility(View.GONE);
				break;
			case MSG_SHOW_MEDIA_CONTROLLER:
				break;
			case MSG_INVALID_EPISODE_INDEX:
				Toast.makeText(PlaySohuActivity.this, "invalid episode", Toast.LENGTH_SHORT).show();
				break;
			case MSG_FAIL_TO_GET_ALBUM_INFO:
				Toast.makeText(PlaySohuActivity.this, "failed to get album info", Toast.LENGTH_SHORT).show();
				break;
			case MSG_FAIL_TO_GET_PLAYLINK:
				Toast.makeText(PlaySohuActivity.this, "failed to get playlink", Toast.LENGTH_SHORT).show();
				finish();
				break;
			case MSG_FAIL_TO_GET_STREAM:
				Toast.makeText(PlaySohuActivity.this, "failed to get stream", Toast.LENGTH_SHORT).show();
				finish();
				break;
            }
		}
	};
	
	boolean setupMediaPlayer() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		
		mTextViewFileName.setText(mTitle);
		mController.setFileName(mTitle);
		Toast.makeText(this, String.format("ready to play video: %s (ft %d)", mTitle, mFt), 
				Toast.LENGTH_SHORT).show();
		
		mPlayer = new FragmentMp4MediaPlayerV2();
		mPlayer.reset();
		
		mPlayer.setDisplay(mHolder);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setScreenOnWhilePlaying(true);
		
		mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdate);
		mPlayer.setOnInfoListener(mOnInfoListener);
		mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
		mPlayer.setOnPreparedListener(mOnPreparedListener);
		mPlayer.setOnErrorListener(mOnErrorListener);
		mPlayer.setOnCompletionListener(mOnCompletionListener);
		
		boolean success = false;
		try {
			mPlayer.setDataSource(m_playlink_list, m_duration_list);
			success = true;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!success)
			return false;
		
		mPlayer.prepareAsync();
		
		mIsBuffering = true;
		mBufferingProgressBar.setVisibility(View.VISIBLE);
		return true;
	}
	
	private void toggleDisplayMode(int mode, boolean popToast) {
		if (mPlayer == null) {
			Log.w(TAG, "Java: cannot toggleDisplayMode when idle");
			return;
		}
		
		int width 	= mLayout.getWidth();
		int height	= mLayout.getHeight();
		
		Log.i(TAG, String.format("Java: mLayout res: %d x %d", width, height)); 
		
		RelativeLayout.LayoutParams sufaceviewParams = (RelativeLayout.LayoutParams) mView.getLayoutParams();
		if (mode == SCREEN_FIT) {
			if ( mVideoWidth * height > width * mVideoHeight ) { 
				Log.d(TAG, "surfaceview is too tall, correcting"); 
				sufaceviewParams.height = width * mVideoHeight / mVideoWidth; 
			}
			else if ( mVideoWidth * height  < width * mVideoHeight ) 
			{ 
				Log.d(TAG, "surfaceview is too wide, correcting"); 
				sufaceviewParams.width = height * mVideoWidth / mVideoHeight; 
			}
			else {
	           sufaceviewParams.height= height;
	           sufaceviewParams.width = width;
			}
		}
		else if (mode == SCREEN_STRETCH) {
	        sufaceviewParams.width = width;
	        sufaceviewParams.height= height;
		}
		else if (mode == SCREEN_FILL) {
			if (mVideoWidth * height > width * mVideoHeight) {
				sufaceviewParams.width = height * mVideoWidth / mVideoHeight;
            } else if (mVideoWidth * height < width * mVideoHeight) {
            	sufaceviewParams.height = width * mVideoHeight / mVideoWidth;
            } 
		}
		
		Log.i(TAG, String.format("Java: surfaceview change res to %d x %d", 
        		sufaceviewParams.width, sufaceviewParams.height)); 
		mView.setLayoutParams(sufaceviewParams);
		
		if (popToast) {
			Toast.makeText(this, "切换显示模式至 " + mode_desc[mode], 
				Toast.LENGTH_SHORT).show();
		}
	}
	
	private class NextEpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			mSwichingEpisode = false;
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			
			int action = params[0];
			int incr = params[1];
			
			PlaylinkSohu l = null;
			
			if (action == LIST_PPTV) {
				if (mVirtualLinkList == null) {
					mEPG = new EPGUtil();
					boolean ret;
					ret = mEPG.virtual_channel(mTitle, mInfoId, 500, 3/*sohu*/, 1);
					if (!ret) {
						Log.e(TAG, "failed to get virtual_channel");
						return false;
					}
			
					mVirtualLinkList = mEPG.getVirtualLink();
				}
				
				mIndex += incr;
				if (mIndex < 0 || mIndex > mVirtualLinkList.size() - 1) {
					Log.i(TAG, String.format("Java: meet end %d %d", mIndex, mVirtualLinkList.size()));
					mHandler.sendEmptyMessage(MSG_INVALID_EPISODE_INDEX);
					return false;
				}
				
				Episode e = mVirtualLinkList.get(mIndex);
				String ext_id = e.getExtId();
				int pos = ext_id.indexOf('|');
	    		String sid = ext_id.substring(0, pos);
	    		String vid = ext_id.substring(pos + 1, ext_id.length());
				
	    		l = mSohu.playlink_pptv(Integer.valueOf(vid), Integer.valueOf(sid));
			}
			else if (action == LIST_SOHU) {
				if (sohu_episode_cnt == -1) {
					AlbumSohu al = mSohu.album_info(mAid);
					if (al == null) {
						Log.e(TAG, String.format("Java: failed to get album info"));
						mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_ALBUM_INFO);
						return false;
					}
					
					sohu_episode_cnt = al.getLastCount();
					Log.i(TAG, "Java: sohu_episode_cnt " + sohu_episode_cnt);
				}
				
				mIndex += incr;
				if (mIndex < 0 || mIndex > sohu_episode_cnt - 1) {
					Log.e(TAG, String.format("Java: mIndex is invlaid %d, sohu_episode_cnt %d",
							mIndex, sohu_episode_cnt));
					mHandler.sendEmptyMessage(MSG_INVALID_EPISODE_INDEX);
					return false;
				}
				
				int page_index  = mIndex / sohu_page_size + 1;
				
				if (mEpisodeList == null || page_index != sohu_page_index) {
					boolean ret;
					ret = mSohu.episode(mAid, mIndex / sohu_page_size + 1, sohu_page_size);
					if (!ret) {
						Log.e(TAG, "failed to get virtual_channel");
						return false;
					}
			
					mEpisodeList = mSohu.getEpisodeList();
				}

				int pos = mIndex - (page_index - 1) * sohu_page_size;
				EpisodeSohu ep = mEpisodeList.get(pos);
				
				if (mAid > 1000000000000L)
					l = mSohu.video_info(mSite, ep.mVid, mAid);
				else
					l = mSohu.playlink_pptv(ep.mVid, 0);
				
				Util.add_sohuvideo_history(PlaySohuActivity.this, 
						l.getTitle(), ep.mVid, mAid, mSite);
			}
	    		
    		if (l == null) {
    			Log.e(TAG, "Failed to get next video");
    			mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_PLAYLINK);
        		return false;
    		}
    		
    		mTitle = l.getTitle();
    		
    		SohuFtEnum ft = SohuFtEnum.SOHU_FT_ORIGIN;
			mUrlListStr = l.getUrl(ft);
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_SUPER;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_HIGH;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			ft = SohuFtEnum.SOHU_FT_NORMAL;
    			mUrlListStr = l.getUrl(ft);
    		}
    		if (mUrlListStr == null || mUrlListStr.isEmpty()) {
    			mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_STREAM);
    			return false;
    		}
    		
    		mDurationListStr	= l.getDuration(ft);
			
			buildPlaylinkList();
			
			mHandler.sendEmptyMessage(MSG_PLAY_NEXT_EPISODE);
			
			return true;
		}
		
	}
	
	private void buildPlaylinkList() {
		m_playlink_list.clear();
		m_duration_list.clear();
		
		StringTokenizer st;
		int i=0;
		
		st = new StringTokenizer(mUrlListStr, ",", false);
		while (st.hasMoreElements()) {
			String url = st.nextToken();
			Log.i(TAG, String.format("Java: segment #%d url: %s", i++, url));
			m_playlink_list.add(url);
		}
		
		st = new StringTokenizer(mDurationListStr, ",", false);
		i=0;
		while (st.hasMoreElements()) {
			String seg_duration = st.nextToken();
			Log.i(TAG, String.format("Java: segment #%d duration: %s", i++, seg_duration));
			int duration_msec = (int)(Double.valueOf(seg_duration) * 1000.0f);
			m_duration_list.add(duration_msec);
		}
	}
	
	private class MyMediaPlayerControl implements MediaPlayerControl {

		@Override
		public boolean canPause() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean canSeekBackward() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean canSeekForward() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public int getBufferPercentage() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getCurrentPosition() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return 0;
			
			return mPlayer.getCurrentPosition();
		}

		@Override
		public int getDuration() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return 0;
			
			return mPlayer.getDuration();
		}

		@Override
		public boolean isPlaying() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return false;
			
			return mPlayer.isPlaying();
		}

		@Override
		public void pause() {
			// TODO Auto-generated method stub
			if (mPlayer != null)
				mPlayer.pause();
		}

		@Override
		public void seekTo(int msec) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Java: seekTo " + msec);

			if (mPlayer != null)
				mPlayer.seekTo(msec);
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
			if (mPlayer != null)
				mPlayer.start();
		}
		
	}
}
