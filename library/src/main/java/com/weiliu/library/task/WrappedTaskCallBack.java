package com.weiliu.library.task;

import android.support.annotation.NonNull;

/**
 *
 * <br/>
 * Created by qumiao on 2016/8/22.
 */
public class WrappedTaskCallBack<ResultType> implements TaskCallBack<ResultType> {

    private TaskCallBack<ResultType> mCallBack;

    public WrappedTaskCallBack(@NonNull TaskCallBack<ResultType> callBack) {
        mCallBack = callBack;
    }

    @Override
    public void onPreviewWithCache(ResultType cache) {
        mCallBack.onPreviewWithCache(cache);
    }

    @Override
    public void onPreExecute() {
        mCallBack.onPreExecute();
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        mCallBack.onProgressUpdate(values);
    }

    @Override
    public void onCancelled(ResultType result, Object extra, Throwable exception) {
        mCallBack.onCancelled(result, extra, exception);
    }

    @Override
    public void onPostExecute(ResultType result, Object extra, Throwable exception) {
        mCallBack.onPostExecute(result, extra, exception);
    }

    @Override
    public boolean isResultFailed(ResultType result, Object extra, Throwable exception) {
        return mCallBack.isResultFailed(result, extra, exception);
    }

    @Override
    public void interruptFollowingTask() {
        mCallBack.interruptFollowingTask();
    }

    @Override
    public boolean isInterruptedFollowingTask() {
        return mCallBack.isInterruptedFollowingTask();
    }
}
