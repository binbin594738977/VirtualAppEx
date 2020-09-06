package com.weiliu.library.task.http;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 作者：qumiao
 * 日期：2017/4/21 9:47
 * 说明：Bitmap数据管理通道
 */
public class HttpFileChannel implements HttpBinaryChannel {
    private File mFile;

    public HttpFileChannel(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    @NonNull
    @Override
    public OutputStream createOutputStream() throws IOException {
        return new FileOutputStream(mFile);
    }

    @NonNull
    @Override
    public InputStream createInputStream() throws IOException {
        return new FileInputStream(mFile);
    }
}
