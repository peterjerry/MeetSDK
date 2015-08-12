package com.pplive.epg;

import java.awt.Font;
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
	private static MyBoxController con;
	
	public static void main(String[] args) {
		createAndShowGUI();
	}
	
	public static void createAndShowGUI() {
		JFrame frame = new JFrame("电视鸭");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		boolean useController = false;
		
		if (useController) {
			initController(frame);
		}
		else {
			frame.add(new TabbedPaneDemo());
			frame.pack();
			frame.setBounds(400, 300, 750, 600);
			
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
		
		String strCfg = Util.readFileContent("config.txt");
		int pos = strCfg.indexOf(":");
		if (pos == -1) {
			lblInfo.setText(" 获取配置文件失败！");
		}
		
		String ip_addr = strCfg.substring(0, pos); // "192.168.200.63"
		int port = Integer.valueOf(strCfg.substring(pos + 1)); // 46891
		
		con = new MyBoxController(ip_addr, port);
		
		if (!con.connect()) {
			lblInfo.setText("连接失败！");
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
