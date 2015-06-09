package com.pplive.epg.vst;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.swing.event.*;

public class VstFrame extends JFrame {
	
	private VST_EPG_STATE mState = VST_EPG_STATE.VST_EPG_STATE_IDLE;
	
	private enum VST_EPG_STATE {
		VST_EPG_STATE_IDLE,
		VST_EPG_STATE_ERROR,
		
		VST_EPG_STATE_CONTENT,
		VST_EPG_STATE_STREAM,
		
		VST_EPG_STATE_FOUND_PLAYLINK,
		VST_EPG_STATE_LIST,
	}
	
	VstUtil mEPG;
	List<ProgramVst> mProgramList;
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	
	JLabel lblInfo = new JLabel("info");
	JLabel lblNowPlayInfo = new JLabel("当前:");
	JLabel lblWillPlayInfo = new JLabel("即将:");
	
	JComboBox<String> comboItem 	= null;
	JComboBox<String> comboStream 	= null;
	
	JTextPane editorPlayLink = new JTextPane();
	
	public VstFrame() {
		super();
		
		mEPG = new VstUtil();
		
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
				mState = VST_EPG_STATE.VST_EPG_STATE_CONTENT;
				comboStream.removeAllItems();
			}

		});
		
		this.getContentPane().add(comboItem);
		
		comboStream = new JComboBox<String>();
		comboStream.setFont(f);
		comboStream.setBounds(20, 130, 450, 40);
		this.getContentPane().add(comboStream);
		
		lblNowPlayInfo.setBounds(5, 180, 500, 40);
		lblNowPlayInfo.setFont(f);
		this.getContentPane().add(lblNowPlayInfo);
		lblWillPlayInfo.setBounds(5, 230, 500, 40);
		lblWillPlayInfo.setFont(f);
		this.getContentPane().add(lblWillPlayInfo);
		
		btnOK.setBounds(0, 0, 80, 30);
		this.getContentPane().add(btnOK);
		btnGo.setBounds(230, 80, 70, 40);
		btnGo.setFont(f);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(300, 80, 70, 40);
		btnReset.setFont(f);
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
				case VST_EPG_STATE_CONTENT:
					selectProgram();
					break;
				case VST_EPG_STATE_STREAM:
					selectStream();
					break;
				default:
					break;
				}
			}
		});
		
		init_combobox();
		
	}
	
	private void selectProgram() {
		int n = comboItem.getSelectedIndex();
		List<String> url_list = mProgramList.get(n).getUrlList();
		
		comboStream.removeAllItems();
		for (int i=0;i<url_list.size();i++) {
			comboStream.addItem(url_list.get(i));
		}
		
		mState = VST_EPG_STATE.VST_EPG_STATE_STREAM;
	}
	
	private void selectStream() {
		int index = comboStream.getSelectedIndex();
		String url = comboStream.getItemAt(index);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void init_combobox() {
		if (!mEPG.program_list()) {
			System.out.println("failed to program_list()");
			return;
		}
		
		comboItem.removeAllItems();
		
		mProgramList = mEPG.getProgramList();
		for (int i=0;i<mProgramList.size();i++) {
			comboItem.addItem(mProgramList.get(i).getName());
		}
		
		mState = VST_EPG_STATE.VST_EPG_STATE_CONTENT;
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
