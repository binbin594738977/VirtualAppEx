package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 10:32
 * 说明：Collection类在savedInstanceState中的保存与恢复逻辑
 */

class CollectionSaveRestoreImpl implements ContainerSaveRestore<Collection> {
    @SuppressWarnings("unchecked")
    @Override
    public void copyElement(@NonNull Collection fieldObj, @Nullable Collection newValue) {
        fieldObj.clear();
        if (newValue != null) {
            fieldObj.addAll(newValue);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType,
                        @NonNull Collection value, @NonNull Object fieldOwner,
                        @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        ParameterizedType type = (ParameterizedType) fieldType;
        Class<? extends Collection> rawType = (Class<? extends Collection>) type.getRawType();
        Type argumentType = type.getActualTypeArguments()[0];
        if (!(argumentType instanceof Class)) {
            if (argumentType instanceof ParameterizedType) {
                ArrayList<Bundle> tempList = new ArrayList<>(value.size());
                int i = 0;
                // 递归保存每个元素
                for (Object element : value) {
                    Bundle temp = new Bundle();
                    if (!SaveRestoreUtil.save(temp, key, argumentType, element, fieldOwner,
                            new SavePath(path).appendKey(key).appendIndex(i), referenceMap)) {
                        return false;
                    }
                    tempList.add(temp);
                    i++;
                }
                bundle.putParcelableArrayList(key, tempList);
                return true;
            }

            if (argumentType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) argumentType;
                /*
                转换成上界型（如 List<? extends Map<String, String>> 转换成 List<Map<String, String>>），
                然后就能继续执行后续逻辑。
                 */
                ParameterizedType createdType = createParameterizedType(
                        type.getOwnerType(), type.getRawType(), wildcardType.getUpperBounds()[0]);
                return save(bundle, key, createdType, value, fieldOwner, path, referenceMap);

            }

            if (argumentType instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) argumentType;
                if (typeVariable.getBounds().length == 1) {
                    /*
                    转换成上界型（如 List<T extends Map<String, String>> 转换成 List<Map<String, String>>），
                    然后就能继续执行后续逻辑。
                    */
                    ParameterizedType createdType = createParameterizedType(
                            type.getOwnerType(), type.getRawType(), typeVariable.getBounds()[0]);
                    return save(bundle, key, createdType, value, fieldOwner, path, referenceMap);
                }

            }

            return false;
        }

        Class<?> parameterType = (Class<?>) argumentType;
        TypeUtil.ListType listType = TypeUtil.hitListType(parameterType);

        if (listType != null) { //可通过Bundle现有的List方法进行保存
            Method method = Bundle.class.getMethod(listType.putMethod, String.class, ArrayList.class);
            if (ArrayList.class.isAssignableFrom(rawType)) {
                method.invoke(bundle, key, value);
            } else {    //不是ArrayList类型的，转为ArrayList来保存
                ArrayList tempList = new ArrayList();
                tempList.addAll(value);
                method.invoke(bundle, key, tempList);
                //存储value的原类型名，以便恢复
                bundle.putString(key + ":class", value.getClass().getName());
            }
            return true;
        }

        ArrayList<Bundle> tempList = new ArrayList<>();
        int i = 0;
        for (Object element : value) {
            Bundle temp = new Bundle();
            if (!SaveRestoreUtil.save(temp, key, parameterType, element, fieldOwner,
                    new SavePath(path).appendKey(key).appendIndex(i), referenceMap)) {
                return false;
            }
            tempList.add(temp);
            i++;
        }
        bundle.putParcelableArrayList(key, tempList);
        //存储value的原类型名，以便恢复
        bundle.putString(key + ":class", value.getClass().getName());
        return true;
    }

    @Nullable
    @Override
    public Collection createInstance(@NonNull Bundle bundle, @NonNull String key, @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        String className = bundle.getString(key + ":class");
        ArrayList value = (ArrayList) bundle.get(key);
        if (value == null) {
            return null;
        }

        Collection result;
        if (className != null) {
            result = (Collection) Class.forName(className).newInstance();
        } else {
            result = new ArrayList();
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public boolean restore(@NonNull Bundle bundle, @NonNull String key,
                           @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull Collection value,
                           @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        ArrayList savedList = (ArrayList) bundle.get(key);
        ParameterizedType type = (ParameterizedType) fieldType;
        Type argumentType = type.getActualTypeArguments()[0];
        if (!(argumentType instanceof Class)) {
            if (argumentType instanceof ParameterizedType) {
                int i = 0;
                // 递归保存每个元素
                for (Bundle temp : (ArrayList<Bundle>) savedList) {
                    value.add(SaveRestoreUtil.restore(temp, key, argumentType, fieldOwner,
                            new SavePath(path).appendKey(key).appendIndex(i), referenceMap));
                    i++;
                }
                return true;
            }

            if (argumentType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) argumentType;
                /*
                转换成上界型（如 List<? extends Map<String, String>> 转换成 List<Map<String, String>>），
                然后就能继续执行后续逻辑。
                 */
                ParameterizedType createdType = createParameterizedType(
                        type.getOwnerType(), type.getRawType(), wildcardType.getUpperBounds()[0]);
                return restore(bundle, key, createdType, fieldOwner, value, path, referenceMap);
            }

            if (argumentType instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) argumentType;
                if (typeVariable.getBounds().length == 1) {
                    /*
                    转换成上界型（如 List<T extends Map<String, String>> 转换成 List<Map<String, String>>），
                    然后就能继续执行后续逻辑。
                    */
                    ParameterizedType createdType = createParameterizedType(
                            type.getOwnerType(), type.getRawType(), typeVariable.getBounds()[0]);
                    return restore(bundle, key, createdType, fieldOwner, value, path, referenceMap);
                }

            }

            return false;
        }

        Class<?> parameterType = (Class<?>) argumentType;
        TypeUtil.ListType listType = TypeUtil.hitListType(parameterType);
        if (listType != null) {
            value.addAll(savedList);
        } else {
            int i = 0;
            for (Bundle temp : (ArrayList<Bundle>) savedList) {
                value.add(SaveRestoreUtil.restore(temp, key, parameterType, fieldOwner,
                        new SavePath(path).appendKey(key).appendIndex(i), referenceMap));
                i++;
            }
        }

        return true;
    }

    private static ParameterizedType createParameterizedType(
            final Type ownerType, final Type rawType, final Type... argumentTypes) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return argumentTypes;
            }

            @Override
            public Type getOwnerType() {
                return ownerType;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }
        };
    }
}
