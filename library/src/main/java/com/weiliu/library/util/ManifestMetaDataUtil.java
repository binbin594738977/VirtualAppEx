package com.weiliu.library.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/**
 * 
 * 获取Mainfest文件中定义的元数据的值 工具类
 *
 */
public class ManifestMetaDataUtil {
	
	private ManifestMetaDataUtil() {
		
	}
	
	private static Object readKey(@NonNull Context context, String keyName) {

		try {

			ApplicationInfo appi = context.getPackageManager()
					.getApplicationInfo(context.getPackageName(),
							PackageManager.GET_META_DATA);

			Bundle bundle = appi.metaData;

            return bundle.get(keyName);

		} catch (NameNotFoundException e) {
			return null;
		}

	}

	@Nullable
	public static String getString(@NonNull Context context, String keyName) {
		return (String) readKey(context, keyName);
	}

	@Nullable
	public static Boolean getBoolean(@NonNull Context context, String keyName) {
		return (Boolean) readKey(context, keyName);
	}

	@Nullable
	public static Object get(@NonNull Context context, String keyName) {
		return readKey(context, keyName);
	}
	
	/**
	 * 获取当前版本
	 * @param context
	 * @return
	 */
	public static int getVersionCode(@NonNull Context context) {
		return getVersionCode(context, context.getPackageName());
	}

	/**
	 * 获取app版本
	 * @param context
	 * @param packageName app包名
	 * @return
	 */
	public static int getVersionCode(@NonNull Context context, String packageName) {
		try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static String getVersionName(@NonNull Context context) {
		return getVersionName(context, context.getPackageName());
	}

	public static String getVersionName(@NonNull Context context, String packageName) {
		try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}
}
