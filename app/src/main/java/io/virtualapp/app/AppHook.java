package io.virtualapp.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.lody.virtual.client.VClientHookManager;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import dalvik.system.DexClassLoader;
import library.ClassUtil;
import library.Utility;
import library.WeiliuLog;
import mirror.android.app.ActivityManagerNative;


public class AppHook implements VClientHookManager.Callback {

    private static final Map<String, Class<? extends VClientHookManager.Callback>> CALLBACK_CLASS_MAP;

    static {
        HashMap<String, Class<? extends VClientHookManager.Callback>> map = new HashMap<>();
        //TODO 在此添加更多Callback映射
        map.put("cn.xuexi.android", QGXXhook.class);
        CALLBACK_CLASS_MAP = Collections.unmodifiableMap(map);
    }

    private static final Map<Class<? extends VClientHookManager.Callback>, VClientHookManager.Callback> CALLBACK_OBJ_MAP = new HashMap<>();

    private static VClientHookManager.Callback getCallbackInstance(String packageName) {
        VClientHookManager.Callback callback = null;
        Class<? extends VClientHookManager.Callback> cls = CALLBACK_CLASS_MAP.get(packageName);
        if (cls != null) {
            callback = CALLBACK_OBJ_MAP.get(cls);
            if (callback == null) {
                try {
                    callback = cls.newInstance();
                    CALLBACK_OBJ_MAP.put(cls, callback);
                } catch (Throwable e) {
                    WeiliuLog.log(e);
                }
            }
        }


        return callback;
    }


    private static final AppHook sAppHook = new AppHook();

    private AppHook() {
        //no instance
    }

    public static AppHook getInstance() {
        return sAppHook;
    }

    public void start() {
        VClientHookManager.CALLBACKS.add(this);
    }

    public static final String SOURCE_INJECT_JAR = "source_inject2.jar";

    private DexClassLoader mSourceInjectJarLoader;
    private Application mApplication;
    private ClassLoader mClassLoader;
    private Handler mHandler = new Handler();
    private Handler mWorkerHandler;

    public static Handler getMainHandler() {
        return sAppHook.mHandler;
    }

    public static Handler getWorkerHandler() {
        return sAppHook.mWorkerHandler;
    }


    @Override
    public void onApplicationInit(Application initialApplication) {
        mApplication = initialApplication;
        mClassLoader = initialApplication.getClassLoader();
        HandlerThread workerThread = new HandlerThread("Hook_Worker_Thread");
        workerThread.start();
        mWorkerHandler = new Handler(workerThread.getLooper());
//
//        mHandler.post(() -> {
//            ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), AppInstrumentation.create());
//        });

        mHandler.postDelayed(() -> {
            Object gDefault = ActivityManagerNative.getDefault.call();
            WeiliuLog.log("ActivityManagerNative.getDefault() == " + gDefault.getClass().getName());

        }, 10000);

        VClientHookManager.Callback callback = getCallbackInstance(initialApplication.getPackageName());
        if (callback != null) {
            callback.onApplicationInit(initialApplication);
        }

        if (!Utility.isMainProcess(initialApplication)) {   //只hook主进程
            return;
        }
        mHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                test(initialApplication);
            }
        }, RESUME_TOKEN, SystemClock.uptimeMillis() + 5000);
//        AnrMonitorClient.getInstance().start();
//        TimingCheckTask.startCheck(initialApplication);
    }

    private void test(Context context) {
        try {
            WeiliuLog.log("当前的包名: " + VClientImpl.get().getCurrentPackage());

            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    VClientImpl.get().getCurrentPackage(), PackageManager.GET_ACTIVITIES);
            for (ActivityInfo activity : packageInfo.activities) {
                WeiliuLog.log("aClass: " + activity.name);
                dexCacheToDexFile(activity.name);
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    private ArrayList<String> addList = new ArrayList<String>();

    public void dexCacheToDexFile(String activityName) {
        try {
            Class<?> aClass = mClassLoader.loadClass(activityName);
            Object dexCache = ClassUtil.getFieldValue(aClass, "dexCache");
            Object dex = ClassUtil.getFieldValue(dexCache, "dex");
            String name = "";
            if (dexCache != null) {
                String location = (String) ClassUtil.getFieldValue(dexCache, "location");
                WeiliuLog.log("location: " + location);
                if (addList.contains(location)) {
//                    WeiliuLog.log(3, "已经添加过了" + location);
                    return;
                }
                addList.add(location);
                int index = location.lastIndexOf("/") + 1;
                name = location.substring(index);
            }
            WeiliuLog.log("name: " + name);
            String processName = Utility.getCurProcessName(VirtualCore.get().getContext());
            WeiliuLog.log("进程名: " + processName);
            byte[] getBytes = (byte[]) ClassUtil.invokeMethod(dex, "getBytes");
            File file = new File(Utility.getDefaultFileDirectory() + "/dexCach0/" + processName);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (getBytes.length == 0) return;
            File file1 = new File(file, name);
            boolean b = Utility.bytesToFile(getBytes, file1);
            WeiliuLog.log(3, b + "  文件: " + file1.getAbsolutePath());
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    @Override
    public void onActivityCreate(Activity activity) {
        WeiliuLog.log("onCreate activity = " + activity);
        VClientHookManager.Callback callback = getCallbackInstance(activity.getPackageName());
        if (callback != null) {
            callback.onActivityCreate(activity);
        }
    }

    private static final Object RESUME_TOKEN = new Object();

    @Override
    public void onActivityResume(Activity activity) {
        mHandler.removeCallbacksAndMessages(RESUME_TOKEN);
        mHandler.postAtTime(new Runnable() {
            @Override
            public void run() {

            }
        }, RESUME_TOKEN, SystemClock.uptimeMillis() + 5000);
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                WeiliuLog.log("activity: " + activity);
            }
        });
        VClientHookManager.Callback callback = getCallbackInstance(activity.getPackageName());
        if (callback != null) {
            callback.onActivityResume(activity);
        }
    }

    @Override
    public void onActivityPause(Activity activity) {
        VClientHookManager.Callback callback = getCallbackInstance(activity.getPackageName());
        if (callback != null) {
            callback.onActivityPause(activity);
        }
    }

    @Override
    public void onActivityDestroy(Activity activity) {
        VClientHookManager.Callback callback = getCallbackInstance(activity.getPackageName());
        if (callback != null) {
            callback.onActivityDestroy(activity);
        }
    }

    @Override
    public void onServiceStartCommand(Service service, Intent intent, int flags, int startId) {
        VClientHookManager.Callback callback = getCallbackInstance(service.getPackageName());
        if (callback != null) {
            callback.onServiceStartCommand(service, intent, flags, startId);
        }
    }

    @Override
    public void onServiceDestroy(Service service) {
        VClientHookManager.Callback callback = getCallbackInstance(service.getPackageName());
        if (callback != null) {
            callback.onServiceDestroy(service);
        }
    }


}
