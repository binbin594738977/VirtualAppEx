package library;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

/**
 *
 * 获取Mainfest文件中定义的元数据的值 工具类
 *
 */
public class ManifestMetaDataUtil {

    private ManifestMetaDataUtil() {

    }

    private static Object readKey(Context context, String keyName) {
        try {
            ApplicationInfo appi = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            Bundle bundle = appi.metaData;
            return bundle.get(keyName);
        } catch (NameNotFoundException e) {
            return null;
        }
    }
    
    public static String getString(Context context, String keyName) {
        return (String) readKey(context, keyName);
    }
    
    public static Boolean getBoolean(Context context, String keyName) {
        return (Boolean) readKey(context, keyName);
    }
    
    public static Object get(Context context, String keyName) {
        return readKey(context, keyName);
    }

    /**
     * 获取当前版本
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        return getVersionCode(context, context.getPackageName());
    }
    /**
     * 获取app版本
     * @param context
     * @param packageName app包名
     * @return
     */
    public static int getVersionCode(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getVersionName(Context context) {
        return getVersionName(context, context.getPackageName());
    }
    public static String getVersionName(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
}