package com.weiliu.library.sr;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.weiliu.library.json.JsonInterface;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 16:29
 * 说明：
 */

class TypeUtil {
    private TypeUtil() {
        //no instance
    }

    /**
     * 获取Type不带包名的名称。
     * @param type
     * @return Type不带包名的名称，如 Map&lt;? extends List&lt;Collection&lt;D&gt&gt;, Integer[]&gt;。
     */
    static String getSimpleNameForType(Type type) {
        if (type instanceof Class) {
            return ((Class) type).getSimpleName();
        }

        if (type instanceof ParameterizedType) {
            StringBuilder sb = new StringBuilder();
            sb.append(((Class) ((ParameterizedType) type).getRawType()).getSimpleName());
            sb.append("<");
            boolean appendComma = false;
            for (Type argumentType : ((ParameterizedType) type).getActualTypeArguments()) {
                if (!appendComma) {
                    appendComma = true;
                } else {
                    sb.append(", ");
                }
                sb.append(getSimpleNameForType(argumentType));
            }
            sb.append(">");
            return sb.toString();
        }

        if (type instanceof WildcardType) {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            WildcardType wildcardType = (WildcardType) type;
            Type[] lowerTypes = wildcardType.getLowerBounds();
            if (lowerTypes.length > 0) {
                sb.append(" super ");
                boolean appendAnd = false;
                for (Type lowerType : lowerTypes) {
                    if (!appendAnd) {
                        appendAnd = true;
                    } else {
                        sb.append(" & ");
                    }
                    sb.append(getSimpleNameForType(lowerType));
                }
            } else {
                Type[] upperTypes = wildcardType.getUpperBounds();
                if (upperTypes.length > 0 && !(upperTypes.length == 1 && upperTypes[0] == Object.class)) {
                    sb.append(" extends ");
                    boolean appendAnd = false;
                    for (Type upperType : upperTypes) {
                        if (!appendAnd) {
                            appendAnd = true;
                        } else {
                            sb.append(" & ");
                        }
                        sb.append(getSimpleNameForType(upperType));
                    }
                }
            }
            return sb.toString();
        }
        return type.toString();
    }

    /**
     * 查询cls是否为可支持的泛型容器的原生类型
     * @param cls 待查询的类型
     * @return 非null表示true；null表示false
     */
    static GenericContainerType hitGenericContainerType(Class<?> cls) {
        for (GenericContainerType genericContainerType : GenericContainerType.values()) {
            //noinspection unchecked
            if (genericContainerType.cls.isAssignableFrom(cls)) {
                return genericContainerType;
            }
        }
        return null;
    }

    /**
     * type是否为可支持的泛型容器的有效参数类型
     * @param type
     * @return
     */
    static boolean isGenericContainerArgumentType(Type type) {
        if (type instanceof GenericArrayType) {
            return isGenericContainerArgumentType(((GenericArrayType) type).getGenericComponentType());
        }

        if (type instanceof Class) {
            Class<?> cls = (Class<?>) type;

            return hitNormalType(cls) != null || hitTranslateType(cls) != null;
        }

        if (type instanceof ParameterizedType) {
            Class<?> rawType = (Class<?>) ((ParameterizedType) type).getRawType();
            if (hitGenericContainerType(rawType) == null) {
                return false;
            }
            for (Type argumentType : ((ParameterizedType) type).getActualTypeArguments()) {
                if (!isGenericContainerArgumentType(argumentType)) {
                    return false;
                }
            }
            return true;
        }

        if (type instanceof WildcardType) {
            Type[] upperBoundTypes = ((WildcardType) type).getUpperBounds();
            if (upperBoundTypes.length == 0) {
                return false;
            }
            return isGenericContainerArgumentType(upperBoundTypes[0]);
        }

        if (type instanceof TypeVariable) {
            Type[] upperBoundTypes = ((TypeVariable) type).getBounds();
            if (upperBoundTypes == null || upperBoundTypes.length != 1) {
                return false;
            }
            return isGenericContainerArgumentType(upperBoundTypes[0]);
        }

        return false;
    }

    /**
     * 查询cls是否为可支持的容器类型
     * @param cls 待查询的类型
     * @return 非null表示true；null表示false
     */
    static ContainerType hitContainerType(Class<?> cls) {
        for (ContainerType containerType : ContainerType.values()) {
            //noinspection unchecked
            if (containerType.cls.isAssignableFrom(cls)) {
                return containerType;
            }
        }
        return null;
    }

    /**
     * 查询cls是否为可支持的其它类型
     * @param cls 待查询的类型
     * @return 非null表示true；null表示false
     */
    static ExtraType hitExtraType(Class<?> cls) {
        for (ExtraType extraType : ExtraType.values()) {
            //noinspection unchecked
            if (extraType.cls.isAssignableFrom(cls)) {
                return extraType;
            }
        }

        return null;
    }

    /**
     * 查询cls是否为简单转换后可支持的类型
     * @param cls 待查询的类型
     * @return 非null表示true；null表示false
     */
    static TranslateType hitTranslateType(Class<?> cls) {
        for (TranslateType translateType : TranslateType.values()) {
            //noinspection unchecked
            if (translateType.cls.isAssignableFrom(cls)) {
                return translateType;
            }
        }

        return null;
    }


    /**
     * 查询给定的类型是否为 Bundle直接支持的最基本类型
     * @param cls 待查询的类型
     * @return 非null表示true；null表示false
     */
    static NormalType hitNormalType(Class<?> cls) {
        for (NormalType normalType : NormalType.values()) {
            boolean hit = false;
            for (Class<?> typeCls : normalType.classes) {
                if (typeCls == cls || typeCls.isAssignableFrom(cls)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                return normalType;
            }
        }
        return null;
    }

    /**
     * 根据元素类型查询list是否为 Bundle直接支持的List类型
     * @param elementClass 待查询list的元素类型
     * @return 非null表示true；null表示false
     */
    static ListType hitListType(Class<?> elementClass) {
        for (ListType listType : ListType.values()) {
            boolean hit = false;
            for (Class<?> typeCls : listType.classes) {
                if (typeCls == elementClass || typeCls.isAssignableFrom(elementClass)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                return listType;
            }
        }
        return null;
    }

    static Class<?> getClassFromType(Type type) {
        Class<?> cls;
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof Class) {
            return (Class<?>) type;
        }
        return null;
    }

    /**
     * 根据类型寻找对应的Save Restore 解决方案
     * @param type 类型
     * @return 如果不为null，表示找到
     */
    @Nullable
    static SaveRestore matchSaveRestoreImpl(Type type) {
        if (type instanceof GenericArrayType) {
            return ArraySaveRestoreImpl.INSTANCE;
        }

        Class<?> cls = getClassFromType(type);
        if (cls == null) {
            return null;
        }

        TypeUtil.GenericContainerType gct = TypeUtil.hitGenericContainerType(cls);
        if (gct != null) {
            return gct.impl;
        }

        TypeUtil.ContainerType ct = TypeUtil.hitContainerType(cls);
        if (ct != null) {
            return ct.impl;
        }

        TypeUtil.TranslateType tt = TypeUtil.hitTranslateType(cls);
        if (tt != null) {
            return tt.impl;
        }

        TypeUtil.ExtraType et = TypeUtil.hitExtraType(cls);
        if (et != null) {
            return et.impl;
        }

        return null;
    }


    /**
     * Bundle直接支持的最基本类型
     */
    enum NormalType {
        putBundle(Bundle.class),
        putBoolean(boolean.class, Boolean.class),
        putBooleanArray(boolean[].class),
        putByte(byte.class, Byte.class),
        putByteArray(byte[].class),
        putChar(char.class, Character.class),
        putCharArray(char[].class),
        putCharSequence(CharSequence.class),
        putCharSequenceArray(CharSequence[].class),
        putDouble(double.class, Double.class),
        putDoubleArray(double[].class),
        putFloat(float.class, Float.class),
        putFloatArray(float[].class),
        putInt(int.class, Integer.class),
        putIntArray(int[].class),
        putLong(long.class, Long.class),
        putLongArray(long[].class),
        putParcelable(Parcelable.class),
        putParcelableArray(Parcelable[].class),
        putShort(short.class, Short.class),
        putShortArray(short[].class),
        putString(String.class),
        putStringArray(String[].class),
        putSerializable(Serializable.class),
        putSparseParcelableArray(SparseArray.class),
        ;

        final Class<?>[] classes;
        final String putMethod;
        final String getMethod;

        NormalType(Class<?>... cls) {
            this.classes = cls;
            this.putMethod = name();
            this.getMethod = putMethod.replaceFirst("put", "get");
        }
    }

    /**
     * Bundle直接支持的List类型
     */
    enum ListType {
        putStringArrayList(String.class),
        putIntegerArrayList(Integer.class, int.class),
        putParcelableArrayList(Parcelable.class),
        putCharSequenceArrayList(CharSequence.class),
        ;

        final Class<?>[] classes;
        final String putMethod;
        final String getMethod;

        ListType(Class<?>... cls) {
            this.classes = cls;
            this.putMethod = name();
            this.getMethod = putMethod.replaceFirst("put", "get");
        }
    }


    /**
     * 可支持的泛型容器类型（元素必须为 {@link #isGenericContainerArgumentType(Type)} 返回true的类型）
     */
    enum GenericContainerType {
        collection(Collection.class, new CollectionSaveRestoreImpl()),
        map(Map.class, new MapSaveRestoreImpl()),
        sparseArray(SparseArray.class, new SparseArraySaveRestoreImpl()),
        sparseArrayCompat(SparseArrayCompat.class, new SparseArrayCompatSaveRestoreImpl()),
        longSparseArray(LongSparseArray.class, new LongSparseArraySaveRestoreImpl()),
        ;

        final Class cls;
        final ContainerSaveRestore impl;

        <T> GenericContainerType(Class<T> cls, ContainerSaveRestore<T> impl) {
            this.cls = cls;
            this.impl = impl;
        }
    }




    /**
     * 可支持的容器类型
     */
    enum ContainerType {
        sparseBooleanArray(SparseBooleanArray.class, new SparseBooleanArraySaveRestoreImpl()),
        sparseIntArray(SparseIntArray.class, new SparseIntArraySaveRestoreImpl()),
        ;

        final Class cls;
        final ContainerSaveRestore impl;

        <T> ContainerType(Class<T> cls, ContainerSaveRestore<T> impl) {
            this.cls = cls;
            this.impl = impl;
        }
    }

    /**
     * 简单转换后就能保存的类型
     */
    enum TranslateType {
        // 注意：有些枚举同时也继承自JsonInterface，优先按枚举处理，所以枚举类放在第一位
        enum_(Enum.class, new EnumSaveRestoreImpl()),
        jsonInterface(JsonInterface.class, new JsonSaveRestoreImpl()),
        ;

        final Class cls;
        final SaveRestore impl;

        <T> TranslateType(Class<T> cls, SaveRestore<T> impl) {
            this.cls = cls;
            this.impl = impl;
        }
    }

    /**
     * 可支持的其它类型（不可放入泛型容器（参考 {@link GenericContainerType}）中）
     */
    enum ExtraType {
        fragment(Fragment.class, new FragmentSaveRestoreImpl()),
        ;

        final Class cls;
        final SaveRestore impl;

        <T> ExtraType(Class<T> cls, SaveRestore<T> impl) {
            this.cls = cls;
            this.impl = impl;
        }
    }
}
