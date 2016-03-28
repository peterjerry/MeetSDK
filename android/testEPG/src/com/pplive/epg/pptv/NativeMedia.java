package com.pplive.epg.pptv;

public class NativeMedia {
	
	public static native int test(String instring);
	
	/**
	 * @param [in]in_flv 输入flv文件内容
	 * @param [in]in_size 输入flv文件大小
	 * @param [out]out_ts 输出mpegts文件内容
	 * @param process_timestamp 是否处理时间戳
	 * @param first_seg 是否是第一个分段(仅当 process_timestamp为1有效)
	 * @return
	 */
	public static native int Convert(byte[] in_flv, int in_size, byte[] out_ts, 
			int process_timestamp, int first_seg);
	
	static {
		try {
			System.loadLibrary("libMedia");
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
    }
}
