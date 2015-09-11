package com.pplive.epg.pptv;

import java.util.Date;

public class CDNItem {
	private String	m_ft;
	private int	m_width;
	private int	m_height;
	
	private String m_sh;
	private String m_st;
	private String m_bh;
	private String m_rid;
	
	private String m_key;
	
	@SuppressWarnings("unused")
	private CDNItem() {
		
	}
	
	public CDNItem(String ft, int width ,int height, 
			String sh, String st, String bh, String key, String rid) {
		m_ft			= ft;
		m_width			= width;
		m_height		= height;
		
		m_sh			= sh;
		m_st			= st;
		m_bh			= bh;
		m_key			= key;
		m_rid			= rid;
	}
	
	@SuppressWarnings("deprecation")
	public String generateK() {
		return Key.getKey(new Date(m_st).getTime());
	}
	
	public String getFT() {
		return m_ft;
	}
	
	public int getWidth() {
		return m_width;
	}
	
	public int getHeight() {
		return m_height;
	}
	
	public String getKey() {
		return m_key;
	}
	
	public String getHost() {
		return m_sh;
	}
	
	public String getST() {
		return m_st;
	}
	
	public String getRid() {
		return m_rid;
	}
}
