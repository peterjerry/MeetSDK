package com.gotye.common.sohu;

import java.util.List;

public class CategorySohu {

	public class CatItem {
		public String name;
		public String search_key;

		public CatItem(String name, String search_key) {
			this.name = name;
			this.search_key = search_key;
		}
	}

//    cates: [
//    {
//        name: "新上架",
//                search_key: "1"
//    },
//    {
//        name: "最热门",
//                search_key: "-1"
//    },
//    {
//        name: "好评榜",
//                search_key: "2"
//    }
//    ],
//    cate_name: "排序",
//    cate_alias: "o",
//    default_keys: "1"
	
	public String mCateName;
	public String mCateAlias;
	public String mDefaultKey;
    public List<CatItem> mItemList;
	
	public CategorySohu(String name, String alias, String defaultKey) {
		mCateName	= name;
		mCateAlias	= alias;

		mDefaultKey	= defaultKey;
	}

    public void setList(List<CatItem> list) {
        mItemList	= list;
    }
}
