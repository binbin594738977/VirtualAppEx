package com.weiliu.library;

import android.app.Application;

/**
 * Application基类，与app无关
 * Created by qumiao on 16/4/27.
 */
public abstract class RootApplication extends Application {
    static RootApplication APP;

    public static RootApplication getInstance() {
        return APP;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        APP = this;
    }

}
