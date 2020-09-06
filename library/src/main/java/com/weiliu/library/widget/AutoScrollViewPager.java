package com.weiliu.library.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

/**
 * 自动轮播的ViewPage,同时支持手动切换，手动切换过程中自动轮播会停止，检测到Touch的action_up事件后继续开启轮播;
 *
 * @author wwxwwq
 */
public class AutoScrollViewPager extends WrapContentViewPager {
    public static final long DEFAULT_INTERVAL = 4 * DateUtils.SECOND_IN_MILLIS;
    public static final int SCROLL_WHAT = 1;
    private long mInterval = DEFAULT_INTERVAL;
    private boolean mIsStopScroll;
    private Handler mHandler;

    public AutoScrollViewPager(Context context) {
        super(context);
        init();
    }

    public AutoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mHandler = new MyHandler(this);
        sendScrollMessage(mInterval);
    }

    private void sendScrollMessage(long interval) {
        mHandler.removeMessages(SCROLL_WHAT);
        mHandler.sendEmptyMessageDelayed(SCROLL_WHAT, interval);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        sendScrollMessage(mInterval);
        super.setAdapter(adapter);
    }

    /**
     * 播放下一个广告；
     */
    private void scrollToNext() {
        PagerAdapter pagerAdapter = getAdapter();
        if (pagerAdapter == null) {
            return;
        }
        int totalCount = pagerAdapter.getCount();
        if (totalCount <= 1) {
            return;
        }
        int currentItem = getCurrentItem();
        int nextItem = ++currentItem % totalCount;

        setCurrentItem(nextItem, true);
    }

    public long getInterval() {
        return mInterval;
    }

    public void setInterval(long interval) {
        this.mInterval = interval;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sendScrollMessage(mInterval);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(SCROLL_WHAT);
    }

    private static class MyHandler extends Handler {

        WeakReference<AutoScrollViewPager> mViewPagerRef;

        MyHandler(AutoScrollViewPager viewPager) {
            mViewPagerRef = new WeakReference<>(viewPager);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            AutoScrollViewPager pager = mViewPagerRef.get();
            if (pager == null) {
                return;
            }

            switch (msg.what) {
                case SCROLL_WHAT:
                    if (pager.isShown()) {
                        pager.scrollToNext();
                    }

                    sendEmptyMessageDelayed(SCROLL_WHAT, pager.mInterval);
                    break;

                default:
                    break;
            }
        }

    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                mIsStopScroll = true;
                mHandler.removeMessages(SCROLL_WHAT);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsStopScroll) {
                    mIsStopScroll = false;
                    mHandler.sendEmptyMessageDelayed(SCROLL_WHAT, mInterval);
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


}
