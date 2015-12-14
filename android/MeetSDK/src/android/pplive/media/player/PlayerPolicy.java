package android.pplive.media.player;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

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
	
	private static String sPlayerPolicy;
	
	public static void setPlayerPolicy(String xml) {
		LogUtils.info("setPlayerPolicy()");
		if (xml != null && !xml.isEmpty()) {
			String tmp = xml;
			if (tmp.length() > 32)
				tmp = tmp.substring(0, 32).replace("\n", "");
			LogUtils.info("setPlayerPolicy xml context: " + tmp);
			
			sPlayerPolicy = xml;
		}
	}

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
			if (sPlayerPolicy != null && !sPlayerPolicy.isEmpty()) {
				if (is_supported_protocol(url))
					return DecodeMode.HW_SYSTEM;
			}
			else {
				// common case
				// network stream
				
				if (buildString.startsWith(BUILDID_PPBOXMINI) || buildString.startsWith(BUILDID_PPBOX1S) ||
						buildString.startsWith(BULDID_XIANFENG_TV))
				{
					// fix dlna push cell-phone recorded clip play stuck problem
					// fix blue-disk airplay play stuck problem
					if (url.startsWith("http://"))
						return DecodeMode.HW_SYSTEM;
				}
			}
			
			return DecodeMode.SW;
		}
		
		// only local file will get media info
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
		
		if (sPlayerPolicy != null && !sPlayerPolicy.isEmpty()) {
			LogUtils.info("Java: use getDeviceCapabilitiesCustomized");
			return getDeviceCapabilitiesCustomized(url, formatName, videoCodecName, audioCodecName);
		}
		else if (buildString.startsWith(BUILDID_PPBOXMINI)) {
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
	
	private static boolean is_supported_protocol(String url) {
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(sPlayerPolicy);  
        Document doc;
        try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			String supported_protocol = root.getChild("Protocol").getText();
			LogUtils.info("Java: supported_protocol " + supported_protocol);
			StringTokenizer st = new StringTokenizer(supported_protocol, ",", false);
			while (st.hasMoreElements()) {
				String protocol = st.nextToken();
				LogUtils.info("Java: protocol " + protocol);
				if (url.toLowerCase().endsWith(protocol))
					return true;
			}
		}
		catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtils.error("Java: PlayerPolicy xml context is broken " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtils.error("Java: PlayerPolicy xml IOException " + e.getMessage());
		}
        
		return false;
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
				if ((null == videoCodecName || videoCodecName.equals("h263") || videoCodecName.equals("h264") || videoCodecName.equals("mpeg4video")) && 
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
				url.toLowerCase().endsWith("mkv") || formatName.equals("mpegts") ||
				formatName.equals("matroska")) {
			if ((null == videoCodecName || 
				 videoCodecName.equals("h263") ||  videoCodecName.equals("h264") || 
				 videoCodecName.equals("hevc") ||  videoCodecName.equals("mpeg4") ||
				 videoCodecName.equals("xvid") ||  videoCodecName.equals("divx")) 
				 && 
				 (null == audioCodecName || audioCodecName.equals("aac") || 
				 audioCodecName.equals("vorbis") || 
				 audioCodecName.equals("wmav1") || audioCodecName.equals("wmav2"))) {
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
		else if (url.toLowerCase().endsWith("mkv") || url.toLowerCase().endsWith("webm") ||
				formatName.equals("matroska")) {
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
				 videoCodecName.equals("mpeg2video") || videoCodecName.equals("mpeg4") ||
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
	
	private static DecodeMode getDeviceCapabilitiesCustomized(
			String url, String formatName, String videoCodecName, String audioCodecName) {
		SAXBuilder builder = new SAXBuilder();
		Reader returnQuote = new StringReader(sPlayerPolicy);  
        Document doc;
        try {
			doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			String device_desc = root.getChild("DeviceDesc").getText();
			int device_id = Integer.valueOf(root.getChild("DeviceId").getText());
			LogUtils.info(String.format("Java: device description %s, id %d",
					device_desc, device_id));
			
			String supported_picture = root.getChild("Picture").getChild("Ext").getText();
			String supported_music = root.getChild("Music").getChild("Ext").getText();

			StringTokenizer st;
			
			st = new StringTokenizer(supported_picture, ",", false);
			while (st.hasMoreElements()) {
				String ext = st.nextToken();
				LogUtils.info("Java: ext " + ext);
				if (url.toLowerCase().endsWith(ext))
					return DecodeMode.HW_SYSTEM;
			}
			
			st = new StringTokenizer(supported_music, ",", false);
			while (st.hasMoreElements()) {
				String ext = st.nextToken();
				if (url.toLowerCase().endsWith(ext))
					return DecodeMode.HW_SYSTEM;
			}
			
			List<Element> videos = root.getChildren("Video");
			LogUtils.info("Java: video list size: " + videos.size());
			for (int i=0;i<videos.size();i++) {
				String supported_ext = videos.get(i).getChild("Ext").getText();
				String supported_demuxer = videos.get(i).getChild("Demuxer").getText();
				String supported_video_codec = videos.get(i).getChild("VideoCodec").getText();
				String supported_audio_codec = videos.get(i).getChild("AudioCodec").getText();
				
				StringTokenizer st1, st2, st3, st4;
				st1 = new StringTokenizer(supported_ext, ",", false);
				st2 = new StringTokenizer(supported_demuxer, ",", false);
				st3 = new StringTokenizer(supported_video_codec, ",", false);
				st4 = new StringTokenizer(supported_audio_codec, ",", false);
				
				boolean format_done = false;
				while (st1.hasMoreElements()) {
					String ext = st1.nextToken();
					if (url.toLowerCase().endsWith(ext)) {
						format_done = true;
						break;
					}
				}

				if (!format_done) {
					while (st2.hasMoreElements()) {
						String demuxer = st2.nextToken();
						if (formatName.equals(demuxer)) {
							format_done = true;
							break;
						}
					}
				}
					
				if (format_done) {
					boolean video_done = false;
					if (videoCodecName == null)
						video_done = true;
					else {
						while (st3.hasMoreElements()) {
							String v_codec = st3.nextToken();
							if (v_codec.equals(videoCodecName)) {
								video_done = true;
								break;
							}
						}
					}
					
					if (video_done) {
						if (audioCodecName == null)
							return DecodeMode.HW_SYSTEM;
						
						while (st4.hasMoreElements()) {
							String a_codec = st4.nextToken();
							if (a_codec.equals(audioCodecName))
								return DecodeMode.HW_SYSTEM;
						}
					}
				}
			}
		}
		catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtils.error("Java: PlayerPolicy xml context is broken " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtils.error("Java: PlayerPolicy xml IOException " + e.getMessage());
		}
        
		return DecodeMode.SW;
	}
}
