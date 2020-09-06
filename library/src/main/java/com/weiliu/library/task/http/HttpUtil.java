package com.weiliu.library.task.http;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.weiliu.library.task.TaskCache;
import com.weiliu.library.task.http.retry.DefaultRetryPolicy;

import java.io.File;


/**
 * http常量和常用方法管理
 * Created by qumiao on 2016/5/3.
 */
public class HttpUtil {

    public static final String UTF_8 = "UTF-8";

    public static final String POST = "POST";

    public static final String GET = "GET";

    public static final String GZIP = "gzip";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final char PARAMETER_DELIMITER = '&';

    public static final char PARAMETER_EQUALS_CHAR = '=';

    public static final String CONTENT_TYPE_FORM_URLENCODE = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; boundary=";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "text/xml";

    public static final String PROTOCOL_HTTP = "http";
    public static final int DEFAULT_TIME_OUT = 20000;


    public static final String PROTOCOL_HTTPS = "https";


    public static final int SC_UNKNOWN = -1;
    public static final int SC_TIME_OUT = -2;
    public static final int SC_UNKNOWN_HOST = -3;
    public static final int SC_CONNECT = -4;

    private HttpUtil() {
        //no instance
    }

    public static HttpTaskData createByteArrayTaskData(@Nullable Context context,
                                                        @NonNull String url, @Nullable HttpParams params,
                                                        @NonNull HttpByteArrayCallBack callBack) {
        HttpTaskData taskData = createHttpTaskData(context, url, params, callBack);
        taskData.param.channel = new HttpByteArrayChannel();
        return taskData;
    }

    public static HttpTaskData createBitmapTaskData(@Nullable Context context,
                                                        @NonNull String url, @Nullable HttpParams params,
                                                        @NonNull HttpBitmapCallBack callBack) {
        HttpTaskData taskData = createHttpTaskData(context, url, params, callBack);
        taskData.param.channel = new HttpBitmapChannel();
        return taskData;
    }

    public static HttpTaskData createFileTaskData(@Nullable Context context,
                                                        @NonNull String url, @Nullable HttpParams params,
                                                        @Nullable File file, @NonNull HttpFileCallBack callBack) {
        HttpTaskData taskData = createHttpTaskData(context, url, params, callBack);
        if (file == null) {
            if (context == null) {
                throw new RuntimeException("startFileTask使用默认File的时候，TaskStarter的Context不能为空！");
            }
            file = HttpCache.getCacheFile(context, taskData.param);
        }
        taskData.param.channel = new HttpFileChannel(file);
        return taskData;
    }


    public static  <T> HttpTaskData createHttpTaskData(@Nullable Context context,
                                                       @NonNull String url, @Nullable HttpParams params,
                                                       @NonNull HttpCallBack<T> callBack) {
        return createHttpTaskData(context, url, params, false, callBack);
    }


    public static  <T> HttpTaskData createHttpTaskData(@Nullable Context context,
                                                       @NonNull String url, @Nullable HttpParams params,
                                                       boolean appendToResumeTaskListIfFailed,
                                                       @NonNull HttpCallBack<T> callBack) {
        return createHttpTaskData(context,
                url,
                params,
                appendToResumeTaskListIfFailed,
                appendToResumeTaskListIfFailed || context == null ? null : new HttpCache(context),
                callBack,
                0);
    }

    public static <T> HttpTaskData createHttpTaskData(@Nullable Context context,
                                                      @NonNull String url, @Nullable HttpParams params,
                                                      boolean appendToResumeTaskListIfFailed,
                                                      @Nullable TaskCache<HttpRequestObject, HttpResponseObject> cache,
                                                      @NonNull HttpCallBack<T> callBack,
                                                      long delay) {
        HttpRequestObject requestObject = new HttpRequestObject();
        requestObject.url = url;
        requestObject.params = params;
        requestObject.appendToResumeTaskListIfFailed = appendToResumeTaskListIfFailed;
        requestObject.traceData = new HttpTraceObject();
        requestObject.requestTime = System.currentTimeMillis();

        HttpTaskData taskData = new HttpTaskData();
        taskData.param = requestObject;
        taskData.callBack = callBack;
        HttpWorker worker = new HttpWorker(context);
        worker.setRetryPolicy(new DefaultRetryPolicy());
        taskData.worker = worker;
        taskData.cache = cache;
        taskData.retain = appendToResumeTaskListIfFailed;
        taskData.delay = delay;

        return taskData;
    }
}
