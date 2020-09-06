package com.weiliu.library.db;

import android.support.annotation.NonNull;

import com.weiliu.library.util.NoProguard;

/**
 * 所有table column枚举实现此接口
 * Created by qumiao on 2016/5/14.
 */
public interface Column extends NoProguard {

    @NonNull
    ColumnDesc getDesc();
}
