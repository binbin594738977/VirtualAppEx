package com.weiliu.library.viewpagerindicator;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import com.weiliu.library.R;

/**
 * UnderlinePageIndicator的扩展，拥有在滑动时指示条也跟着滑动的效果（参见mFadeRunnable）
 * @author qumiao
 *
 */
public class UnderlinePageIndicatorEx extends UnderlinePageIndicator {

    public UnderlinePageIndicatorEx(@NonNull Context context) {
        super(context);

    }

    public UnderlinePageIndicatorEx(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs, R.attr.vpiUnderlinePageIndicatorStyle);

    }

    public UnderlinePageIndicatorEx(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    public void setViewPager(@NonNull ViewPager viewPager) {
        if (mViewPager == viewPager) {
            return;
        }
        // if (mViewPager != null) {
        // //Clear us from the old pager.
        // mViewPager.setOnPageChangeListener(null);
        // }
        if (viewPager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        mViewPager = viewPager;
        // mViewPager.setOnPageChangeListener(this);
        invalidate();
        post(new Runnable() {
            @Override
            public void run() {
                if (mFades) {
                    post(mFadeRunnable);
                }
            }
        });
    }
}
