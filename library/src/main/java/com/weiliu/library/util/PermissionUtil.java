package com.weiliu.library.util;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.weiliu.library.RootActivity;

import java.util.ArrayList;

/**
 * 权限相关的常用方法
 * Created by qumiao on 16/4/7.
 */
public class PermissionUtil {

    private PermissionUtil() {

    }

    /**
     * 如果需要请求权限，则弹框提示，并作出需要权限的解释（以toast展示）
     * @param activity
     * @param permission 权限
     * @param explanation 解释
     * @param requestCode 触发requestPermissions后，在onRequestPermissionsResult里用到（区分是在请求哪组权限）
     * @return 如果为true，表示已经获取了这些权限；false表示还未获取，并正在执行请求
     */
    public static boolean requestPermissionIfNeed(RootActivity activity,
                                                  String permission, CharSequence explanation, int requestCode) {
        return requestPermissionIfNeed(activity, new String[]{permission}, explanation, requestCode);
    }

    /**
     * 如果需要请求权限，则弹框提示，并作出需要权限的解释（以toast展示）
     * @param activity
     * @param permissions 权限
     * @param explanation 解释
     * @param requestCode 触发requestPermissions后，在onRequestPermissionsResult里用到（区分是在请求哪组权限）
     * @return 如果为true，表示已经获取了这些权限；false表示还未获取，并正在执行请求
     */
    public static boolean requestPermissionIfNeed(RootActivity activity,
                                                  String[] permissions, CharSequence explanation, int requestCode) {
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if (permissionList.isEmpty()) {
            return true;
        }

        String[] needRequestPermissions = permissionList.toArray(new String[permissionList.size()]);

        for (String permission : needRequestPermissions) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                if (!TextUtils.isEmpty(explanation)) {
                    Toast.makeText(activity, explanation, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

        ActivityCompat.requestPermissions(activity,
                needRequestPermissions,
                requestCode);

        return false;
    }
}
