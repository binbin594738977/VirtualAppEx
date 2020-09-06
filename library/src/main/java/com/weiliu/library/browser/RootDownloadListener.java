package com.weiliu.library.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.widget.Toast;

import com.weiliu.library.R;

public class RootDownloadListener implements DownloadListener {

    private Context mContext;
    private RootWebView mWebView;
    
    public RootDownloadListener(RootWebView webView) {
        mContext = webView.getContext();
        mWebView = webView;
    }
    
    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                String mimetype, long contentLength) {
        if (!openVideoOnDownloadStart(url, contentDisposition, mimetype)) {
            myDownloadStart((Activity) mContext, mWebView, url, userAgent, contentDisposition,
                    mimetype, contentLength);
        }
    }

    /**
     * 视频要打开，不下载
     * 
     * @param url
     *            视频地址
     * @param contentDisposition
     *            contentDisposition
     * @param mimetype
     *            视频MimeType
     * @return 是否打开了视频
     */
    public boolean openVideoOnDownloadStart(String url, @Nullable String contentDisposition, @Nullable String mimetype) {
        // 只打开视频文件，其它的让其下载
        if (mimetype != null && mimetype.startsWith("video/")) {
            final String attachmentKey = "attachment";
            if (contentDisposition == null 
                    || !contentDisposition.regionMatches(true, 0, attachmentKey, 0, attachmentKey.length())) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), mimetype);
                // 查询是否存在有可以打开此类型的应用程序，如果有，让该应用程序打开此数据
                ResolveInfo info = mContext.getPackageManager().resolveActivity(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                if (info != null) {
                    Activity act = (Activity) mContext;
                    ComponentName myName = act.getComponentName();
                    if (!myName.getPackageName().equals(info.activityInfo.packageName)
                            || !myName.getClassName().equals(info.activityInfo.name)) {
                        try {
                            act.startActivity(intent);
                            return true;
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return false;
    }
    
    
    /**
     * 开始下载
     * @param activity activity
     * @param url url
     * @param userAgent userAgent
     * @param contentDisposition contentDisposition
     * @param mimetype mimetype
     * @param contentLength contentLength
     */
    public static void myDownloadStart(@NonNull Activity activity, @NonNull RootWebView webView,
                                       @NonNull String url, String userAgent,
                                       @Nullable String contentDisposition, String mimetype, long contentLength) {
        final String attachment = "attachment";
        if (!canHandledBySelf(mimetype) //如果自己能处理该mimetype，则不询问系统.
                && (contentDisposition == null
                || !contentDisposition.regionMatches(
                        true, 0, attachment, 0, attachment.length()))) {
            // query the package manager to see if there's a registered handler
            //     that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimetype);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            /**添加结束。*/
            ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                ComponentName myName = activity.getComponentName();
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (!myName.getPackageName().equals(
                        info.activityInfo.packageName)
                        || !myName.getClassName().equals(
                                info.activityInfo.name)) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        activity.startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        ex.printStackTrace();
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        } else {
            onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                    mimetype, contentLength);
        }

//        if (!webView.canGoBack()) {
//            activity.finish();
//        }
    }
    
    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for this type.
     * @param activity Activity
     * @param url The full url to the content that should be downloaded
     * @param userAgent user agent
     * @param contentDisposition Content-disposition http header, if
     *                           present.
     * @param mimetype The mimetype of the content reported by the server
     * @param contentLength The file size reported by the server
     */
    /*package */static void onDownloadStartNoStream(@NonNull Activity activity, @NonNull String url,
                                                    String userAgent, String contentDisposition,
                                                    @Nullable String mimetype, long contentLength) {
//        String filename = URLUtil.guessFileName(url,
//                contentDisposition, mimetype);
//
//        // java.net.URI is a lot stricter than KURL so we have to encode some
//        // extra characters. Fix for b 2538060 and b 1634719
//        WebAddress webAddress;
//        try {
//            webAddress = new WebAddress(url);
//            webAddress.setPath(encodePath(webAddress.getPath()));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }

        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);

        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        String cookies = CookieManager.getInstance().getCookie(url);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.addRequestHeader("Cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setMimeType(mimetype);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
//        request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, filename);
        dm.enqueue(request);

        Toast.makeText(activity, R.string.download_started, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * This is to work around the fact that java.net.URI throws Exceptions
     * instead of just encoding URL's properly
     * Helper method for onDownloadStartNoStream.
     * @param path web path
     * @return encode path.
     * */
    @NonNull
    private static String encodePath(@NonNull String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']') {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
    
    /**
     * 是否自己能处理该mimetype。
     * @param mimetype String
     * @return  自己可以处理该mimetype，则返回true
     */
    private static boolean canHandledBySelf(String mimetype) {
//        return TextUtils.equals(mimetype, "application/vnd.android.package-archive");
        return true;
    }
}
