package com.pplive.meetplayer.media;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;

import android.util.Log;
import android.view.SurfaceHolder;

public class FragmentMp4MediaPlayerV2 {
	private final static String TAG = "FragmentMp4MediaPlayer";
	
	
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;
	
	private int m_playlink_now_index;
	private int m_play_pos_offset;
	private int m_pre_seek_pos;
	private int m_seek_pos;
	private int m_total_duration_msec;
	
	private MediaPlayer mCurrentPlayer, mNextPlayer;
	private SurfaceHolder mHolder;
	private int mVideoWidth, mVideoHeight;
	private boolean mLooping = false;
	private boolean mScreenOnWhilePlaying = true;
	private boolean mSeeking;
	private int mStreamType;
	
	private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
	private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private MediaPlayer.OnErrorListener	 mOnErrorListener;
	private MediaPlayer.OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnInfoListener mOnInfoListener;
	private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
	
	public FragmentMp4MediaPlayerV2() {
		
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
		if (mCurrentPlayer != null)
			mCurrentPlayer.stop();
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
				
				Log.i(TAG, String.format("Java: seekto(back) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;
				
				setupPlayer();
			}
			else if (msec >= m_play_pos_offset + m_duration_list.get(m_playlink_now_index)) {
				for (int i=m_playlink_now_index;i<m_playlink_list.size();i++) {
					m_play_pos_offset += (int)m_duration_list.get(m_playlink_now_index);
					m_playlink_now_index++;
					if (m_playlink_now_index == m_playlink_list.size() - 1)
						break;
					else if (msec < m_play_pos_offset + m_duration_list.get(m_playlink_now_index + 1))
						break;
				}
				
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mCurrentPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
				
				Log.i(TAG, String.format("Java: seekto(forward) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;
				
				setupPlayer();
			}
			else {
				mCurrentPlayer.seekTo(msec - m_play_pos_offset);
				mSeeking = false;
				
				Log.i(TAG, String.format("Java: seekto(inner) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
			}
		}
	}

	public void release() {
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

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
		mOnBufferingUpdateListener = listener;
	}

	public void setOnCompletionListener(OnCompletionListener listener) {
		mOnCompletionListener = listener;
	}

	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}

	public void setOnInfoListener(OnInfoListener listener) {
		mOnInfoListener = listener;
	}

	public void setOnPreparedListener(OnPreparedListener listener) {
		mOnPreparedListener = listener;
	}

	public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
		mOnSeekCompleteListener = listener;
	}

	public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
		mOnVideoSizeChangedListener = listener;
	}
	
	public void setScreenOnWhilePlaying(boolean screenOn) {
		mScreenOnWhilePlaying = screenOn;
	}
	
	private boolean setupPlayer() {
		if (mCurrentPlayer != null) {
			mCurrentPlayer.stop();
			mCurrentPlayer.release();
			mCurrentPlayer = null;
		}
		
		mCurrentPlayer = new MediaPlayer();
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
			mCurrentPlayer.setDataSource(m_playlink_list.get(m_playlink_now_index));
			mCurrentPlayer.prepare();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
				
		if (m_playlink_now_index < m_playlink_list.size() - 1) {
			setupNextPlayer();
		}
		
		return true;
	}
	
	private void setupNextPlayer() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mNextPlayer != null)
					mNextPlayer.release();
				
				mNextPlayer = new MediaPlayer();
				mNextPlayer.reset();
				
				mNextPlayer.setAudioStreamType(mStreamType);
				//mNextPlayer.setScreenOnWhilePlaying(mScreenOnWhilePlaying);
				
				//mNextPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
				mNextPlayer.setOnInfoListener(mInfoListener);
				mNextPlayer.setOnErrorListener(mOnErrorListener);
				mNextPlayer.setOnCompletionListener(mCompletionListener);
				mNextPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
				
				try {
					mNextPlayer.setDataSource(m_playlink_list.get(m_playlink_now_index + 1));
					mNextPlayer.prepare(); // must wait for prepare done to call setNextMediaPlayer
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
			}
			
		}).start();

	}
	
	private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub	
			
			if (m_playlink_now_index == m_playlink_list.size() - 1) {
				// finish!!!
				if (mOnCompletionListener != null)
					mOnCompletionListener.onCompletion(mp);
				
				return;
			}
			
			m_play_pos_offset += m_duration_list.get(m_playlink_now_index++);
			
			mp.setDisplay(null);
			if (mNextPlayer == null && mOnErrorListener != null) {
				mOnErrorListener.onError(mp, 556, m_playlink_now_index);
			}
			
			mCurrentPlayer = mNextPlayer;
			mNextPlayer = null;
			mCurrentPlayer.setDisplay(mHolder);

			Log.i(TAG, "Java: switch to next segment #" + m_playlink_now_index);
			
			if (m_playlink_now_index < m_playlink_list.size() - 1) {
				setupNextPlayer();
			}
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
}
