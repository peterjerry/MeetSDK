package com.pplive.epg;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.pplive.epg.baidu.BaiduPanel;
import com.pplive.epg.bestv.BestvPanel;
import com.pplive.epg.letv.LeTVPanel;
import com.pplive.epg.pptv.PPTVPanel;
import com.pplive.epg.sohu.SohuPanel;
import com.pplive.epg.vst.VstPanel;

@SuppressWarnings("serial")
public class TabbedPaneDemo extends JPanel {
	public TabbedPaneDemo()
    {
       //设置布局管理器，默认的布局管理器是 BorderLayout,这里没那么复杂
       //选择GridLayout(1,1)即可，就是整个为一块     
       super(new GridLayout(1,1));
      
       //创建JTabbedPane
       JTabbedPane tp = new JTabbedPane();
       //创建标签显示的图标
       ImageIcon iiVST = createImageIcon("images/label.png");  
       ImageIcon iiBaidu = createImageIcon("images/baidu.png");  
       ImageIcon iiBestv = createImageIcon("images/bestv.png");
       ImageIcon iiPPTV = createImageIcon("images/pptv.png");  
       ImageIcon iiSohu = createImageIcon("images/sohu.png");  
       
       Font f = new Font("宋体", 0, 20);
       
       PPTVPanel pptvPanel = new PPTVPanel();
       //LeTVPanel letvPanel = new LeTVPanel();
       //VstPanel vstPanel = new VstPanel();
       //BestvPanel bestvPanel = new BestvPanel();
       SohuPanel sohuPanel = new SohuPanel();
       BaiduPanel baiduPanel = new BaiduPanel();
       
       //指定标签名，标签图标，panel，和提示信息
       tp.addTab("PPTV", iiPPTV, pptvPanel, "聚力传媒");
       //设置标签的快捷键
       tp.setMnemonicAt(0, KeyEvent.VK_0);
      
       //第二个标签
       //tp.addTab("LeTV", ii, letvPanel, "乐视");
       //tp.setMnemonicAt(1, KeyEvent.VK_1);
 
       //第三个标签
       //tp.addTab("VST", iiVST, vstPanel, "全聚合");
       //tp.setMnemonicAt(1, KeyEvent.VK_2);
       
       //第四个标签
       //tp.addTab("Best TV", iiBestv, bestvPanel, "百事通");
       //tp.setMnemonicAt(1, KeyEvent.VK_3);
       
       //第五个标签
       tp.addTab("SohuVideo", iiSohu, sohuPanel, "搜狐视频");
       tp.setMnemonicAt(1, KeyEvent.VK_4);
       //设置合适的显示尺寸，这个是必须的，因为如果所有的标签都
       //不指定适合的显示尺寸，系统无法判断初始显示尺寸大小
       //默认是使用最小化，并且对一个标签设计即可
       //tp.setPreferredSize(new Dimension(700,600));
 
       //第六个标签
       tp.addTab("BaiduPan", iiBaidu, baiduPanel, "百度网盘");
       tp.setMnemonicAt(2, KeyEvent.VK_5);
       
       //将tabbedPanel添加到Jpanel中
       add(tp);
      
       //设置窗口过小时，标签的显示策略
       tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
       //设置标签停放的位置，这里设置为左侧停放
       tp.setTabPlacement(JTabbedPane.LEFT);
       tp.setSelectedIndex(2);
    }
	
	private JPanel createPanel(String string) {
		// 创建一个JPanel，并为构造函数初始false
		// 表示不适用双缓冲
		JPanel panel = new JPanel(false);

		// 设置布局
		panel.setLayout(new GridLayout(1, 1));

		// 创建一个label放到panel中
		JLabel filler = new JLabel(string);
		filler.setHorizontalAlignment(JLabel.CENTER);
		panel.add(filler);
		return panel;
	}
	
	private ImageIcon createImageIcon(String string) {
		URL url = this.getClass().getResource(string);
		if (url == null) {
			System.out.println("the image " + string + " is not exist!");
			return null;
		}
		return new ImageIcon(url);
	}
}