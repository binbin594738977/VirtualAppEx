package com.weiliu.library.task;

/**
 * 执行一组LinkedTask需要用到的进度管理器。
 * Created by qumiao on 16/4/27.
 */
public interface TaskProgress {

    /**
     * 总权重
     */
    void setTotalWeight(int totalWeight);

    /**
     * 总权重
     */
    int getTotalWeight();

    /**
     * 当前进度
     */
    void setProgress(int progress);

    /**
     * 当前进度
     */
    int getProgress();

    /**
     * 显示
     */
    void show();

    /**
     * 隐藏
     */
    void hide();
}
