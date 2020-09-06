package com.weiliu.library.more;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * RecyclerView添加该OnScrollListener，能避免跟SwipeRefreshLayout共存时，下拉过早触发刷新的问题。
 * Created by qumiaowin on 2016/6/16.
 */
public class SwipeRefreshLayoutToggleScrollListener extends RecyclerView.OnScrollListener {
    private int mExpectedVisiblePosition = 0;
    private SwipeRefreshLayout mSwipeLayout;

    private boolean mHasGetSwipeLayoutEnable;
    private boolean mSwipeLayoutEnabled;

    public SwipeRefreshLayoutToggleScrollListener(SwipeRefreshLayout swipeLayout) {
        mSwipeLayout = swipeLayout;
    }

    public void setExpectedFirstVisiblePosition(int position) {
        mExpectedVisiblePosition = position;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (!mHasGetSwipeLayoutEnable) {
            mSwipeLayoutEnabled = mSwipeLayout.isEnabled();
            mHasGetSwipeLayoutEnable = true;
        }
        LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisible = llm.findFirstCompletelyVisibleItemPosition();
        if (firstVisible != RecyclerView.NO_POSITION) {
            mSwipeLayout.setEnabled(firstVisible == mExpectedVisiblePosition && mSwipeLayoutEnabled);
        }

    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
    }


}
