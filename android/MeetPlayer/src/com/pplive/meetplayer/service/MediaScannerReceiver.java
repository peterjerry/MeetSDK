package com.pplive.meetplayer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaScannerReceiver extends BroadcastReceiver {
	static final String TAG = "MediaScannerReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (MediaScannerService.ACTION_MEDIA_MOUNTED.equals(action) ||
			MediaScannerService.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action)) {
			
			Log.i(TAG, "ACTION_MEDIA_MOUNTED");
			//context.startService(new Intent(context, MediaScannerService.class));
		}
	}
}
