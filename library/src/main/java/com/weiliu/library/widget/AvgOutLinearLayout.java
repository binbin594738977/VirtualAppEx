package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.weiliu.library.R;


/**
 * 可以配合{@linkplain AvgOutHorizontalScrollView}，实现既能等分、又能超出屏幕滚动的布局效果。
 * Created by qumiaowin on 2016/8/4.
 */
public class AvgOutLinearLayout extends LinearLayout {

    private float mWeightSum = 1f;

    public AvgOutLinearLayout(Context context) {
        super(context);
        init(context, null);
    }

    public AvgOutLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AvgOutLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        if (attributeSet != null) {
            TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.AvgOutLinearLayout);
            mWeightSum = a.getFloat(R.styleable.AvgOutLinearLayout_weightSum, mWeightSum);
            a.recycle();
        }
    }

    public void setWeightSum(float weightSum) {
        if (mWeightSum == weightSum) {
            return;
        }
        mWeightSum = weightSum;
        requestLayout();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int count = getChildCount();
        if (count == 0) {
            return;
        }

        /*
        AvgOutLinearLayout是以MATCH_PARENT形式加入AvgOutHorizontalScrollView的，
        所以经过默认的measure后，此处可以获取高宽了
         */
        int width = MeasureSpec.getSize(ViewCompat.getMeasuredWidthAndState(this));
        int height = MeasureSpec.getSize(ViewCompat.getMeasuredHeightAndState(this));

        int avgWidth = (int) ((width - getPaddingLeft() - getPaddingRight()) / mWeightSum);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            // 等分
            child.measure(MeasureSpec.makeMeasureSpec(
                    avgWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY),
                    ViewCompat.getMeasuredHeightAndState(child));
        }

        setMeasuredDimension(avgWidth * count + getPaddingLeft() + getPaddingRight(), height);
    }
}
