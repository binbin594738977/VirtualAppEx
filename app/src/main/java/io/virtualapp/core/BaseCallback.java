package io.virtualapp.core;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;

import com.weiliu.library.task.http.HttpCallBack;
import com.weiliu.library.task.http.HttpFormatException;
import com.weiliu.library.task.http.HttpUtil;

import io.virtualapp.R;

/**
 * 作者：
 * 日期：2017/6/10 17:54
 * 说明：
 */
public abstract class BaseCallback<T> extends HttpCallBack<T> {

    @Override
    public void previewCache(T resultData) {

    }

    @Override
    public void failed(@Nullable T resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {
        handleFailedResult(resultData, httpStatus, code, info, e);
    }

    @Override
    public void success(T resultData, @Nullable String info) {
        if (!TextUtils.isEmpty(info)) {
            showToastOrNot(info, true);
        }
    }

    public static String handleFailedResult(@Nullable Object resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {
        return handleFailedResult(resultData, httpStatus, code, info, e, true);
    }

    public static String handleFailedResult(@Nullable Object resultData, int httpStatus, int code, @Nullable String info,
                                            @Nullable Throwable e, boolean showToast) {
        if (e != null) {
            e.printStackTrace();
        }

        String content = "";

        if (!TextUtils.isEmpty(info)) {
            if (code == 209) {
                content = info;
            } else {
                content = showToastOrNot(info, showToast);
            }
        } else {
            if (httpStatus != 200) {
                switch (httpStatus) {
                    case HttpUtil.SC_UNKNOWN_HOST:
                        content = showToastOrNot(R.string.net_error, showToast);
                        break;
                    case HttpUtil.SC_CONNECT:
                        content = showToastOrNot(R.string.connect_error, showToast);
                        break;
                    case HttpUtil.SC_TIME_OUT:
                        content = showToastOrNot(R.string.net_time_out, showToast);
                        break;

                    default:
                        if (e instanceof HttpFormatException) {
                            content = showToastOrNot(e.getMessage(), showToast);
                        } else {
                            content = showToastOrNot(R.string.unknown_error, showToast);
                        }
                        break;
                }
            }
        }

        if (httpStatus == 200) {
            switch (code) {
                case 403:
                    break;
                default:
                    break;
            }
        }
        return content;
    }

    private static String showToastOrNot(String content, boolean showToast) {
        if (showToast) {
            Toast.makeText(BaseApplication.app(), content, Toast.LENGTH_SHORT).show();
        }
        return content;
    }

    private static String showToastOrNot(@StringRes int stringRes, boolean showToast) {
        Context context = BaseApplication.app();
        String content = context.getString(stringRes);
        if (showToast) {
            Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
        }
        return content;
    }
}
