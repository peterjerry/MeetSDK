package android.pplive.media.player;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.subtitle.SimpleSubTitleParser;
import android.pplive.media.util.LogUtils;
import android.pplive.media.util.UrlUtil;
import android.pplive.media.util.Utils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

public class XOMediaPlayer extends BaseMediaPlayer {
	
	private static final String TAG = "XOMediaPlayer";
	
	protected final static long TIMEOUT = 5000;// timeoutUs
	
	private static final int EVENT_SEEKTO 				= 1003;
	private static final int EVENT_BUFFERING_UPDATE_CHECK	= 1004;
	
	private static final int AV_SYNC_THRESHOLD_MSEC 		= 100; // 100 msec
	private static final int AV_NOSYNC_THRESHOLD			= 10000; // 10 sec
	
	private String mUrl = null;
	
	private boolean mIsBuffering = false;
	
	private Surface mSurface = null;
	private MediaExtractable mExtractor = null;
	
	private int mVideoWidth = 0;
	private int mVideoHeight = 0;

	private AudioTrack mAudioTrack = null;
	private AudioTrack.OnPlaybackPositionUpdateListener mAudioPositionUpdateListener = null;
	
	private MediaFormat mAudioFormat = null;
	private MediaFormat mVideoFormat = null;
	
	private int mAudioTrackIndex = -1;
	private int mVideoTrackIndex = -1;
	
	private boolean mHaveAudio = false;
	private boolean mHaveVideo = false;
	
	private MediaCodec mAudioCodec = null;
	private MediaCodec mVideoCodec = null;
	
	private MediaCodec.BufferInfo mAudioBufferInfo = null;
	private MediaCodec.BufferInfo mVideoBufferInfo = null;
	
	private byte[] mAudioData = null;
	
	// common
	private long mDurationUsec 		= 0L; // getduration()
	private long mSeekingTimeMsec 		= 0L; // for seek
	private long mCurrentTimeMsec 		= 0L; // getcurrentpos()
	private long mTotalStartMsec		= 0L; // based on system clock
	// video
	private boolean mVideoFirstFrame;
	private Lock mVideoLock;
	private Condition mVideoNotFullCond;   
    private Condition mVideoNotEmptyCond;
	private long mLastOnVideoTimeMsec 	= 0L;
	private long mRenderedFrameCnt 	= 0L;
	private long mDecodedFrameCnt		= 0L;
	private long mLastDelayMsec		= 0L;
	private long mLastVideoFrameMsec	= 0L;
	private long mFrameTimerMsec		= 0L;
	// audio
	private long mAudioStartMsec		= 0L;
	private long mAudioPositionMsec	= 0L;
	private long mAveAudioPktMsec		= 0L;
	private long mLastAudioPktMSec		= 0L;
	private long mAudioLatencyMsec		= 0L;
	
	private final static boolean NO_AUDIO = false;
	
	private Handler mMediaEventHandler = null;
	
	private boolean mSeeking = false;
	
	private Lock mLock = new ReentrantLock();
	private Condition notStopped = mLock.newCondition();
	
	private Thread mReadSampleThr;
	private Thread mRenderVideoThr;
	private List<RenderBuf> mRenderList;
	
	class RenderBuf {
		private long render_clock_msec;
		private int buf_index;
		private boolean eof;
	};
	
	public XOMediaPlayer(MediaPlayer mp) {
		super(mp);
		
		HandlerThread ht = new HandlerThread("MediaEventHandler");
		ht.start();
		
		mMediaEventHandler = new Handler(ht.getLooper()) {
			
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case EVENT_SEEKTO:
						LogUtils.info("EVENT_SEEKTO event");
						onSeekToEvent();
						return;
					case EVENT_BUFFERING_UPDATE_CHECK:
						LogUtils.debug("EVENT_BUFFERING_UPDATE_CHECK event");
						onBufferingUpdateCheckEvent();
						return;
					default:
						LogUtils.error("Unknown message type " + msg.what);
						return;
				}
			}
		};
	}
	
	/**
	 * Called from native code when an interesting event happens. This method
	 * just uses the EventHandler system to post the event back to the main app
	 * thread. We use a weak reference to the original MediaPlayer object so
	 * that the native code is safe from the object disappearing from underneath
	 * it. (This is the cookie passed to native_setup().)
	 */
	private static void postEventFromNative(Object mediaplayer_ref, int what,
			int arg1, int arg2, Object obj) {
		XOMediaPlayer mp = (XOMediaPlayer) ((WeakReference<?>) mediaplayer_ref).get();
		if (mp == null) {
			return;
		}

		if (mp.mEventHandler != null) {			
			Message msg = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
			msg.sendToTarget();
		}
	}
	
	@Override
	public void setDataSource(String path) throws IOException,
			IllegalArgumentException, IllegalStateException {
		
		if (path == null || path.equals("")) {
			throw new IllegalArgumentException("Invalid Uri!!!");
		}
		
		mUrl = path;
		MediaExtractable extractor = 
				UrlUtil.isUseSystemExtractor(mUrl) ? 
				new DefaultMediaExtractor() : new FFMediaExtractor(new WeakReference<XOMediaPlayer>(this));
		
		setDataSource(extractor);
	}
	
	@Override
	public void setDataSource(Context ctx, Uri uri) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {
		setDataSource(ctx, uri, null);
	}
	
	@Override
	public void setDataSource(Context context, Uri uri,
			Map<String, String> headers) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {

		String scheme = uri.getScheme();
		if (scheme == null || scheme.equals("file")) {
			//local file
			setDataSource(uri.getPath());
		}
		else {
			//network path
			setDataSource(uri.toString());
		}
	}
	
	@Override
	public void setDataSource(FileDescriptor fd) throws IOException,
			IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("not implement");
	}
	
	@Override
	public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException,
			IllegalArgumentException, IllegalStateException {
		// intentionally less than LONG_MAX
		throw new IllegalStateException("not implement");
	}
	
	private void setDataSource(MediaExtractable extractor) throws IllegalStateException {
	    LogUtils.info("setDataSource extractor");
		mExtractor = extractor;
		setState(PlayState.INITIALIZED);
	}
	
	private void postSetVideoSizeEvent(int width, int height) {
	    LogUtils.info("postSetVideoSizeEvent: width: " + width + ", height: " + height);
		
		mVideoWidth = width;
		mVideoHeight = height;
		
		Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_SET_VIDEO_SIZE, width, height);
		msg.sendToTarget();
	}

	@Override
	public void setDisplay(SurfaceHolder sh) {
		super.setDisplay(sh);
		
		Surface surface = null;
		if (sh != null) {
			surface = sh.getSurface();
		}
		
		setSurface(surface);
	}
	
	private void setSurface(Surface surface) {
		mSurface = surface;
	}

	@Override
	public void prepare() throws IOException, IllegalStateException {
		throw new IllegalStateException("Do not support this operation.");
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		PlayState state = getState();
		if (state != PlayState.INITIALIZED) {
		    LogUtils.error("Error State: " + state);
			throw new IllegalStateException("Error State: " + state);
		}

		setState(PlayState.PREPARING);
		
		mMediaEventHandler.postAtFrontOfQueue(new Runnable() {
			
			@Override
			public void run() {
				mLock.lock();
				LogUtils.info("onPrepareAsyncEvent Start!!!");
				
				boolean ret;
				ret = initMediaExtractor();
				if (!ret) {
					// omit stop when preparing
					if (getState() != PlayState.STOPPING) {
						Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
						msg.arg1 = MediaPlayer.MEDIA_ERROR_FAIL_TO_OPEN;
						msg.sendToTarget();
					}
					
					mLock.unlock();
					return;
				}
				
				ret = initAudioTrack();
				if (!ret) {
					Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
					msg.arg1 = MediaPlayer.MEDIA_ERROR_AUDIO_RENDER;
					msg.sendToTarget();
					mLock.unlock();
					return;
				}
				
				ret = initAudioDecoder();
				if (!ret) {
					Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
					msg.arg1 = MediaPlayer.MEDIA_ERROR_AUDIO_DECODER;
					msg.sendToTarget();
					mLock.unlock();
					return;
				}
				
				ret = initVideoDecoder();
				if (!ret) {
					Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
					msg.arg1 = MediaPlayer.MEDIA_ERROR_VIDEO_DECODER;
					msg.sendToTarget();
					mLock.unlock();
					return;
				}
					
				setState(PlayState.PREPARED);
					
				Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
				msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE; // extra
				msg.arg2 = MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER;
				msg.sendToTarget();				
				
				mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_PREPARED);
				
				mRenderedFrameCnt = 0;
				mLock.unlock();
			}
		});
	}
	
	private boolean initMediaExtractor() {
	    LogUtils.debug("start initMediaExtractor");
		try {
			// would block
			mExtractor.setDataSource(mUrl);
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.error("IOException", e);
			return false;
		}

		int trackCount = mExtractor.getTrackCount();
		LogUtils.info("Java: trackCount: " + trackCount);
		for (int index = 0; index < trackCount; index++) {
			LogUtils.info("Java: MediaFormat get # " + index + " trackinfo");
			MediaFormat format = mExtractor.getTrackFormat(index);
			LogUtils.info("Java: MediaFormat: " + format.toString());
			
			if (format.containsKey("csd-0")) {
				ByteBuffer bb = format.getByteBuffer("csd-0");
				Log.i(TAG, "Java: MediaFormat csd-0(0): " + bb.toString());
				byte []content = new byte[bb.limit()];
				bb.get(content);
				bb.rewind();
				Log.i(TAG, "Java: MediaFormat csd-0(1): " + content.toString());
				StringBuffer sbHex = new StringBuffer();
				StringBuffer sbInt = new StringBuffer();
				for (int j=0;j<content.length;j++) {
					sbHex.append(String.format("0x%02x ", content[j]));
					sbInt.append(String.format("%d, ", content[j]));
				}
				Log.i(TAG, "Java: MediaFormat csd-0(2): hex: " + sbHex.toString() + "int: " + sbInt.toString());
			}
			if (format.containsKey("csd-1")) {
				ByteBuffer bb = format.getByteBuffer("csd-1");
				Log.i(TAG, "Java: MediaFormat csd-1(0): " + bb.toString());
				byte []content = new byte[bb.limit()];
				bb.get(content);
				bb.rewind();
				Log.i(TAG, "Java: MediaFormat csd-1(1): " + content.toString());
				StringBuffer sbHex = new StringBuffer();
				StringBuffer sbInt = new StringBuffer();
				for(int j=0;j<content.length;j++) {
					sbHex.append(String.format("0x%02x ", content[j]));
					sbInt.append(String.format("%d, ", content[j]));
				}
				Log.i(TAG, "Java: MediaFormat csd-1(2): hex: " + sbHex.toString() + "int: " + sbInt.toString());
			}
			
			String mime = format.getString(MediaFormat.KEY_MIME);
			
			if (mime == null || mime.equals("")) {
				continue;
			} else {
				mime = mime.toLowerCase();
			}
			
			if (!mHaveAudio && mime.startsWith("audio/")) {
				setAudioFormat(format);
				mAudioTrackIndex = index;
				LogUtils.info("Java: mAudioTrackIndex: " + mAudioTrackIndex);
				mHaveAudio = true;
			} else if (!mHaveVideo && mime.startsWith("video/")) {
				setVideoFormat(format);
				mVideoTrackIndex = index;
				LogUtils.info("Java: mVideoTrackIndex: " + mVideoTrackIndex);
				mHaveVideo = true;
			} else {
				// unknown media type;
		         LogUtils.warn("Java: unknown media type");
			}
			
			if (mHaveAudio && mHaveVideo) {
				break;
			}
		}
		
		if (!mHaveAudio || !mHaveVideo) {
			LogUtils.error("Java: both video and audio stream was not found");
			return false;
		}
		
		LogUtils.info("Java: Init MediaExtractor Success!!!");
		return true;
	}
	
	private void setAudioFormat(MediaFormat format) {
	    LogUtils.info("Java: setAudioFormat");
		mAudioFormat = format;
		
		long duration;
		if (format.containsKey(MediaFormat.KEY_DURATION))
			duration = format.getLong(MediaFormat.KEY_DURATION);
		else
			duration = 0;
		mDurationUsec = mDurationUsec > duration ? mDurationUsec : duration;
		LogUtils.info("audio duration: " + mDurationUsec);
	}
	
	private void setVideoFormat(MediaFormat format) {
	    LogUtils.info("Java: setVideoFormat");
		mVideoFormat = format;
		
		long duration;
		if (format.containsKey(MediaFormat.KEY_DURATION))
			duration = format.getLong(MediaFormat.KEY_DURATION);
		else
			duration = 0;
		mDurationUsec = mDurationUsec > duration ? mDurationUsec : duration;
		LogUtils.info("video duration: " + mDurationUsec);
		int width = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
		int height = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
		postSetVideoSizeEvent(width, height);
	}
	
	private boolean initAudioTrack() {
	    LogUtils.info("start to initAudioTrack");
	    
		if (mAudioTrack != null) {
			mAudioTrack.flush();
			mAudioTrack.stop();
			mAudioTrack.release();
			mAudioTrack = null;
		}
		
		int channelCount = mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
		if (channelCount < 1 || channelCount > 2) {
			LogUtils.error("audio track NOT support channelCount: " + channelCount);
			return false;
		}
		
		int channelConfig = (channelCount == 1) ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
		
		int sampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		
		int minSize = AudioTrack.getMinBufferSize(
				sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
		
		mAudioLatencyMsec = minSize * 2 * 1000 / (sampleRate * channelCount * 2/*s16*/);
		
		LogUtils.info(String.format("audio format: channels %d, channel_cfg %d, sample_rate %d, minbufsize %d, latency %d", 
				channelCount, channelConfig, sampleRate, minSize, mAudioLatencyMsec));
		
		mAudioTrack = 
			new AudioTrack(
				AudioManager.STREAM_MUSIC,
				sampleRate,
				channelConfig, 
				AudioFormat.ENCODING_PCM_16BIT,
				minSize * 2,
				AudioTrack.MODE_STREAM);
		
		mAudioPositionUpdateListener = new AudioTrack.OnPlaybackPositionUpdateListener() {

					@Override
					public void onMarkerReached(AudioTrack audiotrack) {
						// TODO Auto-generated method stub
						LogUtils.info("Java: AudioTrack onMarkerReached");
					}

					@Override
					public void onPeriodicNotification(AudioTrack audiotrack) {
						// TODO Auto-generated method stub
						LogUtils.info("Java: AudioTrack onPeriodicNotification");
					}
			
		};
		
		mAudioTrack.setPlaybackPositionUpdateListener(mAudioPositionUpdateListener);
		
		if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
			LogUtils.error("failed to new AudioTrack: " + mAudioTrack.getState());
			return false;
		}
		
		mAudioTrack.play();
		
		LogUtils.info("Init Audio Track Success!!!");
		return true;
	}
	
	private boolean initAudioDecoder() {
        LogUtils.debug("start to initAudioDecoder");

		boolean ret = true;
		
		String mime = mAudioFormat.getString(MediaFormat.KEY_MIME);
		try {
			LogUtils.info("audio mime: " + mime);
			mAudioCodec = MediaCodec.createDecoderByType(mime);
			
			mAudioCodec.configure(mAudioFormat, null /* surface */, null /* crypto */, 0 /* flags */);
			mAudioCodec.start();
			
			mExtractor.selectTrack(mAudioTrackIndex);
			
			mAudioBufferInfo = new MediaCodec.BufferInfo();
			
			LogUtils.info("Init Audio Decoder Success!!!");
		} catch (Exception e) {
			e.printStackTrace();
		    LogUtils.error("Exception", e);
			
			mAudioCodec = null;
			ret = false;
		}
		
		return ret;
	}
	
	private boolean initVideoDecoder() {
        LogUtils.info("start to initVideoDecoder");

		boolean ret = true;
		
		String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
		try {
			LogUtils.info("video mime: " + mime);
			mVideoCodec = MediaCodec.createDecoderByType(mime);
			
			mVideoCodec.configure(mVideoFormat, mSurface /* surface */, null /* crypto */, 0 /* flags */);
			mVideoCodec.start();
	
			mExtractor.selectTrack(mVideoTrackIndex);
			
			mVideoBufferInfo = new MediaCodec.BufferInfo();
			LogUtils.info("mVideoBufferInfo size: " + mVideoBufferInfo.size);
			LogUtils.info("mVideoBufferInfo: " + mVideoBufferInfo.toString());
			
			mVideoLock = new ReentrantLock();
			mVideoNotFullCond = mVideoLock.newCondition();
			mVideoNotEmptyCond = mVideoLock.newCondition();
			
			LogUtils.info("Init Video Decoder Success!!!");
		} catch (Exception e) {
			e.printStackTrace();
		    LogUtils.error("Exception", e);
			
			mVideoCodec = null;
			ret = false;
		}
		
		mRenderList = new ArrayList<RenderBuf>();
		mVideoFirstFrame = false;
		return ret;
	}
	
	@Override
	public void start() throws IllegalStateException {
		PlayState state = getState();
		if (isPlaying()) {
			LogUtils.info("already playing");
			return;
		} else if (state != PlayState.PREPARED && state != PlayState.PAUSED) {
		    LogUtils.error("IllegalStateException - Error State: " + state);
			throw new IllegalStateException("Error State: " + state);
		}
		
		stayAwake(true);
		
		setState(PlayState.STARTED);
		play_l();
	}
	
	private void play_l() {	
		ResetStatics();
		
		if (mReadSampleThr == null) {
			mReadSampleThr = new Thread(new Runnable(){
				@Override
				public void run(){
			        read_sample_proc();
			    }
			});
			mReadSampleThr.start();
		}
		
		if (mRenderVideoThr == null) {
			mRenderVideoThr = new Thread(new Runnable(){
				@Override
				public void run(){
			        render_proc();
			    }
			});
			mRenderVideoThr.start();
		}
		
		postBufferingUpdateCheckEvent();
	}
	
	private void read_sample_proc() {
		LogUtils.info("read sample thread started");
		
		boolean sawInputEOS = false;
		
		while (getState() != PlayState.STOPPING && !sawInputEOS) {
			if (getState() == PlayState.PAUSED) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			
			int trackIndex = mExtractor.getSampleTrackIndex();
			MediaCodec codec = getCodec(trackIndex);
			BufferInfo info = (trackIndex == mVideoTrackIndex ? mVideoBufferInfo : mAudioBufferInfo);
			int inputBufIndex;
			try {
				inputBufIndex = codec.dequeueInputBuffer(TIMEOUT);
			}
			catch (IllegalStateException e) {
				e.printStackTrace();
				LogUtils.error("codec dequeueInputBuffer exception" + e.getMessage());
				break;
			}
			
			if (inputBufIndex >= 0) {
				ByteBuffer dstBuf = codec.getInputBuffers()[inputBufIndex];
                int sampleSize = mExtractor.readSampleData(dstBuf, 0 /* offset */);
                long presentationTimeUs = 0;
                if (sampleSize < 0) {
                    Log.d(TAG, "saw input EOS.");
                    sawInputEOS = true;
                    sampleSize = 0;
                } else {
                    presentationTimeUs = mExtractor.getSampleTime();
                    /*samplecounter++;
                    if (samplecounter == eosframe) {
                    	mSawInputEOS = true;
                    }*/
                }
                int flags = mExtractor.getSampleFlags();
                codec.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        sampleSize,
                        presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                
                LogUtils.info(String.format("track #%d(%s): size %d, pts %d msec, flags %d", 
    					trackIndex, 
    					trackIndex == mVideoTrackIndex ? "video" : "audio",
    					sampleSize, presentationTimeUs / 1000, flags));
                
                if (!sawInputEOS) {
                	mExtractor.advance();
                }
			} // end of inputBufIndex >= 0
			int res = codec.dequeueOutputBuffer(info, TIMEOUT);
            //deadDecoderCounter++;
            if (res >= 0) {
            	if (info.size > 0) {
            	
            	}
            	
            	int outputBufIndex = res;
            	boolean render = true;
            	
            	if (mVideoTrackIndex == trackIndex) {
            		LogUtils.info(String.format("[DecodeVideoBuffer] presentationTimeUs: %d, flags: %d",
            				info.presentationTimeUs, info.flags));

            		RenderBuf buf = new RenderBuf();
            		buf.buf_index = outputBufIndex;
            		buf.render_clock_msec = info.presentationTimeUs / 1000;
            		buf.eof = ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);
            		
            		mVideoLock.lock();
            		mRenderList.add(buf);
            		mVideoNotEmptyCond.signal();
            		mVideoLock.unlock();
            		
            		if (!mVideoFirstFrame) {
            			LogUtils.info("Java: first video frame out");
            			mVideoFirstFrame = true;
            		}
            		
            		continue;
            	}
            	else if (mAudioTrackIndex == trackIndex) {
            		LogUtils.info(String.format("[DecodeAudioBuffer] presentationTimeUs: %d, flags: %d",
            				info.presentationTimeUs, info.flags));

            		render = false;
            		
            		if (!NO_AUDIO) {
	            		int bufSize = info.size;
	    				if (mAudioData == null || mAudioData.length < bufSize) {
	    					// Allocate a new buffer.
	    					mAudioData = new byte[bufSize];
	    				}
	    				
	    				ByteBuffer outputBuf = codec.getOutputBuffers()[outputBufIndex];
	    				outputBuf.get(mAudioData);
	    				// would block
	    				mAudioTrack.write(mAudioData, 0, bufSize);
	    				
	    				// update audio clock
	    				mAudioStartMsec = System.currentTimeMillis();
	    				
	    				mAudioPositionMsec = info.presentationTimeUs / 1000;
            		}
            		
    				// only audio output buffer will be released here!
    				codec.releaseOutputBuffer(outputBufIndex, render);
            	}
                
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(TAG, "saw output EOS.");
                    sawInputEOS = true;
                    postPlaybackCompletionEvent();
                }
			} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
	            //codecOutputBuffers = codec.getOutputBuffers();
	            Log.i(TAG, "output buffers have changed.");
	        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
	            MediaFormat oformat = codec.getOutputFormat();
	            Log.i(TAG, "output format has changed to " + oformat);
	        } else {
	            Log.i(TAG, "no output");
	        }
		} // end of while
		
		LogUtils.info("read sample thread exited");
	}
	
	private void render_proc() {
		LogUtils.info("render thread started");
		
		boolean sawInputEOS = false;
		
		while (getState() != PlayState.STOPPING && !sawInputEOS) {
			if (getState() == PlayState.PAUSED) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				continue;
			}
			
			RenderBuf buf = null;
			
			mVideoLock.lock();
			try {    
	            while (mRenderList.size() == 0 && getState() != PlayState.STOPPING)    
	            	mVideoNotEmptyCond.await(); // how to quit?
	            
	            if (mRenderList.size() == 0) {
					LogUtils.info("Java: video buf is null");
					break;
				}
	            
	            buf = mRenderList.get(0);
				mRenderList.remove(0);
	            mVideoNotFullCond.signal();    
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LogUtils.info("Java: mVideoNotEmptyCond.await InterruptedException");
			} finally {    
	        	mVideoLock.unlock();
	        }
			
			if (buf == null) {
				LogUtils.info("Java: video buf is null");
				break;
			}
			
			boolean render = true;
			
			long video_clock_msec = buf.render_clock_msec;
			int bufIndex = buf.buf_index;
			sawInputEOS = buf.eof;
			
			long delay_msec = video_clock_msec - mLastVideoFrameMsec; // always 1000 / framerate(40 msec)
			if (delay_msec < 0 || delay_msec > 1000) {
				// fix invalid pts
				delay_msec = mLastDelayMsec;
			}
			
			mLastVideoFrameMsec = video_clock_msec;
			mLastDelayMsec = delay_msec;
			
			long audio_clock_msec = (long) getCurrentPosition();
			long av_diff_msec = video_clock_msec - audio_clock_msec;
			if (NO_AUDIO)
				av_diff_msec = 0;
			LogUtils.info(String.format("video %d, audio %d, diff_msec %d msec", 
					video_clock_msec, audio_clock_msec, av_diff_msec));
			
			Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
			msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC;
			msg.arg2 = (int)av_diff_msec;
			msg.sendToTarget();
			
			long sync_threshold_msec = (delay_msec > AV_SYNC_THRESHOLD_MSEC) ? delay_msec : AV_SYNC_THRESHOLD_MSEC;
			if (av_diff_msec < AV_NOSYNC_THRESHOLD && av_diff_msec > -AV_NOSYNC_THRESHOLD) {
				if (av_diff_msec <= -sync_threshold_msec) {
					delay_msec = 0;
				} else if (av_diff_msec >= sync_threshold_msec && av_diff_msec <= (sync_threshold_msec * 2)) {
					delay_msec = 2 * delay_msec;
				} else if (av_diff_msec >= (sync_threshold_msec * 2)){
					delay_msec = av_diff_msec; // for seek case
				}
			}

			LogUtils.debug("delay_msec: " + delay_msec);
			mFrameTimerMsec += delay_msec;
			
			if (av_diff_msec < -sync_threshold_msec * 2) {
				render = false;
				msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
				msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME;
				msg.arg2 = (int)av_diff_msec;
				msg.sendToTarget();
			}
			
			// render it!
			mVideoCodec.releaseOutputBuffer(bufIndex, render);
			
            if (render) {
				mRenderedFrameCnt++;
				
				msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
				msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME;
				msg.arg2 = (int)mRenderedFrameCnt;
				msg.sendToTarget();
            }
			
			long now_msec = System.currentTimeMillis();
			long elapsed_msec = now_msec - mLastOnVideoTimeMsec;
			if (elapsed_msec > 1000) {
				int render_fps = (int)(mRenderedFrameCnt * 1000 / (now_msec - mTotalStartMsec));
				Message msg2 = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
				msg2.arg1 = MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS;
				msg2.arg2 = (int)render_fps;
				msg2.sendToTarget();
				mLastOnVideoTimeMsec = now_msec;
			}
			
			if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				LogUtils.info("saw video Output EOS.");
				sawInputEOS = true;
			}
			
			long schedule_msec = mFrameTimerMsec - System.currentTimeMillis();
			LogUtils.info("schedule_msec: " + schedule_msec);

			if (schedule_msec >= 10 && !NO_AUDIO) {
				try {
					Thread.sleep(schedule_msec);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end of while
		
		LogUtils.info("render thread exited");
	}
	
	private void ResetStatics()
	{
		mDecodedFrameCnt	= 0L;
		mRenderedFrameCnt	= 0L;
		mTotalStartMsec		= System.currentTimeMillis();
		mFrameTimerMsec		= System.currentTimeMillis();
	}
	
	private MediaCodec getCodec(int trackIndex) {
		MediaCodec codec = null; 
		
		if (trackIndex == mAudioTrackIndex) {
			codec = mAudioCodec;
		} else if (trackIndex == mVideoTrackIndex) {
			codec = mVideoCodec;
		} else {
			LogUtils.error("unknown trackIndex: " + trackIndex);
			return null;
		}
		
		return codec;
	}
	
	private void postBufferingUpdateCheckEvent() {
		if (getState() == PlayState.PLAYBACK_COMPLETED) {
			return;
		}
		
		Message msg = mMediaEventHandler.obtainMessage(EVENT_BUFFERING_UPDATE_CHECK);
		mMediaEventHandler.sendMessageDelayed(msg, 1000 /* milliseconds */);
	}
	
	private void removeBufferingUpdateCheckEvent() {
		mMediaEventHandler.removeMessages(EVENT_BUFFERING_UPDATE_CHECK);
	}
	
	private void onBufferingUpdateCheckEvent() {
		// Make sure only one CheckBuffer Event in MessageQueue.
		removeBufferingUpdateCheckEvent();
		
		postBufferingUpdateEvent();
		
		postBufferingUpdateCheckEvent();
	}
	
	private void postBufferingUpdateEvent() {
		long currentTimeUs = getCurrentPosition() * 1000;
		long cachedDurationUs = mExtractor.getCachedDuration();
		
		cachedDurationUs = cachedDurationUs > 0 ? cachedDurationUs : 0;
		
		long pct100;
		
		if (mDurationUsec > 0) {
			pct100 = 1 + cachedDurationUs * 100 / mDurationUsec;
			if (pct100 > 100)
				pct100 = 100;
		} else {
			pct100 = 0;
		}
		
		Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_BUFFERING_UPDATE);
		msg.arg1 = (int)pct100;
		msg.sendToTarget();
	}
	
	private /* synchronized */ boolean isBuffering() {
		return mIsBuffering;
	}
	
	private /* synchronized */ void setBufferingStatus(boolean status) {
		mIsBuffering = status;
	}
	
	private void postPlaybackCompletionEvent() {
		setState(PlayState.PLAYBACK_COMPLETED);
		mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_PLAYBACK_COMPLETE);
	}

	@Override
	public void stop() {
	    LogUtils.debug("stop() 1");
		
	    PlayState state = getState();
		if (state == PlayState.STOPPED) {
		    LogUtils.debug("Already stopped");
			return;
		}
		
		if (state == PlayState.STARTED || state ==  PlayState.PAUSED) {
			setState(PlayState.STOPPING);
			
			mVideoLock.lock();
			mVideoNotEmptyCond.signal();
			mVideoLock.unlock();
			
			if (mRenderVideoThr != null ) {
				mRenderVideoThr.interrupt();
				try {
					LogUtils.info("before mRenderVideoThr join");
					mRenderVideoThr.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (mReadSampleThr != null ) {
				mReadSampleThr.interrupt();
				try {
					LogUtils.info("before mReadSampleThr join");
					mReadSampleThr.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			
		LogUtils.info("before workder thread join done!");
		
		stayAwake(false);
		
		mMediaEventHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
		        LogUtils.info("Java: removeAllEvents");
				
				removeAllEvents();
				
				mLock.lock();
				setState(PlayState.STOPPED);
				notStopped.signal();
				mLock.unlock();
			}
		});
		
		mLock.lock();
		try {
			while (getState() != PlayState.STOPPED) {
				notStopped.await(1, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			LogUtils.error("InterruptedException", e);
		} finally {
			mLock.unlock();
		}
	}

	@Override
	public void pause() {
	    LogUtils.debug("pause");
		
		if (getState() == PlayState.PAUSED) {
		    LogUtils.warn("Already paused...");
			return;
		}
		
		stayAwake(false);
		
		setState(PlayState.PAUSED);
		pause_l();
	}
	
	private void pause_l() {
		removeAllEvents();
		postBufferingUpdateCheckEvent();
	}
	
	public boolean suspend() {
		Log.i(TAG, "suspend()");
		return true;
	}

	public boolean resume() {
		Log.i(TAG, "resume()");
		return true;
	}
	
	private void removeAllEvents() {
		mMediaEventHandler.removeCallbacksAndMessages(null);
		mEventHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void seekTo(int seekingTime /* milliseconds */) throws IllegalStateException {
	    LogUtils.info("seekTo:" + seekingTime);
		
		PlayState state = getState();
		if (state != PlayState.STARTED && 
			state != PlayState.PAUSED && 
			state != PlayState.PREPARED) {
			
		    LogUtils.error("SeekTo Exception!!!");
			throw new IllegalStateException("Error State: " + state);
		}
		
		mSeekingTimeMsec = (long)seekingTime;
		mAudioPositionMsec = mSeekingTimeMsec;
		mSeeking = true;
		
		Message msg = mMediaEventHandler.obtainMessage(EVENT_SEEKTO);
		mMediaEventHandler.sendMessageAtFrontOfQueue(msg);
	}
	
	private void onSeekToEvent() {
	    LogUtils.debug("onSeekToEvent()");
	    
		mExtractor.seekTo(mSeekingTimeMsec * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
		
		ResetStatics();
		
		postSeekCompletionEvent();
		
		mSeeking = false;
	}
	
	private void postSeekCompletionEvent() {
	    LogUtils.debug("postSeekCompletionEvent()");
		mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_SEEK_COMPLETE);
	}

	@Override
	public void release() {
		mLock.lock();
		LogUtils.info("release()");
		
		stayAwake(false);
		
		release_l();
		mLock.unlock();
	}
	
	private void release_l() {
	    LogUtils.info("release_l()");
		
		mMediaEventHandler.postAtFrontOfQueue(new Runnable() {
			
			@Override
			public void run() {
				LogUtils.info("before removeAllEvents()");
				removeAllEvents();
				
				try {
					LogUtils.info("before release audio codec");
					// release Audio Codec
					if (mAudioCodec != null) {
						mAudioCodec.flush();
						mAudioCodec.stop();
						mAudioCodec.release();
						mAudioCodec = null;
					}
					
					LogUtils.info("before release video codec");
					// release Video Codec
					if (mVideoCodec != null) {
						mVideoCodec.flush();
						mVideoCodec.stop();
						mVideoCodec.release();
						mVideoCodec = null;
					}
				}
				catch (IllegalStateException e) {
					e.printStackTrace();
					LogUtils.error("close codec exception " + e.toString());
				}
				
				// release AudioTrack
				LogUtils.info("before release audio track");
				if (mAudioTrack != null) {
					mAudioTrack.flush();
					mAudioTrack.stop();
					mAudioTrack.release();
					mAudioTrack = null;
				}
				
				// release MediaExtractor
				LogUtils.info("before release extractor");
				if (mExtractor != null) {
					mExtractor.release();
					mExtractor = null;
				}
				
				setState(PlayState.END);
			}
		});
	}
	
	@Override
	public final void reset() {
		
	}

	@Override
	public int getVideoWidth() {
		return mVideoWidth;
	}

	@Override
	public int getVideoHeight() {
		return mVideoHeight;
	}

	@Override
	public int getCurrentPosition() {
		long currentPosition = 0L; // msec
		
		if (!mSeeking) {
			long playedDiffMsec = 0; // time gap between last start position and current time
	        
			if (mAudioStartMsec == 0) { // after "seek" and "start" getNowMs()
	        	playedDiffMsec = 0;
	        }
	        else {
				// because audio timestamp is not continous, so should guess current position(msec)
	        	playedDiffMsec = System.currentTimeMillis() - mAudioStartMsec;
	            if (playedDiffMsec < 0)
	            	playedDiffMsec = 0;
	            else if (mAveAudioPktMsec > 0 && playedDiffMsec > mAveAudioPktMsec) {
					// eliminate audio buffer jitter，
	            	// still got stable audio clock when audio frame wasn't decoded out at common pace
	            	playedDiffMsec = mAveAudioPktMsec;
				}
	        }
	        
			LogUtils.info(String.format("calc_current_pos: pos %d, lat %d, diff %d msec",
					mAudioPositionMsec, mAudioLatencyMsec, playedDiffMsec));
	        // |----diff----pts################play->audio_hardware
			currentPosition = mAudioPositionMsec - mAudioLatencyMsec + playedDiffMsec; 
			if (currentPosition < 0)
				currentPosition = 0;
		} else {
			currentPosition = mSeekingTimeMsec;
		}
		
		mCurrentTimeMsec = currentPosition;
		return (int)mCurrentTimeMsec;
	}

	@Override
	public int getDuration() {
		return (int)(Utils.convertTime(
				mDurationUsec /* srcDuration */,
				TimeUnit.MICROSECONDS /* from */,
				TimeUnit.MILLISECONDS /* to */)
				/* microseconds to milliseconds */);
	}
	
	@Override
	public int getBufferingTime() {
	    if (null != mExtractor) {
	        return (int)(Utils.convertTime(mExtractor.getCachedDuration(), TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS));
	    }
	    
	    return 0;
	}

	@Override
	public int flags() throws IllegalStateException {
		
		if (UrlUtil.isLivePlayUrl(mUrl)) { 
			return 0;
		} else {
			return MediaPlayer.CAN_PAUSE | MediaPlayer.CAN_SEEK | 
					MediaPlayer.CAN_SEEK_BACKWARD | MediaPlayer.CAN_SEEK_FORWARD;
		}
	}
	
	@Override
	public DecodeMode getDecodeMode() {
		return DecodeMode.HW_XOPLAYER;
	}
	
    public void selectAudioChannel(int index)
    {
        // TODO Auto-generated method stub
    	ResetStatics();
    	// todo
    	mExtractor.unselectTrack(mAudioTrackIndex);
    	mExtractor.selectTrack(index);
    }

	@Override
	public void setOption(String option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Bitmap getSnapShot(int width, int height, int fmt, int msec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return (PlayState.STARTED == getState());
	}

	@Override
	public boolean isLooping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLooping(boolean looping) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAudioStreamType(int streamType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSubtitleParser(SimpleSubTitleParser parser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TrackInfo[] getTrackInfo() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTimedTextSource(String path, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTimedTextSource(Context context, Uri uri, String mimeType)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectTrack(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deselectTrack(int index) {
		// TODO Auto-generated method stub
		
	}

}
