package com.weiliu.library.task;

import java.util.List;

/**
 * 任务加载监听的界面框架
 * <br/>
 * Created by qumiao on 2016/8/22.
 */
public interface LoadFrame {

    /**
     * 展示加载中
     */
    void showLoadingUI();

    /**
     * 展示加载失败（如果{@linkplain #shouldRetry(List)}返回false，且{@linkplain #shouldFail(List)}）
     */
    void showFailedUI();

    /**
     * 展示加载重试（如果{@linkplain #shouldRetry(List)}返回true）
     */
    void showRetryUI();

    /**
     * 隐藏
     */
    void hide();

    /**
     * 在任务执行失败的情况下，是否应该展示失败界面
     * @param taskDataList 刚执行失败的任务（组）
     * @return
     */
    boolean shouldFail(List<? extends TaskData> taskDataList);

    /**
     * 在任务执行失败的情况下，是否应该展示重试界面
     * @param taskDataList 刚执行失败的任务（组）
     * @return
     */
    boolean shouldRetry(List<? extends TaskData> taskDataList);

    /**
     * 在{@linkplain #shouldRetry(List)}返回true的情况下，设置重试需要的任务启动器和数据，以便后续的重试执行。
     * @param taskStarter 任务启动器
     * @param taskDataList 待重试的任务（组）
     */
    void setRetryAction(TaskStarter taskStarter,
                        List<? extends TaskData> taskDataList);

    /**
     * 执行重试后的回调
     * @param retryTaskTag 重试任务生成的新tag
     */
    void onRetry(TaskGroupTag retryTaskTag);
}
