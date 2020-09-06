package com.weiliu.library.task;

import android.support.annotation.UiThread;

/**
 * 任务的回调（在UI线程中）。如果嫌回调方法太多，请使用{@link TaskCallBackImpl}
 * @param <ResultType> 执行任务的结果
 */
public interface TaskCallBack<ResultType> {
    @UiThread
    void onPreviewWithCache(ResultType cache);
    @UiThread
    void onPreExecute();
    @UiThread
    void onProgressUpdate(Integer... values);
    @UiThread
    void onCancelled(ResultType result, Object extra, Throwable exception);
    @UiThread
    void onPostExecute(ResultType result, Object extra, Throwable exception);
    @UiThread
    boolean isResultFailed(ResultType result, Object extra, Throwable exception);
    @UiThread
    void interruptFollowingTask();
    @UiThread
    boolean isInterruptedFollowingTask();
}
