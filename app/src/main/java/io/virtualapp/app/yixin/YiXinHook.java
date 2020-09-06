package io.virtualapp.app.yixin;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;

import com.lody.virtual.client.VClientHookManager;

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

    @Override
    public void onApplicationInit(Application initialApplication) {
        super.onApplicationInit(initialApplication);
        if (!Utility.isMainProcess(initialApplication)) {   //只hook主进程
            return;
        }
        mAppHook = AppHook.getInstance();
        mApplication = mAppHook.mApplication;
        mClassLoader = mAppHook.mClassLoader;
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

                    satrtTask();
                } catch (Exception e) {
                    WeiliuLog.log(e);
                }
            }
        }, 10000);
    }

    private List<String> searchlist = new ArrayList<>();

    private void satrtTask() {
        mAppHook.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String number = "";
                do {
                    long l = MyUtil.x_y_random(100000, 999999);
                    number = "15073" + l;
                } while (searchlist.contains(number));
                searchPhoneNumber(number);
                mAppHook.mHandler.postDelayed(this, 1000);
            }
        }, 0);
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
