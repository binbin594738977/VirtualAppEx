package com.weiliu.library.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

/**
 * 根据Path裁剪成任何形状的Image
 */
public class ClipPathImageView extends android.support.v7.widget.AppCompatImageView {

	private ClipPathCallBack mClipPathCallBack;

	public ClipPathImageView(Context context) {
		super(context);
	}

	public ClipPathImageView(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ClipPathImageView(@NonNull Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		canvas.save();
		if (mClipPathCallBack != null) {
			mClipPathCallBack.clipPath(this, canvas);
		}
		super.onDraw(canvas);
		canvas.restore();
	}

	public final void setClipPathCallBack(ClipPathCallBack clipPathCallBack) {
		mClipPathCallBack = clipPathCallBack;
		invalidate();
	}
}