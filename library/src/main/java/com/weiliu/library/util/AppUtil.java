package com.weiliu.library.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.weiliu.library.WeiliuLog;

import java.io.File;

/**
 * 作者：qumiao
 * 日期：2017/6/23 14:30
 * 说明：App相关的常用方法集合类
 */
public class AppUtil {

    public static final String APP_INSTALL_MIMETYPE = "application/vnd.android.package-archive";
    public static final String APP_STREAM_MIMETYPE = "application/octet-stream";


    private AppUtil() {
        //no instance
    }


    /**
     * 判断app是否安装
     *
     * @param context
     * @param packageName app的包名
     * @return
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 安装app
     *
     * @param context
     * @param path    app包的文件路径
     */
    public static void installApp(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        Intent appIntent = new Intent(Intent.ACTION_VIEW);
        File file = new File(path);
        Uri installUri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            installUri = Uri.fromFile(file);
        } else {
            installUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            appIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        appIntent.setDataAndType(installUri, APP_INSTALL_MIMETYPE);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(appIntent);
    }

    /**
     * 卸载app
     *
     * @param context
     * @param packageName app包名
     */
    public static void uninstallApp(Context context, String packageName) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
        context.startActivity(intent);
    }

    /**
     * 打开app
     *
     * @param context
     * @param packageName app的包名
     */
    public static void openApp(Context context, String packageName) {
        try {
            Intent pageIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (pageIntent != null) {
                pageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pageIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(pageIntent);
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }
}
