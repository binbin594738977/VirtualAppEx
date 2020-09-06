package io.virtualapp.core;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.lody.virtual.BuildConfig;

import java.util.ArrayList;

import library.ClassUtil;
import library.Utility;
import library.WeiliuLog;

/**
 * 分析微信的View
 */
public class AnalysisView {
    public static Callback callback;
    private static final Object RESUME_TOKEN = new Object();
    static boolean isListener = false;

    public static void OnResume(final Activity activity) {
        Handler handler = new Handler();
        if (callback != null) {
            callback.handleOnResume(activity);
        } else {
            WeiliuLog.log(3, "AnalysisWXView OnResume isListener =" + isListener);
        }
        handler.removeCallbacksAndMessages(RESUME_TOKEN);
        handler.postAtTime(new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG) {
                    WeiliuLog.log("CrashHandler = " + Thread.getDefaultUncaughtExceptionHandler().getClass().getName());
                    WeiliuLog.log("getProcessNameByPid: " + Utility.getCurProcessName(activity));
                    WeiliuLog.log(activity.getClass().getName() + ": 等待完成");
//                    ViewGroup mContentParent = (ViewGroup) activity.getWindow().getDecorView();
//                    analysisContentParent(mContentParent, activity);
                    if (!isListener) {
                        isListener = true;
                    }
                    try {
                        WindowManager windowManager = activity.getWindowManager();
                        Object global = ClassUtil.getFieldValue(windowManager, "mGlobal");
                        ArrayList<View> mViews = (ArrayList<View>) ClassUtil.getFieldValue(global, "mViews");
                        for (View view : mViews) {
                            analysisContentParent(view, activity);
                        }
                    } catch (Exception e) {
                        WeiliuLog.log(e);
                    }
                }
            }
        }, RESUME_TOKEN, SystemClock.uptimeMillis() + 5000);
    }


    /**
     * 解刨视图树
     */
    private static final void analysisContentParent(View view, Activity activity) {
        if (view == null) return;

        final View.OnClickListener listener = getViewClickListener(view);
        //如果是它设置的监听器,才设置监听器
        if (callback != null && !callback.handleSetOnClickListener(activity, view)) {
//            WeiliuLog.log("AnalysisWXView handleSetOnClickListener callback " + callback);
        } else if (listener != null && !(listener instanceof MyOnclickListener)) {
            view.setOnClickListener(new MyOnclickListener() {
                @Override
                public void onClick(View v) {
                    WeiliuLog.log("view ID: " + view.getClass().getName());
                    WeiliuLog.log("OnClickListener ID: " + listener.getClass().getName());
                    listener.onClick(v);
                }
            });
        }

        final View.OnTouchListener onTouchListener = getViewmOnTouchListener(view);
        //如果是它设置的监听器,才设置监听器
        if (callback != null && !callback.handleSetOnTouchListener(activity, view)) {
//            WeiliuLog.log("AnalysisWXView handleSetOnClickListener callback " + callback);
        } else if (onTouchListener != null && !(onTouchListener instanceof MyOnTouchListener)) {
            view.setOnTouchListener(new MyOnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    WeiliuLog.log("view ID: " + view.getClass().getName());
                    WeiliuLog.log("onTouchListener ID: " + onTouchListener.getClass().getName());
                    return onTouchListener.onTouch(v, event);
                }
            });
        }


        if (callback != null && !callback.handleSetOnItemClickListener(activity, view)) {
//            WeiliuLog.log("AnalysisWXView handleSetOnItemClickListener callback " + callback);
        } else if (view instanceof AdapterView) {
            final AdapterView.OnItemClickListener onItemClickListener = getOnItemClickListener((AdapterView) view);
            if (onItemClickListener != null && !(onItemClickListener instanceof MyOnItemClickListener))
                ((AdapterView) view).setOnItemClickListener(new MyOnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        WeiliuLog.log("view ID: " + view.getClass().getName());
                        WeiliuLog.log("onItemClickListener ID: " + onItemClickListener.getClass().getName());
                        //设置页 关于微信 点击
                        onItemClickListener.onItemClick(parent, view, position, id);
                    }
                });
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childAt = viewGroup.getChildAt(i);
                analysisContentParent(childAt, activity);
            }
        }
    }


    public static class MyOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }

    public static class MyOnclickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
        }
    }

    public static class MyOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    }

    /**
     * 得到view的点击监听
     *
     * @param view
     * @return
     */
    public static final View.OnClickListener getViewClickListener(View view) {
        try {
            Object mListenerInfo = ClassUtil.getFieldValue(view, "mListenerInfo");
            //mListenerInfo==null,说明没有设置监听
            if (mListenerInfo == null) return null;
            Object listener = ClassUtil.getFieldValue(mListenerInfo, "mOnClickListener");
            //如果是我们自己的监听器,返回null
            if (listener instanceof MyOnclickListener) return null;
            return (View.OnClickListener) listener;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return null;
        }
    }



    /**
     * 得到OnTouchListener
     *
     * @param view
     * @return
     */
    public static final View.OnTouchListener getViewmOnTouchListener(View view) {
        try {

            Object mListenerInfo = ClassUtil.getFieldValue(view, "mListenerInfo");
            //mListenerInfo==null,说明没有设置监听
            if (mListenerInfo == null) return null;
            Object listener = ClassUtil.getFieldValue(mListenerInfo, "mOnTouchListener");
            //如果是我们自己的监听器,返回null
            if (listener instanceof MyOnTouchListener) return null;
            return (View.OnTouchListener) listener;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return null;
        }
    }


    public static final synchronized AdapterView.OnItemClickListener getOnItemClickListener
            (AdapterView adapterView) {
        try {
            Object listenerInfo = ClassUtil.getFieldValue(adapterView, "mOnItemClickListener");
            if (listenerInfo instanceof MyOnItemClickListener) return null;
            return (AdapterView.OnItemClickListener) listenerInfo;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return null;
        }
    }

    public interface Callback {
        /**
         * @return false不要再设置, true代表还需要设置
         */
        boolean handleSetOnItemClickListener(Activity activity, View view);

        /**
         * @return false不要再设置, true代表还需要设置
         */
        boolean handleSetOnClickListener(Activity activity, View view);
        /**
         * @return false不要再设置, true代表还需要设置
         */
        boolean handleSetOnTouchListener(Activity activity, View view);

        void handleOnResume(Activity activity);


    }
}
