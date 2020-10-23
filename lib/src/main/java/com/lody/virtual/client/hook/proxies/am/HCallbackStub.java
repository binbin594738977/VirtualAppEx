package com.lody.virtual.client.hook.proxies.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.StubActivityRecord;

import java.util.List;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.IActivityManager;


/**
 * 将ActivityThread的mH这个Handle增加一个我们自己的callback,这样所以的事件都会经过我们这里,这个mH一般是处理系统事情的,所以自己处理完再交给系统处理
 *
 * @author Lody
 * @see Handler.Callback
 */
public class HCallbackStub implements Handler.Callback, IInjector {


    private static int LAUNCH_ACTIVITY = 100;
    private static int EXECUTE_TRANSACTION = 159;

    static {
        try {
            if (ActivityThread.H.LAUNCH_ACTIVITY != null) {
                LAUNCH_ACTIVITY = ActivityThread.H.LAUNCH_ACTIVITY.get();
            }

            if (ActivityThread.H.EXECUTE_TRANSACTION != null) {
                EXECUTE_TRANSACTION = ActivityThread.H.EXECUTE_TRANSACTION.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static final int CREATE_SERVICE = ActivityThread.H.CREATE_SERVICE.get();
    private static final int SCHEDULE_CRASH =
            ActivityThread.H.SCHEDULE_CRASH != null ? ActivityThread.H.SCHEDULE_CRASH.get() : -1;

    private static final String TAG = HCallbackStub.class.getSimpleName();
    private static final HCallbackStub sCallback = new HCallbackStub();

    private boolean mCalling = false;


    private Handler.Callback otherCallback;//这里系统一般是没有callback的

    private HCallbackStub() {
    }

    public static HCallbackStub getDefault() {
        return sCallback;
    }

    //系统的原来的
    private static Handler getH() {
        return ActivityThread.mH.get(VirtualCore.mainThread());
    }

    private static Handler.Callback getHCallback() {
        try {
            Handler handler = getH();
            return mirror.android.os.Handler.mCallback.get(handler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!mCalling) {
            mCalling = true;
            try {
                if (LAUNCH_ACTIVITY == msg.what) {
                    //老版本启动activity走的这个
                    if (!handleLaunchActivity(msg)) {
                        return true;
                    }
                } else if (msg.what == EXECUTE_TRANSACTION) {
                    //新版本启动activity走的这个,处理完之后,会将intent ActivityInfo等信息修改成将要打开的activity信息
                    if (!handleExecuteTransaction(msg)) {
                        return true;
                    }
                } else if (CREATE_SERVICE == msg.what) {
                    if (!VClientImpl.get().isBound()) {
                        ServiceInfo info = Reflect.on(msg.obj).get("info");
                        VClientImpl.get().bindApplication(info.packageName, info.processName);
                    }
                } else if (SCHEDULE_CRASH == msg.what) {
                    // to avoid the exception send from System.
                    return true;
                }
                if (otherCallback != null) {
                    boolean desired = otherCallback.handleMessage(msg);
                    mCalling = false;
                    return desired;
                } else {
                    mCalling = false;
                }
            } finally {
                mCalling = false;
            }
        }
        return false;//返回false是继续让系统的handleMessage处理
    }

    private boolean handleLaunchActivity(Message msg) {
        Object r = msg.obj;
        Intent stubIntent = ActivityThread.ActivityClientRecord.intent.get(r);
        StubActivityRecord saveInstance = new StubActivityRecord(stubIntent);
        if (saveInstance.intent == null) {
            return true;
        }
        Intent intent = saveInstance.intent;
        ComponentName caller = saveInstance.caller;
        IBinder token = ActivityThread.ActivityClientRecord.token.get(r);
        ActivityInfo info = saveInstance.info;
        if (VClientImpl.get().getToken() == null) {
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
            if (installedAppInfo == null) {
                return true;
            }
            VActivityManager.get().processRestarted(info.packageName, info.processName, saveInstance.userId);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }
        if (!VClientImpl.get().isBound()) {
            VClientImpl.get().bindApplication(info.packageName, info.processName);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }
        int taskId = IActivityManager.getTaskForActivity.call(
                ActivityManagerNative.getDefault.call(),
                token,
                false
        );
        VActivityManager.get().onActivityCreate(ComponentUtils.toComponentName(info), caller, token, info, intent, ComponentUtils.getTaskAffinity(info), taskId, info.launchMode, info.flags);
        ClassLoader appClassLoader = VClientImpl.get().getClassLoader(info.applicationInfo);
        intent.setExtrasClassLoader(appClassLoader);
        ActivityThread.ActivityClientRecord.intent.set(r, intent);
        ActivityThread.ActivityClientRecord.activityInfo.set(r, info);
        return true;
    }

    @Override
    public void inject() throws Throwable {
        otherCallback = getHCallback();
        mirror.android.os.Handler.mCallback.set(getH(), this);
    }

    @Override
    public boolean isEnvBad() {
        Handler.Callback callback = getHCallback();
        boolean envBad = callback != this;
        if (callback != null && envBad) {
            VLog.d(TAG, "HCallback has bad, other callback = " + callback);
        }
        return envBad;
    }

    /**
     * 启动activity的逻辑,这个逻辑在系统中的LaunchActivityItem类 , 在这里, actiivty和intent都是用的占坑的activity ,intent里面保存的都是将要打开的activity信息 , 包含ActivityInfo ... 将会在这里替换成新的activity
     */
    private boolean handleLaunchActivity2(Message msg) {
        Object clientTransaction = msg.obj;
        List<Object> clientTransactionItemCallBacks = Reflect.on(clientTransaction).call("getCallbacks").get();
        if (clientTransactionItemCallBacks != null && clientTransactionItemCallBacks.size() > 0) {
            for (Object callBacks : clientTransactionItemCallBacks) {
                try {
                    if (callBacks.getClass().getName().contains("LaunchActivityItem")) {
                        Intent stubIntent = Reflect.on(callBacks).field("mIntent").get();
                        final StubActivityRecord saveInstance = new StubActivityRecord(stubIntent);
                        if (saveInstance.intent == null) {
                            return true;
                        }


                        Intent intent = saveInstance.intent;
                        ComponentName caller = saveInstance.caller;
                        IBinder token = Reflect.on(clientTransaction).call("getActivityToken").get();
                        ActivityInfo info = saveInstance.info;
                        if (VClientImpl.get().getToken() == null) {
                            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
                            if (installedAppInfo == null) {
                                return true;
                            }
                            VActivityManager.get().processRestarted(info.packageName, info.processName, saveInstance.userId);
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return false;
                        }
                        if (!VClientImpl.get().isBound()) {
                            VClientImpl.get().bindApplication(info.packageName, info.processName);
                            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                            return false;
                        }
                        int taskId = IActivityManager.getTaskForActivity.call(
                                ActivityManagerNative.getDefault.call(),
                                token,
                                false
                        );//交给x进程记录activity的创建
                        VActivityManager.get().onActivityCreate(ComponentUtils.toComponentName(info), caller, token, info, intent, ComponentUtils.getTaskAffinity(info), taskId, info.launchMode, info.flags);
                        ClassLoader appClassLoader = VClientImpl.get().getClassLoader(info.applicationInfo);

                        stubIntent.setExtrasClassLoader(appClassLoader);
                        ComponentName name = Reflect.on(callBacks).field("mIntent").field("mComponent").get();
                        //替换activity的信息
                        Reflect.on(callBacks).set("mIntent", saveInstance.intent);
                        Reflect.on(callBacks).set("mInfo", saveInstance.info);

                        return true;

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return true;
    }

    private boolean handleExecuteTransaction(Message msg) {
        Object clientTransaction = msg.obj;
        List<Object> clientTransactionItemCallBacks = Reflect.on(clientTransaction).call("getCallbacks").get();
        if (clientTransactionItemCallBacks != null && clientTransactionItemCallBacks.size() > 0) {
            for (Object callBacks : clientTransactionItemCallBacks) {
                try {
                    if (callBacks.getClass().getName().contains("LaunchActivityItem")) {
                        return handleLaunchActivity2(msg);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return true;
    }

}
