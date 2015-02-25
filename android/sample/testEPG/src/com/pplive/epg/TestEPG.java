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
		
		PlayLink2 l = list.get(1);
		String id = l.getId();
		System.out.println("step 2: " + id);
		
		while (true) {
			
			ret = epg.detail(id);
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
			ret = epg.detail(vid);
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
					/*String cdn_url = epg.getCDNUrl(vid, "1", false, false);
					if (cdn_url != null)
						System.out.println("vid: " + vid + " , url: " + cdn_url);*/
					break;
				}
			}
		}
	}
	
	private static void live(EPGUtil epg) {
		boolean ret;
		
		ret = epg.contents_list();
		if (!ret)
			return;

		ArrayList<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		String prefix = "app://aph.pptv.com/v4/cate/live";
		ret = epg.contents(prefix);
		if(!ret)
			return;
		
		ArrayList<Content> contentlist = epg.getContent();
		for(int i=0;i<contentlist.size();i++) {
			System.out.println(contentlist.get(i).toString());
		}
		
		ret = epg.live(30, 164); // 156 164
		if (!ret)
			return;
		
		ArrayList<PlayLink2> playlink2list = null;
		playlink2list = epg.getLink();
		for(int i=0;i<playlink2list.size();i++) {
			System.out.println(playlink2list.get(i).toString());
		}
	}
	
	private static void contents(EPGUtil epg) {
		boolean ret;
		
		ret = epg.contents_list();
		if (!ret)
			return;

		ArrayList<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		String prefix = modulelist.get(3).getLink();//"app://aph.pptv.com/v4/cate/tv";
		String type = "";
		int pos = prefix.indexOf("type=");
		if(pos != -1) {
			type = prefix.substring(pos, prefix.length());
		}
		
		System.out.println("prefix " + prefix);
		ret = epg.contents(prefix);
		if(!ret)
			return;
		
		ArrayList<Content> contentlist = epg.getContent();
		for(int i=0;i<contentlist.size();i++) {
			System.out.println(contentlist.get(i).toString());
		}
		
		String param = contentlist.get(19).getParam();
		
		System.out.println("param: " + param);
		System.out.println("type: " + type);
		ret = epg.list(param, type); // "美国:area|"
		if (!ret)
			return;
		
		ArrayList<PlayLink2> playlink2list = null;
		playlink2list = epg.getLink();
		for(int i=0;i<playlink2list.size();i++) {
			System.out.println(playlink2list.get(i).toString());
		}
		
		/*String vid = playlink2list.get(2).getId();
		
		while (true) {
			ret = epg.detail(vid);
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
					break;
				}
			}
		}*/
	}
	
	public static void main(String[] args) {
		
		EPGUtil epg = new EPGUtil();
		
		int type = 2;
		switch(type) {
		case 0:
			frontpage(epg);
			break;
		case 1:
			search(epg);
			break;
		case 2:
			contents(epg);
			break;
		case 3:
			live(epg);
			break;
		default:
			System.out.println("unknown type: " + type);
			break;
		}
	}
}
