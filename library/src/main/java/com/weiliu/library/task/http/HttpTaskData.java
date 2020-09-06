package com.weiliu.library.task.http;


import com.weiliu.library.task.TaskData;

/**
 * TaskData for http.
 * Created by qumiao on 2016/7/20.
 */
public class HttpTaskData extends TaskData<HttpRequestObject, HttpResponseObject> {
    /**是否使用统一的配置（在同一个任务组中）*/
    public boolean uniformConfig = true;

    public HttpRequestObject getRequest() {
        return param;
    }

    public HttpWorker getWorker() {
        return (HttpWorker) worker;
    }

    public HttpCache getCache() {
        return (HttpCache) cache;
    }

    public HttpCallBack getCallBack() {
        return (HttpCallBack) callBack;
    }
}
