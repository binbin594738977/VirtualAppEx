package com.weiliu.library.db;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

/**
 *
 * Created by qumiao on 2016/5/17.
 */
public class ShortcutControl {

    private ShortcutControl() {
        //no instance
    }

    public static void addShortcut(final Intent actionIntent, final String name, boolean async) {
        final ShortcutDBHelper helper = ShortcutDBHelper.getInstance();
        helper.executeTranaction(new SQLiteTransactionCallback() {
            @Override
            public boolean beforeTransaction(SQLiteDatabase db) {
                return false;
            }

            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                SQLiteStatement statement = helper.createInsertStatement(
                        ShortcutColumn.TABLE, ShortcutColumn.values(), null);

                statement.bindString(ShortcutColumn.action.ordinal() + 1, "" + actionIntent.getAction());
                statement.bindString(ShortcutColumn.categories.ordinal() + 1,
                        actionIntent.getCategories() != null ? TextUtils.join(",", actionIntent.getCategories()) : "");
                statement.bindString(ShortcutColumn.data.ordinal() + 1, "" + actionIntent.getDataString());
                statement.bindString(ShortcutColumn.name.ordinal() + 1, "" + name);
                statement.executeInsert();
                return true;
            }
        }, async);
    }

    public static boolean hasShortcut(Intent actionIntent, String name) {
        String table = ShortcutColumn.TABLE;
        String[] columns = {ShortcutColumn.action.name()};
        String selection = ShortcutColumn.action.name() + " = ?"
                + " AND " + ShortcutColumn.categories.name() + " = ?"
                + " AND " + ShortcutColumn.data.name() + " = ?"
                + " AND " + ShortcutColumn.name.name() + " = ?";
        String[] selectionArgs = {
                "" + actionIntent.getAction(),
                actionIntent.getCategories() != null ? TextUtils.join(",", actionIntent.getCategories()) : "",
                "" + actionIntent.getDataString(),
                "" + name,
        };
        Cursor cursor = ShortcutDBHelper.getInstance().getReadableDatabase().query(
                true, table, columns, selection, selectionArgs, null, null, null, null);

        boolean has = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                has = true;
            }
            cursor.close();
        }
        return has;
    }

    public static void removeShortcut(final Intent actionIntent, final String name, boolean async) {
        final ShortcutDBHelper helper = ShortcutDBHelper.getInstance();
        helper.executeTranaction(new SQLiteTransactionCallback() {
            @Override
            public boolean beforeTransaction(SQLiteDatabase db) {
                return false;
            }

            @Override
            public boolean performTransaction(SQLiteDatabase db) {
                SQLiteStatement statement = helper.createDeleteStatement(
                        ShortcutColumn.TABLE, ShortcutColumn.values(), null);

                statement.bindString(ShortcutColumn.action.ordinal() + 1, "" + actionIntent.getAction());
                statement.bindString(ShortcutColumn.categories.ordinal() + 1,
                        actionIntent.getCategories() != null ? TextUtils.join(",", actionIntent.getCategories()) : "");
                statement.bindString(ShortcutColumn.data.ordinal() + 1, "" + actionIntent.getDataString());
                statement.bindString(ShortcutColumn.name.ordinal() + 1, "" + name);
                statement.executeUpdateDelete();
                return true;
            }
        }, async);
    }
}