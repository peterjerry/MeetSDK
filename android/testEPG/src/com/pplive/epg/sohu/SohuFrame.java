package com.pplive.epg.sohu;

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

public class SohuFrame extends JFrame {
	
	private SOHUVIDEO_EPG_STATE mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_IDLE;
	
	private enum SOHUVIDEO_EPG_STATE {
		SOHUVIDEO_EPG_STATE_IDLE,
		SOHUVIDEO_EPG_STATE_ERROR,
		
		SOHUVIDEO_EPG_STATE_CHANNEL,
		SOHUVIDEO_EPG_STATE_SUBCHANNEL,
		SOHUVIDEO_EPG_STATE_CLIP,
		SOHUVIDEO_EPG_STATE_CATE,
		SOHUVIDEO_EPG_STATE_TOPIC,
		SOHUVIDEO_EPG_STATE_ALBUM,
		SOHUVIDEO_EPG_STATE_EPISODE,
		SOHUVIDEO_EPG_STATE_PLAYLINK,
		
		SOHUVIDEO_EPG_STATE_FOUND_PLAYLINK,
	}
	
	SohuUtil			mEPG;
	List<ChannelSohu>	mChannelList;
	List<SubChannelSohu>mSubChannelList;
	List<CategorySohu>	mCateList;
	List<TopicSohu>		mTopicList;
	List<AlbumSohu>		mAlbumList;
	List<EpisodeSohu>	mEpisodeList;
	
	int last_aid = -1;
	
	private final static int page_size = 20;
	private int page_index = 1;
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	JButton btnNext 	= new JButton("翻页");
	
	JLabel lblInfo = new JLabel("info");
	
	JComboBox<String> comboItem 	= null;
	
	JTextPane editorPlayLink = new JTextPane();
	
	JTextPane editorSearch = new JTextPane();
	JButton btnSearch = new JButton("搜索");
	
	Font f = new Font("宋体", 0, 18);
	
	public SohuFrame() {
		super();
		
		mEPG = new SohuUtil();
		
		this.setTitle("Test EPG");
		this.setBounds(400, 300, 500, 600);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				System.exit(0);
			}
		});

		this.getContentPane().setLayout(null);
		// Action
		lblInfo.setBounds(5, 40, 300, 30);
		this.getContentPane().add(lblInfo);
		
		comboItem = new JComboBox<String>();
		comboItem.setFont(f);
		comboItem.setBounds(20, 80, 200, 40);
		comboItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub	
			}
		});
		
		comboItem.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
			}

		});
		
		this.getContentPane().add(comboItem);
		
		
		btnOK.setBounds(0, 0, 80, 30);
		this.getContentPane().add(btnOK);
		btnGo.setBounds(230, 80, 70, 40);
		btnGo.setFont(f);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(300, 80, 70, 40);
		btnReset.setFont(f);
		this.getContentPane().add(btnReset);
		btnNext.setFont(f);
		btnNext.setBounds(370, 80, 80, 40);
		this.getContentPane().add(btnNext);

		btnOK.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				lblInfo.setText("You Click OK!");
			}
		});

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				init_combobox();
			}
		});
		
		btnGo.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action();
			}
		});
		
		btnNext.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				page_index++;
				action();
			}
		});
		
		editorSearch.setFont(f);
		editorSearch.setBounds(20, 350, 200, 40);
		editorSearch.setText("越狱");
	    this.getContentPane().add(editorSearch);
	    
	    btnSearch.setFont(f);
	    btnSearch.setBounds(230, 350, 80, 40);
	    editorSearch.setFont(f);
		this.getContentPane().add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String key = editorSearch.getText();
				search(key);
			}
		});
		
		init_combobox();
	}
	
	private void search(String key) {
		if (!mEPG.search(key, page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mAlbumList = mEPG.getSearchItemList();
		for (int i=0;i<mAlbumList.size();i++) {
			comboItem.addItem(mAlbumList.get(i).getTitle());
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void action() {
		switch (mState) {
		case SOHUVIDEO_EPG_STATE_CHANNEL:
			selectCate();
			break;
		case SOHUVIDEO_EPG_STATE_SUBCHANNEL:
		case SOHUVIDEO_EPG_STATE_CATE:
			selectAlbumNew();
			break;
		case SOHUVIDEO_EPG_STATE_CLIP:
			selectClip();
			break;
		case SOHUVIDEO_EPG_STATE_TOPIC:
			//selectTopic();
			break;
		case SOHUVIDEO_EPG_STATE_ALBUM:
			selectEpisode();
			break;
		case SOHUVIDEO_EPG_STATE_EPISODE:
			playProgram();
			break;
		default:
			break;
		}
	}
	
	private void selectClip() {
		int n = comboItem.getSelectedIndex();
		System.out.println("Java: clip info: " + mAlbumList.get(n).toString());
		
		int vid = mAlbumList.get(n).getVid();
		int aid = mAlbumList.get(n).getAid();
		PlaylinkSohu link = mEPG.detail(vid, aid);
		if (link != null) {
			System.out.println("Java: link info: " + link.getTitle());
		}
		
		List<String> url_list = link.getUrlListbyFT(2);
		String url = url_list.get(0);

		System.out.println("final play link " + url);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void selectEpisode() {
		int n = comboItem.getSelectedIndex();
		int aid = mAlbumList.get(n).getAid();
		
		if (!mEPG.episode(aid, page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			comboItem.addItem(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}
	
	private void selectCate() {
		int n = comboItem.getSelectedIndex();
		String id = mChannelList.get(n).mChannelId;
		
		if (!mEPG.cate(id)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mSubChannelList = mEPG.getSubChannelList();
		for (int i=0;i<mSubChannelList.size();i++) {
			comboItem.addItem(mSubChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_SUBCHANNEL;
	}
	
	private void selectAlbumNew() {
		int n = comboItem.getSelectedIndex();
		int id = mSubChannelList.get(n).mSubChannelId;
		if (!mEPG.subchannel(id, page_size, 0)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mAlbumList = mEPG.getAlbumList();
		int c = mAlbumList.size();
		for (int i=0;i<c;i++) {
			comboItem.addItem(mAlbumList.get(i).getTitle());
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void selectCatePath() {
		int n = comboItem.getSelectedIndex();
		String cateUrl = mChannelList.get(n).mCateUrl;
		
		if (!mEPG.cate(cateUrl)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mCateList = mEPG.getCateList();
		for (int i=0;i<mCateList.size();i++) {
			comboItem.addItem(mCateList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CATE;
	}
	
	/*private void selectTopic() {
		int n = comboItem.getSelectedIndex();
		int tid = mTopicList.get(n).mTid;
		
		if (!mEPG.album(tid, page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mAlbumList = mEPG.getAlbumList();
		for (int i=0;i<mAlbumList.size();i++) {
			comboItem.addItem(mAlbumList.get(i).mAlbumName);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void selectAlbum() {
		int aid;
		if (last_aid == -1) {
			int n = comboItem.getSelectedIndex();
			last_aid = aid = mAlbumList.get(n).getAid();	
		}
		else {
			aid = last_aid;
		}
		
		if (!mEPG.episode(aid, page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			comboItem.addItem(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}*/
	
	private void playProgram() {
		int index = comboItem.getSelectedIndex();

		int vid = mEpisodeList.get(index).mVid;
		PlaylinkSohu pl = mEPG.getPlayLink(vid, 0);
		List<String> url_list = pl.getUrlListbyFT(1);
		String url = url_list.get(0);

		System.out.println("final play link " + url);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void init_combobox() {
		/*if (!mEPG.topic(page_index, page_size)) {
			System.out.println("failed to channel()");
			return;
		}*/
		
		if (!mEPG.list()) {
			System.out.println("failed to column()");
			return;
		}
		
		comboItem.removeAllItems();
		
		mChannelList = mEPG.getChannelList();
		int size = mChannelList.size();
		for (int i=0;i<size;i++) {
			comboItem.addItem(mChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CHANNEL;
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
