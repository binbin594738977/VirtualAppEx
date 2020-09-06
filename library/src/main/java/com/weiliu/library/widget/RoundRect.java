package com.weiliu.library.widget;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.weiliu.library.R;

/**
 * 圆角处理
 * Created by qumiao on 2015/5/8.
 */
public class RoundRect {

    private View mAttachedView;

    private int mRoundRadius = -1;
    private int mRoundWidth = -1;
    private int mRoundHeight = -1;

    @NonNull
    private Path mPath = new Path();
    @NonNull
    private Rect mRect = new Rect();

    private Object ViewOutlineProvider;

    public RoundRect(@NonNull View attachedView) {
        mAttachedView = attachedView;

        if (!compatible()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mAttachedView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewOutlineProvider = new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, @NonNull Outline outline) {
                    outline.setRoundRect(getRect(), getValidRoundRadius());
                }
            };
            mAttachedView.setOutlineProvider((android.view.ViewOutlineProvider) ViewOutlineProvider);
            mAttachedView.setClipToOutline(true);
        }
    }


    public void setRoundRadius(int radius, boolean invalidateNow) {
        mRoundRadius = radius;
        if (invalidateNow) {
            mAttachedView.invalidate();
        }
    }

    public void setRoundWidth(int width, boolean invalidateNow) {
        mRoundWidth = width;
        if (invalidateNow) {
            mAttachedView.invalidate();
        }
    }

    public void setRoundHeight(int height, boolean invalidateNow) {
        mRoundHeight = height;
        if (invalidateNow) {
            mAttachedView.invalidate();
        }
    }

    public int getRoundRadius() {
        return mRoundRadius;
    }

    public int getRoundWidth() {
        return mRoundWidth;
    }

    public int getRoundHeight() {
        return mRoundHeight;
    }


    public void clipPath(@NonNull Canvas canvas) {
        if (!compatible()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        mPath.reset();
        int roundRadius = getValidRoundRadius();
        Rect rect = getRect();

        if (roundRadius == 0 && rect.left == 0 && rect.top == 0
                && rect.right == mAttachedView.getMeasuredWidth()
                && rect.bottom == mAttachedView.getMeasuredHeight()) {
            return;
        }

        mPath.addRoundRect(new RectF(rect), roundRadius, roundRadius, Path.Direction.CW);
        canvas.clipPath(mPath);
    }

    @NonNull
    private Rect getRect() {
        int width = mAttachedView.getWidth();
        int height = mAttachedView.getHeight();

        int paddingTop = mAttachedView.getPaddingTop();
        int paddingBottom = mAttachedView.getPaddingBottom();
        int paddingLeft = mAttachedView.getPaddingLeft();
        int paddingRight = mAttachedView.getPaddingRight();

        int roundWidth = getValidRoundRWidth();
        int roundHeight = getValidRoundHeight();

        int horizontalDis = (width - paddingLeft - paddingRight - roundWidth) >> 1;
        int verticalDis = (height - paddingTop - paddingBottom - roundHeight) >> 1;
        mRect.set(horizontalDis + paddingLeft, verticalDis + paddingTop,
                horizontalDis - paddingRight + roundWidth, verticalDis - paddingBottom + roundHeight);
        return mRect;
    }

    private int getValidRoundRadius() {
        return mRoundRadius != -1 ? mRoundRadius : mAttachedView.getResources()
                .getDimensionPixelSize(R.dimen.round_rect_radius);
    }

    private int getValidRoundRWidth() {
        return mRoundWidth != -1 ? mRoundWidth : mAttachedView.getWidth();
    }

    private int getValidRoundHeight() {
        return mRoundHeight != -1 ? mRoundHeight : mAttachedView.getHeight();
    }


    public boolean compatible() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

}
