package library;

/**
 *
 * Created by qumiao on 2018/3/7.
 */
public class JniHook {

    static {
        System.loadLibrary("wutahook");
    }

    public static native void hookNativeMethods(Object[] methods);

    private static native void nativeMark();
}
