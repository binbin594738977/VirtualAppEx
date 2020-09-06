package com.weiliu.library.task.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weiliu.library.json.JsonInterface;
import com.weiliu.library.task.TaskProgressDeliver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：qumiao
 * 日期：2017/4/20 17:31
 * 说明：POST多段数据
 */

public class MultiParts implements JsonInterface, Serializable {
    /**分隔线字串*/
    private String boundary = "**MultiPartsBoundary**";
    /**参数列表*/
    private ArrayList<Part> parts = new ArrayList<>();


    private static final String NEW_LINE = "\r\n";
    private static final String START_BOUNDARY = "--";
    private static final String END_BOUNDARY = "--";

    /**
     * 获取分隔线字串
     * @return
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * 设置分隔线字串
     * @param boundary
     */
    public MultiParts setBoundary(@NonNull String boundary) {
        this.boundary = boundary;
        return this;
    }

    /**
     * 参数列表
     * @return
     */
    public List<Part> getParts() {
        return parts;
    }

    public MultiParts put(@NonNull String name, @Nullable String value) {
        Part part;
        if (value == null) {
            part = null;
        } else {
            part = new Part();
            part.data = value.getBytes();
            part.dataOff = 0;
            part.dataCount = part.data.length;
        }
        put(name, part);
        return this;
    }

    public MultiParts put(@NonNull String name, @NonNull String fileName, @NonNull String contentType,
                          @NonNull byte[] data) {
        Part part = new Part();
        part.fileName = fileName;
        part.contentType = contentType;
        part.data = data;
        part.dataOff = 0;
        part.dataCount = data.length;
        put(name, part);
        return this;
    }

    public MultiParts put(@NonNull String name, @NonNull String fileName, @NonNull String contentType,
                          @NonNull byte[] data, int off, int count) {
        Part part = new Part();
        part.fileName = fileName;
        part.contentType = contentType;
        part.data = data;
        part.dataOff = off;
        part.dataCount = count;
        put(name, part);
        return this;
    }

    public MultiParts put(@NonNull String name, @NonNull String fileName, @NonNull String contentType, @NonNull File file) {
        Part part = new Part();
        part.fileName = fileName;
        part.contentType = contentType;
        part.file = file;
        put(name, part);
        return this;
    }

    private void put(@NonNull String name, @Nullable Part part) {
        int i = 0;
        int index = -1;
        for (Part temp : parts) {
            if (temp.name.equals(name)) {
                index = i;
                break;
            }
            i++;
        }

        if (part == null) {
            if (index != -1) {
                parts.remove(index);
            }
        } else {
            part.name = name;
            if (index != -1) {
                parts.set(index, part);
            } else {
                parts.add(part);
            }
        }
    }

    public long getLength() {
        final long[] length = new long[1];
        Callback callback = new Callback() {
            @Override
            public void scanStr(String str) {
                length[0] += str.getBytes().length;
            }

            @Override
            public void scanData(byte[] data, int off, int count) {
                length[0] += count;
            }

            @Override
            public void scanFile(File file) {
                length[0] += file.length();
            }
        };

        try {
            scan(callback);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return length[0];
    }

    public void writeTo(@NonNull OutputStream output, final long total,
                        @Nullable final TaskProgressDeliver progressDeliver) throws IOException {
        final DataOutputStream dataOutputStream = new DataOutputStream(output);
        final long[] length = new long[1];
        scan(new Callback() {
            @Override
            public void scanStr(String str) throws IOException {
                dataOutputStream.write(str.getBytes());
                length[0] += str.getBytes().length;
            }

            @Override
            public void scanData(byte[] data, int off, int count) throws IOException {
                dataOutputStream.write(data, off, count);
                length[0] += count;
            }

            @Override
            public void scanFile(File file) throws IOException {
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[16 * 1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        //文件的IO操作无视Thread的interrupt（不产生IOException），所以还是自己来吧
                        if (Thread.interrupted()) {
                            throw new IOException("task cancel");
                        }

                        dataOutputStream.write(buffer, 0, len);
                        length[0] += len;
                        if (progressDeliver != null) {
                            progressDeliver.publishProgress((int) (length[0] * 100 / total), 0);
                        }
                    }
                } finally {
                    Utility.close(inputStream);
                }
            }
        });
    }

    private void scan(Callback callback) throws IOException {
        for (Part part : parts) {
            callback.scanStr(START_BOUNDARY + boundary + NEW_LINE);
            callback.scanStr("Content-Disposition: form-data; name=\"" + part.name + '\"');
            if (!TextUtils.isEmpty(part.fileName)) {
                callback.scanStr("; filename=\"" +  part.fileName + '\"' + NEW_LINE);
                String type = !TextUtils.isEmpty(part.contentType) ? part.contentType : "application/octet-stream";
                callback.scanStr("Content-Type: " + type + NEW_LINE);
            } else {
                callback.scanStr(NEW_LINE);
            }
            callback.scanStr(NEW_LINE);
            if (part.data != null && part.dataCount > 0) {
                callback.scanData(part.data, part.dataOff, part.dataCount);
            } else if (part.file != null) {
                callback.scanFile(part.file);
            }
            callback.scanStr(NEW_LINE);
        }
        callback.scanStr(START_BOUNDARY + boundary + END_BOUNDARY + NEW_LINE);
    }

    private interface Callback {
        void scanStr(String str) throws IOException;
        void scanData(byte[] data, int off, int count) throws IOException;
        void scanFile(File file) throws IOException;
    }

    private static class Part implements JsonInterface, Serializable {
        /**参数名*/
        String name;
        /**服务端保存的文件名*/
        String fileName;
        /**数据类型，如 image/png */
        String contentType;

        /**数据内容*/
        byte[] data;
        int dataOff;
        int dataCount;


        /**数据内容的文件*/
        File file;
    }
}
