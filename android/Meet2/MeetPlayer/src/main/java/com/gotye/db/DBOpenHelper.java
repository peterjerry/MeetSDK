package com.gotye.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 数据库
 * ##########单例模式。不正确使用单例模式是引起内存泄露的一个常见问题，单例对象在被初始化后将在JVM的整个生命周期中存在（以静态变量的方式），
 * 如果单例对象持有外部对象的引用，那么这个外部对象将不能被jvm正常回收，导致内存泄露
 *
 * @author sugarzhang
 */
public class DBOpenHelper extends SQLiteOpenHelper {

	private final static String TAG = "DBOpenHelper";
    /**
     * The Constant DBNAME.
     */
    public static final String DB_NAME = "meet.db"; // 数据库名称

    /**
     * The Constant VERSION.
     */
    public static final int DB_VERSION = 100; // 626 -> add COLUMN_WATCH_TIME

    private static DBOpenHelper instance;
    private HashMap<String, HashSet<WeakReference<ContentObserver>>> observers = new HashMap<String, HashSet<WeakReference<ContentObserver>>>();

    /**
     * <私有构造函数>
     *
     * @param context Context
     */
    private DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.i(TAG, "Java: DBOpenHelper() constructor");
    }

    /**
     * 单例模式
     *
     * @param context Context
     * @return DBOpenHelper
     * @see [类、类#方法、类#成员]
     */
    public static synchronized DBOpenHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBOpenHelper(context.getApplicationContext());
        }
        return instance;
        // return new DBOpenHelper(context.getApplicationContext());
    }

    public static void execSQL(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void execSQL(SQLiteDatabase db, String sql, Object[] bindArgs) {
        try {
            db.execSQL(sql, bindArgs);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        MediaStoreDatabaseHelper.createDB(db);
        PPTVPlayhistoryDatabaseHelper.createDB(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 更新数据库，以后数据库更新时在添加，注意不要直接删除全部表，因为有些数据不希望更新就没了

        if (oldVersion < newVersion) {
            MediaStoreDatabaseHelper.onUpgrade(db, oldVersion, newVersion);
        } else {
            MediaStoreDatabaseHelper.dropTable(db);
            onCreate(db);
        }
    }

    /**
     * 查询
     */
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy,
            String having, String orderBy) {

        try {
            SQLiteDatabase db = getWritableDatabase();
            return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * 查询
     */
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String orderBy) {
        return query(table, columns, selection, selectionArgs, null, null, orderBy);
    }

    /**
     * 插入
     */
    public long insert(String table, ContentValues values) {

        long id = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            id = db.insert(table, null, values);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        if (id != -1) {
            dispatchChange(table);
        }

        return id;
    }

    /**
     * 更新
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {

        int count = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            count = db.update(table, values, whereClause, whereArgs);
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }

        if (count > 0) {
            dispatchChange(table);
        }

        return count;
    }

    /**
     * 删除
     */
    public int delete(String table, String whereClause, String[] whereArgs) {
        int count = -1;
        try {
            SQLiteDatabase db = getWritableDatabase();
            count = db.delete(table, whereClause, whereArgs);

        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }

        if (count > 0) {
            dispatchChange(table);
        }

        return count;
    }

    /**
     * 注册观察者
     */
    public void registerContentObserver(String table, ContentObserver observer) {
        try {
            HashSet<WeakReference<ContentObserver>> set = observers.get(table);
            if (set == null) {
                set = new HashSet<WeakReference<ContentObserver>>();
                observers.put(table, set);
            }

            set.add(new WeakReference<ContentObserver>(observer));
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
    }

    /**
     * 注销观察者
     */
    public void unregisterContentObserver(ContentObserver observer) {
        try {
            // 记录需要删除的对象
            HashSet<WeakReference<ContentObserver>> trashSet = new HashSet<WeakReference<ContentObserver>>();

            for (HashSet<WeakReference<ContentObserver>> set : observers.values()) {
                for (WeakReference<ContentObserver> reference : set) {
                    if (reference.get() != null) {
                        if (reference.get() == observer) {
                            trashSet.add(reference);
                        }
                    } else {
                        // 已经无引用的
                        trashSet.add(reference);
                    }
                }

                for (WeakReference<ContentObserver> reference : trashSet) {
                    set.remove(reference);
                }

                trashSet.clear();
            }
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
    }

    /**
     * 通知改变
     */
    private void dispatchChange(String table) {
        try {
            HashSet<WeakReference<ContentObserver>> set = observers.get(table);
            if (set != null) {
                // 记录需要删除的对象
                HashSet<WeakReference<ContentObserver>> trashSet = new HashSet<WeakReference<ContentObserver>>();

                for (WeakReference<ContentObserver> reference : set) {
                    if (reference.get() != null) {
                        reference.get().dispatchChange(true);
                    } else {
                        trashSet.add(reference);
                    }
                }

                for (WeakReference<ContentObserver> reference : trashSet) {
                    set.remove(reference);
                }

                trashSet.clear();
            }
        } catch (Exception e) {
        	Log.e(TAG, e.toString());
        }
    }
}
