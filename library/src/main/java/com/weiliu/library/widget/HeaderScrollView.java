package com.weiliu.library.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.weiliu.library.util.ViewUtil;

/**
 * 该ScrollView只有个子View，而且只能是{@linkplain HeaderScrollContentLayout}。<br/>
 * 该Layout能实现如下效果：
 * 		<blockquote>
 * 		1. 当其往上拖时，先整体滚动，直至header完全隐藏，再继续往上拖时，事件完全交给可滚动的子View。<br/>
 * 		2. 当其往下拖时，先将事件交给可滚动的子View，直至其不再滚动，再继续往下拖时，整体向下滚动拉出header。
 * 		</blockquote>
 * @author qumiao
 *
 */
public class HeaderScrollView extends ScrollViewWithListener {
	
	/**为了避免触摸事件判断太过灵敏*/
	private  int mTouchSlop = 5;

//	private HeaderScrollContentLayout mFrame;

	private GestureDetectorCompat mGestureDetector;

    public HeaderScrollView(@NonNull Context context) {
        super(context, null);
        init(context, null);
    }

    public HeaderScrollView(@NonNull Context context, @NonNull AttributeSet attrs) {
    	super(context, attrs);
    	init(context, attrs);
    }

    public HeaderScrollView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

	@SuppressLint("InlinedApi")
    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();

        setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.requestFocusFromTouch();
				return false;
			}
		});

		setVerticalScrollBarEnabled(false);
		ViewCompat.setOverScrollMode(this, ViewCompat.OVER_SCROLL_NEVER);

		mGestureDetector = new GestureDetectorCompat(context,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDown(MotionEvent e) {
//						return ViewUtil.canScrollVertically(HeaderScrollView.this, 1);
                        return false;
					}

					@Override
					public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2,
                                            float distanceX, float distanceY) {
						if (Math.abs(distanceY) < mTouchSlop) {
							return false;
						}
                        HeaderScrollContentLayout contentLayout = (HeaderScrollContentLayout) getChildAt(0);

						if (distanceY < 0) {    //向下拖动
							//子View需要滚动或者角度偏向水平，交给子View（返回false）；否则自己处理（返回true）
                            return !canChildScrollVertically(e2, contentLayout.getScrollableView(), -1)
									&& Math.abs(distanceX) < Math.abs(distanceY);
						}

						if (distanceY > 0) {    //向上拖动
							//能再继续往上拖时，自己处理（返回true）；否则尝试将滚动事件交给子View（返回false）
                            return ViewUtil.canScrollVertically(HeaderScrollView.this, 1);
						}
						return false;
					}

					@Override
					public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2,
                                           float velocityX, float velocityY) {
                        HeaderScrollContentLayout contentLayout = (HeaderScrollContentLayout) getChildAt(0);

						if (velocityY > 0) {    //向下拖动
							//子View需要滚动或者角度偏向水平，交给子View（返回false）；否则自己处理（返回true）
							return !canChildScrollVertically(e2, contentLayout.getScrollableView(), -1)
									&& Math.abs(velocityX) < Math.abs(velocityY);
						}

						if (velocityY < 0) {    //向上拖动
							//能再继续往下拖时，自己处理（返回true）；否则尝试将滚动事件交给子View（返回false）
							return ViewUtil.canScrollVertically(HeaderScrollView.this, 1);
						}
						return false;
					}
				});
	}

	/**
     * 重写父类函数，主要是解决，在FocusableInTouchMode模式下，scroll会对齐某个child view。
     * 修改为 如果 child view 已经在屏幕中可见，则不滚动。如果在屏幕外，则滚动。<br/>
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


    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
    	//必须调用super.onInterceptTouchEvent来保证流程完整，否则会出现诸如Invalid pointerId=-1之类的错误
    	boolean ret = super.onInterceptTouchEvent(ev);

		HeaderScrollContentLayout contentLayout = (HeaderScrollContentLayout) getChildAt(0);
    	if (contentLayout.getChildCount() != 0) {
    		return mGestureDetector.onTouchEvent(ev);
		}
    	return ret;
    }

    /**
     * 递归判断MotionEvent处命中的View（或其子View层级）是否可沿着指定的方向继续滚动
     * @param ev MotionEvent，触摸事件
     * @param child View
     * @param direction 负数表示向下滚动，正数表示向上滚动
     * @return 若该View或者其子View层级内，存在某个View命中MotionEvent，并且能沿着指定的方向继续滚动，则返回true；否则返回false
     */
    private static boolean canChildScrollVertically(@NonNull MotionEvent ev, @Nullable View child, int direction) {
    	if (child == null) {
    		return false;
    	}

    	if (!child.isShown()) {
    		return false;
    	}

    	float evX = ev.getRawX();
    	float evY = ev.getRawY();
    	int[] location = new int[2];
    	child.getLocationOnScreen(location);
    	RectF rect = new RectF(location[0], location[1],
    			location[0] + child.getWidth(), location[1] + child.getHeight());

    	if (!rect.contains(evX, evY)) {
    		return false;
    	}

    	if (ViewUtil.canScrollVertically(child, direction)) {
    		return true;
    	}

    	if (child instanceof ViewGroup) {
    		ViewGroup viewGroup = (ViewGroup) child;
    		int count = viewGroup.getChildCount();
    		for (int i = 0; i < count; i++) {
    			View grantChild = viewGroup.getChildAt(i);
    			if (canChildScrollVertically(ev, grantChild, direction)) {
    				return true;
    			}
    		}

    	}

    	return false;
    }

    /**
     * 父类（ScrollView）中会强制指定子View的MeasureSpec为UNSPECIFIED：
     * 参考{@link android.widget.ScrollView#measureChild(View, int, int)}，其原意是子View想显示多高就显示多高。<br/>
     *
     * 这样会导致HeaderFrameLayout无法获取父View的整个区域大小，
     * 从而无法达到特殊的布局效果（其子View Scrollable无法撑开到整个区域大小）。<br/>
     *
     * 所以此处改成最原始的ViewGroup里的measureChild形式（参考{@link ViewGroup#measureChild(View, int, int)}：
     * 即LayoutParam height为MATCH_PARENT时，就按MATCH_PARENT处理，以便撑到整个区域大小。<br/><br/>
     *
     * {@inheritDoc}
     */
    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                + getPaddingRight(), lp.width);

        childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.getSize(parentHeightMeasureSpec), View.MeasureSpec.EXACTLY);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * 父类（ScrollView）中会强制指定子View的MeasureSpec为UNSPECIFIED：
     * 参考{@link android.widget.ScrollView#measureChildWithMargins(View, int, int, int, int)}，其原意是子View想显示多高就显示多高。<br/>
     *
     * 这样会导致HeaderFrameLayout无法获取父View的整个区域大小，
     * 从而无法达到特殊的布局效果（其子View Scrollable无法撑开到整个区域大小）。<br/>
     *
     * 所以此处改成最原始的ViewGroup里的measureChild形式（参考{@link ViewGroup#measureChildWithMargins(View, int, int, int, int)}：
     * 即LayoutParam height为MATCH_PARENT时，就按MATCH_PARENT处理，以便撑到整个区域大小。<br/><br/>
     *
     * {@inheritDoc}
     */
    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.getSize(parentHeightMeasureSpec), View.MeasureSpec.EXACTLY);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

}