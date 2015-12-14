package com.pplive.epg.baidu;

public class LoginInfo {
	private String token;
	private String BDUSS;
	
	public LoginInfo(String token, String BDUSS) {
		this.token = token;
		this.BDUSS = BDUSS;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getBDUSS() {
		return BDUSS;
	}
}
