package com.weiliu.library.task.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据库相关操作
 * Created by qumiao on 2016/5/14.
 */
abstract class DBHelper extends SQLiteOpenHelper {

    private static Executor sExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "DBHelper async #" + mCount.getAndIncrement());
        }
    });

    DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public final void onCreate(SQLiteDatabase db) {
        onUpdateDatabase(db, 0);
    }

    @Override
    public final void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpdateDatabase(db, oldVersion);
    }

    protected abstract void onUpdateDatabase(SQLiteDatabase db, int oldVersion);

    /**
     * 执行操作（更新、插入、删除）
     * @param callback
     * @param async 是否异步执行
     */
    public void executeTranaction(@NonNull SQLiteTransactionCallback callback, boolean async) {
        SQLiteTransaction transaction = new SQLiteTransaction(getWritableDatabase(), callback);
        if (async) {
            sExecutor.execute(transaction);
        } else {
            transaction.run();
        }
    }

    public static SQLiteStatement createInsertStatement(SQLiteDatabase db,
                                                        @NonNull String table,
                                                        @NonNull Enum<?>[] enumValues,
                                                        @Nullable List<? extends Enum<?>> ignore) {
        return db.compileStatement(createInsertStatementSql(table, enumValues, ignore));
    }

    public static String createInsertStatementSql(@NonNull String table,
                                                  @NonNull Enum<?>[] enumValues,
                                                  @Nullable List<? extends Enum<?>> ignore) {
        StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO " + table + " (");
        boolean empty = true;
        StringBuilder values = new StringBuilder(" VALUES (");
        for (Enum<?> column : enumValues) {
            if (ignore != null && ignore.contains(column)) {
                continue;
            }
            sql.append(empty ? "" : ", ");
            sql.append(column.name());
            values.append(empty ? "" : ", ");
            values.append("?");
            empty = false;
        }
        sql.append(")");
        values.append(")");
        sql.append(values);
        return sql.toString();
    }

    public static SQLiteStatement createDeleteStatement(SQLiteDatabase db,
                                                        @NonNull String table,
                                                        @NonNull Enum<?>[] conditionColumns,
                                                        @Nullable List<? extends Enum<?>> ignore) {
        return db.compileStatement(createDeleteStatementSql(table, conditionColumns, ignore));
    }

    public static String createDeleteStatementSql(@NonNull String table,
                                                  @NonNull Enum<?>[] conditionColumns,
                                                  @Nullable List<? extends Enum<?>> ignore) {
        StringBuilder sql = new StringBuilder("DELETE FROM " + table + " WHERE ");
        boolean empty = true;
        for (Enum<?> column : conditionColumns) {
            if (ignore != null && ignore.contains(column)) {
                continue;
            }
            sql.append(empty ? "" : " AND ");
            sql.append(column.name()).append(" = ?");
            empty = false;
        }
        return sql.toString();
    }

    public static <E extends Enum & Column> void createTable(SQLiteDatabase db, E[] columns, int oldVersion) {
        int version = getVersion(columns);
        String tableName = getTableName(columns);

        if (oldVersion >= version) {
            return;
        }

        db.execSQL(createTableSql(tableName, columns));

        createIndex(db, tableName, columns);
    }

    public static <E extends Enum & Column> String createTableSql(String tableName, E[] columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + "(");
        ArrayList<String> primaryKeys = new ArrayList<>();
        boolean begin = true;
        for (E column : columns) {
            if (!begin) {
                sql.append(", ");
            }
            if (column.getDesc().isPrimaryKey) {
                primaryKeys.add(column.name());
            }
            sql.append(column.name()).append(" ").append(column.getDesc().type);
            begin = false;
        }
        if (primaryKeys.size() > 0) {
            sql.append(", PRIMARY KEY (");
            begin = true;
            for (String key : primaryKeys) {
                sql.append(begin ? "" : ",");
                sql.append(key);
                begin = false;
            }
            sql.append(")");
        }
        for (E column : columns) {
            if (column.getDesc().isForeignKey) {
                sql.append(", FOREIGN KEY(").append(column.name())
                        .append(") REFERENCES ").append(column.getDesc().referenceTable)
                        .append("(").append(column.getDesc().referenceColumn).append(")");
            }
        }
        sql.append(")");
        return sql.toString();
    }

    /*private*/ static <E extends Enum & Column> String getTableName(E[] columns) {
        Class<?> cls = columns[0].getClass();
        try {
            Field tableNameField = cls.getField("TABLE");
            return (String) tableNameField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("必须定义 " + cls.getSimpleName() + " 的TABLE静态常量，并且值不能为空！");
        }
    }

    /*private*/ static <E extends Enum & Column> int getVersion(E[] columns) {
        Class<?> cls = columns[0].getClass();
        try {
            Field tableNameField = cls.getField("VERSION");
            return (int) tableNameField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("必须定义 " + cls.getSimpleName() + " 的VERSION静态常量！");
        }
    }

    private static <E extends Enum & Column> void createIndex(SQLiteDatabase db, String tableName, E[] columns) {
        for (E column : columns) {
            String indexSql = createIndexSql(tableName, column);
            if (indexSql != null) {
                db.execSQL(indexSql);
            }
        }
    }

    /*private*/ static <E extends Enum & Column> String createIndexSql(String tableName, E column) {
        if (column.getDesc().isIndex) {
            return "CREATE INDEX "
                    + tableName + "_" + column.name() + "_index_name"
                    + " ON " + tableName + "(" + column.name() + ")";
        }
        return null;
    }


    /**
     * <blockquote>
     * <p>获取下一个String，比如str为"abx"，那么下一个String就为"aby"。</p>
     * <p>该结果是用来配合实现 like "abx%"（前缀查询）的优化的，即：</p>
     * <p>    将 like "abx%" 转换为 value >= "abx" AND value < "aby"。</p>
     * <p>以下是英文解释：</p>
     * <p>Given a string x, this method returns the least string y such that x is not a prefix of y.</p>
     * <p>This is useful to implement prefix filtering by comparison, since the only strings z that
     * have x as a prefix are such that z is greater than or equal to x and z is less than y.</p>
     * </blockquote>
     *
     * @param str A non-empty string. The contract above is not honored for an empty input string,
     *        since all strings have the empty string as a prefix.
     * @return next-value String
     */
    public static String nextString(String str) {
        int len = str.length();
        if (len == 0) {
            return str;
        }
        // The last code point in the string. Within the Basic Multilingual Plane,
        // this is the same as str.charAt(len-1)
        int codePoint = str.codePointBefore(len);
        // This should be safe from overflow, since the largest code point
        // representable in UTF-16 is U+10FFFF.
        int nextCodePoint = codePoint + 1;
        // The index of the start of the last code point.
        // Character.charCount(codePoint) is always 1 (in the BMP) or 2
        int lastIndex = len - Character.charCount(codePoint);
        return new StringBuilder(len)
                .append(str, 0, lastIndex)  // append everything but the last code point
                .appendCodePoint(nextCodePoint)  // instead of the last code point, use successor
                .toString();
    }
}
