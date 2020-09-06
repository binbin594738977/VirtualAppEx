package io.virtualapp.core;

import com.weiliu.library.task.http.UrlParams;

import io.virtualapp.BuildConfig;

/**
 * 作者：fuheng
 * 日期：2017/9/11 10:48
 * 说明：
 */
public class BaseUrlParams extends UrlParams {

    public BaseUrlParams(String urlType, String urlEvent) {
        String url = BuildConfig.API_HOST;
        setUrl(url);

        BaseParams params = new BaseParams(urlType, urlEvent);
        setParams(params);
    }

    @Override
    public BaseParams getParams() {
        return (BaseParams) super.getParams();
    }
}
