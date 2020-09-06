package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.weiliu.library.R;
import com.weiliu.library.util.ViewUtil;

import java.util.Arrays;

/**
 * <p>单行流式布局：</p>
 * <p></p>
 * <ul>
 *     <li>可以开启reserveMode模式，使最后一个子View为 reservedView；</li>
 *     <li>如果设置了FLAG_AUTO_HIDE_IS_SHOW_COMPLETELY，则当其他子View都能显示完全时，reservedView自动隐藏（GONE）；</li>
 *     <li>如果超出范围，则通过隐藏或者缩短靠后的子View，以保证reservedView的完全显示。</li>
 * </ul>
 * @author qumiao
 */
public class SingleLineLayout extends ViewGroup {
    /**
     * 开启reserveMode模式
     */
    public static final int FLAG_ENABLED = 0x01;
    /**
     * 当其他子View都能显示完全时，reservedView自动隐藏
     */
    public static final int FLAG_AUTO_HIDE_IS_SHOW_COMPLETELY = 0x02;
    /**
     * 如果超出范围，则通过隐藏（而不是缩短）靠后的子View，以保证reservedView的完全显示
     */
    public static final int FLAG_HIDE_OTHERS = 0x04;

    @ViewDebug.ExportedProperty(category = "measurement", flagMapping = {
            @ViewDebug.FlagToString(mask = -1,
                    equals = -1, name = "NONE"),
            @ViewDebug.FlagToString(mask = Gravity.NO_GRAVITY,
                    equals = Gravity.NO_GRAVITY, name = "NONE"),
            @ViewDebug.FlagToString(mask = Gravity.TOP,
                    equals = Gravity.TOP, name = "TOP"),
            @ViewDebug.FlagToString(mask = Gravity.BOTTOM,
                    equals = Gravity.BOTTOM, name = "BOTTOM"),
            @ViewDebug.FlagToString(mask = Gravity.LEFT,
                    equals = Gravity.LEFT, name = "LEFT"),
            @ViewDebug.FlagToString(mask = Gravity.RIGHT,
                    equals = Gravity.RIGHT, name = "RIGHT"),
            @ViewDebug.FlagToString(mask = Gravity.CENTER_VERTICAL,
                    equals = Gravity.CENTER_VERTICAL, name = "CENTER_VERTICAL"),
            @ViewDebug.FlagToString(mask = Gravity.CENTER_HORIZONTAL,
                    equals = Gravity.CENTER_HORIZONTAL, name = "CENTER_HORIZONTAL"),
            @ViewDebug.FlagToString(mask = Gravity.CENTER,
                    equals = Gravity.CENTER, name = "CENTER")
    }, formatToHexString = true)
    private int mGravity = Gravity.START | Gravity.TOP;

    private int mSpace;
    private int mReserveMode;

    private int mTotalWidth;
    private int mTotalHeight;
    private boolean[] mChildrenHidden;

    public static class LayoutParams extends MarginLayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public SingleLineLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public SingleLineLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SingleLineLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SingleLineLayout);
            mSpace = a.getDimensionPixelSize(R.styleable.SingleLineLayout_space, mSpace);
            mGravity = a.getInt(R.styleable.SingleLineLayout_android_gravity, mGravity);
            mReserveMode = a.getInt(R.styleable.SingleLineLayout_reserveMode, mReserveMode);
            a.recycle();
        }
    }

    /**
     * Describes how the child views are positioned. Defaults to GRAVITY_TOP. If
     * this layout has a VERTICAL orientation, this controls where all the child
     * views are placed if there is extra vertical space. If this layout has a
     * HORIZONTAL orientation, this controls the alignment of the children.
     *
     * @param gravity See {@link Gravity}
     * @attr ref android.R.styleable#LinearLayout_gravity
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();
        if (count == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        mChildrenHidden = new boolean[count];

        int reserveWidth = 0;
        int lineHeight = 0;
        int unspecifiedWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        boolean reserveModeEnabled = (mReserveMode & FLAG_ENABLED) != 0;

        if (reserveModeEnabled && count > 0) {
            View reserveView = getChildAt(count - 1);
            if (reserveView.getVisibility() != GONE) {
                measureChildWithMargins(reserveView, unspecifiedWidthSpec, 0, heightMeasureSpec, 0);
                reserveWidth = ViewUtil.getMeasuredWidthWithMargin(reserveView);
                lineHeight = ViewUtil.getMeasuredHeightWithMargin(reserveView);
            }
        }

		int xpos = 0;
        int num = reserveModeEnabled ? count - 1 : count;
        int i = 0;
		for (; i < num; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				measureChildWithMargins(child, unspecifiedWidthSpec, 0, heightMeasureSpec, 0);
                final int space = needSpace(i) ? mSpace : 0;

                final int incrementWidth = ViewUtil.getMeasuredWidthWithMargin(child) + space;
                if (xpos + incrementWidth > width - getPaddingLeft() - getPaddingRight() - reserveWidth) {
                    /*
                    如果是倒数第二个View了，
                    且有FLAG_AUTO_HIDE_IS_SHOW_COMPLETELY模式，
                    且除去reservedView就能显示得下，
                    则不再显示reservedView了（最后一个View）
                     */
                    if (reserveModeEnabled && i == count - 2
                            && (mReserveMode & FLAG_AUTO_HIDE_IS_SHOW_COMPLETELY) != 0
                            && xpos + incrementWidth - space <= width - getPaddingLeft() - getPaddingRight()) {
                        mChildrenHidden[count - 1] = true;
                        xpos += incrementWidth - space;
                        lineHeight = Math.max(lineHeight, ViewUtil.getMeasuredHeightWithMargin(child));
                        break;

                    } else if (reserveModeEnabled && (mReserveMode & FLAG_HIDE_OTHERS) != 0) {
                        Arrays.fill(mChildrenHidden, i, num, true);
                        break;

                    } else {    // 将该View压缩，后续的前部View（reserveModeEnabled时除去reservedView）全部隐藏
                        int atMostSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
                        int widthUsed = xpos + (reserveWidth > 0 ? reserveWidth + space : 0);
                        measureChildWithMargins(child, atMostSpec, widthUsed, heightMeasureSpec, 0);
                        xpos += ViewUtil.getMeasuredWidthWithMargin(child);
                        lineHeight = Math.max(lineHeight, ViewUtil.getMeasuredHeightWithMargin(child));
                        Arrays.fill(mChildrenHidden, i + 1, num, true);
                        break;
                    }

                } else if (reserveModeEnabled && i == count - 2 && (mReserveMode & FLAG_AUTO_HIDE_IS_SHOW_COMPLETELY) != 0) {
                    mChildrenHidden[count - 1] = true;
                }

                xpos += incrementWidth;
                lineHeight = Math.max(lineHeight, ViewUtil.getMeasuredHeightWithMargin(child));
			}
		}

        mTotalWidth = xpos + getPaddingLeft() + getPaddingRight();
        if (reserveModeEnabled && !mChildrenHidden[count - 1]) {
            mTotalWidth += reserveWidth;
        }

        mTotalHeight = lineHeight + getPaddingTop() + getPaddingBottom();

        setDimensionWithTotalWidthAndHeight(widthMeasureSpec, heightMeasureSpec);
	}


    private void setDimensionWithTotalWidthAndHeight(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = mTotalWidth;
        } else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            if (mTotalWidth < width) {
                width = mTotalWidth;
            }
        }

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = mTotalHeight;

        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            if (mTotalHeight < height) {
                height = mTotalHeight;
            }
        }
        setMeasuredDimension(width, height);
    }

    private boolean needSpace(int pos) {
        int count = getChildCount();
        for (int i = pos + 1; i < count; i++) {
            if (getChildAt(i).getVisibility() != GONE && !mChildrenHidden[i]) {
                return true;
            }
        }

        return false;
    }

	@NonNull
	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

    @NonNull
	@Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @NonNull
	@Override
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();

        int xOff = getPaddingLeft();
        switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.RIGHT:
                xOff += r - l - mTotalWidth;
                break;
            case Gravity.CENTER_HORIZONTAL:
                xOff += (r - l - mTotalWidth) / 2;
                break;
            default:
                break;
        }

        int xpos = xOff;
		for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getVisibility() != GONE && !mChildrenHidden[i]) {
                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();

                int yOff = getPaddingTop();
                switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.BOTTOM:
                        yOff += b - t - childh - lp.topMargin - lp.bottomMargin - getPaddingTop() - getPaddingBottom();
                        break;
                    case Gravity.CENTER_VERTICAL:
                        yOff += (b - t - childh - lp.topMargin - lp.bottomMargin - getPaddingTop() - getPaddingBottom()) / 2;
                        break;
                    default:
                        break;
                }

                int left = xpos + lp.leftMargin;
                int right = left + childw;
                int top = lp.topMargin + yOff;
                int bottom = top + childh;
                child.layout(left, top, right, bottom);

                xpos = right + lp.rightMargin;
                if (needSpace(i)) {
                    xpos += mSpace;
                }
            }
        }
    }

	@Nullable
	private ListAdapter mAdapter;
	/**
	 * 模拟ListView的方式来批量添加子View
	 * @param adapter
	 */
	public void setAdapter(@Nullable ListAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mObserver);
        }

	    if (adapter != null) {
	        adapter.registerDataSetObserver(mObserver);
	    }

	    mAdapter = adapter;
		resetChildren();
	}

	@Nullable
	public ListAdapter getAdapter() {
		return mAdapter;
	}

	private void resetChildren() {
		removeAllViews();
        ListAdapter adapter = mAdapter;
        if (adapter == null) {
            return;
        }

		int count = adapter.getCount();
		for (int i = 0; i < count; i++) {
			addView(adapter.getView(i, null, this));
		}
	}

	@NonNull
	private DataSetObserver mObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			resetChildren();
		}

		@Override
		public void onInvalidated() {
			resetChildren();
		}
	};

}
