package com.weiliu.library.task.http;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

/**
 * 作者：qumiao
 * 日期：2017/4/21 9:46
 * 说明：http任务请求回调，响应结果为Bitmap
 */
public abstract class HttpBitmapCallBack extends HttpCallBack<Bitmap> {

    @Override
    public void previewCache(Bitmap resultData) {

    }

    @Override
    protected Bitmap getResultBinaryData(HttpBinaryChannel channel, boolean isCache) throws Exception {
        if (channel instanceof HttpBitmapChannel) {
            return ((HttpBitmapChannel) channel).getBitmap();
        }
        return null;
    }

    @Override
    public void failed(@Nullable Bitmap resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {

    }
}
