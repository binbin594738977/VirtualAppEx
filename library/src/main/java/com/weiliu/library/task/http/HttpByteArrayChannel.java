package com.weiliu.library.task.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 作者：qumiao
 * 日期：2017/11/10 23:35
 * 说明：
 */
public class HttpByteArrayChannel implements HttpBinaryChannel {
    private ByteArrayOutputStream mByteArrayOutputStream;

    @Nullable
    public byte[] getByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = mByteArrayOutputStream;
        if (byteArrayOutputStream != null) {
            return byteArrayOutputStream.toByteArray();
        }
        return null;
    }

    @NonNull
    @Override
    public OutputStream createOutputStream() {
        mByteArrayOutputStream = new ByteArrayOutputStream();
        return mByteArrayOutputStream;
    }

    @NonNull
    @Override
    public InputStream createInputStream() {
        ByteArrayOutputStream byteArrayOutputStream = mByteArrayOutputStream;
        byte[] data = byteArrayOutputStream != null ? byteArrayOutputStream.toByteArray() : new byte[0];
        return new ByteArrayInputStream(data);
    }

}
