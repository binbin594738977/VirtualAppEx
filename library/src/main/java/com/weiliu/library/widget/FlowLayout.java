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

import java.util.ArrayList;

/**
 * 流式布局，如果子View的显示范围超出屏幕，则换行显示
 * @author qumiao
 */
public class FlowLayout extends ViewGroup {

    /**每行行高*/
	private ArrayList<Integer> mLineHeights = new ArrayList<>();
    /**每行的剩余宽度*/
    private ArrayList<Integer> mRestMargins = new ArrayList<>();
    /**每行的起始索引*/
    private ArrayList<Integer> mRowStartIndexes = new ArrayList<>();

	private int mHorizontalSpacing;
	private int mVerticalSpacing;

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
    /**
     * 是否单行
     */
    private boolean mSingleLine;


    public static class LayoutParams extends ViewGroup.LayoutParams {
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public FlowLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public FlowLayout(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FlowLayout(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Flowlayout);
            mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.Flowlayout_horizontalSpace, 0);
            mVerticalSpacing = a.getDimensionPixelSize(R.styleable.Flowlayout_verticalSpace, 0);
            mGravity = a.getInt(R.styleable.Flowlayout_android_gravity, mGravity);
            mSingleLine = a.getBoolean(R.styleable.Flowlayout_android_singleLine, false);
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


    public void setSingleLine(boolean singleLine) {
        if (mSingleLine != singleLine) {
            mSingleLine = singleLine;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("widthMeasureSpec must not be UNSPECIFIED!");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        final int count = getChildCount();
        int lineHeight = 0;
        mRowStartIndexes.clear();
        mLineHeights.clear();
        mRestMargins.clear();

		int xpos = getPaddingLeft();
		int ypos = getPaddingTop();

        int rowStart = 0;
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				measureChild(child, widthMeasureSpec, heightMeasureSpec);
				final int childw = child.getMeasuredWidth();

				if (xpos + childw > width - getPaddingRight()) {
                    mRowStartIndexes.add(rowStart);
                    mLineHeights.add(lineHeight);
                    mRestMargins.add(Math.max(0, width - getPaddingRight() - xpos + mHorizontalSpacing));

                    xpos = getPaddingLeft();
                    ypos += lineHeight + mVerticalSpacing;
                    lineHeight = 0;
                    rowStart = i;
				}

                lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
				xpos += childw + mHorizontalSpacing;
			}
		}
        if (rowStart < count) {
            mRowStartIndexes.add(rowStart);
            mLineHeights.add(lineHeight);
            mRestMargins.add(Math.max(0, width - getPaddingRight() - xpos + mHorizontalSpacing));
            ypos += lineHeight; //最后一行就不用加mVerticalSpacing了
        }

        if (mSingleLine) {
            for (int i = mLineHeights.size() - 1; i > 0; i--) {
                ypos -= mLineHeights.get(i) + mVerticalSpacing;
            }
        }

		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			height = ypos + getPaddingBottom();

		} else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			if (ypos + getPaddingBottom() < height) {
				height = ypos + getPaddingBottom();
			}
		}
		setMeasuredDimension(width, height);
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
        final int width = r - l;
        final int height = b - t;
		int xpos = getPaddingLeft();
        int ypos = getPaddingTop();
        int needHeight = getPaddingTop() + getPaddingBottom();
        for (int lineHeight : mLineHeights) {
            needHeight += lineHeight;
        }

        int row = 0;
		for (int i = 0; i < count; i++) {
            if (row + 1 < mRowStartIndexes.size() && i >= mRowStartIndexes.get(row + 1)) {
                if (mSingleLine) {
                    break;
                }
                xpos = getPaddingLeft();
                ypos += mLineHeights.get(row) + mVerticalSpacing;
                row++;
            }

            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();
                int xOff;
                int yOff;
                switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.RIGHT:
                        xOff = mRestMargins.get(row);
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        xOff = mRestMargins.get(row) >> 1;
                        break;
                    default:
                        xOff = 0;
                        break;
                }
                switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.BOTTOM:
                        yOff = (height - needHeight) + (mLineHeights.get(row) - childh);
                        break;
                    case Gravity.CENTER_VERTICAL:
                        yOff = ((height - needHeight) + (mLineHeights.get(row) - childh)) >> 1;
                        break;
                    default:
                        yOff = 0;
                        break;
                }

                child.layout(xpos + xOff,
                        ypos + yOff,
                        Math.min(xpos + childw + xOff, width - getPaddingRight()),
                        ypos + childh + yOff);
                xpos += childw + mHorizontalSpacing;
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
