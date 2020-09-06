package com.weiliu.library.task.db;

import android.support.annotation.NonNull;

/**
 * 需要恢复执行的http任务列表
 * Created by qumiao on 2016/5/16.
 */
enum ResumeHttpTaskColumn implements Column {

    url(new ColumnDesc("TEXT")),
    params(new ColumnDesc("TEXT")),
    request_time(new ColumnDesc("LONG")),
    whole_response(new ColumnDesc("INT"));

    final ColumnDesc columnDesc;

    ResumeHttpTaskColumn(@NonNull ColumnDesc cd) {
        columnDesc = cd;
    }

    @NonNull
    @Override
    public ColumnDesc getDesc() {
        return columnDesc;
    }

    public static final String TABLE = "HTTP_TASK";
    public static final int VERSION = 1;
}
