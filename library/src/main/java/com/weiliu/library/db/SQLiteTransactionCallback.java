package com.weiliu.library.db;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by qumiao on 2016/5/16.
 */
public interface SQLiteTransactionCallback {

    /**
     * before transaction
     * @param db
     * @return {@code true} if the transaction should interrupted.
     */
    boolean beforeTransaction(SQLiteDatabase db);

    /**
     * Executes the statements that form the transaction.
     *
     * @param db A writable database.
     * @return {@code true} if the transaction should be committed.
     */
    boolean performTransaction(SQLiteDatabase db);
}
