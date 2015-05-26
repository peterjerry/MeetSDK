package com.pplive.epg.bestv;

import java.util.Date;
import java.util.GregorianCalendar;

public class BestvKey {
	private int expiration_time;	// 1432150607702
	private String key; 				// 3910F85C71F674CECF7742D8C3054FAE330FC5A3A62C519E127559318202683D
	private String update_time; 		// 2015-05-20 17:36:47
	private int x_user_id;		// 379704
	
	BestvKey(int exp_time, String key, String update_time, int user_id) {
		this.expiration_time	= exp_time;
		this.key				= key;
		this.update_time		= update_time;
		this.x_user_id			= user_id;
	}
	
	String getKey() {
		return key;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		Date dat=new Date(expiration_time);  
        GregorianCalendar gc = new GregorianCalendar();   
        gc.setTime(dat);  
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
        
		StringBuffer sb = new StringBuffer();
		sb.append("过期时间: ");
		sb.append(expiration_time);
		sb.append("(");
		sb.append(gc.getTime().toString());
		sb.append(") ， key: ");
		sb.append(key);
		sb.append(" , 更新时间: ");
		sb.append(update_time);
		sb.append(" , 用户id");
		sb.append(x_user_id);
		
		return sb.toString();
	}
}
