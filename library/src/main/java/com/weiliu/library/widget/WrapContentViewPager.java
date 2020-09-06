package com.weiliu.library.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class WrapContentViewPager extends ViewPagerCompat {

	public WrapContentViewPager(Context context) {
		super(context);
	}

	public WrapContentViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int height = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			child.measure(widthMeasureSpec, heightMeasureSpec);
			int h = child.getMeasuredHeight();
			if (h > height) {
				height = h;
			}
		}

		heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height,
				View.MeasureSpec.EXACTLY);

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * Determines the height of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @param view
	 *            the base view with already measured height
	 * 
	 * @return The height of the view, honoring constraints from measureSpec
	 */
	private int measureHeight(int measureSpec, @Nullable View view) {
		int result = 0;
		int specMode = View.MeasureSpec.getMode(measureSpec);
		int specSize = View.MeasureSpec.getSize(measureSpec);

		if (specMode == View.MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			// set the height from the base view if available
			if (view != null) {
				result = view.getMeasuredHeight();
			}
			if (specMode == View.MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

}
