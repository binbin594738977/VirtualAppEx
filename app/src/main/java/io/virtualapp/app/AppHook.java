package io.virtualapp.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.lody.virtual.client.VClientHookManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;
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


//        if (!Utility.isMainProcess(initialApplication)) {   //只hook主进程
//            return;
//        }
//
//        AnrMonitorClient.getInstance().start();
//        TimingCheckTask.startCheck(initialApplication);
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
