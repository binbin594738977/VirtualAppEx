package com.weiliu.library.sr;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/7 20:36
 * 说明：
 */

public class ArraySaveRestoreImpl implements ContainerSaveRestore<Object> {

    public static final ArraySaveRestoreImpl INSTANCE = new ArraySaveRestoreImpl();

    private ArraySaveRestoreImpl() {
        //no instance
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public void copyElement(@NonNull Object fieldObj, @Nullable Object newValue) {
        if (newValue == null) {
            return;
        }
        System.arraycopy(newValue, 0, fieldObj, 0, Array.getLength(newValue));
    }

    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        int length = Array.getLength(value);
        Bundle[] elementBundles = new Bundle[length];
        if (length > 0) {
            GenericArrayType arrayType = (GenericArrayType) fieldType;
            Type componentType = arrayType.getGenericComponentType();
            for (int i = 0; i < length; i++) {
                elementBundles[i] = new Bundle();
                if (!SaveRestoreUtil.save(elementBundles[i], key, arrayType.getGenericComponentType(),
                        Array.get(value, i), fieldOwner, new SavePath(path).appendKey(key).appendIndex(i), referenceMap)) {
                    return false;
                }
            }
        }

        bundle.putParcelableArray(key, elementBundles);
        return true;
    }

    @Nullable
    @Override
    public Object createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        Object result;
        Type componentType;

        Parcelable[] elementBundles = bundle.getParcelableArray(key);

        if (fieldType instanceof Class) {
            componentType = ((Class) fieldType).getComponentType();
            result = Array.newInstance((Class<?>) componentType, elementBundles.length);
        } else {
            GenericArrayType arrayType = (GenericArrayType) fieldType;
            componentType = arrayType.getGenericComponentType();
            Class<?> cls = null;
            if (componentType instanceof Class) {
                cls = (Class<?>) componentType;
            } else if (componentType instanceof ParameterizedType) {
                cls = (Class<?>) ((ParameterizedType) componentType).getRawType();
            } else if (componentType instanceof WildcardType) {
                cls = (Class<?>) ((WildcardType) componentType).getUpperBounds()[0];
            } else if (componentType instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) componentType;
                if (typeVariable.getBounds().length == 1) {
                    cls = (Class<?>) typeVariable.getBounds()[0];
                }
            } else if (componentType instanceof GenericArrayType) {
                cls = Object[][].class;
            } else {
                return null;
            }

            result = Array.newInstance(cls, elementBundles.length);
        }
        return result;
    }

    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                           @NonNull Object fieldOwner, @NonNull Object value,
                           @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        Parcelable[] elementBundles = bundle.getParcelableArray(key);
        Type componentType;
        if (fieldType instanceof Class) {
            componentType = ((Class) fieldType).getComponentType();
        } else {
            GenericArrayType arrayType = (GenericArrayType) fieldType;
            componentType = arrayType.getGenericComponentType();
        }
        for (int i = 0; i < elementBundles.length; i++) {
            Bundle elementBundle = (Bundle) elementBundles[i];
            Array.set(value, i, SaveRestoreUtil.restore(elementBundle, key, componentType, fieldOwner,
                    new SavePath(path).appendKey(key).appendIndex(i), referenceMap));
        }
        return true;
    }
}
