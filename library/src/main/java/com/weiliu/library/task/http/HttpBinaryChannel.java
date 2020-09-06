package com.weiliu.library.task.http;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 作者：qumiao
 * 日期：2017/4/20 21:36
 * 说明：二进制形式的http响应数据管理通道
 */
public interface HttpBinaryChannel {
    /**
     * 创建输出流。请保证每次创建的都是新的输出流，因为在使用时可能会close。
     * @return
     */
    @NonNull
    OutputStream createOutputStream() throws IOException;

    /**
     * 创建输入流。请保证每次创建的都是新的输入流，因为在使用时可能会close。
     * @return
     */
    @NonNull
    InputStream createInputStream() throws IOException;
}
