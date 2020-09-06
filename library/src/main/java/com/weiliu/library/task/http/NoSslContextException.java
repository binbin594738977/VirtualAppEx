package com.weiliu.library.task.http;

/**
 *
 * Created by qumiao on 2016/5/3.
 */
public class NoSslContextException extends Exception {

    public NoSslContextException(String detailMessage) {
        super(detailMessage);
    }

    public NoSslContextException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoSslContextException(Throwable throwable) {
        super(throwable);
    }
}
