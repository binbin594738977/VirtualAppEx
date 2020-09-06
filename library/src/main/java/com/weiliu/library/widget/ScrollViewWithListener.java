package com.weiliu.library.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * 带滚动监听的ScrollView,原生的ScrollView没有监听的回调
 */
public class ScrollViewWithListener extends ScrollView {

    private OnScrollListener mOnScrollListener;

    public ScrollViewWithListener(@NonNull Context context) {
        super(context);
    }

    public ScrollViewWithListener(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewWithListener(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollChanging(l, t, oldl, oldt);
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public interface OnScrollListener {
        void onScrollChanging(int x, int y, int oldx, int oldy);
    }
}
