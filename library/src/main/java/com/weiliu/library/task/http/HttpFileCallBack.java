package com.weiliu.library.task.http;

import android.support.annotation.Nullable;

import java.io.File;

/**
 * 作者：qumiao
 * 日期：2017/4/21 9:46
 * 说明：http任务请求回调，响应结果为文件
 */
public abstract class HttpFileCallBack extends HttpCallBack<File> {

    @Override
    public void previewCache(File resultData) {

    }

    @Override
    protected File getResultBinaryData(HttpBinaryChannel channel, boolean isCache) throws Exception {
        if (channel instanceof HttpFileChannel) {
            return ((HttpFileChannel) channel).getFile();
        }
        return null;
    }

    @Override
    public void failed(@Nullable File resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {

    }
}
