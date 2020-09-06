package io.virtualapp.core;

import android.content.Context;

import com.weiliu.library.RootApplication;
import com.weiliu.library.util.PfUtil;
import com.weiliu.library.util.Utility;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.virtualapp.VApp;

/**
 * 作者：
 * 日期：2017/5/20 12:47
 * 说明：
 */
public class BaseApplication {

    private static final String PF = "config";
    private static String sChannel = "fh";

    public static Context app() {
        return VApp.getApp();
    }

    public static String getChannelName() {
        return sChannel;
    }

}
