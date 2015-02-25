package com.pplive.epg;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
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
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("Reset");
	JButton btnGo 		= new JButton("Go");
	
	JLabel lblCaption = new JLabel("PPTV");
	
	String []items = {"Item1", "Item2", "Item3", "Item4", "Item5", "Item6"};
	
	JComboBox<String> comboItem = null;
	JList<String> lstType = null;//new JList<String>(items);
	
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
		comboItem.addActionListener(new ActionListener(){

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

		cbNoVideo.setBounds(150, 100, 120, 30);
		this.getContentPane().add(cbNoVideo);
	}
	
	private void playvideo() {
		String filename = "E:\\archive\\Movie\\Mindhunters.LiMiTED.DVDRiP.XviD-HLS\\hls-mh.avi";
		
		String link = mPlayLinkList.get(0).getId();
		String ft = "1";
		boolean is_m3u8 = false;
		boolean noVideo = false;
		
		String url = mEPG.getCDNUrl(link, ft, is_m3u8, noVideo);
		if (url == null)
			return;
		
		openExe(url);
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
			mState = EPG_STATE.EPG_STATE_FOUND_PLAYLINK;
			System.out.println("playlink found! " + mPlayLinkList.get(0).getId());
		}
		else
			mState = EPG_STATE.EPG_STATE_LINK;
	}
	
	private void init_combobox() {
		int type = 0;
		
		switch (type) {
		case 0:
			frontpage();
			break;
		case 1:
			//search();
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
		
		ret = mEPG.list(param, mContentType, 2, "order=n", 10);
		if (!ret)
			return;
		
		comboItem.removeAllItems();
		
		mPlayLinkList = mEPG.getLink();
		int size = mPlayLinkList.size();
		
		for(int i=0;i<size;i++) {
			System.out.println(mPlayLinkList.get(i).toString());
			comboItem.addItem(mPlayLinkList.get(i).getTitle());
		}
		
		mState = EPG_STATE.EPG_STATE_LIST;
	}
	
	private void openExe(String url) {
		Runtime rn = Runtime.getRuntime();
		Process p = null;
		try {
			String exe_filepath = "C:/Program Files (x86)/VideoLAN/VLC/vlc.exe";
			String[] cmd = new String[] {exe_filepath, url};
			p = rn.exec(cmd);
		} catch (Exception e) {
			System.out.println("Error exec!");
		}
	}
}
