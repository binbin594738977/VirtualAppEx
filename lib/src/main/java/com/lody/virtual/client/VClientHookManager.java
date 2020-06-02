package com.lody.virtual.client;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class VClientHookManager {
    public static final List<Callback> CALLBACKS = new ArrayList<>();

    public static void hookApplicationInit(Application initialApplication) {
        for (Callback callback : CALLBACKS) {
            callback.onApplicationInit(initialApplication);
        }
    }

    public static void hookActivityCreate(Activity activity) {
        for (Callback callback : CALLBACKS) {
            callback.onActivityCreate(activity);
        }
    }

    public static void hookActivityResume(Activity activity) {
        for (Callback callback : CALLBACKS) {
            callback.onActivityResume(activity);
        }
    }

    public static void hookActivityPause(Activity activity) {
        for (Callback callback : CALLBACKS) {
            callback.onActivityPause(activity);
        }
    }

    public static void hookActivityDestroy(Activity activity) {
        for (Callback callback : CALLBACKS) {
            callback.onActivityDestroy(activity);
        }
    }

    public static void hookServiceStartCommand(Service service, Intent intent, int flags, int startId) {
        for (Callback callback : CALLBACKS) {
            callback.onServiceStartCommand(service, intent, flags, startId);
        }
    }

    public static void hookServiceDestroy(Service service) {
        for (Callback callback : CALLBACKS) {
            callback.onServiceDestroy(service);
        }
    }

    public interface Callback {
        void onApplicationInit(Application initialApplication);

        void onActivityCreate(Activity activity);

        void onActivityResume(Activity activity);

        void onActivityPause(Activity activity);

        void onActivityDestroy(Activity activity);

        void onServiceStartCommand(Service service, Intent intent, int flags, int startId);

        void onServiceDestroy(Service service);
    }

    public static class CallbackAdapter implements Callback {

        @Override
        public void onApplicationInit(Application initialApplication) {

        }

        @Override
        public void onActivityCreate(Activity activity) {

        }

        @Override
        public void onActivityResume(Activity activity) {

        }

        @Override
        public void onActivityPause(Activity activity) {

        }

        @Override
        public void onActivityDestroy(Activity activity) {

        }

        @Override
        public void onServiceStartCommand(Service service, Intent intent, int flags, int startId) {

        }

        @Override
        public void onServiceDestroy(Service service) {

        }
    }
}
