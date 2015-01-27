package android.pplive.media.player;

import java.util.ArrayList;

import android.net.Uri;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.util.DeviceInfoUtil;
import android.pplive.media.util.LogUtils;

public class PlayerPolicy {

	private static final String TAG = "pplive/PlayerPolicy";
	
	private static final String PPBOX_MINI_MODEL_NAME = "mt8127_box_p1v1";
	private static final String BUILDID_PPBOX1S = "ppbox1s";
	private static final String BUILDID_PPBOXMINI = "ppboxmini";

	@Deprecated
	public static DecodeMode getDeviceCapabilities(Uri uri) {
		// fix me
		return DecodeMode.SW;
	}
	
	public static DecodeMode getDeviceCapabilities(String url) {
		LogUtils.info("Java: getDeviceCapabilities " + url);
		
		if (!url.startsWith("/") && !url.startsWith("file://")) {
			// network stream use ffplay
			return DecodeMode.SW;
		}
		
		MediaInfo info = MeetPlayerHelper.getMediaDetailInfo(url);
		if (info == null) {
			LogUtils.info("Java: failed to get media info");
			return DecodeMode.SW;
		}
		
		String formatName = info.getFormatName();
		String videoCodecName = info.getVideoCodecName();
		String audioCodecName = null;
		ArrayList<TrackInfo> audiolist = info.getAudioChannelsInfo();
		if (audiolist.size() > 0) {
			audioCodecName = audiolist.get(0).getCodecName();
		}
		
		String buildString = android.os.Build.ID;
		
		if (buildString.startsWith(BUILDID_PPBOXMINI)) {
			LogUtils.info("use ppbox getDeviceCapabilitiesPPBoxMini");
			return getDeviceCapabilitiesPPBoxMini(url, formatName, videoCodecName, audioCodecName);
		}
		else if(buildString.startsWith(BUILDID_PPBOX1S)) {
			LogUtils.info("use ppbox getDeviceCapabilitiesPPBox");
			return getDeviceCapabilitiesPPBox(url, formatName, videoCodecName, audioCodecName);
		}
		else {
			LogUtils.info("use ppbox getDeviceCapabilitiesCommon");
			return getDeviceCapabilitiesCommon(url, formatName, videoCodecName, audioCodecName);
		}
	}
	
	private static DecodeMode getDeviceCapabilitiesCommon(
			String url, String formatName, String videoCodecName, String audioCodecName) {
		
		if (null == url || url.equals(""))
			return DecodeMode.SW;
		
		int AndroidSystemVersion = DeviceInfoUtil.getSystemVersionInt();
		
		// audio
		final String[] audioformats = {"flac", "mp3", "ogg", "wav", "mid", "amr"};
		for (String temp : audioformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		// image
		final String[] imageformats = {"bmp", "jpeg", "jpg", "png", "gif"};
		for (String temp : imageformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		if (AndroidSystemVersion >= 14 /* 4.0+ */ ) { 
			// video
			if (url.endsWith("mp4") || url.endsWith("3gp") ||
				formatName.equals("mpegts") || url.endsWith("mkv")) {
				if ((null == videoCodecName || videoCodecName.equals("h263") || videoCodecName.equals("h264")) && 
					(null == audioCodecName || audioCodecName.equals("aac"))) {
					return DecodeMode.HW_SYSTEM;
				}
			}
		}
		else if (AndroidSystemVersion >= 11 /* < 3.0 */) {
			// video
			if (url.endsWith("mp4") || url.endsWith("3gp")) {
				if ((null == videoCodecName || videoCodecName.equals("h263") || videoCodecName.equals("h264")) && 
						(null == audioCodecName || audioCodecName.equals("aac"))) {
						return DecodeMode.HW_SYSTEM;
					}
			}
		}
		else { /* 2.0 */
			// video
			if (url.endsWith("3gp")) {
				if ((null == videoCodecName || videoCodecName.equals("h263")) && null == audioCodecName) {
					return DecodeMode.HW_SYSTEM;
				}
			}
		}
		
		return DecodeMode.SW;
	}
	
	private static DecodeMode getDeviceCapabilitiesPPBoxMini(
			String url, String formatName, String videoCodecName, String audioCodecName) {
		if (null == url || url.equals(""))
			return DecodeMode.SW;
		
		// audio
		final String[] audioformats = {"flac", "mp3", "ogg", "wav", "mid", "amr", "ape", "pcm"};
		for (String temp : audioformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		// image
		final String[] imageformats = {"bmp", "jpeg", "jpg", "png", "gif"};
		for (String temp : imageformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		// video
		if (url.endsWith("mp4") || url.endsWith("3gp") || formatName.equals("mpegts") ||
				url.endsWith("flv")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("h263") ||  videoCodecName.equals("h264") || 
				 videoCodecName.equals("hevc") ||  videoCodecName.equals("mpeg4") ||
				 videoCodecName.equals("xvid") ||  videoCodecName.equals("divx")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("aac") || 
				 audioCodecName.equals("vorbis") || audioCodecName.equals("wma"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}

		return DecodeMode.SW;
	}
	
	private static DecodeMode getDeviceCapabilitiesPPBox(
			String url, String formatName, String videoCodecName, String audioCodecName) {
		return getDeviceCapabilitiesCommon(url, formatName, videoCodecName, audioCodecName);
	}
}
