package library;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

/**
 * 网络相关方法
 */
public class NetUtil {
    /**
     * tag
     */
    private static final String TAG = "NetUtil";
    /**
     * debug
     */
    private static final boolean DEBUG = false;
    private NetUtil() {
    }
    public static boolean enable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }
    public static int getNetTypeInt(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {    //没有网络,属于WIFI情况下。
            return -1;
        }
        return networkInfo.getType();
    }
    /**
     * 判断当前是否为WIFI环境
     *
     * @return
     */
    public static boolean isWifi(Context context) {
        return getNetTypeInt(context) == ConnectivityManager.TYPE_WIFI;
    }
    /**
     * APN（接入点）查询的URI.
     */
    public static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
    /**
     * 检查当前网络类型。
     *
     * @param context context
     * @return wifi, cmnet, uninet, ctnet, cmwap ……
     */
    public static String getNetType(Context context) {
        String netType = null;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null) {
            if (DEBUG) {
                Log.d(TAG, "network type : " + activeNetInfo.getTypeName().toLowerCase());
            }
//            if ("wifi".equals(activeNetInfo.getTypeName().toLowerCase())) {
//            	netType = "wifi";
//            } else {
//            	netType = checkApn(context);
//            }
            netType = activeNetInfo.getTypeName()/* + "-" + activeNetInfo.getSubtypeName()*/;
        }
        return netType;
    }
}