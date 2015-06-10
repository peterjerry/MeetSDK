package com.pplive.epg.sohu;

import javax.imageio.ImageIO;
import javax.swing.*; 

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*; 
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.epg.sohu.PlaylinkSohu.SOHU_FT;

public class SohuFrame extends JFrame {
	
	private SOHUVIDEO_EPG_STATE mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_IDLE;
	
	private enum SOHUVIDEO_EPG_STATE {
		SOHUVIDEO_EPG_STATE_IDLE,
		SOHUVIDEO_EPG_STATE_ERROR,
		
		SOHUVIDEO_EPG_STATE_CHANNEL,
		SOHUVIDEO_EPG_STATE_SUBCHANNEL,
		SOHUVIDEO_EPG_STATE_CLIP,
		SOHUVIDEO_EPG_STATE_CATE,
		SOHUVIDEO_EPG_STATE_TOPIC,
		SOHUVIDEO_EPG_STATE_ALBUM,
		SOHUVIDEO_EPG_STATE_EPISODE,
		SOHUVIDEO_EPG_STATE_PLAYLINK,
		
		SOHUVIDEO_EPG_STATE_FOUND_PLAYLINK,
	}
	
	private SohuUtil			mEPG;
	private List<ChannelSohu>	mChannelList;
	private List<SubChannelSohu>mSubChannelList;
	private List<CategorySohu>	mCateList;
	private List<TopicSohu>		mTopicList;
	private List<AlbumSohu>		mAlbumList;
	private List<EpisodeSohu>	mEpisodeList;
	private String 				mMoreList;
	
	long last_aid = -1;
	int last_site = -1;
	
	private final static int PAGE_SIZE 		= 10;
	private final int EPISODE_INCR_PAGE_NUM 	= 30;
	
	private int album_page_index = 1;
	private int ep_page_index = 1;
	private int ep_page_incr;
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	JButton btnNext 	= new JButton("翻页");
	
	JLabel lblInfo = new JLabel("info");
	
	JList<String> listItem 				= null;
	DefaultListModel<String> listModel 	= null;
	
	JTextPane editorSearch = new JTextPane();
	JButton btnSearch = new JButton("搜索");
	
	JLabel lblImage = new JLabel();
	JLabel lblTip = new JLabel();
	
	Font f = new Font("宋体", 0, 18);
	
	public SohuFrame() {
		super();
		
		mEPG = new SohuUtil();
		
		this.setTitle("Test EPG");
		this.setBounds(400, 300, 700, 600);
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
		
		listItem = new JList<String>();
		listItem.setFont(f);
		listItem.setBounds(20, 80, 300, 250);
		listModel = new DefaultListModel<String>();
		listItem.setModel(listModel);
		listItem.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listItem.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent event) {
				// TODO Auto-generated method stub
				if (listItem.getSelectedIndex() != -1) {
					if (event.getClickCount() == 1) {
						if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM) {
							int n = listItem.getSelectedIndex();
							AlbumSohu al = mAlbumList.get(n);

							lblTip.setText(al.getTip());
							
							BufferedImage image = null;
							try {
								String img_url = al.getImgUrl(false);
								System.out.println("Java image url: " + img_url);
								URL imageURL = new URL(img_url);
								InputStream is = imageURL.openConnection()
										.getInputStream();
								image = ImageIO.read(is);
								System.out.println("Java image is: " + image);
							} catch (Exception e) {
								e.printStackTrace();
								return;
							}

							lblImage.setIcon(new ImageIcon(image));
						}
					} else if (event.getClickCount() == 2) {
						action();
					}

				}
			}
		});
		this.getContentPane().add(listItem);
		
		lblImage.setBounds(350, 100, 256, 256);
		this.getContentPane().add(lblImage);
		
		lblTip.setFont(f);
		lblTip.setBounds(500, 320, 100, 40);
		this.getContentPane().add(lblTip);
		
		btnOK.setBounds(0, 0, 80, 30);
		this.getContentPane().add(btnOK);
		
		btnGo.setBounds(350, 80, 70, 40);
		btnGo.setFont(f);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(430, 80, 70, 40);
		btnReset.setFont(f);
		this.getContentPane().add(btnReset);
		btnNext.setFont(f);
		btnNext.setBounds(510, 80, 80, 40);
		this.getContentPane().add(btnNext);

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
				action();
			}
		});
		
		btnNext.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				
				if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM) {
					album_page_index++;
					morelist();

					listModel.clear();
					
					mAlbumList = mEPG.getAlbumList();
					int c = mAlbumList.size();
					for (int i=0;i<c;i++) {
						listModel.addElement(mAlbumList.get(i).getTitle());
					}
				}
				else if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE) {
					ep_page_index += ep_page_incr;
					selectEpisode();
				}
					
			}
		});
		
		editorSearch.setFont(f);
		editorSearch.setBounds(20, 350, 200, 40);
		editorSearch.setText("墨丹文");
	    this.getContentPane().add(editorSearch);
	    
	    btnSearch.setFont(f);
	    btnSearch.setBounds(230, 350, 80, 40);
	    editorSearch.setFont(f);
		this.getContentPane().add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String key = editorSearch.getText();
				search(key);
			}
		});
		
		init_combobox();
	}
	
	private void search(String key) {
		if (!mEPG.search(key, album_page_index, PAGE_SIZE)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mAlbumList = mEPG.getSearchItemList();
		for (int i=0;i<mAlbumList.size();i++) {
			listModel.addElement(mAlbumList.get(i).getTitle());
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void action() {
		switch (mState) {
		case SOHUVIDEO_EPG_STATE_CHANNEL:
			selectCate();
			break;
		case SOHUVIDEO_EPG_STATE_SUBCHANNEL:
		case SOHUVIDEO_EPG_STATE_CATE:
			selectAlbumNew();
			break;
		case SOHUVIDEO_EPG_STATE_CLIP:
			selectClip();
			break;
		case SOHUVIDEO_EPG_STATE_TOPIC:
			//selectTopic();
			break;
		case SOHUVIDEO_EPG_STATE_ALBUM:
			selectEpisode();
			break;
		case SOHUVIDEO_EPG_STATE_EPISODE:
			playProgram();
			break;
		default:
			break;
		}
	}
	
	private void selectClip() {
		int n = listItem.getSelectedIndex();
		System.out.println("Java: clip info: " + mAlbumList.get(n).toString());
		
		AlbumSohu al = mAlbumList.get(n);
		PlaylinkSohu link = null;
		if (al.getAid() > 1000000000000L)
			link = mEPG.video_info(al.getSite(), al.getVid(), al.getAid());
		else
			link = mEPG.playlink_pptv(al.getVid(), 0);
		if (link != null) {
			System.out.println("Java: link info: " + link.getTitle());
		}
		
		String url = link.getUrl(SOHU_FT.SOHU_FT_HIGH);

		System.out.println("final play link " + url);
		
		gen_xml(link, SOHU_FT.SOHU_FT_HIGH);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void selectEpisode() {
		if (last_aid == -1) {
			int n = listItem.getSelectedIndex();
			AlbumSohu al = mAlbumList.get(n);
			last_site = al.getSite();
			if (!al.isAlbum()) {
				PlaylinkSohu pl = null;
				if (al.getAid() > 1000000000000L)
					pl = mEPG.video_info(al.getSite(), al.getVid(), al.getAid());
				else
					pl = mEPG.playlink_pptv(al.getVid(), 0);
				if (pl != null) {
					String url = pl.getUrl(SOHU_FT.SOHU_FT_HIGH);

					System.out.println("quick play link " + url);
					
					gen_xml(pl, SOHU_FT.SOHU_FT_HIGH);
					
					String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
					String[] cmd = new String[] {exe_filepath, url};
					openExe(cmd);
				}
				
				return;
			}
			
			int last_count = al.getLastCount();
			if (last_count > EPISODE_INCR_PAGE_NUM) {
				ep_page_index = last_count / PAGE_SIZE + 1;
				ep_page_incr = -1;
			}
			else {
				ep_page_index = 1;
				ep_page_incr = 1;
			}
			
			last_aid = al.getAid();
		}
		
		AlbumSohu al = mEPG.album_info(last_aid);
		if (al != null) {
			System.out.println("album info: " + al.toString());
		}
		
		if (!mEPG.episode(last_aid, ep_page_index, PAGE_SIZE)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			listModel.addElement(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}
	
	private void selectCate() {
		int n = listItem.getSelectedIndex();
		String id = mChannelList.get(n).mChannelId;
		
		if (!mEPG.channel_select(id)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mSubChannelList = mEPG.getSubChannelList();
		for (int i=0;i<mSubChannelList.size();i++) {
			listModel.addElement(mSubChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_SUBCHANNEL;
	}
	
	private void selectAlbumNew() {
		int n = listItem.getSelectedIndex();
		int id = mSubChannelList.get(n).mSubChannelId;
		if (!mEPG.subchannel(id, PAGE_SIZE, 0)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}

		//if (id != 70020004)
			morelist();
		
		listModel.clear();
		
		mAlbumList = mEPG.getAlbumList();
		int c = mAlbumList.size();
		for (int i=0;i<c;i++) {
			listModel.addElement(mAlbumList.get(i).getTitle());
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void morelist() {
		mMoreList = mEPG.getMoreList();
		if (mMoreList != null && !mMoreList.isEmpty()) {
			if (!mEPG.morelist(mMoreList, PAGE_SIZE, (album_page_index - 1) * PAGE_SIZE)) {
				mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
				return;
			}
		}
	}
	
	/*private void selectCatePath() {
		int n = comboItem.getSelectedIndex();
		String cateUrl = mChannelList.get(n).mCateUrl;
		
		if (!mEPG.channel_sel(cateUrl)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mCateList = mEPG.getCateList();
		for (int i=0;i<mCateList.size();i++) {
			comboItem.addItem(mCateList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CATE;
	}
	
	private void selectTopic() {
		int n = comboItem.getSelectedIndex();
		int tid = mTopicList.get(n).mTid;
		
		if (!mEPG.album(tid, album_page_index, PAGE_SIZE)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mAlbumList = mEPG.getAlbumList();
		for (int i=0;i<mAlbumList.size();i++) {
			comboItem.addItem(mAlbumList.get(i).mAlbumName);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void selectAlbum() {
		int aid;
		if (last_aid == -1) {
			int n = comboItem.getSelectedIndex();
			last_aid = aid = mAlbumList.get(n).getAid();	
		}
		else {
			aid = last_aid;
		}
		
		if (!mEPG.episode(aid, album_page_index, PAGE_SIZE)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			comboItem.addItem(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}*/
	
	private void playProgram() {
		int index = listItem.getSelectedIndex();
		EpisodeSohu ep = mEpisodeList.get(index);
		
		PlaylinkSohu pl = null;
		if (ep.mAid > 1000000000000L)
			pl = mEPG.video_info(last_site, ep.mVid, ep.mAid);
		else
			pl = mEPG.playlink_pptv(ep.mVid, 0);
		
		SOHU_FT ft = SOHU_FT.SOHU_FT_ORIGIN;
		String strUrl = pl.getUrl(ft);
		if (strUrl == null || strUrl.isEmpty()) {
			ft = SOHU_FT.SOHU_FT_SUPER;
			strUrl = pl.getUrl(ft);
		}
		if (strUrl == null || strUrl.isEmpty()) {
			ft = SOHU_FT.SOHU_FT_HIGH;
			strUrl = pl.getUrl(ft);
		}
		if (strUrl == null || strUrl.isEmpty()) {
			ft = SOHU_FT.SOHU_FT_NORMAL;
			strUrl = pl.getUrl(ft);
		}
		if (strUrl == null || strUrl.isEmpty()) {
			System.out.println("no stream available");
			return;
		}
		
		System.out.println(String.format("Java strUrl(ft %s): %s", ft.toString(), strUrl));
		
		gen_xml(pl, ft);
		
		int pos = strUrl.indexOf(',');
		String url = null;
		if (pos != -1)
			url = strUrl.substring(0, pos);
		else
			url = strUrl;

		System.out.println("final play link " + url);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void gen_xml(PlaylinkSohu pl, SOHU_FT ft) {
		String strUrl = pl.getUrl(ft);
		String strDuration = pl.getDuration(ft);
		String strSize = pl.getSize(ft);
		
		// 创建根节点 list;
        Element root = new Element("ckplayer");
        // 将根节点添加到文档中；
        Document Doc = new Document(root);
        // 创建新节点 flashvars;
        Element flashvars = new Element("flashvars").setText("{h->2}");
        root.addContent(flashvars);
        
        StringTokenizer stUrl, stSize, stDuration;
		int i=0;
		
		stUrl = new StringTokenizer(strUrl, ",", false);
		stDuration = new StringTokenizer(strDuration, ",", false);
		stSize = new StringTokenizer(strSize, ",", false);
		
		while (stUrl.hasMoreElements() && stDuration.hasMoreElements() && stSize.hasMoreElements()) {
			String url = stUrl.nextToken();
			String duration = stDuration.nextToken();
			String size = stSize.nextToken();
			
			System.out.println(String.format("Java: segment #%d url: %s", i++, url));
			Element video = new Element("video");
            video.addContent(new Element("file").setText(url));
            video.addContent(new Element("size").setText(size));
            video.addContent(new Element("seconds").setText(duration));
            // 给父节点list添加company子节点;
            root.addContent(video);
		}
        
        // 输出company_list.xml文件
        try {
        	XMLOutputter XMLOut = new XMLOutputter();
			XMLOut.output(Doc, new FileOutputStream("\\\\172.16.204.106\\web\\list.xml"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void init_combobox() {
		/*if (!mEPG.topic(album_page_index, PAGE_SIZE)) {
			System.out.println("failed to channel()");
			return;
		}*/
		
		album_page_index	= 1;
		ep_page_index 		= 1;
		last_aid			= -1;
		
		if (!mEPG.channel_list()) {
			System.out.println("failed to column()");
			return;
		}
		
		listModel.clear();
		
		mChannelList = mEPG.getChannelList();
		int size = mChannelList.size();
		for (int i=0;i<size;i++) {
			listModel.addElement(mChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CHANNEL;
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
