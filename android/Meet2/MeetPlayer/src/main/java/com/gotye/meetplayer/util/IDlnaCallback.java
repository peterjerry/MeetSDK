package com.gotye.meetplayer.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

import com.gotye.common.util.LogUtil;
import com.pplive.dlna.DLNASdk;
import com.pplive.dlna.DLNASdkDMSItemInfo;

public class IDlnaCallback implements DLNASdk.DLNASdkInterface {
	private final static String TAG = "dlna";

	private static IDlnaCallback mInstance = null;

	public static Map<String, String> mDMRmap = new HashMap<String,String>();
	public static Map<String, String> mDMSmap = new HashMap<String,String>();
	private DLNASdk.DLNASdkInterface mCallback;

	public static IDlnaCallback getInstance() {
		if (mInstance == null)
			mInstance = new IDlnaCallback();

		return mInstance;
	}

	public void setCallback(DLNASdk.DLNASdkInterface callback) {
		mCallback = callback;
	}
	
	synchronized public void OnDeviceAdded(String uuid, String friendname, String logourl, int devicetype) {
		LogUtil.info(TAG, String.format(Locale.US,
                "Java: dlna [add] uuid: %s, name: %s, type: %d, logourl %s",
                uuid, friendname, devicetype, logourl));
		
		if (devicetype == DLNASdk.DEVICE_TYPE_DMR) {
            mDMRmap.put(uuid, friendname);
        }
		else if(devicetype == DLNASdk.DEVICE_TYPE_DMS) {
            mDMSmap.put(uuid, friendname);
		}
	}

	synchronized public void OnDeviceRemoved(String uuid, int devicetype) {
        LogUtil.info(TAG, "Java: dlna [remove] uuid: " + uuid + " , type: " + devicetype);
		if (devicetype == DLNASdk.DEVICE_TYPE_DMR) {
            mDMRmap.remove(uuid);
        }
        else if (devicetype == DLNASdk.DEVICE_TYPE_DMS) {
            mDMSmap.remove(uuid);
        }
	}
	
	public void OnLogPrintf(String msg) {
		Log.d("dlna_print", msg);
	}

	//////////////////////////////////////////////////////////////////////////
	// mediashaker callback function
	public boolean OnConnect(String uuid, String requestName) {
		return true;
	}
	
	public void OnConnectCallback(String uuid, int state) {
	}
	
	public void OnDisConnect(String uuid) {
	}
	
	public void OnDisConnectCallback(String uuid, boolean isTimeout) {
	}
	
	public void OnRemoveTransportFile(String uuid, String transportuuid){}
	public void OnRemoveTransportFileCallback(String uuid, String transportuuid, boolean isTimeout){}
	public void OnAddTransportFile(String uuid, String transportuuid, String fileurl, String filename, String thumburl){}
	public void OnAddTransportFileCallback(String uuid, String transportuuid, int state){}

	// dmr callback function mediatype: -1:unknown 0:video 1:audio 2: picture
	public int OnSetURI(String url, String urltitle, String remoteip, int mediatype) {
		LogUtil.info(TAG, String.format(Locale.US,
                "Java: dlna OnSetURI() url: %s, title %s, remoteip %s, mediatype %d",
				url, urltitle, remoteip, mediatype));
		if (mCallback != null)
			mCallback.OnSetURI(url, urltitle, remoteip, mediatype);
			
		return 0;
	}
	
	public void OnPlay() {
        LogUtil.info(TAG, "Java: dlna OnPlay()");

        if (mCallback != null)
            mCallback.OnPlay();
	}
	
	public void OnPause() {
        LogUtil.info(TAG, "Java: dlna OnPause()");

        if (mCallback != null)
            mCallback.OnPause();
	}
	
	public void OnStop() {
        LogUtil.info(TAG, "Java: dlna OnStop()");

        if (mCallback != null)
            mCallback.OnStop();
	}
	
	public void OnSeek(long position) {
        LogUtil.info(TAG, "Java: dlna OnSeek pos: " + position);

        if (mCallback != null)
            mCallback.OnSeek(position);
	}
	
	public void OnSetVolume(long volume) {
        LogUtil.info(TAG, "Java: dlna OnSetVolume vol: " + volume);

        if (mCallback != null)
            mCallback.OnSetVolume(volume);
    }
	public void OnSetMute(boolean mute) {
        LogUtil.info(TAG, "Java: dlna OnSetMute: " + mute);

        if (mCallback != null)
            mCallback.OnSetMute(mute);
    }

	// dmc callback function
	public void OnVolumeChanged(String uuid, long lVolume) {

    }
	public void OnMuteChanged(String uuid, boolean bMute) {

    }

	public void OnPlayStateChanged(String uuid, String state) {
        LogUtil.info(TAG, "Java: OnPlayStateChanged uuid: " + uuid + " , state: " + state);

        if (mCallback != null)
            mCallback.OnPlayStateChanged(uuid, state);
    }

	public void OnPlayUrlChanged(String uuid, String url) {
        LogUtil.info(TAG, "Java: OnPlayUrlChanged uuid: " + uuid + " , url: " + url);

        if (mCallback != null)
            mCallback.OnPlayUrlChanged(uuid, url);
    }

    public void OnContainerChanged(String uuid, String item_id, String update_id) {

    }

    public void OnGetCaps(String uuid, String caps) {

    }

    public void OnSetUrl(String uuid, long error) {
        LogUtil.info(TAG, "Java: OnSetUrl uuid: " + uuid + " , error: " + error);

        if (mCallback != null)
            mCallback.OnSetUrl(uuid, error);
    }

    public void OnBrowse(boolean success,
			String uuid, String objectid, long count, long total, DLNASdkDMSItemInfo[] filelists) {
	}
}
