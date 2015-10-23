package com.pplive.epg;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import com.pplive.epg.baidu.BaiduPanel;
import com.pplive.epg.boxcontroller.Code;
import com.pplive.epg.boxcontroller.MyActionEvent;
import com.pplive.epg.boxcontroller.MyBoxController;
import com.pplive.epg.util.Util;

public class TestEPG { 
	private final static int APP_WIDTH	= 800;
	private final static int APP_HEIGHT	= 600;
	
	private static MyBoxController con;
	
	public static void main(String[] args) {
		createAndShowGUI();
	}
	
	public static void createAndShowGUI() {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int screen_width = dim.width;
		int screen_height = dim.height;
		int x, y;
		
		if (screen_width <= APP_WIDTH || screen_height <= APP_HEIGHT) {
			x = 0;
			y = 0;
		}
		else {
			x = (screen_width - APP_WIDTH) / 2;
			y = (screen_height - APP_HEIGHT) / 2;
		}
		
		JFrame frame = new JFrame("电视鸭");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		System.out.println("tm: " + System.currentTimeMillis());
		
		boolean useController = false;
		
		if (useController) {
			initController(frame);
		}
		else {
			frame.add(new TabbedPaneDemo());
			frame.pack();
			frame.setBounds(x, y, APP_WIDTH, APP_HEIGHT);
			
			frame.setVisible(true);
		}
	}
	
	private static void initController(JFrame frame) {
		frame.setLayout(null);
		
		Font f = new Font("宋体", 0, 18);
		String []strManuel = {"上下左右 - 上下左右", "Ctrl键 - 退出", "Enter - 进入", 
				"空格键 - 菜单", "backspace - 退格", "F1 - HOME"};
		for (int i=0;i<strManuel.length;i++) {
			JLabel lblManual = new JLabel(strManuel[i]);
			lblManual.setFont(f);
			lblManual.setBounds(20, 60 + i * 40, 300, 40);
			frame.add(lblManual);
		}
		
		final JLabel lblInfo = new JLabel("信息");
		lblInfo.setBounds(20, 300, 300, 40);
		frame.add(lblInfo);
		
		String ip_addr = "192.168.200.63";
		int port = 46891;
		
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
				if (key.equals("listen")) {
					pos = value.indexOf(":");
					if (pos == -1) {
						lblInfo.setText(" 获取配置文件失败！");
					}
					
					ip_addr = value.substring(0, pos); // "192.168.200.63"
					port = Integer.valueOf(value.substring(pos + 1)); // 46891
				}
				else {
					System.out.println("Java: unknown key " + key);
				}
			}
		}
		
		con = new MyBoxController(ip_addr, port);
		
		if (!con.connect()) {
			lblInfo.setText("连接失败！");
		}
		else {
			lblInfo.setText(String.format("连接主机 %s:%d", ip_addr, port));
		}
		
		frame.setBounds(400, 400, 400, 400);
		frame.setVisible(true);
		
		frame.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println("Java: keyPressed " + e.getKeyCode());
				// TODO Auto-generated method stub
				int keycode = e.getKeyCode();
				if (keycode >= KeyEvent.VK_A && keycode <= KeyEvent.VK_Z)
					con.sendKeyEvent(Code.KEYCODE_A + keycode - KeyEvent.VK_A);
				else if (keycode >= KeyEvent.VK_0 && keycode <= KeyEvent.VK_9)
					con.sendKeyEvent(Code.KEYCODE_0 + keycode - KeyEvent.VK_0);
				else if (keycode == KeyEvent.VK_SLASH)
					con.sendKeyEvent(Code.KEYCODE_NUMPAD_DIVIDE);
				else {
					switch (keycode) {
					case KeyEvent.VK_DOWN:
						con.sendKeyEvent(Code.KEYCODE_DPAD_DOWN);
						break;
					case KeyEvent.VK_UP:
						con.sendKeyEvent(Code.KEYCODE_DPAD_UP);
						break;
					case KeyEvent.VK_LEFT:
						con.sendKeyEvent(Code.KEYCODE_DPAD_LEFT);
						break;
					case KeyEvent.VK_RIGHT:
						con.sendKeyEvent(Code.KEYCODE_DPAD_RIGHT);
						break;
					case KeyEvent.VK_ENTER:
						con.sendKeyEvent(Code.KEYCODE_ENTER);
						break;
					case KeyEvent.VK_CONTROL:
						con.sendKeyEvent(Code.KEYCODE_BACK);
						break;
					case KeyEvent.VK_SPACE:
						con.sendKeyEvent(Code.KEYCODE_MENU);
						break;
					case KeyEvent.VK_BACK_SPACE:
						con.sendKeyEvent(Code.KEYCODE_DEL);
						break;
					case KeyEvent.VK_F1:
						con.sendKeyEvent(Code.KEYCODE_HOME);
						break;
					default:
						break;
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				System.out.println("Java: keyReleased " + e.getKeyCode());
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				System.out.println("Java: keyTyped " + e.getKeyCode());
			}
			
		});
	}
	
}
