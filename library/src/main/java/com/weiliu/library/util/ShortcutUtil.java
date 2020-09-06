package com.weiliu.library.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;

import com.weiliu.library.db.ShortcutControl;

/**
 * 快捷方式方法集合
 * Created by qumiao on 2016/10/28.
 */

public class ShortcutUtil {

    // Action 添加Shortcut
    public static final String ACTION_ADD_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    // Action 移除Shortcut
    public static final String ACTION_REMOVE_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    private ShortcutUtil() {
        //no instance
    }

    /**
     * 添加快捷方式
     *
     * @param context      context
     * @param allowRepeat  允许重复添加
     * @param actionIntent 要启动的Intent
     * @param name         name
     */
    public static boolean addShortcut(Context context, boolean allowRepeat,
                                      Intent actionIntent, String name, Bitmap iconBitmap) {
        if (ContextCompat.checkSelfPermission(context, "com.android.launcher.permission.INSTALL_SHORTCUT")
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        boolean has = ShortcutControl.hasShortcut(actionIntent, name);
        if (!allowRepeat && has) {
            return false;
        }

        actionIntent.setPackage(context.getPackageName());

        Intent addShortcutIntent = new Intent(ACTION_ADD_SHORTCUT);
        // 是否允许重复创建
        addShortcutIntent.putExtra("duplicate", false);
        // 快捷方式的标题
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        // 快捷方式的图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, iconBitmap);
        // 快捷方式的动作
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, actionIntent);
        context.sendBroadcast(addShortcutIntent);

        if (!has) {
            ShortcutControl.addShortcut(actionIntent, name, false);
        }
        return true;
    }

    /**
     * 移除快捷方式
     *
     * @param context      context
     * @param actionIntent 要启动的Intent
     * @param name         name
     */
    public static boolean removeShortcut(Context context, Intent actionIntent, String name) {
        if (ContextCompat.checkSelfPermission(context, "com.android.launcher.permission.UNINSTALL_SHORTCUT")
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        actionIntent.setPackage(context.getPackageName());

        Intent intent = new Intent(ACTION_REMOVE_SHORTCUT);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra("duplicate", false);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, actionIntent);
        context.sendBroadcast(intent);

        ShortcutControl.removeShortcut(actionIntent, name, false);
        return true;
    }

}
