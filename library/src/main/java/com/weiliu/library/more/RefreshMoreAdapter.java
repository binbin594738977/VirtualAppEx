package com.weiliu.library.more;

import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weiliu.library.R;
import com.weiliu.library.SaveState;
import com.weiliu.library.widget.PerformanceAdapter;
import com.weiliu.library.widget.ViewByIdHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qumiao on 2016/5/9.
 */
public abstract class RefreshMoreAdapter<DT> extends PerformanceAdapter<ViewByIdHolder> {

    private List<DT> mItems = new ArrayList<>();
    @SaveState
    private boolean mHasMore;
    @SaveState
    private boolean mHasShowNoMoreView;
    @SaveState
    private int mItemCountPerPage = 20;
    @SaveState
    private int mOffset;

    private Status mStatus = Status.init;

    private CallBack mCallBack;

    private RefreshMoreLayout refreshMoreLayout;

    /**
     * 请在此处对recyclerView进行UI初始化，如{@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager) setLayoutManager}等
     *
     * @param recyclerView
     */
    public void onInitUI(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
    }

    /**
     * 回收资源
     */
    @CallSuper
    public void destroy() {

    }

    public void setShowNoMoreView(boolean mHasShowNoMoreView) {
        this.mHasShowNoMoreView = mHasShowNoMoreView;
    }

    public boolean isShowNoMoreView() {
        return mHasShowNoMoreView;
    }

    public final void setItems(@NonNull List<DT> items) {
        mItems = items;
    }

    public final List<DT> getItems() {
        return mItems;
    }

    public final void setHasMore(boolean hasMore) {
        mHasMore = hasMore;
    }

    public final boolean getHasMore() {
        return mHasMore;
    }

    public final void setOffset(int offset) {
        mOffset = offset;
    }

    public final int getOffset() {
        return mOffset;
    }

    public void setRefreshMoreLayout(RefreshMoreLayout layout) {
        this.refreshMoreLayout = layout;
    }

    public RefreshMoreLayout getRefreshMoreLayout() {
        return refreshMoreLayout;
    }

    /**
     * 设置分页大小，0表示不分页。默认为20
     *
     * @param count
     */
    public final void setItemCountPerPage(int count) {
        mItemCountPerPage = count;
    }

    /**
     * 获取分页大小，0表示不分页
     *
     * @return
     */
    public final int getItemCountPerPage() {
        return mItemCountPerPage;
    }

    /**
     * 加载数据的具体实现
     *
     * @param start 分页起点，0表示重新加载
     * @param count 非0表示分页大小，0表示不分页
     */
    protected abstract void onStartLoad(int start, int count);

    /**
     * 取消加载的具体实现
     *
     * @param start 分页起点，0表示重新加载
     * @param count 非0表示分页大小，0表示不分页
     */
    protected abstract void onCancelLoad(int start, int count);

    /**
     * 数据发生改变
     *
     * @param newItemList 新数据列表
     * @param cache       是否为缓存
     */
    protected void onChanged(List<DT> newItemList, boolean cache) {

    }

    /**
     * 开始加载数据
     *
     * @param start 分页起点，0表示重新加载
     * @param count 非0表示分页大小，0表示不分页
     */
    /*package */
    final void startLoad(int start, int count) {
        mCallBack.startLoad(start, count);
        onStartLoad(start, count);

        Status oldStatus = mStatus;
        if (isEmpty()) {
            mStatus = Status.loading_init;
        } else {
            mStatus = start == 0 ? Status.loading_first : Status.loading_more;
        }
        if (isEmpty() && oldStatus != mStatus) {
            notifyDataSetChanged();
        }
    }

    /**
     * 取消加载数据
     *
     * @param start 分页起点，0表示重新加载
     * @param count 非0表示分页大小，0表示不分页
     */
    /*package */
    final void cancelLoad(int start, int count) {
        mStatus = start == 0 ? Status.canceled_first : Status.canceled_more;
        if (isEmpty()) {
            notifyDataSetChanged();
        }

        mCallBack.cancelLoad(start, count);
        onCancelLoad(start, count);
    }


    /**
     * 加载缓存预览
     *
     * @param resultItems
     * @param start
     * @param count
     */
    public final void cacheLoad(@NonNull List<DT> resultItems, int start, int count) {
        mStatus = Status.success_cache;
        if (start == 0 && !resultItems.isEmpty() && mItems.isEmpty()) {  //为了避免分页混乱，缓存只服务于第一页（初始状态）
            mItems.clear();
            mItems.addAll(resultItems);
            onChanged(new ArrayList<>(mItems), true);
        }
        notifyDataSetChanged();
        mCallBack.cacheLoad(start, count);
    }


    /**
     * 加载数据失败
     *
     * @param start       分页起点，0表示重新加载
     * @param count       非0表示分页大小，0表示不分页
     * @param message     失败信息
     * @param canTryAgain 是否还可以重试
     */
    public final void failLoad(int start, int count, String message, boolean canTryAgain) {
        mStatus = start == 0
                ? (canTryAgain ? Status.failed_first_can_retry : Status.failed_first_cannot_retry)
                : (canTryAgain ? Status.failed_more_can_retry : Status.failed_more_cannot_retry);
        if (isEmpty()) {
            notifyDataSetChanged();
        }
        mCallBack.failLoad(start, count, message, canTryAgain);
    }

    /**
     * 加载数据成功。分页起点随结果大小增加（如果开启分页的话）
     *
     * @param resultItems 结果
     * @param start       分页起点，0表示重新加载
     * @param count       非0表示分页大小，0表示不分页
     * @param hasMore     是否还有更多结果（更多分页）
     */
    public final void successLoad(@NonNull List<DT> resultItems, int start, int count, boolean hasMore) {
        successLoad(resultItems, start, count, resultItems.size(), hasMore);
    }

    /**
     * 加载数据成功
     *
     * @param resultItems     结果
     * @param start           分页起点，0表示重新加载
     * @param count           非0表示分页大小，0表示不分页
     * @param offsetIncrement 分页起点增加量
     * @param hasMore         是否还有更多结果（更多分页）
     */
    public final void successLoad(@NonNull List<DT> resultItems,
                                  int start, int count, int offsetIncrement, boolean hasMore) {
        mStatus = start == 0 ? Status.success_first : Status.success_more;

        if (start == 0 || mItemCountPerPage == 0) {
            mItems.clear();
            mItems.addAll(resultItems);

            mOffset = offsetIncrement;
            mHasMore = hasMore;
            notifyDataSetChanged();

        } else {
            int currentCount = getItemCountSimply();
            mItems.addAll(resultItems);

            mOffset += offsetIncrement;
            if (mHasMore != hasMore) {
                mHasMore = hasMore;
                notifyDataSetChanged();
            } else {
                notifyItemRangeInserted(currentCount, resultItems.size());
            }
        }

        mCallBack.successLoad(start, count);
        onChanged(new ArrayList<>(mItems), false);
    }

    /**
     * 判定当前数据是否为空
     *
     * @return
     */
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    /**
     * 可以返回empty view的layout资源。 如果不显示empty view，则返回0.
     *
     * @param status 状态。为空的时候也分好几种情况，
     *               比如初始加载中（{@link Status#loading_init loading_init})暂时为空，
     *               或者加载成功（{@link Status#success_first success_first})但结果为空，
     *               或者加载失败（{@link Status#failed_first_can_retry failed_first_can_retry}、
     *               {@linkplain Status#failed_first_cannot_retry failed_first_cannot_retry})导致结果为空
     * @return
     */
    protected int getEmptyViewType(Status status) {
        return 0;
    }

    public Status getStatus() {
        return mStatus;
    }

    /**
     * 定义各个item的view type
     * 请重载该方法来完成{@link RecyclerView.Adapter#getItemViewType(int) getItemViewType}实现
     *
     * @param position
     * @return
     */
    protected int getItemViewTypeSimply(int position) {
        return 0;
    }

    /**
     * 定义item个数
     * 请重载该方法来完成{@link RecyclerView.Adapter#getItemCount() getItemCount}实现
     *
     * @return
     */
    protected int getItemCountSimply() {
        return mItems.size();
    }

    /**
     * 生成ViewHolder（针对各个view type）
     *
     * @param parent
     * @param viewType
     * @return
     */
    protected abstract ViewByIdHolder onCreateViewHolderSimply(ViewGroup parent, int viewType);

    /**
     * 用View来展示数据。请重载该方法来完成：
     * {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List) onBindViewHolder}
     *
     * @param holder
     * @param position
     * @param performance performance为true时（一般是正在滚动），请尽量执行轻任务，比如ImageView上暂时只显示缓存，而不拉取网络图片
     * @param payloads
     */
    protected abstract void onBindViewHolderSimply(ViewByIdHolder holder, int position, boolean performance, List<Object> payloads);

    /**
     * item为空时的View。
     * 比如初始加载中暂时为空({@link Status#loading_init loading_init}}时的全屏菊花转，
     * 或者加载成功但结果为空({@link Status#success_first success_first}}时的数据为空提示，
     * 或者加载失败导致没有获取到结果({@link Status#failed_first_can_retry failed_first_can_retry}、
     * {@link Status#failed_first_cannot_retry failed_first_cannot_retry}}时的失败提示。
     *
     * @param parent
     * @return
     */
    protected View onCreateEmptyView(ViewGroup parent, @LayoutRes int emptyLayoutRes, Status status) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(emptyLayoutRes, parent, false);
    }

    protected void onBindEmptyView(ViewByIdHolder holder, int position, boolean performance, List<Object> payloads, Status status) {

    }

    /**
     * 上拉加载时提示正在加载下一分页的View（一般是菊花转）
     *
     * @param parent
     * @return
     */
    protected View onCreateItemTypeMoreView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(R.layout.more_item, parent, false);
    }

    /**
     * 没有更加之后,需要给出没有更多的提醒
     *
     * @param parent
     * @return
     */
    protected View onCreateItemTypeNoMoreView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(R.layout.no_more_item, parent, false);
    }


    /**
     * 获取各个item的具体数据
     *
     * @param position
     * @return
     */
    public DT getItem(int position) {
        return mItems.get(position);
    }

    /**
     * 不要直接重载该方法，请重载{@link RefreshMoreAdapter#getItemCountSimply() getItemCountSimply}
     */
    @Deprecated
    @Override
    public int getItemCount() {
        if (isEmpty()) {
            if (getEmptyViewType(mStatus) != 0) {
                return 1;
            }
        }
        int count = getItemCountSimply();
        return (mHasMore || mHasShowNoMoreView) ? count + 1 : count;
    }

    /**
     * 不要直接重载该方法，请重载{@link RefreshMoreAdapter#getItemViewTypeSimply(int) getItemViewTypeSimply}
     */
    @Deprecated
    @Override
    public int getItemViewType(int position) {
        if (isEmpty()) {
            int emptyViewType = getEmptyViewType(mStatus);
            if (emptyViewType != 0) {
                return emptyViewType;
            }
        }
        //noinspection deprecation
        if (mHasMore && position == getItemCount() - 1) {
            return R.id.item_type_more;
        }
        //增加了没有更多,但是要显示没有更多时候的布局
        if (!mHasMore && mHasShowNoMoreView && position == getItemCount() - 1) {
            return R.id.item_type_no_more;
        }
        return getItemViewTypeSimply(position);
    }

    @Deprecated
    @Override
    public ViewByIdHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (isEmpty()) {
            int emptyViewType = getEmptyViewType(mStatus);
            if (emptyViewType != 0 && viewType == emptyViewType) {
                return new ViewByIdHolder(onCreateEmptyView(parent, emptyViewType, mStatus));
            }
        }

        if (viewType == R.id.item_type_more) {
            return new ViewByIdHolder(onCreateItemTypeMoreView(parent));
        }
        if (viewType == R.id.item_type_no_more) {
            return new ViewByIdHolder(onCreateItemTypeNoMoreView(parent));
        }
        return onCreateViewHolderSimply(parent, viewType);
    }

    /**
     * 不要直接重载该方法，请重载{@link RefreshMoreAdapter#onBindViewHolderSimply(ViewByIdHolder, int, boolean, List) onBindViewHolderSimply}
     */
    @Deprecated
    @Override
    public void onBindViewHolder(ViewByIdHolder holder, int position, boolean performance, List<Object> payloads) {
        //noinspection deprecation
        int viewType = getItemViewType(position);
        if (isEmpty()) {
            int emptyViewType = getEmptyViewType(mStatus);
            if (emptyViewType != 0 && viewType == emptyViewType) {
                onBindEmptyView(holder, position, performance, payloads, mStatus);
                return;
            }
        }
        if (viewType == R.id.item_type_more) {
            if (!performance && mCallBack.canLoad(mOffset, mItemCountPerPage)) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        startLoad(mOffset, mItemCountPerPage);
                    }
                });
            }
        } else if (viewType == R.id.item_type_no_more) {
            //没有更多的提醒只用来显示
        } else {
            onBindViewHolderSimply(holder, position, performance, payloads);
        }
    }


    /*package */void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }

    /*package */interface CallBack {

        boolean canLoad(int start, int count);

        void startLoad(int start, int count);

        void cancelLoad(int start, int count);

        void successLoad(int start, int count);

        void cacheLoad(int start, int count);

        void failLoad(int start, int count, String message, boolean canTryAgain);
    }

    public enum Status {
        init,
        loading_init,
        loading_first,
        loading_more,
        success_cache,
        success_first,
        success_more,
        failed_first_can_retry,
        failed_first_cannot_retry,
        failed_more_can_retry,
        failed_more_cannot_retry,
        canceled_first,
        canceled_more,
    }
}
