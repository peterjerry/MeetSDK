package com.pplive.epg;

import java.util.ArrayList;

public class TestEPG { 
	
	private static void search(EPGUtil epg) {
		String key = "泰坦尼克";
		String type = "0";
		
		boolean ret;
		
		ArrayList<PlayLink2> list = null;
		System.out.println("step 1");
		ret = epg.search(key, type, "0", "10");
		if(!ret)
			return;
		
		list = epg.getLink();
		if(list.size() < 1)
			return;
		
		for(int i=0;i<list.size();i++) {
			PlayLink2 l = list.get(i);
			System.out.println(l.toString());
		}
		
		PlayLink2 l = list.get(0);
		String id = l.getId();
		System.out.println("step 2: " + id);
		
		while (true) {
			
			ret = epg.collection(id);
			if (!ret)
				break;
			
			list = epg.getLink();
			
			for (int i=0;i<list.size();i++) {
				PlayLink2 l2 = list.get(i);
				System.out.println(l2.toString());
			}
			
			if (list.size() == 1)
				break;
			
			PlayLink2 first = list.get(0);
			id = first.getId();
		}
	}
	
	private static void frontpage(EPGUtil epg) {
		boolean ret;
		
		ret = epg.frontpage();
		if(!ret)
			return;
		
		ArrayList<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		ret = epg.catalog(4);
		if(!ret)
			return;
		
		ArrayList<Catalog> catloglist = epg.getCatalog();
		for(int i=0;i<catloglist.size();i++) {
			System.out.println(catloglist.get(i).toString());
		}
		
		String vid = catloglist.get(4).getVid();
		
		ArrayList<PlayLink2> playlink2list = null;
		
		while (true) {
			ret = epg.collection(vid);
			if (!ret)
				break;
			
			playlink2list = epg.getLink();
			for(int i=0;i<playlink2list.size();i++) {
				System.out.println(playlink2list.get(i).toString());
			}
			
			if (playlink2list.size() > 0) {
				PlayLink2 first = playlink2list.get(0);
				vid = first.getId();
				
				if (playlink2list.size() == 1) { 
					String cdn_url = epg.getCDNUrl(vid, "1", false, false);
					if (cdn_url != null)
						System.out.println("vid: " + vid + " , url: " + cdn_url);
					break;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		
		EPGUtil epg = new EPGUtil();
		
		if (false) {
			frontpage(epg);
		}
		else {
			search(epg);
		}
	}
}
