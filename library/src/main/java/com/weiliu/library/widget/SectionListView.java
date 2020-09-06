package com.weiliu.library.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * 实现类似iOS的SectionView的效果
 */
public class SectionListView extends ListView implements ListView.OnScrollListener {

    private View mTitleView;

    /**
     * 标题位置
     */
    private int mTitlePos = -1;

    /**
     * ListView的顶部是否处在divider区域
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private boolean mInDividerArea;

    private OnScrollListener mWrapedScrollListener;
    private OnTitleChangedListener mOnTitleChangedListener;

    public SectionListView(@NonNull Context context) {
        super(context);
    }

    public SectionListView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 设置用来显示section标题的的View
     *
     * @param titleView 该View悬浮在ListView之上，与ListView顶部对齐，并且处在其Parent的最顶端。
     *                  另外，该titleView的parent一定要是空背景，并且无其他子View（用来实现titleView的滚动效果）
     */
    public void setTitleView(View titleView) {
        if (titleView != mTitleView) {
            mTitleView = titleView;
            changeTitle();
        }
    }

    /**
     * 设置标题内容变化的监听回调
     *
     * @param l OnTitleChangedListener
     */
    public void setOnTitleChangedListener(OnTitleChangedListener l) {
        mOnTitleChangedListener = l;
        changeTitle();
    }

    private void changeTitle() {
        if (mTitleView != null && mOnTitleChangedListener != null
                && getAdapter() != null && getCount() > 0) {
            int pos = getFirstVisiblePosition();

            if (inDataPos(pos)) {
                mTitleView.setVisibility(VISIBLE);
                int dataPos = getDataPos(pos);
                if (mTitlePos != dataPos) {
                    mTitlePos = dataPos;

                    //listView的getItemAtPosition方法对应的是绝对位置，所以此处的参数是pos而不是dataPos
                    Object item = getItemAtPosition(pos);

                    mOnTitleChangedListener.onTitleChanged(mTitleView, item, dataPos);
                }
            } else {
                mTitleView.setVisibility(GONE);
                mTitlePos = -1;
            }
        } else if (mTitleView != null) {
            mTitleView.setVisibility(GONE);
            mTitlePos = -1;
        }
    }

    @Override
    public void setAdapter(@Nullable ListAdapter adapter) {
        ListAdapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mObserver);
        }

        if (adapter != null) {
            adapter.registerDataSetObserver(mObserver);
        }
        super.setAdapter(adapter);
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        super.setOnScrollListener(this);
        mWrapedScrollListener = l;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            if (mTitleView != null && mOnTitleChangedListener != null) {
                changeTitle();
            }
        }
        if (mWrapedScrollListener != null) {
            mWrapedScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(@NonNull AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        if (mTitleView != null && mOnTitleChangedListener != null) {
            changeTitle();

            if (inDataPos(firstVisibleItem)) {
                if (firstVisibleItem < getCount() - 1
                        //下一条是“连接状态”，则执行滚动，实现类似iOS section的承上启下的效果
                        && isConnectPos(firstVisibleItem + 1)) {

                    int titleHeight = mTitleView.getHeight();
                    int titleTop = mTitleView.getTop();
                    int titleBottom = mTitleView.getBottom();

                    int itemTop = view.getChildAt(0).getTop();
                    int itemBottom = view.getChildAt(0).getBottom();

                    int offset;

                    if (itemTop > 0 || itemBottom < 0) {
                        /*
						 * first item的top大于0，或者bottom小于0，肯定是发生在ListView的顶部是divider区域的时候，
						 */
                        mInDividerArea = true;
                        offset = -titleBottom;
                    } else {
                        mInDividerArea = false;

                        if (itemBottom < titleBottom) {    //SUPPRESS CHECKSTYLE
							/*
							 * first item的bottom值不断变小，从而导致itemBottom < titleBottom，
							 * 所以此处逻辑一定是发生在list往上拖的时候。
							 * 当上拖到first item的bottom值甚至小于title view的bottom值时，
							 * 立即调整title view的位置，使title view的底部与之对齐，
							 * 从而形成一种title view跟着item一起向上滚动至消失不见的现象
							 */
                            offset = itemBottom - titleBottom;
                        } else if (itemBottom < titleHeight) {
							/*
							 * 此处逻辑是上处逻辑的反过程，
							 * 即在list往下拖的时候，title view跟着item一起同步向下滚动至完全显示，
							 * 只要未超过titleHeight，就表示title view还有继续往下滚动的余地；
							 * 否则就不再同步滚动，以免title view与parent的顶部脱离，故加上itemBottom < titleHeight条件限制
							 */
                            offset = itemBottom - titleBottom;
                        } else {
							/*
							 * 去掉title view的偏移，回到其正常的位置。
							 */
                            offset = -titleTop;
                        }
                    }

                    if (offset + titleTop > 0) {    //SUPPRESS CHECKSTYLE
						/*
						 * 确保title view不会与parent的顶部脱离
						 */
                        offset = -titleTop;
                    }
                    offsetTitleScrollPos(offset);

                } else {
                    //通过反向偏移回到原始位置（不滚动）
                    offsetTitleScrollPos(-mTitleView.getTop());
                }
            }
        }

        if (mWrapedScrollListener != null) {
            mWrapedScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    private void offsetTitleScrollPos(int offset) {
        ((View) mTitleView.getParent()).scrollTo(0, -offset);
    }

    /**
     * 该位置是否为“连接状态”。比如该位置处在A区域，而下一条就进入了B区域，则此位置就为“连接状态”
     *
     * @param pos ListView中的绝对位置，比如该位置有可能处在header或者footer中
     * @return
     */
    private boolean isConnectPos(int pos) {
        if (!inDataPos(pos)) {
            return true;
        }

        int dataPos = getDataPos(pos);
        return mOnTitleChangedListener.hasTitle(dataPos);
    }

    /**
     * 该位置是否处在数据索引中，而非header footer ignore
     *
     * @param pos ListView中的绝对位置，比如该位置有可能处在header或者footer中
     * @return
     */
    private boolean inDataPos(int pos) {
        int viewType = getAdapter().getItemViewType(pos);
        return viewType != Adapter.IGNORE_ITEM_VIEW_TYPE
                && viewType != AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    /**
     * 获取对应位置在list数据中的索引，即减去listView的header view count
     *
     * @param pos ListView中的绝对位置，比如该位置有可能处在header或者footer中
     * @return
     */
    private int getDataPos(int pos) {
        return pos - getHeaderViewsCount();
    }

    @NonNull
    private DataSetObserver mObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            changeTitle();
        }

        @Override
        public void onInvalidated() {
            changeTitle();
        }

    };

    /**
     * 标题内容变化的监听回调
     *
     * @author qumiao
     */
    public interface OnTitleChangedListener {

        /**
         * 当前位置是否有标题。
         * 比如从位置3开始一直到5都是以A开头的，一般只在位置3处显示标题A，那么此时hasTitle(3)就应该返回true，
         * 而hasTitle(4)和hasTitle(5)就应该返回false.
         *
         * @param position 当前位置
         * @return
         */
        boolean hasTitle(int position);

        /**
         * 标题内容发生变化
         *
         * @param titleView 显示标题的View
         * @param item      标题内容
         * @param position  标题相对list的位置
         */
        void onTitleChanged(View titleView, Object item, int position);
    }
}
