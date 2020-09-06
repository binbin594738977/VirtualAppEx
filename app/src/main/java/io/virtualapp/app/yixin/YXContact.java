package io.virtualapp.app.yixin;

import android.text.TextUtils;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import library.ClassUtil;
import library.WeiliuLog;


public class YXContact {
    public String address;
    public String birthday;
    public String bkimage;
    public String config;
    public String email;
    public Integer gender;
    public String mobile;
    public String mobileHash;
    public String nickname;
    public String photourl;
    public String signature;
    public String socials;
    public String uid;
    public String yid;


    public static YXContact get(Object obj) throws Exception {
        YXContact yxContact = new YXContact();
        Field[] declaredFields = YXContact.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            try {
                declaredField.set(yxContact, ClassUtil.getFieldValue(obj, declaredField.getName()));
            } catch (Exception e) {
                WeiliuLog.log(declaredField.getName() + "字段设置异常");
            }
        }
        return yxContact;
    }

    @Override
    public String toString() {
        return "YXContact{" +
                "address='" + address + '\'' +
                ", birthday='" + birthday + '\'' +
                ", bkimage='" + bkimage + '\'' +
                ", config='" + config + '\'' +
                ", email='" + email + '\'' +
                ", gender=" + gender +
                ", mobile='" + mobile + '\'' +
                ", mobileHash='" + mobileHash + '\'' +
                ", nickname='" + nickname + '\'' +
                ", photourl='" + photourl + '\'' +
                ", signature='" + signature + '\'' +
                ", socials='" + socials + '\'' +
                ", uid='" + uid + '\'' +
                ", yid='" + yid + '\'' +
                '}';
    }
}
