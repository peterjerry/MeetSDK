package com.pplive.db;

import android.content.Context;  
import android.database.sqlite.SQLiteDatabase;  
import android.database.sqlite.SQLiteOpenHelper;

public class DBhelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "media.db";  
    private static final int DATABASE_VERSION = 1;
    
    public DBhelper(Context context) {   
        super(context, DATABASE_NAME, null, DATABASE_VERSION);  
    } 

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("CREATE TABLE IF NOT EXISTS video" +  
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "path VARCHAR, title VARCHAR, duration INTEGER, size INTEGER, " +
                "mime_type INTEGER, last_play_pos INTEGER, info TEXT)"); 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("ALTER TABLE video ADD COLUMN other STRING");
	}

}
