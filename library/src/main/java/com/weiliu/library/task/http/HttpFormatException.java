package com.weiliu.library.task.http;

import java.io.IOException;

/**
 * 响应数据解析错误
 * Created by qumiao on 2016/7/27.
 */
public class HttpFormatException extends IOException {

    public HttpFormatException() {
        super();
    }

    public HttpFormatException(String detailMessage) {
        super(detailMessage);
    }

    public HttpFormatException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HttpFormatException(Throwable throwable) {
        super(throwable);
    }
}
