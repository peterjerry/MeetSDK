package com.pplive.epg.sohu;

public class CategorySohu {
	//"name":"全部",
    //"search_key":""
	
	//"cate_name":"排序",
    //"cate_alias":"o",
    //"default_keys":"-1"
	
	public String mTitle;
	public String mCateName;
	public String mCateAlias;
	public String mSearchKey;
	public String mDefaultKey;
	
	public CategorySohu(String title, String cateName, String alias, String searchKey, String defaultKey) {
		mTitle		= title;
		mCateName	= cateName;
		mCateAlias	= alias;
		mSearchKey	= searchKey;
		mDefaultKey	= defaultKey;
	}
}
