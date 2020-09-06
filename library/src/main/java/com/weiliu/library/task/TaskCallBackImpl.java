package com.weiliu.library.task;

/**
 * 任务的回调（在UI线程中）。{@link TaskCallBack}的简化版
 * @param <ResultType> 执行任务的结果
 */
public abstract class TaskCallBackImpl<ResultType> implements TaskCallBack<ResultType> {

    private boolean mInterruptFollowingTask;

    @Override
    public void interruptFollowingTask() {
        mInterruptFollowingTask = true;
    }

    @Override
    public boolean isInterruptedFollowingTask() {
        return mInterruptFollowingTask;
    }

    @Override
    public void onPreviewWithCache(ResultType cache) {

    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(Integer... values) {

    }

    @Override
    public void onCancelled(ResultType result, Object extra, Throwable exception) {

    }

    @Override
    public void onPostExecute(ResultType result, Object extra, Throwable exception) {

    }

    @Override
    public boolean isResultFailed(ResultType result, Object extra, Throwable exception) {
        return exception != null;
    }
}
