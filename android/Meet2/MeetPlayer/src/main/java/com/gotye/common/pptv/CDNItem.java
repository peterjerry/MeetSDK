package com.gotye.common.pptv;

import java.util.Date;

public class CDNItem {
	private String	m_ft;
	private int	m_width;
	private int	m_height;
	private String m_format;
	private int m_bitrate;
	
	private int m_interval;
	private int m_delay;
	
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
		this(ft, width, height, "", 0, 60, 5,
				sh, st, bh, key, rid);
	}
	
	public CDNItem(String ft, int width ,int height, 
			String format, int bitrate,
			int delay, int interval,
			String sh, String st, String bh, String key, String rid) {
		m_ft			= ft;
		m_width			= width;
		m_height		= height;
		m_format		= format;
		m_bitrate		= bitrate;
		
		m_delay			= delay;
		m_interval		= interval;
		
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
	
	public String getFormat() {
		return m_format;
	}
	
	public int getBitrate() {
		return m_bitrate;
	}
	
	public int getDelay() {
		return m_delay;
	}
	
	public int getInterval() {
		return m_interval;
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
