/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weiliu.library.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * helper base class for SQLite write transactions.
 */
public class SQLiteTransaction implements Runnable {

    private SQLiteDatabase mDb;
    private SQLiteTransactionCallback mCallback;

    public SQLiteTransaction(SQLiteDatabase db, SQLiteTransactionCallback callback) {
        mDb = db;
        mCallback = callback;
    }

    /**
     * Runs the transaction against the database. The results are committed if
     * {@link SQLiteTransactionCallback#performTransaction(SQLiteDatabase)} completes normally and returns {@code true}.
     */
    @Override
    public void run() {
        if (mCallback.beforeTransaction(mDb)) {
            return;
        }

        mDb.beginTransaction();
        try {
            if (mCallback.performTransaction(mDb)) {
                mDb.setTransactionSuccessful();
            }
        } finally {
            mDb.endTransaction();
            //此操作有可能引发与查询的冲突，因为getWritableDatabase和getReadableDataBase是一个实例。
            //在程序退出的时候执行关闭即可。
            //db.close();
        }
    }
}
