package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.weiliu.library.R;

public class RoundRectRelativeLayout extends ClipPathRelativeLayout {

	private RoundRect mRoundRect;

	public RoundRectRelativeLayout(Context context) {
		super(context);
		init(context, null);
	}

	public RoundRectRelativeLayout(@NonNull Context context, @NonNull AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public RoundRectRelativeLayout(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		mRoundRect = new RoundRect(this);

		if (attrs != null && mRoundRect.compatible()) {
			Resources.Theme theme = context.getTheme();
			if (theme != null) {
				TypedArray a = theme.obtainStyledAttributes(
						attrs,
						R.styleable.RoundRectRelativeLayout,
						0, 0);
				mRoundRect.setRoundRadius(a.getDimensionPixelSize(
						R.styleable.RoundRectRelativeLayout_roundRectRadius, mRoundRect.getRoundRadius()), false);
				mRoundRect.setRoundWidth(a.getDimensionPixelSize(
						R.styleable.RoundRectRelativeLayout_roundRectWidth, mRoundRect.getRoundWidth()), false);
				mRoundRect.setRoundHeight(a.getDimensionPixelSize(
						R.styleable.RoundRectRelativeLayout_roundRectHeight, mRoundRect.getRoundHeight()), false);
			}
		}


		setClipPathCallBack(new ClipPathCallBack() {
			@Override
			public void clipPath(View view, @NonNull Canvas canvas) {
				mRoundRect.clipPath(canvas);
			}
		});
	}

	public void setRoundRadius(int radius) {
		mRoundRect.setRoundRadius(radius, true);
	}

	public void setRoundWidth(int width) {
		mRoundRect.setRoundWidth(width, true);
	}

	public void setRoundHeight(int height) {
		mRoundRect.setRoundHeight(height, true);
	}
}