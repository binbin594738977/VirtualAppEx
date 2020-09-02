package library;

import com.lody.virtual.client.core.VirtualCore;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;

import library.util.ArrayUtil;

/**
 * Created by qumiao on 2018/3/7.
 */
public class JniHook {

    static {
        System.loadLibrary("jnihook");
    }


    public static void hookNative(ClassLoader classLoader) throws Exception {
        //hook加载dex的native函数
        Class c = classLoader.loadClass("dalvik.system.DexPathList$Element");
        Object[] newTesters = (Object[]) Array.newInstance(c, 1);
        Class cls = classLoader.loadClass("dalvik.system.DexFile");
        Method openDexFileNative = cls.getDeclaredMethod("openDexFileNative",
                String.class,
                String.class,
                int.class,
                ClassLoader.class,
                newTesters.getClass());
        openDexFileNative.setAccessible(true);
        Method[] methodList = {openDexFileNative};
        JniHook.hookNativeMethods(methodList);
    }


    public static void hookGetDexNative(ClassLoader classLoader) {
        try {
            Class cls = classLoader.loadClass("java.lang.DexCache");
            Method getDexNative = cls.getDeclaredMethod("getDexNative");
            getDexNative.setAccessible(true);
            Method[] methodList = {getDexNative};
            JniHook.hookNativeMethods(methodList);
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    public static Object on_getDexNative_before(Object Object, boolean[] hooked) {
        return false;
    }

    public static Object on_getDexNative_after(Object object, Object returnObj, boolean[] hooked) {
        try {
            String name = "";
            if (object != null) {
                String location = (String) ClassUtil.getFieldValue(object, "location");
                int index = location.length() - 12;
                name = location.substring(index);
                WeiliuLog.log("location: " + location);
            }
            String processName = Utility.getCurProcessName(VirtualCore.get().getContext());
            WeiliuLog.log("进程名: " + processName);
            byte[] getBytes = (byte[]) ClassUtil.invokeMethod(returnObj, "getBytes");
            WeiliuLog.log("on_getDexNative_after: " + getArrayString(getBytes));
            File file = new File(Utility.getDefaultFileDirectory() + "/dexCach/" + processName);
            if (!file.exists()) {
                file.mkdirs();
            }
            Utility.bytesToFile(getBytes, new File(file, name));
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
        return false;
    }

    public static native void hookNativeMethods(Object[] methods);

    private static native void nativeMark();

    static int index = 0;

    public static java.lang.Object on_openDexFileNative_before(String p0, String p1, int p2, ClassLoader p3, Object[] p4, boolean[] hooked) {
        File file = new File(p0);
        if (file.exists()) {
            WeiliuLog.log(3, "file.exists");
            File file1 = new File(Utility.getDefaultFileDirectory() + "/hfx/" + index + file.getName());
            WeiliuLog.log(3, "newfilepath" + file1);
            FileUtil.copy(file, file1);
        }
        index++;
        WeiliuLog.log("on_openDexFileNative_before() called with: p0 = [" + p0 + "], p1 = [" + p1 + "], p2 = [" + p2 + "], p3 = [" + p3 + "], p4 = [" + Arrays.toString(p4) + "]");
        return null;
    }

    public static java.lang.Object on_openDexFileNative_after(String p0, String p1, int p2, ClassLoader p3, Object[] p4, Object returnObj, boolean[] hooked) {
        WeiliuLog.log("on_openDexFileNative_after() called with: p0 = [" + p0 + "], p1 = [" + p1 + "], p2 = [" + p2 + "], p3 = [" + p3 + "], p4 = [" + p4 + "], returnObj = [" + returnObj + "]]");
        return null;
    }

    private static String getArrayString(Object object) {
        if (object == null) {
            return null;
        }
        if (object.getClass().isArray()) {
            return ArrayUtil.deepToString(object);
        }
        return object.toString();
    }
}
