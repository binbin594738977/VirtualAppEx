package com.weiliu.library.task;

import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 基于AsyncTask的改造，实现多个任务串行执行
 * Created by qumiao on 16/4/27.
 */
public class LinkedTask<Param, ResultType> extends AsyncTask<Param, Integer, TaskResult<ResultType>> {

    private TaskData<Param, ResultType> mTaskData;
    private TaskProgress mTaskProgress;
    private OnExecuteNextListener<Param, ResultType> mOnExecuteNextListener;
    private boolean mSync;

    private TaskProgressDeliver mProgressDeliver = new TaskProgressDeliver() {
        @Override
        public void publishProgress(Integer... values) {
            LinkedTask.this.publishProgress(values);
        }
    };

    public LinkedTask(@NonNull TaskData<Param, ResultType> taskData,
                      @Nullable TaskProgress taskProgress,
                      @NonNull OnExecuteNextListener<Param, ResultType> listener,
                      boolean sync) {
        mTaskData = taskData;
        mTaskProgress = taskProgress;
        mOnExecuteNextListener = listener;
        mSync = sync;
    }

    @Override
    @CallSuper
    protected void onPreExecute() {
        TaskCallBack<ResultType> callBack = mTaskData.callBack;
        if (callBack != null) {
            callBack.onPreExecute();
        }
    }

    @Override
    protected TaskResult<ResultType> doInBackground(Param[] params) {
        long delay = mTaskData.delay;
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return null;
            }
        }

        TaskWorker<Param, ResultType> worker = mTaskData.worker;
        if (worker != null) {
            TaskResult<ResultType> result = worker.work(mProgressDeliver, params[0]);
            TaskCache<Param, ResultType> cache = mTaskData.cache;
            if (cache != null) {
                if (result != null && result.saveCache && result.value != null && result.exception == null) {
                    cache.save(params[0], result.value);
                }/* else if (result.exception != null) {
                    if (mTaskData.cache.hit(params[0])) {
                        result = new TaskResult<>(mTaskData.cache.read(params[0], result.exception), false, result.extra);
                    }
                }*/
            }
            return result;
        }
        return null;
    }

    @Override
    @CallSuper
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (mTaskProgress != null) {
            int total = 0;
            for (int val : values) {
                total += val;
            }
            int avg = total / values.length;
            int curProgress = avg * mTaskData.weight / mTaskProgress.getTotalWeight();
            mTaskProgress.setProgress(mTaskProgress.getProgress() + curProgress);
        } else {
            TaskCallBack<ResultType> callBack = mTaskData.callBack;
            if (callBack != null) {
                callBack.onProgressUpdate(values);
            }
        }

    }

    @Override
    @CallSuper
    protected void onCancelled(TaskResult<ResultType> taskResult) {
        if (mTaskProgress != null) {
            mTaskProgress.hide();
        }

        TaskCallBack<ResultType> callBack = mTaskData.callBack;
        if (callBack != null) {
            if (taskResult == null) {
                callBack.onCancelled(null, null, null);
            } else {
                callBack.onCancelled(taskResult.value, taskResult.extra, taskResult.exception);
            }

        }

        clear();
    }

    @Override
    @CallSuper
    protected void onPostExecute(TaskResult<ResultType> taskResult) {
        boolean interruptFromCallBack = false;
        TaskCallBack<ResultType> callBack = mTaskData.callBack;
        if (callBack != null) {
            if (taskResult == null) {
                callBack.onPostExecute(null, null, null);
            } else {
                callBack.onPostExecute(taskResult.value, taskResult.extra, taskResult.exception);
            }
            interruptFromCallBack = callBack.isInterruptedFollowingTask();
        }

        if (mTaskProgress != null) {
            mTaskProgress.setProgress(mTaskProgress.getProgress()
                    + 100 * mTaskData.weight / mTaskProgress.getTotalWeight());
        }

        TaskData<Param, ResultType> nextTaskData = mTaskData.next;
        if (nextTaskData == null) {
            if (mTaskProgress != null) {
                mTaskProgress.hide();
            }
        }

        if (!interruptFromCallBack && !(taskResult != null && taskResult.interrupt)) {
            if (nextTaskData != null) {
                mOnExecuteNextListener.execute(nextTaskData, mSync);
            }
        } else {
            if (mTaskProgress != null) {
                mTaskProgress.hide();
            }
        }

        // 任务都执行完毕了，随时就可以回收了，不需要清除（因为或许还有retry）
//        clear();
    }

    /**
     * 同步执行任务
     * @param params
     */
    @SafeVarargs
    public final void executeSync(Param... params) {
        onPreExecute();
        TaskResult<ResultType> result = doInBackground(params);
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
    }

    private void clear() {
        mTaskProgress = null;
        // callBack = null 很有必要，因为很多callBack重载时可能携带Activity级的引用
        mTaskData.callBack = null;
        // worker = null 没有实际意义，因为是个耗时操作，就算这里置为null了，还有引用在跑
//        mTaskData.worker = null;
    }


    public interface OnExecuteNextListener<Param, ResultType> {
        void execute(TaskData<Param, ResultType> data, boolean sync);
    }

}
