package com.weiliu.library.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import com.weiliu.library.util.ViewUtil;

/**
 * 作者：qumiao
 * 日期：2017/11/16 18:29
 * 说明：
 */
public class FixedNestedScrollView extends NestedScrollView {
    private static final String TAG = "xxx----------";

    public FixedNestedScrollView(Context context) {
        super(context);
    }

    public FixedNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(target, dx, dy, consumed);
        if (dy > 0 && ViewUtil.canScrollVertically(this, dy)) { //如果往上拖动，并且父view(即this)可以往上拖动
            final int dyUnconsumed = dy - consumed[1];
            final int oldScrollY = getScrollY();
            scrollBy(0, dyUnconsumed);
            final int myConsumed = getScrollY() - oldScrollY;
            consumed[1] += myConsumed;//能拖多少拖多少
        }
        //否则交给子view(即target)
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (velocityY > 0) {//如果往上猛拖动
            if (ViewUtil.canScrollVertically(this, (int) velocityY)) {//父view(即this)可以往上猛拖
                return tryFling(velocityX, velocityY);//给父view(即this)拖
            }
            return false;//给子view(即target)拖
        } else if (velocityY < 0) {//如果往下猛拖动
            if (ViewUtil.canScrollVertically(target, (int) velocityY)) {//子view(即target)可以往下猛拖动
                return false;//给子view(即target)拖
            }
            if (!super.onNestedPreFling(target, velocityX, velocityY)) {//如果子view(即target)拖不完
                return tryFling(velocityX, velocityY);//给父view(即this)拖
            }
        }
        return false;
    }

    boolean tryFling(float velocityX, float velocityY) {
        final int scrollY = getScrollY();
        final boolean canFling = (scrollY > 0 || velocityY > 0)
                && (scrollY < myGetScrollRange() || velocityY < 0);

        if (canFling) {
            fling((int) velocityY);
        }

        return canFling;
    }

    int myGetScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
        }
        return scrollRange;
    }


    /**
     * 重写父类函数，主要是解决，在FocusableInTouchMode模式下，scroll会对齐某个child view。
     * 修改为 如果 child view 已经在屏幕中可见，则不滚动。如果在屏幕外，则滚动。<br/>
     * {@inheritDoc}
     */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(@NonNull Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        int fadingEdge = getVerticalFadingEdgeLength();

        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // leave room for bottom fading edge as long as rect isn't at very bottom
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.top > screenBottom && rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.bottom < screenTop && rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }
}
