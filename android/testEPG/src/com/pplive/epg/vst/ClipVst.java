package com.pplive.epg.vst;

public class ClipVst {
	private String 			m_name;
	private String			m_uuid;
	
	@SuppressWarnings("unused")
	private ClipVst() {
		
	}
	
	public ClipVst(String name, String uuid) {
		m_name		= name;
		m_uuid		= uuid;
	}
	
	public String getUUID() {
		return m_uuid;
	}
	
	public String getName() {
		return m_name;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("名称: ");
		sb.append(m_name);
		sb.append("uuid: ");
		sb.append(m_uuid);
		
		return sb.toString();
	}
}

