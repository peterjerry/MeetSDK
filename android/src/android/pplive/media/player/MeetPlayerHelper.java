package android.pplive.media.player;

import android.graphics.Bitmap;
import android.view.Surface;

public class MeetPlayerHelper {
	
	private MeetPlayerHelper() {}
	
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
	
	public static MediaInfo getMediaDetailInfo(String mediaFilePath) {
		return FFMediaPlayer.getMediaDetailInfo(mediaFilePath);
	}
	
	public static MediaInfo getMediaInfo(String mediaFilePath) {
		return FFMediaPlayer.getMediaInfo(mediaFilePath);
	}
	
	
}
