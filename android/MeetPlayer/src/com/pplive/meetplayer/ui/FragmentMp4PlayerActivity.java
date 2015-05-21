package com.pplive.meetplayer.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.meetplayer.R;
import com.pplive.meetplayer.util.EPGUtil;
import com.pplive.meetplayer.util.Episode;
import com.pplive.meetplayer.util.sohu.PlaylinkSohu;
import com.pplive.meetplayer.util.sohu.SohuUtil;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

public class FragmentMp4PlayerActivity extends Activity implements Callback {

	private final static String TAG = "FragmentMp4PlayerActivity";
	
	private RelativeLayout mLayout;
	private SurfaceView mView;
	private SurfaceHolder mHolder;
	private MediaPlayer mPlayer;
	private MediaController mController;
	private MediaPlayerControl mMediaPlayerControl;
	private ProgressBar mBufferingProgressBar;
	
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private MediaPlayer.OnErrorListener	 mOnErrorListener;
	private MediaPlayer.OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdate;
	private MediaPlayer.OnInfoListener mOnInfoListener;
	
	private EPGUtil mEPG;
	private List<Episode> mVirtualLinkList;
	
	private String mUrlListStr;
	private String mDurationListStr;
	private String mTitle;
	private int mInfoId, mIndex;
	
	private int mVideoWidth, mVideoHeight;
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;
	private int m_playlink_now_index;
	private int m_play_pos_offset;
	private int m_pre_seek_pos;
	private int m_seek_pos;
	
	private boolean mSeeking = false;
	private boolean mIsBuffering = false;
	
	private int m_total_duration_msec = 1419251;
	
	public static final int SCREEN_FIT = 0; // 自适应
    public static final int SCREEN_STRETCH = 1; // 铺满屏幕 
    public static final int SCREEN_FILL = 2; // 放大裁切
    public static final int SCREEN_CENTER = 3; // 原始大小
    
    private final static String []mode_desc = {"自适应", "铺满屏幕", "放大裁切", "原始大小"};
	
	private int mDisplayMode = SCREEN_FIT;
	
	private final static int MSG_PLAY_NEXT_EPISODE 		= 1;
	private final static int MSG_INVALID_EPISODE_INDEX	= 2;
	
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
	
	private final static String duration_list = "300120,300040,300040,300040,219011";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_frag_mp4_player);
		mLayout = (RelativeLayout) findViewById(R.id.main_layout);
		mView = (SurfaceView) findViewById(R.id.player_view);
		mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		
		SurfaceHolder holder = mView.getHolder();
		holder.addCallback(this);
		
		m_playlink_list = new ArrayList<String>();
		m_duration_list = new ArrayList<Integer>();
		
		Intent intent = getIntent();
		if (intent.hasExtra("url_list") && intent.hasExtra("duration_list")) {
			mUrlListStr			= intent.getStringExtra("url_list");
			mDurationListStr	= intent.getStringExtra("duration_list");
			mTitle				= intent.getStringExtra("title");
			mInfoId				= intent.getIntExtra("info_id", 0);
    		mIndex				= intent.getIntExtra("index", 0);
		}
		else {
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
				
				if (m_pre_seek_pos > 0) {
					mp.seekTo(m_pre_seek_pos);
					m_pre_seek_pos = 0;
					mSeeking = false;
				}
				
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
				mp.start();
				
				mController.setMediaPlayer(mMediaPlayerControl);
				mController.show(3000);
			}
		};
		
		mOnErrorListener = new MediaPlayer.OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int error, int extra) {
				// TODO Auto-generated method stub
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
				Toast.makeText(FragmentMp4PlayerActivity.this, "Error " + error + " , extra " + extra,
						Toast.LENGTH_SHORT).show();
				finish();
				
				return true;
			}
		};
		
		mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				if (m_playlink_now_index == m_playlink_list.size() - 1) {
					
					if (mInfoId != -1) {
						new NextEpisodeTask().execute(1);
		        		return;
					}
					
					Toast.makeText(FragmentMp4PlayerActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
					mIsBuffering = false;
					mBufferingProgressBar.setVisibility(View.GONE);
					return;
				}
				
				m_play_pos_offset += m_duration_list.get(m_playlink_now_index);
				m_playlink_now_index++;
				
				setupMediaPlayer();
			}
		};
		
		
		mMediaPlayerControl = new MyMediaPlayerControl();
		mController = new MediaController(this);
		
		Log.i(TAG, "Java: onCreate()");
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
			int duration_msec = Integer.valueOf(seg_duration);
			m_duration_list.add(duration_msec);
		}
		
		m_playlink_now_index	= 0;
		m_play_pos_offset		= 0;
		m_pre_seek_pos			= 0;
	}
	
	private Handler mHandler = new Handler(){ 
		@Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
			case MSG_PLAY_NEXT_EPISODE:
				setupMediaPlayer();
				break;
			case MSG_INVALID_EPISODE_INDEX:
				Toast.makeText(FragmentMp4PlayerActivity.this, "invalid episode", Toast.LENGTH_SHORT).show();
				break;
            }
		}
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || 
				keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mPlayer != null) {
				mController.show(3000);
				return true;
			}
		}
		else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
				keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			if (mPlayer != null) {
				int incr = 1;
				if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
					incr = -1;
				
				new NextEpisodeTask().execute(incr);
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
		
		return super.onKeyDown(keyCode, event);
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
		if (mPlayer != null)
			mController.show(3000);
		
		return super.onTouchEvent(event);
	}

	boolean setupMediaPlayer() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		
		if (m_playlink_now_index == 0)
			Toast.makeText(this, "ready to play video: " + mTitle, Toast.LENGTH_SHORT).show();
		
		mPlayer = new MediaPlayer(DecodeMode.HW_SYSTEM);
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
		
		try {
			mPlayer.setDataSource(m_playlink_list.get(m_playlink_now_index));
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
		
		mPlayer.prepareAsync();
		
		mController.setMediaPlayer(mMediaPlayerControl);
		mController.setAnchorView(mView);
		
		mIsBuffering = true;
		mBufferingProgressBar.setVisibility(View.VISIBLE);
		return true;
	}
	
	private class NextEpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			
			int incr = params[0];
			
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
			
			SohuUtil sohu = new SohuUtil();
    		PlaylinkSohu l = sohu.getPlayLink(Integer.valueOf(vid), Integer.valueOf(sid));
    		
    		if (l == null) {
    			Toast.makeText(FragmentMp4PlayerActivity.this, "Failed to get next video", 
    					Toast.LENGTH_SHORT).show();
        		return false;
    		}
    		
    		Log.i(TAG, "Java: EPG_ITEM_VIRTUAL_SOHU " + l.getUrlListbyFT(1));
    		
    		mUrlListStr 		= l.getUrl(1);
			mDurationListStr	= l.getDuration(1);
			mTitle				= l.getTitle();
			
			buildPlaylinkList();
			
			mHandler.sendEmptyMessage(MSG_PLAY_NEXT_EPISODE);
			
			return true;
		}
		
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
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
		mHolder = holder;
		
		buildPlaylinkList();
		setupMediaPlayer();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		
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
		public int getAudioSessionId() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getBufferPercentage() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getCurrentPosition() {
			// TODO Auto-generated method stub
			if (mSeeking) {
				return m_seek_pos;
			}
			
			if (mPlayer == null)
				return 0;
			
			return m_play_pos_offset + mPlayer.getCurrentPosition();
		}

		@Override
		public int getDuration() {
			// TODO Auto-generated method stub
			if (mPlayer == null)
				return 0;
			else
				return m_total_duration_msec;
		}

		@Override
		public boolean isPlaying() {
			// TODO Auto-generated method stub
			if (mSeeking)
				return true;
			
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

			if (mPlayer != null) {
				m_seek_pos = msec;
				mSeeking = true;
				
				if (msec < m_play_pos_offset) {
					for (int i=m_playlink_now_index;i>=0;i--) {
						m_playlink_now_index--;
						m_play_pos_offset -= m_duration_list.get(m_playlink_now_index);
						if (msec >= m_play_pos_offset)
							break;
					}
					
					Log.i(TAG, String.format("Java: seekto(back) pos %d, #%d, offset %d", 
							msec, m_playlink_now_index, m_play_pos_offset));
					m_pre_seek_pos = msec - m_play_pos_offset;
					setupMediaPlayer();
				}
				else if (msec >= m_play_pos_offset + m_duration_list.get(m_playlink_now_index)) {
					for (int i=m_playlink_now_index;i<m_playlink_list.size();i++) {
						m_playlink_now_index++;
						m_play_pos_offset += m_duration_list.get(m_playlink_now_index);
						if (m_playlink_now_index == m_playlink_list.size() - 1)
							break;
						else if (msec < m_play_pos_offset + m_duration_list.get(m_playlink_now_index + 1))
							break;
					}
					
					Log.i(TAG, String.format("Java: seekto(forward) pos %d, #%d, offset %d", 
							msec, m_playlink_now_index, m_play_pos_offset));
					m_pre_seek_pos = msec - m_play_pos_offset;
					setupMediaPlayer();
				}
				else {
					Log.i(TAG, String.format("Java: seekto(inner) pos %d, #%d, offset %d", 
							msec, m_playlink_now_index, m_play_pos_offset));
					mPlayer.seekTo(msec - m_play_pos_offset);
					mSeeking = false;
				}
			}
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
			if (mPlayer != null)
				mPlayer.start();
		}
		
	}
}
