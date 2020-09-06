package com.weiliu.library.sr;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作者：qumiao
 * 日期：2017/3/3 11:50
 * 说明：Map类在savedInstanceState中的保存与恢复逻辑
 */

class MapSaveRestoreImpl implements ContainerSaveRestore<Map> {
    @SuppressWarnings("unchecked")
    @Override
    public void copyElement(@NonNull Map fieldObj, @Nullable Map newValue) {
        fieldObj.clear();
        if (newValue != null) {
            fieldObj.putAll(newValue);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Map value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        Bundle[] keyBundleArray = new Bundle[value.size()];
        Bundle[] valueBundleArray = new Bundle[value.size()];
        Type[] argumentTypes = ((ParameterizedType) fieldType).getActualTypeArguments();
        int i = 0;
        for (Map.Entry entry : (Set< Map.Entry>) value.entrySet()) {
            Object entryKey = entry.getKey();
            Bundle keyBundle = new Bundle();
            if (!SaveRestoreUtil.save(keyBundle, key, argumentTypes[0], entryKey, fieldOwner,
                    new SavePath(path).appendKey(key).appendIndex(i), referenceMap)) {
                return false;
            }
            Object entryValue = entry.getValue();
            Bundle valueBundle = new Bundle();
            if (!SaveRestoreUtil.save(valueBundle, key, argumentTypes[1], entryValue, fieldOwner,
                    new SavePath(path).appendKey(key + ":value").appendIndex(i), referenceMap)) {
                return false;
            }

            keyBundleArray[i] = keyBundle;
            valueBundleArray[i] = valueBundle;
            i++;
        }

        bundle.putParcelableArray(key, keyBundleArray);
        bundle.putParcelableArray(key + ":value", valueBundleArray);

        //存储value的原类型名，以便恢复
        bundle.putString(key + ":class", value.getClass().getName());

        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Map createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                              @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        String className = bundle.getString(key + ":class");
        return (Map) Class.forName(className).newInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull Map value, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        Parcelable[] keyBundleArray = bundle.getParcelableArray(key);
        Parcelable[] valueBundleArray = bundle.getParcelableArray(key + ":value");
        Type[] argumentTypes = ((ParameterizedType) fieldType).getActualTypeArguments();

        int length = keyBundleArray.length;
        for (int i = 0; i < length; i++) {
            Bundle keyBundle = (Bundle) keyBundleArray[i];
            Bundle valueBundle = (Bundle) valueBundleArray[i];
            Object keyResult = SaveRestoreUtil.restore(keyBundle, key, argumentTypes[0], fieldOwner,
                    new SavePath(path).appendKey(key).appendIndex(i), referenceMap);
            Object valueResult = SaveRestoreUtil.restore(valueBundle, key, argumentTypes[1], fieldOwner,
                    new SavePath(path).appendKey(key + ":value").appendIndex(i), referenceMap);
            value.put(keyResult, valueResult);
        }
        return true;
    }
}
