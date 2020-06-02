package library;

import java.lang.reflect.Field;

/**
 *
 * Created by qumiao on 2018/3/20.
 */

public class FieldUtils {


    public static Object getField(String clazzName, Object target, String name) throws Exception {
        return getField(Class.forName(clazzName), target, name);
    }

    public static Object getField(Class clazz, Object target, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    public static void setField(String clazzName, Object target, String name, Object value) throws Exception {
        setField(Class.forName(clazzName), target, name, value);
    }

    public static void setField(Class clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

}
