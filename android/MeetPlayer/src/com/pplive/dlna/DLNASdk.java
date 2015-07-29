package com.pplive.dlna;

import android.util.Log;

public class DLNASdk
{
	private boolean mIsLoadSuccess = true;
	public DLNASdk() 
	{
		try {
			System.loadLibrary("dlna");
		}
		catch (Throwable e)
		{
			Log.e("DLNASdk_jni", e.toString());
			mIsLoadSuccess = false;
		}
	}
	
	public boolean isLibLoadSuccess() {return mIsLoadSuccess;}
    public native void Init(DLNASdkInterface callback);
	public native void UnInit();
	public native boolean IsInitOK();
	public native void RefreshDeviceList();
    public native void setLogPath(String logPath);

	//////////////////////////////////////////////////////////////////////////
	// media shaker
	public native void EnableMediaShaker(boolean enable, String friendlyName, String uuid, String logoPath);
	public native void DisConnect(String uuid);
	public native void Connect(String uuid);
	public native void AddTransportFile(String uuid, String transportuuid, String filepath, String filename, String thumbfilepath, boolean isfilepath);
	public native void RemoveTransportFile(String uuid, String transportuuid);
	public native DLNASdkMTFileInfo GetTransportFileInfo(String uuid, String transportuuid);
	public native void UpdateTransportInfo(String transportuuid, DLNASdkMTFileInfo info);

	//////////////////////////////////////////////////////////////////////////
	// dmc for dmr or dms
	public native void EnableRendererControler(boolean enable);
	public native void EnableServerControler(boolean enable);
	
	public native long GetVolume(String uuid);
	public native boolean GetMute(String uuid);
	public native String GetTransportState(String uuid);
	public native long GetPosition(String uuid);
	public native long GetTotalTime(String uuid);
	public native String GetDeviceCaps(String uuid);
	public native String GetProtocols(String uuid);
	public native String GetMediaUri(String uuid);

	public native void Pause(String uuid);
	public native void Play(String uuid);
	public native void Stop(String uuid);
	public native void SetURI(String uuid, String uri);
	public native void Seek(String uuid, long lPosition);
	public native void SetVolume(String uuid, long lVolume);
	public native void SetMute(String uuid, boolean bMute);
	public native boolean Browse(String uuid, String objectId, long startIndex, long countMax, boolean forcerefresh);
	
	//////////////////////////////////////////////////////////////////////////
	// dmr function
	public native void EnableRenderer(boolean enable, String friendlyName, String uuid, String caps, String logoPath);
	public native void Play();
	public native void Pause();
	public native void Stop();
	public native void Seek(long reltime);
	public native void SetVolume(long lVolume);
	public native void SetMute(boolean bMute);
	public native void SetTotalTime(long totaltime);

	//////////////////////////////////////////////////////////////////////////
	// dms function
	public native void EnableServer(boolean enable, String friendlyName, String uuid, String logoPath);
	public native void AddVirtualPath(String path);
	public native void RemoveVirtualFath(String path);
	public native String GetVirtualPathUrl(String filepath);

	//////////////////////////////////////////////////////////////////////////
	// http fileserver
	public native void StartHttpServer(int port);
	public native void StopHttpServer();
	public native String GetServerFileUrl(String filepath);

	public interface DLNASdkInterface
	{
		//////////////////////////////////////////////////////////////////////////
		// device online callback function
		public void OnDeviceAdded(String uuid, String firendname, String logourl, int devicetype);
		public void OnDeviceRemoved(String uuid, int devicetype);
		public void OnLogPrintf(String msg);

		//////////////////////////////////////////////////////////////////////////
		// mediashaker callback function
		public boolean OnConnect(String uuid, String requestName);
		public void OnConnectCallback(String uuid, int state);
		public void OnDisConnect(String uuid);
		public void OnDisConnectCallback(String uuid, boolean isTimeout);
		public void OnRemoveTransportFile(String uuid, String transportuuid);
		public void OnRemoveTransportFileCallback(String uuid, String transportuuid, boolean isTimeout);
		public void OnAddTransportFile(String uuid, String transportuuid, String fileurl, String filename, String thumburl);
		public void OnAddTransportFileCallback(String uuid, String transportuuid, int state);

		// dmr callback function mediatype: -1:unknown 0:video 1:audio 2: picture
		public int OnSetURI(String url, String urltitle, String remoteip, int mediatype);
		public void OnPlay();
		public void OnPause();
		public void OnStop();
		public void OnSeek(long position);
		public void OnSetVolume(long volume);
		public void OnSetMute(boolean mute);

		// dmc callback function
		public void OnVolumeChanged(String uuid, long lVolume);
		public void OnMuteChanged(String uuid, boolean bMute);
		public void OnPlayStateChanged(String uuid, String state);
		public void OnPlayUrlChanged(String uuid, String url);
		public void OnContainerChanged(String uuid, String item_id, String update_id);
		public void OnGetCaps(String uuid, String caps);
		public void OnSetUrl(String uuid, long error);
		public void OnBrowse(boolean success, String uuid, String objectid, long count, long total, DLNASdkDMSItemInfo[] filelists);		
	}
}

