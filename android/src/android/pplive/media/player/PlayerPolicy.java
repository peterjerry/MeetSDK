package android.pplive.media.player;

import java.util.ArrayList;

import android.net.Uri;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.util.DeviceInfoUtil;
import android.pplive.media.util.LogUtils;

class PlayerPolicy {

	private static final String TAG = "pplive/PlayerPolicy";
	
	private static final String PPBOX_MODEL_NAME = "mt8127_box_p1v1";
	
	@Deprecated
	public static DecodeMode getDeviceCapabilities(Uri uri) {
		// fix me
		return DecodeMode.SW;
	}
	
	public static DecodeMode getDeviceCapabilities(String path) {
		MediaInfo info;

		info = MeetPlayerHelper.getMediaDetailInfo(path);
		if (info == null)
			return DecodeMode.UNKNOWN;
		
		int AndroidSystemVersion = DeviceInfoUtil.getSystemVersionInt();
		String formatName = info.getFormatName();
		String videoCodecName = info.getVideoCodecName();
		String audioCodecName = null;
		ArrayList<TrackInfo> audiolist = info.getAudioChannelsInfo();
		if (audiolist.size() > 0) {
			audioCodecName = audiolist.get(0).getCodecName();
		}
		
		if (PPBOX_MODEL_NAME == DeviceInfoUtil.getModel()) {
			LogUtils.info("use ppbox getDeviceCapabilitiesPPBox");
			return getDeviceCapabilitiesPPBox(formatName, videoCodecName, audioCodecName);
		}
		
		
		// audio
		if ("flac" == formatName || "mp3" == formatName || "ogg" == formatName ||
				"wav" == formatName || "mid" == formatName || "amr" == formatName) {
			return DecodeMode.HW_SYSTEM;
		}
		
		// image
		if ("bmp" == formatName || "jpeg" == formatName ||
			"png" == formatName || "gif" == formatName) {
			return DecodeMode.HW_SYSTEM;
		}
		
		if (AndroidSystemVersion >= 14 /* 4.0+ */ ) { 
			// video
			if (formatName.contains("mp4") || formatName.contains("3gp") ||
				formatName.contains("mpegts") || formatName.contains("mkv")) {
				if ((null == videoCodecName || "h263" == videoCodecName || "h264" == videoCodecName) && 
					(null == audioCodecName || "aac" == audioCodecName)) {
					return DecodeMode.HW_SYSTEM;
				}
			}
		}
		else if (AndroidSystemVersion >= 11 /* < 3.0 */) {
			// video
			if ("mp4" == formatName || "3gp" == formatName) {
				if ((null == videoCodecName || "h263" == videoCodecName || "h264" == videoCodecName) && 
					(null == audioCodecName || "aac" == audioCodecName)) {
					return DecodeMode.HW_SYSTEM;
				}
			}
		}
		else { /* 2.0 */
			// video
			if ("3gp" == formatName) {
				if ((null == videoCodecName || "h263" == videoCodecName) && null == audioCodecName) {
					return DecodeMode.HW_SYSTEM;
				}
			}
		}
		
		return DecodeMode.SW;
	}
	
	private static DecodeMode getDeviceCapabilitiesPPBox(
			String formatName, String videoCodecName, String audioCodecName) {
		// video
		if ("mp4" == formatName || "3gp" == formatName || "mpegts" == formatName ||
				"flv" == formatName) {
			if (("h263" == videoCodecName || "h264" == videoCodecName || 
				"hevc" == videoCodecName || "mpeg4" == videoCodecName ||
				"xvid" == videoCodecName || "divx" == videoCodecName) 
				&& 
				(null == audioCodecName || "aac" == audioCodecName || 
				"vorbis" == audioCodecName || "wma" == audioCodecName)) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		
		// audio
		if ("mp3" == formatName || "flac" == formatName || 
			"vorbis" == formatName || "ape" == formatName || 
			"amr" == formatName || "mid" == formatName ||
			"pcm" == formatName || "wav" == formatName) {
			return DecodeMode.HW_SYSTEM;
		}
		
		// image
		if ("bmp" == formatName || "jpeg" == formatName ||
			"png" == formatName || "gif" == formatName) {
			return DecodeMode.HW_SYSTEM;
		}

		return DecodeMode.SW;
	}
}
