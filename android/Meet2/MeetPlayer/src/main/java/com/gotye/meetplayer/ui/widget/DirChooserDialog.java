package com.gotye.meetplayer.ui.widget;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.gotye.meetplayer.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DirChooserDialog extends Dialog implements android.view.View.OnClickListener{
	
	public interface onOKListener {
		abstract void saveFolder(String folder);
	}
	
	private ListView list;
	ArrayAdapter<String> Adapter;
	ArrayList<String> arr=new ArrayList<String>();
	
	Context context;
	private String path;
	
	private TextView title;
	private EditText et;
	private Button home,back,ok;
	private LinearLayout titleView;
	
	private int type = 1;
	private String[] fileType = null;
	
	public final static int TypeOpen = 1;
	public final static int TypeSave = 2;
	
	
	private onOKListener mOnListener;
	
	public void setOnOKListener(onOKListener listener) {
		mOnListener = listener;
	}
	
	public String getPath() {
		return path;
	}
	
	/**
	 * @param context
	 * @param type ֵΪ1��ʾ������Ŀ¼���͵ĶԻ���2Ϊ���������ļ���Ŀ¼���͵ĶԻ���
	 * @param fileType Ҫ���˵��ļ�����,null��ʾֻѡ��Ŀ¼
	 * @param resultPath ��OK��ť���صĽ��Ŀ¼����Ŀ¼+�ļ���
	 */
	public DirChooserDialog(Context context, int type, String[]fileType, String resultPath) {
		super(context);
		// TODO Auto-generated constructor stub
		this.context = context;
		this.type = type;
		this.fileType = fileType;
		this.path = resultPath;
	}
	/* (non-Javadoc)
	 * @see android.app.Dialog#dismiss()
	 */
	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		super.dismiss();
	}
	/* (non-Javadoc)
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chooserdialog);
		
		arr = (ArrayList<String>) getDirs(path);
		Adapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, arr);
		
		list = (ListView)findViewById(R.id.list_dir);
		list.setAdapter(Adapter);
		
		list.setOnItemClickListener(lvLis);

		home = (Button) findViewById(R.id.btn_home);
		home.setOnClickListener(this);
		
		back = (Button) findViewById(R.id.btn_back);
		back.setOnClickListener(this);
		
		ok = (Button) findViewById(R.id.btn_ok);
		ok.setOnClickListener(this);
		
		titleView = (LinearLayout) findViewById(R.id.dir_layout);
		
		if (type == TypeOpen){
			title = new TextView(context);
			titleView.addView(title);
			title.setText(path);
		}else if(type == TypeSave){
			et = new EditText(context);
			et.setWidth(240);
			et.setHeight(70);
			et.setGravity(Gravity.CENTER);
			et.setPadding(0, 2, 0, 0);
			titleView.addView(et);
			et.setText("wfFileName");
		}
//		title = (TextView) findViewById(R.id.dir_str);
//		title.setText(path);
		
	}
	//��̬����ListView
	Runnable add=new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			arr.clear();
//System.out.println("Runnable path:"+path);

			//����������ַ���Ϊarr��ֵ���ܸ���
			List<String> temp = getDirs(path);
			for(int i = 0;i < temp.size();i++)
				arr.add(temp.get(i));
			Adapter.notifyDataSetChanged();
		}   	
    };
   
    private OnItemClickListener lvLis = new OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position,
				long id) {
			String temp = (String) arg0.getItemAtPosition(position);			
			if (temp.equals(".."))
				path = getSubDir(path);
			else
				path = path + "/" + temp;
	
			if (type == TypeOpen)
				title.setText(path);
			
			Handler handler=new Handler();
	    	handler.post(add);
		}
    };
	
	private List<String> getDirs(String ipath) {
		List<String> file = new ArrayList<String>();
		file.add("..");
		
		File[] myFile = new File(ipath).listFiles();
		if (myFile != null) {
			Arrays.sort(myFile, new FileComparator());
			
			for (File f : myFile) {
				if (f.isHidden() || !f.canRead())
					continue;
				
				if (f.isDirectory()) {
					String tempf = f.toString();
					int pos = tempf.lastIndexOf("/");
					String subTemp = tempf.substring(pos + 1, tempf.length());
					file.add(subTemp);
				}

				// 过滤知道类型的文件
				if (f.isFile() && fileType != null) {
					for (int i = 0; i < fileType.length; i++) {
						int typeStrLen = fileType[i].length();

						String fileName = f.getPath().substring(
								f.getPath().length() - typeStrLen);
						if (fileName.toLowerCase().equals(fileType[i])) {
							file.add(f.toString().substring(path.length() + 1,
									f.toString().length()));
						}
					}
				}
			}
		}

		if (file.size() == 0)
			file.add("..");

		// System.out.println("file[0]:"+file.get(0)+" File size:"+file.size());
		return file;
	}
	
	class FileComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			if (f1.isFile() && f2.isDirectory())
				return 1;
			if (f2.isFile() && f1.isDirectory())
				return -1;
				
			String s1=f1.getName().toString().toLowerCase();
			String s2=f2.getName().toString().toLowerCase();
			return s1.compareTo(s2);
	    }
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId() == home.getId()){
			path = getRootDir();
			if(type == TypeOpen)
				title.setText(path);			
			Handler handler=new Handler();
	    	handler.post(add);
		}else if(v.getId() == back.getId()){
			path = getSubDir(path);
			if(type == TypeOpen)
				title.setText(path);			
			Handler handler=new Handler();
	    	handler.post(add);
		}else if(v.getId() == ok.getId()){
			dismiss();
			
			if (mOnListener != null) {
				mOnListener.saveFolder(path);
			}
		}
	}

	private String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // �ж�sd���Ƿ����
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// ��ȡ��Ŀ¼
		}
		if (sdDir == null) {
			// Toast.makeText(context,
			// "No SDCard inside!",Toast.LENGTH_SHORT).show();
			return null;
		}
		return sdDir.toString();

	}
	
	private String getRootDir(){
		String root = "/";
		
		path = getSDPath();
		if (path == null)
			path="/";
		
		return root;
	}
	
	private String getSubDir(String path){
		if (path.equals("/"))
			return path;
		
		int pos = path.lastIndexOf("/");
		if (pos == -1)
			return path;
		else if (pos == 0)
			return "/";
		else
			return path.substring(0, pos);
	}
}