package com.weiliu.library.task.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.weiliu.library.json.JsonUtil;
import com.weiliu.library.task.http.HttpParams;
import com.weiliu.library.task.http.HttpRequestObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by qumiao on 2016/5/17.
 */
public class ResumeHttpTaskControl {

    private ResumeHttpTaskControl() {
        //no instance
    }

    public static synchronized void write(
            Context context, final List<HttpRequestObject> requestList, final boolean append, boolean async) {
        final ResumeHttpTaskDBHelper helper = ResumeHttpTaskDBHelper.getInstance(context);
        helper.executeTranaction(new SQLiteTransactionCallback() {
            @Override
            public boolean beforeTransaction(SQLiteDatabase db) {
                return false;
            }

            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                if (!append) {
                    db.execSQL("delete from " + ResumeHttpTaskColumn.TABLE);
                }

                SQLiteStatement statement = ResumeHttpTaskDBHelper.createInsertStatement(
                        db, ResumeHttpTaskColumn.TABLE, ResumeHttpTaskColumn.values(), null);
                for (HttpRequestObject request : requestList) {
                    statement.bindString(ResumeHttpTaskColumn.url.ordinal() + 1, request.url);
                    statement.bindString(ResumeHttpTaskColumn.params.ordinal() + 1,
                            JsonUtil.objectToJsonString(request.params, HttpParams.class));
                    statement.bindLong(ResumeHttpTaskColumn.request_time.ordinal() + 1, request.requestTime);
                    statement.bindLong(ResumeHttpTaskColumn.whole_response.ordinal() + 1,
                            request.wholeResponse ? 1 : 0);
                    statement.executeInsert();
                }
                return true;
            }
        }, async);
    }

    public static synchronized List<HttpRequestObject> read(Context context) {
        ArrayList<HttpRequestObject> list = new ArrayList<>();
        SQLiteDatabase database = null;
        try {
            database = ResumeHttpTaskDBHelper.getInstance(context).getReadableDatabase();
        } catch (Exception e) {
//            android.database.sqlite.SQLiteException: Can't upgrade read-only database from version 0 to 2: root.db
//            at android.database.sqlite.SQLiteOpenHelper.getDatabaseLocked(SQLiteOpenHelper.java:245)
//            at android.database.sqlite.SQLiteOpenHelper.getReadableDatabase(SQLiteOpenHelper.java:188)
//            at ResumeHttpTaskControl.read(ResumeHttpTaskControl.java:56)
            e.printStackTrace();
        }
        if (database == null) {
            return list;
        }
        Cursor cursor = database.query(true, ResumeHttpTaskColumn.TABLE, null, null, null, null, null,
                ResumeHttpTaskColumn.request_time.name(), null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    HttpRequestObject request = new HttpRequestObject();
                    request.url = cursor.getString(cursor.getColumnIndex(ResumeHttpTaskColumn.url.name()));
                    String paramsStr = cursor.getString(cursor.getColumnIndex(ResumeHttpTaskColumn.params.name()));
                    request.params = JsonUtil.jsonStringToObject(paramsStr, HttpParams.class);
                    request.requestTime = cursor.getLong(cursor.getColumnIndex(
                            ResumeHttpTaskColumn.request_time.name()));
                    request.wholeResponse = cursor.getInt(cursor.getColumnIndex(
                            ResumeHttpTaskColumn.whole_response.name())) != 0;
                    list.add(request);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    public static synchronized void delete(
            Context context, final List<HttpRequestObject> requestList, boolean async) {
        final ResumeHttpTaskDBHelper helper = ResumeHttpTaskDBHelper.getInstance(context);
        helper.executeTranaction(new SQLiteTransactionCallback() {
            @Override
            public boolean beforeTransaction(SQLiteDatabase db) {
                return false;
            }

            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                SQLiteStatement statement = ResumeHttpTaskDBHelper.createDeleteStatement(
                        db, ResumeHttpTaskColumn.TABLE, ResumeHttpTaskColumn.values(), null);
                for (HttpRequestObject request : requestList) {
                    statement.bindString(ResumeHttpTaskColumn.url.ordinal() + 1, request.url);
                    statement.bindString(ResumeHttpTaskColumn.params.ordinal() + 1,
                            JsonUtil.objectToJsonString(request.params, HttpParams.class));
                    statement.bindLong(ResumeHttpTaskColumn.request_time.ordinal() + 1, request.requestTime);
                    statement.bindLong(ResumeHttpTaskColumn.whole_response.ordinal() + 1,
                            request.wholeResponse ? 1 : 0);
                    statement.executeUpdateDelete();
                }
                return true;
            }
        }, async);
    }
}