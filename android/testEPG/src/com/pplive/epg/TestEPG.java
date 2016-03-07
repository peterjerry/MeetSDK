package com.pplive.epg;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.pplive.epg.boxcontroller.Code;
import com.pplive.epg.boxcontroller.MyActionEvent;
import com.pplive.epg.boxcontroller.MyBoxController;
import com.pplive.epg.pptv.NativeMedia;
import com.pplive.epg.util.LrcDownloadUtil;
import com.pplive.epg.util.MyNanoHTTPD;
import com.pplive.epg.util.Util;
import com.pplive.epg.youku.youkuUtil;

public class TestEPG { 
	private final static int APP_WIDTH	= 800;
	private final static int APP_HEIGHT	= 600;
	
	private static MyBoxController con;
	
	public static MyNanoHTTPD httpd;
	private static int mPort = 8080;
	
	public static void main(String[] args) {
		Random rand = new Random();
		mPort = 8080 + rand.nextInt(100);
		System.out.println("http port: " + mPort);
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				httpd = new MyNanoHTTPD(mPort, null);
				//httpd.setBDUSS(cookie_BDUSS);
				try {
					httpd.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.start();
		
		createAndShowGUI();
		
		/*try {
			String url = "http://musicmini.baidu.com/app/search/searchList.php?qword=%E5%8D%83%E5%8D%83%E9%98%99%E6%AD%8C&ie=utf-8&page=1";
			Document doc = Jsoup.connect(url).timeout(5000).get();
			Element test = doc.select("tbody").first();
			int size = test.childNodeSize();
			System.out.println("size " + size);
			for (int i = 2; i < size / 2; i++) {
				Element song = test.child(i);
				String id = song.child(0).child(0).child(0).attr("id");
				String song_name = song.child(2).attr("key");
				String artist = song.child(3).attr("key");
				String album = song.child(4).child(0).attr("title");
				System.out.println(String.format(
						"song id %s, name: %s, artist %s, album %s", 
						id, song_name, artist, album));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public static int getHttpPort() {
		return mPort;
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
