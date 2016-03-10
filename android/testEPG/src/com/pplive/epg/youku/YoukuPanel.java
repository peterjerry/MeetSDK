package com.pplive.epg.youku;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.pplive.epg.youku.YKUtil;
import com.pplive.epg.util.Util;

public class YoukuPanel extends JPanel {
	private static String exe_ffplay = "E:/software/ffmpeg/bin/ffplay.exe";
	
	private YKUtil mEPG;
	
	JButton btnPlay	= new JButton("播放");
	JTextPane youkuUrl = new JTextPane();
	
	public YoukuPanel() {
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
				if (key.equals("exe_ffplay")) {
					exe_ffplay = value;
					System.out.println("Java: set ffplay path to " + exe_ffplay);
				}
				else {
					System.out.println("Java: unknown key " + key);
				}
			}
		}
		
		mEPG = new YKUtil();
		
		Font f = new Font("宋体", 0, 18);
		
		youkuUrl.setBounds(20, 80, 500, 40);
		youkuUrl.setFont(f);
		youkuUrl.setText(
				"http://v.youku.com/v_show/id_XMTQ5NDg3NjQzNg==.html" +
				"?from=s1.8-1-1.1");
	    this.add(youkuUrl);
	    
	    btnPlay.setBounds(550, 80, 70, 40);
		btnPlay.setFont(f);
		this.add(btnPlay);
		
		btnPlay.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String m3u8Url = mEPG.getPlayUrl(youkuUrl.getText());
				if (m3u8Url == null) {
					JOptionPane.showMessageDialog(
							null, "获取播放地址失败", "优酷视频", 
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String[] cmd = new String[] {exe_ffplay, m3u8Url};
				openExe(cmd);
			}
		});
	}
	
	private static void openExe(String... params) {
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
			e.printStackTrace();
			System.out.println("failed to run exec");
		}
	}
	
	private static class StreamGobbler extends Thread {
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
					if (type.equals("Error")) {
						System.out.println("[error] " + line);
					}
					else {
						System.out.println("[info] " + line);
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
