package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.weiliu.library.json.JsonInterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/9 14:31
 * 说明：
 */

class JsonSaveRestoreImpl implements SaveRestore<JsonInterface> {

    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull JsonInterface value,
                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        // 如果JsonInterface还实现了基本序列化类型（比如Parcelable、Serializable），那就直接应用序列化的方式
        TypeUtil.NormalType normalType = TypeUtil.hitNormalType(value.getClass());
        if (normalType != null) {
            Method method = Bundle.class.getMethod(normalType.putMethod, String.class, normalType.classes[0]);
            method.invoke(bundle, key, value);
            return true;
        }

        Bundle temp = new Bundle();
        Class<?> cls = value.getClass();
        while (cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!SaveRestoreUtil.saveToBundle(temp, field.toString(), field, value,
                        new SavePath(path).appendKey(key), referenceMap)) {
                    return false;
                }
            }
            cls = cls.getSuperclass();
        }
        bundle.putBundle(key, temp);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public JsonInterface createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                                        @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        Class<? extends JsonInterface> type = (Class<? extends JsonInterface>) fieldType;
        // 如果JsonInterface还实现了基本序列化类型（比如Parcelable、Serializable），那就直接应用序列化的方式
        TypeUtil.NormalType normalType = TypeUtil.hitNormalType(type);
        if (normalType != null) {
            return (JsonInterface) bundle.get(key);
        }

        // 有些自定义的JsonInterface类没有public构造方法，所以不能直接调用Class.newInstance
//        T value = type.newInstance();
        Constructor<? extends JsonInterface> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull JsonInterface value, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        // 如果JsonInterface还实现了基本序列化类型（比如Parcelable、Serializable），那就直接应用序列化的方式
        TypeUtil.NormalType normalType = TypeUtil.hitNormalType((Class<?>) fieldType);
        if (normalType != null) {
            return true;
        }

        Bundle subBundle = bundle.getBundle(key);

        Class<?> cls = (Class<?>) fieldType;
        while (cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!SaveRestoreUtil.restoreFromBundle(subBundle, field.toString(), field, value,
                        new SavePath(new SavePath(path).appendKey(key)), referenceMap)) {
                    return false;
                }
            }
            cls = cls.getSuperclass();
        }

        return true;
    }
}
