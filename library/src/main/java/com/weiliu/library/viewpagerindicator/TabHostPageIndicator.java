package com.weiliu.library.viewpagerindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.weiliu.library.R;

/**
 * 一种类似TabHost形式的ViewPager切换器
 * Created by qumiao on 2015/10/19.
 */
public class TabHostPageIndicator extends LinearLayout implements PageIndicator {

    private ViewPager mViewPager;

    private int mSelectedPosition;

    private int mScrollState;
    private int mScrollPosition;
    private float mScrollPositionOffset;

    private boolean mClip;

    public TabHostPageIndicator(Context context) {
        super(context);
        init(context, null);
    }

    public TabHostPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabHostPageIndicator);
            mClip = a.getBoolean(R.styleable.TabHostPageIndicator_clip, mClip);
            a.recycle();
        }
    }

    @Override
    public void setViewPager(ViewPager view) {
        if (mViewPager == view) {
            return;
        }
//        if (mViewPager != null) {
//            mViewPager.setOnPageChangeListener(null);
//        }
        final PagerAdapter adapter = view.getAdapter();
        if (!(adapter instanceof TabHostPageAdapter)) {
            throw new IllegalStateException("ViewPager's adapter is not implement interface TabHostPageAdapter!");
        }
        mViewPager = view;
        view.addOnPageChangeListener(this);
        notifyDataSetChanged();
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mSelectedPosition = item;
        mViewPager.setCurrentItem(item);

        final int tabCount = getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = getChildAt(i);
            final boolean isSelected = (i == item);
            child.setSelected(isSelected);
        }
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        if (listener == null) {
            return;
        }

        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mViewPager.addOnPageChangeListener(listener);
    }

    @Override
    public void notifyDataSetChanged() {
        removeAllViews();
        PagerAdapter adapter = mViewPager.getAdapter();
        TabHostPageAdapter tabHostPageAdapter = (TabHostPageAdapter) adapter;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            View tab = tabHostPageAdapter.makeTabView(inflater, this, i);
            tab.setTag(R.id.vpi__tab_position, i);
            tab.setOnClickListener(mTabClickListener);
            addView(tab);
        }
        if (mSelectedPosition > count) {
            mSelectedPosition = count - 1;
        }
        setCurrentItem(mSelectedPosition);
        requestLayout();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mScrollPosition = position;
        mScrollPositionOffset = positionOffset;
        invalidate();
    }

    @Override
    public void onPageSelected(int position) {
        setCurrentItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mClip) {
            super.dispatchDraw(canvas);
            return;
        }

        canvas.save();
        canvas.clipRect(getMaskRect());
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private RectF getMaskRect() {
        int position = mScrollState == ViewPager.SCROLL_STATE_IDLE ? mSelectedPosition : mScrollPosition;
        float offset = mScrollState == ViewPager.SCROLL_STATE_IDLE ? 0 : mScrollPositionOffset;
        View curView = getChildAt(position);
        RectF rect = new RectF();
        if (position < getChildCount() - 1) {
            View nextView = getChildAt(position + 1);
            rect.left = curView.getLeft() + offset * (nextView.getLeft() - curView.getLeft());
            rect.top = curView.getTop() + offset * (nextView.getTop() - curView.getTop());
            rect.right = curView.getRight() + offset * (nextView.getRight() - curView.getRight());
            rect.bottom = curView.getBottom() + offset * (nextView.getBottom() - curView.getBottom());
        } else {
            rect.left = curView.getLeft();
            rect.top = curView.getTop();
            rect.right = curView.getRight();
            rect.bottom = curView.getBottom();
        }
        return rect;
    }


    private final OnClickListener mTabClickListener = new OnClickListener() {
        public void onClick(View view) {
            int position = (int) view.getTag(R.id.vpi__tab_position);
            mViewPager.setCurrentItem(position);
        }
    };
}
