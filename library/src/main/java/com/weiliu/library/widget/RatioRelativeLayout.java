package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.weiliu.library.R;

/**
 * 根据高宽比来决定高度的RelativeLayout
 * @author qumiao
 *
 */
public class RatioRelativeLayout extends RoundRectRelativeLayout {

	/**高宽比，如果为0，则按RelativeLayout原本的行为来决定高度*/
	protected float mRatio;
	
	public RatioRelativeLayout(Context context) {
		super(context);
	}

	public RatioRelativeLayout(@NonNull Context context, @NonNull AttributeSet attrs) {
		super(context, attrs);
		initFromAttributes(context, attrs);
	}

	public RatioRelativeLayout(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initFromAttributes(context, attrs);
	}
	
	private void initFromAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
		TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.RatioRelativeLayout);
		mRatio = array.getFloat(R.styleable.RatioRelativeLayout_ratio, 0);
		array.recycle();

		setRoundRadius(0);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mRatio != 0) {
			int width = View.MeasureSpec.getSize(widthMeasureSpec);
			int height = (int) (width * mRatio);
			heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	/**
	 * 设置高宽比。
	 * @param ratio 如果为0，则按RelativeLayout原本的行为来决定高度；否则，高度等于宽度乘以这个值。
	 */
	public void setRatio(float ratio) {
		mRatio = ratio;
		requestLayout();
	}

}
