package com.weiliu.library.db;

import android.database.sqlite.SQLiteDatabase;

import com.weiliu.library.RootApplication;

/**
 *
 * Created by qumiao on 2016/5/18.
 */
public class ShortcutDBHelper extends DBHelper {
    private static final int VERSION = 2;

    private static final String NAME  = "shortcut.db";

    private static ShortcutDBHelper sInstance;

    public static ShortcutDBHelper getInstance() {
        if (sInstance == null) {
            synchronized (DBHelper.class) {
                if (sInstance == null) {
                    sInstance = new ShortcutDBHelper();
                }
            }
        }
        return sInstance;
    }


    private ShortcutDBHelper() {
        super(RootApplication.getInstance(), NAME, null, VERSION);
    }

    @Override
    protected void onUpdateDatabase(SQLiteDatabase db, int oldVersion) {
        createTable(db, ShortcutColumn.values(), oldVersion);
    }
}
