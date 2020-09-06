package com.weiliu.library.task.db;

import android.support.annotation.NonNull;

import com.weiliu.library.util.NoProguard;

/**
 * 所有table column枚举实现此接口
 * Created by qumiao on 2016/5/14.
 */
interface Column extends NoProguard {

    @NonNull
    ColumnDesc getDesc();
}
