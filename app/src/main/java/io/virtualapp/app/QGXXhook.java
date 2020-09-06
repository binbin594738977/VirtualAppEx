package io.virtualapp.app;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.lody.virtual.client.VClientHookManager;

import library.ClassUtil;
import library.WeiliuLog;


public class QGXXhook extends VClientHookManager.CallbackAdapter {

    @Override
    public void onActivityCreate(Activity activity) {
        super.onActivityCreate(activity);
        if (TextUtils.equals("com.alibaba.lightapp.runtime.activity.CommonWebViewActivity", activity.getClass().getName())) {
            WeiliuLog.log("activiti");
        }
    }


    @Override
    public void onActivityResume(Activity activity) {
        super.onActivityResume(activity);
        if (TextUtils.equals("com.alibaba.lightapp.runtime.activity.CommonWebViewActivity", activity.getClass().getName())) {
            try {
                WeiliuLog.log("activiti");
                Object webview = ClassUtil.getFieldValue(activity, "c");
                //addJavascriptInterface()
                ClassUtil.invokeMethod(webview, "addJavascriptInterface"
                        , new Class[]{Object.class, String.class}
                        , new Object[]{new InJavaScriptLocalObj(), "java_obj"});

                // 获取页面内容
                loadUrl(webview, "javascript:window.java_obj.showSource("
                        + "document.getElementsByTagName('html')[0].innerHTML);");

                // 获取解析<meta name="share-description" content="获取到的值">
                loadUrl(webview, "javascript:window.java_obj.showDescription("
                        + "document.querySelector('meta[name=\"share-description\"]').getAttribute('content')"
                        + ");");
            } catch (Exception e) {
                WeiliuLog.log(e);
            }
        }
    }

    public void loadUrl(Object webview, String url) throws Exception {
        ClassUtil.invokeMethod(webview, "webview"
                , new Class[]{String.class}
                , new Object[]{url});

    }


    /**
     * jsObj
     */
    public final class InJavaScriptLocalObj {

        public InJavaScriptLocalObj() {
        }

        @JavascriptInterface
        public void showSource(String html) {
            WeiliuLog.longLog(html);
        }

        @JavascriptInterface
        public void showDescription(String str) {
            WeiliuLog.longLog(str);
        }
    }
}
