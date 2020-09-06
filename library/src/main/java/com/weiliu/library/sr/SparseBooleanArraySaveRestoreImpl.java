package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.SparseBooleanArray;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 16:18
 * 说明：
 */

class SparseBooleanArraySaveRestoreImpl implements ContainerSaveRestore<SparseBooleanArray> {

    @Override
    public void copyElement(@NonNull SparseBooleanArray fieldObj, @Nullable SparseBooleanArray newValue) {
        fieldObj.clear();
        if (newValue != null) {
            int size = newValue.size();
            for (int i = 0; i < size; i++) {
                int key = newValue.keyAt(i);
                boolean val = newValue.valueAt(i);
                fieldObj.append(key, val);
            }
        }
    }

    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull SparseBooleanArray value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        int[] keyIntArray = new int[value.size()];
        boolean[] valueBooleanArray = new boolean[value.size()];
        int size = value.size();
        for (int i = 0; i < size; i++) {
            keyIntArray[i] = value.keyAt(i);
            valueBooleanArray[i] = value.valueAt(i);
        }

        bundle.putIntArray(key, keyIntArray);
        bundle.putBooleanArray(key + ":value", valueBooleanArray);

        return true;
    }

    @Nullable
    @Override
    public SparseBooleanArray createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                                             @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        return new SparseBooleanArray();
    }

    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull SparseBooleanArray value, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        int[] keyIntArray = bundle.getIntArray(key);
        boolean[] valueBooleanArray = bundle.getBooleanArray(key + ":value");

        int length = keyIntArray.length;
        for (int i = 0; i < length; i++) {
            int keyResult = keyIntArray[i];
            boolean valueResult = valueBooleanArray[i];
            value.append(keyResult, valueResult);
        }
        return true;
    }
}
