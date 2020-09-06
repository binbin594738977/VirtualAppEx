package com.weiliu.library.task.http;

import android.support.annotation.Nullable;

/**
 * 作者：qumiao
 * 日期：2017/11/10 23:39
 * 说明：
 */
public abstract class HttpByteArrayCallBack extends HttpCallBack<byte[]> {
    @Override
    public void previewCache(byte[] resultData) {

    }

    @Override
    protected byte[] getResultBinaryData(HttpBinaryChannel channel, boolean isCache) throws Exception {
        if (channel instanceof HttpByteArrayChannel) {
            return ((HttpByteArrayChannel) channel).getByteArray();
        }
        return super.getResultBinaryData(channel, isCache);
    }

    @Override
    public void failed(@Nullable byte[] resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {

    }
}
