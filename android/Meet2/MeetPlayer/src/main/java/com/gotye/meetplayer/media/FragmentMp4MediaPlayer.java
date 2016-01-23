package com.gotye.meetplayer.media;

import java.io.IOException;
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

public class FragmentMp4MediaPlayer {
	private final static String TAG = "FragmentMp4MediaPlayer";
	
	
	private List<String> m_playlink_list;
	private List<Integer> m_duration_list;
	
	private int m_playlink_now_index;
	private int m_play_pos_offset;
	private int m_pre_seek_pos;
	private int m_seek_pos;
	private int m_total_duration_msec;
	
	private MediaPlayer mPlayer;
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
	
	public FragmentMp4MediaPlayer() {
		
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
		if (!setupMediaPlayer())
			throw new IllegalStateException();
	}

	public void start() throws IllegalStateException {
		if (mSeeking)
			return;
		
		if (mPlayer != null)
			mPlayer.start();
	}

	public void stop() throws IllegalStateException {
		if (mPlayer != null)
			mPlayer.stop();
	}

	public void pause() throws IllegalStateException {
		if (mSeeking)
			return;
		
		if (mPlayer != null)
			mPlayer.pause();
	}

	public void seekTo(int msec) throws IllegalStateException {
		if (msec < 0)
			throw new IllegalStateException("negative seek position");
		
		if (mSeeking) {
			m_seek_pos 		= msec;
			m_pre_seek_pos 	= msec - m_play_pos_offset;
			return;
		}
		
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
				
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
				
				Log.i(TAG, String.format("Java: seekto(back) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;
				setupMediaPlayer();
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
					mOnInfoListener.onInfo(mPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
				
				Log.i(TAG, String.format("Java: seekto(forward) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
				m_pre_seek_pos = msec - m_play_pos_offset;
				setupMediaPlayer();
			}
			else {
				mPlayer.seekTo(msec - m_play_pos_offset);
				mSeeking = false;
				
				Log.i(TAG, String.format("Java: seekto(inner) pos %d, #%d, offset %d", 
						msec, m_playlink_now_index, m_play_pos_offset));
			}
		}
	}

	public void release() {
		if (mPlayer != null)
			mPlayer.release();
	}

	public void reset() {
		
	}

	public int getCurrentPosition() {
		if (mSeeking) {
			return m_seek_pos;
		}
		
		if (mPlayer == null)
			return 0;
		
		return m_play_pos_offset + mPlayer.getCurrentPosition();
	}

	public int getDuration() {
		if (mPlayer != null || mSeeking)
			return m_total_duration_msec;
		
		return 0;
	}

	public int getVideoWidth() {
		if (mVideoWidth != 0)
			return mVideoWidth;
		else if (mPlayer != null)
			return mPlayer.getVideoWidth();
		else return 0;
	}

	public int getVideoHeight() {
		if (mVideoHeight != 0)
			return mVideoHeight;
		else if (mPlayer != null)
			return mPlayer.getVideoHeight();
		else return 0;
	}

	public boolean isPlaying() {
		if (mSeeking)
			return true;
		else if (mPlayer != null)
			return mPlayer.isPlaying();
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
	
	boolean setupMediaPlayer() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		
		mPlayer = new MediaPlayer();
		mPlayer.reset();
		
		mPlayer.setDisplay(mHolder);
		mPlayer.setAudioStreamType(mStreamType);
		mPlayer.setScreenOnWhilePlaying(mScreenOnWhilePlaying);
		mPlayer.setLooping(mLooping);
		
		mPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
		mPlayer.setOnInfoListener(mInfoListener);
		mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
		mPlayer.setOnPreparedListener(mPreparedListener);
		mPlayer.setOnErrorListener(mOnErrorListener);
		mPlayer.setOnCompletionListener(mCompletionListener);
		mPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
		
		boolean done = false;
		try {
			mPlayer.setDataSource(m_playlink_list.get(m_playlink_now_index));
			done = true;
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
		
		if (!done)
			return false;
		
		mPlayer.prepareAsync();
		return true;
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
			
			setupMediaPlayer();
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
			
			if (mOnPreparedListener != null) {
				if (m_playlink_now_index == 0) {
					mOnPreparedListener.onPrepared(mp);
				}
				else {
					if (mOnInfoListener != null) {
						mOnInfoListener.onInfo(mPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
					}
				}
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
