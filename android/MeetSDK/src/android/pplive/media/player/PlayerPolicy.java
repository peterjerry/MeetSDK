package android.pplive.media.player;

import java.util.ArrayList;

import android.net.Uri;
import android.pplive.media.player.MediaPlayer.DecodeMode;
import android.pplive.media.util.DeviceInfoUtil;
import android.pplive.media.util.LogUtils;

public class PlayerPolicy {

	private static final String TAG = "pplive/PlayerPolicy";
	
	//private static final String PPBOX_MINI_MODEL_NAME = "mt8127_box_p1v1";
	private static final String BUILDID_PPBOX1S		= "ppbox1s";
	private static final String BUILDID_PPBOXMINI		= "ppboxmini";
	private static final String BULDID_XIANFENG_TV 	= "PR_CVT";

	public static DecodeMode getDeviceCapabilities(Uri uri) {
		if (null == uri) {
			return DecodeMode.SW;
		}

		String schema = uri.getScheme();
		String path = null;
		
		if ("file".equalsIgnoreCase(schema))
			path = uri.getPath();
		else
			path = uri.toString();
		
		return getDeviceCapabilities(path);
	}
	
	public static DecodeMode getDeviceCapabilities(String url) {
		LogUtils.info("Java: getDeviceCapabilities " + url);
		
		if (null == url || url.equals(""))
			return DecodeMode.SW;
		
		String buildString = android.os.Build.ID;
		
		if (!url.startsWith("/") && !url.startsWith("file://")) {
			// network stream
			if (url.startsWith("http://")) {
				if (buildString.startsWith(BUILDID_PPBOXMINI) || buildString.startsWith(BUILDID_PPBOX1S) ||
						buildString.startsWith(BULDID_XIANFENG_TV))
				{
					// fix dlna push cell-phone recorded clip play stuck problem
					// fix blue-disk airplay play stuck problem
					return DecodeMode.HW_SYSTEM;
				}
			}
			
			return DecodeMode.SW;
		}
		
		MediaInfo info = MeetPlayerHelper.getMediaDetailInfo(url);
		if (info == null) {
			LogUtils.warn("Java: failed to get media info");
			return DecodeMode.SW;
		}
		
		String formatName = info.getFormatName();
		String videoCodecName = info.getVideoCodecName();
		String audioCodecName = null;
		ArrayList<TrackInfo> audiolist = info.getAudioChannelsInfo();
		if (audiolist.size() > 0) {
			audioCodecName = audiolist.get(0).getCodecName();
		}
		
		LogUtils.info(String.format("Java: getDeviceCapabilities url %s, format %s, video %s, audio %s", 
				url, formatName, videoCodecName, audioCodecName));
		
		if (buildString.startsWith(BUILDID_PPBOXMINI)) {
			LogUtils.info("Java: use getDeviceCapabilitiesPPBoxMini");
			return getDeviceCapabilitiesPPBoxMini(url, formatName, videoCodecName, audioCodecName);
		}
		else if (buildString.startsWith(BUILDID_PPBOX1S)) {
			LogUtils.info("Java: use getDeviceCapabilitiesPPBox");
			return getDeviceCapabilitiesPPBox(url, formatName, videoCodecName, audioCodecName);
		}
		else if (buildString.startsWith(BULDID_XIANFENG_TV)) {
			LogUtils.info("Java: use getDeviceCapabilitiesXianFengTV");
			return getDeviceCapabilitiesXianFengTV(url, formatName, videoCodecName, audioCodecName);
		}
		else {
			LogUtils.info("Java: use getDeviceCapabilitiesCommon");
			return getDeviceCapabilitiesCommon(url, formatName, videoCodecName, audioCodecName);
		}
	}
	
	private static DecodeMode getDeviceCapabilitiesCommon(
			String url, String formatName, String videoCodecName, String audioCodecName) {
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
			if (url.toLowerCase().endsWith("mp4") || url.toLowerCase().endsWith("3gp") || 
					url.toLowerCase().endsWith("ts") || url.toLowerCase().endsWith("mkv") || 
					formatName.equals("mpegts")) {
				if ((null == videoCodecName || videoCodecName.equals("h263") || videoCodecName.equals("h264")) && 
					(null == audioCodecName || audioCodecName.equals("aac"))) {
					return DecodeMode.HW_SYSTEM;
				}
			}
			
			// audio
			if (url.toLowerCase().endsWith("ape"))
				return DecodeMode.HW_SYSTEM;
		}
		else if (AndroidSystemVersion >= 11 /* < 3.0 */) {
			// video
			if (url.toLowerCase().endsWith("mp4") || url.toLowerCase().endsWith("3gp")) {
				if ((null == videoCodecName || videoCodecName.equals("h263") || videoCodecName.equals("h264")) && 
						(null == audioCodecName || audioCodecName.equals("aac"))) {
						return DecodeMode.HW_SYSTEM;
					}
			}
		}
		else { /* 2.0 */
			// video
			if (url.toLowerCase().endsWith("3gp")) {
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
		if (url.toLowerCase().endsWith("mp4") || url.toLowerCase().endsWith("3gp")  ||
				url.toLowerCase().endsWith("flv") || url.toLowerCase().endsWith("ts") ||
				url.toLowerCase().endsWith("mkv") || formatName.equals("mpegts")) {
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
	
	private static DecodeMode getDeviceCapabilitiesXianFengTV(
			String url, String formatName, String videoCodecName, String audioCodecName) {
		if (null == url || url.equals(""))
			return DecodeMode.SW;
		
		// audio
		final String[] audioformats = {"flac", "mp3", "mp2", "ape"};
		for (String temp : audioformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		// image
		final String[] imageformats = {"bmp", "jpeg", "jpg", "png", "gif", "webp", "mpo", "jps", "pns"};
		for (String temp : imageformats) {
			if (url.toLowerCase().endsWith(temp))
				return DecodeMode.HW_SYSTEM;
		}
		
		// video
		if (url.toLowerCase().endsWith("mp4") || url.toLowerCase().endsWith("3gp") ||
				url.toLowerCase().endsWith("mov")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("h263") || videoCodecName.equals("h264") || 
				 videoCodecName.equals("hevc") || videoCodecName.equals("mpeg4") ||
				 videoCodecName.equals("vp8") || videoCodecName.equals("vc1") ||
				 videoCodecName.equals("mjpeg") || videoCodecName.equals("wmv3")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("aac") || 
				 audioCodecName.equals("mp1") || audioCodecName.equals("mp2") ||
				 audioCodecName.equals("mp3") || audioCodecName.equals("amr_nb") ||
				 audioCodecName.equals("amr_wb"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("avi")) {
			if ((null == videoCodecName || 
					 videoCodecName.equals("xvid") || videoCodecName.equals("h264") || 
					 videoCodecName.equals("hevc") || videoCodecName.equals("mpeg4") ||
					 videoCodecName.equals("mjpeg") || videoCodecName.equals("h263") ||
					 videoCodecName.equals("wmv3")) 
					 && 
					 (null == audioCodecName || audioCodecName.equals("mp1") ||
					 audioCodecName.equals("mp2") || audioCodecName.equals("mp3") || 
					 audioCodecName.equals("aac") || audioCodecName.equals("ac3") || 
					 audioCodecName.equals("wmav1"))) {
					return DecodeMode.HW_SYSTEM;
				}
		}
		else if (url.toLowerCase().endsWith("wmv") || url.toLowerCase().endsWith("asf") ||
				url.toLowerCase().endsWith("wma")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("mpeg4") || videoCodecName.equals("h264") || 
				 videoCodecName.equals("xvid") || videoCodecName.equals("mjpeg") ||
				 videoCodecName.equals("wmv3")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("wmapro") ||
				 audioCodecName.equals("wmav1") || audioCodecName.equals("wmav2"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("mkv") || url.toLowerCase().endsWith("webm")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("mpeg4") || videoCodecName.equals("h264") || 
				 videoCodecName.equals("hevc") || videoCodecName.equals("mjpeg") ||
				 videoCodecName.equals("wmv3") || videoCodecName.equals("vc1") ||
				 videoCodecName.equals("vp8")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("aac") ||
				 audioCodecName.equals("mp1") || audioCodecName.equals("mp2") ||
				 audioCodecName.equals("mp3") || audioCodecName.equals("ac3") ||
				 audioCodecName.contains("pcm") || audioCodecName.equals("vorbis"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("mpg") || url.toLowerCase().endsWith("mpeg") ||
				url.toLowerCase().endsWith("vro") || url.toLowerCase().endsWith("vob") ||
				formatName.equals("mpegps")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("mpeg1video") || videoCodecName.equals("mpeg2video")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("mp1") || 
				 audioCodecName.equals("mp2") || audioCodecName.equals("mp3") || 
				 audioCodecName.equals("ac3"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("ts") || url.toLowerCase().endsWith("m2ts") ||
				formatName.equals("mpegts")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("h264") || videoCodecName.equals("vc1") ||
				 videoCodecName.equals("mpeg2video") || videoCodecName.equals("hevc") ||
				 videoCodecName.equals("cavs")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("mp1") || 
				 audioCodecName.equals("mp2") || audioCodecName.equals("aac") || 
				 audioCodecName.equals("ac3"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("rm") || url.toLowerCase().endsWith("rmvb") ||
				formatName.equals("rm")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("rv10") || videoCodecName.equals("rv20") ||
				 videoCodecName.equals("rv30") || videoCodecName.equals("rv40")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("aac") || 
				 audioCodecName.equals("ac3") || audioCodecName.equals("cook"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("ogm") || formatName.equals("ogg")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("mpeg2video") || videoCodecName.equals("mp4") ||
				 videoCodecName.equals("h264") || videoCodecName.equals("mjpeg"))) {
				return DecodeMode.HW_SYSTEM;
			}
		}
		else if (url.toLowerCase().endsWith("flv") || formatName.equals("flv")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("vp6") || videoCodecName.equals("h264") ||
				 videoCodecName.equals("mpeg4") || videoCodecName.equals("mjpeg") ||
				 videoCodecName.equals("flv1") || 
				 videoCodecName.equals("svq1") ||  videoCodecName.equals("svq3"))
				 && 
				 (null == audioCodecName || audioCodecName.equals("mp1") || 
				 audioCodecName.equals("mp2") || audioCodecName.equals("mp3") ||
				 audioCodecName.contains("aac"))) {
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
