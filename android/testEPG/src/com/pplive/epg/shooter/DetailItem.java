package com.pplive.epg.shooter;

import java.util.List;
import java.util.Map;

public class DetailItem {

	public int mId;
	public String mFileName;
	public String mNativeName;
	public String mUrl;
	public List<String> mArvList;
	
	public DetailItem(int id, String fileName, String nativeName, String url, List<String> arv_list) {
		mId			= id;
		mFileName	= fileName;
		mNativeName	= nativeName;
		mUrl		= url;
		mArvList	= arv_list;
	}
}
