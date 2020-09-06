package com.weiliu.library.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * 配合该View可以使滚动时流畅
 * Created by qumiaowin on 2016/6/10.
 */
public class PerformanceRecyclerView extends RecyclerView {

    public static final String PAYLOAD_SCROLL_IDLE = "SCROLL_STATE_TRANS_TO_IDLE";


    public PerformanceRecyclerView(Context context) {
        super(context);
        init();
    }

    public PerformanceRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PerformanceRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void saveScrollPosition(Bundle outState) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            outState.putInt("PRVScrollPosition", linearLayoutManager.findFirstVisibleItemPosition());
            if (getChildCount() > 0) {
                outState.putInt("PRVScrollOffset", getChildAt(0).getTop());
            }
        }
    }

    public void restoreScrollPosition(Bundle savedInstanceState) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            int position = savedInstanceState.getInt("PRVScrollPosition");
            int offset = savedInstanceState.getInt("PRVScrollOffset");
            linearLayoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    private void init() {
//        addOnScrollListener(mOnScrollListener);
    }

    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                int first = RecyclerView.NO_POSITION;
                int end = RecyclerView.NO_POSITION;
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager manager = (LinearLayoutManager) layoutManager;
                    first = manager.findFirstVisibleItemPosition();
                    end = manager.findLastVisibleItemPosition();
                } else if (layoutManager instanceof GridLayoutManager) {
                    GridLayoutManager manager = (GridLayoutManager) layoutManager;
                    first = manager.findFirstVisibleItemPosition();
                    end = manager.findLastVisibleItemPosition();
                }

                if (first == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION) {
                    return;
                }

                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                if (adapter != null) {
                    adapter.notifyItemRangeChanged(first, end - first + 1, PAYLOAD_SCROLL_IDLE);
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };
}
