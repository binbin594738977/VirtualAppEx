package com.weiliu.library.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * 实现类似iOS的SectionView的效果
 */
public class SectionRecyclerView extends PerformanceRecyclerView {

    private View mTitleView;

    /**
     * 标题位置
     */
    private int mTitlePos = -1;

    /**
     * RecyclerView的顶部是否处在divider区域
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private boolean mInDividerArea;

    private OnTitleChangedListener mOnTitleChangedListener;

    public SectionRecyclerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SectionRecyclerView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        addOnScrollListener(mOnScrollListener);
    }

    /**
     * 设置用来显示section标题的的View
     *
     * @param titleView 该View悬浮在RecyclerView之上，与RecyclerView顶部对齐，并且处在其Parent的最顶端。
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
                && getAdapter() != null && getAdapter().getItemCount() > 0) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
            int pos = layoutManager.findFirstVisibleItemPosition();
            if (pos < 0) {
                return;
            }

            if (inDataPos(pos)) {
                mTitleView.setVisibility(VISIBLE);
                if (mTitlePos != pos) {
                    mTitlePos = pos;

                    mOnTitleChangedListener.onTitleChanged(mTitleView, this, pos);
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
    public void setAdapter(Adapter adapter) {
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mObserver);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }
        super.setAdapter(adapter);
    }




    private void offsetTitleScrollPos(int offset) {
        ((View) mTitleView.getParent()).scrollTo(0, -offset);
    }

    /**
     * 该位置是否处在数据索引中，而非header footer ignore
     *
     * @param pos
     * @return
     */
    private boolean inDataPos(int pos) {
        return mOnTitleChangedListener != null && mOnTitleChangedListener.inDataPos(pos);
    }

    /**
     * 该位置是否为“连接状态”。比如该位置处在A区域，而下一条就进入了B区域，则此位置就为“连接状态”
     *
     * @param pos
     * @return
     */
    private boolean isConnectPos(int pos) {
        if (!inDataPos(pos)) {
            return true;
        }

        return mOnTitleChangedListener.hasTitle(pos);
    }


    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == SCROLL_STATE_IDLE) {
                if (mTitleView != null && mOnTitleChangedListener != null) {
                    changeTitle();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (recyclerView.getChildCount() == 0) {
                return;
            }

            if (mTitleView != null && mOnTitleChangedListener != null) {
                changeTitle();

                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                int count = getAdapter().getItemCount();

                if (inDataPos(firstVisibleItem)) {
                    if (firstVisibleItem < count - 1
                            //下一条是“连接状态”，则执行滚动，实现类似iOS section的承上启下的效果
                            && isConnectPos(firstVisibleItem + 1)) {

                        int titleHeight = mTitleView.getHeight();
                        int titleTop = mTitleView.getTop();
                        int titleBottom = mTitleView.getBottom();

                        int itemTop = recyclerView.getChildAt(0).getTop();
                        int itemBottom = recyclerView.getChildAt(0).getBottom();

                        int offset;

                        if (itemTop > 0 || itemBottom < 0) {
                        /*
						 * first item的top大于0，或者bottom小于0，肯定是发生在recyclerView的顶部是divider区域的时候，
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
        }
    };


    private AdapterDataObserver mObserver = new AdapterDataObserver() {

        @Override
        public void onChanged() {
            mTitlePos = -1;
            mOnScrollListener.onScrolled(SectionRecyclerView.this, 0, 0);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mOnScrollListener.onScrolled(SectionRecyclerView.this, 0, 0);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mOnScrollListener.onScrolled(SectionRecyclerView.this, 0, 0);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mOnScrollListener.onScrolled(SectionRecyclerView.this, 0, 0);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mOnScrollListener.onScrolled(SectionRecyclerView.this, 0, 0);
        }
    };

    /**
     * 标题内容变化的监听回调
     *
     * @author qumiao
     */
    public static abstract class OnTitleChangedListener {

        /**
         * 是否在标题内容区域。一般如果是header或者footer，则不在标题区域
         * @param position
         * @return
         */
        public boolean inDataPos(int position) {
            return true;
        }

        /**
         * 当前位置是否有标题。
         * 比如从位置3开始一直到5都是以A开头的，一般只在位置3处显示标题A，那么此时hasTitle(3)就应该返回true，
         * 而hasTitle(4)和hasTitle(5)就应该返回false.
         *
         * @param position 当前位置
         * @return
         */
        public abstract boolean hasTitle(int position);

        /**
         * 标题内容发生变化
         *
         * @param titleView 显示标题的View
         * @param recyclerView 显示列表的SectionRecyclerView
         * @param position  标题在RecyclerView中的位置
         */
        protected void onTitleChanged(View titleView, SectionRecyclerView recyclerView, int position) {
            int pos = getTitlePos(position);
            setTitle(titleView, pos);
        }

        /**
         * 设置（显示）标题
         * @param titleView 显示标题的View
         * @param titlePos 标题位置（相对列表中的位置）
         */
        public abstract void setTitle(View titleView, int titlePos);

        public int getTitlePos(int pos) {
            do {
                if (hasTitle(pos)) {
                    return pos;
                }
            } while (--pos > 0);
            return 0;
        }
    }
}
