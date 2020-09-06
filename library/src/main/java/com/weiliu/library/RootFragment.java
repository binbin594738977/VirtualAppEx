package com.weiliu.library;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.umeng.analytics.MobclickAgent;
import com.weiliu.library.sr.RootSavableImpl;
import com.weiliu.library.task.TaskManager;
import com.weiliu.library.task.TaskStarter;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * 所有Fragment的基类，与app无关。
 * <p/>
 * Created by qumiao on 16/4/27.
 */
public abstract class RootFragment extends Fragment {

    private boolean mHasDestroyed;

    private TaskManager mTaskManager;
    @Nullable
    private TaskStarter mTaskStarter;

    private final Map<Integer, RootActivity.OnActivityResultListener> mOnActivityResultMap = new Hashtable<>();

    private final RootSavableImpl mSavableImpl = new RootSavableImpl(this);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHasDestroyed = false;

        mSavableImpl.onCreate(savedInstanceState);

        initHandleActivityResultMethods();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mTaskManager = new TaskManager();
        Activity activity = getActivity();
        if (activity instanceof RootActivity) {
            mTaskStarter = new TaskStarter(activity, mTaskManager, ((RootActivity) activity).getTaskProgressDialog());
        }

        return onCreateContentView(inflater, container, savedInstanceState);
    }

    /**
     * 请使用该方法来完成Fragment的onCreateView
     */
    @Nullable
    public abstract View onCreateContentView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                switchRealVisible();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                switchRealVisible();
            }
        });

        initViewsByAnnotation();
    }

    private void initHandleActivityResultMethods() {
        HashMap<Integer, Method> map = new HashMap<>();
        Class<?> cls = getClass();
        while (cls != Object.class) {
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                HandleActivityResultOK handleActivityResult = method.getAnnotation(HandleActivityResultOK.class);
                if (handleActivityResult != null) {
                    try {
                        cls.getDeclaredMethod(method.getName(), Intent.class);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(method + " 定义了@HandleActivityResultOK，但是参数不合法！" +
                                "应该定义为：" + method.getName() + "(Intent data)");
                    }
                    int requestCode = handleActivityResult.value();
                    Method m = map.get(requestCode);
                    if (m != null) {
                        throw new RuntimeException(method + " 和 " + m +
                                " 在 @HandleActivityResultOK 中定义了相同的value，不合法！");
                    }
                    map.put(requestCode, method);
                }
            }
            if (cls == RootFragment.class) {
                break;
            }
            cls = cls.getSuperclass();
        }
        for (Map.Entry<Integer, Method> entry : map.entrySet()) {
            int requestCode = entry.getKey();
            final Method method = entry.getValue();
            addOnActivityResultListener(entry.getKey(), new RootActivity.OnActivityResultOKListener() {
                @Override
                protected boolean onResult(Intent data) {
                    try {
                        method.setAccessible(true);
                        method.invoke(RootFragment.this, data);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        }
    }

    private void initViewsByAnnotation() {
        Class<?> cls = getClass();
        while (cls != Object.class) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                ViewById viewByIdAnnotation = field.getAnnotation(ViewById.class);
                if (viewByIdAnnotation != null) {
                    handleViewById(field, viewByIdAnnotation);
                }
            }
            if (cls == RootFragment.class) {
                break;
            }
            cls = cls.getSuperclass();
        }
    }

    private void handleViewById(Field field, ViewById viewByIdAnnotation) {
        checkAnnotationOwner(field, viewByIdAnnotation);
        try {
            field.setAccessible(true);
            field.set(this, findViewById(viewByIdAnnotation.value()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void checkAnnotationOwner(Field field, Annotation annotation) {
        if (!View.class.isAssignableFrom(field.getType())) {
            throw new RuntimeException(field.getName() + " is not a view, cannot define annotation " + annotation);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mSavableImpl.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavableImpl.onSaveInstanceState(outState);
    }

    @SuppressLint("MissingSaveStateAnnotation")
    private boolean mRealVisible;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!switchRealVisible()) {
            return;
        }

        List<Fragment> fragments = null;
        try {
            //noinspection RestrictedApi
            fragments = getChildFragmentManager().getFragments();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 父Fragment切换UserVisibleHint时，强制调用子Fragment的setUserVisibleHint，以便让其有机会感知整体可见性的切换
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                try {
                    if (!(fragment instanceof RootFragment)) {
                        continue;
                    }
                    ((RootFragment) fragment).switchRealVisible();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        switchRealVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        switchRealVisible();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        switchRealVisible();
    }

    /**
     * 真正可见性的切换。
     * 只有Fragment已经resumed，并且自身以及往上所有父级Fragment，全部可见并UserVisibleHint为true，才算是真正可见；其它都算不可见
     * @param visible
     */
    @CallSuper
    public void onVisibleChanged(boolean visible) {
        if (visible) {
            MobclickAgent.onPageStart(getClass().getName());
        } else {
            MobclickAgent.onPageEnd(getClass().getName());
        }
    }

    public final boolean getRealVisible() {
        return mRealVisible;
    }

    private boolean switchRealVisible() {
        boolean oldVisible = mRealVisible;
        mRealVisible = isRealVisible();

        if (oldVisible == mRealVisible) {
            return false;
        }

        onVisibleChanged(mRealVisible);
        return true;
    }

    /**
     * 判断该Fragment是否真正可见。
     * @return 只有Fragment已经resumed，并且自身以及往上所有父级Fragment，全部可见并UserVisibleHint为true，才算是真正可见（返回true）。
     */
    private boolean isRealVisible() {
        Fragment fragment = this;
        while (fragment != null) {
            if (!fragment.isResumed() || !fragment.isVisible() || !fragment.getUserVisibleHint()) {
                return false;
            }
            fragment = fragment.getParentFragment();
        }
        return true;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Map.Entry<Integer, RootActivity.OnActivityResultListener> entry : mOnActivityResultMap.entrySet()) {
            if (requestCode == entry.getKey() && entry.getValue().onActivityResult(resultCode, data)) {
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 将监听器注入onActivityResult回调（自动生成requestCode）
     * @param requestCode 只截取16位
     * @param listener
     */
    public void addOnActivityResultListener(int requestCode, @NonNull RootActivity.OnActivityResultListener listener) {
        int code = correctRequestCode(requestCode);
        if (mOnActivityResultMap.containsKey(code)) {
            throw new RuntimeException("addOnActivityResultListener with same requestCode: "
                    + listener + ", " + mOnActivityResultMap.get(code));
        }

        mOnActivityResultMap.put(code, listener);
    }

    /**
     * 根据requestCode（截取16位）获取监听器
     * @param requestCode 只截取16位
     * @return
     */
    public RootActivity.OnActivityResultListener getOnActivityResultListener(int requestCode) {
        return mOnActivityResultMap.get(correctRequestCode(requestCode));
    }

    /**
     * 从onActivityResult回调中移除该监听器（如果存在的话）
     * @param requestCode 只截取16位
     */
    public void removeOnActivityResultListener(int requestCode) {
        mOnActivityResultMap.remove(correctRequestCode(requestCode));
    }

    private int correctRequestCode(int requestCode) {
        return requestCode & 0xffff;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTaskManager.stop(null);
        mTaskStarter = null;

        mInnerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHasDestroyed = true;
    }

    @SuppressWarnings("unchecked")
    public final <T extends View> T findViewById(@IdRes int id) {
        View view = getView();
        return view == null ? null : (T) view.findViewById(id);
    }

    public TaskStarter getTaskStarter() {
        return mTaskStarter;
    }

    public boolean hasDestroyed() {
        return mHasDestroyed;
    }


    private final Handler mInnerHandler = new InnerHandler(this);

    public final Handler getInnerHandler() {
        return mInnerHandler;
    }

    /**
     * 子类可重写该方法，实现内部消息投递
     * @param msg
     */
    protected void handleInnerMessage(Message msg) {

    }

    private static class InnerHandler extends Handler {
        WeakReference<RootFragment> mFragmentRef;

        InnerHandler(RootFragment fragment) {
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            RootFragment fragment = mFragmentRef.get();
            if (fragment != null) {
                fragment.handleInnerMessage(msg);
            }
        }
    }
}
