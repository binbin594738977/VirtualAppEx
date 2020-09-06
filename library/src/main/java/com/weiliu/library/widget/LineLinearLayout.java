package com.weiliu.library.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.weiliu.library.R;

/**
 * 在每个子View之间划线
 * Created by qumiao on 2015/5/20.
 */
public class LineLinearLayout extends LinearLayout {
    @NonNull
    private Paint mPaint = new Paint();
    private int mLineColor;
    private int mLineSize;

    public LineLinearLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public LineLinearLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public LineLinearLayout(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LineLinearLayout(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.LineLinearLayout,
                    0, 0);
            mLineColor = a.getColor(R.styleable.LineLinearLayout_lineColor, mLineColor);
            mLineSize = a.getDimensionPixelSize(R.styleable.LineLinearLayout_lineSize, mLineSize);
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mLineSize == 0 || mLineColor == Color.TRANSPARENT) {
            return;
        }

        mPaint.setColor(mLineColor);
        mPaint.setStyle(Paint.Style.FILL);

        int left;
        int top;
        int right;
        int bottom;
        int orientation = getOrientation();
        int width = getWidth();
        int height = getHeight();
        int count = getChildCount();
        for (int i = 0; i < count - 1; i++) {
            View child = getChildAt(i);
            if (child == null) {
                continue;
            }

            if (orientation == LineLinearLayout.HORIZONTAL) {
                left = child.getRight() - mLineSize;
                top = getPaddingTop();
                right = left + mLineSize;
                bottom = height - getPaddingBottom();
            } else {
                left = getPaddingLeft();
                top = child.getBottom() - mLineSize;
                right = width - getPaddingRight();
                bottom = top + mLineSize;
            }

            canvas.drawRect(left, top, right, bottom, mPaint);
        }
    }

    public int getLineColor() {
        return mLineColor;
    }

    public void setLineColor(int lineColor) {
        mLineColor = lineColor;
        invalidate();
    }

    public int getLineSize() {
        return mLineSize;
    }

    public void setLineSize(int lineSize) {
        mLineSize = lineSize;
        invalidate();
    }
}
