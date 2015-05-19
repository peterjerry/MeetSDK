package com.pplive.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBmanager {
	private DBhelper helper;
	private SQLiteDatabase db;
	
	public DBmanager(Context context) {
		helper = new DBhelper(context);
		//因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0, mFactory);
		//所以要确保context已初始化,我们可以把实例化DBmanager的步骤放在Activity的onCreate里
		db = helper.getWritableDatabase();
	}
	
	/**
	 * add persons
	 * @param persons
	 */
	public void add(List<MediaInfoEntry> video_list) {
        db.beginTransaction();	//开始事务
        try {
        	for (MediaInfoEntry video : video_list) {
        		db.execSQL("INSERT INTO video VALUES(null, ?, ?, ?, ?, ?, ?, ?)", 
        				new Object[]{video._id, video.path, video.title,
        					video.duration, video.size, video.mime_type,
        					video.last_play_pos});
        	}
        	db.setTransactionSuccessful();	//设置事务成功完成
        } finally {
        	db.endTransaction();	//结束事务
        }
	}
	
	/**
	 * update last played position
	 * @param person
	 */
	public void updatePos(MediaInfoEntry video) {
		// todo
	}
	
	/**
	 * query all persons, return list
	 * @return List<Person>
	 */
	public List<MediaInfoEntry> query() {
		ArrayList<MediaInfoEntry> videos = new ArrayList<MediaInfoEntry>();
		Cursor c = queryTheCursor();
        while (c.moveToNext()) {
        	MediaInfoEntry video = new MediaInfoEntry();
        	
        	video._id = c.getInt(c.getColumnIndex("_id"));
        	video.path = c.getString(c.getColumnIndex("path"));
        	video.title = c.getString(c.getColumnIndex("title"));
        	video.size = c.getInt(c.getColumnIndex("size"));
        	videos.add(video);
        }
        c.close();
        return videos;
	}
	
	/**
	 * query all persons, return cursor
	 * @return	Cursor
	 */
	public Cursor queryTheCursor() {
        Cursor c = db.rawQuery("SELECT * FROM video", null);
        return c;
	}
	
	/**
	 * close database
	 */
	public void closeDB() {
		db.close();
	}
}

