package com.pplive.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

//import com.pptv.atv.tvplayer.player.util.LogUtils;

public class PPTVPlayhistoryDatabaseHelper {
    private static final String TAG = "PPTVStoreDatabaseHelper";
	
    private static final String TABLE_NAME = "pptvhistory";
    
    private static final String COLUMN_TITLE = "title";
    
    private static final String COLUMN_PLAYLINK = "playlink";
    
    private static final String COLUMN_ALBUM_ID = "album_id";
    
    private static final String COLUMN_FT = "ft";
    
    private static final String COLUMN_LAST_PLAY_POSITION = "last_play_pos";
    
    private static final String COLUMN_WATCH_TIME = "watch_time";
    
    private static PPTVPlayhistoryDatabaseHelper instance = null;
    
    private DBOpenHelper dbOpenHelper;
    
    private Context context;
    
    public class ClipInfo {
    	public ClipInfo(String title, String playlink, String album_id, int ft, int pos) {
    		this.mTitle = title;
    		this.mPlaylink = playlink;
    		this.mAlbumId = album_id;
    		this.mFt = ft;
    		this.mLastPos = pos;
    	}
    	
    	public String mTitle;
    	public String mPlaylink;
    	public String mAlbumId;
    	public int mFt;
    	public int mLastPos;
    }
    
    /**
     * <私有构造函数>
     * 
     * @param context
     *            Context
     */
    private PPTVPlayhistoryDatabaseHelper(Context context) {
        dbOpenHelper = DBOpenHelper.getInstance(context);
        this.context = context;
    }
    
    /**
     * 单例模式
     * 
     * @param context
     *            Context
     * @return UUIDDatabaseHelper
     * @see [类、类#方法、类#成员]
     */
    public static synchronized PPTVPlayhistoryDatabaseHelper getInstance(Context context) {
         if (instance == null)
        	 instance = new PPTVPlayhistoryDatabaseHelper(context);
         
         return instance;
    }
    
    /**
     * 创建数据库
     * 
     * @param db
     *            SQLiteDatabase
     * @see [类、类#方法、类#成员]
     */
    public static void createDB(SQLiteDatabase db) {
        createTable(db);
    }
    
    public synchronized void saveHistory(String title, String playlink, String album_id, int ft) {
        if (title == null || playlink == null) {
            return;
        }
        
        Cursor c = null;
        try {
            SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
            
            c = db.query(TABLE_NAME, new String[] { COLUMN_TITLE, COLUMN_PLAYLINK },
            		COLUMN_PLAYLINK + "=?", new String[] { playlink }, null, null, null);
            if (c.moveToFirst()) {
            	return;
            }
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_PLAYLINK, playlink);
            values.put(COLUMN_WATCH_TIME, System.currentTimeMillis());
            if (album_id != null)
            	values.put(COLUMN_ALBUM_ID, album_id);
            if (ft >=0)
            	values.put(COLUMN_FT, ft);

            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        } finally {
        	if (c != null)
            	c.close();
        }
    }
    
    public synchronized void savePlayedPosition(String playlink, int position) {
    	if (position < 0) {
            return;
        }
    	
    	try {
        	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_PLAYLINK, playlink);
            values.put(COLUMN_LAST_PLAY_POSITION, position);
            if (getLastPlayedPosition(playlink) >= 0) {
            	String[] args = {playlink};
                db.update(TABLE_NAME, values, COLUMN_PLAYLINK + "=?", args);
            }
            else {
                Log.e(TAG, "Java: no playlink found in db " + playlink);
            }
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
    }
    
    public synchronized int getLastPlayedPosition(String playlink) {
    	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor c = null;
        try {
        	c = db.query(TABLE_NAME, new String[] { COLUMN_LAST_PLAY_POSITION },
                    COLUMN_PLAYLINK + "=?", new String[] { playlink }, null, null, null);
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        
        return -1;
    }
    
    public synchronized void deletePlayedClip(String playlink) {
		if (playlink == null || playlink.length() < 1)
			return;

		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		
		try {
			String[] args = { playlink };
			db.delete(TABLE_NAME, COLUMN_PLAYLINK + "=?", args);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
	}
    
    public synchronized List<ClipInfo> getPlayedClips() {
    	 SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
         Cursor c = null;
         
         try {
             c = db.query(TABLE_NAME, 
            		 new String[] { COLUMN_TITLE, COLUMN_PLAYLINK, COLUMN_ALBUM_ID, COLUMN_FT, COLUMN_LAST_PLAY_POSITION},
                     null, null, null, null, null);
             if (c.moveToFirst()) {
            	 List<ClipInfo> listClips = new ArrayList<ClipInfo>();

	             int title_index = c.getColumnIndex(COLUMN_TITLE);
	             int playlink_index = c.getColumnIndex(COLUMN_PLAYLINK);
	             int album_index = c.getColumnIndex(COLUMN_ALBUM_ID);
	             int ft_index = c.getColumnIndex(COLUMN_FT);
	             int last_pos_index = c.getColumnIndex(COLUMN_LAST_PLAY_POSITION);
	             
	             while (true) {
	            	 String title = c.getString(title_index);
	            	 String playlink = c.getString(playlink_index);
	            	 String album_id = c.getString(album_index);
	            	 int ft = c.getInt(ft_index);
	            	 int pos = c.getInt(last_pos_index);
	            	 
	            	 ClipInfo info = new ClipInfo(title, playlink, album_id, ft, pos);
	            	 listClips.add(info);
	            	 
	            	 if (!c.moveToNext())
	            		 break;
	             }  
	             return listClips;
             }
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             if (c != null) {
                 c.close();
             }
         }

    	return null;
    }
    
	public synchronized List<ClipInfo> getRecentPlay() {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		Cursor c = null;
		try {
			c = db.query(TABLE_NAME, 
					new String[] { COLUMN_TITLE, COLUMN_PLAYLINK, COLUMN_ALBUM_ID, 
						COLUMN_FT, COLUMN_LAST_PLAY_POSITION, COLUMN_WATCH_TIME}, 
					null, null, null, null,
					COLUMN_WATCH_TIME + " DESC", "10");
			List<ClipInfo> listClips = new ArrayList<ClipInfo>();
			if (c.moveToFirst()) {
				int title_index = c.getColumnIndex(COLUMN_TITLE);
				int playlink_index = c.getColumnIndex(COLUMN_PLAYLINK);
				int album_index = c.getColumnIndex(COLUMN_ALBUM_ID);
				int ft_index = c.getColumnIndex(COLUMN_FT);
				int last_pos_index = c.getColumnIndex(COLUMN_LAST_PLAY_POSITION);
				
				while (true) {
					 String title = c.getString(title_index);
	            	 String playlink = c.getString(playlink_index);
	            	 String album_id = c.getString(album_index);
	            	 int ft = c.getInt(ft_index);
	            	 int pos = c.getInt(last_pos_index);
	            	 
	            	 ClipInfo info = new ClipInfo(title, playlink, album_id, ft, pos);
	            	 listClips.add(info);
	            	 
	            	 if (!c.moveToNext())
	            		 break;
				}
				
				return listClips;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return null;
	}
    
    public synchronized void clearHistory() {  
    	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);  
    }  
    
    /**
     * 数据库升级
     * 
     * @param db
     *            SQLiteDatabase
     * @param oldV
     *            老版本号
     * @param newV
     *            新版本号
     * @see [类、类#方法、类#成员]
     */
    public static void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
    	Log.i(TAG, String.format("Java: onUpgrade() oldV %d, newV %d", oldV, newV));
    	//db.execSQL("alter table " + TABLE_NAME + " add " + COLUMN_WATCH_TIME + " INTEGER ");
    }
    
    private static void createTable(SQLiteDatabase db) {
        try {
            DBOpenHelper.execSQL(db, "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                    + " (_id integer primary key autoincrement, "
            		+ COLUMN_TITLE + " TEXT,"
            		+ COLUMN_PLAYLINK + " TEXT,"
            		+ COLUMN_ALBUM_ID + " TEXT,"
            		+ COLUMN_FT + " INTEGER,"
            		+ COLUMN_LAST_PLAY_POSITION + " INTEGER,"
                    + COLUMN_WATCH_TIME + " INTEGER" + ");");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
    
    public static void dropTable(SQLiteDatabase db) {
        try {
            DBOpenHelper.execSQL(db, "DROP TABLE IF EXISTS " + TABLE_NAME);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
    
}
