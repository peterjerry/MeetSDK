package com.pplive.epg;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.*;

public class MyFrame extends JFrame {
	
	private EPG_STATE mState = EPG_STATE.EPG_STATE_IDLE;
	private EPGUtil mEPG;
	private List<Content> mContentList;
	private List<Module> mModuleList;
	private List<Catalog> mCatalogList;
	private List<PlayLink2> mPlayLinkList;
	private List<Navigator> mNavList;
	private String mContentType;
	
	private boolean mListLive = false;
	
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
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("Reset");
	JButton btnGo 		= new JButton("Go");
	
	JLabel lblCaption = new JLabel("PPTV");
	
	String []items = {"Item1", "Item2", "Item3", "Item4", "Item5", "Item6"};
	
	JComboBox<String> comboItem 	= null;
	JComboBox<String> comboFt 		= null;
	JComboBox<String> comboBwType 	= null;
	JList<String> lstType 			= null;
	
	JCheckBox cbNoVideo = new JCheckBox("NoVideo");

	MyFrame() {
		super();
		
		mEPG = new EPGUtil();
		
		this.setTitle("Test EPG");
		this.setBounds(400, 300, 400, 600);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				System.exit(0);
			}
		});

		this.getContentPane().setLayout(null);
		// Action
		lblCaption.setBounds(5, 40, 120, 30);
		this.getContentPane().add(lblCaption);
		
		btnOK.setBounds(0, 0, 80, 30);
		this.getContentPane().add(btnOK);
		btnGo.setBounds(230, 80, 50, 20);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(280, 80, 80, 20);
		this.getContentPane().add(btnReset);

		btnOK.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				lblCaption.setText("You Click OK!");
			}
		});

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				init_combobox();
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
					selectLink();
					break;
				case EPG_STATE_FOUND_PLAYLINK:
					//JOptionPane.showMessageDialog(null, "playlink already found!");
					playvideo();
					break;
				case EPG_STATE_CONTENT:
					selectList();
					break;
				case EPG_STATE_SEARCH:
					break;
				default:
					System.out.println("invalid state: " + mState.toString());
					break;
				}
				
			}
		});

		comboItem = new JComboBox<String>();
		Font f = new Font("宋体", 0, 12);
		comboItem.setFont(f);
		comboItem.setBounds(20, 80, 200, 20);
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
		
		comboFt = new JComboBox<String>();
		comboFt.setBounds(20, 110, 80, 20);
		this.getContentPane().add(comboFt);
		
		String[] bw_type = {"P2P", "CDNP2P", "CDN", "PPTV", "DLNA"};
		comboBwType = new JComboBox<String>(bw_type);
		comboBwType.setBounds(120, 110, 80, 20);
		comboBwType.setSelectedIndex(3);
		this.getContentPane().add(comboBwType);

		cbNoVideo.setBounds(220, 110, 120, 20);
		this.getContentPane().add(cbNoVideo);
		
		/*String exe_filepath  = "D:/Software/ppbox/ppbox_test-win32-msvc90-mt-gd-1.1.0.exe";
		String[] cmd = new String[] {exe_filepath, ""};
		openExe(cmd);*/
	}
	
	private void playvideo() {
		String link = mPlayLinkList.get(0).getId();
		int ft = -1;// = comboFt.getSelectedIndex();
		String ft_desc_item = (String)comboFt.getSelectedItem();
		for (int i=0;i<ft_desc.length;i++) {
			if (ft_desc_item.equals(ft_desc[i])) {
				ft = i;
				break;
			}
		}
		
		if (ft == -1) {
			System.out.println("failed to find ft");
			return;
		}
		
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
			url = PlayLinkUtil.getPlayUrl(Integer.valueOf(link), 9006, ft, bw_type, link_surfix);
		}
		
		if (url == null) {
			System.out.println("failed to get url");
			return;
		}
		
		System.out.println("ready to open url: " + url);
		String exe_filepath  = "D:/Software/ffmpeg/ffplay.exe";
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
			System.out.println("live playlink found! " + vid);
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
			
			int []ft_list = mEPG.getAvailableFT(vid);
			if (ft_list == null || ft_list.length == 0) {
				System.out.println("failed to get available ft: " + mPlayLinkList.get(0).getId());
				mState = EPG_STATE.EPG_STATE_ERROR;
			}
			else {
				comboFt.removeAll();
				for (int i=0;i<ft_list.length;i++) {
					int index = ft_list[i];
					if (index < 4)
						comboFt.addItem(ft_desc[index]);
				}
				
				comboFt.setSelectedIndex(0);
				
				mState = EPG_STATE.EPG_STATE_FOUND_PLAYLINK;
			}
		}
		else
			mState = EPG_STATE.EPG_STATE_LINK;
	}
	
	private void init_combobox() {
		int type = 2;
		
		switch (type) {
		case 0:
			frontpage();
			break;
		case 1:
			search();
			break;
		case 2:
			contents();
			break;
		case 3:
			//live();
			break;
		default:
			System.out.println("unknown type: " + type);
			break;
		}
	}
	
	private void search() {
		EPGUtil epg = new EPGUtil();
		
		String key = "泰坦尼克";
		int type = 0;
		int content_type = 0;
		
		boolean ret;
		
		ret = epg.search(key, type, content_type, 2, 10);
		if(!ret)
			return;
		
		mPlayLinkList = epg.getLink();
		if(mPlayLinkList.size() < 1)
			return;
		
		comboItem.removeAllItems();
		
		int size = mPlayLinkList.size();
		for(int i=0;i<size;i++) {
			PlayLink2 l = mPlayLinkList.get(i);
			System.out.println(l.toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
		
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
			if (n == 3)
				type = 156;
			else if (n == 2)
				type = 164;
			else {
				JOptionPane.showMessageDialog(null, "invalid live type!");
				return;
			}
			
			ret = mEPG.live(1, 15, type);
		}
		else
			ret = mEPG.list(param, mContentType, 1, "order=n", 10);
		
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
