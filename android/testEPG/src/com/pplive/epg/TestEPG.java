package com.pplive.epg;

import javax.swing.JFrame;

import com.pplive.epg.baidu.BaiduPanel;

public class TestEPG { 
	
	public static void main(String[] args) {
		createAndShowGUI();
	}
	
	public static void createAndShowGUI() {
		JFrame frame = new JFrame("电视鸭");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new TabbedPaneDemo());

		frame.pack();
		frame.setBounds(400, 300, 750, 600);
		frame.setVisible(true);
		
		//JFrame myFrame = new BaiduFrame();
		//myFrame.setVisible(true);
	}
	
}
