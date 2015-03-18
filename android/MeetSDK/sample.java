////////////////////MediaPlayer///////////////////
void start_player() {

	// force refresh a new surface
	mPreview.setVisibility(View.INVISIBLE);

	if (DecodeMode.HW_SYSTEM == dec_mode) {
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	else if (DecodeMode.SW == dec_mode){
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		mHolder.setFormat(PixelFormat.RGBX_8888/*RGB_565*/);
	}
	
	mPreview.setVisibility(View.VISIBLE);
			
	mPlayer = new MediaPlayer(dec_mode);
	
	// fix Mediaplayer setVideoSurfaceTexture failed: -17
	mPlayer.setDisplay(null);
	mPlayer.reset();

	mPlayer.setDisplay(mPreview.getHolder());
	mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	mPlayer.setScreenOnWhilePlaying(true);
	mPlayer.setOnPreparedListener(this);
	mPlayer.setOnVideoSizeChangedListener(this);
	mPlayer.setOnCompletionListener(this);
	mPlayer.setOnErrorListener(this);
	mPlayer.setOnBufferingUpdateListener(this);
	mPlayer.setOnInfoListener(this);
	
	boolean succeed = true;
	try {
		mPlayer.setDataSource(path);
	} catch (IllegalArgumentException e) {
		// TODO Auto-generated catch block
		succeed = false;
		e.printStackTrace();
	} catch (IllegalStateException e) {
		// TODO Auto-generated catch block
		succeed = false;
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		succeed = false;
		e.printStackTrace();
	}
	
	try {
		mPlayer.prepareAsync();
	}
	catch (IllegalStateException e) {
		// TODO Auto-generated catch block
		succeed = false;
		e.printStackTrace();
		Log.e(TAG, "Java: prepareAsync() exception: " + e.getMessage());
	}
	
	if (succeed) {
		mBufferingProgressBar.setVisibility(View.VISIBLE);
		mIsBuffering = true;
	}
	else {
		Toast.makeText(this, "Java: failed to play: " + path, Toast.LENGTH_SHORT).show();
	}	
}
////////////////////MediaPlayer///////////////////

////////////////////MeetVideoView/////////////////
void start_videoview() {
	mController = (MyMediaController) findViewById(R.id.video_controller);
	mVideoView = (MeetVideoView) findViewById(R.id.surface_view);
	
	mVideoView.setDecodeMode(mDecodeMode);
	mVideoView.setVideoURI(mUri);
	mController.setMediaPlayer(mVideoView);
	mVideoView.setOnCompletionListener(mCompletionListener);
	mVideoView.setOnErrorListener(mErrorListener);
	mVideoView.setOnInfoListener(mInfoListener);
	mVideoView.setOnPreparedListener(mPreparedListener);
	
	mVideoView.start();
}

private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
	public void onCompletion(MediaPlayer mp) {
		Log.d(TAG, "MEDIA_PLAYBACK_COMPLETE");
		mVideoView.stopPlayback();
		finish();
	}
};

private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
	public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
		Log.d(TAG, "Error: " + framework_err + "," + impl_err);
		mVideoView.stopPlayback();
		finish();
		return true;
	}
};

private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
	
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.d(TAG, "Java: onInfo: " + what + " " + extra);
		
		if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_START) && !mIsBuffering) {
			mBufferingProgressBar.setVisibility(View.VISIBLE);
			mIsBuffering = true;
		} else if ((what == MediaPlayer.MEDIA_INFO_BUFFERING_END) && mIsBuffering) {
			mBufferingProgressBar.setVisibility(View.GONE);
			mIsBuffering = false;
		} else if (MediaPlayer.MEDIA_INFO_TEST_PLAYER_TYPE == what) {
			String str_player_type;
			if (MediaPlayer.PLAYER_IMPL_TYPE_SYSTEM_PLAYER == extra)
				str_player_type = "System Player";
			else if(MediaPlayer.PLAYER_IMPL_TYPE_NU_PLAYER == extra)
				str_player_type = "Nu Player";
			else if(MediaPlayer.PLAYER_IMPL_TYPE_FF_PLAYER == extra)
				str_player_type = "FF Player";
			else if(MediaPlayer.PLAYER_IMPL_TYPE_PP_PLAYER == extra)
				str_player_type = "PP Player";
			else
				str_player_type = "Unknown Player";
			Toast.makeText(VideoPlayerActivity.this, str_player_type, Toast.LENGTH_SHORT).show();
		}
		
		return true;
	}
};

private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.i(TAG, "Java: OnPrepared");
		mController.show();
	}
};
////////////////////MeetVideoView/////////////////
