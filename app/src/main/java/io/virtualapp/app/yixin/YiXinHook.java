package io.virtualapp.app.yixin;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.lody.virtual.client.VClientHookManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.virtualapp.MyUtil;
import io.virtualapp.app.AppHook;
import library.ClassUtil;
import library.Utility;
import library.WeiliuLog;

public class YiXinHook extends VClientHookManager.CallbackAdapter {
    public Application mApplication;
    public ClassLoader mClassLoader;
    public AppHook mAppHook;
    public static YiXinHook mYiXinHook;
    public SharedPreferences sp;

    @Override
    public void onApplicationInit(Application initialApplication) {
        super.onApplicationInit(initialApplication);
        if (!Utility.isMainProcess(initialApplication)) {   //只hook主进程
            return;
        }
        mAppHook = AppHook.getInstance();
        mApplication = mAppHook.mApplication;
        mClassLoader = mAppHook.mClassLoader;
        mYiXinHook = this;
        sp = mApplication.getSharedPreferences("yixin", Context.MODE_PRIVATE);
        mAppHook.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    WeiliuLog.log("开始监听");
                    //监听handler
                    Object a = ClassUtil.invokeStaticMethod(mClassLoader.loadClass("im.yixin.common.a.f"), "a");
                    List<Handler> a_b = (List<Handler>) ClassUtil.getFieldValue(a, "b");
//            TViewWatcher a = TViewWatcher.a();
                    YXRemoteHandler yxRemoteHandler = new YXRemoteHandler();
                    synchronized (a_b) {
                        a_b.add(yxRemoteHandler);
                    }
                    yxRemoteHandler.satrtTask();
                } catch (Exception e) {
                    WeiliuLog.log(e);
                }
            }
        }, 15000);
    }

    public static YiXinHook get() {
        return mYiXinHook;
    }

    public void searchPhoneNumber(String phoneNumber) {
        try {
            WeiliuLog.log(3, "phoneNumber: " + phoneNumber);
            Object instance = ClassUtil.getInstance(mClassLoader.loadClass("im.yixin.service.bean.a.b.b"));
            ClassUtil.setFieldValue(instance, phoneNumber, "a");
            Object toRemote = ClassUtil.invokeMethod(instance, "toRemote");
            Class<?> aRemoteClass = mClassLoader.loadClass("im.yixin.service.Remote");
            Object a = ClassUtil.invokeStaticMethod(mClassLoader.loadClass("im.yixin.common.a.f"), "a");
            ClassUtil.invokeMethod(a, "a"
                    , new Class[]{aRemoteClass, boolean.class}
                    , new Object[]{toRemote, false});
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    @Override
    public void onActivityCreate(Activity activity) {
        super.onActivityCreate(activity);
    }

    @Override
    public void onActivityResume(Activity activity) {
        super.onActivityResume(activity);
    }

}
