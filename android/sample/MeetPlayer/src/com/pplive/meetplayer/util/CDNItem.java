package com.pplive.meetplayer.util;

import java.util.Date;

public class CDNItem {
	private String m_sh;
	private String m_st;
	private String m_bh;
	private String m_ft;
	
	private String m_key;
	
	@SuppressWarnings("unused")
	private CDNItem() {
		
	}
	
	public CDNItem(String ft, String sh, String st, String bh, String key) {
		m_ft			= ft;
		m_sh			= sh;
		m_st			= st;
		m_bh			= bh;
		m_key			= key;
	}
	
	@SuppressWarnings("deprecation")
	public String getK() {
		return Key.getKey(new Date(m_st).getTime());
	}
	
	public String getKey() {
		return m_key;
	}
	
	public String getFT() {
		return m_ft;
	}
	
	public String getHost() {
		return m_sh;
	}
}
