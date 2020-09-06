package com.weiliu.library.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * 作者：qumiao
 * 日期：2017/6/27 12:07
 * 说明：
 */
public class IntentUtil {

    private IntentUtil() {
        //no instance
    }

    public static void startActivity(Context context, Class<? extends Activity> activityClass) {
        startActivity(context, activityClass, null);
    }

    public static void startActivity(Fragment fragment, Class<? extends Activity> activityClass) {
        startActivity(fragment, activityClass, null);
    }

    public static void startActivity(Context context, Class<? extends Activity> activityClass, Bundle extras) {
        startActivity(context, activityClass, extras, 0);
    }

    public static void startActivity(Context context, Class<? extends Activity> activityClass, Bundle extras, int flags) {
        Intent intent = new Intent(context, activityClass);
        if (extras != null) {
            intent.putExtras(extras);
        }
        Activity activity = Utility.getActivity(context);
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (flags != 0) {
            intent.addFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void startActivity(Fragment fragment, Class<? extends Activity> activityClass, Bundle extras) {
        Intent intent = new Intent(fragment.getActivity(), activityClass);
        if (extras != null) {
            intent.putExtras(extras);
        }
        fragment.startActivity(intent);
    }

    public static void startActivityForResult(Activity activity, Class<? extends Activity> activityClass, int requestCode) {
        startActivityForResult(activity, activityClass, null, requestCode);
    }

    public static void startActivityForResult(Activity activity, Class<? extends Activity> activityClass, Bundle extras, int requestCode) {
        Intent intent = new Intent(activity, activityClass);
        if (extras != null) {
            intent.putExtras(extras);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startActivityForResult(Fragment fragment, Class<? extends Activity> activityClass, int requestCode) {
        startActivityForResult(fragment, activityClass, null, requestCode);
    }

    public static void startActivityForResult(Fragment fragment, Class<? extends Activity> activityClass, Bundle extras, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), activityClass);
        if (extras != null) {
            intent.putExtras(extras);
        }
        fragment.startActivityForResult(intent, requestCode);
    }
}
