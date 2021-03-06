package com.pplive.epg.baidu;

import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.TestEPG;
import com.pplive.epg.shooter.DetailItem;
import com.pplive.epg.shooter.SearchItem;
import com.pplive.epg.shooter.ShooterUtil;
import com.pplive.epg.util.LrcDownloadUtil;
import com.pplive.epg.util.LrcInfo;
import com.pplive.epg.util.LrcParser;
import com.pplive.epg.util.LrcParser2;
import com.pplive.epg.util.MyNanoHTTPD;
import com.pplive.epg.util.SeparatorUtils;
import com.pplive.epg.util.TimeLrc;
import com.pplive.epg.util.Util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*; 
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;


@SuppressWarnings("serial")
public class BaiduPanel extends JPanel {
	
	private String mbOauth 		= "23.48f0a3ad0fe8b41428000bb0509f1fb2.2592000.1441874259.184740130-266719";
	private String mbRootPath 	=  "/我的视频";
	private String mChildPath;
	private String mOperatePath;
	private String[] mOperatePaths;
	//private String mBaiduToken;
	//private String mBDUSS;
	private boolean mIsCopy	= false;
	private String list_by 		= "time"; // "name" "size"
	private String list_order 	= "desc"; // "asc"
	
	private final static int ONE_KILOBYTE = 1024;
	private final static int ONE_MAGABYTE = (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE = (ONE_MAGABYTE * ONE_KILOBYTE);
	
	private static String exe_vlc = "D:/Program Files/vlc-3.0.0/vlc.exe";
	private static String exe_foobar = "D:/Program Files/Foobar2000/foobar2000.exe";
	private static String exe_ffplay = "D:/Program Files/ffmpeg/ffplay.exe";
	private static String cookie_BDUSS = "NGYm9lQ3h0MTYxZEJSYXdVNkNDWlZNMXk5" +
			"Q0ZCc3BKaEk2d1FUci1Lb1JYWHBXQVFBQUFBJCQAAAAAA" +
			"AAAAAEAAACnC~0Gd25wbGxyem9kaWFjAAAAAAAAAAAAAAAAAAAAAAA" +
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABHQUlYR0FJWcF";
	
	private final String BAIDU_PCS_PREFIX = "https://pcs.baidu.com/rest/2.0/pcs";
	private final String BAIDU_PCS_FILE_PREFIX = BAIDU_PCS_PREFIX + "/file";
	private final String BAIDU_PCS_THUMBNAIL_PREFIX = BAIDU_PCS_PREFIX + "/thumbnail";
	private final String BAIDU_PCS_SERVICE_PREFIX = BAIDU_PCS_PREFIX + "/services/cloud_dl";
	
	private final String BAIDU_PCS_DOWNLOAD_V2 = "http://c.pcs.baidu.com/rest/2.0/pcs/file" + 
			"?method=download" +
			"&app_id=250528" + 
			"&path=";
	
	private final String BAIDU_PCS_DOWNLOAD_PROXY_FMT = "http://127.0.0.1:%d/rest/2.0/pcs/file" + 
			"?method=download" +
			"&app_id=250528" + 
			"&new_api=1" +
			"&path=";

	private final String BAIDU_PCS_CLOUD_DL_V2 = "http://pan.baidu.com/rest/2.0/services/cloud_dl" +
			"?method=add_task" +
			"&app_id=250528" +
			//"&bdstoken=%s" +
			"&channel=chunlei" +
			"&clienttype=0" +
			"&web=0";
	
	private final String BAIDU_PCS_CLOUD_LIST_TASK = "http://pan.baidu.com/rest/2.0/services/cloud_dl" +
			"?method=list_task" +
			"&app_id=250528" +
			"&channel=chunlei" +
			"&clienttype=0" +
			"&web=0" +
			"&need_task_info=1" +
			"&start=0" +
			"&limit=10" +
			"&task_ids=";
	
	private final String BAIDU_PCS_CLOUD_QUERY_TASK = "http://pan.baidu.com/rest/2.0/services/cloud_dl" +
			"?method=query_task" +
			"&app_id=250528" +
			"&channel=chunlei" +
			"&clienttype=0" +
			"&web=0" +
			"&op_type=1" + // 0 查信息, 1 查进度
			"&task_ids=";
	
	private final String BAIDU_PCS_CLOUD_CLEAR_TASK = "http://pan.baidu.com/rest/2.0/services/cloud_dl" +
			"?method=clear_task" +
			"&app_id=250528" +
			"&channel=chunlei" +
			"&clienttype=0" +
			"&web=1";
	
	private final static String ApiKey = "4YchBAkgxfWug3KRYCGOv8EK"; // from es explorer
	
	private final String BAIDU_PCS_AUTHORIZE = "https://openapi.baidu.com/oauth/2.0" + 
		"/authorize?response_type=token" + 
		"&client_id=" + ApiKey + 
		"&redirect_uri=oob&scope=netdisk";
	
	private final String BAIDU_PCS_LIST;
	private final String BAIDU_PCS_META;
	private final String BAIDU_PCS_DOWNLOAD;
	private final String BAIDU_PCS_UPLOAD;
	private final String BAIDU_PCS_CREATE_SUPER_FILE;
	private final String BAIDU_PCS_STREAMING;
	private final String BAIDU_PCS_THUMBNAIL;
	private final String BAIDU_PCS_MOVE;
	private final String BAIDU_PCS_COPY;
	private final String BAIDU_PCS_MKDIR;
	private final String BAIDU_PCS_DELETE;
	private final String BAIDU_PCS_CLOUD_DL;
	private final String BAIDU_PCS_SEARCH;
	
	private final int UPLOAD_READSIZE = 4096; // 4k
	
	private int mDownloadBlockSize = 1048576; // 1M
	private int mUploadBlockSize = 0;

	private final static String[] list_by_desc = {"按时间", "按名称", "按大小"}; //time" "name" "size"
	
	private List<Map<String, Object>> mFileList;

	JButton btnUp	 	= new JButton("...");
	JButton btnReset 	= new JButton("重置");
	JButton btnAuth 	= new JButton("认证");
	JButton btnPlay		= new JButton("播放");
	JButton btnPaste	= new JButton("粘贴");
	JButton btnDelete	= new JButton("删除");
	
	JLabel lblRootPath	= new JLabel("info");
	JComboBox<String> comboListBy 		= null;
	
	JList<String> listItem 				= null;
	DefaultListModel<String> listModel 	= null;
	JScrollPane scrollPane				= null;
	
	JPopupMenu jPopupMenu				= null;
	JMenuItem menuItemDownload			= null;
	JMenuItem menuItemCut				= null;
	JMenuItem menuItemCopy				= null;
	JMenuItem menuItemRename			= null;
	JMenuItem menuItemSearchOnlineSub	= null;
	JMenuItem menuItemSearchOnlineLrc	= null;
	JMenuItem menuItemGetFolderSize		= null;
	JMenuItem menuItemGetDownloadPath	= null;
	
	JLabel lblImage = new JLabel();
	
	JTextPane editPath = new JTextPane();
	JButton btnCreateFolder = new JButton("添加");
	JButton btnDownload = new JButton("下载");
	JButton btnYunDownload = new JButton("云存");
	JButton btnUpload = new JButton("上传");
	JButton btnSearch = new JButton("搜索");
	
	boolean bDownloading = false;
	boolean bInterrupt = false;
	
	JCheckBox cbTranscode = new JCheckBox("转码");
	JCheckBox cbUseVLC = new JCheckBox("vlc");
	JCheckBox cbOrder = new JCheckBox("降序");
	JLabel lblInfo = new JLabel("信息");
	
	Font f = new Font("宋体", 0, 18);
	
	public BaiduPanel() {
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
				if (key.equals("listen")) {
					
				}
				else if (key.equals("token")) {
					mbOauth = value;
					System.out.println("Java: set token to " + mbOauth);
				}
				else if (key.equals("ffplay_path")) {
					exe_ffplay = value;
					System.out.println("Java: set ffplay path to " + exe_ffplay);
				}
				else if (key.equals("vlc_path")) {
					exe_vlc = value;
					System.out.println("Java: set vlc path to " + exe_vlc);
				}
				else if (key.equals("foobar_path")) {
					exe_foobar = value;
					System.out.println("Java: set foobar path to " + exe_foobar);
				}
				else if (key.equals("write_blocksize")) {
					mDownloadBlockSize = Integer.valueOf(value);
					System.out.println("Java: set mDownloadBlockSize to " + mDownloadBlockSize);
				}
				else if (key.equals("upload_blocksize")) {
					mUploadBlockSize = Integer.valueOf(value);
					System.out.println("Java: set mUploadBlockSize to " + mUploadBlockSize);
				}
				else if (key.equals("BDUSS")) {
					cookie_BDUSS = value;
					TestEPG.httpd.setBDUSS(cookie_BDUSS);
					System.out.println("Java: set cookie_BDUSS to " + cookie_BDUSS);
				}
				else {
					System.out.println("Java: unknown key " + key);
				}
			}
		}

		BAIDU_PCS_LIST = BAIDU_PCS_FILE_PREFIX + 
				"?method=list" +
				"&access_token=" + mbOauth + 
				"&path=";
		BAIDU_PCS_META = BAIDU_PCS_FILE_PREFIX + 
				"?method=meta" +
				"&access_token=" + mbOauth + 
				"&path=";
		BAIDU_PCS_DOWNLOAD = BAIDU_PCS_FILE_PREFIX +  
				"?method=download" +
				"&app_id=250528" + 
				"&access_token=" + mbOauth +
				"&path=";
		BAIDU_PCS_UPLOAD = BAIDU_PCS_FILE_PREFIX + 
				"?method=upload" +
				"&access_token=" + mbOauth + 
				"&ondup=newcopy";
				//"&path=";
				//"&type=tmpfile";
				//"&ondup=newcopy"; // overwrite
		BAIDU_PCS_CREATE_SUPER_FILE = BAIDU_PCS_FILE_PREFIX + 
				"?method=createsuperfile" +
				"&access_token=" + mbOauth + 
				"&ondup=newcopy";
		BAIDU_PCS_STREAMING = BAIDU_PCS_FILE_PREFIX + 
				"?method=streaming" +
				"&access_token=" + mbOauth + 
				"&path=";
		BAIDU_PCS_THUMBNAIL = BAIDU_PCS_THUMBNAIL_PREFIX +
				"?method=generate" +
				"&access_token=" + mbOauth +
				"&path=%s" +
				"&quality=100" +
				"&width=%d" +
				"&height=%d";
		BAIDU_PCS_MOVE = BAIDU_PCS_FILE_PREFIX +
				"?method=move" + 
				"&access_token=" + mbOauth +
				"&from=%s" +
				"&to=%s";
		BAIDU_PCS_COPY = BAIDU_PCS_FILE_PREFIX +
				"?method=copy" + 
				"&access_token=" + mbOauth +
				"&from=%s" +
				"&to=%s";
		BAIDU_PCS_MKDIR = BAIDU_PCS_FILE_PREFIX +
				"?method=mkdir" + 
				"&access_token=" + mbOauth +
				"&path=";
		BAIDU_PCS_DELETE = BAIDU_PCS_FILE_PREFIX +
				"?method=delete" + 
				"&access_token=" + mbOauth +
				"&path=";
		BAIDU_PCS_CLOUD_DL = BAIDU_PCS_SERVICE_PREFIX + 
				"?method=add_task" +
				"&access_token=" + mbOauth;
		BAIDU_PCS_SEARCH = BAIDU_PCS_FILE_PREFIX + 
				"?method=search" +
				"&access_token=" + mbOauth;
		
		// Action
		lblRootPath.setFont(f);
		lblRootPath.setBounds(20, 40, 300, 30);
		this.add(lblRootPath);
		
		jPopupMenu = new JPopupMenu();
		
		menuItemDownload = new JMenuItem("下载");
		menuItemDownload.setFont(f);
		menuItemDownload.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (bDownloading) {
					JOptionPane.showMessageDialog(
							null, "已有任务在下载中", "下载任务", 
							JOptionPane.INFORMATION_MESSAGE); 
					return;
				}
				
				download();
			}
		});
		jPopupMenu.add(menuItemDownload);
		
		menuItemCut = new JMenuItem("剪切");
		menuItemCut.setFont(f);
		menuItemCut.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePaths = getOperatePaths();
				mIsCopy = false;
				System.out.println("cut");
			}
		});
		jPopupMenu.add(menuItemCut);
		
		menuItemCopy = new JMenuItem("复制");
		menuItemCopy.setFont(f);
		menuItemCopy.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePaths = getOperatePaths();
				mIsCopy = true;
				System.out.println("copy");
			}
		});
		jPopupMenu.add(menuItemCopy);
		
		menuItemRename = new JMenuItem("重命名");
		menuItemRename.setFont(f);
		menuItemRename.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePath = getOperatePath();
				int pos = mOperatePath.lastIndexOf("/");
				String oldFileName = mOperatePath.substring(pos + 1);
				String newFileName = JOptionPane.showInputDialog(null, "输入新文件名", oldFileName);
				if (newFileName == null || newFileName.equals(""))
					return;
				
				String toPath = mbRootPath + "/" + newFileName;
				move_copy(mOperatePath, toPath, false);
				init_combobox();
			}
		});
		jPopupMenu.add(menuItemRename);
		
		menuItemSearchOnlineSub = new JMenuItem("查找在线字幕");
		menuItemSearchOnlineSub.setFont(f);
		menuItemSearchOnlineSub.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePath = getOperatePath();
				int pos = mOperatePath.lastIndexOf("/");
				int pos2 = mOperatePath.lastIndexOf(".");
				if (pos == -1 || pos2 == -1 || pos >= pos2)
					return;
				
				String FileName = mOperatePath.substring(pos + 1, pos2);
				String searchKey = JOptionPane.showInputDialog(null, "输入搜索字幕关键字", FileName);
				if (searchKey == null || searchKey.equals(""))
					return;
				
				searchSub(searchKey);
			}
		});
		jPopupMenu.add(menuItemSearchOnlineSub);
		
		menuItemSearchOnlineLrc = new JMenuItem("查找在线歌词");
		menuItemSearchOnlineLrc.setFont(f);
		menuItemSearchOnlineLrc.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePath = getOperatePath();
				int pos = mOperatePath.lastIndexOf("/");
				int pos2 = mOperatePath.lastIndexOf(".");
				if (pos == -1 || pos2 == -1 || pos >= pos2)
					return;
				String filename = mOperatePath.substring(pos + 1, pos2);
				
				String song_name = JOptionPane.showInputDialog(null, "输入歌曲名", filename);
				if (song_name == null)
					return;
				
				String artist = JOptionPane.showInputDialog(null, "输入艺术家", filename);
				if (artist == null)
					return;

				String lrc_url = LrcDownloadUtil.getBaiduLyc(song_name, artist);
				if (lrc_url == null) {
					System.out.println("Java: failed to get lrc_path");
					return;
				}
				
				if (!process_lrc(lrc_url)) {
					System.out.println("failed to process_lrc()");
					return;
				}
			}
		});
		jPopupMenu.add(menuItemSearchOnlineLrc);
		
		menuItemGetFolderSize = new JMenuItem("统计文件夹大小");
		menuItemGetFolderSize.setFont(f);
		menuItemGetFolderSize.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				GetFolderSizeWorker worker = new GetFolderSizeWorker(getOperatePath());
				worker.execute();
			}
		});
		jPopupMenu.add(menuItemGetFolderSize);
		
		menuItemGetDownloadPath = new JMenuItem("获取下载路径");
		menuItemGetDownloadPath.setFont(f);
		menuItemGetDownloadPath.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mOperatePath = getOperatePath();
				try {
					String encoded_path = URLEncoder.encode(mOperatePath, "utf-8");
 
					//String url = BAIDU_PCS_DOWNLOAD_V2 + encoded_path;
					String url = String.format(BAIDU_PCS_DOWNLOAD_PROXY_FMT, 
							TestEPG.getHttpPort());
					url += encoded_path;
					System.out.println("get download url: " + url);
					Clipboard clipboard = getToolkit().getSystemClipboard();//获取系统剪贴板;
					StringSelection text = new StringSelection(url);
					clipboard.setContents(text,null);
					JOptionPane.showMessageDialog(null,"地址已复制至剪贴板");
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		jPopupMenu.add(menuItemGetDownloadPath);
		
		listItem = new JList<String>();
		listItem.setFont(f);
		listModel = new DefaultListModel<String>();
		listItem.setModel(listModel);
		listItem.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listItem.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent event) {
				// TODO Auto-generated method stub
				int index = listItem.getSelectedIndex();
				if (index == -1)
					return;
				
				int button = event.getButton();
				if (button == 1) { // Left button
					if (event.getClickCount() == 2) {
						if (mFileList != null) {
							Map<String, Object> fileinfo = mFileList.get(index);
							boolean isdir = (Boolean) fileinfo.get("isdir");
							if (isdir) {
								mbRootPath = (String) fileinfo.get("path");
								System.out.println("go into folder: " + mbRootPath);
								init_combobox();
							}
							else {
								String path = (String) fileinfo.get("path");
								if (cbTranscode.isSelected() && streaming(path, "M3U8_640_480")) {
									String[] cmd = new String[] {exe_ffplay, "index.m3u8"};
									openExe(cmd);
									return;
								}
								else {
									play_url(path);
								}
							}
						}
					}
					else if (event.getClickCount() == 1) {
						FileInfoThread myThread = new FileInfoThread(index);
						Thread t = new Thread(myThread);
						t.start();
					}
				}
				else if (button == 3) { // right
					 jPopupMenu.show(listItem, event.getX(), event.getY());
				}
				
			}
		});
		listItem.setCellRenderer(new MyRender());
		listItem.add(jPopupMenu);

		scrollPane = new JScrollPane(listItem);
		scrollPane.setBounds(20, 80, 400, 350);
		this.add(scrollPane);
		
		btnUp.setBounds(410, 30, 70, 40);
		btnUp.setFont(f);
		this.add(btnUp);

		btnUp.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (mbRootPath.equals("/"))
					return;
				
				int pos = mbRootPath.lastIndexOf("/");
				if (pos < 0)
					return;
				
				mChildPath = mbRootPath;
				if (pos > 0)
					mbRootPath = mbRootPath.substring(0, pos);
				else 
					mbRootPath = "/";
				init_combobox();
			}
		});
		
		btnReset.setBounds(490, 30, 70, 40);
		btnReset.setFont(f);
		this.add(btnReset);
		
		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mbRootPath = "/我的视频";
				init_combobox();
			}
		});
		
		btnAuth.setBounds(570, 30, 70, 40);
		btnAuth.setFont(f);
		this.add(btnAuth);
		
		btnAuth.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				authorize();
				
				String user_name = JOptionPane.showInputDialog(null, 
						"输入用户名", "wnpllrzodiac");
				String passwd = JOptionPane.showInputDialog(null, 
						"输入密码", "daqiao1");
				LoginInfo info = EmulateLoginBaidu.login(user_name, passwd);
				cookie_BDUSS = info.getBDUSS();
				TestEPG.httpd.setBDUSS(cookie_BDUSS);
				
				Clipboard clipboard = getToolkit().getSystemClipboard();//获取系统剪贴板;
				StringSelection text = new StringSelection(cookie_BDUSS);
				clipboard.setContents(text, null);
				JOptionPane.showMessageDialog(null,"BDUSS 已复制至剪贴板");
				
				//login("wnpllrzodiac", "daqiao1");
				//login("shxm.ma@163.com", "Git84hub");
			}
		});
		
		btnPlay.setBounds(570, 80, 80, 40);
		btnPlay.setFont(f);
		this.add(btnPlay);
		
		btnPlay.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int[] indices = listItem.getSelectedIndices();
				if (indices == null)
					return;
				
				System.out.println("Java selected indices length " + indices.length);
				
				if (indices.length == 1) {
					Map<String, Object> fileinfo = mFileList.get(indices[0]);
					boolean isdir = (Boolean) fileinfo.get("isdir");
					String path = (String) fileinfo.get("path");
					if (!isdir)
						play_url(path);
				}
				else if (indices.length > 1) {
					String first_filename = "";
					String play_url = null;
							
					try {
						File file = new File("playlist.m3u");
				        if (!file.exists())
				        	file.createNewFile();
				        play_url = file.getAbsolutePath();
				        
				        FileOutputStream out = new FileOutputStream(file,false); //如果追加方式用true
				        out.write("#EXTM3U\n".getBytes("utf-8"));
				        
				        for (int i=0;i<indices.length;i++) {
							int index = indices[i];
							Map<String, Object> fileinfo = mFileList.get(index);
							boolean isdir = (Boolean) fileinfo.get("isdir");
							String path = (String) fileinfo.get("path");
							
							if (i == 0)
								first_filename = path.toLowerCase();
							
							if (!isdir) {
								StringBuffer sb = new StringBuffer();
								sb.append("#EXTINF:song #");
								sb.append(i);
								sb.append(", ");
								sb.append(path);
								sb.append("\n");
								
								String encoded_path = URLEncoder.encode(path, "utf-8");
								String url = BAIDU_PCS_DOWNLOAD + encoded_path;
								sb.append(url);
								sb.append("\n");
								
						        out.write(sb.toString().getBytes("utf-8"));//注意需要转换对应的字符集
							}
						}
				        
				        out.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						return;
					}
					
					String exe_path = exe_vlc;
					if (first_filename.endsWith(".mp3") || 
							first_filename.endsWith(".flac") ||
							first_filename.endsWith(".ape") ||
							first_filename.endsWith(".ogg") ||
							first_filename.endsWith(".wav")) {
						exe_path = exe_foobar;
					}
					
					String[] cmd = null;
					if (isMac()) {
						cmd = new String[] {"open",
								"-a", "/Applications/VLC.app", "--args",
								play_url};
					}
					else {
						cmd = new String[] {exe_path, play_url};
					}
					
					openExe(cmd);
				}
			}
		});
		
		btnPaste.setBounds(570, 130, 80, 40);
		btnPaste.setFont(f);
		this.add(btnPaste);
		btnPaste.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (mOperatePaths == null || mOperatePaths.length == 0) {
					JOptionPane.showMessageDialog(
							null, "未选择源文件", "复制任务", 
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				for (int i=0;i<mOperatePaths.length;i++) {
					String path = mOperatePaths[i];
					
					int pos = path.lastIndexOf("/");
					if (pos > -1) {
						String to = mbRootPath + "/" + path.substring(pos + 1);
						if (!move_copy(path, to, mIsCopy)) {
							lblInfo.setText(String.format("%s %s 至 %s 失败", 
								mOperatePath, mIsCopy ? "复制" : "移动", to));
						}
					}
				}
				
				init_combobox();
				
				lblInfo.setText(String.format("%s(等文件) 成功 %s 至 %s", 
						mOperatePaths[0], mIsCopy ? "复制" : "移动", mbRootPath));
			}
		});
		
		btnDelete.setBounds(570, 180, 80, 40);
		btnDelete.setFont(f);
		this.add(btnDelete);
		btnDelete.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int []indices = listItem.getSelectedIndices();
				int count = indices.length;
				if (count == 0)
					return;
				
				for (int i=0;i<count;i++) {
					Map<String, Object> fileinfo = mFileList.get(indices[i]);
					mOperatePath = (String) fileinfo.get("path");
					if (!delete(mOperatePath)) {
						lblInfo.setText(mOperatePath + " 删除失败");
					}
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				mChildPath = "";
				init_combobox();
				
				if (count == 1)
					lblInfo.setText(mOperatePath + " 已删除");
				else
					lblInfo.setText("文件已删除");
			}
		});
		
		lblImage.setBounds(430, 240, 192, 192);
		this.add(lblImage);
		
		comboListBy = new JComboBox<String>(list_by_desc);
		comboListBy.setFont(f);
		comboListBy.setBounds(430, 80, 120, 40);
		comboListBy.setSelectedIndex(0);
		this.add(comboListBy);
		
		comboListBy.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (e.getStateChange() == ItemEvent.SELECTED){
					int index = comboListBy.getSelectedIndex();
					String new_list_by;
					switch (index) {
					case 0:
						new_list_by = "time";
						break;
					case 1:
						new_list_by = "name";
						break;
					case 2:
						new_list_by = "size";
						break;
					default:
						index = 0;
						System.out.println("Java: unknown list_by " + index);
						new_list_by = "time";
						break;
					}
					
					if (!list_by.equals(new_list_by)) {
						list_by = new_list_by;
						init_combobox();
						System.out.println("Java: list_by changed to " + list_by_desc[index]);
					}
				}
			}

		});

		editPath.setBounds(20, 450, 200, 40);
		editPath.setFont(f);
		editPath.setText("新建文件夹");
	    this.add(editPath);
	    
	    btnCreateFolder.setBounds(230, 450, 80, 40);
	    btnCreateFolder.setFont(f);
		this.add(btnCreateFolder);
		btnCreateFolder.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String folder_path = mbRootPath + "/" + editPath.getText();
				if (mkdir(folder_path)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					init_combobox();
					
					lblInfo.setText(String.format("文件夹 %s 已成功创建!", folder_path));
				}
			}
		});
		
		btnDownload.setFont(f);
		btnDownload.setBounds(320, 450, 80, 40);
		btnDownload.setFont(f);
		this.add(btnDownload);
		btnDownload.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (bDownloading) {
					bInterrupt = true;
					JOptionPane.showMessageDialog(
							null, "下载已取消", "下载任务", 
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				download();
			}
		});
		
		btnYunDownload.setFont(f);
		btnYunDownload.setBounds(410, 450, 80, 40);
		btnYunDownload.setFont(f);
		this.add(btnYunDownload);
		btnYunDownload.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//String default_path = "http://dlsw.baidu.com/sw-search-sp/soft/cc/13478/npp_V6.8.2_Installer.1440141668.exe";
				String default_path = "ed2k://|file|[www.ed2kers.com]" +
						"TLF-VIDEO-04.12.14.The.Maze.Runner" +
						".(2014).iNT.BDRip.720p.AC3.X264" +
						"-TLF.mkv|2094704851|E30B41" +
						"BAFE4D3056AF027232A7FEF3A6|h=JD2TWIEK" +
						"2PLEMO3ENWF3FCS5RF6W5MBF|/";
				String download_url = JOptionPane.showInputDialog(null, 
						"输入下载地址", default_path);
				if (download_url == null || download_url.equals(""))
					return;
				
				System.out.println("Java: download url: " + download_url);
				String save_path = mbRootPath;
				save_path += "/";
				
				int pos1, pos2;
				if (download_url.startsWith("ed2k://")) {
					pos1 = download_url.indexOf("|file|");
					pos2 = download_url.indexOf("|", pos1 + "|file|".length());
					save_path += download_url.substring(pos1 + "|file|".length(), pos2);
				}
				else {
					pos1 = download_url.lastIndexOf("/");
					pos2 = download_url.lastIndexOf("?");
					if (pos2 == -1)
						pos2 = download_url.length();
					if (pos2 <= pos1 + 1)
						save_path += "default.mp4";
					else
						save_path += download_url.substring(pos1 + 1, pos2);
				}
				
				if (!add_cloud_dl_v2(download_url, save_path)) {
					JOptionPane.showMessageDialog(
							null, "添加任务失败", "云任务", 
							JOptionPane.ERROR_MESSAGE);
				}
				
				lblInfo.setText("离线下载任务添加成功!");
			}
		});
		
		btnUpload.setFont(f);
		btnUpload.setBounds(500, 450, 80, 40);
		btnUpload.setFont(f);
		this.add(btnUpload);
		btnUpload.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String upload_path = SelUploadFile();
				if (upload_path == null)
					return;
				
				int pos = upload_path.lastIndexOf(SeparatorUtils.getFileSeparator());
				if (pos == -1)
					return;
				
				String filename = upload_path.substring(pos + 1);
				String save_path = mbRootPath + "/" + filename;
				System.out.println("file upload src path: " + upload_path + " save_path: " + save_path);
				
				UploadWorker worker = new UploadWorker(upload_path, save_path);
				worker.execute();
			}
		});
		
		btnSearch.setFont(f);
		btnSearch.setBounds(590, 450, 80, 40);
		btnSearch.setFont(f);
		this.add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mFileList = search("/", editPath.getText(), true);
				listModel.clear();
				
				int size = mFileList.size();
				for (int i=0;i<size;i++) {
					Map<String, Object> fileinfo = mFileList.get(i);
					String path = (String) fileinfo.get("path");
					String filename = (String) fileinfo.get("filename");
					
					listModel.addElement(filename);
				}
				
				lblRootPath.setText("搜索结果");
			}
		});
		
		cbTranscode.setBounds(20, 500, 80, 20);
		this.add(cbTranscode);
		
		cbUseVLC.setBounds(20, 520, 60, 20);
		cbUseVLC.setSelected(true);
		this.add(cbUseVLC);
		
		cbOrder.setBounds(80, 520, 80, 20);
		cbOrder.setSelected(true);
		cbOrder.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				// TODO Auto-generated method stub
				if (e.getStateChange() == ItemEvent.SELECTED)
					list_order = "desc";
				else
					list_order = "asc";
				
				System.out.println("Java: list_order changed to " + list_order);
				init_combobox();
			}
		});
		this.add(cbOrder);
		
		lblInfo.setBounds(100, 500, 500, 20);
		this.add(lblInfo);
		
		//LoginInfo info = EmulateLoginBaidu.login("wnpllrzodiac", "daqiao1");
		//mBDUSS = info.getBDUSS();
		//mBaiduToken = info.getToken();
		
		File f = new File("e:/dump/test2");
		
		init_combobox();
	}
	
	private String SelUploadFile() {
		// 弹出"选择上传文件"对话框
		FileDialog fload = new FileDialog(new Frame(), "选择上传文件",
				FileDialog.LOAD);

		fload.setVisible(true);

		return fload.getDirectory() + fload.getFile();
	}
	
	private String SelUploadFile2() {
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"JPG & GIF Images", "jpg", "gif");
		//chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			System.out.println("You choose to open this file: " + path);
			return path;
		}
		
		return null;
	}
	
	private boolean process_lrc(String lrc_path) {
		try {
			LrcParser2 parser = new LrcParser2();
			
			URL url = new URL(lrc_path);
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(3000);
			InputStream inStream = conn.getInputStream();
			
			LrcInfo info = null;
			info = parser.readLrc(inStream, "GB2312");
			//info = parser.parseStream(inStream, "GB2312");
			System.out.println(String.format("Java: lrc: artist %s, album %s, title %s",
					info.getArtist(), info.getAlbum(), info.getTitle()));
			List<TimeLrc> Lyric = info.getInfos();
			int count = Lyric.size();
			StringBuffer sb = new StringBuffer();
			for (int i=0;i<count;i++) {
				TimeLrc lrc = Lyric.get(i);
				String strLine = String.format("Java: lrc time %s: text %s", lrc.getTimePoint(), lrc.getLrcString());
				sb.append(strLine);
				sb.append("\n");
			}
			
			System.out.println("Java: all lrc " + sb.toString());
			return true;
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private void searchSub(String key) {
		
		List<SearchItem> list = ShooterUtil.search(key, 10);
		for (int j=0;j<list.size();j++) {
			System.out.println("title " + list.get(j).mVideoName);
		}
		
		List<String> titleList = new ArrayList<String>();
		for (int i=0;i<list.size();i++) {
			String title = list.get(i).mNativeName;
			if (title == null || title.isEmpty())
				title = list.get(i).mVideoName;
			title += "(";
			title += list.get(i).mLangDesc;
			title += ")";
			
			titleList.add(title);
		}
		
		String []titles = titleList.toArray(new String[titleList.size()]);
		String strTitle = (String) JOptionPane.showInputDialog(null,
				 "选择字幕文件", "选择在线字幕", JOptionPane.INFORMATION_MESSAGE ,
				 null, titles , titles[0]);
		if (strTitle == null)
			return;
		
		int id = -1;
		for (int i=0;i<list.size();i++) {
			if (strTitle.contains(list.get(i).mNativeName) ||
					strTitle.contains(list.get(i).mVideoName)) {
				id = list.get(i).mId;
				break;
			}
		}
		
		if (id == -1)
			return;
		
		DetailItem detailItem = ShooterUtil.detail(id);
		
		// step2
		String download_url = detailItem.mUrl;
		String save_path = null;
		if (detailItem.mFileName != null && !detailItem.mFileName.isEmpty())
			save_path = detailItem.mFileName;
		else
			save_path = detailItem.mNativeName;
		
		if (detailItem.mArvList != null) {
			String []filenames = detailItem.mArvList.toArray(new String[detailItem.mArvList.size()]);
			String filename = (String) JOptionPane.showInputDialog(null,
					 "选择压缩包内字幕文件", "选择字幕文件", JOptionPane.INFORMATION_MESSAGE ,
					 null, filenames , filenames[0]);
			if (filename == null)
				return;
			
			int path_pos = detailItem.mUrl.lastIndexOf("/");
			String encoded_path;
			try {
				encoded_path = URLEncoder.encode(filename, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
			
			download_url = detailItem.mUrl.substring(0, path_pos + 1) + 
					"-/" + String.valueOf(0 + 1) + "/" + encoded_path;
			save_path = "./" + filename;
		}

		download_url = download_url.replace("+", "%20");
		download_url = download_url.replace("%28", "(");
		download_url = download_url.replace("%29", ")");
		download_url = download_url.replace("%5B", "[");
		download_url = download_url.replace("%5D", "]");
		System.out.println("download_url " + download_url);
		
		URL url = null;
		try {
			url = new URL(download_url);
			
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			//conn.setConnectTimeout(3000);
			//conn.setReadTimeout(3000);

			conn.connect();
			int code = conn.getResponseCode();
			System.out.println("http response code " + code);
			InputStream inStream = conn.getInputStream();
			
			final FileOutputStream fos = new FileOutputStream(save_path);

			try {
				int readed;
				byte[] buf = new byte[mDownloadBlockSize];

				bDownloading = true;
				while ((readed = inStream.read(buf)) != -1 && !bInterrupt) {
					fos.write(buf, 0, readed);
				}

				fos.close();
				inStream.close();
				String title = save_path;
				if (title.length() > 32)
					title = title.substring(0, 32) + "...";
				lblInfo.setText("字幕 " + title + " 下载成功!");
			} catch (Exception ex) {
				ex.printStackTrace();
				lblInfo.setText("字幕 " + save_path + "下载失败: " + ex.getMessage());
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private String getOperatePath() {
		int index = listItem.getSelectedIndex();
		if (index == -1)
			return null;
		
		Map<String, Object> fileinfo = mFileList.get(index);
		return (String) fileinfo.get("path");
	}
	
	private String[] getOperatePaths() {
		int []indices = listItem.getSelectedIndices();
		if (indices == null || indices.length == 0)
			return null;
		
		List<String>pathList = new ArrayList<String>();
		for (int i=0;i<indices.length;i++) {
			Map<String, Object> fileinfo = mFileList.get(indices[i]);
			String path = (String) fileinfo.get("path");
			pathList.add(path);
			System.out.println("add select path: " + path);
		}
		
		return pathList.toArray(new String [indices.length]);
	}
	
	private void play_url(String path) {
		try {
			String encoded_path = URLEncoder.encode(path, "utf-8");

			String url = String.format(BAIDU_PCS_DOWNLOAD_PROXY_FMT, 
					TestEPG.getHttpPort());
			url += encoded_path;
			System.out.println("ready to play url: " + url);
			lblInfo.setText("ready to play url: " + url);
			
			String exe_path = exe_vlc;
			if (!cbUseVLC.isSelected())
				exe_path = exe_ffplay;
			if (path.toLowerCase().endsWith(".mp3") || 
					path.toLowerCase().endsWith(".flac") ||
					path.toLowerCase().endsWith(".ape") ||
					path.toLowerCase().endsWith("open.ogg") ||
					path.toLowerCase().endsWith(".wav")) {
				exe_path = exe_foobar;
			}

			String[] cmd = null;
			if (isMac() && cbUseVLC.isSelected()) {
				cmd = new String[] {"open",
						"-a", "/Applications/VLC.app", "--args",
						url};
			}
			else {
				cmd = new String[] {exe_path, url};
			}
			
			openExe(cmd);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean isMac() {
		Properties prop = System.getProperties();

		String os = prop.getProperty("os.name");
		System.out.println("Java: os.name " + os);

		return os.toLowerCase().contains("mac");
	}
	
	private class MyRender extends JLabel implements ListCellRenderer<String> {
		public MyRender() {
            this.setOpaque(true);
        }
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends String> list, String value, 
				int index, boolean isSelected, boolean cellHasFocus) {
			// TODO Auto-generated method stub
			this.setText(value);
			this.setFont(f);
			
			Color background = Color.WHITE;
            Color foreground = Color.BLACK;
 
            Map<String, Object> fileinfo = mFileList.get(index);
			boolean isdir = (Boolean) fileinfo.get("isdir");
			
            if (isdir) {
                foreground = Color.BLUE;
            }
            
            if (isSelected) {
            	background = new Color(170, 170, 255);
            }
 
            setBackground(background);
            setForeground(foreground);
            
			return this;
		}
	};
		
	private void showInfo(int index) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, Object> fileinfo = mFileList.get(index);
		boolean isdir = (Boolean) fileinfo.get("isdir");
		String path = (String) fileinfo.get("path");
		
		if (!isdir) {
			List<Map<String, Object>> metaList = meta(path);
			if (metaList != null && metaList.size() > 0) {
				Map<String, Object> metainfo = metaList.get(0);
				long meta_size = (Long) metainfo.get("filesize");
				int meta_mtime = (Integer)metainfo.get("mtime");
				Date date = new Date(meta_mtime/*unit is sec*/ * 1000L);
				lblInfo.setText("文件大小: " + getFileSize(meta_size) + 
						"   修改时间 " + sdf.format(date));
			}
			
			BufferedImage image = thumbnail(path, 320, 240);
			if (image != null)
				lblImage.setIcon(new ImageIcon(image));
		}
	}
	
	private void authorize() {
		if (Desktop.isDesktopSupported()){
            try {
                //创建一个URI实例
                URI uri = URI.create(BAIDU_PCS_AUTHORIZE); 
                //获取当前系统桌面扩展
                Desktop dp = Desktop.getDesktop();
                //判断系统桌面是否支持要执行的功能
                if(dp.isSupported(Desktop.Action.BROWSE)){
                    //获取系统默认浏览器打开链接
                    dp.browse(uri);    
                }
            } catch(java.lang.NullPointerException e){
                //此为uri为空时抛出异常
            } catch (java.io.IOException e) {
                //此为无法获取系统默认浏览器
            }             
        }
	}
	
	private void downloadFolder(final String folder, final String save_path) {
		Runnable r = new Runnable() {
			public void run() {
				File f = new File(save_path);
				if (!f.exists()) {
					f.mkdirs();
				}
				
				List<Map<String, Object>> fileList = list(folder);
				
				for (int i=0;i<fileList.size();i++) {
					Map<String, Object>fileinfo = fileList.get(i);
					boolean isdir = (Boolean) fileinfo.get("isdir");
					String path = (String) fileinfo.get("path");
					String filename = (String) fileinfo.get("filename");
					
					if (isdir) {
						downloadFolder(path, save_path + "/" + filename);
					}
					else {
						try {
							String encoded_path = URLEncoder.encode(path, "utf-8");

							String url = BAIDU_PCS_DOWNLOAD_V2 + encoded_path;
							System.out.println("ready to download url: " + url);
							lblInfo.setText("ready to download url: " + url);
							
							downloadFile(url, filename, 
									save_path + "/" + filename, false,
									0);
						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		};

		Thread t = new Thread(r);
		t.start();
	}
	
	private void download() {
		int index = listItem.getSelectedIndex();
		if (index < 0) {
			System.out.println("Java: no download file selected");
			return;
		}
		
		Map<String, Object> fileinfo = mFileList.get(index);
		boolean isdir = (Boolean) fileinfo.get("isdir");
		String path = (String) fileinfo.get("path");
		if (isdir) {
			downloadFolder(path, "e:/test2");
		}
		else {
			try {
				String encoded_path = URLEncoder.encode(path, "utf-8");

				String url = BAIDU_PCS_DOWNLOAD_V2 + encoded_path;
				System.out.println("ready to download url: " + url);
				lblInfo.setText("ready to download url: " + url);
				
				int pos = path.lastIndexOf("/");
				String filename = path.substring(pos + 1);
				
				//String userDirLocation = System.getProperty("user.dir");
				//File userDir = new File(userDirLocation);
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(new File(filename));
				int i = fc.showSaveDialog(null);
                if (i== JFileChooser.APPROVE_OPTION) { //打开文件
                    String file_path = fc.getSelectedFile().getAbsolutePath();
                    String file_name = fc.getSelectedFile().getName();
                    
                    fc.setCurrentDirectory(fc.getCurrentDirectory());
                    
                    File f = new File(file_path);
    				long from = 0L;
    				if (f.exists()) {
    					int res = JOptionPane.showConfirmDialog(
    							fc, 
    							file_name + " 文件已经存在, 是否续传?", 
    							"文件存在", 
    							JOptionPane.YES_NO_OPTION, 
    							JOptionPane.WARNING_MESSAGE);
    					
						if (res == JOptionPane.YES_OPTION) {
							from = f.length();
							System.out.println("Java: resume download from " + from);
						}
						/*else if (res == JOptionPane.NO_OPTION)
							System.out.println ("Result=No");
						else if (res == JOptionPane.CANCEL_OPTION)
							System.out.println("Result=Cancel");
						else if (res == JOptionPane.CLOSED_OPTION)
							System.out.println("Result=Closed Window");
						else
							System.out.println ("Unknow Result="+res);*/
    				}
    				
    				downloadFile(url, filename, file_path, true, from);
                } else {
                    System.out.println("NO file selected");
                    return;
                }
				
				/*
				// 弹出"保存文件"对话框
				FileDialog fsave = new FileDialog(new Frame(), "保存文件",
						FileDialog.SAVE);
			
				FilenameFilter ff = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.endsWith("flac")) {
							return true;
						}
						
						return false;
					}
				};
				
				//fsave.setFilenameFilter(ff);
				fsave.setFile(filename);
				fsave.setVisible(true);
				if (fsave.getFile() == null || fsave.getDirectory() == null)
					return;
			
				final String save_path = fsave.getDirectory() + fsave.getFile();
				System.out.println("file save path: " + save_path);
				
				File f = new File(save_path);
				long from = 0L;
				if (f.exists()) {
					from = f.length();
					System.out.println("Java: resume download from " + from);
				}
				
				downloadFile(url, filename, save_path, true, from);*/
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void downloadFile(String path, String filename, 
			final String save_path, final boolean bSingleFile,
			final long from) {
		try {
			URL url = new URL(path);
			URLConnection urlCon = url.openConnection();

			urlCon.setRequestProperty("Cookie", 
					"BDUSS=" + cookie_BDUSS);
			
			if (from > 0) {
				// Range: bytes=500-999
				urlCon.setRequestProperty("RANGE", String.format("bytes=%d-", from));
				System.out.println(String.format("set range: bytes=%d-", from));
			}
			
			// 显示下载信息
			System.out.println("文件下载信息: ");
			System.out.println("host : " + url.getHost());
			System.out.println("port :" + url.getPort());
			System.out.println("Contenttype : " + urlCon.getContentType());
			System.out.println("Contentlength : " + urlCon.getContentLength());
			
			//final FileOutputStream fos = new FileOutputStream(save_path);
			
			final RandomAccessFile randomFile = new RandomAccessFile(save_path, "rw");
			if (from > 0) {
				randomFile.seek(from);
				System.out.println("Java: seek file to " + from);
			}
			
			final InputStream is = urlCon.getInputStream();
		    Runnable r = new Runnable()
		    {
		    	public void run() {
		          try {
		        	  long downloaded = from;
		        	  int readed;
		        	  byte []buf= new byte[4096];
		        	  long start_msec = System.currentTimeMillis();
		        	  long last_msec = 0;
		        	  
		        	  bDownloading = true;
		              while((readed = is.read(buf))!=-1 && !bInterrupt) {
		            	  //fos.write(buf, 0, readed);
		            	  randomFile.write(buf, 0, readed);
		            	  downloaded += readed;
		            	  
		            	  long cur_msec = System.currentTimeMillis();
		            	  if (cur_msec - last_msec > 300) {
		            		  long elapsed_msec = cur_msec - start_msec;
		            		  double speed = (downloaded - from) / (double)elapsed_msec;
			            	  lblInfo.setText(String.format("文件下载进度 %s, 速度 %.3f kB/s", 
			            			  getFileSize(downloaded), speed));  
			            	  last_msec = cur_msec;
		            	  }
		              }
		              
		              //fos.close();
		              
		              if (bInterrupt)
		            	  lblInfo.setText("文件 " + save_path + " 下载取消");
		              else
		            	  lblInfo.setText("文件 " + save_path + " 下载成功!");
		              
		              bInterrupt = false;
		              bDownloading = false;
		              if (bSingleFile)
		            	  btnDownload.setText("下载");
		          }
		          catch (Exception ex) {
		        	  ex.printStackTrace();
		              lblInfo.setText("下载失败: " + ex.getMessage());
		          }
		          finally {
		        	  try {
		        		  if (is != null)
		        			  is.close();
		        		  if (randomFile != null)
		        			  randomFile.close();
		        	  } catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
		        	  }
		          }
		      }
		  };
		  
		  Thread t = new Thread(r);
		  t.start();
		  
		  if (bSingleFile)
			  btnDownload.setText("取消");
		  
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String uploadPiece(byte[] bytes, String target) {
		try {
			ContentBody bsData = new ByteArrayBody(bytes, "upload.tmp");

			String url_path = null;
			boolean isPiece = true;
			if (target != null && target.length() > 0) {
				url_path = BAIDU_PCS_UPLOAD + "&path=" + target;
				isPiece = false;
			}
			else
				url_path = BAIDU_PCS_UPLOAD + "&type=tmpfile";
			System.out.println("Java: uploadPiece() " + url_path);

			HttpPost httppost = new HttpPost(url_path);

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addPart("file", bsData);
			httppost.setEntity(builder.build());
			
			/*ProgressHttpEntityWrapper.ProgressCallback progressCallback = 
					new ProgressHttpEntityWrapper.ProgressCallback() {

		        @Override
		        public void progress(float progress) {
		            //Use the progress
		        	//System.out.println(String.format("Java: progress %.3f", progress));
		        }

		    };

			httppost.setEntity(new ProgressHttpEntityWrapper(builder.build(), progressCallback));*/

			HttpResponse response = HttpClients.createDefault().execute(httppost);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.out.println(String.format(
						"Java: response is not ok: %d %s", response
								.getStatusLine().getStatusCode(), EntityUtils
								.toString(response.getEntity())));
				return null;
			}

			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			
			String md5 = root.getString("md5");
			
			if (isPiece) {
				System.out.println("Java: piece file md5 " + md5);
			}
			else {
				String res_path = root.getString("path");
				int size = root.getInt("size");
				System.out.println(String.format("Java: whole file path: %s, size %d, md5 %d",
						res_path, size, md5));
			}

			return md5;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	private boolean create_superfile(List<String> md5s, String target) {
		if ((md5s != null) && (md5s.size() > 0) && (target != null)
				&& (target.length() > 0)) {
			String url = BAIDU_PCS_CREATE_SUPER_FILE + "&path=" + target;

			List<BasicNameValuePair> bodyParams = new ArrayList<BasicNameValuePair>();

			if (md5s != null) {
				JSONArray json = new JSONArray(md5s);
				Map<String, JSONArray> map = new HashMap<String, JSONArray>();
				map.put("block_list", json);

				JSONObject md5list = new JSONObject(map);

				bodyParams.add(new BasicNameValuePair("param", md5list
						.toString()));
			}

			System.out.println("Java: create_superfile() " + url);
			HttpPost post = new HttpPost(url);
			try {
				post.setEntity(new UrlEncodedFormEntity(bodyParams, "utf-8"));
				
				HttpResponse response = HttpClients.createDefault().execute(post);
				if (response.getStatusLine().getStatusCode() != 200) {
					System.out.println(String.format(
							"Java: response is not ok: %d %s", response
									.getStatusLine().getStatusCode(), EntityUtils
									.toString(response.getEntity())));
					return false;
				}

				String result = EntityUtils.toString(response.getEntity());
				JSONTokener jsonParser = new JSONTokener(result);
				JSONObject root = (JSONObject) jsonParser.nextValue();
				String res_path = root.getString("path");
				int size = root.getInt("size");
				System.out.println("Java: superfile() " + res_path + " , size " + size);
				
				return true;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return false;
	}

	private void init_combobox() {
		lblRootPath.setText(mbRootPath);
		
		mFileList = list(mbRootPath);
		if (mFileList == null)
			return;
		
		listModel.clear();
		
		int size = mFileList.size();
		int child_index = -1;
		for (int i=0;i<size;i++) {
			Map<String, Object> fileinfo = mFileList.get(i);
			String path = (String) fileinfo.get("path");
			String filename = (String) fileinfo.get("filename");
			
			if (path.equals(mChildPath)) {
				System.out.println(String.format("Java: found mChildPath at #%d, %s, %s",
						i, path, mChildPath));
				child_index = i;
			}
			
			listModel.addElement(filename);
		}
		
		if (child_index != -1) {
			listItem.setSelectedIndex(child_index);
			System.out.println("Java: setSelectedIndex at #" + child_index);
		}
	}
	
	private boolean streaming(String path, String type) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = BAIDU_PCS_STREAMING + encoded_path;
		url += "&type=";
		url += type;
		
		System.out.println("Java: streaming() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to get streaming file, code not 200: " + 
						response.getStatusLine().getStatusCode());
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			saveAsFileWriter(result);
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private void saveAsFileWriter(String content) {

		FileWriter fwriter = null;
		String filename = "index.m3u8";
		try {
			fwriter = new FileWriter(filename);
			fwriter.write(content);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fwriter.flush();
				fwriter.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private long getFolderSize(String path) {
		List<Map<String, Object>> infoList = list(path);
		if (infoList == null)
			return 0;
		
		long total_size = 0;
		int count = infoList.size();
		for (int i=0;i<count;i++) {
			Map<String, Object> fileinfo = infoList.get(i);
			boolean item_isdir = (Boolean) fileinfo.get("isdir");
			String item_path = (String) fileinfo.get("path");
			long item_size = (Long) fileinfo.get("filesize");
			if (item_isdir)
				total_size += getFolderSize(item_path);
			else
				total_size += item_size;
		}
		
		if (lblInfo.getText().length() > 12)
			lblInfo.setText("统计文件夹大小中");
		else
			lblInfo.setText(lblInfo.getText() + ".");
		return total_size;
	}
	
	private List<Map<String, Object>> list(String listPath) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(listPath, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = BAIDU_PCS_LIST + encoded_path;
		url += "&by=";
		url += list_by;
		url += "&order=";
		url += list_order;
		
		System.out.println("Java: list() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to list(): code " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONArray list = root.getJSONArray("list");
			int cnt = list.length();
			
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i=0;i<cnt;i++) {
				JSONObject item = list.getJSONObject(i);
				String path = item.getString("path");
				long filesize = item.getLong("size");
				int isdir = item.getInt("isdir");
				
				String filename = path;
				int pos = filename.lastIndexOf("/");
				if (pos > -1) {
					filename = filename.substring(pos + 1);
				}
				
				Map<String, Object> fileinfo = new HashMap<String, Object>();
				fileinfo.put("path", path);
				fileinfo.put("filename", filename);
				fileinfo.put("filesize", filesize);
				fileinfo.put("isdir", isdir == 1 ? true : false);
				
				fileList.add(fileinfo);
			}
			
			return fileList;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private List<Map<String, Object>> search(String search_path, String wd, boolean recursive) {
		String encoded_wd = null;
		try {
			encoded_wd = URLEncoder.encode(wd, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = BAIDU_PCS_SEARCH;
		url += "&path=";
		url += search_path;
		url += "&wd=";
		url += encoded_wd;
		url += "&re=";
		url += (recursive ? "1" : "0");
		
		System.out.println("Java: search() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: failed to list(): code " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONArray list = root.getJSONArray("list");
			int cnt = list.length();
			
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i=0;i<cnt;i++) {
				JSONObject item = list.getJSONObject(i);
				String path = item.getString("path");
				long filesize = item.getLong("size");
				int isdir = item.getInt("isdir");
				
				String filename = path;
				int pos = filename.lastIndexOf("/");
				if (pos > -1) {
					filename = filename.substring(pos + 1);
				}
				
				Map<String, Object> fileinfo = new HashMap<String, Object>();
				fileinfo.put("path", path);
				fileinfo.put("filename", filename);
				fileinfo.put("filesize", filesize);
				fileinfo.put("isdir", isdir == 1 ? true : false);
				
				fileList.add(fileinfo);
			}
			
			return fileList;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private List<Map<String, Object>> meta(String filepath) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(filepath, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = BAIDU_PCS_META + encoded_path;
		
		System.out.println("Java: meta() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONArray list = root.getJSONArray("list");
			int cnt = list.length();
			
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i=0;i<cnt;i++) {
				JSONObject item = list.getJSONObject(i);
				String path = item.getString("path");
				long filesize = item.getLong("size");
				int isdir = item.getInt("isdir");
				int mtime = item.getInt("mtime");
				int ctime = item.getInt("ctime");
				
				String filename = path;
				int pos = filename.lastIndexOf("/");
				if (pos > -1) {
					filename = filename.substring(pos + 1);
				}
				
				Map<String, Object> fileinfo = new HashMap<String, Object>();
				fileinfo.put("path", path);
				fileinfo.put("filename", filename);
				fileinfo.put("filesize", filesize);
				fileinfo.put("isdir", isdir == 1 ? true : false);
				fileinfo.put("mtime", mtime);
				fileinfo.put("ctime", ctime);
				
				fileList.add(fileinfo);
			}
			
			return fileList;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private BufferedImage thumbnail(String filepath, int width, int height) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(filepath, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = String.format(BAIDU_PCS_THUMBNAIL,
				encoded_path, width, height);
		
		System.out.println("Java: thumbnail() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}

			InputStream is = response.getEntity().getContent();
			return ImageIO.read(is);
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private boolean add_cloud_dl(String source_url) {	
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("method", "add_task"));
		params.add(new BasicNameValuePair("access_token", mbOauth));
		params.add(new BasicNameValuePair("source_url", source_url));
		
		String save_path = "/apps/pcstest_oauth/";
		if (source_url.startsWith("ed2k://")) {
			int pos = source_url.indexOf("|file|");
			int pos2 = source_url.indexOf("|", pos + "|file|".length());
			save_path += source_url.substring(pos + "|file|".length(), pos2);
		}
		else {
			save_path += "123.exe";
		}
		
		System.out.println("save_path: " + save_path);
		
		params.add(new BasicNameValuePair("save_path", save_path));
		params.add(new BasicNameValuePair("type", "0"));
		params.add(new BasicNameValuePair("v", "1"));
		/*params.add(new BasicNameValuePair("rate_limit", "100"));
		params.add(new BasicNameValuePair("timeout", "100"));
		params.add(new BasicNameValuePair("callback", "www.baidu.com"));*/
		
		String url = "https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl?" + 
				buildParams(params);
		/*String url = "https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl?" + 
				"method=add_task" + "&access_token=" + mbOauth +
				"&source_url=" + "http://6.jsdx3.crsky.com/201107/winzip150zh.exe" + 
				"&save_path=" + encoded_path + 
				"&type=0";*/

		System.out.println("Java: add_cloud_dl() " + url);
		
		HttpResponse response;
		
		try {
			HttpPost httppost = new HttpPost(url);
			httppost.setHeader("Cookie", "BDUSS=" + cookie_BDUSS);
			
			response = HttpClients.createDefault().execute(httppost);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println(String.format("Java: response is not ok: %d %s",
						response.getStatusLine().getStatusCode(),
						EntityUtils.toString(response.getEntity())));
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("result: " + result);
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			if (root.has("error_code")) {
				int error_code = root.getInt("error_code");
				String error_msg = root.getString("error_msg");
				long request_id = root.getLong("request_id");
				System.out.println(String.format("Java: failed to add cloud job: %d(%s), request_id %d",
						error_code, error_msg, request_id));
				return false;
			}
			
			int task_id = root.getInt("task_id");
			long request_id = root.getLong("request_id");
			System.out.println(String.format("Java: new cloud job added: taskid: %d, request_id: %d",
					task_id, request_id));
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean add_cloud_dl_v2(String source_url, String save_path) {
		String encoded_source_url = null;
		String encoded_save_path = null;
		
		try {
			encoded_source_url = URLEncoder.encode(source_url, "utf-8");
			encoded_save_path = URLEncoder.encode(save_path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		
		String url = BAIDU_PCS_CLOUD_DL_V2;
		url += "&source_url=";
		url += encoded_source_url;
		url += "&save_path=";
		url += encoded_save_path;
		System.out.println("Java: add_cloud_dl() " + url);
		
		HttpResponse response;
		
		try {
			HttpPost httppost = new HttpPost(url);
			httppost.setHeader("Cookie", "BDUSS=" + cookie_BDUSS);

			response = HttpClients.createDefault().execute(httppost);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println(String.format("Java: response is not ok: %d %s",
						response.getStatusLine().getStatusCode(),
						EntityUtils.toString(response.getEntity())));
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			if (root.has("error_code")) {
				System.out.println("result: " + result);
				int error_code = root.getInt("error_code");
				String error_msg = root.getString("error_msg");
				long request_id = root.getLong("request_id");
				System.out.println(String.format("Java: failed to add cloud job: %d(%s), request_id %d",
						error_code, error_msg, request_id));
				return false;
			}
			
			int task_id = root.getInt("task_id");
			long request_id = root.getLong("request_id");
			System.out.println(String.format("Java: new cloud job added: taskid: %d, request_id: %d",
					task_id, request_id));
			
			CheckCloudTaskWorker worker = new CheckCloudTaskWorker(task_id);
			worker.execute();
			
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean delete(String path) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = BAIDU_PCS_DELETE + encoded_path;
		
		System.out.println("Java: delete() " + url);
		HttpPost request = new HttpPost(url);
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}

			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			
			//"request_id":4043312866
			long request_id = root.getLong("request_id");
			System.out.println(String.format("Java: file %s deleted(request_id %d)", 
					path, request_id));
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean mkdir(String path) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = BAIDU_PCS_MKDIR + encoded_path;
		
		System.out.println("Java: mkdir() " + url);
		HttpPost request = new HttpPost(url);
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}

			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			
			//"path":"/apps/yunfom/music",
			//"ctime":1331183814,
			//"mtime":1331183814,
			int fs_id = root.getInt("fs_id");
			String item_path = root.getString("path");
			System.out.println(String.format("Java: folder %s created", item_path));
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean move_copy(String from, String to, boolean isCopy) {
		String encoded_from = null;
		String encoded_to = null;
		try {
			encoded_from = URLEncoder.encode(from, "utf-8");
			encoded_to = URLEncoder.encode(to, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = null;
		if (isCopy)
			url = String.format(BAIDU_PCS_COPY, encoded_from, encoded_to);
		else
			url = String.format(BAIDU_PCS_MOVE, encoded_from, encoded_to);
		
		System.out.println("Java: move() " + url);
		HttpPost request = new HttpPost(url);
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			}

			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			
			JSONObject extra = root.getJSONObject("extra");
			JSONArray list = extra.getJSONArray("list");
			for (int i=0;i<list.length();i++) {
				JSONObject item = list.getJSONObject(i);
				String item_from = item.getString("from");
				String item_to = item.getString("to");
				System.out.println(String.format("Java: %s %s to %s successfully",
						item_from, isCopy?"copy":"move", item_to));
			}
			
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private String buildParams(List<BasicNameValuePair> urlParams) {
		String ret = null;

		if ((urlParams != null) && (urlParams.size() > 0))
			try {
				HttpEntity paramsEntity = new UrlEncodedFormEntity(urlParams,
						"utf8");
				ret = EntityUtils.toString(paramsEntity);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		return ret;
	}
	
	public boolean login(String username, String password){
		//是否成功登陆的标记
		boolean isLogin = false;
		CloseableHttpClient httpClient;
		CloseableHttpResponse response;
		try {
			HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
			httpClient = httpClientBuilder.build();  
			
			/**1,BAIDUID**/
			String baiduId = null;
			HttpGet get_main = new HttpGet("http://tieba.baidu.com/dc/common/tbs/");
			response = httpClient.execute(get_main);
			get_main.abort();
			HeaderIterator it = response.headerIterator("Set-Cookie");
			while(it.hasNext())
				baiduId = it.next().toString();
			baiduId = HttpUtils.mid(baiduId,":",";");
			System.out.println("[step 1] BAIDUID: " + baiduId);
			
			/**2,token**/
			HttpGet get_token = new HttpGet("https://passport.baidu.com/v2/api/?getapi&tpl=mn");
			response = httpClient.execute(get_token);
			String token = EntityUtils.toString(response.getEntity(),"utf-8");
			get_token.abort();
			token = HttpUtils.mid(token,"_token='","'");
			System.out.println("[step 2] TOKEN: " + token);
			
			/**3,Login**/
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("username", username);				map.put("password", password);
			map.put("token", token);					map.put("isPhone", "false");
			map.put("quick_user", "0");					map.put("tt", System.currentTimeMillis()+"");
			map.put("loginmerge", "true");				map.put("logintype", "dialogLogin");
			map.put("splogin", "rate");					map.put("mem_pass", "on");
			map.put("tpl", "mn");						map.put("apiver", "v3");
			map.put("u", "http://www.baidu.com/");		map.put("safeflg", "0");
			map.put("ppui_logintime", "43661");			map.put("charset", "utf-8");
			
			//封装
			HttpEntity entity = HttpUtils.mapToEntity(map);
			HttpPost http_login = new HttpPost("https://passport.baidu.com/v2/api/?login");
			http_login.setEntity(entity);
			response = httpClient.execute(http_login);
			http_login.abort();
			
			it = response.headerIterator();
			while(it.hasNext()){
				//这里是根据是否写入的BDUSS-cookie判断是否登录成功
				String str = it.next().toString();
				if (str.contains("BDUSS")){
					System.out.println("str " + str);
					String BDUSS = HttpUtils.mid(str, "BDUSS=", ";");
					System.out.println("BDUSS: " + BDUSS);
					isLogin = true;
					break;
				}
			}
			System.out.println("[step 3] login status: " + (isLogin ? "OK" : "failed"));
			return isLogin;
		} catch (Exception e) {
			throw new RuntimeException("未知错误");
		}
	}
	
	private String getFileSize(long size) {
	    String strSize;
	    if (size < 0)
	    	return "N/A";
	    
	    if (size > ONE_GIGABYTE)
			strSize = String.format("%.3f GB",
					(double) size / (double) ONE_GIGABYTE);
	    else if (size > ONE_MAGABYTE)
			strSize = String.format("%.3f MB",
					(double) size / (double) ONE_MAGABYTE);
		else if (size > ONE_KILOBYTE)
			strSize = String.format("%.3f kB",
					(double) size / (double) ONE_KILOBYTE);
		else
			strSize = String.format("%d Byte", size);
		return strSize;
    }
	
	private class FileInfoThread implements Runnable {
		private int mIndex;
		
		FileInfoThread(int index) {
			mIndex = index;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			showInfo(mIndex);
		}
		
	};
	
	
	
	private class CheckCloudTaskWorker extends SwingWorker<Boolean, Integer> {
		private int mTaskId;
		
		private final static int TASK_STATUS_OK 			= 0;
		private final static int TASK_STATUS_DOWNLOADING	= 1;
		private final static int TASK_STATUS_ERROR			= 100;
		
		public CheckCloudTaskWorker(int task_id) {
			mTaskId	= task_id;
		}
		
		private int getCloudStatus(int task_id) {
			String strTaskId = String.valueOf(task_id);
			
			String url = BAIDU_PCS_CLOUD_QUERY_TASK;
			url += strTaskId;
			HttpGet request = new HttpGet(url);
			request.setHeader("Cookie", "BDUSS=" + cookie_BDUSS);
			
			HttpResponse response;
			try {
				response = HttpClients.createDefault().execute(request);
				
				String result = EntityUtils.toString(response.getEntity());
				
				
				if (response.getStatusLine().getStatusCode() != 200){
					System.out.println("Java: code not 200: " + 
							response.getStatusLine().getStatusCode());
					System.out.println("result: " + result);
					return TASK_STATUS_ERROR;
				}
				
				JSONTokener jsonParser = new JSONTokener(result);
				JSONObject root = (JSONObject) jsonParser.nextValue();
				int error_code = root.getInt("error_code");
				long request_id = root.getLong("request_id");
				if (error_code != 0) {
					System.out.println(String.format("Java: failed to query cloud job: %d, request_id %d",
							error_code, request_id));
					return TASK_STATUS_ERROR;
				}
				
				JSONObject task_list = root.getJSONObject("task_info");
				JSONObject info = task_list.getJSONObject(strTaskId);
				
				// 0查询成功，结果有效，1要查询的task_id不存在
				int info_result = info.getInt("result");
				if (info_result != 0)
					return TASK_STATUS_ERROR;
				
				// "status":1 (0下载成功，1下载进行中 2系统错误，
				// 3资源不存在，4下载超时，5资源存在但下载失败 6存储空间不足 7目标地址数据已存在 8任务取消)
				int info_status = info.getInt("status");
				
				System.out.println(String.format("result: %d, status %d", 
						info_result, info_status));
				
				if (info_result == 0)
					return TASK_STATUS_OK;
				else if (info_result == 1)
					return TASK_STATUS_DOWNLOADING;
				else
					return TASK_STATUS_ERROR;
			}
			catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return TASK_STATUS_ERROR;
		}
		
		@Override
		protected Boolean doInBackground() throws Exception {
			// TODO Auto-generated method stub
			int ret;
			while (true) {
				ret = getCloudStatus(mTaskId);
				if (TASK_STATUS_ERROR == ret || TASK_STATUS_OK == ret)
					break;
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (TASK_STATUS_OK == ret) {
				System.out.println("file cloud download done!");
				return true;
			}
			
			return false;
		}
	
		@Override
		protected void done() {
			// TODO Auto-generated method stub
			init_combobox();
		}
	};
	
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
			e.printStackTrace();
			System.out.println("failed to run exec");
			lblInfo.setText("failed to run exec");
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
					if (type.equals("Error")) {
						System.out.println("[error] " + line);
						lblInfo.setText("[error] " + line);
					}
					else {
						System.out.println("[info] " + line);
						lblInfo.setText("[info] " + line);
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	private class UploadWorker extends SwingWorker<Boolean, Long> {

		private String mSrcPath;
		private String mDstPath;
		private String mFileName;
		
		public UploadWorker(String srcPath, String dstPath) {
			mSrcPath	= srcPath;
			mDstPath	= dstPath;
			mFileName	= srcPath;
			
			int pos = srcPath.lastIndexOf(SeparatorUtils.getFileSeparator());
			if (pos > -1)
				mFileName = srcPath.substring(pos + 1);
		}
		
		@Override
		protected Boolean doInBackground() throws Exception {
			FileInputStream fin = null;
			
			try {
				String encoded_path = URLEncoder.encode(mDstPath, "utf-8");

				String url_path = BAIDU_PCS_UPLOAD + "&path=" + encoded_path;
				System.out.println("Java: upload_file() " + url_path);
				
				File file = new File(mSrcPath);
				long len = file.length();
				System.out.println("Java file size " + len);
				
				if (len <= 0) {
					System.out.println("Java cannot get file size ");
					return false;
				}
				
				if (mUploadBlockSize == 0) {
					mUploadBlockSize = (int)(len / 1048576 / 200 * 1048576);
					if (0 == mUploadBlockSize)
						mUploadBlockSize = 1048576;
					System.out.println("Java set mUploadBlockSize to " + mUploadBlockSize);
					lblInfo.setText("上传块大小设置为 " + mUploadBlockSize / 1048576 + " MB");
				}
				
				fin = new FileInputStream(file);

				int offset;
				int left;
				long total_left = len;
				long total_sent = 0;
				int byteread, toread;

				long start_msec = System.currentTimeMillis();
				List<String> md5s = new ArrayList<String>();
				
				while (total_left > 0) {
					offset = 0;
					left = mUploadBlockSize;
					if (left > total_left)
						left = (int)total_left;
					byte[] context = new byte[left];
					
					while (true) {
						toread = UPLOAD_READSIZE;
						if (left < toread)
							toread = left;
						byteread = fin.read(context, offset, toread);
						if (byteread == -1) {
							System.out.println("Java eof " + offset);
							break;
						}
						
						offset += byteread;
						left -= byteread;

						if (left <= 0)
							break;
					}
					
					System.out.println("Java: read context " + offset);
					
					int retry = 0;
					while (true) {
						String md5 = uploadPiece(context, null);
						
						if (md5 != null) {
							md5s.add(md5);
							
							total_left -= offset;
							total_sent += offset;
							
							long elapsed_msec = System.currentTimeMillis() - start_msec;
							long speed = total_sent / elapsed_msec;
							publish(total_sent, total_left, speed);
							System.out.println(String.format("Java: add md5 to list %s, total_left %d",
									md5, total_left));
							
							break;
						}
						else {
							if (retry < 10)
								System.out.println("Java: failed to uploadPiece(), retry #" + retry);
							else {
								System.out.println("Java: failed to uploadPiece() after try 10 times.");
								lblInfo.setText("上传块文件失败，重试中");
								return false;
							}
						}

						retry++;
					}
				}
				
				if (!create_superfile(md5s, encoded_path)) {
					lblInfo.setText("创建大文件失败 " + mDstPath);
					return false;
				}
				
				System.out.println(String.format("Java: file %s uploaded to %s, size %s", 
						mSrcPath, mDstPath, getFileSize(len)));
				
				long elapsed_msec = System.currentTimeMillis() - start_msec;
				double total_speed = len / (double)elapsed_msec;
				lblInfo.setText(String.format("%s 上传至\n %s, 大小 %s, 速度 %.3f kB/s", 
						/*src_path*/file.getName(), mDstPath, getFileSize(len), total_speed));

				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				try {
					fin.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return false;
		}
		
		@Override
		protected void done() {
			// TODO Auto-generated method stub
			super.done();
			init_combobox();
		}
		
		@Override
		protected void process(List<Long> list) {
			// TODO Auto-generated method stub
			super.process(list);
			
			long total_sent = list.get(0);
			long total_left = list.get(1);
			long speed 		= list.get(2);
			
			lblInfo.setText(String.format("%s 已传  %s, 剩余  %s, 速度  %d kB/s", 
					mFileName, getFileSize(total_sent), getFileSize(total_left), speed));
		}
	};
	
	private class GetFolderSizeWorker extends SwingWorker<Boolean, Long> {

		private String mFolderPath;
		private long mSize;
		
		public GetFolderSizeWorker(String folderPath) {
			mFolderPath	= folderPath;
		}
		
		@Override
		protected Boolean doInBackground() throws Exception {
			lblInfo.setText("统计文件夹大小中");
			mSize = getFolderSize(mFolderPath);
			return true;
		}
		
		@Override
		protected void done() {
			// TODO Auto-generated method stub
			lblInfo.setText(mFolderPath + " 占用空间 " + getFileSize(mSize));
		}
		
		@Override
		protected void process(List<Long> list) {
			// TODO Auto-generated method stub
			super.process(list);
		}
	};
}