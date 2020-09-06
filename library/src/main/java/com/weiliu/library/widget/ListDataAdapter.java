package com.weiliu.library.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * 作者：qumiao
 * 日期：2017/8/2 20:35
 * 说明：
 */
public abstract class ListDataAdapter<T> extends BaseAdapter {

    private List<T> mList;

    public ListDataAdapter() {

    }

    public ListDataAdapter(List<T> listData) {
        mList = listData;
    }

    public void setList(List<T> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public List<T> getList() {
        return mList;
    }

    @Override
    public int getCount() {
        List<T> list = getList();
        return list != null ? list.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return getList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createView(LayoutInflater.from(parent.getContext()), parent, getItemViewType(position));
        }
        bindView(convertView, position, getList().get(position));
        return convertView;
    }

    protected abstract View createView(LayoutInflater inflater, ViewGroup parent, int viewType);

    protected abstract void bindView(View view, int position, T item);
}
