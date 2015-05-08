package com.pplive.epg;

public class ProgramItemlb {
	private int 	m_id;
	private String m_title;
	private String m_playtime;
	private String m_viewpic;
	
	@SuppressWarnings("unused")
	private ProgramItemlb() {
		
	}
	
	public ProgramItemlb(int id, String title, String playtime, String viewpic) {
		m_id		= id;
		m_title		= title;
		m_playtime	= playtime;
		m_viewpic	= viewpic;
	}
	
	public String getTitle() {
		return m_title;
	}
	
	public String getPlaytime() {
		return m_playtime;
	}
	
	public String getViewPic() {
		return m_viewpic;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID: ");
		sb.append(m_id);
		sb.append(" , 标题: ");
		sb.append(m_title);
		sb.append(" , 播放时间: ");
		sb.append(m_playtime);
		sb.append(" , 图片预览: ");
		sb.append(m_viewpic);
		
		return sb.toString();
	}
}
