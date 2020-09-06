package com.weiliu.library.widget;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 配合该Adapter可以使RecyeclerView滚动时流畅
 * Created by qumiaowin on 2016/6/10.
 */
public abstract class PerformanceAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    @Deprecated
    @Override
    public void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(holder, position, isPerformanceMode(holder), new ArrayList<>());
    }

    @Override
    public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        onBindViewHolder(holder, position, isPerformanceMode(holder), payloads);
    }

    public abstract void onBindViewHolder(VH holder, int position, boolean performance, List<Object> payloads);

    /**
     * 是否处于performance模式（一般是正在滚动）。
     * 该模式下请尽量执行轻任务，比如ImageView上暂时只显示缓存，而不拉取网络图片
     * @param holder
     * @return
     */
    protected boolean isPerformanceMode(VH holder) {
        RecyclerView owner = (RecyclerView) holder.itemView.getParent();
        if (owner == null) {
            return false;
        }
        int state = owner.getScrollState();
        return state != RecyclerView.SCROLL_STATE_IDLE;
    }
}
