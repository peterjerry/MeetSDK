package com.pplive.db;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.pplive.meetplayer.service.MediaScannerService;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.TrackInfo;
import android.util.Log;

//import com.pptv.atv.tvplayer.player.util.LogUtils;

public class MediaStoreDatabaseHelper {
    private static final String TAG = "MediaStoreDatabaseHelper";
	
    private static final String TABLE_NAME = "mediastore";
    
    private static final String COLUMN_TITLE = "title";
    
    private static final String COLUMN_PLAY_URL = "play_url";
    
    private static final String COLUMN_MIME = "mime";
    
    private static final String COLUMN_WIDTH = "width";
    
    private static final String COLUMN_HEIGHT = "height";
    
    private static final String COLUMN_DURATION = "duration";
    
    private static final String COLUMN_FILESIZE = "filesize";
    
    private static final String COLUMN_FORMAT_NAME = "format";
    
    private static final String COLUMN_VCODEC_NAME = "video_codec";
    
    private static final String COLUMN_VCODEC_PROFILE = "video_codec_profile";
    
    private static final String COLUMN_AUDIO_CHANNELS = "audio_channels";
    
    private static final String COLUMN_AUDIO_STREAMS = "audio_streams"; // 1|2|3
    
    private static final String COLUMN_ACODEC_NAMES = "audio_codecs"; // aac|aac|mp3
    
    private static final String COLUMN_SUBTITLE_CHANNELS = "subtitle_channels";
    
    private static final String COLUMN_SUBTITLE_STREAMS = "subtitle_streams"; // 4|5|6
    
    private static final String COLUMN_SCODEC_NAMES = "subtitle_codecs"; // ass|ass|srt
    
    private static final String COLUMN_LAST_PLAY_POSITION = "last_play_pos";
    
    private static final String COLUMN_WATCH_TIME = "watch_time";
    
    private static MediaStoreDatabaseHelper instance = null;
    
    private DBOpenHelper dbOpenHelper;
    
    private Context context;
    
    /**
     * <私有构造函数>
     * 
     * @param context
     *            Context
     */
    private MediaStoreDatabaseHelper(Context context) {
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
    public static synchronized MediaStoreDatabaseHelper getInstance(Context context) {
         if (instance == null)
        	 instance = new MediaStoreDatabaseHelper(context);
         
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
    
    public synchronized MediaInfo getMediaInfo(String path) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL,
					COLUMN_TITLE, COLUMN_WIDTH, COLUMN_HEIGHT, COLUMN_FILESIZE,
					COLUMN_DURATION, COLUMN_FORMAT_NAME, COLUMN_VCODEC_NAME,
					COLUMN_VCODEC_PROFILE, COLUMN_AUDIO_CHANNELS,
					COLUMN_AUDIO_STREAMS, COLUMN_ACODEC_NAMES,
					COLUMN_SUBTITLE_CHANNELS, COLUMN_SUBTITLE_STREAMS,
					COLUMN_SCODEC_NAMES },
                    COLUMN_PLAY_URL + "=?", new String[] { path }, null, null, null);
            if (c.moveToFirst()) {
                return fillMediaInfo(c);
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
    
    public synchronized void saveMediaInfo(String path, String title, MediaInfo info) {
        if (path == null || info == null) {
            return;
        }
        
        Cursor c = null;
        try {
            SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
            
            c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL, COLUMN_TITLE },
                    COLUMN_PLAY_URL + "=?", new String[] { path }, null, null, null);
            if (c.moveToFirst()) {
            	return;
            }
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_PLAY_URL, path);
            if (title != null)
            	values.put(COLUMN_TITLE, title);
            values.put(COLUMN_MIME, MediaScannerService.getMimeType(title));
            values.put(COLUMN_WIDTH, info.getWidth());
            values.put(COLUMN_HEIGHT, info.getHeight());
            values.put(COLUMN_DURATION, info.getDuration());
            values.put(COLUMN_FILESIZE, info.getFile().length());
            values.put(COLUMN_FORMAT_NAME, info.getFormatName());
            values.put(COLUMN_VCODEC_NAME, info.getVideoCodecName());
            if (info.getVideoCodecProfile() != null)
            	values.put(COLUMN_VCODEC_PROFILE, info.getVideoCodecProfile());
            if (info.getAudioChannels() > 0) {
            	values.put(COLUMN_AUDIO_CHANNELS, info.getAudioChannels());
            	ArrayList<TrackInfo> list = info.getAudioChannelsInfo();
            	StringBuffer sbStrm = new StringBuffer();
            	StringBuffer sbCodec = new StringBuffer();
            	for (int i=0;i<list.size();i++) {
            		if (i > 0) {
            			sbStrm.append("|");
            			sbCodec.append("|");
            		}
            		sbStrm.append(list.get(i).getStreamIndex());
            		sbCodec.append(list.get(i).getCodecName());
            	}
            	
            	values.put(COLUMN_AUDIO_STREAMS, sbStrm.toString());
            	values.put(COLUMN_ACODEC_NAMES, sbCodec.toString());
            }
            else {
            	values.put(COLUMN_AUDIO_CHANNELS, 0);
            }
            if (info.getSubtitleChannels() > 0) {
            	values.put(COLUMN_SUBTITLE_CHANNELS, info.getSubtitleChannels());
            	ArrayList<TrackInfo> list = info.getSubtitleChannelsInfo();
            	StringBuffer sbStrm = new StringBuffer();
            	StringBuffer sbCodec = new StringBuffer();
            	for (int i=0;i<list.size();i++) {
            		if (i > 0) {
            			sbStrm.append("|");
            			sbCodec.append("|");
            		}
            		sbStrm.append(list.get(i).getStreamIndex());
            		sbCodec.append(list.get(i).getCodecName());
            	}
            	
            	values.put(COLUMN_SUBTITLE_STREAMS, sbStrm.toString());
            	values.put(COLUMN_SCODEC_NAMES, sbCodec.toString());
            }
            else {
            	values.put(COLUMN_SUBTITLE_CHANNELS, 0);
            }
            
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        } finally {
        	if (c != null)
            	c.close();
        }
    }
    
    public synchronized boolean hasMediaInfo(String path) {
    	if (path == null || path.isEmpty())
    		return false;
    	
    	try {
	        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
	    	Cursor c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL, COLUMN_TITLE },
	                COLUMN_PLAY_URL + "=?", new String[] { path }, null, null, null);
	        if (c != null && c.moveToFirst()) {
	        	return true;
	        }
    	} catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
        
		return false;
    }
    
    public synchronized void deleteMediaInfo(String path) {
    	if (path == null || path.isEmpty())
    		return;
    	
    	 try {
             SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
             String[] args = {path};
             db.delete(TABLE_NAME, COLUMN_PLAY_URL + "=?", args);
         } catch (Exception e) {
         	Log.e(TAG, e.toString());
         }
    }
    
    /**
     * @param path
     * @param position: unit msec
     */
    public synchronized void savePlayedPosition(String path, int position) {
    	if (path == null || path.isEmpty() || position < 0)
    		return;
    	
    	 try {
			SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

			Cursor c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL,
					COLUMN_TITLE }, COLUMN_PLAY_URL + "=?",
					new String[] { path }, null, null, null);
			if (c == null || !c.moveToFirst()) {
				Log.w(TAG, "Java: url NOT found");
				return;
			}

			ContentValues values = new ContentValues();
			values.put(COLUMN_PLAY_URL, path);
			values.put(COLUMN_LAST_PLAY_POSITION, position);
			values.put(COLUMN_WATCH_TIME, System.currentTimeMillis());
			String[] args = { path };
			db.update(TABLE_NAME, values, COLUMN_PLAY_URL + "=?", args);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
         }
    }

    /**
     * @param from_path
     * @param to_path
     * @return is updated ok
     */
    public synchronized boolean updatePath(String from_path, String to_path) {
    	if (from_path == null || to_path == null)
    		return false;
    	
    	 try {
			SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

			Cursor c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL,
					COLUMN_TITLE }, COLUMN_PLAY_URL + "=?",
					new String[] { from_path }, null, null, null);
			if (c == null || !c.moveToFirst()) {
				Log.w(TAG, "Java: url NOT found");
				return false;
			}

			ContentValues values = new ContentValues();
			values.put(COLUMN_PLAY_URL, to_path);
			String[] args = { from_path };
			db.update(TABLE_NAME, values, COLUMN_PLAY_URL + "=?", args);
			return true;
		} catch (Exception e) {
			Log.e(TAG, e.toString());
         }
    	 
    	 return false;
    }
    
    public synchronized int getLastPlayedPosition(String path) {
    	SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        Cursor c = null;
        try {
        	c = db.query(TABLE_NAME, new String[] { COLUMN_LAST_PLAY_POSITION },
                    COLUMN_PLAY_URL + "=?", new String[] { path }, null, null, null);
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
    
    public synchronized List<MediaInfo> getMediaStore() {
    	 SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
         Cursor c = null;
         
         try {
			c = db.query(TABLE_NAME, new String[] { COLUMN_PLAY_URL,
					COLUMN_TITLE, COLUMN_WIDTH, COLUMN_HEIGHT, COLUMN_FILESIZE,
					COLUMN_DURATION, COLUMN_FORMAT_NAME, COLUMN_VCODEC_NAME,
					COLUMN_VCODEC_PROFILE, COLUMN_AUDIO_CHANNELS,
					COLUMN_AUDIO_STREAMS, COLUMN_ACODEC_NAMES,
					COLUMN_SUBTITLE_CHANNELS, COLUMN_SUBTITLE_STREAMS,
					COLUMN_SCODEC_NAMES }, null, null, null, null, 
					"LOWER(" + COLUMN_TITLE + ") ASC", null);
			List<MediaInfo> listMediaInfo = new ArrayList<MediaInfo>();
			if (c.moveToFirst()) {
				while (true) {
					MediaInfo info = fillMediaInfo(c);
					if (info != null)
						listMediaInfo.add(info);
					
					if (!c.moveToNext())
						break;
				}
				
				return listMediaInfo;
			}
         } catch (Exception e) {
             
         } finally {
             if (c != null) {
                 c.close();
             }
         }

    	return null;
    }
    
    public synchronized void clearMediaStore() {  
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
            		+ COLUMN_PLAY_URL + " TEXT,"
            		+ COLUMN_TITLE + " TEXT,"
            		+ COLUMN_MIME + " TEXT,"
            		+ COLUMN_WIDTH + " INTEGER,"
            		+ COLUMN_HEIGHT + " INTEGER,"
            		+ COLUMN_DURATION + " INTEGER,"
            		+ COLUMN_FILESIZE + " INTEGER,"
            		+ COLUMN_FORMAT_NAME + " TEXT,"
            		+ COLUMN_VCODEC_NAME + " TEXT,"
            		+ COLUMN_VCODEC_PROFILE + " TEXT,"
            		+ COLUMN_AUDIO_CHANNELS + " INTEGER,"
            		+ COLUMN_AUDIO_STREAMS + " TEXT,"
            		+ COLUMN_ACODEC_NAMES + " TEXT,"
            		+ COLUMN_SUBTITLE_CHANNELS + " INTEGER,"
            		+ COLUMN_SUBTITLE_STREAMS + " TEXT,"
            		+ COLUMN_SCODEC_NAMES + " TEXT,"
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
    
    private static MediaInfo fillMediaInfo(Cursor c) {
    	int filepath_index = c.getColumnIndex(COLUMN_PLAY_URL);
		int duration_index = c.getColumnIndex(COLUMN_DURATION);
		int size_index = c.getColumnIndex(COLUMN_FILESIZE);
		int width_index = c.getColumnIndex(COLUMN_WIDTH);
		int height_index = c.getColumnIndex(COLUMN_HEIGHT);
		
		int format_name_index = c.getColumnIndex(COLUMN_FORMAT_NAME);
		int video_codec_index = c.getColumnIndex(COLUMN_VCODEC_NAME);
		
		int audio_channels_index = c.getColumnIndex(COLUMN_AUDIO_CHANNELS);
		int audio_streams_index = c.getColumnIndex(COLUMN_AUDIO_STREAMS);
		int audio_codecs_index = c.getColumnIndex(COLUMN_ACODEC_NAMES);
		int subtitle_channels_index = c.getColumnIndex(COLUMN_SUBTITLE_CHANNELS);
		int subtitle_streams_index = c.getColumnIndex(COLUMN_SUBTITLE_STREAMS);
		int subtitle_codecs_index = c.getColumnIndex(COLUMN_SCODEC_NAMES);
		
		String filepath = c.getString(filepath_index);
		int width = c.getInt(width_index);
		int height = c.getInt(height_index);
		int duration = c.getInt(duration_index);
		long filesize = c.getLong(size_index);
		String formatName = c.getString(format_name_index);
		String videoCodec = c.getString(video_codec_index);
		
		MediaInfo info = new MediaInfo(filepath, duration, filesize);
		info.setFormatName(formatName);
		info.setVideoInfo(width, height, videoCodec, duration);
		
		int audio_channels = c.getInt(audio_channels_index);
		info.setAudioChannels(audio_channels);
		if (audio_channels > 0) {
			String audio_streams = c.getString(audio_streams_index);
			String audio_codecs = c.getString(audio_codecs_index);
			
			StringTokenizer st1 = new StringTokenizer(audio_streams, "|", false);
			StringTokenizer st2 = new StringTokenizer(audio_codecs, "|", false);
			int id = 0;
			while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
				int streamIndex = Integer.valueOf(st1.nextToken());
				String codec_name = st2.nextToken();
				info.setAudioChannelsInfo(id++, streamIndex, codec_name, "", "", "");
			}
		}
		
		int subtitle_channels = c.getInt(subtitle_channels_index);
		info.setSubtitleChannels(subtitle_channels);
		if (subtitle_channels > 0) {
			String subtitle_streams = c.getString(subtitle_streams_index);
			String subtitle_codecs = c.getString(subtitle_codecs_index);
			
			StringTokenizer st1 = new StringTokenizer(subtitle_streams, "|", false);
			StringTokenizer st2 = new StringTokenizer(subtitle_codecs, "|", false);
			int id = 0;
			while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
				int streamIndex = Integer.valueOf(st1.nextToken());
				String codec_name = st2.nextToken();
				info.setSubtitleChannelsInfo(id++, streamIndex, codec_name, "", "");
			}
		}
		
		return info;
    }
}
