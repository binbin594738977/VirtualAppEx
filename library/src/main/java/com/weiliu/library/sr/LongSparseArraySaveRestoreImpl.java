package com.weiliu.library.sr;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.Pair;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 16:14
 * 说明：
 */

class LongSparseArraySaveRestoreImpl implements ContainerSaveRestore<LongSparseArray> {
    @SuppressWarnings("unchecked")
    @Override
    public void copyElement(@NonNull LongSparseArray fieldObj, @Nullable LongSparseArray newValue) {
        fieldObj.clear();
        if (newValue != null) {
            int size = newValue.size();
            for (int i = 0; i < size; i++) {
                long key = newValue.keyAt(i);
                Object val = newValue.valueAt(i);
                fieldObj.append(key, val);
            }
        }
    }

    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull LongSparseArray value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        long[] keyLongArray = new long[value.size()];
        Bundle[] valueBundleArray = new Bundle[value.size()];
        Type argumentType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];
        int size = value.size();
        for (int i = 0; i < size; i++) {
            keyLongArray[i] = value.keyAt(i);
            Object entryValue = value.valueAt(i);
            Bundle valueBundle = new Bundle();
            if (!SaveRestoreUtil.save(valueBundle, key, argumentType, entryValue, fieldOwner,
                    new SavePath(path).appendKey(key + ":value").appendIndex(i), referenceMap)) {
                return false;
            }
            valueBundleArray[i] = valueBundle;
        }

        bundle.putLongArray(key, keyLongArray);
        bundle.putParcelableArray(key + ":value", valueBundleArray);

        return true;
    }

    @Nullable
    @Override
    public LongSparseArray createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                                          @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        return new LongSparseArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull LongSparseArray value, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        long[] keyLongArray = bundle.getLongArray(key);
        Parcelable[] valueBundleArray = bundle.getParcelableArray(key + ":value");
        Type argumentType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];

        int length = keyLongArray.length;
        for (int i = 0; i < length; i++) {
            long keyResult = keyLongArray[i];
            Bundle valueBundle = (Bundle) valueBundleArray[i];
            Object valueResult = SaveRestoreUtil.restore(valueBundle, key, argumentType, fieldOwner,
                    new SavePath(path).appendKey(key + ":value").appendIndex(i), referenceMap);
            value.append(keyResult, valueResult);
        }
        return true;
    }
}
