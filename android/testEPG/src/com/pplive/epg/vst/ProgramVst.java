package com.pplive.epg.vst;

import java.util.List;

public class ProgramVst {
	private int 			m_id;
	private String 			m_name;
	private String			m_area;
	private String			m_quality;
	private List<String> 	m_url_list;
	
	@SuppressWarnings("unused")
	private ProgramVst() {
		
	}
	
	public ProgramVst(int id, String name, String area, String quality, List<String> url_list) {
		m_id		= id;
		m_name		= name;
		m_area		= area;
		m_quality	= quality;
		m_url_list	= url_list;
	}
	
	public int getId() {
		return m_id;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getArea() {
		return m_area;
	}
	
	public String getQuality() {
		return m_quality;
	}
	
	public List<String> getUrlList() {
		return m_url_list;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID: ");
		sb.append(m_id);
		sb.append(" , 名称: ");
		sb.append(m_name);
		sb.append(" , 地区: ");
		sb.append(m_area);
		sb.append(" , 质量: ");
		sb.append(m_quality);
		sb.append(" , 播放地址: ");
		for (int i=0;i<m_url_list.size();i++) {
			sb.append(m_url_list.get(i));
			sb.append("###");
		}
		
		return sb.toString();
	}
}

