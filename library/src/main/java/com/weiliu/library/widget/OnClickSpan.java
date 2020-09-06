package com.weiliu.library.widget;

import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 响应点击监听的span
 * @author qumiao
 *
 */
public class OnClickSpan extends ClickableSpan {
	
	private OnClickListener mListener;
	
	public OnClickSpan(OnClickListener listener) {
		mListener = listener;
	}

	@Override
	public void onClick(View widget) {
		if (mListener != null) {
			mListener.onClick(widget);
		}
	}

	@Override
	public void updateDrawState(@NonNull TextPaint ds) {
		ds.setColor(ds.linkColor);
		//不划线
//        ds.setUnderlineText(true);
	}

}
