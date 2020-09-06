package com.weiliu.library.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * 根据Path裁剪成任何形状的Image
 */
public class ClipPathLinearLayout extends LinearLayout {

	private ClipPathCallBack mClipPathCallBack;

	public ClipPathLinearLayout(Context context) {
		super(context);
	}

	public ClipPathLinearLayout(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public ClipPathLinearLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
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