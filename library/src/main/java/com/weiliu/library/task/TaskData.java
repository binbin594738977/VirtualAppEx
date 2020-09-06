package com.weiliu.library.task;

/**
 * 任务
 * @param <Param> 执行任务需要的参数
 * @param <ResultType> 执行任务的结果
 */
public class TaskData<Param, ResultType> {
    /**执行任务*/
    public TaskWorker<Param, ResultType> worker;

    /**执行任务的参数，在{@link TaskWorker#work}中用到*/
    public Param param;

    /**执行任务的回调*/
    public TaskCallBack<ResultType> callBack;

    /**缓存管理器*/
    public TaskCache<Param, ResultType> cache;

    /**延后多少毫秒执行*/
    public long delay;

    /**任务的权重，默认为100。该值代表任务执行时对总进度的影响。注意不能为0*/
    public int weight = 100;

    /**不随界面（如Activity）的生命周期结束而停止*/
    public boolean retain;

    /**下一个任务的相关数据。请勿自行设置！*/
    /*package*/ TaskData<Param, ResultType> next;
}
