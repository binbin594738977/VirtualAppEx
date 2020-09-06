package com.weiliu.library.browser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.weiliu.library.R;
import com.weiliu.library.RootActivity;
import com.weiliu.library.util.ManifestMetaDataUtil;
import com.weiliu.library.util.Utility;

import java.net.URLEncoder;

public class RootWebView extends WebView {

    private static final String TAG = "RootWebView";


    /** 获取当前点击的链接的HREF。 */
    private static final int GET_FOCUS_NODE_HREF = 102;
    /** 复制当前点击的链接的HREF。 */
    private static final int COPY_FOCUS_NODE_HREF = 103;
    /** 保存当前点击的链接的HREF。 */
    private static final int SAVE_FOCUS_NODE_HREF = 104;
    /** 新窗口打开当前点击的链接的HREF。 */
    private static final int NEW_WINDOW_FOCUS_NODE_HREF = 105;
    /** 分享当前点击的链接的HREF。 */
    private static final int SHARE_FOCUS_NODE_HREF = 106;


    /** 缓存目录。 */
    public static final String APP_CACHE_PATH = "appcache";
    /** 数据库目录。 */
    public static final String APP_DATABASE_PATH = "databases";
    /** 地理位置定位信息目录。 */
    public static final String APP_GEO_PATH = "geolocation";
    /**
     * 滑动监听
     */
    private OnScrollListener mOnScrollListener;
    
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private WebViewClient mWebViewClient;
    private WebChromeClient mWebChromeClient;

    private String mDefaultUserAgent;
    
    public RootWebView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public RootWebView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RootWebView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private static String sUserAgent;

    public static String getUserAgent() {
        return sUserAgent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init(@NonNull Context context) {
        initLayerType();

        WebSettings settings = getSettings();
        String versionName = ManifestMetaDataUtil.getVersionName(context);
        int versionCode = ManifestMetaDataUtil.getVersionCode(context);
        mDefaultUserAgent = settings.getUserAgentString() + "sqxbs/" + versionName + "/" + versionCode;
        
        supportHtml5(settings);
        
        settings.setSaveFormData(true);
        settings.setSavePassword(true);

        settings.setLightTouchEnabled(false);
        settings.setNeedInitialFocus(false);
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);
        // 若此项打开，在destroy时会出现WindowLeaked问题
//        settings.setBuiltInZoomControls(true);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // 暂不支持打开多窗口 FIXME
        settings.setSupportMultipleWindows(false);
            
        try {
            settings.setPluginState(PluginState.ON_DEMAND);
        } catch (NoClassDefFoundError e) {  //真烦恼，有些机型上木有。。。
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        settings.setUserAgentString(mDefaultUserAgent);
        sUserAgent = mDefaultUserAgent;

        setDownloadListener(new RootDownloadListener(this));
        setWebViewClient(new RootWebViewClient(this));
        setWebChromeClient(new RootWebChromeClient(Utility.getActivity(context), null) {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                if (!super.onCreateWindow(view, dialog, userGesture, resultMsg)) {
                    if (mOnNewWindowListener != null && mOnNewWindowListener.supportNewWindow(view)) {
                        WebView newView = mOnNewWindowListener.openInNewWindow(view, null, dialog, userGesture);
                        WebViewTransport transport = (WebViewTransport) resultMsg.obj;
                        transport.setWebView(newView);
                        resultMsg.sendToTarget();
                        return true;
                    }
                }
                return false;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
        attachToRootActivity();
    }

    public void appendUserAgent(String append) {
        mDefaultUserAgent += append;
        getSettings().setUserAgentString(mDefaultUserAgent);
        sUserAgent = mDefaultUserAgent;
    }

    private void attachToRootActivity() {
        Activity activity = Utility.getActivity(getContext());
        if (activity instanceof RootActivity) {
            RootActivity rootActivity = (RootActivity) activity;
            rootActivity.addWebView(this);
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initLayerType() {
        // 加上此处逻辑的原因参考MRECIPE-2446
        // 现将其废弃，因为会影响同款机器（P7-L00）上的话题CSS显示
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            if (TextUtils.equals(Build.MODEL, "HUAWEI P7-L00")) {
//                setLayerType(LAYER_TYPE_SOFTWARE, null);
//            }
//        }
    }
    
    
    /**
     * 支持Html5
     * @param settings
     */
    private void supportHtml5(@NonNull WebSettings settings) {
        settings.setGeolocationEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheEnabled(true);
        String databasePath = getContext().getDir(APP_DATABASE_PATH, 0).getPath();
        String geolocationDatabasePath = getContext().getDir(APP_GEO_PATH, 0).getPath();
        String appCachePath = getContext().getDir(APP_CACHE_PATH, 0).getPath();
        settings.setGeolocationDatabasePath(geolocationDatabasePath);
        settings.setDatabasePath(databasePath);
       // settings.setAppCacheMaxSize(appCacheMaxSize);
        settings.setAppCachePath(appCachePath);
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(client);
        mWebViewClient = client;
    }
    
    @Override
    public void setWebChromeClient(WebChromeClient client) {
        super.setWebChromeClient(client);
        mWebChromeClient = client;
    }

    /**
     * 长按等操作触发的ContextMenu。
     * */
    public void enableContextMenu() {
        setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {
                if (v != RootWebView.this) {
                    return;
                }
                Activity activity = Utility.getActivity(v.getContext());
                WebView.HitTestResult result = getHitTestResult();
                if (result == null) {
                    return;
                }

                int type = result.getType();
                if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
                    Log.w(TAG, "We should not show context menu when nothing is touched");
                    return;
                }
                if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
                    // let TextView handles context menu
                    return;
                }

                // Note, http://b/issue?id=1106666 is requesting that
                // an inflated menu can be used again. This is not available
                // yet, so inflate each time (yuk!)
                MenuInflater inflater = activity.getMenuInflater();
                inflater.inflate(R.menu.browser_menu, menu);

                // Show the correct menu group
                String extra = result.getExtra();
                menu.setGroupVisible(R.id.PHONE_MENU,
                        type == WebView.HitTestResult.PHONE_TYPE);
                menu.setGroupVisible(R.id.EMAIL_MENU,
                        type == WebView.HitTestResult.EMAIL_TYPE);
                menu.setGroupVisible(R.id.GEO_MENU,
                        type == WebView.HitTestResult.GEO_TYPE);
                menu.setGroupVisible(R.id.IMAGE_MENU,
                        type == WebView.HitTestResult.IMAGE_TYPE
                                || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                menu.setGroupVisible(R.id.ANCHOR_MENU,
                        type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                                || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);

                // Setup custom handling depending on the type
                Intent intent;
                switch (type) {
                    case WebView.HitTestResult.PHONE_TYPE:
                        menu.setHeaderTitle(Uri.decode(extra));
                        menu.findItem(R.id.dial_context_menu_id).setIntent(
                                new Intent(Intent.ACTION_VIEW, Uri
                                        .parse(WebView.SCHEME_TEL + extra)));
                        intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, Uri.decode(extra));
                        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                        menu.findItem(R.id.add_contact_context_menu_id).setIntent(intent);
                        menu.findItem(R.id.copy_phone_context_menu_id).setOnMenuItemClickListener(
                                new Copy(extra));
                        break;

                    case WebView.HitTestResult.EMAIL_TYPE:
                        menu.setHeaderTitle(extra);
                        intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(WebView.SCHEME_MAILTO + extra));
                        menu.findItem(R.id.email_context_menu_id).setIntent(intent);
                        menu.findItem(R.id.copy_mail_context_menu_id).setOnMenuItemClickListener(
                                new Copy(extra));
                        break;

                    case WebView.HitTestResult.GEO_TYPE:
                        menu.setHeaderTitle(extra);
                        intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(WebView.SCHEME_GEO + URLEncoder.encode(extra)));
                        menu.findItem(R.id.map_context_menu_id).setIntent(intent);
                        menu.findItem(R.id.copy_geo_context_menu_id).setOnMenuItemClickListener(
                                new Copy(extra));
                        break;

                    case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                    case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
//                        TextView titleView = (TextView) LayoutInflater.from(activity)
//                                .inflate(android.R.layout.browser_link_context_header, null);
//                        titleView.setText(extra);
//                        menu.setHeaderView(titleView);
                        menu.setHeaderTitle(extra);
                        menu.findItem(R.id.open_newtab_context_menu_id).setVisible(
                                mOnNewWindowListener != null
                                        && mOnNewWindowListener.supportNewWindow((WebView) v));
                        /*menu.findItem(R.id.bookmark_context_menu_id).setVisible(
                                Bookmarks.urlHasAcceptableScheme(extra));*/

                        PackageManager pm = activity.getPackageManager();
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("text/plain");
                        ResolveInfo ri = pm.resolveActivity(send, PackageManager.MATCH_DEFAULT_ONLY);
                        menu.findItem(R.id.share_link_context_menu_id).setVisible(ri != null);
                        if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                            break;
                        }
                        // otherwise fall through to handle image part
                    case WebView.HitTestResult.IMAGE_TYPE:
                        if (type == WebView.HitTestResult.IMAGE_TYPE) {
                            menu.setHeaderTitle(extra);
                        }
//                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(extra));
//                        menu.findItem(R.id.view_image_context_menu_id).setIntent(intent);
                        menu.findItem(R.id.view_image_context_menu_id)
                                .setOnMenuItemClickListener(new ViewImage(extra));
                        menu.findItem(R.id.download_context_menu_id).
                                setOnMenuItemClickListener(new Download(extra));
                            /*menu.findItem(R.id.set_wallpaper_context_menu_id).
                                    setOnMenuItemClickListener(new SetAsWallpaper(extra));*/
                        break;

                    default:
                        Log.w(TAG, "We should not get here.");
                        break;
                }
            }
        });

    }

    private void setItemSelectedListener() {

    }

    public boolean onContextMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        boolean result = false;
        Message msg;
        if (id == R.id.open_context_menu_id) {
            msg = mHandler.obtainMessage(GET_FOCUS_NODE_HREF, id, 0, null);
            requestFocusNodeHref(msg);
            result = true;
        }

        if (id == R.id.copy_link_context_menu_id) {
            msg = mHandler.obtainMessage(COPY_FOCUS_NODE_HREF, id, 0, null);
            requestFocusNodeHref(msg);
            result = true;

        }

        if (id == R.id.save_link_context_menu_id) {
            msg = mHandler.obtainMessage(SAVE_FOCUS_NODE_HREF, id, 0, null);
            requestFocusNodeHref(msg);
            result = true;
        }

        if (id == R.id.open_newtab_context_menu_id) {
            msg = mHandler.obtainMessage(NEW_WINDOW_FOCUS_NODE_HREF, id, 0, null);
            requestFocusNodeHref(msg);
            result = true;
        }

        if (id == R.id.share_link_context_menu_id) {
            msg = mHandler.obtainMessage(SHARE_FOCUS_NODE_HREF, id, 0, null);
            requestFocusNodeHref(msg);
            result = true;
        }

        return result;
    }


    /**
     * 有些机型的webview destroy会出现tts Service not registered的错误，蛋疼。
     * FIXME 暂时先简单拦截了，以后找到根本原因再完整修复
     */ 
    @Override
    public void destroy() {
        mHandler.removeCallbacksAndMessages(null);
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        try {
            super.destroy();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取默认的UA。因为WebSettings.getDefaultUserAgent方法只有API 17以上才有，所以用该方法来代替
     * @return
     */
    public String getDefaultUserAgent() {
        return mDefaultUserAgent;
    }
    
    /**
     * 上传文件相关的回调。如果要实现上传文件功能，应该在对应的Activity里调用该方法。
     * @param requestCode requestCode
     * @param resultCode resultCode
     * @param data data
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (mWebChromeClient instanceof RootWebChromeClient) {
            RootWebChromeClient client = (RootWebChromeClient) mWebChromeClient;
            return client.handleActivityResult(requestCode, resultCode, data);
        }
        return false;
    }



    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollChanging(l, t, oldl, oldt);
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public interface OnScrollListener {
        void onScrollChanging(int x, int y, int oldx, int oldy);
    }

    /** 在mHandler中增加对url返回ding部分数据的处理. */
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String url = (String) msg.getData().get("url");
            switch (msg.what) {
                case GET_FOCUS_NODE_HREF:
                    loadUrl(url);
                    break;

                case COPY_FOCUS_NODE_HREF:
                    Utility.copyToClipboard(url, getContext());
                    break;

                case SAVE_FOCUS_NODE_HREF:
                    RootDownloadListener.onDownloadStartNoStream(
                            Utility.getActivity(getContext()), url, null, null, null, -1);
                    break;

                case NEW_WINDOW_FOCUS_NODE_HREF:
                    mOnNewWindowListener.openInNewWindow(RootWebView.this, url, false, false);
                    break;

                case SHARE_FOCUS_NODE_HREF:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                    sendIntent.setType("text/plain");
                    getContext().startActivity(sendIntent);
                    break;

                default:
                    break;
            }
        }
    };

    private OnNewWindowListener mOnNewWindowListener;

    public void setOnNewWindowListener(OnNewWindowListener listener) {
        mOnNewWindowListener = listener;
    }

    public OnNewWindowListener getOnNewWindowListener() {
        return mOnNewWindowListener;
    }

    public interface OnNewWindowListener {
        boolean supportNewWindow(WebView webView);
        WebView openInNewWindow(WebView srcWebView, String url, boolean dialog, boolean useGesture);
    }

    /**
     * 响应"复制"。
     * */
    private class Copy implements MenuItem.OnMenuItemClickListener {
        /**被复制的字串。*/
        private CharSequence mText;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Utility.copyToClipboard(mText.toString(), getContext());
            return true;
        }

        /**
         * 构造函数。
         * @param toCopy 字串。
         * */
        public Copy(CharSequence toCopy) {
            mText = toCopy;
        }
    }

    /**
     * 响应"保存图片"。
     * */
    private class Download implements MenuItem.OnMenuItemClickListener {
        /**图片的URL。*/
        private String mUrl;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            RootDownloadListener.onDownloadStartNoStream(Utility.getActivity(getContext()),
                    mUrl, null, null, null, -1);
            return true;
        }

        /**
         * 构造函数。
         * @param toDownload 链接。
         * */
        public Download(String toDownload) {
            mUrl = toDownload;
        }
    }

    /**
     * 响应"保存图片"。
     * */
    private class ViewImage implements MenuItem.OnMenuItemClickListener {
        /**图片的URL。*/
        private String mUrl;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            loadUrl(mUrl);
            return true;
        }

        /**
         * 构造函数。
         * @param url 链接。
         * */
        public ViewImage(String url) {
            mUrl = url;
        }
    }

}
