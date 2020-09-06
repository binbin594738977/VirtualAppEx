package com.weiliu.library.widget;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 数据为list形式的普通View组成的PagerAdapter
 * Created by qumiao on 2015/10/19.
 */
public abstract class ListPagerAdapter<E> extends PagerAdapter {

    private List<E> mList;

    public ListPagerAdapter(@NonNull List<E> list) {
        mList = list;
    }

    public List<E> getList() {
        return mList;
    }

    public void setList(@NonNull List<E> list) {
        mList = list;
        notifyDataSetChanged();
    }

    /**
     * Create the page view for the given position.
     *
     * @param inflater  The layout inflater.
     * @param container The containing View in which the page will be shown.
     * @param data      The data associate with this item.
     * @param position  The page position to be instantiated.
     * @return The page view.
     */
    protected abstract View makeItemView(LayoutInflater inflater, final ViewGroup container, E data, final int position);

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
        View view = makeItemView(LayoutInflater.from(container.getContext()), container, mList.get(position), position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, Object object) {
        View view = (View) object;
        destroyItemSimply(container, position, mList.get(position), view);
        container.removeView(view);
    }

    /**
     * 页面销毁的时候调用(提供重载)
     */
    protected void destroyItemSimply(ViewGroup container, int position, E data, View view) {

    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
