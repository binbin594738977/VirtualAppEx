package com.weiliu.library.task.http;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

/**
 * 作者：qumiao
 * 日期：2017/4/21 9:47
 * 说明：Bitmap数据管理通道
 */
public class HttpBitmapChannel extends HttpByteArrayChannel {

    @Nullable
    public Bitmap getBitmap() {
        byte[] data = getByteArray();
        if (data != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }

}
