package com.weiliu.library.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weiliu.library.R;
import com.weiliu.library.task.http.HttpBitmapCallBack;
import com.weiliu.library.task.http.HttpByteArrayCallBack;
import com.weiliu.library.task.http.HttpCallBack;
import com.weiliu.library.task.http.HttpCallBackNoResult;
import com.weiliu.library.task.http.HttpFileCallBack;
import com.weiliu.library.task.http.HttpParams;
import com.weiliu.library.task.http.HttpRequestObject;
import com.weiliu.library.task.http.HttpResponseObject;
import com.weiliu.library.task.http.HttpTaskData;
import com.weiliu.library.task.http.HttpUtil;
import com.weiliu.library.task.http.UrlParams;
import com.weiliu.library.task.http.retry.DefaultRetryPolicy;
import com.weiliu.library.task.http.retry.RetryPolicy;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by qumiao on 2016/5/9.
 */
public class TaskStarter {

    private Context mContext;
    private TaskManager mTaskManager;
    private TaskProgressDialog mDialog;

    private static int DEFAULT_TIME_OUT = 5000;
    private static int DEFAULT_RETRY_COUNT = 1;

    private int mHttpConnectTimeOut = DEFAULT_TIME_OUT;
    private int mHttpReadTimeOut = DEFAULT_TIME_OUT;
    private int mHttpRetryCount = DEFAULT_RETRY_COUNT;
    private RetryPolicy mHttpRetryPolicy = new DefaultRetryPolicy();
    private boolean mHttpWholeResponse;
    private boolean mHttpAppendToResumeTaskListIfFailed;
    private boolean mHttpRetain;

    /**
     * @param context 如果为空，则无法使用默认缓存，也无法实现接口失败后下一次自动执行的功能
     */
    public TaskStarter(@Nullable Context context) {
        super();
        mContext = context != null ? context.getApplicationContext() : null;
        mTaskManager = new TaskManager();
    }

    /**
     * @param context     如果为空，则无法使用默认缓存，也无法实现接口失败后下一次自动执行的功能
     * @param taskManager
     * @param dialog      如果为空，则无法默认弹出对话框提示进度
     */
    public TaskStarter(@Nullable Context context, @NonNull TaskManager taskManager,
                       @Nullable TaskProgressDialog dialog) {
        mContext = context != null ? context.getApplicationContext() : null;
        mTaskManager = taskManager;
        mDialog = dialog;
    }

    /**
     * 设置http读取超时时间，单位为毫秒（默认为15000毫秒）。
     * 如果设置了重试策略{@link #setHttpRetryPolicy(RetryPolicy)}，则可能被覆盖。
     *
     * @param timeOut
     * @return
     */
    public TaskStarter setHttpReadTimeOut(int timeOut) {
        mHttpReadTimeOut = timeOut;
        return this;
    }

    /**
     * 设置http连接超时时间，单位为毫秒（默认为15000毫秒）。
     * 如果设置了重试策略{@link #setHttpRetryPolicy(RetryPolicy)}，则可能被覆盖。
     *
     * @param timeOut
     * @return
     */
    public TaskStarter setHttpConnectTimeOut(int timeOut) {
        mHttpConnectTimeOut = timeOut;
        return this;
    }

    /**
     * 设置http请求失败后的重试次数（默认为1次）。
     * 如果设置了重试策略{@link #setHttpRetryPolicy(RetryPolicy)}，则可能被覆盖。
     *
     * @param retryCount
     * @return
     */
    public TaskStarter setHttpRetryCount(int retryCount) {
        mHttpRetryCount = retryCount;
        return this;
    }

    /**
     * 设置http响应数据的解析策略：<br/>
     * 为true：将整个response内容作为data解析；<br/>
     * 为false：则将response视为标准的{code:xxx, msg:xxx, data:xxx}，并只解析其中的data内容。<br/>
     * 默认为false。
     *
     * @param wholeResponse
     * @return
     */
    public TaskStarter setHttpWholeResponse(boolean wholeResponse) {
        mHttpWholeResponse = wholeResponse;
        return this;
    }

    /**
     * 设置重试策略。不为空时，可能会重写设定的超时时间。
     *
     * @param retryPolicy 默认为DefaultRetryPolicy，可以自定义，或者指定为空。
     * @return
     */
    public TaskStarter setHttpRetryPolicy(@Nullable RetryPolicy retryPolicy) {
        mHttpRetryPolicy = retryPolicy;
        return this;
    }

    /**
     * http请求如果执行失败（但是状态码标明可以重试），是否追加到需要恢复执行的任务列表里
     *
     * @param appendToResumeTaskListIfFailed 默认为false。
     *                                       如果设为true，则默认将retain也设为true，
     *                                       即保持该任务到其执行完毕，而不随界面（如Activity）的生命周期结束而停止
     * @return
     */
    public TaskStarter setHttpAppendToResumeTaskListIfFailed(boolean appendToResumeTaskListIfFailed) {
        mHttpAppendToResumeTaskListIfFailed = appendToResumeTaskListIfFailed;
        mHttpRetain = appendToResumeTaskListIfFailed;
        return this;
    }

    /**
     * 是否保持任务到其执行完毕，而不随界面（如Activity）的生命周期结束而停止
     *
     * @param retain 默认为false。调用{@link #setHttpAppendToResumeTaskListIfFailed}会影响该值
     * @return
     */
    public TaskStarter setHttpRetain(boolean retain) {
        mHttpRetain = retain;
        return this;
    }


    private void tryToConfigHttpTaskData(List<? extends TaskData> taskList) {
        for (TaskData data : taskList) {
            if (!(data instanceof HttpTaskData)) {
                continue;
            }
            HttpTaskData taskData = (HttpTaskData) data;
            if (!taskData.uniformConfig) {
                continue;
            }

            if (taskData.param.params != null) {
                taskData.param.params.refresh();
            }
            taskData.getWorker().setConnectTimeOut(mHttpConnectTimeOut);
            taskData.getWorker().setReadTimeOut(mHttpReadTimeOut);
            taskData.getWorker().setRetryCount(mHttpRetryCount);
            if (mHttpRetryPolicy instanceof DefaultRetryPolicy) {
                DefaultRetryPolicy defaultRetryPolicy = (DefaultRetryPolicy) mHttpRetryPolicy;
                defaultRetryPolicy.setDefaultTimeoutMs(mHttpConnectTimeOut);
                defaultRetryPolicy.setDefaultReadTimeoutMs(mHttpReadTimeOut);
                defaultRetryPolicy.setDefaultMaxRetries(mHttpRetryCount);
            }
            taskData.getWorker().setRetryPolicy(mHttpRetryPolicy);

            taskData.getRequest().wholeResponse = mHttpWholeResponse;
            taskData.getRequest().appendToResumeTaskListIfFailed = mHttpAppendToResumeTaskListIfFailed;
            taskData.retain = mHttpRetain;
        }

        // 重置，以免影响下一次的配置
        mHttpConnectTimeOut = DEFAULT_TIME_OUT;
        mHttpReadTimeOut = DEFAULT_TIME_OUT;
        mHttpRetryCount = DEFAULT_RETRY_COUNT;
        mHttpRetryPolicy = new DefaultRetryPolicy();
        mHttpWholeResponse = false;
        mHttpAppendToResumeTaskListIfFailed = false;
        mHttpRetain = false;
    }

    /**
     * 简单的http提交任务。响应结果只考虑成功和失败。
     * <ul>
     * <li>该任务<b> 不会 </b>随界面（如Activity）的生命周期结束而自动停止。</li>
     * <li>如果任务执行失败，会在下一次http请求时自动重新执行。</li>
     * </ul>
     *
     * @param urlParams
     * @return 任务的标记，便于手动stop时引用
     */
    public TaskGroupTag startAutoCommitTask(
            @NonNull UrlParams urlParams) {
        return startAutoCommitTask(urlParams.getUrl(), urlParams.getParams(), null);
    }

    /**
     * 简单的http提交任务。响应结果只考虑成功和失败。
     * <ul>
     * <li>该任务<b> 不会 </b>随界面（如Activity）的生命周期结束而自动停止。</li>
     * <li>如果任务执行失败，会在下一次http请求时自动重新执行。</li>
     * </ul>
     *
     * @param url
     * @param params
     * @return 任务的标记，便于手动stop时引用
     */
    public TaskGroupTag startAutoCommitTask(
            @NonNull String url, @NonNull HttpParams params) {
        return startAutoCommitTask(url, params, null);
    }

    /**
     * http提交任务。
     * <ul>
     * <li>该任务<b> 不会 </b>随界面（如Activity）的生命周期结束而自动停止。</li>
     * <li>如果任务执行失败，会在下一次http请求时自动重新执行。</li>
     * </ul>
     *
     * @param url
     * @param params
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startAutoCommitTask(
            @NonNull String url, @Nullable HttpParams params, @Nullable HttpCallBack<T> callBack) {
        HttpTaskData taskData = HttpUtil.createHttpTaskData(
                mContext, url, params, true, callBack != null ? callBack : new HttpCallBackNoResult());
        return startTask(taskData, null);
    }

    /**
     * 提交http任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param urlParams
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startTask(
            @NonNull UrlParams urlParams, @NonNull HttpCallBack<T> callBack) {
        HttpTaskData taskData = HttpUtil.createHttpTaskData(mContext, urlParams.getUrl(), urlParams.getParams(), callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交http任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startTask(
            @NonNull String url, @Nullable HttpParams params, @NonNull HttpCallBack<T> callBack) {
        HttpTaskData taskData = HttpUtil.createHttpTaskData(mContext, url, params, callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交字节流下载任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startByteArrayTask(
            @NonNull String url, @Nullable HttpParams params, @NonNull HttpByteArrayCallBack callBack) {
        HttpTaskData taskData = HttpUtil.createByteArrayTaskData(mContext, url, params, callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交Bitmap下载任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startBitmapTask(
            @NonNull String url, @Nullable HttpParams params, @NonNull HttpBitmapCallBack callBack) {
        HttpTaskData taskData = HttpUtil.createBitmapTaskData(mContext, url, params, callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交文件下载任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startFileTask(
            @NonNull String url, @Nullable HttpParams params, @NonNull HttpFileCallBack callBack) {
        HttpTaskData taskData = HttpUtil.createFileTaskData(mContext, url, params, null, callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交文件下载任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startFileTask(
            @NonNull String url, @Nullable HttpParams params, File file, @NonNull HttpFileCallBack callBack) {
        HttpTaskData taskData = HttpUtil.createFileTaskData(mContext, url, params, file, callBack);
        return startTask(taskData, null);
    }

    /**
     * 提交http任务。该任务会随界面（如Activity）的生命周期结束而自动停止。
     *
     * @param url
     * @param params
     * @param callBack
     * @param loadFrame 任务加载监听的界面框架
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startTask(
            @NonNull String url, @Nullable HttpParams params, @NonNull HttpCallBack<T> callBack,
            @Nullable LoadFrame loadFrame) {
        HttpTaskData taskData = HttpUtil.createHttpTaskData(
                mContext, url, params, callBack);
        return startTask(taskData, loadFrame);
    }

    /**
     * 提交任务
     *
     * @param task 任务数据
     * @return 任务的标记，便于手动stop时引用
     */
    public <Param, ResultType> TaskGroupTag startTask(
            @NonNull TaskData<Param, ResultType> task) {
        return startTask(Collections.singletonList(task), null);
    }

    /**
     * 提交任务
     *
     * @param task      任务数据
     * @param loadFrame 任务加载监听的界面框架
     * @return 任务的标记，便于手动stop时引用
     */
    public <Param, ResultType> TaskGroupTag startTask(
            @NonNull TaskData<Param, ResultType> task,
            @Nullable LoadFrame loadFrame) {
        return startTask(Collections.singletonList(task), loadFrame);
    }

    /**
     * 提交一组任务
     *
     * @param tasks     任务（组）数据
     * @param loadFrame 任务加载监听的界面框架
     * @return 任务组的标记，便于手动stop时引用
     */
    @SuppressWarnings("unchecked")
    public TaskGroupTag startTask(
            @NonNull final List<? extends TaskData> tasks,
            @Nullable final LoadFrame loadFrame) {
        tryToConfigHttpTaskData(tasks);

        if (loadFrame != null) {
            loadFrame.showLoadingUI();

            for (TaskData taskData : tasks) {
                TaskCallBack rawCallBack = taskData.callBack;
                taskData.callBack = new WrappedTaskCallBack(rawCallBack) {
                    @Override
                    public void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    public void onPostExecute(Object result, Object extra, Throwable exception) {
                        if (isResultFailed(result, extra, exception)) {
                            if (loadFrame.shouldRetry(tasks)) {
                                loadFrame.setRetryAction(TaskStarter.this, tasks);
                            } else if (loadFrame.shouldFail(tasks)) {
                                loadFrame.showFailedUI();
                            }
                        } else {
                            loadFrame.hide();
                        }

                        super.onPostExecute(result, extra, exception);
                    }

                    @Override
                    public void onCancelled(Object result, Object extra, Throwable exception) {
                        loadFrame.hide();
                        super.onCancelled(result, extra, exception);
                    }
                };
            }
        }

        return mTaskManager.start(tasks, null);
    }


    /**
     * 提交http任务，并以弹窗阻塞界面其它操作，直到任务执行完毕，或者按回退取消。
     *
     * @param url
     * @param params
     * @param callBack
     * @param <T>
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startTaskWithDialog(
            @NonNull String url, @Nullable HttpParams params,
            @NonNull HttpCallBack<T> callBack) {
        return startTaskWithDialog(url, params, callBack, null);
    }

    /**
     * 提交http任务，并以弹窗阻塞界面其它操作，直到任务执行完毕，或者按回退取消。
     *
     * @param url
     * @param params
     * @param callBack
     * @param dialogMsg 弹窗上的消息提示
     * @param <T>
     * @return 任务的标记，便于手动stop时引用
     */
    public <T> TaskGroupTag startTaskWithDialog(
            @NonNull String url, @Nullable HttpParams params,
            @NonNull HttpCallBack<T> callBack, @Nullable String dialogMsg) {
        TaskData<HttpRequestObject, HttpResponseObject> taskData = HttpUtil.createHttpTaskData(
                mContext, url, params, callBack);
        return startTaskWithDialog(taskData, dialogMsg);
    }

    /**
     * 提交任务，并以弹窗阻塞界面其它操作，直到任务执行完毕，或者按回退取消。
     *
     * @param task
     * @param dialogMsg 弹窗上的消息提示
     * @return 任务的标记，便于手动stop时引用
     */
    public <Param, ResultType> TaskGroupTag startTaskWithDialog(
            @NonNull TaskData<Param, ResultType> task, @Nullable String dialogMsg) {
        return startTaskWithDialog(Collections.singletonList(task), dialogMsg);
    }

    /**
     * 提交一组任务，并以弹窗阻塞界面其它操作，直到所有任务执行完毕，或者按回退取消。
     * 如果中途有一个任务返回结果指明中断（参考 {@link TaskResult#interrupt}，则后续任务不再执行。
     *
     * @param tasks
     * @param dialogMsg 弹窗上的消息提示
     * @return 任务组的标记，便于手动stop时引用
     */
    @SuppressWarnings("deprecation")
    public TaskGroupTag startTaskWithDialog(
            @NonNull List<? extends TaskData<?, ?>> tasks, @Nullable String dialogMsg) {
        if (mDialog.isShowing()) {
            return null;
        }

        if (TextUtils.isEmpty(dialogMsg)) {
            if (mContext != null) {
                dialogMsg = mContext.getString(R.string.executing);
            } else {
                dialogMsg = "";
            }
        }
        mDialog.setMessage(dialogMsg);
        if (tasks.size() > 1) {
            mDialog.setIndeterminate(false);
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        } else {
            mDialog.setIndeterminate(true);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }

        final TaskGroupTag tag = mTaskManager.start(tasks, mDialog);
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mTaskManager.stop(tag);
            }
        });

        return tag;
    }


    /**
     * @param tag 如果为null，则只要有一组任务正在执行，就返回true。否则，只查询指定的任务组
     * @return
     */
    public boolean isRunning(@Nullable TaskGroupTag tag, boolean includeRetainTask) {
        return mTaskManager.isRunning(tag, includeRetainTask);
    }

    /**
     * @param tag 如果为null，则结束关联页面上的所有任务组。否则，只结束指定的任务组
     */
    public void stopTask(TaskGroupTag tag) {
        mTaskManager.stop(tag);
    }

}
