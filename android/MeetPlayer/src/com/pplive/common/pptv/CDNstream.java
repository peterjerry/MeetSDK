package com.pplive.common.pptv;

public class CDNstream {
	public String m_ft;
	public String m_rid;
	
	public String m_format;
	public int	m_width;
	public int	m_height;
	public int	m_bitrate;
	
	@SuppressWarnings("unused")
	private CDNstream() {
		
	}
	
	public CDNstream(String ft, String rid) {
		this(ft, rid, "", 0, 0, 0);
	}
	
	public CDNstream(String ft, String rid, String format, int width, int height, int bitrate) {
		m_ft		= ft;
		m_rid		= rid;
		
		m_format	= format;
		m_width		= width;
		m_height	= height;
		m_bitrate	= bitrate;
	}

}
