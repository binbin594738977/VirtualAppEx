package com.weiliu.library.util;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.URLSpan;

/**
 * <pre>
 * 作者：qumiao
 * 日期：2017/12/1 16:59
 * 说明：
 * </pre>
 */
class URLSpanNoUnderline extends URLSpan {

    public URLSpanNoUnderline(String url) {
        super(url);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);    //去掉下划线
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    protected URLSpanNoUnderline(Parcel in) {
        super(in);
    }

    public static final Creator<URLSpanNoUnderline> CREATOR = new Creator<URLSpanNoUnderline>() {
        @Override
        public URLSpanNoUnderline createFromParcel(Parcel source) {
            return new URLSpanNoUnderline(source);
        }

        @Override
        public URLSpanNoUnderline[] newArray(int size) {
            return new URLSpanNoUnderline[size];
        }
    };
}
