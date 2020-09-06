package com.weiliu.library.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

/**
 * 继承于系统ScrollView
 * 
 * 主要解决系统ScrollView自动滚动到focus子View的问题
 *
 */
public class FixedScrollView extends android.widget.ScrollView {

    /** tag.*/
	//private static final String TAG = ScrollView.class.getSimpleName();

	/** debug switch.*/
	//private static final boolean DEBUG = false;

	/**
	 * constructor
	 * @param context context
	 * @param attrs attrs
	 * @param defStyle defStyle
	 */
    public FixedScrollView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }
    /**
     * constructor
     * @param context context
     * @param attrs attrs
     */
    public FixedScrollView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }
    /**
     * constructor
     * @param context context
     */
    public FixedScrollView(@NonNull Context context) {
        super(context);
        
        init(context);
    }
    
    /**
     * init
     * @param context context
     */
    private void init(Context context) {
        //空函数
    }
    
    /**
     * 重写父类函数，主要是解决，在FocusableInTouchMode模式下，scroll会对齐某个child view。
     * 修改为 一律不滚动
     * {@inheritDoc}
     */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(@NonNull Rect rect) {
        return 0;
    }
}
