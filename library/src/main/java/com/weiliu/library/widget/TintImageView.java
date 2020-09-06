package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.weiliu.library.R;
import com.weiliu.library.util.DrawableUtil;

/**
 * 向下兼容ImageView的tint属性
 * <br/>
 * Created by qumiao on 2016/10/8.
 */
public class TintImageView extends AppCompatImageView {
    public TintImageView(Context context) {
        super(context);
        init(context, null);
    }

    public TintImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TintImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            Drawable drawable = getDrawable();
            if (drawable != null) {
                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TintImageView);
                ColorStateList tint = a.getColorStateList(R.styleable.TintImageView_tint);
                setImageDrawableWithTint(drawable, tint);
                a.recycle();
            }
        }
    }

    public void setImageDrawableWithTint(Drawable drawable, ColorStateList tint) {
        setImageDrawable(DrawableUtil.tintDrawable(drawable, tint));
    }
}
