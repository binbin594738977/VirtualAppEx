package library;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClassUtil {
    /**
     * 生产一个构造方法带参的实例
     *
     * @param classParam  :是参数类型：类类型
     * @param paramValues ：是参数值
     */
    public static Object getInstance(Class c, Class[] classParam, Object[] paramValues) throws Exception {
        Object obj = null;
        //调用构造方法,创建对象的Constructor对象，用他来获取构造方法的信息：即用其调用构造方法创建实例
        Constructor con;
        try {
            con = c.getConstructor(classParam);
        } catch (NoSuchMethodException e) {
            con = c.getDeclaredConstructor(classParam);
            con.setAccessible(true);
        }
        //调用构造方法并创建实例
        obj = con.newInstance(paramValues);
        return obj;
    }
    /**
     * 生产一个构造方法带参的实例
     */
    public static Object getInstance(Class c) throws Exception {
        Object obj = null;
        //调用构造方法,创建对象的Constructor对象，用他来获取构造方法的信息：即用其调用构造方法创建实例
        Constructor con;
        try {
            con = c.getConstructor(new Class[]{});
        } catch (NoSuchMethodException e) {
            con = c.getDeclaredConstructor(new Class[]{});
            con.setAccessible(true);
        }
        //调用构造方法并创建实例
        obj = con.newInstance(new Object[]{});
        return obj;
    }
    /**
     * 调用静态方法
     *
     * @param methodName      方法名
     * @param methodType      方法参数type
     * @param methodParameter 方法参数
     */
    public static Object invokeStaticMethod(Class clazz, String methodName, Class[] methodType, Object[] methodParameter) throws Exception {
        Method gf = clazz.getDeclaredMethod(methodName, methodType);
        gf.setAccessible(true);
        return gf.invoke(null, methodParameter);
    }
    /**
     * 调用静态方法
     *
     * @param methodName 方法名
     */
    public static Object invokeStaticMethod(Class clazz, String methodName) throws Exception {
        Method gf = clazz.getDeclaredMethod(methodName);
        gf.setAccessible(true);
        return gf.invoke(null);
    }
    /**
     * 调用方法
     *
     * @param obj             调用的对象
     * @param methodName      方法名
     * @param methodType      方法参数type
     * @param methodParameter 方法参数
     */
    public static Object invokeMethod(Object obj, String methodName, Class[] methodType, Object[] methodParameter) throws Exception {
        Object[] objects = new Object[1];
        Utility.invokeMethod(obj, methodName, methodType, methodParameter, objects);
        return objects[0];
    }
    /**
     * 调用方法
     *
     * @param obj        调用的对象
     * @param methodName 方法名
     */
    public static Object invokeMethod(Object obj, String methodName) throws Exception {
        Object[] objects = new Object[1];
        Utility.invokeMethod(obj, methodName, new Class[]{}, new Object[]{}, objects);
        return objects[0];
    }
    /**
     * @param obj       调用的对象
     * @param fieldName 属性名
     * @return
     * @throws Exception
     */
    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field qMv = Utility.getField(obj, fieldName);
        qMv.setAccessible(true);
        return qMv.get(obj);
    }
    /**
     * @param fieldName 属性名
     * @return
     * @throws Exception
     */
    public static Object getStaticFieldValue(Class clazz, String fieldName) throws Exception {
        Field qMv = clazz.getDeclaredField(fieldName);
        qMv.setAccessible(true);
        return qMv.get(null);
    }
    /**
     * @param obj       调用的对象
     * @param value     设置的对象
     * @param fieldName 属性名
     * @return
     * @throws Exception
     */
    public static void setFieldValue(Object obj, Object value, String fieldName) throws Exception {
        Field qMv = Utility.getField(obj, fieldName);
        qMv.setAccessible(true);
        qMv.set(obj, value);
    }
    /**
     * @param value     设置的对象
     * @param fieldName 属性名
     * @return
     * @throws Exception
     */
    public static void setStaticField(Class clazz, Object value, String fieldName) throws Exception {
        Field qMv = clazz.getDeclaredField(fieldName);
        qMv.setAccessible(true);
        qMv.set(null, value);
    }
}
