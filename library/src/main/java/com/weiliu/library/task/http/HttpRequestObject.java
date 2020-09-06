package com.weiliu.library.task.http;


import com.weiliu.library.json.JsonInterface;

/**
 * http请求需要的结构
 * Created by qumiao on 2016/5/3.
 */
public class HttpRequestObject implements JsonInterface {

    public String url;

    public HttpParams params;

    /**如果执行失败（但是状态码标明可以重试），是否追加到需要恢复执行的任务列表里*/
    public boolean appendToResumeTaskListIfFailed;

    /**请求时间的记录。该字段会自动设置，主要是用来对恢复执行的任务列表进行排序*/
    public long requestTime;

    /**一般情况下，只是解析response body中的data字段。如果需要解析整个response body，请将该字段设为true。*/
    public boolean wholeResponse;

    /**将响应结果以二进制方式保存在该通道中*/
    public transient HttpBinaryChannel channel;

    public HttpTraceObject traceData = new HttpTraceObject();
}
