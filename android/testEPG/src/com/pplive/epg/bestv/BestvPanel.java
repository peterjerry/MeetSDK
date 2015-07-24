package com.pplive.epg.bestv;

import javax.swing.*; 

import java.awt.Font;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public class BestvPanel extends JPanel {
	
	private BESTV_EPG_STATE mState = BESTV_EPG_STATE.BESTV_EPG_STATE_IDLE;
	
	private enum BESTV_EPG_STATE {
		BESTV_EPG_STATE_IDLE,
		BESTV_EPG_STATE_ERROR,
		
		BESTV_EPG_STATE_CONTENT,
		BESTV_EPG_STATE_CHANNEL,
		
		BESTV_EPG_STATE_FOUND_PLAYLINK,
		BESTV_EPG_STATE_LIST,
	}
	
	BestvUtil mEPG;
	List<BestvChannel> mChannelList;
	//List<PlayLinkLb> mPlayLinkList;
	//List<StreamIdLb> mStrmList;
	
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	
	JLabel lblInfo = new JLabel("info");
	JLabel lblNowPlayInfo = new JLabel("当前:");
	JLabel lblWillPlayInfo = new JLabel("即将:");
	
	JComboBox<String> comboItem 	= null;
	JComboBox<String> comboStream 	= null;
	
	JTextPane editorPlayLink = new JTextPane();
	
	JLabel lbl_day = new JLabel("日期");
	JLabel lbl_start_time = new JLabel("开始");
	JLabel lbl_duration = new JLabel("时长");
	
	JComboBox<String> comboDay 		= null;
	JComboBox<String> comboHour 	= null;
	JComboBox<String> comboDuration	= null;
	
	Font f = new Font("宋体", 0, 18);
	
	public BestvPanel() {
		super();
		
		this.setLayout(null);
		
		mEPG = new BestvUtil();

		// Action
		lblInfo.setBounds(5, 40, 300, 30);
		this.add(lblInfo);
		
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
				mState = BESTV_EPG_STATE.BESTV_EPG_STATE_CONTENT;
				comboStream.removeAllItems();
			}

		});
		
		this.add(comboItem);
		
		comboStream = new JComboBox<String>();
		comboStream.setFont(f);
		comboStream.setBounds(20, 130, 450, 40);
		//this.add(comboStream);
		
		lblNowPlayInfo.setBounds(5, 130, 500, 40);
		lblNowPlayInfo.setFont(f);
		this.add(lblNowPlayInfo);
		lblWillPlayInfo.setBounds(5, 180, 500, 40);
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
				case BESTV_EPG_STATE_CONTENT:
					selectProgram();
					break;
				case BESTV_EPG_STATE_CHANNEL:
					playProgram();
					break;
				default:
					break;
				}
			}
		});
		
		init_combobox();
		
		initTimePicker();
	}
	
	private void initTimePicker() {
		lbl_day.setBounds(10, 250, 50, 40);
		lbl_day.setFont(f);
		this.add(lbl_day);
		
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
		this.add(comboDay);
		
		lbl_start_time.setBounds(210, 250, 50, 40);
		lbl_start_time.setFont(f);
		this.add(lbl_start_time);
		
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
		this.add(comboHour);
		
		lbl_duration.setBounds(10, 300, 50, 40);
		lbl_duration.setFont(f);
		this.add(lbl_duration);
		
		String[] duration_desc = {"直播", "半小时", "1小时",
				"1.5小时", "2小时", "2.5小时", "3小时"};
		comboDuration = new JComboBox<String>(duration_desc);
		comboDuration.setFont(f);
		comboDuration.setBounds(60, 300, 80, 40);
		comboDuration.setSelectedIndex(0);
		this.add(comboDuration);
	}
	
	private void selectProgram() {
		int n = comboItem.getSelectedIndex();
		String code = mChannelList.get(n).getChannelCode();
		BestvPlayInfo info = mEPG.playInfo(code);
		if (info == null) {
			mState = BESTV_EPG_STATE.BESTV_EPG_STATE_ERROR;
		}
		
		lblNowPlayInfo.setText("当前: " + info.nowPlay);
		lblWillPlayInfo.setText("即将: " + info.willPlay);
		mState = BESTV_EPG_STATE.BESTV_EPG_STATE_CHANNEL;
	}
	
	private void playProgram() {
		int index = comboItem.getSelectedIndex();
		String url = mChannelList.get(index).getPlayUrl();
		
		/*BestvKey b_key = mEPG.getLiveKey();
		if (b_key == null) {
			System.out.println("Java: failed to get bestv key");
			return;
		}
		
		System.out.println("Java: key " + b_key.toString());
		String key = b_key.getKey();*/
		String key = "4BE9666ECD5CA07A02A52EC9689B81621E354113967482965E8A7CC79F52A526";
		
		String timeStr = getTimeStr();
		if (timeStr != null && !timeStr.isEmpty()) {
			url += timeStr;
		}
		else {
			url = url.replace("index.m3u8", "live.m3u8");
		}
		
		url += "&_cp=1&_fk=";
		url += key;

		System.out.println("final play link " + url);
		
		String exe_filepath  = "E:/git/PPTV/MeetSDK/engine2/build/win32/bin/Release/player_vc.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private String getTimeStr() {
		String link_surfix = null;
		
		long start_time, duration;
		
		int dayIndex = comboDay.getSelectedIndex();
		int hourIndex = comboHour.getSelectedIndex();
		int durationIndex = comboDuration.getSelectedIndex();
		
		duration = durationIndex * 30;
		if (duration == 0)
			return null;
		
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
		
		link_surfix = String.format("&starttime=%d&endtime=%d", 
				start_time, start_time + duration * 60);

        System.out.println("Java: mPlayerLinkSurfix final: " + link_surfix);
        return link_surfix;
	}
	
	private void init_combobox() {
		if (!mEPG.channel(3)) {
			System.out.println("failed to channel()");
			return;
		}
		
		comboItem.removeAllItems();
		
		mChannelList = mEPG.getChannelList();
		for (int i=0;i<mChannelList.size();i++) {
			comboItem.addItem(mChannelList.get(i).getTitle());
		}
		
		mState = BESTV_EPG_STATE.BESTV_EPG_STATE_CONTENT;
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
