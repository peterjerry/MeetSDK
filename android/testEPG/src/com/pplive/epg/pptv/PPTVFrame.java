package com.pplive.epg.pptv;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.swing.event.*;

public class PPTVFrame extends JFrame {
	
	private EPG_STATE mState = EPG_STATE.EPG_STATE_IDLE;
	private EPGUtil mEPG;
	private List<Content> mContentList;
	private List<Module> mModuleList;
	private List<Catalog> mCatalogList;
	private List<PlayLink2> mPlayLinkList;
	private List<Navigator> mNavList;
	private String mContentType;
	
	private int start_page = 1;
	private int last_live_type = 0;
	
	private boolean mListLive = false;
	
	private final static int PAGE_NUM = 10;
	
	private enum EPG_STATE {
		EPG_STATE_IDLE,
		EPG_STATE_ERROR,
		
		EPG_STATE_FRONTPAGE,
		EPG_STATE_CATALOG,
		EPG_STATE_DETAIL,
		EPG_STATE_LINK,
		
		EPG_STATE_CONTENT_LIST,
		EPG_STATE_CONTENT,
		
		EPG_STATE_FOUND_PLAYLINK,
		EPG_STATE_LIST,
		EPG_STATE_SEARCH,
	}
	
	private final static String[] ft_desc = {"流畅","高清","超清","蓝光"};
	
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("选择");
	JButton btnNext 	= new JButton("翻页");
	
	JLabel lblInfo = new JLabel("信息");
	
	JLabel lbl_link = new JLabel("链接");
	JTextPane editorPlayLink = new JTextPane();
	
	JComboBox<String> comboItem 	= null;
	JComboBox<String> comboFt 		= null;
	JComboBox<String> comboBwType 	= null;
	JList<String> lstType 			= null;
	
	JCheckBox cbNoVideo = new JCheckBox("NoVideo");
	
	JTextPane editorSearch = new JTextPane();
	JButton btnSearch = new JButton("search");
	
	JLabel lbl_day = new JLabel("日期");
	JLabel lbl_start_time = new JLabel("开始");
	JLabel lbl_duration = new JLabel("时长");
	
	JComboBox<String> comboDay 		= null;
	JComboBox<String> comboHour 	= null;
	JComboBox<String> comboDuration	= null;
	
	JTextPane editorPlayExe = new JTextPane();

	public PPTVFrame() {
		super();
		
		mEPG = new EPGUtil();
		
		this.setTitle("PPTV 电视节目");
		this.setBounds(400, 300, 500, 600);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				System.exit(0);
			}
		});
		
		Font f = new Font("宋体", 0, 20);

		this.getContentPane().setLayout(null);
		// Action
		lblInfo.setBounds(20, 20, 300, 40);
		lblInfo.setFont(f);
		this.getContentPane().add(lblInfo);
		
		btnGo.setFont(f);
		btnGo.setBounds(180, 110, 80, 40);
		this.getContentPane().add(btnGo);
		btnReset.setFont(f);
		btnReset.setBounds(260, 110, 80, 40);
		this.getContentPane().add(btnReset);
		btnNext.setFont(f);
		btnNext.setBounds(330, 110, 80, 40);
		this.getContentPane().add(btnNext);
		
		lbl_link.setFont(f);
		lbl_link.setBounds(20, 110, 40, 40);
		this.getContentPane().add(lbl_link);
		
		editorPlayLink.setFont(f);
		editorPlayLink.setBounds(60, 110, 100, 40);
		editorPlayLink.setText("20986187");
	    this.getContentPane().add(editorPlayLink);

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				init_combobox();
			}
		});
		
		btnNext.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				start_page++;
				selectList();
			}
		});
		
		btnGo.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				switch (mState) {
				case EPG_STATE_FRONTPAGE:
					selectCatalog();
					break;
				case EPG_STATE_CATALOG:
					selectDetail();
					break;
				case EPG_STATE_CONTENT_LIST:
					selectContents();
					break;
				case EPG_STATE_DETAIL:
				case EPG_STATE_LIST:
				case EPG_STATE_LINK:
				case EPG_STATE_SEARCH:	
					selectLink();
					break;
				case EPG_STATE_FOUND_PLAYLINK:
					//JOptionPane.showMessageDialog(null, "playlink already found!");
					playvideo();
					break;
				case EPG_STATE_CONTENT:
					selectList();
					break;
				default:
					System.out.println("invalid state: " + mState.toString());
					break;
				}
				
			}
		});
		
		comboItem = new JComboBox<String>();
		comboItem.setFont(f);
		comboItem.setBounds(20, 60, 450, 40);
		comboItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub	
			}
		});
		
		init_combobox();
		
		comboItem.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
			}

		});
		
		this.getContentPane().add(comboItem);
		
		comboFt = new JComboBox<String>(ft_desc);
		comboFt.setFont(f);
		comboFt.setBounds(20, 170, 80, 40);
		comboFt.setSelectedIndex(1);
		this.getContentPane().add(comboFt);
		
		String[] bw_type = {"P2P", "CDNP2P", "CDN", "PPTV", "DLNA"};
		comboBwType = new JComboBox<String>(bw_type);
		comboBwType.setFont(f);
		comboBwType.setBounds(120, 170, 80, 40);
		comboBwType.setSelectedIndex(3);
		this.getContentPane().add(comboBwType);

		cbNoVideo.setBounds(220, 170, 120, 40);
		cbNoVideo.setFont(f);
		this.getContentPane().add(cbNoVideo);
		
		/*String exe_filepath  = "D:/Software/ppbox/ppbox_test-win32-msvc90-mt-gd-1.1.0.exe";
		String[] cmd = new String[] {exe_filepath, ""};
		openExe(cmd);*/
		
		editorSearch.setFont(f);
		editorSearch.setBounds(20, 350, 200, 40);
		editorSearch.setText("海贼王");
	    this.getContentPane().add(editorSearch);
	    
	    btnSearch.setBounds(230, 350, 80, 40);
	    editorSearch.setFont(f);
		this.getContentPane().add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String key = editorSearch.getText();//"沈震轩PPTV独家专访";
				search(key);
			}
		});
		
		lbl_day.setBounds(10, 250, 50, 40);
		lbl_day.setFont(f);
		this.getContentPane().add(lbl_day);
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		List<String> DayList = new ArrayList<String>();
		Date today = new Date();
		for (int j=0;j<=7;j++) {
			Calendar c = Calendar.getInstance();
			c.setTime(today);
			c.add(Calendar.DAY_OF_MONTH, -1 * j);//把日期往后增加一天.整数往后推,负数往前移动 
			Date day = c.getTime();
			String strDay = df.format(day);
			DayList.add(strDay);
		}
		String[] day_desc = new String[8];
		DayList.toArray(day_desc);
		comboDay = new JComboBox<String>(day_desc);
		comboDay.setFont(f);
		comboDay.setBounds(60, 250, 140, 40);
		comboDay.setSelectedIndex(0);
		this.getContentPane().add(comboDay);
		
		lbl_start_time.setBounds(210, 250, 50, 40);
		lbl_start_time.setFont(f);
		this.getContentPane().add(lbl_start_time);
		
		List<String> HourList = new ArrayList<String>();
		for(int i=0;i<24;i++) {
			HourList.add(String.format("%02d:00", i));
			HourList.add(String.format("%02d:30", i));
		}
		String[] hour_desc = new String[48];
		HourList.toArray(hour_desc);
		comboHour = new JComboBox<String>(hour_desc);
		comboHour.setFont(f);
		comboHour.setBounds(260, 250, 80, 40);
		comboHour.setSelectedIndex(16);
		this.getContentPane().add(comboHour);
		
		lbl_duration.setBounds(10, 300, 50, 40);
		lbl_duration.setFont(f);
		this.getContentPane().add(lbl_duration);
		
		String[] duration_desc = {"直播", "半小时", "1小时",
				"1.5小时", "2小时", "2.5小时", "3小时"};
		comboDuration = new JComboBox<String>(duration_desc);
		comboDuration.setFont(f);
		comboDuration.setBounds(80, 300, 80, 40);
		comboDuration.setSelectedIndex(0);
		this.getContentPane().add(comboDuration);
		
		editorPlayExe.setFont(f);
		editorPlayExe.setBounds(10, 400, 450, 40);
		editorPlayExe.setText("D:/Software/ffmpeg/ffplay.exe");
	    this.getContentPane().add(editorPlayExe);
	}
	
	private void playvideo() {
		/*
		String link = mPlayLinkList.get(0).getId();
		int ft = comboFt.getSelectedIndex();
		
		int []ft_list = mEPG.getAvailableFT(link);
		if (ft_list == null || ft_list.length == 0) {
			System.out.println("failed to get available ft: " + mPlayLinkList.get(0).getId());
			mState = EPG_STATE.EPG_STATE_ERROR;
			return;
		}

		boolean found = false;
		for (int i=0;i<ft_list.length;i++) {
			if (ft == ft_list[i]) {
				found = true;
				break;
			}
		}
		
		if (!found) {
			System.out.println("failed to find ft");
			mState = EPG_STATE.EPG_STATE_ERROR;
			return;
		}*/
		
		String link = editorPlayLink.getText();
		int ft = comboFt.getSelectedIndex();
		
		int bw_type = comboBwType.getSelectedIndex();
		String link_surfix = "";
		
		String url = null;
		
		if (4 == bw_type) {
			String str_ft = String.valueOf(ft);
			boolean is_m3u8 = false;
			boolean noVideo = cbNoVideo.isSelected();
			
			url = mEPG.getCDNUrl(link, str_ft, is_m3u8, noVideo);
		}
		else {
			long start_time, duration;
			
			int dayIndex = comboDay.getSelectedIndex();
			int hourIndex = comboHour.getSelectedIndex();
			int durationIndex = comboDuration.getSelectedIndex();
			
			duration = durationIndex * 30;
			if (duration != 0) {
				Date date = new Date();
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				c.add(Calendar.DAY_OF_MONTH, -dayIndex);//把日期往后增加一天.整数往后推,负数往前移动 
				c.set(Calendar.HOUR_OF_DAY, hourIndex / 2);
				if (hourIndex % 2 == 0)
					c.set(Calendar.MINUTE, 0);
				else
					c.set(Calendar.MINUTE, 30);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				System.out.println("Java: set time to: " + c.getTime());
				
				start_time = c.getTime().getTime() / 1000;
				System.out.println(
						"start_time: " + start_time + 
						" , duration：" + duration);
				
				link_surfix = String.format("&begin_time=%d&end_time=%d", 
						start_time, start_time + duration * 60);

                try {
                	link_surfix = URLEncoder.encode(link_surfix, "utf-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}

                System.out.println("Java: mPlayerLinkSurfix final: " + link_surfix);
			}
			
			int port;
			if (mListLive)
				port = 5054;
			else
				port = 9006;
			url = PlayLinkUtil.getPlayUrl(
					Integer.valueOf(link), port, ft, bw_type, link_surfix);
		}
		
		if (url == null) {
			System.out.println("failed to get url");
			return;
		}
		
		System.out.println("ready to open url: " + url);
		
		String exe_filepath = editorPlayExe.getText();
		if (exe_filepath == null || exe_filepath.equals(""))
			exe_filepath  = "D:/Software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void selectCatalog() {
		int n = comboItem.getSelectedIndex();
		int index = mModuleList.get(n).getIndex();
		
		mEPG.catalog(index);
		
		mCatalogList = mEPG.getCatalog();
		if (null == mCatalogList)
			return;
		
		int size = mCatalogList.size();
		if (size < 1)
			return;
		
		comboItem.removeAllItems();
		
		for (int i=0;i<size;i++) {
			System.out.println(mCatalogList.get(i).toString());
			comboItem.addItem(mCatalogList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_CATALOG;
	}
	
	private void selectDetail() {
		boolean ret;
		
		int n = comboItem.getSelectedIndex();
		String vid = mCatalogList.get(n).getVid();
		if (vid == null) {
			System.out.println("vid is null");
			mState = EPG_STATE.EPG_STATE_ERROR;
			return;
		}
		
		ret = mEPG.detail(vid);
		if (!ret) {
			System.out.println("failed to get detail");
			return;
		}
		
		mPlayLinkList = mEPG.getLink();
		int size = mPlayLinkList.size();
		
		comboItem.removeAllItems();
		
		for (int i=0;i<size;i++) {
			System.out.println(mPlayLinkList.get(i).toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_DETAIL;
	}
	
	private void selectLink() {
		boolean ret;
		
		int n = comboItem.getSelectedIndex();
		String vid = mPlayLinkList.get(n).getId();
		if (vid == null) {
			System.out.println("vid is null");
			mState = EPG_STATE.EPG_STATE_ERROR;
			return;
		}
		
		if (mListLive) {
			mState = EPG_STATE.EPG_STATE_FOUND_PLAYLINK;
			editorPlayLink.setText(vid);
			lblInfo.setText("live vid " + vid + " selected");
			System.out.println("live playlink found! " + vid);
			
			CDNItem liveitem = mEPG.live_cdn(Integer.valueOf(vid));
			
			if (liveitem != null) {
				String block_url_fmt = "http://%s/live/074094e6c24c4ebbb4bf6a82f4ceabda/" +
						"%d.block?ft=1&platform=android3" +
						"&type=phone.android.vip&sdk=1" +
						"&channel=162&vvid=41&k=%s";
				String m3u8_url_fmt = "http://%s/live/interval/delay/" +
						"074094e6c24c4ebbb4bf6a82f4ceabda.m3u8" +
						"?type=phone.android.vip&sdk=1" +
						"&k=%s";
				String ts_url_fmt = "http://%s/live/074094e6c24c4ebbb4bf6a82f4ceabda/" +
						"%d.ts" +
						"?type=phone.android.vip" +
						"&k=%s";
	            
	            String st = liveitem.getST();
	            long start_time = new Date(st).getTime() / 1000;
	            start_time -= 45;
	            start_time -= (start_time % 5);
	            
				String httpUrl = String.format(block_url_fmt, liveitem.getHost(), start_time, liveitem.getK());
				System.out.println("Java: live flv block: " + httpUrl);
				
				String m3u8Url = String.format(m3u8_url_fmt, liveitem.getHost(), liveitem.getK());
				System.out.println("Java: live m3u8: " + m3u8Url);
				
				String tsUrl = String.format(ts_url_fmt, liveitem.getHost(), start_time, liveitem.getK());
				System.out.println("Java: live ts: " + tsUrl);
				
				String saveFile = String.format("d:\\%d.flv", start_time);
				//Util.httpDownload(httpUrl, saveFile);
			}
			
			return;
		}
		
		ret = mEPG.detail(vid);
		if (!ret) {
			System.out.println("vid is null");
			return;
		}
		
		mPlayLinkList = mEPG.getLink();
		int size = mPlayLinkList.size();
		
		comboItem.removeAllItems();
		
		for (int i=0;i<size;i++) {
			System.out.println(mPlayLinkList.get(i).toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
		
		if (size == 1) {
			System.out.println("playlink found! " + mPlayLinkList.get(0).getId());
			editorPlayLink.setText(String.valueOf(vid));
			lblInfo.setText("vid " + vid + " selected");
			mState = EPG_STATE.EPG_STATE_FOUND_PLAYLINK;
		}
		else
			mState = EPG_STATE.EPG_STATE_LINK;
	}
	
	private void init_combobox() {
		mListLive = false;
		start_page = 1;
		last_live_type = 0;
		
		int type = 1;
		switch (type) {
		case 0:
			frontpage();
			break;
		case 1:
			contents();
			break;
		default:
			System.out.println("unknown type: " + type);
			break;
		}
	}
	
	private void search(String key) {
		int type = 0;
		int content_type = 0; // 0-只正片，1-非正片，-1=不过滤
		
		boolean ret;
		
		ret = mEPG.search(key, type, content_type, 1, 10);
		if(!ret)
			return;
		
		mPlayLinkList = mEPG.getLink();
		if(mPlayLinkList.size() < 1)
			return;
		
		comboItem.removeAllItems();
		
		int size = mPlayLinkList.size();
		for(int i=0;i<size;i++) {
			PlayLink2 l = mPlayLinkList.get(i);
			System.out.println(l.toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
	
		mState = EPG_STATE.EPG_STATE_SEARCH;
	}
	
	private void frontpage() {
		boolean ret;
		
		ret = mEPG.frontpage();
		if (!ret)
			return;
		
		mModuleList = mEPG.getModule();
		int size = mModuleList.size();
		
		comboItem.removeAllItems();
		
		for (int i=0;i<size;i++) {
			System.out.println(mModuleList.get(i).toString());
			comboItem.addItem(mModuleList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_FRONTPAGE;
	}
	
	private void contents() {
		boolean ret;
		
		ret = mEPG.contents_list();
		if (!ret)
			return;

		comboItem.removeAllItems();
		
		mModuleList = mEPG.getModule();
		int size = mModuleList.size();
		
		for (int i=0;i<size;i++) {
			System.out.println(mModuleList.get(i).toString());
			comboItem.addItem(mModuleList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_CONTENT_LIST;
	}
	
	private void selectContents() {
		boolean ret;
		
		int n = comboItem.getSelectedIndex();
		String link = mModuleList.get(n).getLink(); //"app://aph.pptv.com/v4/cate/tv";
		
		if (mModuleList.get(n).getTitle().equals("直播"))
			mListLive = true;
		else
			mListLive = false;
		
		// save "type" for list()
		mContentType = "";
		int pos = link.indexOf("type=");
		if (pos != -1) {
			mContentType = link.substring(pos, link.length());
		}
		
		ret = mEPG.contents(link);
		if(!ret)
			return;
		
		comboItem.removeAllItems();
		
		mContentList = mEPG.getContent();
		int size = mContentList.size();
		
		for(int i=0;i<size;i++) {
			System.out.println("#" + i + ": " + mContentList.get(i).toString());
			comboItem.addItem(mContentList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_CONTENT;
	}
	
	private void selectList() {
		boolean ret;
		
		int n = comboItem.getSelectedIndex();
		String param = mContentList.get(n).getParam();
		if (param.startsWith("type="))
			mContentType = "";
		
		System.out.println(String.format("param: %s, type %s", param, mContentType));
		
		if (mListLive) {
			int type;
			if (last_live_type == 0) {
				if (n == 3)
					type = 156;
				else if (n == 2)
					type = 164;
				else {
					JOptionPane.showMessageDialog(null, "invalid live type!");
					return;
				}
				last_live_type = type;
			}
			else {
				type = last_live_type;
			}
			
			ret = mEPG.live(start_page, PAGE_NUM, type);
		}
		else
			ret = mEPG.list(param, mContentType, start_page, "order=t", 10);
		
		if (!ret)
			return;
		
		comboItem.removeAllItems();
		
		mPlayLinkList = mEPG.getLink();
		int size = mPlayLinkList.size();
		
		for (int i=0;i<size;i++) {
			System.out.println(mPlayLinkList.get(i).toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_LIST;
	}
	
	private void openExe(String... params) {
		Runtime rn = Runtime.getRuntime();
		Process proc = null;
		try {
			proc = rn.exec(params);
			
			 StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "Error");            
             StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "Output");
             errorGobbler.start();
             outputGobbler.start();
             //proc.waitFor();
		} catch (Exception e) {
			System.out.println("Error exec!");
		}
	}
	
	class StreamGobbler extends Thread {
		InputStream is;

		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					if (type.equals("Error"))
						System.out.println("[error] " + line);
					else
						System.out.println("[info] " + line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
