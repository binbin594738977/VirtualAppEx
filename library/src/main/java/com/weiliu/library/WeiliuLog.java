package com.weiliu.library;
import android.util.Log;

public class WeiliuLog {
    public static final String TAG = "xxxWEILIU_XBS";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    public static void logWithArgs(String format, Object... args) {
        log(String.format(format, args));
    }
    public static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
    public static void log(int level, String msg) {
        if (DEBUG) {
            switch (level) {
                case 0:
                    Log.v(TAG, msg);
                    break;
                case 1:
                    Log.d(TAG, msg);
                    break;
                case 2:
                    Log.i(TAG, msg);
                    break;
                case 3:
                    Log.w(TAG, msg);
                    break;
                default:
                    Log.e(TAG, msg);
                    break;
            }
        }
    }
    public static void log(String tag, int level, String msg) {
        if (DEBUG) {
            switch (level) {
                case 0:
                    Log.v(tag, msg);
                    break;
                case 1:
                    Log.d(tag, msg);
                    break;
                case 2:
                    Log.i(tag, msg);
                    break;
                case 3:
                    Log.w(tag, msg);
                    break;
                default:
                    Log.e(tag, msg);
                    break;
            }
        }
    }
    public static void log(String tag, String arg) {
        if (DEBUG) {
            Log.d(tag, arg);
        }
    }
    public static void log(String tag, Throwable e) {
        if (DEBUG) {
            Log.w(tag, e);
        }
    }
    public static void log(Throwable e) {
        if (DEBUG) {
            Log.w(TAG, e);
        }
    }
    // 使用Log来显示调试信息,因为log在实现上每个message有4k字符长度限制
    // 所以这里使用自己分节的方式来输出足够长度的message
    private static void longLog(String tag, int level, String str) {
        int index = 0;
        int maxLength = 2000;
        while (str.length() > index * maxLength) {
            // java的字符不允许指定超过总的长度end
            if (str.length() < (index + 1) * maxLength) {
                WeiliuLog.log(tag, level, str.substring(index * maxLength));
            } else {
                WeiliuLog.log(tag, level, str.substring(index * maxLength, (index + 1) * maxLength));
            }
            index++;
        }
    }
    public static void longLog(int level, String str) {
        if (DEBUG) {
            longLog(TAG, level, str);
        }
    }

    // 使用Log来显示调试信息,因为log在实现上每个message有4k字符长度限制
    // 所以这里使用自己分节的方式来输出足够长度的message
    public static void longLog(String str) {
        if (DEBUG) {
            longLog(1, str);
        }
    }
}