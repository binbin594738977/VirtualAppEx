package com.weiliu.library.task.http;

/**
 * 作者：qumiao
 * 日期：2017/9/11 10:09
 * 说明：
 */
public class UrlParams {
    private String url;
    private HttpParams params;

    public UrlParams setUrl(String url) {
        this.url = url;
        return this;
    }

    public UrlParams setParams(HttpParams params) {
        this.params = params;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public HttpParams getParams() {
        return params;
    }
}
