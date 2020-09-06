package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.Pair;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;

import com.weiliu.library.SaveState;
import com.weiliu.library.json.JsonInterface;
import com.weiliu.library.json.JsonInterfaceCheck;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bundle相关的方法集合
 * <br/>
 * Created by qumiao on 2016/8/18.
 */
public class SaveRestoreUtil {

    private static final boolean DEBUG = true;
    private static final String TAG = "SaveRestoreUtil";

    private static final Set<Class<?>> GENERIC_CONTAINER_SET;

    static {
        HashSet<Class<?>> set = new HashSet<>();
        set.add(Collection.class);
        set.add(SparseArrayCompat.class);
        set.add(LongSparseArray.class);
        GENERIC_CONTAINER_SET = Collections.unmodifiableSet(set);
    }

    private SaveRestoreUtil() {
        //no instance
    }

    /**
     * 检查属性值是否符合Save Restore的要求
     * @param key
     * @param field 属性
     * @param fieldOwner 对象
     * @throws Exception
     */
    public static void check(
            @NonNull String key, @NonNull Field field, @NonNull Object fieldOwner) throws Exception {
        if (!DEBUG) {
            return;
        }

        field.setAccessible(true);
        Type type = field.getGenericType();

        if (isGenericContainer(key, field, fieldOwner)) {
            return;
        }

        if (type instanceof Class && TypeUtil.hitContainerType((Class<?>) type) != null) {
            return;
        }

        if (type instanceof GenericArrayType
                && TypeUtil.isGenericContainerArgumentType(((GenericArrayType) type).getGenericComponentType())) {
            return;
        }

        if (Modifier.isFinal(field.getModifiers())) {
            failed(fieldOwner, key, "除了容器类型，其它类型的属性为final都无法保存与恢复");
            return;
        }

        if (type instanceof Class) {
            Class<?> cls = (Class<?>) type;

            if (TypeUtil.hitExtraType(cls) != null) {
                if (Fragment.class.isAssignableFrom(cls)) {
                    if (!(fieldOwner instanceof Fragment || fieldOwner instanceof FragmentActivity)) {
                        failed(fieldOwner, key, "Fragment只能在Fragment和FragmentActivity中才能保存与恢复。");
                    }
                }
                return;
            }

            if (TypeUtil.hitTranslateType(cls) != null) {
                return;
            }

            if (TypeUtil.hitNormalType(cls) != null) {
                return;
            }

            if (JsonInterface.class.isAssignableFrom(cls)) {
                JsonInterfaceCheck.assetType(cls);
                return;
            }
        }

        failed(fieldOwner, key, "类型不合法。");
    }

    private static boolean isGenericContainer(
            @NonNull String key, @NonNull Field field, @NonNull Object fieldOwner)
            throws Exception {
        Type genericType = field.getGenericType();
        if (genericType instanceof Class) {
            Class<?> cls = (Class<?>) genericType;
            if (TypeUtil.hitGenericContainerType(cls) != null) {
                failed(fieldOwner, key, "泛型类的数据必须明确指定泛型参数，不能直接使用原生类型。");
                return true;
            }
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class && TypeUtil.hitGenericContainerType((Class<?>) rawType) != null) {
                for (Type type : parameterizedType.getActualTypeArguments()) {
                    if (!TypeUtil.isGenericContainerArgumentType(type)) {
                        failed(fieldOwner, key,
                                String.format("泛型参数 %1$s 类型不合法。", TypeUtil.getSimpleNameForType(type)));
                    }
                }
                return true;
            }
        }

        return false;
    }


    /**
     * 从Bundle中恢复对象的属性值
     * @param bundle
     * @param key
     * @param field 属性
     * @param fieldOwner 对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 成功与否
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static boolean restoreFromBundle(@NonNull Bundle bundle, @NonNull String key,
                                            @NonNull Field field, @NonNull Object fieldOwner,
                                            @NonNull SavePath path,
                                            @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        if (Modifier.isStatic(field.getModifiers())) {
            return true;    //跳过static属性
        }
        if (!bundle.containsKey(key)) {
            return true;    //跳过未保存的属性
        }

        field.setAccessible(true);
        Class<?> cls = field.getType();

        Object newValue = restore(bundle, key, field.getGenericType(), fieldOwner, path, referenceMap);

        boolean isFinal = Modifier.isFinal(field.getModifiers());
        if (isFinal) {
            int savedPairIndex = -1;
            SavePath savedPath = new SavePath(path).appendKey(key);
            List<Pair<SavePath, Object>> pairs = referenceMap.get(savedPath);
            if (pairs != null) {
                int i = 0;
                for (Pair<SavePath, Object> pair : pairs) {
                    if (pair.second == newValue) {
                        savedPairIndex = i; //找出引用存储的位置，以便一会替换
                        break;
                    }
                    i++;
                }
            }

            Object finalValue = field.get(fieldOwner);
            TypeUtil.GenericContainerType gct = TypeUtil.hitGenericContainerType(cls);
            if (gct != null) {
                gct.impl.copyElement(finalValue, newValue);
            } else {
                TypeUtil.ContainerType ct = TypeUtil.hitContainerType(cls);
                if (ct != null) {
                    ct.impl.copyElement(finalValue, newValue);
                } else {
                    if (cls.isArray()) {
                        ArraySaveRestoreImpl.INSTANCE.copyElement(finalValue, newValue);
                    } else {
                        failed(fieldOwner, key, "除了泛型容器类型，其它类型的属性为final都无法保存与恢复");
                        return false;
                    }
                }
            }
            if (savedPairIndex != -1) {
                // 替换成finalValue，从而使保存的引用与final属性的原引用保持一致
                pairs.set(savedPairIndex, new Pair<SavePath, Object>(savedPath, finalValue));
            }
        } else {
            field.set(fieldOwner, newValue);
        }

        return true;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static Object restore(@NonNull Bundle bundle, @NonNull String key,
                          @NonNull Type type, @NonNull Object fieldOwner,
                          @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception {
        if (!bundle.containsKey(key)) {
            return null;
        }

        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
                /*
                转换成上界型（如 ? extends Map<String, String> 转换成 Map<String, String>），
                然后就能继续执行后续逻辑。
                 */
            return restore(bundle, key, wildcardType.getUpperBounds()[0], fieldOwner, path, referenceMap);
        }

        if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            if (typeVariable.getBounds().length == 1) {
                    /*
                    转换成上界型（如 T extends Map<String, String> 转换成 Map<String, String>），
                    然后就能继续执行后续逻辑。
                    */
                return restore(bundle, key, typeVariable.getBounds()[0], fieldOwner, path, referenceMap);
            }

        }

        if (bundle.getBoolean(key + ":isRef")) {
            // 循环引用，直接将已经恢复的引用进行赋值即可
            SavePath savedPath = new SavePath(bundle.getString(key));
            return SaveRestoreUtil.findObj(referenceMap, savedPath);
        }


        SaveRestore impl = TypeUtil.matchSaveRestoreImpl(type);
        if (impl != null) {
            Object value = impl.createInstance(bundle, key, type, fieldOwner, path, referenceMap);
            if (value != null) {
                SavePath currentPath = new SavePath(path).appendKey(key);
                SaveRestoreUtil.saveObj(referenceMap, currentPath, value);
                impl.restore(bundle, key, type, fieldOwner, value, path, referenceMap);
            }
            return value;
        }

        Class<?> cls = TypeUtil.getClassFromType(type);
        if (cls != null) {
            TypeUtil.NormalType normalType = TypeUtil.hitNormalType(cls);
            if (normalType != null) {
                return bundle.get(key);
            }
        }

        failed(fieldOwner, key, "类型不合法，无法恢复。");
        return null;
    }




    /**
     * 将对象的属性值存入Bundle
     * @param bundle
     * @param key
     * @param field 属性
     * @param fieldOwner 对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 成功与否
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static boolean saveToBundle(@NonNull Bundle bundle, @NonNull String key,
                                       @NonNull Field field, @NonNull Object fieldOwner,
                                       @NonNull SavePath path,
                                       @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        field.setAccessible(true);
        if (Modifier.isStatic(field.getModifiers())) {
            return true;    //static的属性直接跳过
        }

        Object value = field.get(fieldOwner);
        if (value == null) {
            return true;    //属性为null直接跳过
        }

        Bundle temp = new Bundle();
        boolean result = save(bundle, key, field.getGenericType(), value, fieldOwner, path, referenceMap);
        if (result) {
            bundle.putAll(temp);
        } else {
            failed(fieldOwner, key, "保存失败！");
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    static boolean save(@NonNull Bundle bundle, @NonNull String key, @NonNull Type type, @Nullable Object value,
                        @NonNull Object fieldOwner,
                        @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception {
        if (value == null) {
            return true;
        }

        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
                /*
                转换成上界型（如 ? extends Map<String, String> 转换成 Map<String, String>），
                然后就能继续执行后续逻辑。
                 */
            return save(bundle, key, wildcardType.getUpperBounds()[0], value, fieldOwner, path, referenceMap);
        }

        if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            if (typeVariable.getBounds().length == 1) {
                    /*
                    转换成上界型（如 T extends Map<String, String> 转换成 Map<String, String>），
                    然后就能继续执行后续逻辑。
                    */
                return save(bundle, key, typeVariable.getBounds()[0], value, fieldOwner, path, referenceMap);
            }

        }

        // 循环引用，直接保存上次引用保存的路径即可
        SavePath savedPath = SaveRestoreUtil.findPath(referenceMap, value);
        if (savedPath != null) {
            bundle.putString(key, savedPath.getPath());
            bundle.putBoolean(key + ":isRef", true);
            return true;
        }


        SaveRestore impl = TypeUtil.matchSaveRestoreImpl(type);
        if (impl != null) {
            SavePath currentPath = new SavePath(path).appendKey(key);
            SaveRestoreUtil.savePath(referenceMap, value, currentPath);
            return impl.save(bundle, key, type, value, fieldOwner, path, referenceMap);
        }

        Class<?> cls = TypeUtil.getClassFromType(type);
        if (cls != null) {
            TypeUtil.NormalType normalType = TypeUtil.hitNormalType(cls);
            if (normalType != null) {
                Method method = Bundle.class.getMethod(normalType.putMethod, String.class, normalType.classes[0]);
                method.invoke(bundle, key, value);
                return true;
            }
        }

        failed(fieldOwner, key, "类型不合法，无法保存。");
        return false;
    }


    @Nullable
    private static SavePath findPath(@NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap,
                                     @NonNull Object value) {
        List<Pair<Object, SavePath>> pairs = referenceMap.get(value);
        if (pairs == null) {
            return null;
        }
        for (Pair<Object, SavePath> pair : pairs) {
            if (pair.first == value) {
                return pair.second;
            }
        }
        return null;
    }

    @Nullable
    private static Object findObj(@NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap,
                                  @NonNull SavePath path) {
        List<Pair<SavePath, Object>> pairs = referenceMap.get(path);
        if (pairs == null) {
            return null;
        }
        for (Pair<SavePath, Object> pair : pairs) {
            if (path.equals(pair.first)) {
                return pair.second;
            }
        }
        return null;
    }

    private static void savePath(@NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap,
                                 @NonNull Object value, @NonNull SavePath path) {
        List<Pair<Object, SavePath>> pairs = referenceMap.get(value);
        if (pairs == null) {
            pairs = new ArrayList<>();
            referenceMap.put(value, pairs);
        }
        pairs.add(new Pair<Object, SavePath>(value, path));
    }

    private static void saveObj(@NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap,
                                @NonNull SavePath path, @NonNull Object value) {
        List<Pair<SavePath, Object>> pairs = referenceMap.get(path);
        if (pairs == null) {
            pairs = new ArrayList<>();
            referenceMap.put(path, pairs);
        }
        pairs.add(new Pair<SavePath, Object>(path, value));
    }



    private static Field getField(Class<?> cls, String fieldName) {
        Class<?> theClass = cls;
        for (; theClass != Object.class; theClass = theClass.getSuperclass()) {
            try {
                return theClass.getDeclaredField(fieldName);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void failed(@NonNull Object fieldOwner, @NonNull String key, String extraMessage) {
        RuntimeException exception = new RuntimeException(
                fieldOwner.getClass().getName() + " field = " + key + " : " + extraMessage);
        if (DEBUG) {
            throw exception;
        } else {
            Log.e(TAG, "", exception);
        }
    }




    /**
     * 获取需要保存的字段列表
     * @param fieldOwner 字段所属对象
     * @return
     */
    @NonNull
    public static List<Field> getSaveStateFields(@NonNull Object fieldOwner) {
        ArrayList<Field> fieldList = new ArrayList<>();

        Class<?> cls = fieldOwner.getClass();
        while (cls != Object.class) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                SaveState annotation = field.getAnnotation(SaveState.class);
                if (annotation != null) {
                    fieldList.add(field);
                }
            }
            cls = cls.getSuperclass();
        }

        Collections.sort(fieldList, SaveRestoreUtil.FIELD_COMPARATOR);
        return fieldList;
    }

    private static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
        @Override
        public int compare(Field lhs, Field rhs) {
            SaveState lhsAnnotation = lhs.getAnnotation(SaveState.class);
            SaveState rhsAnnotation = rhs.getAnnotation(SaveState.class);

            int result = lhsAnnotation.order() - rhsAnnotation.order();
            if (result == 0) {
                boolean isLeftFinal = Modifier.isFinal(lhs.getModifiers());
                boolean isRightFinal = Modifier.isFinal(rhs.getModifiers());
                if (isLeftFinal && !isRightFinal) {
                    return -1;
                }
                if (!isLeftFinal && isRightFinal) {
                    return 1;
                }
                return 0;
            }
            return result;
        }
    };
}
