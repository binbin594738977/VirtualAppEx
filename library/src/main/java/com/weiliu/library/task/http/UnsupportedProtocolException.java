package com.weiliu.library.task.http;

/**
 *
 * Created by qumiao on 2016/5/3.
 */
public class UnsupportedProtocolException extends Exception {

    public UnsupportedProtocolException(String detailMessage) {
        super(detailMessage);
    }

    public UnsupportedProtocolException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnsupportedProtocolException(Throwable throwable) {
        super(throwable);
    }
}
