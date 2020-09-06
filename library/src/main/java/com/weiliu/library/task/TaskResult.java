package com.weiliu.library.task;

/**
 * 执行任务的结果包装
 * @param <ResultType> 真正的结果
 */
public class TaskResult<ResultType> {
    /**真正的结果*/
    public ResultType value;
    /**是否保存为缓存*/
    public boolean saveCache;
    /**是否应该中止后续任务*/
    public boolean interrupt;
    /**附加数据*/
    public Object extra;

    /**执行任务时发生的异常*/
    public Throwable exception;

    public TaskResult(ResultType val) {
        value = val;
    }

    public TaskResult(ResultType val, boolean shouldSaveCache, boolean shouldInterrupt, Object ext) {
        value = val;
        saveCache = shouldSaveCache;
        interrupt = shouldInterrupt;
        extra = ext;
    }

    public TaskResult(ResultType val, boolean shouldSaveCache, boolean shouldInterrupt, Object ext, Throwable e) {
        value = val;
        saveCache = shouldSaveCache;
        interrupt = shouldInterrupt;
        extra = ext;
        exception = e;
    }
}
