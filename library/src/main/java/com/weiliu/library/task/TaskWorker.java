package com.weiliu.library.task;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

/**
 * 任务的执行体。可能会比较耗时（如联网操作），所以在非UI线程中执行
 * Created by qumiao on 2016/5/3.
 */
public interface TaskWorker<Param, ResultType> {

    @WorkerThread
    TaskResult<ResultType> work(@Nullable TaskProgressDeliver progressDeliver, Param param);
}
