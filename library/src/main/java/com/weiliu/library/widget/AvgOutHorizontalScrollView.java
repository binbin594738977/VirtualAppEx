package com.weiliu.library.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

/**
 * 可以配合{@linkplain AvgOutLinearLayout}，实现既能等分、又能超出屏幕滚动的布局效果
 * Created by qumiaowin on 2016/8/4.
 */
public class AvgOutHorizontalScrollView extends HorizontalScrollView {
    public AvgOutHorizontalScrollView(Context context) {
        super(context);
    }

    public AvgOutHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AvgOutHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 父类（ScrollView）中会强制指定子View的MeasureSpec为UNSPECIFIED：
     * 参考{@link HorizontalScrollView#measureChild(View, int, int)}，其原意是子View想显示多高就显示多高。<br/>
     *
     * 这样会导致HeaderFrameLayout无法获取父View的整个区域大小，
     * 从而无法达到特殊的布局效果（其子View Scrollable无法撑开到整个区域大小）。<br/>
     *
     * 所以此处改成最原始的ViewGroup里的measureChild形式（参考{@link ViewGroup#measureChild(View, int, int)}：
     * 即LayoutParam height为MATCH_PARENT时，就按MATCH_PARENT处理，以便撑到整个区域大小。<br/><br/>
     *
     * {@inheritDoc}
     */
    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop()
                + getPaddingBottom(), lp.height);

        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(parentWidthMeasureSpec), MeasureSpec.EXACTLY);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * 父类（ScrollView）中会强制指定子View的MeasureSpec为UNSPECIFIED：
     * 参考{@link HorizontalScrollView#measureChildWithMargins(View, int, int, int, int)}，其原意是子View想显示多高就显示多高。<br/>
     *
     * 这样会导致HeaderFrameLayout无法获取父View的整个区域大小，
     * 从而无法达到特殊的布局效果（其子View Scrollable无法撑开到整个区域大小）。<br/>
     *
     * 所以此处改成最原始的ViewGroup里的measureChild形式（参考{@link ViewGroup#measureChildWithMargins(View, int, int, int, int)}：
     * 即LayoutParam height为MATCH_PARENT时，就按MATCH_PARENT处理，以便撑到整个区域大小。<br/><br/>
     *
     * {@inheritDoc}
     */
    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(parentWidthMeasureSpec), MeasureSpec.EXACTLY);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    private OnScrollChangedListener mOnScrollChangedListener;

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        mOnScrollChangedListener = onScrollChangedListener;
    }

    public interface OnScrollChangedListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }
}
