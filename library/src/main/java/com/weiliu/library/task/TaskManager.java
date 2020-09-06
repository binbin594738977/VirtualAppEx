package com.weiliu.library.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * <p>使用该类来执行 LinkedTask任务组（也可以是单个任务组成的任务组）。
 *
 * Created by qumiao on 16/4/27.
 */
public class TaskManager {

    /**非retain任务组中，当前执行的任务映射*/
    private final Map<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> mCurrentTaskMap = new HashMap<>();
    /**非retain任务组中，当前执行的缓存预览任务映射*/
    private final Map<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> mCurrentCacheTaskMap = new HashMap<>();

    /**retain（即不随界面销毁而停止）任务组中，当前执行的任务映射。 这种任务不存在缓存预览（因为没有界面依赖）*/
    private final Map<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> mCurrentRetainTaskMap = new HashMap<>();

    private Executor mExecutor;

    /**是否同步执行任务*/
    private boolean mSync;

    public TaskManager() {
        this(AsyncTask.THREAD_POOL_EXECUTOR, false);
    }

    public TaskManager(boolean sync) {
        this(AsyncTask.THREAD_POOL_EXECUTOR, sync);
    }

    public TaskManager(Executor executor, boolean sync) {
        mExecutor = executor;
        mSync = sync;
    }

    public void setExecutor(Executor executor) {
        mExecutor = executor;
    }

    /**
     * 是否同步执行。
     * @param sync 为true表示同步执行（回调和执行体处于同一个线程），为false表示异步执行（回调永远在主线程）。
     */
    public void setSync(boolean sync) {
        mSync = sync;
    }

    /**
     * 开始执行任务
     * @param task 单个任务组成的任务列表
     * @param <Param>
     * @param <ResultType>
     * @return 返回该任务组的标记，用于后续的控制
     */
    public <Param, ResultType> TaskGroupTag start(@NonNull TaskData<Param, ResultType> task,
                                                  @Nullable TaskProgress taskProgress) {
        return start(Collections.singletonList(task), taskProgress);
    }


    /**
     * 开始执行任务
     * @param tasks 任务列表
     * @return 返回该任务组的标记，用于后续的控制
     */
    public TaskGroupTag start(@NonNull List<? extends TaskData> tasks,
                                                  @Nullable final TaskProgress taskProgress) {
        final TaskGroupTag tag = new TaskGroupTag();

        if (tasks.isEmpty()) {
            return tag;
        }

        final TaskData firstTaskData = tasks.get(0);

        int totalWeight = 0;
        TaskData preData = firstTaskData;
        for (TaskData taskData : tasks) {
            if (taskData != preData) {
                preData.next = taskData;
            }
            totalWeight += taskData.weight;

            preData = taskData;
        }

        if (taskProgress != null) {
            taskProgress.setProgress(0);
            taskProgress.setTotalWeight(totalWeight);
            taskProgress.show();
        }

        List<TaskData> cacheTasks = new ArrayList<>();
        TaskData preCacheData = null;
        for (final TaskData taskData : tasks) {
            if (/*!taskData.retain && */taskData.cache != null && taskData.callBack != null) {
                final TaskData cacheTaskData = new TaskData<>();
                cacheTaskData.param = taskData.param;
                cacheTaskData.worker = new TaskWorker() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public TaskResult<Object> work(TaskProgressDeliver deliver, Object param) {
                        if (taskData.cache.hit(taskData.param)) {
                            return new TaskResult<>(taskData.cache.read(taskData.param));
                        }
                        return null;
                    }
                };
                cacheTaskData.callBack = new TaskCallBackImpl<Object>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onPostExecute(Object result, Object extra, Throwable exception) {
                        if (result != null && taskData.callBack != null) {
                            taskData.callBack.onPreviewWithCache(result);
                        }
                        if (cacheTaskData.next == null) {
                            executeTask(firstTaskData, taskProgress, tag, mSync);
                        }
                    }
                };

                if (preCacheData != null) {
                    preCacheData.next = cacheTaskData;
                }
                preCacheData = cacheTaskData;

                cacheTasks.add(cacheTaskData);
            }
        }

        if (!cacheTasks.isEmpty()) {
            executeCacheTask(cacheTasks.get(0), tag, mSync);
        } else {
            executeTask(firstTaskData, taskProgress, tag, mSync);
        }

        return tag;
    }


    /**
     *
     * @param tag 如果为null，则只要有一组任务正在执行，就返回true。否则，只查询指定的任务组
     * @return
     */
    public boolean isRunning(@Nullable TaskGroupTag tag, boolean includeRetainTask) {
        if (tag == null) {
            for (Map.Entry<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> entry : mCurrentCacheTaskMap.entrySet()) {
                LinkedTask task = getValueFromReference(entry.getValue());
                if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                    return true;
                }
            }

            for (Map.Entry<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> entry : mCurrentTaskMap.entrySet()) {
                LinkedTask task = getValueFromReference(entry.getValue());
                if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                    return true;
                }
            }

            if (includeRetainTask) {
                for (Map.Entry<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> entry : mCurrentRetainTaskMap.entrySet()) {
                    LinkedTask task = getValueFromReference(entry.getValue());
                    if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                        return true;
                    }
                }
            }

            return false;
        }

        LinkedTask cacheTask = getValueFromReference(mCurrentCacheTaskMap.get(tag));
        if (cacheTask != null && cacheTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }

        LinkedTask<?, ?> task = getValueFromReference(mCurrentTaskMap.get(tag));
        return task != null && task.getStatus() != AsyncTask.Status.FINISHED;
    }

    /**
     *
     * @param tag 如果为null，则结束所有任务组。否则，只结束指定的任务组
     */
    public void stop(@Nullable TaskGroupTag tag) {
        if (tag == null) {
            for (Map.Entry<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> entry : mCurrentCacheTaskMap.entrySet()) {
                LinkedTask task = getValueFromReference(entry.getValue());
                if (task != null) {
                    task.cancel(true);
                }
            }
            for (Map.Entry<TaskGroupTag, WeakReference<LinkedTask<?, ?>>> entry : mCurrentTaskMap.entrySet()) {
                LinkedTask task = getValueFromReference(entry.getValue());
                if (task != null) {
                    task.cancel(true);
                }
            }

            return;
        }

        LinkedTask cacheTask = getValueFromReference(mCurrentCacheTaskMap.get(tag));
        if (cacheTask != null) {
            cacheTask.cancel(true);
        }

        LinkedTask task = getValueFromReference(mCurrentTaskMap.get(tag));
        if (task != null) {
            task.cancel(true);
        }
    }


    private <Param, ResultType> void executeCacheTask(
            final TaskData<Param, ResultType> taskData, final TaskGroupTag tag, boolean sync) {
        MyCacheExecuteNextListener<Param, ResultType> listener =
                new MyCacheExecuteNextListener<>(tag);
        listener.execute(taskData, sync);
    }

    private <Param, ResultType> void executeTask(
            final TaskData<Param, ResultType> taskData,
            final TaskProgress taskProgress, final TaskGroupTag tag, boolean sync) {
        MyExecuteNextListener<Param, ResultType> listener =
                new MyExecuteNextListener<>(taskData.retain, taskProgress, tag);
        listener.execute(taskData, sync);
    }

    @SuppressWarnings("deprecation")
    public static TaskProgressDialog createTaskProgressDialog(Context context, String message, boolean indeterminate) {
        TaskProgressDialog dialog = new TaskProgressDialog(context);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog.setMessage(message);
        if (!indeterminate) {
            dialog.setIndeterminate(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        } else {
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }

        return dialog;
    }

    private static <Param, ResultType> void executeTask(Executor executor,
                                                        LinkedTask<Param, ResultType> task, TaskData<Param, ResultType> data, boolean sync) {
        if (sync) {
            task.executeSync(data.param);
        } else {
            try {
                //noinspection unchecked
                task.executeOnExecutor(executor, data.param);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    static <T> T getValueFromReference(@Nullable Reference<T> reference) {
        return reference == null ? null : reference.get();
    }

    private class MyCacheExecuteNextListener<Param, ResultType>
            implements LinkedTask.OnExecuteNextListener<Param, ResultType> {

        TaskGroupTag mTag;

        MyCacheExecuteNextListener(TaskGroupTag tag) {
            mTag = tag;
        }

        @Override
        public void execute(final TaskData<Param, ResultType> data, boolean sync) {

            final LinkedTask<Param, ResultType> task =
                    new LinkedTask<>(data, null, this, sync);

            mCurrentCacheTaskMap.put(mTag, new WeakReference<LinkedTask<?, ?>>(task));
            executeTask(mExecutor, task, data, sync);
        }
    }

    private class MyExecuteNextListener<Param, ResultType>
            implements LinkedTask.OnExecuteNextListener<Param, ResultType> {

        boolean mRetain;
        TaskProgress mTaskProgress;
        TaskGroupTag mTag;

        MyExecuteNextListener(boolean retain, TaskProgress taskProgress, TaskGroupTag tag) {
            mRetain = retain;
            mTaskProgress = taskProgress;
            mTag = tag;
        }

        @Override
        public void execute(final TaskData<Param, ResultType> data, boolean sync) {

            final LinkedTask<Param, ResultType> task =
                    new LinkedTask<>(data, mTaskProgress, this, sync);

            if (!mRetain) {
                mCurrentTaskMap.put(mTag, new WeakReference<LinkedTask<?, ?>>(task));
                executeTask(mExecutor, task, data, sync);

            } else {
                mCurrentRetainTaskMap.put(mTag, new WeakReference<LinkedTask<?, ?>>(task));
                executeTask(mExecutor, task, data, sync);
            }

        }
    }

}
