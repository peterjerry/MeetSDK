package com.pplive.epg;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.swing.event.*;

public class LeTVFrame extends JFrame {
	
	private LETV_EPG_STATE mState = LETV_EPG_STATE.LETV_EPG_STATE_IDLE;
	
	private enum LETV_EPG_STATE {
		LETV_EPG_STATE_IDLE,
		LETV_EPG_STATE_ERROR,
		
		LETV_EPG_STATE_CONTENT,
		
		LETV_EPG_STATE_FOUND_PLAYLINK,
		LETV_EPG_STATE_LIST,
	}
	
	LetvUtil mEPG;
	List<Programlb> mProgramList;
	List<PlayLinkLb> mPlayLinkList;
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("Reset");
	JButton btnGo 		= new JButton("Go");
	
	JLabel lblInfo = new JLabel("info");
	
	JComboBox<String> comboItem 	= null;
	
	JTextPane editorPlayLink = new JTextPane();
	
	LeTVFrame() {
		super();
		
		mEPG = new LetvUtil();
		
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
		lblInfo.setBounds(5, 40, 300, 30);
		this.getContentPane().add(lblInfo);
		
		comboItem = new JComboBox<String>();
		Font f = new Font("宋体", 0, 12);
		comboItem.setFont(f);
		comboItem.setBounds(20, 80, 300, 20);
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
		btnGo.setBounds(230, 120, 50, 20);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(280, 120, 80, 20);
		this.getContentPane().add(btnReset);

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
				switch (mState) {
				case LETV_EPG_STATE_CONTENT:
					selectProgram();
					break;
				default:
					break;
				}
			}
		});
		
		init_combobox();
		
	}
	
	void selectProgram() {
		int n = comboItem.getSelectedIndex();
		String stream_id = mProgramList.get(n).getStreamId();
		String must = mEPG.recommend(stream_id);
		if (must == null) {
			System.out.println("Java: failed to recommend()");
			return;
		}
		
		if (!mEPG.live(stream_id, must)) {
			System.out.println("Java: failed to live()");
			return;
		}
		
		mPlayLinkList = mEPG.getPlaylinkList();
		if (mPlayLinkList.size() > 0) {
			PlayLinkLb lb = mPlayLinkList.get(0);
			String url = lb.getUrl();
			System.out.println(String.format("Java: select %s %s", 
					lb.getName(), url));
			
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
