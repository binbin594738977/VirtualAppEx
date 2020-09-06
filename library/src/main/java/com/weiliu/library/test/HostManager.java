package com.weiliu.library.test;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;
import com.weiliu.library.RootApplication;
import com.weiliu.library.json.JsonUtil;
import com.weiliu.library.util.Utility;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HostManager {

	public static final String CONFIG_FILE_NAME = "test_config.ini";
	
	private static final String PREF_ENABLE_CONFIG = "PREF_ENABLE_CONFIG";
	
	private HostManager() {
		
	}
	
	/**
	 * 重写Host配置列表
	 * @param list host配置列表
	 * @return true表示改写成功，false表示失败
	 */
	public static boolean writeHostConfigs(List<HostConfig> list) {
		String str = JsonUtil.genericObjectToJsonString(list, new TypeToken<List<HostConfig>>() { }.getType());
        boolean saved = false;
        try {
        	Application application = RootApplication.getInstance();
            saved = Utility.stringToStream(str,
            		application.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        return saved;
	}
	
	/**
	 * 读取Host配置列表
	 * @return host配置列表，不会为null
	 */
	public static List<HostConfig> readHostConfigs() {
		List<HostConfig> configs;
		try {
			Application application = RootApplication.getInstance();
            String str = Utility.streamToString(application.openFileInput(CONFIG_FILE_NAME));
            configs = JsonUtil.jsonStringToList(str, HostConfig.class);
        } catch (FileNotFoundException e) {
        	configs = new ArrayList<>();

        	writeHostConfigs(configs);
        }
		return configs;
	}
	
	/**
	 * 启用或者关闭Host配置机制
	 * @param enable true表示启用，false表示关闭
	 */
	public static void enableHostConfig(boolean enable) {
		Application application = RootApplication.getInstance();
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);
		Editor editor = p.edit();
		editor.putBoolean(PREF_ENABLE_CONFIG, enable);
		editor.apply();
	}
	
	/**
	 * Host配置机制是否开启
	 * @return
	 */
	public static boolean isHostConfigEnable() {
		Application application = RootApplication.getInstance();
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(application);
		return p.getBoolean(PREF_ENABLE_CONFIG, false);
	}
	
	/**
	 * 如果Host配置机制开启，并且url的host命中了配置列表，则返回结果
	 * @param url
	 * @return
	 */
	public static HostAnalyzeResult analyze(String url) {
		if (!isHostConfigEnable()) {
			return null;
		}
		
		List<HostConfig> configs = readHostConfigs();
		if (configs.isEmpty()) {
			return null;
		}
		
		try {
			URL uri = new URL(url);
			String host = uri.getHost();
			if (TextUtils.isEmpty(host)) {
				return null;
			}
			for (HostConfig config : configs) {
				String regex = config.host.replace(".", "\\.");
				regex = regex.replace("*", ".*");
				if (config.enable && host.matches(regex)) {
					HostAnalyzeResult result = new HostAnalyzeResult();
					result.host = host;
					result.originUrl = url;
					result.replacedUrl = url.replace(host, config.getIpHost());
					return result;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
