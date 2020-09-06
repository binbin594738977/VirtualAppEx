package com.weiliu.library.task.http;

import android.support.annotation.Nullable;

import com.weiliu.library.json.JsonVoid;


/**
 * 没有具体数据返回的 http任务回调（如提交操作，只有成功与否）
 * Created by qumiao on 2016/5/26.
 */
public class HttpCallBackNoResult extends HttpCallBack<JsonVoid> {

    @Override
    public void previewCache(JsonVoid resultData) {
        // No cache
    }

    @Override
    public void success(JsonVoid resultData, @Nullable String info) {

    }

    @Override
    protected boolean acceptNullResult() {
        return true;
    }

    @Override
    public void failed(@Nullable JsonVoid resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {

    }
}
