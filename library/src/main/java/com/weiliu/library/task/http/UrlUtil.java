package com.weiliu.library.task.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Map;

/**
 * Url 相关的处理
 * <br/>
 * Created by qumiao on 2016/8/19.
 */
class UrlUtil {

    private UrlUtil() {
        //no instance
    }


    /**
     * 在url后批量增加多个参数。对于每个参数，如果url中已有该参数，则将该参数的value替换为新值.
     * @param url 在url附加参数
     * @param params 多个参数的key - value键值对
     * @return 加完（或修改完）参数后的url.
     */
    public static String addParams(String url, @Nullable Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                url = addParam(url, entry.getKey(), entry.getValue());
            }
        }
        return url;
    }

    /**
     * 在url后增加参数, 如果已有该参数，则将该参数的value替换为新值.
     * @param url 在url附加参数
     * @param key 参数key
     * @param value 参数value
     * @return 加完（或修改完）参数后的url.
     */
    public static String addParam(String url, String key, String value) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        String[] subUrlHolder = new String[1];
        url = removeSubUrl(url, subUrlHolder);

        key += '=';

        int index = url.indexOf('?');
        if (index < 0) { //原来没有参数
            return addFirstParam(url, key, value) + subUrlHolder[0];
        }

        //在'?'后查找是否参数已存在
        int keyIndex = url.indexOf('&' + key, index);
        if (keyIndex == -1) {
            keyIndex = url.indexOf('?' + key, index);
        }
        if (keyIndex != -1) { //已经存在
            return replaceParam(url, value, keyIndex + 1 + key.length()) + subUrlHolder[0];
        }

        return addFollowedParam(url, key, value) + subUrlHolder[0];
    }

    /**
     * 加上第一个参数
     * @param url 在url附加参数
     * @param key 参数key（带"="）
     * @param value 参数value
     * @return 加完参数后的url.
     */
    @NonNull
    private static String addFirstParam(String url, String key, String value) {
        return url + '?' + key + value;
    }

    /**
     * 追加参数
     * @param url 在url附加参数
     * @param key 参数key（带"="）
     * @param value 参数value
     * @return 加完参数后的url.
     */
    private static String addFollowedParam(@NonNull String url, String key, String value) {
        StringBuilder sb = new StringBuilder(url);
        if (!url.endsWith("&") && !url.endsWith("?")) {	//SUPPRESS CHECKSTYLE
            sb.append('&');
        }
        sb.append(key).append(value);
        return sb.toString();
    }

    /**
     * 替换参数值
     * @param url 即将替换参数值的url
     * @param value 参数的新值
     * @param valueStart 原参数值在url中的位置
     * @return 替换完参数值的url
     */
    private static String replaceParam(@NonNull String url, String value, int valueStart) {
        int valueEnd = url.indexOf('&', valueStart);
        if (valueEnd == -1) {
            valueEnd = url.length();
        }

        StringBuilder sb = new StringBuilder(url);
        sb.replace(valueStart, valueEnd, value);

        return sb.toString();
    }

    /**
     * 删除参数
     * @param url 原url
     * @param paramName 参数名
     * @return 删除完参数后的url
     */
    @NonNull
    public static String removeParam(@NonNull String url, String paramName) {
        String value;
        String[] subUrlHolder = new String[1];
        String ret = removeSubUrl(url, subUrlHolder);

        if ((value = getQueryValue(new StringBuilder(ret), '?' + paramName + '=')) // SUPPRESS CHECKSTYLE
                != null) {
            if (ret.endsWith(value)) {
                ret = ret.replace(paramName + '=' + value, "");
            } else {
                ret = ret.replace(paramName + '=' + value + "&", "");
            }
            if (ret.endsWith("?")) {
                ret = ret.substring(0, ret.length() - 1);
            }
        } else if ((value = getQueryValue(new StringBuilder(ret), '&' + paramName + '=')) 	// SUPPRESS CHECKSTYLE
                != null) {
            ret = ret.replace('&' + paramName + '=' + value, "");
        }

        return ret + subUrlHolder[0];
    }

    /**
     * 获取url中指定参数的值
     * @param url Url
     * @param paramName 参数名
     * @return 参数值
     */
    @Nullable
    public static String getParamValue(@NonNull String url, String paramName) {
        String value;
        StringBuilder sb = new StringBuilder(removeSubUrl(url, null));

        if ((value = getQueryValue(sb, '?' + paramName + '=')) == null) {	// SUPPRESS CHECKSTYLE
            value = getQueryValue(sb, '&' + paramName + '=');
        }

        return value;
    }

    @NonNull
    private static String removeSubUrl(@NonNull String url, @Nullable String[] subUrlHolder) {
        int indexHash = url.indexOf('#');
        if (indexHash < 0) { //没有#
            if (subUrlHolder != null) {
                subUrlHolder[0] = "";
            }
            return url;
        }

        if (subUrlHolder != null) {
            subUrlHolder[0] = url.substring(indexHash);
        }
        return url.substring(0, indexHash);
    }

    private static String getQueryValue(@NonNull StringBuilder sb, @NonNull String query) {
        int index = sb.indexOf(query);

        if (index != -1) {
            int startIndex = index + query.length();
            int endIndex = sb.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = sb.length();
            }

            return sb.substring(startIndex, endIndex);
        }

        return null;
    }
}
