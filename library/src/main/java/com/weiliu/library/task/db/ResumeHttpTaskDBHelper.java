package com.weiliu.library.task.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


/**
 *
 * Created by qumiao on 2016/5/18.
 */
public class ResumeHttpTaskDBHelper extends DBHelper {
    private static final int VERSION = 2;

    private static final String NAME  = "root.db";

    private static ResumeHttpTaskDBHelper sInstance;

    public static ResumeHttpTaskDBHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DBHelper.class) {
                if (sInstance == null) {
                    sInstance = new ResumeHttpTaskDBHelper(context);
                }
            }
        }
        return sInstance;
    }


    private ResumeHttpTaskDBHelper(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
    }

    @Override
    protected void onUpdateDatabase(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 1) {
            createTable(db, ResumeHttpTaskColumn.values(), oldVersion);
        } else if (oldVersion < 2) {    //版本2增加了whole_response列（是否对整个response body进行解析）
            String sql = String.format("ALTER TABLE %1$s ADD COLUMN %2$s %3$s",
                    ResumeHttpTaskColumn.TABLE,
                    ResumeHttpTaskColumn.whole_response,
                    ResumeHttpTaskColumn.whole_response.columnDesc.type);
            db.execSQL(sql);
        }
    }
}
