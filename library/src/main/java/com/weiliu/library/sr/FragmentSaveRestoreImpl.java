package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 16:18
 * 说明：
 */

class FragmentSaveRestoreImpl implements SaveRestore<Fragment> {
    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Fragment value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        FragmentManager manager;
        if (fieldOwner instanceof Fragment) {
            Fragment fragment = (Fragment) fieldOwner;
            if (fragment.getTargetFragment() != value) {
                bundle.putBoolean(key + "_isChild", true);
                manager = fragment.getChildFragmentManager();
            } else {
                manager = fragment.getFragmentManager();
            }
        } else {
            manager = ((FragmentActivity) fieldOwner).getSupportFragmentManager();
        }

        manager.putFragment(bundle, key, value);
        return true;
    }

    @Nullable
    @Override
    public Fragment createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                                   @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        FragmentManager manager;
        if (fieldOwner instanceof Fragment) {
            Fragment fragment = (Fragment) fieldOwner;
            if (bundle.getBoolean(key + "_isChild")) {
                manager = fragment.getChildFragmentManager();
            } else {
                manager = fragment.getFragmentManager();
            }
        } else {
            manager = ((FragmentActivity) fieldOwner).getSupportFragmentManager();
        }

        return manager.getFragment(bundle, key);
    }

    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull Fragment value, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        return true;
    }
}
