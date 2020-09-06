package com.weiliu.library.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

/**
 * 继承于系统ScrollView
 * 
 * 主要解决系统ScrollView自动滚动到focus子View的问题，改为若子View已有屏幕内，则不滚动
 *
 */
public class ListScrollView extends android.widget.ScrollView {
    
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
    public ListScrollView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        init(context);
    }
    /**
     * constructor
     * @param context context
     * @param attrs attrs
     */
    public ListScrollView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        
        init(context);
    }
    /**
     * constructor
     * @param context context
     */
    public ListScrollView(@NonNull Context context) {
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
     * 修改为 如果 child view 已经在屏幕中可见，则不滚动。如果在屏幕外，则滚动
     * {@inheritDoc}
     */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(@NonNull Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        int fadingEdge = getVerticalFadingEdgeLength();

        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // leave room for bottom fading edge as long as rect isn't at very bottom
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.top > screenBottom && rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.bottom < screenTop && rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }     
}
