package com.weiliu.library.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * 根据Path裁剪成任何形状的Image
 */
public class ClipPathRelativeLayout extends RelativeLayout {

	private ClipPathCallBack mClipPathCallBack;

	public ClipPathRelativeLayout(@NonNull Context context) {
		super(context);
	}

	public ClipPathRelativeLayout(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ClipPathRelativeLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		canvas.save();
		if (mClipPathCallBack != null) {
			mClipPathCallBack.clipPath(this, canvas);
		}
		super.draw(canvas);
		canvas.restore();
	}

	@Override
	protected void dispatchDraw(@NonNull Canvas canvas) {
		canvas.save();
		if (mClipPathCallBack != null) {
			mClipPathCallBack.clipPath(this, canvas);
		}
		super.dispatchDraw(canvas);
		canvas.restore();
	}

	public final void setClipPathCallBack(ClipPathCallBack clipPathCallBack) {
		mClipPathCallBack = clipPathCallBack;
		invalidate();
	}
}