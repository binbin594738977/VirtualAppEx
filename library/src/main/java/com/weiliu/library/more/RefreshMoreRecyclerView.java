package com.weiliu.library.more;

import android.annotation.SuppressLint;
import android.support.v4.widget.SwipeRefreshLayout;

import com.weiliu.library.widget.PerformanceRecyclerView;

/**
 * 修复原生RecyclerView跟SwipeRefreshLayout共存时，下拉过早触发刷新的问题
 * Created by qumiao on 2016/5/18.
 */
@SuppressLint("ViewConstructor")
public class RefreshMoreRecyclerView extends PerformanceRecyclerView {

    public RefreshMoreRecyclerView(SwipeRefreshLayout swipeRefreshLayout) {
        super(swipeRefreshLayout.getContext());
        addOnScrollListener(new SwipeRefreshLayoutToggleScrollListener(swipeRefreshLayout));
    }

//    @Override
//    public boolean canScrollVertically(int direction) {
//        // check if scrolling up
//        if (direction < 1) {
//            boolean original = super.canScrollVertically(direction);
//            return !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
//        }
//        return super.canScrollVertically(direction);
//
//    }


}
