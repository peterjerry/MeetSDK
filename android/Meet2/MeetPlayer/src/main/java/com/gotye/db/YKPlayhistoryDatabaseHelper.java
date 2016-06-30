package com.gotye.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.gotye.common.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class YKPlayhistoryDatabaseHelper {
    private static final String TAG = "YKdbHelper";

    private static final String TABLE_NAME = "ykHistory";

    private static final String COLUMN_TITLE = "title";

    private static final String COLUMN_VIDEO_ID = "vid";

    private static final String COLUMN_EPISODE_INDEX = "episode_index"; // base 0

    private static final String COLUMN_SHOW_ID = "show_id";

    private static final String COLUMN_FT = "ft"; // always hd2 2

    private static final String COLUMN_LAST_PLAY_POSITION = "last_play_pos";

    private static final String COLUMN_WATCH_TIME = "watch_time";

    private static YKPlayhistoryDatabaseHelper instance = null;

    private DBOpenHelper dbOpenHelper;

    private Context context;

    public class ClipInfo {
    	public ClipInfo(String title, String vid, String show_id, int episode_index,
                        int ft, int pos) {
    		this.mTitle         = title;
    		this.mVideoId       = vid;
    		this.mShowId        = show_id;
            this.mEpisodeIndex  = episode_index;
    		this.mFt            = ft;
    		this.mLastPos       = pos;
    	}

    	public String mTitle;
    	public String mVideoId;
    	public String mShowId;
        public int mEpisodeIndex;
    	public int mFt;
    	public int mLastPos;
    }

    /**
     * <私有构造函数>
     *
     * @param context
     *            Context
     */
    private YKPlayhistoryDatabaseHelper(Context context) {
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
    public static synchronized YKPlayhistoryDatabaseHelper getInstance(Context context) {
         if (instance == null)
        	 instance = new YKPlayhistoryDatabaseHelper(context);
         
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
        LogUtil.info(TAG, "createDB()");
        createTable(db);
    }
    
    public synchronized void saveHistory(String title,
                                         String vid, @Nullable String show_id,
                                         int episode_index) {
        if (title == null || vid == null) {
            return;
        }
        
        Cursor c = null;
        try {
            SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
            
            c = db.query(TABLE_NAME, new String[] { COLUMN_TITLE, COLUMN_VIDEO_ID },
                    COLUMN_VIDEO_ID + "=?", new String[] { vid }, null, null, null);
            if (c.moveToFirst()) {
            	return;
            }
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_VIDEO_ID, vid);
            values.put(COLUMN_WATCH_TIME, System.currentTimeMillis());
            if (show_id != null)
            	values.put(COLUMN_SHOW_ID, show_id);
            if (episode_index != -1)
                values.put(COLUMN_EPISODE_INDEX, episode_index);
            values.put(COLUMN_FT, 2);

            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
        	LogUtil.error(TAG, e.toString());
        } finally {
        	if (c != null)
            	c.close();
        }
    }
    
    public synchronized void savePlayedPosition(String vid, int position) {
    	if (position < 0) {
            return;
        }
    	
    	try {
        	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_VIDEO_ID, vid);
            values.put(COLUMN_LAST_PLAY_POSITION, position);
            if (getLastPlayedPosition(vid) >= 0) {
            	String[] args = {vid};
                db.update(TABLE_NAME, values, COLUMN_VIDEO_ID + "=?", args);
            }
            else {
                LogUtil.error(TAG, "Java: no playlink found in db " + vid);
            }
        } catch (Exception e) {
            LogUtil.error(TAG, e.toString());
        }
    }
    
    public synchronized int getLastPlayedPosition(String vid) {
    	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor c = null;
        try {
        	c = db.query(TABLE_NAME, new String[] { COLUMN_LAST_PLAY_POSITION },
                    COLUMN_VIDEO_ID + "=?", new String[] { vid }, null, null, null);
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
    
    public synchronized void deletePlayedClip(String vid) {
		if (vid == null || vid.isEmpty())
			return;

		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		
		try {
			String[] args = { vid };
			db.delete(TABLE_NAME, COLUMN_VIDEO_ID + "=?", args);
		} catch (Exception e) {
			e.printStackTrace();
            LogUtil.error(TAG, e.toString());
		}
	}
    
    public synchronized List<ClipInfo> getPlayedClips() {
    	 SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
         Cursor c = null;
         
         try {
             c = db.query(TABLE_NAME, 
            		 new String[] { COLUMN_TITLE, COLUMN_VIDEO_ID,
                             COLUMN_SHOW_ID, COLUMN_EPISODE_INDEX,
                             COLUMN_FT, COLUMN_LAST_PLAY_POSITION},
                     null, null, null, null, COLUMN_WATCH_TIME + " DESC");
             if (c.moveToFirst()) {
            	 List<ClipInfo> listClips = new ArrayList<ClipInfo>();

	             int title_index = c.getColumnIndex(COLUMN_TITLE);
	             int vid_index = c.getColumnIndex(COLUMN_VIDEO_ID);
	             int show_id_index = c.getColumnIndex(COLUMN_SHOW_ID);
                 int epindex_index = c.getColumnIndex(COLUMN_EPISODE_INDEX);
	             int ft_index = c.getColumnIndex(COLUMN_FT);
	             int last_pos_index = c.getColumnIndex(COLUMN_LAST_PLAY_POSITION);
	             
	             while (true) {
	            	 String title = c.getString(title_index);
	            	 String vid = c.getString(vid_index);
	            	 String show_id = c.getString(show_id_index);
                     int episode_index = c.getInt(epindex_index);
	            	 int ft = c.getInt(ft_index);
	            	 int pos = c.getInt(last_pos_index);
	            	 
	            	 ClipInfo info = new ClipInfo(title, vid, show_id, episode_index,
                             ft, pos);
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
					new String[] { COLUMN_TITLE, COLUMN_VIDEO_ID, COLUMN_SHOW_ID,
                            COLUMN_EPISODE_INDEX, COLUMN_FT,
                            COLUMN_LAST_PLAY_POSITION, COLUMN_WATCH_TIME},
					null, null, null, null,
					COLUMN_WATCH_TIME + " DESC", "10");
			List<ClipInfo> listClips = new ArrayList<ClipInfo>();
			if (c.moveToFirst()) {
				int title_index = c.getColumnIndex(COLUMN_TITLE);
				int vid_index = c.getColumnIndex(COLUMN_VIDEO_ID);
				int show_id_index = c.getColumnIndex(COLUMN_SHOW_ID);
                int epindex_index = c.getColumnIndex(COLUMN_EPISODE_INDEX);
				int ft_index = c.getColumnIndex(COLUMN_FT);
				int last_pos_index = c.getColumnIndex(COLUMN_LAST_PLAY_POSITION);

                while (true) {
                    String title = c.getString(title_index);
                    String playlink = c.getString(vid_index);
                    String album_id = c.getString(show_id_index);
                    int episode_index = c.getInt(epindex_index);
                    int ft = c.getInt(ft_index);
                    int pos = c.getInt(last_pos_index);

                    ClipInfo info = new ClipInfo(title, playlink, album_id, episode_index,
                            ft, pos);
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
        LogUtil.info(TAG, String.format(Locale.US, "Java: onUpgrade() oldV %d, newV %d", oldV, newV));
    	//db.execSQL("alter table " + TABLE_NAME + " add " + COLUMN_WATCH_TIME + " INTEGER ");
    }
    
    private static void createTable(SQLiteDatabase db) {
        LogUtil.info(TAG, "createTable()");

        try {
            DBOpenHelper.execSQL(db, "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                    + " (_id integer primary key autoincrement, "
            		+ COLUMN_TITLE + " TEXT,"
            		+ COLUMN_VIDEO_ID + " TEXT,"
            		+ COLUMN_SHOW_ID + " TEXT,"
                    + COLUMN_EPISODE_INDEX + " INTEGER,"
            		+ COLUMN_FT + " INTEGER,"
            		+ COLUMN_LAST_PLAY_POSITION + " INTEGER,"
                    + COLUMN_WATCH_TIME + " INTEGER" + ");");
        } catch (Exception e) {
            LogUtil.error(TAG, e.toString());
        }
    }
    
    public static void dropTable(SQLiteDatabase db) {
        try {
            DBOpenHelper.execSQL(db, "DROP TABLE IF EXISTS " + TABLE_NAME);
        } catch (Exception e) {
            LogUtil.error(TAG, e.toString());
        }
    }
    
}
