package com.weiliu.library.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.weiliu.library.R;
import com.weiliu.library.util.NoProguard;


public class RootWebChromeClient extends WebChromeClient implements NoProguard {
    
    private Fragment mFragment;
    private Activity mActivity;
    private WebChromeClient mDelegateClient;
    
    private View mCustomView;
    private FullscreenHolder mFullscreenContainer;
    private int mOriginalOrientation;
    private CustomViewCallback mCustomViewCallback;
    
    private Handler mHandler;
    
    /** 用来控制WebView上传文件的Callback. */
    private UploadHandler mUploadHandler;
    
    /** 通知WebView上传文件的Callback. */
    @Nullable
    private ValueCallback<Uri> mUploadMessage;
    
    /** VideoProgress View*/
    private View mVideoProgressView;
    
    /** custom view layout param. */
    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
        new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT);
    
    public RootWebChromeClient(Activity activity, Fragment fragment) {
        mActivity = activity;
        mFragment = fragment;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void setDelegateClient(WebChromeClient delegateClient) {
        mDelegateClient = delegateClient;
    }
    
    /**
     * 上传文件相关的回调。如果要实现上传文件功能，应该在对应的Activity里调用该方法。
     * @param requestCode requestCode
     * @param resultCode resultCode
     * @param data data
     */
    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        boolean handled = false;
        switch (requestCode) {
        case UploadHandler.FILE_SELECTED:
            if (mUploadHandler != null) {
                mUploadHandler.onResult(resultCode, data);
                handled = true;
            }
            break;

        default:
            break;
        }
        
        return handled;
    }
    
    @Override
    public void onProgressChanged(WebView aView, int aNewProgress) {
        if (mDelegateClient != null) {
            mDelegateClient.onProgressChanged(aView, aNewProgress);
        }
    }

    @Override
    public void onReceivedTitle(WebView aView, String aTitle) {
        if (mDelegateClient != null) {
            mDelegateClient.onReceivedTitle(aView, aTitle);
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        if (mDelegateClient != null) {
            mDelegateClient.onReceivedIcon(view, icon);
        }
    }
    
    @Nullable
    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mDelegateClient != null) {
            return mDelegateClient.getDefaultVideoPoster();
        }
        return null;
    }

    // 解决视频播放全屏崩溃的问题
    @SuppressLint("InflateParams")
	@Override
    public View getVideoLoadingProgressView() {
        if (mDelegateClient != null) {
            View view = mDelegateClient.getVideoLoadingProgressView();
            if (view != null) {
                return view;
            }
        }
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            mVideoProgressView = inflater.inflate(R.layout.browser_video_loading_progress, null);
        }
        return mVideoProgressView;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
        if (mDelegateClient != null && mDelegateClient.onCreateWindow(view, dialog, userGesture, resultMsg)) {
            return true;
        }
        return super.onCreateWindow(view, dialog, userGesture, resultMsg);
    }

    @Override
    public void onCloseWindow(WebView window) {
        if (mDelegateClient != null) {
            mDelegateClient.onCloseWindow(window);
        }
    }

    // 解决视频播放不能横屏问题 BEGIN
    @Override
    public void onHideCustomView() {
        if (mDelegateClient != null) {
            mDelegateClient.onHideCustomView();
        }
        hideCustomView();
    }

    @Override
    public void onShowCustomView(@NonNull View view, @NonNull CustomViewCallback callback) {
        if (mDelegateClient != null) {
            mDelegateClient.onShowCustomView(view, callback);
        }

        onShowCustomView(view, mActivity.getRequestedOrientation(), callback);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void onShowCustomView(@NonNull View view, int requestedOrientation, @NonNull CustomViewCallback callback) {
        if (mDelegateClient != null) {
            mDelegateClient.onShowCustomView(view, requestedOrientation, callback);
        }
        showCustomView(view, requestedOrientation, callback);
    }
    // 解决视频播放不能横屏问题 END


    /**
     * For Android 4.1+
     * Tell the client to open a file chooser.
     * @param uploadFile A ValueCallback to set the URI of the file to upload.
     *      onReceiveValue must be called to wake up the thread.a
     * @param acceptType The value of the 'accept' attribute of the input tag
     *         associated with this file picker.
     * @param capture The value of the 'capture' attribute of the input tag
     *         associated with this file picker.
     */
    public void openFileChooser(ValueCallback<Uri> uploadFile, @NonNull String acceptType, String capture) {
        mUploadHandler = new UploadHandler(mActivity, mFragment);
        mUploadHandler.openFileChooser(uploadFile, acceptType, capture);
    }
    /**
     * For Android 3+
     * Tell the client to open a file chooser.
     * @param uploadMsg A ValueCallback to set the URI of the file to upload.
     *      onReceiveValue must be called to wake up the thread.a
     * @param acceptType The value of the 'accept' attribute of the input tag
     *         associated with this file picker.
     *         associated with this file picker.
     */
    public void openFileChooser(ValueCallback<Uri> uploadMsg, @NonNull String acceptType) {
         mUploadHandler = new UploadHandler(mActivity, mFragment);
         mUploadHandler.openFileChooser(uploadMsg, acceptType, null);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
            @NonNull GeolocationPermissions.Callback callback) {
        if (mDelegateClient != null) {
            mDelegateClient.onGeolocationPermissionsShowPrompt(origin, callback);
        } else {
            callback.invoke(origin, true, true);
        }
    }

    @Override
    public void onGeolocationPermissionsHidePrompt() {
        if (mDelegateClient != null) {
            mDelegateClient.onGeolocationPermissionsHidePrompt();
        }
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        return super.onJsAlert(view, url, message, result);
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        return super.onJsConfirm(view, url, message, result);
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        return super.onJsPrompt(view, url, message, defaultValue, result);
    }

    @Override
    public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
        return super.onJsBeforeUnload(view, url, message, result);
    }

    public boolean isCustomViewShowing() {
        return mCustomView != null;
    }
    
    @NonNull
    private Runnable mResetOrentationAction = new Runnable() {
        
        @Override
        public void run() {
            mActivity.setRequestedOrientation(mOriginalOrientation);
        }
    };
    
    /**
     * 隐藏 custom view。
     */
    private void hideCustomView() {
        if (mCustomView == null) {
            return;
        }

        setFullscreen(mActivity, false);
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        decor.removeView(mFullscreenContainer);
        mFullscreenContainer = null;
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();
        
        // 在小米2上全屏播放视频，如果改为横向播放，再退出时，立即setRequestedOrientation会导致WebViewCore崩溃。。。
        if (TextUtils.equals(Build.MODEL, "MI 2")) {
            final long delayMillis = 1000;
            mHandler.postDelayed(mResetOrentationAction, delayMillis);
        } else {
            mActivity.setRequestedOrientation(mOriginalOrientation);
        }
    }
    
    /**
     * 显示 customview.
     * 把回调的 custom view 添加到 layout中。显示出来。
     * 
     * @param view 要显示的 customview。
     * @param requestedOrientation  customview 申请的 orientation 
     * @param callback callback.
     */
    private void showCustomView(@NonNull View view, int requestedOrientation, @NonNull CustomViewCallback callback) {
        mHandler.removeCallbacks(mResetOrentationAction);
        
        // if a view already exists then immediately terminate the new one
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        
        mOriginalOrientation = mActivity.getRequestedOrientation();
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(mActivity);
        mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        mCustomView = view;
        setFullscreen(mActivity, true);
        mCustomViewCallback = callback;
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }
    
    /**
     * 设置 activity是否全屏.
     * @param activity Activity
     * @param enabled 是否全屏。
     */
    public void setFullscreen(@NonNull Activity activity, boolean enabled) {
        Window win = activity.getWindow();
        int flag = !enabled ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    
    /**
     * webview custom view full screen holder. as a container.
     */
    static class FullscreenHolder extends FrameLayout {

        /**
         * constructor.
         * @param ctx Context
         */
        public FullscreenHolder(@NonNull Context ctx) {
            super(ctx);
            //noinspection deprecation
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }

    }
}
