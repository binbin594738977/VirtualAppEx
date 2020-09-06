package com.weiliu.library.widget;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

/**
 * 作者：qumiao
 * 日期：2017/8/22 21:38
 * 说明：
 *
 * <pre>
 *
 * 原生的FragmentStatePagerAdapter有个bug：
 *
 * 1. 假设ViewPager的adapter为FragmentStatePagerAdapter，
 *    OffscreenPageLimit为1，且当前显示第一个fragment（fragment0）；
 *
 * 2. 通过点击tab跳过OffscreenPageLimit个item，
 *    会引发fragment0的mUserVisibleHint变为false，并且随后保存到savestate中，接着执行销毁；
 *
 * 3. 再通过点击tab回到fragment0的位置，
 *    FragmentStatePagerAdapter会通过setPrimaryItem方法，将fragment0的mUserVisibleHint设为true；
 *
 * 4. 但是，在最后的finishUpdate里，
 *    执行的transaction commitNowAllowingStateLoss，会将fragment0的mUserVisibleHint又恢复成了false……
 *
 * 5. 于是最终导致了fragment0明明在ViewPager中可见，它的mUserVisibleHint却为false。
 *
 *
 * 解决方案：
 *    在finishUpdate里，执行完默认逻辑之后，再次执行mCurrentPrimaryItem.setUserVisibleHint(true)。
 *
 * </pre>
 */
public abstract class FixedFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private Fragment mCurrentPrimaryItem;

    public FixedFragmentStatePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        mCurrentPrimaryItem = (Fragment) object;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        if (mCurrentPrimaryItem != null) {
            mCurrentPrimaryItem.setUserVisibleHint(true);
        }
    }
}
