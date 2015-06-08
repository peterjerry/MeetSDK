package com.pplive.epg;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.JFrame;

import com.pplive.epg.bestv.BestvFrame;
import com.pplive.epg.bestv.BestvKey;
import com.pplive.epg.bestv.BestvUtil;
import com.pplive.epg.letv.LeTVFrame;
import com.pplive.epg.pptv.Catalog;
import com.pplive.epg.pptv.Content;
import com.pplive.epg.pptv.EPGUtil;
import com.pplive.epg.pptv.Module;
import com.pplive.epg.pptv.PPTVFrame;
import com.pplive.epg.pptv.PlayLink2;
import com.pplive.epg.sohu.SohuFrame;
import com.pplive.epg.vst.VstFrame;

public class TestEPG { 
	
	public static void main(String[] args) {

		//PPTVFrame myFrame = new PPTVFrame();
		LeTVFrame myFrame = new LeTVFrame();
		//VstFrame myFrame = new VstFrame();
        //BestvFrame myFrame = new BestvFrame();
		//SohuFrame myFrame = new SohuFrame();
		myFrame.setVisible(true);
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myFrame.addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				System.out.println("aaaaaa");
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					System.exit(0);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		/*EPGUtil epg = new EPGUtil();
		
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
		}*/
	}
	
	private static void search(EPGUtil epg) {
		String key = "泰坦尼克";
		int type = 0;
		int content_type = 0;
		
		boolean ret;
		
		List<PlayLink2> list = null;
		System.out.println("step 1");
		ret = epg.search(key, type, content_type, 1, 10);
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
		
		List<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		ret = epg.catalog(18);
		if(!ret)
			return;
		
		List<Catalog> catloglist = epg.getCatalog();
		for(int i=0;i<catloglist.size();i++) {
			System.out.println(catloglist.get(i).toString());
		}
		
		String vid = catloglist.get(3).getVid();
		if (vid == null) {
			System.out.println("vid is null");
			return;
		}
		
		List<PlayLink2> playlink2list = null;
		
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

		List<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		String prefix = "app://aph.pptv.com/v4/cate/live";
		ret = epg.contents(prefix);
		if(!ret)
			return;
		
		List<Content> contentlist = epg.getContent();
		for(int i=0;i<contentlist.size();i++) {
			System.out.println(contentlist.get(i).toString());
		}
		
		ret = epg.live(1, 15, 164); // 156 164
		if (!ret)
			return;
		
		List<PlayLink2> playlink2list = null;
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

		List<Module> modulelist = epg.getModule();
		for(int i=0;i<modulelist.size();i++) {
			System.out.println(modulelist.get(i).toString());
		}
		
		String link = modulelist.get(7).getLink(); //"app://aph.pptv.com/v4/cate/tv";
		
		// save "type" for list()
		String type = "";
		int pos = link.indexOf("type=");
		if (pos != -1) {
			type = link.substring(pos, link.length());
		}
		
		ret = epg.contents(link);
		if(!ret)
			return;
		
		List<Content> contentlist = epg.getContent();
		for(int i=0;i<contentlist.size();i++) {
			System.out.println("#" + i + ": " + contentlist.get(i).toString());
		}
		
		String param = contentlist.get(5).getParam();
		if (param.startsWith("type="))
			type = "";
		
		System.out.println(String.format("param: %s, type %s", param, type));
		
		ret = epg.list(param, type, 2, "order=n", 10); // "美国:area|"
		if (!ret)
			return;
		
		List<PlayLink2> playlink2list = null;
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
	
}
