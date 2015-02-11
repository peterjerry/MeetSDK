package com.pplive.meetplayer.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.util.Log;

public class PlayLinkUtil {
    public static final String TAG = "PlayLinkUtil";
    
    public static final String P2PType_CDN = "2";

    public static final String P2PType_CDNP2P = "1";

    public static final String P2PType_P2P = "0";

    /**
     * P2P type,茂哥提供，0：只是用P2P,1：CDN+P2P，2：CDN
     * 最新增加t参数，所以修改？bwtype为&bwtype。play接口会返回
     */
    public final static String P2PType = "bwtype=";

    /** 点播 */
    public static final String TYPE_PPVOD2 = "ppvod2";

    /** 直播 */
    public static final String TYPE_PPLIVE3 = "pplive3";
    
    public static final String TYPE_UNICOM = "ppliveunicom";


    private static final String HOST = "127.0.0.1";

    private static final String HTTP_MP4_RECORD_PPVOD2 = "http://" + HOST + ":%s/record.mp4?type=ppvod2&playlink=%s";

    private static final String HTTP_M3U8_RECORD_PPVOD2 = "http://" + HOST
            + ":%s/record.m3u8?type=ppvod2&playlink=%s&mux.M3U8.segment_duration=5";

    private static final String HTTP_M3U8_RECORD_PPVOD2_CHUNKED = HTTP_M3U8_RECORD_PPVOD2 + "&chunked=true";

    private static final String HTTP_M3U8_PLAY_PPLIVE3 = "http://" + HOST+":%s/play.m3u8?type=pplive3&playlink=%s";

    private static final String RTSP_ES_URL = "rtsp://" + HOST + ":%s/play.es?type=%s&playlink=%s";

    private static final String PPVOD2_URL = "ppvod2:///%s";

    private static final String PPLIVE3_URL = "pplive3:///%s";

    private static final String HTTP_MP4_PLAYINFO = "http://" + HOST + ":%s/playinfo.mp4";

    private static final String HTTP_MP4_MEDIAINFO_PPVOD2 = "http://" + HOST + ":%s/mediainfo.mp4?type=ppvod2&playlink=%s";

    private static final String HTTP_M3U8_CLOSE_URL = "http://" + HOST + ":%s/close";
	
	 /** 码流 */
    /** baseline */
    public static final int FT_BASELINE = 5;

    /** 流畅 */
    public static final int FT_LOW = 0;

    /** 高清 */
    public static final int FT_DVD = 1;

    /** 超清 */
    public static final int FT_HD = 2;

    /** 蓝光 */
    public static final int FT_BD = 3;

    public static final int FT_UNKNOWN = -1;
    
    public static String getPlayUrl(boolean isVOD, 
    		int playlink, int http_port, int ft, int bwt, String link_surfix) {
    	String ppbox_url;
		String str_playlink;
		str_playlink = addPlaylinkParam(playlink, ft, bwt);

		try {
			str_playlink = URLEncoder.encode(str_playlink, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (isVOD) {
			ppbox_url = String.format(HTTP_M3U8_RECORD_PPVOD2, http_port, str_playlink);
		}
		else {
			// live
			if (link_surfix == null || link_surfix.equals("")) {
				// real live
				ppbox_url = String.format(HTTP_M3U8_PLAY_PPLIVE3, http_port, str_playlink) + "&m3u8seekback=true"; // &chunked=true
			}
			else {
				// fake vod
				ppbox_url = String.format(HTTP_M3U8_PLAY_PPLIVE3, http_port, str_playlink) + link_surfix;
			}
		}

		Log.i(TAG, "Java: getPlayUrl " + ppbox_url);
		return ppbox_url;
    }
    
    private static String addPlaylinkParam(int playlink, int ft, int bwt) {
    	StringBuffer sbPlaylink = new StringBuffer();
    	sbPlaylink.append(playlink);
    	sbPlaylink.append("?ft=");
    	sbPlaylink.append(ft);
    	sbPlaylink.append("&bwtype=");
    	sbPlaylink.append(bwt);
    	
    	sbPlaylink.append("&platform=android3");
    	sbPlaylink.append("&type=phone.android.vip");
        sbPlaylink.append("&sv=4.0.1");
        sbPlaylink.append("&param=userType%3D1"); // fix cannot find blue-disk ft problem
    	
    	return sbPlaylink.toString();
    }
}