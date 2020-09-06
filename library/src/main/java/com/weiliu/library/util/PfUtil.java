package com.weiliu.library.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 *
 */
public class PfUtil {

    private SharedPreferences mSharedPreferences;

    private SharedPreferences.Editor mEdit;

    private static final PfUtil INSTANCE = new PfUtil();

    private PfUtil() {

    }

    @NonNull
    public static PfUtil getInstance() {
        return INSTANCE;
    }

    @SuppressLint("CommitPrefEdits")
    public void init(@NonNull Context context, String file) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(file, 0);
            mEdit = mSharedPreferences.edit();
        }
    }

    public void setAdShow(boolean adShow) {
        putBoolean("adShow", adShow);
    }

    public boolean getAdShow() {
        return getBoolean("adShow", false);
    }

    public String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    public Long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    public Float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    public Boolean getBoolean(String key, Boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public Integer getInt(String key, Integer defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    public void putInt(String key, Integer value) {
        mEdit.putInt(key, value);
        apply(mEdit);
    }

    public void putString(String key, String value) {
        mEdit.putString(key, value);
        apply(mEdit);
    }

    public void putLong(String key, long value) {
        mEdit.putLong(key, value);
        apply(mEdit);
    }

    public void putBoolean(String key, Boolean value) {
        mEdit.putBoolean(key, value);
        apply(mEdit);
    }

    public void putFloat(String key, float value) {
        mEdit.putFloat(key, value);
        apply(mEdit);
    }

    public void remove(String key) {
        mEdit.remove(key);
        apply(mEdit);
    }

    public static void apply(@NonNull SharedPreferences.Editor editor) {
        editor.commit();
    }
}
