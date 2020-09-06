package com.weiliu.library.widget;


import com.weiliu.library.json.JsonInterface;

/**
 * RefreshMoreAdapter的item相关回调
 * Created by qumiao on 2016/5/12.
 */
public interface AdapterItemCallback<T extends JsonInterface> {

    T getItem(int position);

    long getItemId(int position);

    int getItemCount();

    int getItemViewType(int position);
}
