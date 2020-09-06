package com.weiliu.library.viewpagerindicator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * TabHostPageIndicator需要用到的，用来生成tab的接口。
 * 一般是ViewPager的PageAdapter实现该接口。
 */
public interface TabHostPageAdapter {
    /**
     * 生成tab
     * @param inflater for inflate
     * @param parent for inflate (parent param)
     * @param position position
     * @return tab view
     */
    View makeTabView(LayoutInflater inflater, ViewGroup parent, int position);
}
