package com.weiliu.library.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * 可以指定高度为wrap_content的GridView
 * 
 * @author qumiao
 * 
 */
public class WrapContentGridView extends GridView {

    /**
     * 构造
     * 
     * @param context
     *            Context
     */
    public WrapContentGridView(@NonNull Context context) {
        super(context);
    }

    /**
     * 构造
     * 
     * @param context
     *            Context
     * @param attrs
     *            AttributeSet
     */
    public WrapContentGridView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 构造
     * 
     * @param context
     *            Context
     * @param attrs
     *            AttributeSet
     * @param defStyle
     *            default style
     */
    public WrapContentGridView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec;

        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            // The great Android "hackatlon", the love, the magic.
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                    MeasureSpec.AT_MOST);
        } else {
            // Any other height should be respected as is.
            heightSpec = heightMeasureSpec;
        }

        super.onMeasure(widthMeasureSpec, heightSpec);
    }

}
