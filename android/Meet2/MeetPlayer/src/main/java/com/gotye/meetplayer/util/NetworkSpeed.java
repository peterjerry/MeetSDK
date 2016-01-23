package com.gotye.meetplayer.util;

import android.net.TrafficStats;

public class NetworkSpeed {
	private final static String TAG = "NetworkSpeed";
	
	private long mBeforeTime;
	private long mTotalTxBeforeTest;
	private long mTotalRxBeforeTest;
	
	public NetworkSpeed() {
		this.mBeforeTime = System.currentTimeMillis();
		this.mTotalTxBeforeTest = TrafficStats.getTotalTxBytes();
		this.mTotalRxBeforeTest = TrafficStats.getTotalRxBytes();
	}
	
	// kB/s
	public int[] currentSpeed() {
		/* DO WHATEVER NETWORK STUFF YOU NEED TO DO */

		long TotalTxAfterTest = TrafficStats.getTotalTxBytes();
		long TotalRxAfterTest = TrafficStats.getTotalRxBytes();
		long AfterTime = System.currentTimeMillis();

		double TimeDifference = AfterTime - mBeforeTime;

		double rxDiff = TotalRxAfterTest - mTotalRxBeforeTest;
		double txDiff = TotalTxAfterTest - mTotalTxBeforeTest;

		if ((rxDiff != 0) && (txDiff != 0)) {
			double rxBPS = rxDiff / TimeDifference; // total rx bytes per second.
			double txBPS = txDiff / TimeDifference; // total tx bytes per second.
		    //LogUtil.info(TAG, String.format("rx %.2f kB/s(Total %.0f)", rxBPS, rxDiff));
		    //LogUtil.info(TAG, String.format("tx %.2f kB/s(Total %.0f)", txBPS, txDiff));
		    
		    int []values = new int[2];
		    values[0] = (int)rxBPS;
		    values[1] = (int)txBPS;
		    return values;
		}
		
		return null;
	}
	
	/*private xxxx() {
		String host = YOUR_HOST
		HttpGet request = new HttpGet(host);
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);

		for(int i=0; i<5; i++) {
		    long BeforeTime = System.currentTimeMillis();
		    HttpResponse response = httpClient.execute(request);
		    long AfterTime = System.currentTimeMillis();
		    Long TimeDifference = AfterTime - BeforeTime;
		    time[i] = TimeDifference 
		}
	}*/
}
