package com.pplive.epg.letv;

public class Programlb {
	private int 	m_id;
	private String m_name;
	private String m_area;
	private String m_epg_id;
	private String m_stream_id;
	
	@SuppressWarnings("unused")
	private Programlb() {
		
	}
	
	public Programlb(int id, String name, String area, String epg_id, String stream_id) {
		m_id		= id;
		m_name		= name;
		m_area		= area;
		m_epg_id	= epg_id;
		m_stream_id	= stream_id;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getEPGId() {
		return m_epg_id;
	}
	
	public String getStreamId() {
		return m_stream_id;
	}
	
	public String getArea() {
		return m_area;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID: ");
		sb.append(m_id);
		sb.append(" , 名称: ");
		sb.append(m_name);
		sb.append(" , 区域: ");
		sb.append(m_area);
		sb.append(" , epg_id: ");
		sb.append(m_epg_id);
		sb.append(" , stream_id: ");
		sb.append(m_stream_id);
		
		return sb.toString();
	}
}
