package com.gotye.common.sohu;

public class LiveChannelSohu {
	private String m_title;
	private int 	m_tvId;
	private String m_nowPlay;
	private String m_willPlay;
	private String m_smallPic;
	private String m_bigPic;
	private String m_liveUrl;
	
	@SuppressWarnings("unused")
	private LiveChannelSohu() {
		
	}
	
	public LiveChannelSohu(int id, String title, String nowPlay, String willPlay, 
			String smallPic, String bigPic, String url) {
		m_tvId		= id;
		m_title		= title;
		m_nowPlay	= nowPlay;
		m_willPlay	= willPlay;
		m_smallPic	= smallPic;
		m_bigPic	= bigPic;
		m_liveUrl	= url;
	}
	
	public String getTitle() {
		return m_title;
	}
	
	public String getNowPlay() {
		return m_nowPlay;
	}
	
	public String getWillPlay() {
		return m_willPlay;
	}
	
	public String getUrl() {
		return m_liveUrl;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ID: ");
		sb.append(m_tvId);
		sb.append(" , 标题: ");
		sb.append(m_title);
		sb.append(" , 当前: ");
		sb.append(m_nowPlay);
		sb.append(" , 即将: ");
		sb.append(m_willPlay);
		sb.append(" , 播放url: ");
		sb.append(m_liveUrl);
		
		return sb.toString();
	}
}
