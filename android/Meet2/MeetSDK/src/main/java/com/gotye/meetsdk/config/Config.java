/** Automatically generated file. DO NOT MODIFY */
package com.gotye.meetsdk.config;

import com.gotye.meetsdk.BuildConfig;

public class Config {
	
	private final static String VERSION_NUMBER = "unspecified";
	
	//private final static String START_P2P = "false";
	
	public static String getVersion() {
		return VERSION_NUMBER;
	}
	
	public static boolean needStartP2P() {
		return BuildConfig.START_P2P;
	}
}
