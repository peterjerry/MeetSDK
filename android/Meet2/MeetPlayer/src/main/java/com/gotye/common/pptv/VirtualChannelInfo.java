package com.gotye.common.pptv;

public class VirtualChannelInfo {
	
	// site logo="http://img3.pplive.cn/images/2013/04/25/14482681748.png" 
	// title="搜狐" siteid="3" total="714" denydownload="1" mode="3">
	private String m_title;
	private String m_logo_url;
	private int m_siteid;
	private int m_infoid;
	private int m_total;
	private int m_mode;
	
	@SuppressWarnings("unused")
	private VirtualChannelInfo() {
		
	}
	
	public VirtualChannelInfo(String title, String logo_url, 
			int siteid, int infoid, int total, int mode) {
		m_title		= title;
		m_logo_url	= logo_url;
		m_siteid	= siteid;
		m_infoid	= infoid;
		m_total		= total;
		m_mode		= mode;
	}

	public String getTitle() {
		return m_title;
	}
	
	public int getSiteId() {
		return m_siteid;
	}
	
	public int getTotal() {
		return m_total;
	}
	
	public int getInfoId() {
		return m_infoid;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("标题: ");
		sb.append(m_title);
		sb.append(", 图标: ");
		sb.append(m_logo_url);
		sb.append(", siteid: ");
		sb.append(m_siteid);
		sb.append(", infoid: ");
		sb.append(m_infoid);
		sb.append(", 数量: ");
		sb.append(m_total);
		
		return sb.toString();
	}
}
