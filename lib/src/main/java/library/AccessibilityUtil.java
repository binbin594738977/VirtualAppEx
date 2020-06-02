package library;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;


/**
 * 辅助功能/无障碍相关工具
 */
public class AccessibilityUtil {
    private static final String TAG = AccessibilityUtil.class.getSimpleName();

    /**
     * 检查系统设置：是否开启辅助服务
     */
    public static boolean isSettingOpen(String packgeName, String className, Context cxt) {
        try {
            int enable = Settings.Secure.getInt(cxt.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enable != 1)
                return false;
            String services = Settings.Secure.getString(cxt.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (!TextUtils.isEmpty(services)) {
                TextUtils.SimpleStringSplitter split = new TextUtils.SimpleStringSplitter(':');
                split.setString(services);
                while (split.hasNext()) { // 遍历所有已开启的辅助服务名
                    if (split.next().equalsIgnoreCase(packgeName + "/" + className))
                        return true;
                }
            }
        } catch (Throwable e) {//若出现异常，则说明该手机设置被厂商篡改了,需要适配
            Log.e(TAG, "isSettingOpen: " + e.getMessage());
        }
        return false;
    }

    /**
     * 跳转到系统设置：开启辅助服务
     */
    public static void jumpToSetting(final Context cxt) {
        try {
            cxt.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable e) {//若出现异常，则说明该手机设置被厂商篡改了,需要适配
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cxt.startActivity(intent);
            } catch (Throwable e2) {
                Log.e(TAG, "jumpToSetting: " + e2.getMessage());
            }
        }
    }
}