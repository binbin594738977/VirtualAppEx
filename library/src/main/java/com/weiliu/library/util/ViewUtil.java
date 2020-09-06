package com.weiliu.library.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * View方法集合
 * @author qumiao
 *
 */
public class ViewUtil {

	private ViewUtil() {

	}

    /**
     * get activity from view
     * @param view
     * @return
     */
	public static Activity getActivity(View view) {
        return Utility.getActivity(view.getContext());
    }

    /**
     * 截图
     * @param view
     * @return
     */
    public static Bitmap takeShot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public static void setEnabledAll(View v, boolean enabled) {
        v.setEnabled(enabled);

        if(v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++)
                setEnabledAll(vg.getChildAt(i), enabled);
        }
    }

    /**
     * Return getMeasuredWidth;
     * or getMeasuredWidth + leftMargin + rightMargin, if layoutParams instanceof MarginLayoutParams
     * @param view
     * @return
     */
    public static int getMeasuredWidthWithMargin(View view) {
        int width = 0;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutParams;
            width += lp.leftMargin + lp.rightMargin;
        }
        width += view.getMeasuredWidth();
        return width;
    }

    /**
     * Return getMeasuredHeight;
     * or getMeasuredHeight + topMargin + bottomMargin, if layoutParams instanceof MarginLayoutParams
     * @param view
     * @return
     */
    public static int getMeasuredHeightWithMargin(View view) {
        int height = 0;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutParams;
            height += lp.topMargin + lp.bottomMargin;
        }
        height += view.getMeasuredHeight();
        return height;
    }

    /**
     * 重置layout的子View个数。
     * @param layout
     * @param newCount
     * @param childRes 如果newCount大于old count，则新的子View通过childRes inflate来填充
     */
    public static void resizeChildrenCount(ViewGroup layout, int newCount, @LayoutRes int childRes) {
        int oldCount = layout.getChildCount();
        if (newCount < oldCount) {
            layout.removeViews(newCount, oldCount - newCount);
        } else if (newCount > oldCount) {
            for (int i = oldCount; i < newCount; i++) {
                LayoutInflater.from(layout.getContext()).inflate(childRes, layout);
            }
        }
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     * @param view View
     * @param direction Negative to check scrolling up (dragging down), positive to check scrolling down (dragging up).
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
	public static boolean canScrollVertically(View view, int direction) {
		Object[] result = new Object[1];
		boolean ret = Utility.invokeHideMethod(view, "canScrollVertically",
				new Class<?>[] {int.class}, new Object[] {direction}, result);
		if (ret) {
			return (Boolean) result[0];
		}
		Utility.invokeMethod(view, "computeVerticalScrollOffset", result);
		final int offset = (Integer) result[0];
		Utility.invokeMethod(view, "computeVerticalScrollRange", result);
		int scrollRange = (Integer) result[0];
		Utility.invokeMethod(view, "computeVerticalScrollExtent", result);
		int scrollExtent = (Integer) result[0];

        final int range = scrollRange - scrollExtent;
        if (range == 0) {
        	return false;
        }
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
	}

	/**
     * Check if this view can be scrolled horizontally in a certain direction.
     * @param view View
     * @param direction Negative to check scrolling left (dragging right), positive to check scrolling right (dragging left).
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public static boolean canScrollHorizontally(View view, int direction) {
    	Object[] result = new Object[1];
		boolean ret = Utility.invokeHideMethod(view, "canScrollHorizontally",
				new Class<?>[] {int.class}, new Object[] {direction}, result);
		if (ret) {
			return (Boolean) result[0];
		}
		Utility.invokeMethod(view, "computeHorizontalScrollOffset", result);
		final int offset = (Integer) result[0];
		Utility.invokeMethod(view, "computeHorizontalScrollRange", result);
		int scrollRange = (Integer) result[0];
		Utility.invokeMethod(view, "computeHorizontalScrollExtent", result);
		int scrollExtent = (Integer) result[0];
        final int range = scrollRange - scrollExtent;
        if (range == 0) {
        	return false;
        }
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    /**
     * 判断TextView是不是正在省略显示（有未显示完的内容）
     * @param textView
     */
    public static boolean isTextViewEllipsized(@NonNull TextView textView) {
        Layout l = textView.getLayout();
        if (l != null) {
            int lines = l.getLineCount();
            if (lines > 0) {
                if (l.getEllipsisCount(lines - 1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static Fragment getViewPagerCurrentFragment(@NonNull FragmentManager manager,
                                                       @NonNull ViewPager pager, @NonNull FragmentPagerAdapter adapter) {
        int item = pager.getCurrentItem();
        if (item < 0 || item >= adapter.getCount()) {
            return null;
        }
        int viewId = pager.getId();
        long id = adapter.getItemId(item);
        String tag = "android:switcher:" + viewId + ":" + id;
        return manager.findFragmentByTag(tag);
    }
}
