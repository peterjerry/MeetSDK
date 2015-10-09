package com.pplive.epg.letv;

import javax.swing.*; 

import com.pplive.epg.util.Util;

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class LeTVPanel extends JPanel {
	
	private LETV_EPG_STATE mState = LETV_EPG_STATE.LETV_EPG_STATE_IDLE;
	
	private enum LETV_EPG_STATE {
		LETV_EPG_STATE_IDLE,
		LETV_EPG_STATE_ERROR,
		
		LETV_EPG_STATE_CONTENT,
		LETV_EPG_STATE_STREAM,
		
		LETV_EPG_STATE_FOUND_PLAYLINK,
		LETV_EPG_STATE_LIST,
	}
	
	private LetvUtil mEPG;
	private LeVideoUtil mVideoUtil = new LeVideoUtil();
	private List<Programlb> mProgramList;
	private List<PlayLinkLb> mPlayLinkList;
	private List<StreamIdLb> mStrmList;
	
	private String exe_ffplay;
	
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	
	JLabel lblInfo = new JLabel("info");
	JLabel lblNowPlayInfo = new JLabel("当前:");
	JLabel lblWillPlayInfo = new JLabel("即将:");
	
	JTextPane editorSearch = new JTextPane();
	JButton btnSearch = new JButton("搜索");
	
	JComboBox<String> comboItem 	= null;
	JComboBox<String> comboStream 	= null;
	
	JTextPane editorPlayLink = new JTextPane();
	
	public LeTVPanel() {
		super();
		
		this.setLayout(null);
		
		String strConfig = Util.readFileContent("config.txt");
		StringTokenizer st = new StringTokenizer(strConfig, "\n", false);
		while (st.hasMoreElements()) {
			String strLine = (String) st.nextElement();
			if (strLine.startsWith("#"))
				continue;
				
			int pos = strLine.indexOf("=");
			if (pos > 0 && pos != strLine.length() - 1) {
				String key = strLine.substring(0, pos);
				String value = strLine.substring(pos + 1);
				System.out.println(String.format("Java: key %s, value %s", key ,value));
				if (key.equals("ffplay_path")) {
					exe_ffplay = value;
					System.out.println("Java: set ffplay path to " + exe_ffplay);
				}
				else {
					System.out.println("Java: unknown key:" + key);
				}
			}
		}
		
		mEPG = new LetvUtil();
		mVideoUtil = new LeVideoUtil();
		
		// Action
		lblInfo.setBounds(5, 40, 300, 30);
		this.add(lblInfo);
		
		comboItem = new JComboBox<String>();
		Font f = new Font("宋体", 0, 18);
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
				mState = LETV_EPG_STATE.LETV_EPG_STATE_CONTENT;
				comboStream.removeAllItems();
			}

		});
		
		this.add(comboItem);
		
		comboStream = new JComboBox<String>();
		comboStream.setFont(f);
		comboStream.setBounds(20, 130, 200, 40);
		this.add(comboStream);
		
		lblNowPlayInfo.setBounds(5, 180, 500, 40);
		lblNowPlayInfo.setFont(f);
		this.add(lblNowPlayInfo);
		lblWillPlayInfo.setBounds(5, 230, 500, 40);
		lblWillPlayInfo.setFont(f);
		this.add(lblWillPlayInfo);
		
		btnGo.setBounds(230, 80, 70, 40);
		btnGo.setFont(f);
		this.add(btnGo);
		btnReset.setBounds(300, 80, 70, 40);
		btnReset.setFont(f);
		this.add(btnReset);

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				init_combobox();
			}
		});
		
		btnGo.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				switch (mState) {
				case LETV_EPG_STATE_CONTENT:
					selectProgram();
					break;
				case LETV_EPG_STATE_STREAM:
					selectStream();
					break;
				default:
					break;
				}
			}
		});
		
		editorSearch.setFont(f);
		editorSearch.setBounds(20, 350, 200, 40);
		editorSearch.setText("越光宝盒");
	    this.add(editorSearch);
	    
	    btnSearch.setFont(f);
	    btnSearch.setBounds(230, 350, 80, 40);
	    editorSearch.setFont(f);
		this.add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String key = editorSearch.getText();//"沈震轩PPTV独家专访";
				
				int vid = mVideoUtil.search(key);
				if (vid == -1)
					return;
				
				VideoInfo vInfo = mVideoUtil.detail(vid);
				String streamDesc = "mp4_1300";
				if (vInfo != null) {
					List<String> brList = vInfo.getBrList();
					if (brList != null) {
						int count = brList.size();
						streamDesc = brList.get(count - 1);
						for (int i=0;i<count;i++) {
							System.out.println(String.format("br #%d %s", i, brList.get(i)));
						}
					}
				}
				String cdn_url = mVideoUtil.cdn_url(vid, streamDesc);
				if (cdn_url == null)
					return;
				
				String play_url = mVideoUtil.play_url(cdn_url);
				if (play_url == null)
					return;
				
				System.out.println("ready to play url: " + play_url);
				String[] cmd = new String[] {exe_ffplay, play_url};
				openExe(cmd);
			}
		});
		
		init_combobox();
	}
	
	private void selectProgram() {
		int n = comboItem.getSelectedIndex();
		String epg_id = mProgramList.get(n).getEPGId();
		String stream_id = mProgramList.get(n).getStreamId();
		System.out.println("Java: epg_id " + epg_id + " ,stream_id " + stream_id);

		if (!mEPG.play_list(epg_id)) {
			System.out.println("Java: failed to play_list()");
			return;
		}
			
		mStrmList = mEPG.getStreamIdList();
		comboStream.removeAllItems();
		for (int i=0;i<mStrmList.size();i++) {
			comboStream.addItem(mStrmList.get(i).getId());
		}
		
		List<ProgramItemlb> list = mEPG.getProgramItemList();
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		SimpleDateFormat df_prefix = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println("current time " + df.format(new Date()));// new Date()为获取当前系统时间
		Date now_date = new Date();
		String now_prefix = df_prefix.format(now_date);
		
		for (int i=0;i<list.size();i++) {
			ProgramItemlb item = list.get(i);
			
			Date item_date;
			try {
				item_date = df.parse(now_prefix + " " + item.getPlaytime());
				if (item_date.getTime() >= now_date.getTime()) {
					int index = i - 1;
					if (index < 0)
						index = 0;
					ProgramItemlb nowplay_item = list.get(index);
					ProgramItemlb willplay_item = item;
					String now_play = nowplay_item.getTitle();
					String will_play = willplay_item.getTitle();
					lblNowPlayInfo.setText("当前: " + now_play);
					lblWillPlayInfo.setText("即将: " + will_play);
					System.out.println(String.format("Java: now playing %s, next %s",
							now_play, will_play));
					
					mState = LETV_EPG_STATE.LETV_EPG_STATE_STREAM;
					break;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void selectStream() {
		int index = comboStream.getSelectedIndex();
		String select_strm_id = mStrmList.get(index).getId();
		
		String must = mEPG.recommend(select_strm_id);
		if (must == null) {
			System.out.println("Java: failed to recommend()");
			return;
		}
		
		if (!mEPG.live(select_strm_id, must)) {
			System.out.println("Java: failed to live()");
			return;
		}
		
		mPlayLinkList = mEPG.getPlaylinkList();
		if (mPlayLinkList.size() > 0) {
			PlayLinkLb lb = mPlayLinkList.get(0);
			String url = lb.getUrl();
			System.out.println(String.format("Java: select location_server %s, url %s", 
					lb.getName(), url));
			
			String url_fmt = "http://live.gslb.letv.com/gslb" +
					"?stream_id=%s" +
					"&tag=live" +
					"&ext=m3u8" +
					"&sign=live_tv" +
					"&platid=10" +
					"&splatid=1009" +
					"&format=C1S" +
					"&expect=1";
			
			url = String.format(url_fmt, select_strm_id);
			
			String exe_filepath  = "D:/Software/ffmpeg/ffplay.exe";
			String[] cmd = new String[] {exe_filepath, url};
			openExe(cmd);
		}
	}
	
	private void init_combobox() {
		if (!mEPG.context()) {
			System.out.println("failed to get context");
			return;
		}
		
		comboItem.removeAllItems();
		
		mProgramList = mEPG.getProgramList();
		for (int i=0;i<mProgramList.size();i++) {
			comboItem.addItem(mProgramList.get(i).getName());
		}
		
		mState = LETV_EPG_STATE.LETV_EPG_STATE_CONTENT;
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
