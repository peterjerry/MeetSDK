/** Automatically generated file. DO NOT MODIFY */
package com.gotye.meetsdk.config;

public class Config {
	
	private final static String VERSION_NUMBER = "unspecified";
	
	private final static String START_P2P = "false";
	
	
	public static String getVersion() {
		return VERSION_NUMBER;
	}
	
	public static boolean needStartP2P() {
		return "true".equalsIgnoreCase(START_P2P);
	}
}
