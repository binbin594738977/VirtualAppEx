package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.weiliu.library.R;

/**
 * 该Layout是{@linkplain HeaderScrollView}的直接子View。
 * 其本身只容纳两个子View：一个header view和一个可以滚动的View（如ScrollView、ListView等）。
 * 它可以达到如下布局效果：
 *
 * 		<blockquote>
 * 		1. header view显示在顶端，并以其自有的layout_width和layout_height决定大小；<br/>
 *
 * 		2. 可滚动View显示在header view的下方，并且当其layout_height指定为match_parent的时候，
 * 		高度等于FrameLayout的实际高度，而非减去header view的高度。
 * 		</blockquote>
 *
 * @author qumiao
 *
 */
public class HeaderScrollContentLayout extends FrameLayout {

    private int mExtraHeaderOffset;

    public HeaderScrollContentLayout(Context context) {
        super(context);
    }

    public HeaderScrollContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HeaderScrollContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attributeSet) {
        if (attributeSet != null) {
            TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.HeaderScrollContentLayout);
            mExtraHeaderOffset = a.getDimensionPixelSize(R.styleable.HeaderScrollContentLayout_extraOffset, 0);
            a.recycle();
        }
    }

    public void setExtraHeaderOffset(int extraOffset) {
        mExtraHeaderOffset = extraOffset;
        requestLayout();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getChildCount() == 0) {
            return;
        }

        /*
         * 因为HeaderScrollContentLayout是以MATCH_PARENT形式被加到HeaderScrollView中的，
         * 所以此处可以获取到HeaderScrollView的整个区域大小的高宽
         */
        int widthSpec = ViewCompat.getMeasuredWidthAndState(this);

        ViewGroup parent = (ViewGroup) getParent();
        int height = parent.getMeasuredHeight();
        height -= parent.getPaddingTop();
        height -= parent.getPaddingBottom();

        // header view要多高就显示多高
        measureChild(getHeaderView(), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST));

        int offset = getHeaderOffset();

        //根据布局效果说明，最终高度应该额外加上header View的高度和margin
        int heightSpec = MeasureSpec.makeMeasureSpec(height + offset - mExtraHeaderOffset,
                MeasureSpec.EXACTLY);

        setMeasuredDimension(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));

        View scrollableView = getScrollableView();
        if (scrollableView == null) {
            return;
        }

        // scrollable view就撑到整个ScrollView区域的高度
        measureChild(scrollableView, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightSpec) - offset, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (getChildCount() == 0) {
            return;
        }

        View scrollableView = getScrollableView();
        if (scrollableView == null) {
            return;
        }

        int l = scrollableView.getLeft();
        int r = scrollableView.getRight();
        int t = scrollableView.getTop();
        int b = scrollableView.getBottom();

        int offset = getHeaderOffset();
        //将可滚动View的位置偏移至header view的下方
        scrollableView.layout(l, t + offset, r, b + offset);
    }

    private int getHeaderOffset() {
        View headerView = getHeaderView();
        if (headerView == null) {
            return 0;
        }

        LayoutParams p = (LayoutParams) headerView.getLayoutParams();
        return p.topMargin + headerView.getMeasuredHeight() + p.bottomMargin;
    }

    @Nullable
    public View getHeaderView() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Nullable
    public View getScrollableView() {
        return getChildCount() > 1 ? getChildAt(1) : null;
    }

}
