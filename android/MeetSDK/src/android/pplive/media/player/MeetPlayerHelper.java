package android.pplive.media.player;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class MeetPlayerHelper {
	
	private MeetPlayerHelper() {}
	
	// all method are static function
	public static Bitmap createVideoThumbnail(String mediaFilePath, int kind) {
		return FFMediaPlayer.createVideoThumbnail(mediaFilePath, kind);
	}
	
	public static int checkSoftwareDecodeLevel() {
		return FFMediaPlayer.checkSoftwareDecodeLevel();
	}
	
	public static String getBestCodec(String appPath) {
		return FFMediaPlayer.getBestCodec(appPath);
	}
	
	public static int getCpuArchNumber() {
		return FFMediaPlayer.getCpuArchNumber();
	}
	
	/*
	@Deprecated
	public static MediaInfo getSystemMediaDetailInfo(String mediaFilePath) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			return null;
	
		MediaPlayer mp = new MediaPlayer(DecodeMode.HW_SYSTEM);
		try {
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					// TODO Auto-generated method stub
					Log.e("MeetPlayerHelper", String.format("getMediaDetailInfo system_player onError %d, %d", what, extra));
					return true;
				}
			});
			
			mp.setDataSource(mediaFilePath);
			mp.prepare();
			
			android.media.MediaPlayer.TrackInfo [] trackinfos = mp.getTrackInfo();
			if (trackinfos != null && trackinfos.length > 0) {
				return fillMediaInfo(mediaFilePath, mp, trackinfos);
			}
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
		finally {
			mp.release();
		}
		
		return null;
	}*/
	
	public static MediaInfo getMediaDetailInfo(String mediaFilePath) {
		return FFMediaPlayer.getMediaDetailInfo(mediaFilePath);
	}
	
	public static MediaInfo getMediaInfo(String mediaFilePath) {
		return FFMediaPlayer.getMediaInfo(mediaFilePath);
	}
	
	
}
