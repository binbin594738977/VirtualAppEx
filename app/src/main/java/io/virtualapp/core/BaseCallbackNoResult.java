package io.virtualapp.core;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.weiliu.library.json.JsonVoid;
import com.weiliu.library.task.http.HttpCallBackNoResult;

/**
 * 作者：
 * 日期：2017/6/10 17:54
 * 说明：
 */
public class BaseCallbackNoResult extends HttpCallBackNoResult {


    @Override
    public void previewCache(JsonVoid resultData) {

    }

    @Override
    public void success(JsonVoid resultData, @Nullable String info) {
        if (!TextUtils.isEmpty(info)) {
            Toast.makeText(BaseApplication.app(), info, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void failed(@Nullable JsonVoid resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {
        super.failed(resultData, httpStatus, code, info, e);
        BaseCallback.handleFailedResult(resultData, httpStatus, code, info, e);
    }
}
