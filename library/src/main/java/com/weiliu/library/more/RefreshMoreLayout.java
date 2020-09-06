package com.weiliu.library.more;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.weiliu.library.widget.FixedSwipeRefreshLayout;

/**
 * 下拉刷新，上拉加载
 * Created by qumiao on 2016/5/9.
 */
public class RefreshMoreLayout extends FixedSwipeRefreshLayout {

    private RecyclerView mRecyclerView;
    private RefreshMoreAdapter mAdapter;

    private boolean mLoading;
    private int mCurrentLoadStart;
    private int mCurrentLoadCount;
    private boolean mCanTryAgain = true;

    public RefreshMoreLayout(Context context) {
        super(context);
        init(context);
    }

    public RefreshMoreLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mRecyclerView = new RefreshMoreRecyclerView(this);
        addView(mRecyclerView);

        mRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);

        setOnRefreshListener(mOnRefreshListener);
    }

    /**
     * 设置Adapter。同时也会执行RecyclerView的setAdapter，并将上一次的adapter回收（如果存在的话）
     *
     * @param adapter
     * @param <DT>
     */
    public <DT> void setAdapter(@NonNull RefreshMoreAdapter<DT> adapter) {
        if (mAdapter != adapter && mAdapter != null) {
            stop();
            mAdapter.destroy();
        }

        mAdapter = adapter;
        mAdapter.setCallBack(mCallBack);
        mAdapter.onInitUI(mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setRefreshMoreLayout(this);
    }

    /**
     * 不是特别必要时，请不要直接获取RecyclerView引用来执行操作（尤其是setAdapter）
     *
     * @return
     */
    @Deprecated
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void start() {
        if (mAdapter == null) {
            return;
        }

        if (mAdapter.isEmpty() && mCanTryAgain && !mLoading && !isRefreshing()) {
            refresh();
        }
    }

    /**
     * 停止当前的数据加载任务，并执行刷新。
     */
    public void refresh() {
        if (mAdapter == null) {
            return;
        }

        stop(false);
        setRefreshing(true);
        mAdapter.startLoad(0, mAdapter.getItemCountPerPage());
    }

    /**
     * 停止数据加载任务。
     */
    public void stop() {
        stop(true);
    }

    private void stop(boolean stopRefreshingAnimation) {
        if (mAdapter == null) {
            return;
        }

        if (mLoading) {
            mAdapter.cancelLoad(mCurrentLoadStart, mCurrentLoadCount);
        }

        if (stopRefreshingAnimation) {
            setRefreshing(false);
        }
    }

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refresh();
        }
    };

    private RefreshMoreAdapter.CallBack mCallBack = new RefreshMoreAdapter.CallBack() {
        @Override
        public boolean canLoad(int start, int count) {
            return !mLoading;
        }

        @Override
        public void startLoad(int start, int count) {
            mLoading = true;
            mCurrentLoadStart = start;
            mCurrentLoadCount = count;
        }

        @Override
        public void cancelLoad(int start, int count) {
            mLoading = false;
            setRefreshing(false);
        }

        @Override
        public void successLoad(int start, int count) {
            mLoading = false;
            setRefreshing(false);
        }

        @Override
        public void cacheLoad(int start, int count) {
            mLoading = false;
            setRefreshing(false);
        }

        @Override
        public void failLoad(int start, int count, String message, boolean canTryAgain) {
            mLoading = false;
            mCanTryAgain = canTryAgain;
            setRefreshing(false);
        }
    };

}
