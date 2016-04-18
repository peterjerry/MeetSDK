package com.gotye.meetsdk.player;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.gotye.meetsdk.player.MediaPlayer.DecodeMode;
import com.gotye.meetsdk.subtitle.SimpleSubTitleParser;
import com.gotye.meetsdk.util.LogUtils;
import com.gotye.meetsdk.util.UrlUtil;
import com.gotye.meetsdk.util.Utils;
import android.view.Surface;
import android.view.SurfaceHolder;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class XOMediaPlayer extends BaseMediaPlayer {
    private final static boolean USE_READ_SAMPLE_THREAD = false;

	protected final static long TIMEOUT = 5000;// timeoutUs

	private static final int EVENT_SEEKTO = 1003;
	private static final int EVENT_BUFFERING_UPDATE_CHECK = 1004;

	private static final int AV_SYNC_THRESHOLD_MSEC = 100; // 100 msec
	private static final int AV_NOSYNC_THRESHOLD = 10000; // 10 sec

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
    private String mSubtitleMime = null;

	private int mAudioTrackIndex    = -1;
	private int mVideoTrackIndex    = -1;
    private int mSubtitleTrackIndex = -1;

	private boolean mHaveAudio      = false;
	private boolean mHaveVideo      = false;
    private boolean mHaveSubtitle   = false;

	private Lock mAudioCodecLock = null;
	private MediaCodec mAudioCodec = null;
	private Lock mVideoCodecLock = null;
	private MediaCodec mVideoCodec = null;

    private Lock mPacketLock = null;

	private byte[] mAudioData = null;
	private ByteBuffer mAudioCacheBuffer = null;

	// common
	private long mDurationUsec = 0L; // getduration()
	private long mSeekingTimeMsec = 0L; // for seek
	private long mCurrentTimeMsec = 0L; // getcurrentpos()
	private long mTotalStartMsec = 0L; // based on system clock
	private boolean mSawInputEOS = false;
	private boolean mLooping = false;
	private Lock mPlayLock;
	private Condition mPlayCond;
	private Lock mSeekLock;
	private Condition mSeekCond;
	// video
	private boolean mVideoFirstFrame;
	private Lock mVideoListLock;
	private Condition mVideoNotFullCond;
	private Condition mVideoNotEmptyCond;
	private List<PacketBuf> mVideoPktList;
	private long mLastOnVideoTimeMsec = 0L;
	private long mRenderedFrameCnt = 0L;
	private long mDecodedFrameCnt = 0L;
	private long mLastDelayMsec = 0L;
	private long mLastVideoFrameMsec = 0L;
	private long mFrameTimerMsec = 0L;
	// audio
	private Lock mAudioListLock;
	private Condition mAudioNotFullCond;
	private Condition mAudioNotEmptyCond;
	private List<PacketBuf> mAudioPktList;
	private long mAudioStartMsec = 0L;
	private long mAudioPositionMsec = 0L;
	private long mAveAudioPktMsec = 0L;
	private long mLastAudioPktMSec = 0L;
	private long mAudioLatencyMsec = 0L;
	private int mAudioDataLen = 0;
    // subtitle
    private SimpleSubTitleParser mSubParser;

	private final static boolean NO_AUDIO = false;

	private Handler mMediaEventHandler = null;

	private boolean mSeeking = false;

	private Lock mLock = new ReentrantLock();

	private Thread mPrepareThr;
	private Thread mReadSampleThr;
	private Thread mRenderVideoThr;
	private Thread mRenderAudioThr;

	class PacketBuf {
		private int track_index;
		private long presentationTimeUs;
		private int buf_index;
		private int buf_size;
		private int flags;
	};

	public XOMediaPlayer(MediaPlayer mp) {
		super(mp);

		mPlayLock = new ReentrantLock();
		mPlayCond = mPlayLock.newCondition();

		mSeekLock = new ReentrantLock();
		mSeekCond = mSeekLock.newCondition();

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
		XOMediaPlayer mp = (XOMediaPlayer) ((WeakReference<?>) mediaplayer_ref)
				.get();
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
				UrlUtil.isUseSystemExtractor(mUrl) 
				? new SystemMediaExtractor()
				: new FFMediaExtractor(new WeakReference<XOMediaPlayer>(this));

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
			// local file
			setDataSource(uri.getPath());
		} else {
			// network path
			setDataSource(uri.toString());
		}
	}

	@Override
	public void setDataSource(FileDescriptor fd) throws IOException,
			IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("not implement");
	}

	@Override
	public void setDataSource(FileDescriptor fd, long offset, long length)
			throws IOException, IllegalArgumentException, IllegalStateException {
		// intentionally less than LONG_MAX
		throw new IllegalStateException("not implement");
	}

	private void setDataSource(MediaExtractable extractor)
			throws IllegalStateException {
		LogUtils.info("setDataSource extractor");
		mExtractor = extractor;
		setState(PlayState.INITIALIZED);
	}

	private void postSetVideoSizeEvent(int width, int height) {
		LogUtils.info("postSetVideoSizeEvent: width: " + width + ", height: "
				+ height);

		mVideoWidth = width;
		mVideoHeight = height;

		Message msg = mEventHandler.obtainMessage(
				MediaPlayer.MEDIA_SET_VIDEO_SIZE, width, height);
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

	@Override
	public void setSurface(Surface surface) {
		mSurface = surface;
	}

	@Override
	public void prepare() throws IOException, IllegalStateException {
		//throw new IllegalStateException("Do not support this operation.");
        prepare_proc();
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		PlayState state = getState();
		if (state != PlayState.INITIALIZED) {
			LogUtils.error("Error State: " + state);
			throw new IllegalStateException("Error State: " + state);
		}

		setState(PlayState.PREPARING);

		if (mPrepareThr != null) {
			throw new IllegalStateException("mPrepareThr is running");
		}

		mPrepareThr = new Thread(new Runnable() {
			@Override
			public void run() {
				prepare_proc();
			}
		});
		mPrepareThr.start();
	}

	private boolean prepare_proc() {
		mLock.lock();

		boolean ret;

		// would block
		ret = initMediaExtractor();
		if (!ret) {
			// omit stop when preparing
			if (getState() != PlayState.STOPPING) {
				Message msg = mEventHandler
						.obtainMessage(MediaPlayer.MEDIA_ERROR);
				msg.arg1 = MediaPlayer.MEDIA_ERROR_FAIL_TO_OPEN;
				msg.sendToTarget();
			}

			mLock.unlock();
			return false;
		}

		ret = initAudioTrack();
		if (!ret) {
			Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
			msg.arg1 = MediaPlayer.MEDIA_ERROR_AUDIO_RENDER;
			msg.sendToTarget();
			mLock.unlock();
			return false;
		}

		ret = initAudioDecoder();
		if (!ret) {
			Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
			msg.arg1 = MediaPlayer.MEDIA_ERROR_AUDIO_DECODER;
			msg.sendToTarget();
			mLock.unlock();
			return false;
		}

		ret = initVideoDecoder();
		if (!ret) {
			Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
			msg.arg1 = MediaPlayer.MEDIA_ERROR_VIDEO_DECODER;
			msg.sendToTarget();
			mLock.unlock();
			return false;
		}

        mPacketLock = new ReentrantLock();

		setState(PlayState.PREPARED);

		Message msg = mEventHandler.obtainMessage(MediaPlayer.MEDIA_INFO);
		msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE; // extra
		msg.arg2 = MediaPlayer.PLAYER_IMPL_TYPE_XO_PLAYER;
		msg.sendToTarget();

		mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_PREPARED);

		mLock.unlock();

		return true;
	}

	private boolean initMediaExtractor() {
		LogUtils.info("start initMediaExtractor");
        if (mSubParser != null)
            mExtractor.setSubtitleParser(mSubParser);
		try {
			// would block
			mExtractor.setDataSource(mUrl);
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.error("setDataSource() IOException", e);
			return false;
		}

		int trackCount = mExtractor.getTrackCount();
		LogUtils.info("Java: trackCount: " + trackCount);
		for (int index = 0; index < trackCount; index++) {
			LogUtils.info("Java: MediaFormat get # " + index + " trackinfo");
			MediaFormat format = mExtractor.getTrackFormat(index);
			if (format == null) {
				LogUtils.error("fail to get track format");
				return false;
			}
			
			LogUtils.info("Java: MediaFormat: " + format.toString());

			if (format.containsKey("csd-0")) {
				ByteBuffer bb = format.getByteBuffer("csd-0");
				LogUtils.info("Java: MediaFormat csd-0(a): " + bb.toString());
				byte[] content = new byte[bb.limit()];
				bb.get(content);
				bb.rewind();
				LogUtils.info("Java: MediaFormat csd-0(b): "
						+ content.toString());
				StringBuffer sbHex = new StringBuffer();
				StringBuffer sbInt = new StringBuffer();
				for (int j = 0; j < content.length; j++) {
					sbHex.append(String.format("0x%02x ", content[j]));
					sbInt.append(String.format("%d, ", content[j]));
				}
				LogUtils.info("Java: MediaFormat csd-0(c): hex: "
						+ sbHex.toString());
			}
			if (format.containsKey("csd-1")) {
				ByteBuffer bb = format.getByteBuffer("csd-1");
				LogUtils.info("Java: MediaFormat csd-1(a): " + bb.toString());
				byte[] content = new byte[bb.limit()];
				bb.get(content);
				bb.rewind();
				LogUtils.info("Java: MediaFormat csd-1(b): "
						+ content.toString());
				StringBuffer sbHex = new StringBuffer();
				StringBuffer sbInt = new StringBuffer();
				for (int j = 0; j < content.length; j++) {
					sbHex.append(String.format("0x%02x ", content[j]));
					sbInt.append(String.format("%d, ", content[j]));
				}
				LogUtils.info("Java: MediaFormat csd-1(c): hex: "
						+ sbHex.toString());
			}
			if (format.containsKey("csd-2")) {
				ByteBuffer bb = format.getByteBuffer("csd-2");
				LogUtils.info("Java: MediaFormat csd-2(a): " + bb.toString());
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
			} else if (mime.startsWith("subtitle/")) {
                mSubtitleMime = mime;
                mSubtitleTrackIndex = index;
                LogUtils.info("Java: mSubtitleTrackIndex: " + mSubtitleTrackIndex);
                mHaveSubtitle = true;
            } else {
				// unknown media type;
				LogUtils.warn("Java: unknown media type");
			}

			if (mHaveAudio && mHaveVideo && mHaveSubtitle) {
                // enough stream info
				break;
			}
		}

		if (!mHaveAudio || !mHaveVideo) {
			LogUtils.error("Java: ONLY support play media which has both video and audio stream");
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

		int channelCount = mAudioFormat
				.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
		if (channelCount < 1 || channelCount > 2) {
			LogUtils.error("audio track NOT support channelCount: "
					+ channelCount);
			return false;
		}

		int channelConfig = (channelCount == 1) ? AudioFormat.CHANNEL_OUT_MONO
				: AudioFormat.CHANNEL_OUT_STEREO;

		int sampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		// fixme!!! AAC LC
		if (mExtractor.isSystemExtractor() == true && sampleRate == 22050) {
			LogUtils.warn("force double sample rate: " + sampleRate);
			sampleRate *= 2;
		}
		
		int minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig,
				AudioFormat.ENCODING_PCM_16BIT);

		mAudioLatencyMsec = minSize * 1000
				/ (sampleRate * channelCount * 2/* s16 */);
		mAudioDataLen = sampleRate * channelCount * 2/* s16 */;
		
		LogUtils.info(String
				.format("Java: audio format: channels %d, channel_cfg %d, sample_rate %d, minbufsize %d, latency %d",
						channelCount, channelConfig, sampleRate, minSize,
						mAudioLatencyMsec));

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				channelConfig, AudioFormat.ENCODING_PCM_16BIT, minSize,
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

		mAudioTrack
				.setPlaybackPositionUpdateListener(mAudioPositionUpdateListener);

		if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
			LogUtils.error("failed to new AudioTrack: "
					+ mAudioTrack.getState());
			return false;
		}

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

			mAudioCodec.configure(mAudioFormat, null /* surface */,
					null /* crypto */, 0 /* flags */);
			mAudioCodec.start();

			mExtractor.selectTrack(mAudioTrackIndex);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtils.error("Exception", e);

			mAudioCodec = null;
			ret = false;
		}

		mAudioCodecLock = new ReentrantLock();

		mAudioListLock = new ReentrantLock();
		mAudioNotFullCond = mAudioListLock.newCondition();
		mAudioNotEmptyCond = mAudioListLock.newCondition();
		mAudioPktList = new ArrayList<PacketBuf>();

		LogUtils.info("Init Audio Decoder Success!!!");
		return ret;
	}

	private boolean initVideoDecoder() {
		LogUtils.info("start to initVideoDecoder");

		boolean ret = true;

		String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
		try {
			LogUtils.info("video mime: " + mime);
			mVideoCodec = MediaCodec.createDecoderByType(mime);

			mVideoCodec.configure(mVideoFormat, mSurface /* surface */,
					null /* crypto */, 0 /* flags */);
			mVideoCodec.start();

			mExtractor.selectTrack(mVideoTrackIndex);

			LogUtils.info(String.format(
					"Video Decoder inputBuf count: %d, outputBuf count: %d",
					mVideoCodec.getInputBuffers().length,
					mVideoCodec.getOutputBuffers().length));

		} catch (Exception e) {
			e.printStackTrace();
			LogUtils.error("Exception", e);

			mVideoCodec = null;
			ret = false;
		}

		mVideoCodecLock = new ReentrantLock();

		mVideoListLock = new ReentrantLock();
		mVideoNotFullCond = mVideoListLock.newCondition();
		mVideoNotEmptyCond = mVideoListLock.newCondition();
		mVideoPktList = new ArrayList<PacketBuf>();

		LogUtils.info("Init Video Decoder Success!!!");

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

		if (USE_READ_SAMPLE_THREAD && mReadSampleThr == null) {
			mReadSampleThr = new Thread(new Runnable() {
				@Override
				public void run() {
					read_sample_proc();
				}
			});
			mReadSampleThr.start();
		}

		if (mRenderVideoThr == null) {
			mRenderVideoThr = new Thread(new Runnable() {
				@Override
				public void run() {
					video_proc();
				}
			});
			mRenderVideoThr.start();
		}

		if (mRenderAudioThr == null) {
			mRenderAudioThr = new Thread(new Runnable() {
				@Override
				public void run() {
					audio_proc();
				}
			});
			mRenderAudioThr.start();
		}

		mAudioTrack.play();
		mAudioStartMsec = mSeekingTimeMsec;

		mPlayLock.lock();
		mPlayCond.signalAll();
		mPlayLock.unlock();

		postBufferingUpdateCheckEvent();
	}

	private long get_audio_clock() {
		long audio_clock_msec;

		long playedDiffMsec = 0; // time gap between last start position and
									// current time

		if (mAudioStartMsec == 0) { // after "seek" and "start" getNowMs()
			playedDiffMsec = 0;
		} else {
			// because audio timestamp is not continous, so should guess current
			// position(msec)
			playedDiffMsec = System.currentTimeMillis() - mAudioStartMsec;
			if (playedDiffMsec < 0)
				playedDiffMsec = 0;
			else if (mAveAudioPktMsec > 0 && playedDiffMsec > mAveAudioPktMsec) {
				// eliminate audio buffer jitter，
				// still got stable audio clock when audio frame wasn't decoded
				// out at common pace
				playedDiffMsec = mAveAudioPktMsec;
			}
		}

		//LogUtils.debug(String.format(
		//		"audio_clock: pos %d, lat %d, diff %d msec",
		//		mAudioPositionMsec, mAudioLatencyMsec, playedDiffMsec));
		// |----diff----pts################play->audio_hardware
		audio_clock_msec = mAudioPositionMsec - mAudioLatencyMsec
				+ playedDiffMsec;

		if (audio_clock_msec < 0)
			audio_clock_msec = 0;
		/*
		 * LogUtils.info(String.format("aaaa %d %d %d %d", audio_clock_msec,
		 * mAudioStartMsec, mAudioTrack.getPlaybackHeadPosition(),
		 * mAudioTrack.getPlaybackHeadPosition() * 1000 /
		 * mAudioTrack.getSampleRate())); return mAudioStartMsec +
		 * mAudioTrack.getPlaybackHeadPosition() * 1000 /
		 * mAudioTrack.getSampleRate();
		 */
		return audio_clock_msec;
	}

	private void read_sample_proc() {
		LogUtils.info("read sample thread started");

		boolean sawInputEOS = false;

		while (getState() != PlayState.STOPPING) {
			if (getState() == PlayState.PAUSED) {
				try {
					mPlayLock.lock();
					mPlayCond.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					mPlayLock.unlock();
				}

				if (getState() == PlayState.PAUSED) {
					LogUtils.info("receive exit signal when paused");
					break;
				}
			}

			if (sawInputEOS) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				continue;
			}

			MediaCodec codec = null;
			int trackIndex = mExtractor.getSampleTrackIndex();
			if (trackIndex < 0) {
				LogUtils.info("Java: getSampleTrackIndex saw InputEOS");
				sawInputEOS = true;
				codec = mVideoCodec;
			}
			else {
				codec = getCodec(trackIndex);
				if (codec == null) {
					LogUtils.error("failed to get mediacodec: trackIndex "
							+ trackIndex);
					break;
				}
			}

			int inputBufIndex = -1;
			try {
				inputBufIndex = codec.dequeueInputBuffer(TIMEOUT);
			} catch (IllegalStateException e) {
				e.printStackTrace();
				LogUtils.warn("codec dequeueInputBuffer exception: "
						+ e.getMessage());

				/*
				 * Message msg =
				 * mEventHandler.obtainMessage(MediaPlayer.MEDIA_ERROR);
				 * msg.arg1 = MediaPlayer.MEDIA_ERROR_DEMUXER; msg.arg2 = 0;
				 * msg.sendToTarget(); break;
				 */
			}

			if (inputBufIndex >= 0) {
				if (trackIndex < 0) {
					// add eof buf
					LogUtils.info("Java: add eof buf");
					PacketBuf eof_buf = new PacketBuf();

					eof_buf.track_index			= mVideoTrackIndex;
					eof_buf.buf_index 			= inputBufIndex;	
					eof_buf.buf_size 			= -1;
					eof_buf.presentationTimeUs	= 0;
					eof_buf.flags				= 0;

					mVideoListLock.lock();
					mVideoPktList.add(eof_buf);
					mVideoNotEmptyCond.signal();
					mVideoListLock.unlock();
					continue;
				}
				
				if (trackIndex == mVideoTrackIndex)
					mVideoCodecLock.lock();
				else
					mAudioCodecLock.lock();
				ByteBuffer dstBuf = codec.getInputBuffers()[inputBufIndex];
				int sampleSize = mExtractor
						.readSampleData(dstBuf, 0 /* offset */);
				long presentationTimeUs = 0;
				if (sampleSize < 0) {
					// mExtractor.hasCachedReachedEndOfStream()
					LogUtils.info("Java: saw input EOS.");
					sawInputEOS = true;
				} else {
					presentationTimeUs = mExtractor.getSampleTime();
					sawInputEOS = false;
				}

				boolean is_flush_pkt = false;
				if (sampleSize > 0 /* 5 */) {
					String str_flush = "FLUSH";
					byte[] byte_ctx = new byte[5];
					dstBuf.get(byte_ctx);
					dstBuf.rewind();
					String strPkt = new String(byte_ctx);
					if (str_flush.equals(strPkt)) {
						is_flush_pkt = true;
						LogUtils.error("Java: found flush pkt");

						if (trackIndex == mVideoTrackIndex) {
							LogUtils.error("Java: flush video");

							mVideoListLock.lock();
							mVideoPktList.clear();
							mVideoListLock.unlock();

							ResetStatics();
							mCurrentTimeMsec = mSeekingTimeMsec;
							
							mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_SEEK_COMPLETE);
						} else {
							LogUtils.error("Java: flush audio");

							mAudioListLock.lock();
							mAudioPktList.clear();
							if (mAudioCacheBuffer != null)
								mAudioCacheBuffer.clear();
							mAudioListLock.unlock();

							mAudioTrack.pause();
							mAudioTrack.flush();
							mAudioTrack.play();
							mAudioStartMsec = mSeekingTimeMsec;
						}

						codec.flush();
					}
				} // end of flush pkt

				if (!is_flush_pkt) {
					// normal packet(except flush pkt, including eos pkt)
					/*byte[] byte_ctx = new byte[5];
					dstBuf.get(byte_ctx);
					dstBuf.rewind();
					LogUtils.info(String.format("Java: readsample [%s] size %d, %02x %02x %02x %02x %02x",
							trackIndex == mVideoTrackIndex ? "video" : "audio",
							sampleSize,
							byte_ctx[0], byte_ctx[1], byte_ctx[2], byte_ctx[3], byte_ctx[4]));*/
					
					int flags = mExtractor.getSampleFlags();

					PacketBuf buf = new PacketBuf();

					buf.track_index = trackIndex;
					buf.buf_index = inputBufIndex;
					buf.buf_size = sampleSize;
					buf.presentationTimeUs = presentationTimeUs;
					buf.flags = flags;

					if (mVideoTrackIndex == trackIndex) {
						mVideoListLock.lock();
						mVideoPktList.add(buf);
						mVideoNotEmptyCond.signal();
						mVideoListLock.unlock();
					} else {
						mAudioListLock.lock();
						mAudioPktList.add(buf);
						mAudioNotEmptyCond.signal();
						mAudioListLock.unlock();
					}
				}

				if (trackIndex == mVideoTrackIndex)
					mVideoCodecLock.unlock();
				else
					mAudioCodecLock.unlock();

				if (!sawInputEOS) {
					mExtractor.advance();
				}
			} // end of inputBufIndex >= 0
		} // end of while

		LogUtils.info("read sample thread exited");
	}

	// return false: not packet added
	private boolean queue_packet(boolean isVideo) {
		if (mSawInputEOS)
			return true;

		PacketBuf buf = null;

		List<PacketBuf> list = null;
		Lock lock = null;
		Condition notFullCond = null;
		Condition notEmptyCond = null;
		MediaCodec codec = null;

		if (isVideo) {
			codec = mVideoCodec;
			list = mVideoPktList;
			lock = mVideoListLock;
			notFullCond = mVideoNotFullCond;
			notEmptyCond = mVideoNotEmptyCond;
		} else {
			codec = mAudioCodec;
			list = mAudioPktList;
			lock = mAudioListLock;
			notFullCond = mAudioNotFullCond;
			notEmptyCond = mAudioNotEmptyCond;
		}

		try {
			lock.lock();

			while (list.size() == 0 && getState() != PlayState.STOPPING)
				notEmptyCond.await(); // would block

			if (list.size() == 0) {
				LogUtils.info(String.format("Java: %s list is empty",
						isVideo ? "video" : "audio"));
				return false;
			}

			buf = list.get(0);

			if (buf == null) {
				LogUtils.info(String.format("Java: %s buf is null",
						isVideo ? "video" : "audio"));
				return false;
			}

			int trackIndex = buf.track_index;
			int inputBufIndex = buf.buf_index;
			int sampleSize = buf.buf_size;
			long presentationTimeUs = buf.presentationTimeUs;
			int flags = buf.flags;

			if (sampleSize < 0) {
				mSawInputEOS = true;
				sampleSize = 0;
				LogUtils.error("saw video Input EOS.");
			}

			codec.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize,
					presentationTimeUs,
					mSawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

			//LogUtils.debug(String
			//		.format("queueInputBuffer track #%d(%s): size %d, pts %d msec, flags %d",
			//				trackIndex, isVideo ? "video" : "audio",
			//				sampleSize, presentationTimeUs / 1000, flags));

			list.remove(0);
			notFullCond.signal();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtils.info(String.format(
					"Java: %s NotEmptyCond.await InterruptedException",
					isVideo ? "video" : "audio"));
		} finally {
			lock.unlock();
		}

		return true;
	}

	private void video_proc() {
		LogUtils.info("video thread started");

		int noOutputCounter = 0;
        boolean sawOutputEOS = false;
		int videoAheadMax = 0;
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		while (getState() != PlayState.STOPPING) {
			if (getState() == PlayState.PAUSED) {
				try {
					mPlayLock.lock();
					mPlayCond.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mPlayLock.unlock();
				}

				if (getState() == PlayState.PAUSED) {
					LogUtils.info("receive exit signal when paused");
					break;
				}
			}

            if (sawOutputEOS) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                continue;
            }

			noOutputCounter++;

            if (USE_READ_SAMPLE_THREAD) {
                if (!queue_packet(true))
                    continue;
            }
            else {
                int inputBufIndex = -1;
                try {
                    inputBufIndex = mVideoCodec.dequeueInputBuffer(TIMEOUT);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    LogUtils.warn("codec dequeueInputBuffer exception: "
                            + e.getMessage());
                }

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = mVideoCodec.getInputBuffers()[inputBufIndex];
                    mPacketLock.lock();
                    int sampleSize = mExtractor.readPacket(mVideoTrackIndex, dstBuf, 0);
                    if (sampleSize < 0) {
                        mSawInputEOS = true;
                        sampleSize = 0;
                        LogUtils.error("saw video Input EOS.");
                    }

                    boolean is_flush_pkt = false;
                    if (sampleSize > 0 /* 5 */) {
                        String str_flush = "FLUSH";
                        byte[] byte_ctx = new byte[5];
                        dstBuf.get(byte_ctx);
                        dstBuf.rewind();
                        String strPkt = new String(byte_ctx);
                        if (str_flush.equals(strPkt)) {
                            is_flush_pkt = true;
                            LogUtils.error("Java: flush video");

                            ResetStatics();
                            mCurrentTimeMsec = mSeekingTimeMsec;

                            mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_SEEK_COMPLETE);

                            mVideoCodec.flush();
                        }
                    } // end of flush pkt

                    if (!is_flush_pkt) {
                        long presentationTimeUs = mExtractor.getSampleTime();
                        mVideoCodec.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize,
                                presentationTimeUs,
                                mSawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    }
                    mPacketLock.unlock();
                }
            }

			if (noOutputCounter >= 50) {
				LogUtils.warn("output eos not found after 50 frames");
				break;
			}

			int res;
            Message msg;
			try {
				mVideoCodecLock.lock();
				//LogUtils.debug("before video dequeueOutputBuffer");
				res = mVideoCodec.dequeueOutputBuffer(info, TIMEOUT);

				if (res >= 0) {
					// 20151221 guoliang.ma fixed
					// some device info.size is always 0
					// will cause play "stucked" after several seconds
					noOutputCounter = 0;
					//if (info.size > 0) {
						//noOutputCounter = 0;
					//}

					int outputBufIndex = res;
					boolean render = true;

					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						LogUtils.info("saw output EOS.");

						if (mLooping) {
                            mVideoCodec.flush();

							mSawInputEOS = false; // notify audio_proc to restart
                            noOutputCounter = 0;
							seekTo(0);
							continue;
						} else {
                            sawOutputEOS = true;
							postPlaybackCompletionEvent();
							break;
						}
					}

					//LogUtils.debug(String
					//		.format("[DecodeVideoBuffer] presentationTimeUs: %d, flags: %d",
					//				info.presentationTimeUs, info.flags));

					mDecodedFrameCnt++;

					long now_msec = System.currentTimeMillis();
					long elapsed_msec = now_msec - mLastOnVideoTimeMsec;
					if (elapsed_msec > 1000) {
						int decode_fps = (int) (mDecodedFrameCnt * 1000 / (now_msec - mTotalStartMsec));
						msg = mEventHandler
								.obtainMessage(MediaPlayer.MEDIA_INFO);
						msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_DECODE_FPS;
						msg.arg2 = (int) decode_fps;
						msg.sendToTarget();

						int render_fps = (int) (mRenderedFrameCnt * 1000 / (now_msec - mTotalStartMsec));
						Message msg2 = mEventHandler
								.obtainMessage(MediaPlayer.MEDIA_INFO);
						msg2.arg1 = MediaPlayer.MEDIA_INFO_TEST_RENDER_FPS;
						msg2.arg2 = (int) render_fps;
						msg2.sendToTarget();

						mLastOnVideoTimeMsec = now_msec;
					}

					if (!mVideoFirstFrame) {
						LogUtils.info("Java: first video frame out");
						mTotalStartMsec = System.currentTimeMillis();
						mFrameTimerMsec = System.currentTimeMillis();
						mVideoFirstFrame = true;
						mSeeking = false;
					}

					long video_clock_msec = info.presentationTimeUs / 1000;

					long delay_msec = video_clock_msec - mLastVideoFrameMsec/*framerate 40msec*/;
					if (delay_msec < 0 || delay_msec > 1000) {
						// fix invalid pts
						delay_msec = mLastDelayMsec;
					}

					mLastVideoFrameMsec = video_clock_msec;
					mCurrentTimeMsec = video_clock_msec; // fix me!!!
					mLastDelayMsec = delay_msec;

					long audio_clock_msec = get_audio_clock();
					long av_diff_msec = video_clock_msec - audio_clock_msec;
					if (NO_AUDIO)
						av_diff_msec = 0;
					//LogUtils.info(String.format(Locale.US,
					//		"video %d, audio %d, diff_msec %d msec",
					//		video_clock_msec, audio_clock_msec, av_diff_msec));

                    if (mDecodedFrameCnt % 10 == 0) {
                        msg = mEventHandler
                                .obtainMessage(MediaPlayer.MEDIA_INFO);
                        msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_LATENCY_MSEC;
                        msg.arg2 = (int) av_diff_msec;
                        msg.sendToTarget();
                    }

					long sync_threshold_msec = (delay_msec > AV_SYNC_THRESHOLD_MSEC) ? delay_msec
							: AV_SYNC_THRESHOLD_MSEC;
					if (av_diff_msec < AV_NOSYNC_THRESHOLD
							&& av_diff_msec > -AV_NOSYNC_THRESHOLD) {
						if (av_diff_msec <= -sync_threshold_msec) {
							delay_msec = 0;
						} else if (av_diff_msec >= sync_threshold_msec
								&& av_diff_msec <= (sync_threshold_msec * 2)) {
							delay_msec = 2 * delay_msec;
						} else if (av_diff_msec >= (sync_threshold_msec * 2)) {
							delay_msec = av_diff_msec; // for seek case
						}

                        if (USE_READ_SAMPLE_THREAD && av_diff_msec >= sync_threshold_msec && videoAheadMax > 0) {
                            videoAheadMax -= 50;
                            if (videoAheadMax < 0)
                                videoAheadMax = 0;

                            mExtractor.setVideoAhead(videoAheadMax);
                            LogUtils.error("Java: setVideoAhead(too early) " + videoAheadMax);
                        }
					}

					//LogUtils.debug("delay_msec: " + delay_msec);
					mFrameTimerMsec += delay_msec;

					if (av_diff_msec < -sync_threshold_msec * 2) {
						render = false;
						
						if (USE_READ_SAMPLE_THREAD &&
                                av_diff_msec > -sync_threshold_msec * 4 &&
								av_diff_msec < -videoAheadMax) {
							//videoAheadMax = (int)-av_diff_msec;
                            videoAheadMax = 500;// force set a big value
							mExtractor.setVideoAhead(videoAheadMax);
							LogUtils.error("Java: setVideoAhead(too late) " + videoAheadMax);
						}

                        //LogUtils.info(String.format(Locale.US,
                        //        "drop frame: av_diff %d msec", av_diff_msec));
						msg = mEventHandler
								.obtainMessage(MediaPlayer.MEDIA_INFO);
						msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_DROP_FRAME;
						msg.arg2 = (int) av_diff_msec;
						msg.sendToTarget();
					}

					if (mSeeking)
						render = false;

					// render it!
					mVideoCodec.releaseOutputBuffer(outputBufIndex, render);

					if (render) {
						mRenderedFrameCnt++;

						msg = mEventHandler
								.obtainMessage(MediaPlayer.MEDIA_INFO);
						msg.arg1 = MediaPlayer.MEDIA_INFO_TEST_RENDER_FRAME;
						msg.arg2 = (int) mRenderedFrameCnt;
						msg.sendToTarget();
					}

					long schedule_msec = mFrameTimerMsec
							- System.currentTimeMillis();
					//LogUtils.debug("schedule_msec: " + schedule_msec);

					if (schedule_msec >= 10 && !NO_AUDIO) {
						try {
							Thread.sleep(schedule_msec);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					// codecOutputBuffers = codec.getOutputBuffers();
					LogUtils.info("output buffers have changed.");
				} else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat oformat = mVideoCodec.getOutputFormat();
					LogUtils.info("output format has changed to " + oformat);
				} else {
					LogUtils.debug("video no output: " + res);
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
				LogUtils.warn("codec dequeueOutputBuffer exception"
						+ e.getMessage());
				msg = mEventHandler
						.obtainMessage(MediaPlayer.MEDIA_ERROR);
				msg.arg1 = MediaPlayer.MEDIA_ERROR_VIDEO_DECODER;
				msg.arg2 = 0;
				msg.sendToTarget();

				break;
			} finally {
				mVideoCodecLock.unlock();
			}
		} // end of while

		LogUtils.info("video thread exited");
	}

	private void audio_proc() {
		LogUtils.info("audio thread started");

		boolean sawOutputEOS = false;
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		while (getState() != PlayState.STOPPING) {
			if (getState() == PlayState.PAUSED) {
				try {
					mPlayLock.lock();
					mPlayCond.await();
				} catch (InterruptedException e) {

				} finally {
					mPlayLock.unlock();
				}

				if (getState() == PlayState.PAUSED) {
					LogUtils.info("receive exit signal when paused");
					break;
				}
			}

            if (sawOutputEOS) {
                if (mSawInputEOS) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    continue;
                }
                else {
                    LogUtils.info("receive seek back to 0 signal");
                    sawOutputEOS = false;
                }
            }

            if (USE_READ_SAMPLE_THREAD) {
                if (!queue_packet(false))
                    continue;
            }
            else {
                int inputBufIndex = -1;
                try {
                    inputBufIndex = mAudioCodec.dequeueInputBuffer(TIMEOUT);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    LogUtils.warn("codec dequeueInputBuffer exception: "
                            + e.getMessage());
                }

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = mAudioCodec.getInputBuffers()[inputBufIndex];
                    mPacketLock.lock();
                    int sampleSize = mExtractor.readPacket(mAudioTrackIndex, dstBuf, 0);
                    if (sampleSize < 0) {
                        mSawInputEOS = true;
                        sampleSize = 0;
                        LogUtils.error("saw audio Input EOS.");
                    }

                    boolean is_flush_pkt = false;
                    if (sampleSize > 0 /* 5 */) {
                        String str_flush = "FLUSH";
                        byte[] byte_ctx = new byte[5];
                        dstBuf.get(byte_ctx);
                        dstBuf.rewind();
                        String strPkt = new String(byte_ctx);
                        if (str_flush.equals(strPkt)) {
                            is_flush_pkt = true;
                            LogUtils.error("Java: flush audio");

                            ResetStatics();
                            mCurrentTimeMsec = mSeekingTimeMsec;

                            mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_SEEK_COMPLETE);

                            mAudioCodec.flush();
                        }
                    } // end of flush pkt

                    if (!is_flush_pkt) {
                        long presentationTimeUs = mExtractor.getSampleTime();
                        mAudioCodec.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize,
                                presentationTimeUs,
                                mSawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    }
                    mPacketLock.unlock();
                }
            }

			int res;
			try {
				mAudioCodecLock.lock();
				res = mAudioCodec.dequeueOutputBuffer(info, TIMEOUT);

				if (res >= 0) {
					int outputBufIndex = res;

					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						LogUtils.info("saw audio output EOS.");
						sawOutputEOS = true;

						if (!mLooping)
							break;

                        mAudioCodec.flush();
                        continue;
					}

					//LogUtils.debug(String
					//		.format("[DecodeAudioBuffer] presentationTimeUs: %d, flags: %d",
					//				info.presentationTimeUs, info.flags));

					// update audio average duration
					if (mLastAudioPktMSec != 0) {
						long pkt_duration = info.presentationTimeUs / 1000
								- mLastAudioPktMSec;
						mAveAudioPktMsec = (mAveAudioPktMsec * 4 + pkt_duration) / 5;
					} else {
						mLastAudioPktMSec = info.presentationTimeUs / 1000;
					}

					if (!NO_AUDIO) {
						ByteBuffer outputBuf = mAudioCodec.getOutputBuffers()[outputBufIndex];

						int bufSize = info.size;
						if (mAudioData == null || mAudioData.length < bufSize) {
							// Allocate a new buffer.
							mAudioData = new byte[bufSize];
						}

						outputBuf.get(mAudioData);

						//if (mVideoFirstFrame) {
							/*if (mAudioCacheBuffer != null && mAudioCacheBuffer.remaining() > 0) {
								mAudioCacheBuffer.flip();
								int size = mAudioCacheBuffer.remaining();
								LogUtils.error("Java: remaining " + size);
								int left = size;
								int written = 0;
								final int chunksize = 4096;
								byte []data = new byte[chunksize];
								while (left > 0) {
									mAudioCacheBuffer.get(data);
									// would block
									int write_size = chunksize;
									if (left < chunksize)
										write_size = left;
									mAudioTrack.write(data, 0, write_size);
									LogUtils.error("write audio data " + write_size);

									left -= write_size;
									written += write_size;
									// update time
									mAudioPositionMsec = mAudioStartMsec + 1000 * written / mAudioDataLen;
								}

								mAudioCacheBuffer.clear();
							}*/

						// would block
						mAudioTrack.write(mAudioData, 0, bufSize);

						// update audio clock
						mAudioStartMsec = System.currentTimeMillis();

						mAudioPositionMsec = info.presentationTimeUs / 1000;
						//}
						/*else {
							if (mAudioCacheBuffer == null) {
								mAudioCacheBuffer = ByteBuffer.allocate(1048576);
							}

							mAudioCacheBuffer.put(mAudioData);
							LogUtils.error("add audio buffer " + info.size);
						}*/
					}

					// only audio output buffer will be released here!
					mAudioCodec.releaseOutputBuffer(outputBufIndex, false);
					// mAudioCodecLock.unlock();
				} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					// codecOutputBuffers = codec.getOutputBuffers();
					LogUtils.info("output buffers have changed.");
				} else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat oformat = mAudioCodec.getOutputFormat();
					LogUtils.info("output format has changed to " + oformat);
				} else {
					LogUtils.info("audio no output: " + res);
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
				LogUtils.error("codec dequeueOutputBuffer exception"
						+ e.getMessage());
				Message msg = mEventHandler
						.obtainMessage(MediaPlayer.MEDIA_ERROR);
				msg.arg1 = MediaPlayer.MEDIA_ERROR_AUDIO_DECODER;
				msg.arg2 = 0;
				msg.sendToTarget();

				break;
			} finally {
				mAudioCodecLock.unlock();
			}
		} // end of while

		LogUtils.info("audio thread exited");
	}

	private void ResetStatics() {
		mDecodedFrameCnt = 0L;
		mRenderedFrameCnt = 0L;
		// mTotalStartMsec = System.currentTimeMillis();
		// mFrameTimerMsec = System.currentTimeMillis();
		mVideoFirstFrame = false;
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

		Message msg = mMediaEventHandler
				.obtainMessage(EVENT_BUFFERING_UPDATE_CHECK);
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

		Message msg = mEventHandler
				.obtainMessage(MediaPlayer.MEDIA_BUFFERING_UPDATE);
		msg.arg1 = (int) pct100;
		msg.sendToTarget();
	}

	private/* synchronized */boolean isBuffering() {
		return mIsBuffering;
	}

	private/* synchronized */void setBufferingStatus(boolean status) {
		mIsBuffering = status;
	}

	private void postPlaybackCompletionEvent() {
		setState(PlayState.PLAYBACK_COMPLETED);
		mEventHandler.sendEmptyMessage(MediaPlayer.MEDIA_PLAYBACK_COMPLETE);
	}

	@Override
	public void stop() {
		LogUtils.info("stop()");

		PlayState state = getState();
		if (PlayState.STOPPED == state) {
			LogUtils.warn("Already stopped");
			return;
		}

		if (PlayState.PAUSED == state) {
			mPlayLock.lock();
			mPlayCond.signal();
			mPlayLock.unlock();
		}

		if (PlayState.PREPARING == state) {
			if (mPrepareThr != null) {
				if (mExtractor != null) {
					mExtractor.stop();
				}

				try {
					LogUtils.info("before mPrepareThr join");
					mPrepareThr.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (state == PlayState.STARTED || state == PlayState.PAUSED
				|| state == PlayState.PLAYBACK_COMPLETED) {
			setState(PlayState.STOPPING);

			mVideoListLock.lock();
			mVideoNotEmptyCond.signal();
			mVideoListLock.unlock();

			if (mRenderVideoThr != null) {
				mRenderVideoThr.interrupt();
				try {
					LogUtils.info("before mRenderVideoThr join");
					mRenderVideoThr.join();
					mRenderVideoThr = null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (mRenderAudioThr != null) {
				mRenderAudioThr.interrupt();
				try {
					LogUtils.info("before mRenderAudioThr join");
					mRenderAudioThr.join();
					mRenderAudioThr = null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// 2015.12.10 fix stop stuck in some case
			if (mExtractor != null) {
				// mExtractor.stop();
			}

			if (mReadSampleThr != null) {
				mReadSampleThr.interrupt();
				try {
					LogUtils.info("before mReadSampleThr join");
					mReadSampleThr.join();
					mReadSampleThr = null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		LogUtils.info("worker thread join done!");

		stayAwake(false);

		LogUtils.info("Java: removeAllEvents");

		removeAllEvents();

		setState(PlayState.STOPPED);
	}

	@Override
	public void pause() {
		LogUtils.debug("pause");

		if (getState() == PlayState.PAUSED) {
			LogUtils.warn("Already paused...");
			return;
		}

		mAudioTrack.pause();

		stayAwake(false);

		setState(PlayState.PAUSED);
		pause_l();
	}

	private void pause_l() {
		removeAllEvents();
		removeBufferingUpdateCheckEvent();
	}

	public boolean suspend() {
		LogUtils.info("suspend()");
		return true;
	}

	public boolean resume() {
		LogUtils.info("resume()");
		return true;
	}

	private void removeAllEvents() {
		mMediaEventHandler.removeCallbacksAndMessages(null);
		mEventHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void seekTo(int seekingTime /* milliseconds */)
			throws IllegalStateException {
		LogUtils.info("seekTo:" + seekingTime);

		PlayState state = getState();
		if (state != PlayState.STARTED && state != PlayState.PAUSED
				&& state != PlayState.PREPARED && state != PlayState.PLAYBACK_COMPLETED) {

			LogUtils.error("SeekTo Exception!!!");
			throw new IllegalStateException("Error State: " + state);
		}

		mSeekingTimeMsec = (long) seekingTime;
		mSeeking = true;
		LogUtils.info("Java: set mSeeking to true");

		Message msg = mMediaEventHandler.obtainMessage(EVENT_SEEKTO);
		mMediaEventHandler.sendMessageAtFrontOfQueue(msg);
	}

	private void onSeekToEvent() {
	    LogUtils.error("onSeekToEvent() " + mSeekingTimeMsec);
	    
		mExtractor.seekTo(mSeekingTimeMsec * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
	}

	@Override
	public void release() {
		super.release();
		
		LogUtils.info("release()");
		
		mLock.lock();
		stayAwake(false);
		release_l();
		mLock.unlock();
	}

	private void release_l() {
		LogUtils.info("release_l()");
		
		reset();
		
		removeAllEvents();
		LogUtils.info("after removeAllEvents()");

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
		} catch (IllegalStateException e) {
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

	@Override
	public void reset() {
		stayAwake(false);
		
		PlayState state = getState();
		if (state != PlayState.IDLE && state != PlayState.INITIALIZED)
			stop();
		
		removeAllEvents();
		
		setState(PlayState.IDLE);
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
		if (mSeeking)
			return (int) mSeekingTimeMsec;
		else
			return (int) mCurrentTimeMsec;
	}

	@Override
	public int getDuration() {
		return (int) (Utils.convertTime(mDurationUsec /* srcDuration */,
				TimeUnit.MICROSECONDS /* from */, TimeUnit.MILLISECONDS /* to */)
		/* microseconds to milliseconds */);
	}

	@Override
	public int getBufferingTime() {
		if (null != mExtractor) {
			return (int) (Utils.convertTime(mExtractor.getCachedDuration(),
					TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS));
		}

		return 0;
	}

	@Override
	public int flags() throws IllegalStateException {

		if (UrlUtil.isLivePlayUrl(mUrl)) {
			return 0;
		} else {
			return MediaPlayer.CAN_PAUSE | MediaPlayer.CAN_SEEK
					| MediaPlayer.CAN_SEEK_BACKWARD
					| MediaPlayer.CAN_SEEK_FORWARD;
		}
	}

	@Override
	public DecodeMode getDecodeMode() {
		return DecodeMode.HW_XOPLAYER;
	}

	public void selectAudioChannel(int index) {
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
		LogUtils.info(String.format("getSnapShot() %d x %d, fmt: %d", width,
				height, fmt));

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
		return mLooping;
	}

	@Override
	public void setLooping(boolean looping) {
		// TODO Auto-generated method stub
		mLooping = looping;
	}

	@Override
	public void setAudioStreamType(int streamType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSubtitleParser(SimpleSubTitleParser parser) {
		// TODO Auto-generated method stub
        mSubParser = parser;
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
		if (mExtractor != null)
			mExtractor.selectTrack(index);
	}

	@Override
	public void deselectTrack(int index) {
		// TODO Auto-generated method stub
		if (mExtractor != null)
			mExtractor.unselectTrack(index);
	}

	@Override
	public MediaInfo getMediaInfo() throws IllegalStateException {
		// TODO Auto-generated method stub

		//dragon_trainer_4audio|/storage/usbotg/usbotg-sda1/dragon_trainer_4audio.mkv
		//|6115243|0|1280x720|null|
		//{language=und, height=720, width=1280, mime=video/avc}
		//|{sample-rate=24000, channel-count=2, language=und, mime=audio/mp4a-latm}
		//(null,und)|{sample-rate=24000, channel-count=2, language=chi, mime=audio/mp4a-latm}
		//(null,chi)|{sample-rate=24000, channel-count=2, language=chi, mime=audio/mp4a-latm}
		//(null,chi)|{sample-rate=24000, channel-count=2, language=chi, mime=audio/mp4a-latm}
		//(null,chi)|

		MediaInfo mediaInfo = new MediaInfo(mUrl);
		if (mVideoFormat != null) {
			String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
			int pos = mime.indexOf("/");
			if (pos != -1)
				mime = mime.substring(pos + 1);
			mediaInfo.setVideoInfo(
					mVideoWidth, mVideoHeight, mime, 
					(int)(mDurationUsec / 1000));
		}
		if (mAudioFormat != null) {
			String mime = mAudioFormat.getString(MediaFormat.KEY_MIME);
			int pos = mime.indexOf("/");
			if (pos != -1)
				mime = mime.substring(pos + 1);
			mediaInfo.setAudioChannels(1);
			mediaInfo.setAudioChannelsInfo(
                    0, mAudioTrackIndex, mime, null, "N/A", "N/A");
		}
        if (mHaveSubtitle) {
            mediaInfo.setSubtitleChannels(1);
            mediaInfo.setSubtitleChannelsInfo(
                    0, mSubtitleTrackIndex, mSubtitleMime, "N/A", "N/A");
        }
		
		return mediaInfo;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setNextMediaPlayer(MediaPlayer next) {

	}

}
