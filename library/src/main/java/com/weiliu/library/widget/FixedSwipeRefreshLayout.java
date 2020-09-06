package com.weiliu.library.widget;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

/**
 * 修复setRefreshing可能造成冲突的问题。
 * 修复与NestScrollView的滚动冲突的问题。
 * Created by qumiaowin on 2016/6/16.
 */
public class FixedSwipeRefreshLayout extends SwipeRefreshLayout {

    public FixedSwipeRefreshLayout(Context context) {
        super(context);
    }

    public FixedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Runnable mCurrentAction;

    /**
     * 异步执行setRefreshing。
     * 为了避免在手动下拉触发refresh时直接调用setRefreshing，造成下拉被cancel 等问题
     */
    public void setRefreshing(final boolean refreshing) {
        if (refreshing && !isEnabled()) {
            return;
        }

        if (mCurrentAction != null) {
            removeCallbacks(mCurrentAction);
        }

        mCurrentAction = new Runnable() {
            @Override
            public void run() {
                FixedSwipeRefreshLayout.super.setRefreshing(refreshing);
            }
        };
        postDelayed(mCurrentAction, 50);
    }


//    private boolean mCanChildScrollUpCache;
//    private boolean mCanceledWhenActionDown;
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        final int action = ev.getActionMasked();
//        if (action == MotionEvent.ACTION_DOWN) {
//            mCanceledWhenActionDown = false;    //init
//        }
//        boolean ret = super.onInterceptTouchEvent(ev);
//
//        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
//            mCanceledWhenActionDown = false;    //reset
//        } else if (!ret) {
//            if (mCanChildScrollUpCache) {
//                mCanceledWhenActionDown = true;
//            }
//        }
//
//        return ret;
//    }
//
//    @Override
//    public boolean canChildScrollUp() {
//        boolean ret = false;
//        if (super.canChildScrollUp()) {
//            ret = true;
//        } else if (mCanceledWhenActionDown) {
//            ret = true;
//        } else {
//            View current = this;
//            while (current.getParent() instanceof View) {
//                View p = (View) current.getParent();
//                if (ViewUtil.canScrollVertically(p, -1)) {
//                    ret = true;
//                    break;
//                }
//                current = p;
//            }
//        }
//
//        mCanChildScrollUpCache = ret;
//        return ret;
//    }
}
