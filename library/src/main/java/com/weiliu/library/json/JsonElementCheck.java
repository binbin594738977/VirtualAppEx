package com.weiliu.library.json;

import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by qumiao on 2016/8/30.
 */
public class JsonElementCheck {

    public static final boolean DEBUG = true;


    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS;
    private static final Map<Class<?>, Class<?>> WRAPPERS_TO_PRIMITIVES;

    static {
        HashMap<Class<?>, Class<?>> map = new HashMap<>();
        map.put(boolean.class, Boolean.class);
        map.put(byte.class, Byte.class);
        map.put(char.class, Character.class);
        map.put(short.class, Short.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(float.class, Float.class);
        map.put(double.class, Double.class);
        map.put(void.class, Void.class);
        PRIMITIVES_TO_WRAPPERS = Collections.unmodifiableMap(map);
        HashMap<Class<?>, Class<?>> wrap = new HashMap<>();
        for (Map.Entry<Class<?>, Class<?>> entry : map.entrySet()) {
            wrap.put(entry.getValue(), entry.getKey());
        }
        WRAPPERS_TO_PRIMITIVES = Collections.unmodifiableMap(wrap);
    }

    private static <T> Class<T> wrap(Class<T> c) {
        //noinspection unchecked
        return PRIMITIVES_TO_WRAPPERS.containsKey(c) ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
    }

    private static <T> Class<T> unwrap(Class<T> c) {
        //noinspection unchecked
        return WRAPPERS_TO_PRIMITIVES.containsKey(c) ? (Class<T>) WRAPPERS_TO_PRIMITIVES.get(c) : c;
    }



    public static void checkType(Type type, JsonElement element) {
        checkType(type, null, null, 0, element, null);
    }

    public static void checkType(Type type, String fieldOwner, String fieldName, int modifiers,
                                 JsonElement element, String elementOwner) {
        if (!DEBUG) {
            return;
        }
        // transient和static修饰的field不参与检查，直接跳过
        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
            return;
        }

        if (type instanceof Class) {
            Class cls = unwrap((Class) type);
            if (cls.isPrimitive() && !element.isJsonPrimitive()
                    || cls.isArray() && !isJsonArrayOrNull(element)
                    || cls != String.class && !cls.isPrimitive() && !cls.isArray() && !isJsonObjectOrNull(element)) {
                throwException(type, fieldOwner, fieldName, elementOwner, element.toString());
            } else if (element.isJsonObject()) {    // 普通对象 --> JsonObject
                JsonObject object = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    String name = entry.getKey();
                    Field field = getField(cls, name);
                    if (field != null) {
                        checkType(field.getGenericType(), append(fieldOwner, cls), name, field.getModifiers(),
                                entry.getValue(), append(elementOwner, name));
                    }
                }
            } else if (element.isJsonArray()) {    // 普通数组 --> JsonArray
                JsonArray array = element.getAsJsonArray();
                Class itemClass = cls.getComponentType();
                if (itemClass == null) {
                    throwException(type, fieldOwner, fieldName, elementOwner, element.toString());
                }
                for (int i = 0; i < array.size(); i++) {
                    JsonElement itemElement = array.get(i);
                    checkType(itemClass, append(fieldOwner, cls), itemClass.getSimpleName(), 0,
                            itemElement, append(elementOwner, i));
                }
            } else if (cls.isPrimitive() && element.isJsonPrimitive()) {    // 这里String类型也是isJsonPrimitive
                Class wrappedClass = wrap(cls);
                Method method = getMethod(wrappedClass, "valueOf", String.class);
                try {
                    if (method != null) {
                        method.invoke(null, element.getAsString());
                    }
                } catch (Exception e) {
                    throwException(type, fieldOwner, fieldName, elementOwner, element.toString());
                }
            }
        } else if (type instanceof ParameterizedType) {
            Class cls = (Class) ((ParameterizedType) type).getRawType();
            if (Collection.class.isAssignableFrom(cls) && !isJsonArrayOrNull(element)
                    || Map.class.isAssignableFrom(cls) && !isJsonObjectOrNull(element)) {
                throwException(type, fieldOwner, fieldName, elementOwner, element.toString());
            } else if (element.isJsonObject()) {    // Map --> JsonObject
                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                JsonObject object = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    checkType(valueType, append(fieldOwner, cls), valueType.toString(), 0,
                            entry.getValue(), append(elementOwner, entry.getKey()));
                }
            } else if (element.isJsonArray()) {    // Collection --> JsonArray
                JsonArray array = element.getAsJsonArray();
                Type itemType = ((ParameterizedType) type).getActualTypeArguments()[0];
                for (int i = 0; i < array.size(); i++) {
                    JsonElement itemElement = array.get(i);
                    checkType(itemType, append(fieldOwner, cls), itemType.toString(), 0,
                            itemElement, append(elementOwner, i));
                }
            }
        }
    }


    private static boolean isJsonArrayOrNull(JsonElement element) {
        return element.isJsonArray() || element.isJsonNull();
    }

    private static boolean isJsonObjectOrNull(JsonElement element) {
        return element.isJsonObject() || element.isJsonNull();
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

    private static Method getMethod(Class<?> cls, String methodName, Class<?>... parameterType) {
        Class<?> theClass = cls;
        for (; theClass != Object.class; theClass = theClass.getSuperclass()) {
            try {
                return theClass.getDeclaredMethod(methodName, parameterType);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String append(String str, Object value) {
        if (value instanceof Class) {
            value = ((Class) value).getSimpleName();
        }
        if (TextUtils.isEmpty(str)) {
            return "" + value;
        }
        return str + " -> " + value;
    }

    private static void throwException(Type type, String fieldOwner, String fieldName, String elementOwner, String msg) {
        String f = "%s （ 类：%s，属性：%s ）  --->   JSON  ( %s  %s )";
        throw new RuntimeException(String.format(f, type, fieldOwner, fieldName, elementOwner, msg));
    }


}
