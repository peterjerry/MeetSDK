package com.pplive.epg.baidu;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.boxcontroller.Code;
import com.pplive.epg.util.Util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*; 
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import java.util.StringTokenizer;


@SuppressWarnings("serial")
public class BaiduPanel extends JPanel {
	
	private String mbOauth 		= "23.48f0a3ad0fe8b41428000bb0509f1fb2.2592000.1441874259.184740130-266719";
	private String mbRootPath 	=  "/我的视频";
	private String list_by 		= "time"; // "name" "size"
	private String list_order 	= "desc"; // "asc"
	
	private final static int ONE_KILOBYTE = 1024;
	private final static int ONE_MAGABYTE = (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE = (ONE_MAGABYTE * ONE_KILOBYTE);
	
	private static String exe_vlc = "D:/Program Files/vlc-3.0.0/vlc.exe";
	private static String exe_ffplay = "D:/Program Files/ffmpeg/ffplay.exe";
	
	private final String BAIDU_PCS_PREFIX = "https://pcs.baidu.com/rest/2.0/pcs";
	private final String BAIDU_PCS_FILE_PREFIX = BAIDU_PCS_PREFIX + "/file";
	private final String BAIDU_PCS_THUMBNAIL_PREFIX = BAIDU_PCS_PREFIX + "/thumbnail";
	private final String BAIDU_PCS_SERVICE_PREFIX = BAIDU_PCS_PREFIX + "/services/cloud_dl";
	
	private final static String ApiKey = "4YchBAkgxfWug3KRYCGOv8EK"; // from es explorer
	
	private final String BAIDU_PCS_AUTHORIZE = "https://openapi.baidu.com/oauth/2.0" + 
		"/authorize?response_type=token" + 
		"&client_id=" + ApiKey + 
		"&redirect_uri=oob&scope=netdisk";
	
	private final String BAIDU_PCS_LIST;
	private final String BAIDU_PCS_META;
	private final String BAIDU_PCS_DOWNLOAD;
	private final String BAIDU_PCS_STREAMING;
	private final String BAIDU_PCS_THUMBNAIL;
	private final String BAIDU_PCS_CLOUD_DL;

	private final static String[] list_by_desc = {"按时间", "按名称", "按大小"}; //time" "name" "size"
	
	private List<Map<String, Object>> mFileList;

	JButton btnUp	 	= new JButton("...");
	JButton btnReset 	= new JButton("重置");
	JButton btnAuth 	= new JButton("认证");
	
	JLabel lblRootPath = new JLabel("info");
	JComboBox<String> comboListBy 		= null;
	
	JList<String> listItem 				= null;
	DefaultListModel<String> listModel 	= null;
	JScrollPane scrollPane				= null;
	
	JLabel lblImage = new JLabel();
	
	JTextPane editPath = new JTextPane();
	JButton btnCreateFolder = new JButton("添加");
	JButton btnDownload = new JButton("下载");
	JButton btnYunDownload = new JButton("云存");
	
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
				"&access_token=" + mbOauth + 
				"&path=";
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
		BAIDU_PCS_CLOUD_DL = BAIDU_PCS_SERVICE_PREFIX + 
				"?method=add_task" +
				"&access_token=" + mbOauth;
		
		// Action
		lblRootPath.setFont(f);
		lblRootPath.setBounds(20, 40, 300, 30);
		this.add(lblRootPath);
		
		listItem = new JList<String>();
		listItem.setFont(f);
		listModel = new DefaultListModel<String>();
		listItem.setModel(listModel);
		listItem.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listItem.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent event) {
				// TODO Auto-generated method stub
				int index = listItem.getSelectedIndex();
				if (index == -1)
					return;
				
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
								try {
									String encoded_path = URLEncoder.encode(path, "utf-8");

									String url = BAIDU_PCS_DOWNLOAD + encoded_path;
									System.out.println("ready to play url: " + url);
									lblInfo.setText("ready to play url: " + url);
									
									String exe_path = exe_vlc;
									if (!cbUseVLC.isSelected())
										exe_path = exe_ffplay;
									String[] cmd = new String[] {exe_path, url};
									openExe(cmd);
								} catch (UnsupportedEncodingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
				else if (event.getClickCount() == 1) {
					showInfo(index);
				}
			}
		});
		listItem.setCellRenderer(new MyRender());

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
			}
		});
		
		lblImage.setBounds(430, 200, 192, 192);
		this.add(lblImage);
		
		comboListBy = new JComboBox<String>(list_by_desc);
		comboListBy.setFont(f);
		comboListBy.setBounds(430, 150, 80, 40);
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
		editPath.setText("新建文件夹");
	    this.add(editPath);
	    
	    btnCreateFolder.setFont(f);
	    btnCreateFolder.setBounds(230, 450, 80, 40);
	    btnCreateFolder.setFont(f);
		this.add(btnCreateFolder);
		btnCreateFolder.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String src_url = editPath.getText();
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
					return;
				}
				
				int index = listItem.getSelectedIndex();
				Map<String, Object> fileinfo = mFileList.get(index);
				boolean isdir = (Boolean) fileinfo.get("isdir");
				if (isdir) {
					System.out.println("cannot download folder");
					lblInfo.setText("cannot download folder");
				}
				else {
					String path = (String) fileinfo.get("path");
					
					try {
						String encoded_path = URLEncoder.encode(path, "utf-8");

						String url = BAIDU_PCS_DOWNLOAD + encoded_path;
						System.out.println("ready to download url: " + url);
						lblInfo.setText("ready to download url: " + url);
						
						int pos = path.lastIndexOf("/");
						String filename = path.substring(pos + 1);
						downloadFile(url, filename);
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		
		btnYunDownload.setFont(f);
		btnYunDownload.setBounds(410, 450, 80, 40);
		btnYunDownload.setFont(f);
		this.add(btnYunDownload);
		btnYunDownload.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//"http://dlsw.baidu.com/sw-search-sp/soft/cc/13478/npp_V6.8.2_Installer.1440141668.exe"
				String download_url = editPath.getText();
				add_cloud_dl(download_url);
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
		
		init_combobox();
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
		Map<String, Object> fileinfo = mFileList.get(index);
		boolean isdir = (Boolean) fileinfo.get("isdir");
		if (!isdir) {
			String path = (String) fileinfo.get("path");
			List<Map<String, Object>> metaList = meta(path);
			if (metaList != null && metaList.size() > 0) {
				Map<String, Object> metainfo = metaList.get(0);
				long size = (Long) metainfo.get("filesize");
				int mtime = (Integer)metainfo.get("mtime");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = new Date(mtime/*unit is sec*/ * 1000L);
				
				lblInfo.setText("文件大小: " + getFileSize(size) + 
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
	
	private void downloadFile(String path, String filename) {
		try {
			URL url = new URL(path);
			URLConnection urlCon = url.openConnection();

			// 显示下载信息
			System.out.println("文件下载信息: ");
			System.out.println("host : " + url.getHost());
			System.out.println("port :" + url.getPort());
			System.out.println("Contenttype : " + urlCon.getContentType());
			System.out.println("Contentlength : " + urlCon.getContentLength());

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
			
			final String save_path = fsave.getDirectory() + fsave.getFile();
			System.out.println("file save path: " + save_path);
			
			final FileOutputStream fos = new FileOutputStream(save_path);
			
			final InputStream is = urlCon.getInputStream();
		    Runnable r = new Runnable()
		    {
		    	public void run() {
		          try {
		        	  long downloaded = 0;
		        	  int readed;
		        	  byte []buf= new byte[4096];
		        	  long start_msec = System.currentTimeMillis();
		        	  long last_msec = 0;
		        	  
		        	  bDownloading = true;
		              while((readed = is.read(buf))!=-1 && !bInterrupt) {
		            	  fos.write(buf, 0, readed);
		            	  downloaded += readed;
		            	  
		            	  long cur_msec = System.currentTimeMillis();
		            	  if (cur_msec - last_msec > 300) {
		            		  long elapsed_msec = cur_msec - start_msec;
		            		  double speed = downloaded / (double)elapsed_msec;
			            	  lblInfo.setText(String.format("文件下载进度 %s, 速度 %.3f kB/s", 
			            			  getFileSize(downloaded), speed));  
			            	  last_msec = cur_msec;
		            	  }
		              }
		              
		              fos.close();
		              is.close();
		              if (bInterrupt)
		            	  lblInfo.setText("文件 " + save_path + " 下载取消");
		              else
		            	  lblInfo.setText("文件 " + save_path + " 下载成功!");
		              
		              bInterrupt = false;
		              bDownloading = false;
		              btnDownload.setText("下载");
		          }
		          catch (Exception ex) {
		        	  ex.printStackTrace();
		              lblInfo.setText("下载失败: " + ex.getMessage());
		          }
		      }
		  };
		  
		  Thread t = new Thread(r);
		  t.start();
		  
		  btnDownload.setText("取消");
		  
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init_combobox() {
		lblRootPath.setText(mbRootPath);
		
		mFileList = list(mbRootPath);
		if (mFileList == null)
			return;
		
		listModel.clear();
		
		int size = mFileList.size();
		for (int i=0;i<size;i++) {
			Map<String, Object> fileinfo = mFileList.get(i);
			String filename = (String) fileinfo.get("filename");
			listModel.addElement(filename);
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
				System.out.println("Java: code not 200: " + 
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
		
		// ed2k://|file|[www.ed2kers.com]TLF-VIDEO-04.12.14.The.Maze.Runner.(2014).iNT
		// .BDRip.720p.AC3.X264-TLF.mkv|2094704851|E30B41BAFE4D3056AF027232A7FEF3A6
		// |h=JD2TWIEK2PLEMO3ENWF3FCS5RF6W5MBF|/
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

		// 把请求的数据，添加到NameValuePair中
		/*NameValuePair nameValuePair1 = new BasicNameValuePair("method",
				"add_task");
		NameValuePair nameValuePair2 = new BasicNameValuePair("source_url",
				"http://dlsw.baidu.com/sw-search-sp/soft/da/17519/BaiduYunGuanjia_5.2.7_setup.1430884921.exe");
		NameValuePair nameValuePair3 = new BasicNameValuePair("save_path", "/apps/pcstest_oauth/123.exe");
		NameValuePair nameValuePair4 = new BasicNameValuePair("type", "0");
		NameValuePair nameValuePair5 = new BasicNameValuePair("access_token", mbOauth);

		List<NameValuePair> list = new ArrayList<NameValuePair>();
		list.add(nameValuePair1);
		list.add(nameValuePair2);
		list.add(nameValuePair3);
		list.add(nameValuePair4);
		list.add(nameValuePair5);*/
		
		HttpResponse response;
		
		try {
			//HttpEntity requesthttpEntity = new UrlEncodedFormEntity(list);
			
			HttpPost httppost = new HttpPost(url);
			//httppost.setEntity(requesthttpEntity);
			
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
				int error_code = root.getInt("error_code");
				String error_msg = root.getString("error_msg");
				int request_id = root.getInt("request_id");
				System.out.println(String.format("Java: failed to add cloud job: %d(%s), request_id %d",
						error_code, error_msg, request_id));
				return false;
			}
			
			int task_id = root.getInt("task_id");
			int request_id = root.getInt("request_id");
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
	
	
}
