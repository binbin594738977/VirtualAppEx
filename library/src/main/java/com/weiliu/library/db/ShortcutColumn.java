package com.weiliu.library.db;

import android.support.annotation.NonNull;

/**
 * 快捷方式
 * Created by qumiao on 2016/5/16.
 */
public enum ShortcutColumn implements Column {

    action(new ColumnDesc("TEXT", true, false)),
    categories(new ColumnDesc("TEXT", true, false)),
    data(new ColumnDesc("TEXT", true, false)),
    name(new ColumnDesc("TEXT", true, false));

    final ColumnDesc columnDesc;

    ShortcutColumn(@NonNull ColumnDesc cd) {
        columnDesc = cd;
    }

    @NonNull
    @Override
    public ColumnDesc getDesc() {
        return columnDesc;
    }

    public static final String TABLE = "SHORTCUT";
    public static final int VERSION = 2;
}
