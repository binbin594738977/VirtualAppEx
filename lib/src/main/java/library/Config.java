package library;

import com.lody.virtual.BuildConfig;

public class Config {
    public static final boolean DEV = BuildConfig.DEBUG;

    public static final String WS_URL;
    public static final String HTTP_URL;

    static {
        if (DEV) {
            WS_URL = "ws://dev-test-tklws.faquange.cn/";
            HTTP_URL = "http://dev-test.alimama.faquange.cn/index.php";
        } else {
            WS_URL = "ws://tklws.faquange.cn/";
            HTTP_URL = "http://alimama.faquange.cn/index.php";
        }
    }
}
