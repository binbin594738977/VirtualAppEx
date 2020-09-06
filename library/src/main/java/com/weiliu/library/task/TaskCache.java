package com.weiliu.library.task;

/**
 * 任务的缓存管理。
 * Created by qumiao on 2016/5/18.
 */
public interface TaskCache<Param, ResultType> {

    boolean hit(Param param);

    void save(Param param, ResultType resultValue);

    ResultType read(Param param);
}
