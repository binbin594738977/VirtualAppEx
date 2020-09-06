package com.weiliu.library.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.weiliu.library.util.NoProguard;

public class RootWebViewClient extends WebViewClient implements NoProguard {
    
    private Context mContext;

    private WebViewClient mDelegateClient;

    public RootWebViewClient(RootWebView webView) {
        super();
        mContext = webView.getContext();
    }

    public void setDelegateClient(WebViewClient delegateClient) {
        mDelegateClient = delegateClient;
    }

    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, String url) {
//        Uri uri = Uri.parse(url);
//        if (WebSchemeRedirect.handleWebClick((Activity) mContext, uri, null, false)) {
//            return true;
//        }
//
//        HostAnalyzeResult result = HostManager.analyze(url);
//        if (result != null) {
//            url = result.replacedUrl;
//            if (!TextUtils.isEmpty(result.host)) {
//                HashMap<String, String> additionalHttpHeaders = new HashMap<String, String>();
//                additionalHttpHeaders.put("Host", result.host);
//                view.loadUrl(url, additionalHttpHeaders);
//            } else {
//                view.loadUrl(url);
//            }
//            return true;
//        }
        //noinspection deprecation
        if (mDelegateClient != null && mDelegateClient.shouldOverrideUrlLoading(view, url)) {
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }
    
    @Override
    public void onLoadResource(WebView view, String url) {
        if (mDelegateClient != null) {
            mDelegateClient.onLoadResource(view, url);
        }
        super.onLoadResource(view, url);
    }
    
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (mDelegateClient != null) {
            mDelegateClient.onPageStarted(view, url, favicon);
        }
        super.onPageStarted(view, url, favicon);
    }
    
    @Override
    public void onPageFinished(WebView view, String url) {
        if (mDelegateClient != null) {
            mDelegateClient.onPageFinished(view, url);
        }
        super.onPageFinished(view, url);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieSyncManager.getInstance().sync();
        } else {
            CookieManager.getInstance().flush();
        }
    }
    
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (mDelegateClient != null) {
            mDelegateClient.onReceivedError(view, errorCode, description, failingUrl);
        }
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error) {
        if (mDelegateClient != null) {
            mDelegateClient.onReceivedSslError(view, handler, error);
        }
        handler.proceed();
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        if (mDelegateClient != null) {
            mDelegateClient.doUpdateVisitedHistory(view, url, isReload);
        }
    }
}
