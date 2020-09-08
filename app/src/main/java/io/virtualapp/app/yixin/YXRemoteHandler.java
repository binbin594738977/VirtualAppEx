package io.virtualapp.app.yixin;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.lody.virtual.os.VUserHandle;
import com.weiliu.library.json.JsonInterface;
import com.weiliu.library.task.TaskStarter;
import com.weiliu.library.task.http.HttpCallBack;

import java.lang.ref.WeakReference;

import io.virtualapp.MyUtil;
import io.virtualapp.app.AppHook;
import io.virtualapp.core.BaseApplication;
import io.virtualapp.core.BaseCallback;
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
            com.weiliu.library.WeiliuLog.log(3, "结果1:" + type);
            if (type == 221) {
                Object a = (Object) ClassUtil.invokeMethod(message.obj, "a");
                int a_a = (int) ClassUtil.getFieldValue(a, "a");
                com.weiliu.library.WeiliuLog.log(3, "结果2:" + a_a);
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
        com.weiliu.library.WeiliuLog.log(3, "成功数据");
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

    long mNumber = 0;
    long mMaxNumber = 0;
    public final String MAX_NUMBER = "MaxNumber";
    public final String NUMBER = "Number";


    public void satrtTask() {
        mNumber = YiXinHook.get().sp.getLong(NUMBER, 0);
        mMaxNumber = YiXinHook.get().sp.getLong(MAX_NUMBER, 0);

        AppHook.getInstance().mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        mNumber++;
                        if (mNumber == 0 || mMaxNumber == 0 || mNumber >= mMaxNumber) {
                            com.weiliu.library.WeiliuLog.log("请求数据");
                            requestNumber();
                            Thread.sleep(2000);
                            continue;
                        }
                        YiXinHook.get().searchPhoneNumber(String.valueOf(mNumber));
                        YiXinHook.get().sp.edit().putLong(NUMBER, mNumber).commit();

                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void requestNumber() {
        BaseUrlParams up = new BaseUrlParams("yixin", "requestNumber");
        up.getParams().put("id", VUserHandle.myUserId());
        taskStarter.startTask(up, new BaseCallback<RequestNumberData>() {


            @Override
            public void success(RequestNumberData resultData, @Nullable String info) {
                if (resultData != null) {
                    WeiliuLog.log(resultData.toString());
                    YiXinHook.get().sp.edit().putLong(MAX_NUMBER, resultData.maxNumber).commit();
                    YiXinHook.get().sp.edit().putLong(NUMBER, resultData.number).commit();
                    mNumber = resultData.number;
                    mMaxNumber = resultData.maxNumber;

                }
            }

            @Override
            public void failed(@Nullable RequestNumberData resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e) {
                com.weiliu.library.WeiliuLog.log(3, "请求失败了");
            }
        });
    }


    public static class RequestNumberData implements JsonInterface {
        public long number;
        public long maxNumber;

        @Override
        public String toString() {
            return "RequestNumberData{" +
                    "number=" + number +
                    ", maxNumber=" + maxNumber +
                    '}';
        }
    }

}
