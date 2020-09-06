package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 作者：qumiao
 * 日期：2017/5/20 11:48
 * 说明：
 */
public class RootSavableImpl implements Savable {

    private Map<SavePath, List<Pair<SavePath, Object>>> mRestoreRefMap;

    private final Comparator<Object> mObjComparator = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return compareInt(o1.hashCode(), o2.hashCode());
        }

        int compareInt(int lhs, int rhs) {
            return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
        }
    };

    private final Saver mSaver = new DefaultSaver();

    @NonNull
    @Override
    public Saver getSaver() {
        return mSaver;
    }

    private final Object mOwner;

    @NonNull
    @Override
    public Object getOwner() {
        return mOwner;
    }

    @Override
    public void onSave() {

    }

    @Override
    public void onRestored() {

    }

    public RootSavableImpl(@NonNull Object owner) {
        mOwner = owner;
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mRestoreRefMap = new TreeMap<>(new Comparator<SavePath>() {
                @Override
                public int compare(SavePath o1, SavePath o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            mSaver.restoreInstanceState(savedInstanceState, this, new SavePath(), mRestoreRefMap);
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        Map<Savable, List<Savable>> savableMap = new TreeMap<>(mObjComparator);
        mSaver.restoreSavableFields(savedInstanceState, this, new SavePath(), mRestoreRefMap, savableMap);
    }

    public void onSaveInstanceState(Bundle outState) {
        if (outState == null) {
            return;
        }

        long start = System.currentTimeMillis();

        Map<Object, List<Pair<Object, SavePath>>> saveRefMap = new TreeMap<>(mObjComparator);
        Map<Savable, List<Savable>> savableMap = new TreeMap<>(mObjComparator);
        mSaver.saveInstanceState(outState, this, new SavePath(), saveRefMap);
        mSaver.saveSavableFields(outState, this, new SavePath(), saveRefMap, savableMap);

        long time = System.currentTimeMillis() - start;
        if (time > 50) {
            Log.e("RootSavable", getOwner().getClass().getName() + "  onSaveInstanceState  " + time + "ms");
        }
    }
}
