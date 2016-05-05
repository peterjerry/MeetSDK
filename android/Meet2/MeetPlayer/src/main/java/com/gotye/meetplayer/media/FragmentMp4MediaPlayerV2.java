package com.gotye.meetplayer.media;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import com.gotye.common.util.LogUtil;
import com.gotye.meetsdk.player.MediaPlayer;

import org.apache.ivy.Main;

public class FragmentMp4MediaPlayerV2 {
	private final static String TAG = "FragmentMp4MediaPlayer";

	private final static boolean SETUP_NEXT_PLAYER_AT_ONCE = false;
	
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;
	
	private int m_playlink_now_index;
	private int m_play_pos_offset; // msec
	private int m_pre_seek_pos; // msec
	private int m_seek_pos;
	private int m_total_duration_msec;
	
	private MediaPlayer mCurrentPlayer, mNextPlayer;
	private int mPlayerImpl;
	private SurfaceHolder mHolder;
	private int mVideoWidth, mVideoHeight;
	private boolean mLooping = false;
	private boolean mScreenOnWhilePlaying = true;
	private boolean mSeeking;
	private int mStreamType;

	private static final int SYSTEM_PLAYER  = 1;
	private static final int XO_PLAYER      = 2;
	private static final int FF_PLAYER      = 3;

	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private MediaPlayer.OnErrorListener	 mOnErrorListener;
	private MediaPlayer.OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnInfoListener mOnInfoListener;
	private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;

    private MainHandler mHandler;
	
	public FragmentMp4MediaPlayerV2(int impl) {
		mPlayerImpl = impl;

        mHandler = new MainHandler(this);
	}
	
	public void setDataSource(List<String> urlList, List<Integer>durationList /* msec */)
			throws IOException,
			IllegalArgumentException, IllegalStateException {
		if (urlList == null || urlList.size() < 1 ||
				durationList == null || durationList.size() < 1) 
		{
			throw new IllegalArgumentException();
		}
		
		m_playlink_list 		= urlList;
		m_duration_list 		= durationList;
		
		m_playlink_now_index	= 0;
		m_play_pos_offset		= 0;
		m_pre_seek_pos			= 0;
		
		m_total_duration_msec	= 0;
		for (int i=0;i<durationList.size();i++) {
			m_total_duration_msec += (int)durationList.get(i);
		}
	}
			
	public void setDisplay(SurfaceHolder sh) {
		mHolder = sh;
	}
	
	public void prepareAsync() throws IllegalStateException {
		if (!setupPlayer())
			throw new IllegalStateException();
	}

	public void start() throws IllegalStateException {
		if (mSeeking)
			return;
		
		if (mCurrentPlayer != null)
			mCurrentPlayer.start();
	}

	public void stop() throws IllegalStateException {
        mHandler.removeMessages(MainHandler.MSG_CHECK_SETUP_NEXT_PLAYER);

		if (mCurrentPlayer != null) {
            try {
                mCurrentPlayer.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

	public void pause() throws IllegalStateException {
		if (mSeeking)
			return;
		
		if (mCurrentPlayer != null)
			mCurrentPlayer.pause();
	}

	public void seekTo(int msec) throws IllegalStateException {
		if (msec < 0)
			throw new IllegalStateException("negative seek position");
		
		if (mSeeking) {
			m_seek_pos 		= msec;
			m_pre_seek_pos 	= msec - m_play_pos_offset;
			return;
		}
		
		if (mCurrentPlayer != null) {
			m_seek_pos = msec;
			mSeeking = true;
			
			if (msec < m_play_pos_offset) {
				for (int i=m_playlink_now_index;i>=0;i--) {
					m_playlink_now_index--;
					m_play_pos_offset -= m_duration_list.get(m_playlink_now_index);
					if (msec >= m_play_pos_offset)
						break;
				}
				
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mCurrentPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
				
				LogUtil.info(TAG, String.format(Locale.US,
                        "Java: seekto(back) pos %d, #%d, offset %d",
                        msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;

                mHandler.removeMessages(MainHandler.MSG_CHECK_SETUP_NEXT_PLAYER);
				setupPlayer();
			}
			else if (msec >= m_play_pos_offset + m_duration_list.get(m_playlink_now_index)) {
				for (int i=m_playlink_now_index;i<m_playlink_list.size();i++) {
					m_play_pos_offset += m_duration_list.get(m_playlink_now_index);
					m_playlink_now_index++;
					if (m_playlink_now_index == m_playlink_list.size() - 1)
						break;
					else if (msec < m_play_pos_offset + m_duration_list.get(m_playlink_now_index + 1))
						break;
				}
				
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mCurrentPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
				
				LogUtil.info(TAG, String.format(Locale.US,
                        "Java: seekto(forward) pos %d, #%d, offset %d",
                        msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;

                mHandler.removeMessages(MainHandler.MSG_CHECK_SETUP_NEXT_PLAYER);
				setupPlayer();
			}
			else {
				mCurrentPlayer.seekTo(msec - m_play_pos_offset);
				mSeeking = false;
				
				LogUtil.info(TAG, String.format(Locale.US,
                        "Java: seekto(inner) pos %d, #%d, offset %d",
                        msec, m_playlink_now_index, m_play_pos_offset));
			}
		}
	}

	public void release() {
        mHandler.removeMessages(MainHandler.MSG_CHECK_SETUP_NEXT_PLAYER);

		if (mCurrentPlayer != null)
			mCurrentPlayer.release();
		
		if (mNextPlayer != null)
			mNextPlayer.release();
	}

	public void reset() {
		
	}

	public int getCurrentPosition() {
		if (mSeeking) {
			return m_seek_pos;
		}
		
		if (mCurrentPlayer == null)
			return 0;
		
		return m_play_pos_offset + mCurrentPlayer.getCurrentPosition();
	}

	public int getDuration() {
		if (mCurrentPlayer != null || mSeeking)
			return m_total_duration_msec;
		
		return 0;
	}

	public int getVideoWidth() {
		if (mVideoWidth != 0)
			return mVideoWidth;
		else if (mCurrentPlayer != null)
			return mCurrentPlayer.getVideoWidth();
		else return 0;
	}

	public int getVideoHeight() {
		if (mVideoHeight != 0)
			return mVideoHeight;
		else if (mCurrentPlayer != null)
			return mCurrentPlayer.getVideoHeight();
		else return 0;
	}

	public boolean isPlaying() {
		if (mSeeking)
			return true;
		else if (mCurrentPlayer != null)
			return mCurrentPlayer.isPlaying();
		else
			return false;
	}
	
	public boolean isLooping() {
		return mLooping;
	}

	public void setLooping (boolean looping) {
		mLooping = looping;
	}

	public void setAudioStreamType(int streamType) {
		mStreamType = streamType;
	}

	public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener listener) {
		mOnBufferingUpdateListener = listener;
	}

	public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
		mOnCompletionListener = listener;
	}

	public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
		mOnErrorListener = listener;
	}

	public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
		mOnInfoListener = listener;
	}

	public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
		mOnPreparedListener = listener;
	}

	public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener listener) {
		mOnSeekCompleteListener = listener;
	}

	public void setOnVideoSizeChangedListener(MediaPlayer.OnVideoSizeChangedListener listener) {
		mOnVideoSizeChangedListener = listener;
	}
	
	public void setScreenOnWhilePlaying(boolean screenOn) {
		mScreenOnWhilePlaying = screenOn;
	}
	
	private boolean setupPlayer() {
        LogUtil.info(TAG, "setupPlayer()");

		if (mCurrentPlayer != null) {
			try {
				mCurrentPlayer.stop();
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			mCurrentPlayer.release();
			mCurrentPlayer = null;
		}

        MediaPlayer.DecodeMode mode;
        if (mPlayerImpl == FF_PLAYER)
            mode = MediaPlayer.DecodeMode.SW;
        else if (mPlayerImpl == XO_PLAYER)
            mode = MediaPlayer.DecodeMode.HW_XOPLAYER;
        else
            mode = MediaPlayer.DecodeMode.HW_SYSTEM;

		mCurrentPlayer = new MediaPlayer(mode);
		mCurrentPlayer.reset();
		
		mCurrentPlayer.setDisplay(mHolder);
		mCurrentPlayer.setAudioStreamType(mStreamType);
		mCurrentPlayer.setScreenOnWhilePlaying(mScreenOnWhilePlaying);
		
		//mCurrentPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
		mCurrentPlayer.setOnInfoListener(mInfoListener);
		mCurrentPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
		mCurrentPlayer.setOnPreparedListener(mPreparedListener);
		mCurrentPlayer.setOnErrorListener(mOnErrorListener);
		mCurrentPlayer.setOnCompletionListener(mCompletionListener);
		mCurrentPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
		
		try {
            String url = m_playlink_list.get(m_playlink_now_index);
            LogUtil.info(TAG, "curr_player set_play_url: " + url);
			mCurrentPlayer.setDataSource(url);
			mCurrentPlayer.prepareAsync();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private void setupNextPlayer() {
        LogUtil.info(TAG, "setupNextPlayer()");

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mNextPlayer != null) {
                    try {
                        mNextPlayer.stop();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    mNextPlayer.release();
                }

                MediaPlayer.DecodeMode mode;
                if (mPlayerImpl == FF_PLAYER)
                    mode = MediaPlayer.DecodeMode.SW;
                else if (mPlayerImpl == XO_PLAYER)
                    mode = MediaPlayer.DecodeMode.HW_XOPLAYER;
                else
                    mode = MediaPlayer.DecodeMode.HW_SYSTEM;
				mNextPlayer = new MediaPlayer(mode);
				mNextPlayer.reset();

                // ffplayer MUST set HERE!!!
                // xoplayer cannot set HERE because of 2 instance cannot share 1 native window
                if (mPlayerImpl == FF_PLAYER)
                    mNextPlayer.setDisplay(mHolder);
                mNextPlayer.setAudioStreamType(mStreamType);
				//mNextPlayer.setScreenOnWhilePlaying(mScreenOnWhilePlaying);
				
				//mNextPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
				mNextPlayer.setOnInfoListener(mInfoListener);
				mNextPlayer.setOnErrorListener(mOnErrorListener);
				mNextPlayer.setOnCompletionListener(mCompletionListener);
				mNextPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
				
				try {
                    String url = m_playlink_list.get(m_playlink_now_index + 1);
                    LogUtil.info(TAG, "next_player set_play_url: " + url);
					mNextPlayer.setDataSource(url);
					mNextPlayer.prepare(); // must wait for prepare done to call setNextMediaPlayer
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
			}
			
		}).start();

	}

    private void process_next_player() {
        if (SETUP_NEXT_PLAYER_AT_ONCE) {
            if (m_playlink_now_index < m_playlink_list.size() - 1)
                setupNextPlayer();
        }
        else {
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(MainHandler.MSG_CHECK_SETUP_NEXT_PLAYER), 5000);
        }
    }

	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub	
			
			if (m_playlink_now_index == m_playlink_list.size() - 1) {
				LogUtil.info(TAG, String.format(Locale.US,
                        "Java: playlink meet end: m_playlink_now_index %d, list_size %d",
                        m_playlink_now_index, m_playlink_list.size()));

				// finish!!!
				if (mOnCompletionListener != null)
					mOnCompletionListener.onCompletion(mp);
				
				return;
			}
			
			m_play_pos_offset += m_duration_list.get(m_playlink_now_index);
            m_playlink_now_index++;

            LogUtil.info(TAG, String.format(Locale.US,
                    "Java: m_play_pos_offset %d, m_playlink_now_index %d",
                    m_play_pos_offset, m_playlink_now_index));

			if (mNextPlayer == null) {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(mp, MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
                            m_playlink_now_index);
                }

                LogUtil.error(TAG, "next player is null");
				return;
			}

			// SYSTEM player nextplay auto play ok code
            // must release first???
			// ffplay cannot set null Display now!
			if (mPlayerImpl == SYSTEM_PLAYER)
                mp.setDisplay(null);
			mp.release();

            mCurrentPlayer = mNextPlayer;
            mNextPlayer = null;
			// system player set display HERE!
			// XOPlayer cannot share one native window simultaneously
			if (mPlayerImpl == SYSTEM_PLAYER || mPlayerImpl == XO_PLAYER)
				mCurrentPlayer.setDisplay(mHolder);
            if (mPlayerImpl == XO_PLAYER || mPlayerImpl == FF_PLAYER)
                mCurrentPlayer.start(); // ffplay MUST start manually

            LogUtil.info(TAG, "Java: switch to next segment #" + m_playlink_now_index);

            process_next_player();
		}
		
	};
	
	private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			// TODO Auto-generated method stub
			if (m_pre_seek_pos > 0) {
				mp.seekTo(m_pre_seek_pos);
				m_pre_seek_pos = 0;
			}
			
			if (mSeeking)
				mSeeking = false;
			
			mp.start();
			
			if (m_playlink_now_index == 0) {
				if (mOnPreparedListener != null)
					mOnPreparedListener.onPrepared(mp);
			}
			else {
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mCurrentPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
			}

			// ONLY first OnPrepared will trigger check next player
            if (mNextPlayer == null)
				process_next_player();
		}
		
	};
	
	private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
		
		@Override
		public boolean onInfo(MediaPlayer mp, int what	, int extra) {
			// TODO Auto-generated method stub
			if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && mSeeking)
				mSeeking = false;
			
			// remove duplicated MEDIA_INFO_BUFFERING_START msg
			// because seekTo() has trigger this msg
			//if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START && mSeeking)
			//	return true;
			
			if (mOnInfoListener != null) {
				mOnInfoListener.onInfo(mp, what, extra);
			}
			
			return true;
		}
	};
	
	private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			// TODO Auto-generated method stub
			if (mSeeking)
				mSeeking = false;
			
			if (mOnSeekCompleteListener != null)
				mOnSeekCompleteListener.onSeekComplete(mp);
		}
		
	};

    private static class MainHandler extends Handler {
        private WeakReference<FragmentMp4MediaPlayerV2> mWeakPlayer;

        protected final static int MSG_CHECK_SETUP_NEXT_PLAYER = 1001;

        public MainHandler(FragmentMp4MediaPlayerV2 player) {
            mWeakPlayer = new WeakReference<FragmentMp4MediaPlayerV2>(player);
        }

        @Override
        public void handleMessage(Message msg) {
            FragmentMp4MediaPlayerV2 player = mWeakPlayer.get();
            if (player == null) {
                LogUtil.debug(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {
                case MSG_CHECK_SETUP_NEXT_PLAYER:
                    if (player.mCurrentPlayer != null) {
                        if (player.mCurrentPlayer.getCurrentPosition() + 10000 >=
                                player.mCurrentPlayer.getDuration()) {
                            player.setupNextPlayer();
                        }
                        else {
                            this.sendMessageDelayed(
                                    this.obtainMessage(MSG_CHECK_SETUP_NEXT_PLAYER), 5000);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
