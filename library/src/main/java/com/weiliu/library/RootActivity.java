package com.weiliu.library;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.weiliu.library.sr.RootSavableImpl;
import com.weiliu.library.task.TaskManager;
import com.weiliu.library.task.TaskProgressDialog;
import com.weiliu.library.task.TaskStarter;
import com.weiliu.library.util.PermissionUtil;
import com.weiliu.library.util.PhotoUtil;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * 所有Activity的基类，与app无关。
 *
 * Created by qumiao on 16/4/27.
 */
public abstract class RootActivity extends AppCompatActivity {

    private static final boolean DEBUG = true;

    private static final int PERMISSIONS_REQUEST_CAMERA = 1001;

    @SaveState
    private String mCameraPhotoPath;
    @SaveState
    private String mCropPhotoPath;
    @SaveState
    private boolean mCrop;


    private TaskManager mTaskManager;
    private TaskProgressDialog mDialog;

    @Nullable
    private TaskStarter mTaskStarter;

    private final Set<WebView> mWebViewSet = new HashSet<>();

    private final Map<Integer, OnActivityResultListener> mOnActivityResultMap = new Hashtable<>();

    private final RootSavableImpl mSavableImpl = new RootSavableImpl(this);

    private boolean mIsDestroyed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();

        mTaskManager = new TaskManager();

        mDialog = new TaskProgressDialog(this);
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);

        mTaskStarter = new TaskStarter(this, mTaskManager, mDialog);

        mSavableImpl.onCreate(savedInstanceState);

        initHandleActivityResultMethods();
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        initActionBar();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void setActionBarTitle(CharSequence title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public void setActionBarTitle(@StringRes int title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public void setActionBarHomeButtonEnabled(boolean enabled) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(enabled);
        }
    }

    public void setActionBarDisplayHomeAsUpEnabled(boolean enabled) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }

    public void setActionBarHomeAsUpIndicator(Drawable drawable) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(drawable);
        }
    }

    public void setActionBarHomeAsUpIndicator(@DrawableRes int drawable) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(drawable);
        }
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
            if (cls == RootActivity.class) {
                break;
            }
            cls = cls.getSuperclass();
        }
        for (Map.Entry<Integer, Method> entry : map.entrySet()) {
            int requestCode = entry.getKey();
            final Method method = entry.getValue();
            addOnActivityResultListener(entry.getKey(), new OnActivityResultOKListener() {
                @Override
                protected boolean onResult(Intent data) {
                    try {
                        method.setAccessible(true);
                        method.invoke(RootActivity.this, data);
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

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        initViewsByAnnotation();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initViewsByAnnotation();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        initViewsByAnnotation();
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
            if (cls == RootActivity.class) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mSavableImpl.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavableImpl.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Map.Entry<Integer, OnActivityResultListener> entry : mOnActivityResultMap.entrySet()) {
            if (requestCode == entry.getKey() && entry.getValue().onActivityResult(resultCode, data)) {
                return;
            }
        }

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PhotoUtil.PICK_PHOTO_FROM_GALLERY:
                    PhotoUtil.getPhotoFromStorageResult(this, data, new PhotoUtil.OnPickPhotoListener() {
                        @Override
                        public void onPickPhoto(File file) {
                            if (mCrop) {
                                PhotoUtil.cropPhoto(RootActivity.this, null, file, createCropPath());
                            } else {
                                handlePhoto(file);
                            }
                        }
                    });
                    break;
                case PhotoUtil.TAKE_PHOTO_FROM_CAMERA:
                    if (mCameraPhotoPath != null) {
                        File photoFile = new File(mCameraPhotoPath);
                        if (mCrop) {
                            PhotoUtil.cropPhoto(this, null, photoFile, createCropPath());
                        } else {
                            handlePhoto(photoFile);
                        }
                    }
                    break;
                case PhotoUtil.CROP_PHOTO:
                    // 魅族手机可能取不到 data.getData()
                    if (null != data.getData()) {
                        handlePhoto(new File(PhotoUtil.getTempPhotoDir(), data.getData().getLastPathSegment()));
                    } else if (!TextUtils.isEmpty(mCropPhotoPath) && new File(mCropPhotoPath).isFile()) {
                        handlePhoto(new File(mCropPhotoPath));
                    } else {
                        Toast.makeText(this, R.string.get_image_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 将监听器注入onActivityResult回调（自动生成requestCode）
     * @param requestCode 只截取16位
     * @param listener
     */
    public void addOnActivityResultListener(int requestCode, @NonNull OnActivityResultListener listener) {
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
    public OnActivityResultListener getOnActivityResultListener(int requestCode) {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                // BEGIN_INCLUDE(permission_result)
                // Received permission result for camera permission.
                // Check if the only required permission has been granted
                if (grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPhotoPath = PhotoUtil.openCamera(this, null).getAbsolutePath();
                }
                // END_INCLUDE(permission_result)
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }


    protected void openGallery(boolean crop) {
        mCrop = crop;
        PhotoUtil.openGallery(this, null);
    }

    protected void openCamera(boolean crop) {
        mCrop = crop;
        if (PermissionUtil.requestPermissionIfNeed(
                this, Manifest.permission.CAMERA, null,
                PERMISSIONS_REQUEST_CAMERA)) {
            mCameraPhotoPath = PhotoUtil.openCamera(this, null).getAbsolutePath();
        }
    }

    private String createCropPath() {
        mCropPhotoPath = new File(PhotoUtil.getTempPhotoDir(), System.currentTimeMillis() + ".jpg")
                .getAbsolutePath();
        return mCropPhotoPath;
    }

    /**
     * 子类可以重载此方法处理图片（来源于相册、拍照，或者之后的裁剪等）
     * @param photoFile 图片文件
     */
    protected void handlePhoto(File photoFile) {

    }


    public void addWebView(WebView webView) {
        if (mIsDestroyed) {
            if (DEBUG) {
                throw new RuntimeException(getClass().getName() + " has destroyed, can not add WebView any more");
            }
        }
        mWebViewSet.add(webView);
    }



    /*package*/ TaskProgressDialog getTaskProgressDialog() {
        return mDialog;
    }


    public TaskStarter getTaskStarter() {
        return mTaskStarter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mTaskManager.stop(null);
        mTaskStarter = null;

        for (WebView webView : mWebViewSet) {
            webView.destroy();
        }

        mInnerHandler.removeCallbacksAndMessages(null);

        mIsDestroyed = true;
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    /**
     * 调用{@link RootActivity#addOnActivityResultListener(int, OnActivityResultListener)}，
     * 可以将该监听器注入onActivityResult回调。
     */
    public interface OnActivityResultListener {
        /**
         * Activity.onActivityResult
         * @param resultCode
         * @param data
         * @return 是否消耗掉Activity.onActivityResult的后续执行
         */
        boolean onActivityResult(int resultCode, Intent data);
    }

    /**
     * 该监听器会筛选出RESULT_OK结果并执行
     */
    public abstract static class OnActivityResultOKListener implements OnActivityResultListener {

        @Override
        public boolean onActivityResult(int resultCode, Intent data) {
            if (resultCode != RESULT_OK) {
                return false;
            }
            return onResult(data);
        }

        /**
         * RESULT_OK情况下的结果
         * @param data
         * @return 是否消耗掉Activity.onActivityResult的后续执行
         */
        protected abstract boolean onResult(Intent data);
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
        WeakReference<RootActivity> mActivityRef;

        InnerHandler(RootActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RootActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.handleInnerMessage(msg);
            }
        }
    }
}
