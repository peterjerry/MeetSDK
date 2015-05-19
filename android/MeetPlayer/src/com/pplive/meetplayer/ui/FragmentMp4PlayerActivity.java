package com.pplive.meetplayer.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.meetplayer.R;

import android.app.Activity;
import android.os.Bundle;
import android.pplive.media.player.MediaPlayer;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.util.Log;
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
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_frag_mp4_player);
		mLayout = (RelativeLayout) findViewById(R.id.main_layout);
		mView = (SurfaceView) findViewById(R.id.player_view);
		mBufferingProgressBar = (ProgressBar) findViewById(R.id.progressbar_buffering);
		
		SurfaceHolder holder = mView.getHolder();
		holder.addCallback(this);
		
		m_playlink_list = new ArrayList<String>();
		m_duration_list = new ArrayList<Integer>();
		
		StringTokenizer st;
		st = new StringTokenizer(url_list, ",", false);
		int i=0;
		while (st.hasMoreElements()) {
			String url = st.nextToken();
			Log.i(TAG, String.format("Java: segment #%d url: %s", i++, url));
			m_playlink_list.add(url);
		}
		
		st = new StringTokenizer(duration_list, ",", false);
		i=0;
		while (st.hasMoreElements()) {
			String seg_duration = st.nextToken();
			Log.i(TAG, String.format("Java: segment #%d duration: %s", i++, seg_duration));
			int duration_msec = (int)(Double.valueOf(seg_duration) * 1000);
			m_duration_list.add(duration_msec);
		}
		
		mOnInfoListener = new MediaPlayer.OnInfoListener() {
			
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				if ((MediaPlayer.MEDIA_INFO_BUFFERING_START == what) && !mIsBuffering) {
					mIsBuffering = true;
					mBufferingProgressBar.setVisibility(View.VISIBLE);
					Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_START");
				}
				else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
					mIsBuffering = false;
					mBufferingProgressBar.setVisibility(View.GONE);
					Log.i(TAG, "Java: MEDIA_INFO_BUFFERING_END");
				}
				
				return true;
			}
		};
		
		mOnBufferingUpdate = new MediaPlayer.OnBufferingUpdateListener() {
			
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int pct) {
				// TODO Auto-generated method stub
				
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
				
				mp.start();
				
				mIsBuffering = false;
				mBufferingProgressBar.setVisibility(View.GONE);
				
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
					Toast.makeText(FragmentMp4PlayerActivity.this, "Play complete", Toast.LENGTH_SHORT).show();
					mIsBuffering = false;
					mBufferingProgressBar.setVisibility(View.GONE);
					return;
				}
				
				m_play_pos_offset += m_duration_list.get(m_playlink_now_index);
				m_playlink_now_index++;
				
				try {
					mp.setDataSource(m_playlink_list.get(m_playlink_now_index));
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
				
				mp.prepareAsync();
			}
		};
		
		
		mMediaPlayerControl = new MyMediaPlayerControl();
		mController = new MediaController(this);
		
		Log.i(TAG, "Java: onCreate()");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
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
		}
		
		mPlayer = new MediaPlayer(DecodeMode.HW_SYSTEM);
		mPlayer.reset();
		
		mPlayer.setDisplay(mHolder);
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
		
		m_playlink_now_index	= 0;
		m_play_pos_offset		= 0;
		m_pre_seek_pos			= 0;
		
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
