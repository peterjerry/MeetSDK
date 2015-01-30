package com.pplive.meetplayer.util;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.pplive.dlna.DLNASdk;
import com.pplive.dlna.DLNASdkDMSItemInfo;

public class IDlnaCallback implements DLNASdk.DLNASdkInterface {
	private final static String TAG = "dlna";
	
	public Map<String, String> mDeviceMap = new HashMap<String,String>();
	public void OnDeviceAdded(String uuid, String firendname, String logourl, int devicetype)
	{
		Log.i(TAG, "Java: dlna [add] uuid: "+ uuid +", name:" + firendname);
		
		/*String uuid_device = "3fb202d6-142e-4e49-8b4d-f9f236f68db5";
		if (uuid.equalsIgnoreCase(uuid_device)) {
			Log.i(TAG, "Java: find dev!!!");
		}*/
		
		if (devicetype == 1)
			mDeviceMap.put(uuid,firendname); 
	}

	
	
	public void OnDeviceRemoved(String uuid, int devicetype)
	{
		Log.i(TAG, "Java: dlna [remove] uuid:" + uuid);
		if (devicetype == 1)
			mDeviceMap.remove(uuid); 
	}
	
	public void OnLogPrintf(String msg)
	{
		Log.i("dlna_print", msg);
	}

	//////////////////////////////////////////////////////////////////////////
	// mediashaker callback function
	public boolean OnConnect(String uuid, String requestName){return true;}
	public void OnConnectCallback(String uuid, int state){}
	public void OnDisConnect(String uuid){}
	public void OnDisConnectCallback(String uuid, boolean isTimeout){}
	public void OnRemoveTransportFile(String uuid, String transportuuid){}
	public void OnRemoveTransportFileCallback(String uuid, String transportuuid, boolean isTimeout){}
	public void OnAddTransportFile(String uuid, String transportuuid, String fileurl, String filename, String thumburl){}
	public void OnAddTransportFileCallback(String uuid, String transportuuid, int state){}

	// dmr callback function mediatype: -1:unknown 0:video 1:audio 2: picture
	public int OnSetURI(String url, String urltitle, String remoteip, int mediatype){return 0;}
	public void OnPlay(){}
	public void OnPause(){}
	public void OnStop(){}
	public void OnSeek(long position){}
	public void OnSetVolume(long volume){}
	public void OnSetMute(boolean mute){}

	// dmc callback function
	public void OnVolumeChanged(String uuid, long lVolume){}
	public void OnMuteChanged(String uuid, boolean bMute){}
	public void OnPlayStateChanged(String uuid, String state){}
	public void OnPlayUrlChanged(String uuid, String url){}
	public void OnContainerChanged(String uuid, String item_id, String update_id){}
	public void OnGetCaps(String uuid, String caps){}
	public void OnSetUrl(String uuid, long error){}
	public void OnBrowse(boolean success, String uuid, String objectid, long count, long total, DLNASdkDMSItemInfo[] filelists){}
}
