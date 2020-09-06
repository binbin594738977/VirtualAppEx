package com.weiliu.library.widget;

import android.graphics.Canvas;
import android.view.View;

/**
 * 参考{@link Canvas#clipPath(android.graphics.Path)}
 * Created by qumiao on 2015/5/8.
 */
public interface ClipPathCallBack {
    /**
     * 参考{@link Canvas#clipPath(android.graphics.Path)}
     * @param view
     * @param canvas
     */
    void clipPath(View view, Canvas canvas);
}
