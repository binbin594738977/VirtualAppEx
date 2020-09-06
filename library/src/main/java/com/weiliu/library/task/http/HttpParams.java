package com.weiliu.library.task.http;

import android.support.annotation.Nullable;

import com.weiliu.library.json.JsonInterface;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Http请求的参数，包括get，header，body参数
 * <br/>
 * Created by qumiao on 2016/8/26.
 */
public class HttpParams implements JsonInterface, Serializable {
    private TreeMap<String, String> get = new TreeMap<>();
    private TreeMap<String, String> header = new TreeMap<>();
    private TreeMap<String, String> body = new TreeMap<>();
    private String bodyJson;
    private String bodyXml;
    private MultiParts multiParts;

    private boolean isBodyGZip;

    public HttpParams() {
        this(null);
    }

    public HttpParams(HttpParams params) {
        if (params == null) {
            return;
        }
        get.putAll(params.get);
        header.putAll(params.header);
        body.putAll(params.body);
        bodyJson = params.bodyJson;
        bodyXml = params.bodyXml;
        multiParts = params.multiParts;
        isBodyGZip = params.isBodyGZip;
    }

    public void refresh() {

    }

    /**
     * 设置参数键值对。
     * @param key
     * @param value
     * @return
     */
    public HttpParams put(String key, String value) {
        return put(key, value, null);
    }

    /**
     * 设置参数键值对。
     * @param key
     * @param value
     * @return
     */
    public HttpParams put(String key, Number value) {
        return put(key, String.valueOf(value), null);
    }

    /**
     * 设置参数键值对
     * @param key
     * @param value
     * @param type get表示放在url参数中；header表示放在请求头中；body表示放在post体中
     * @return
     */
    public HttpParams put(String key, String value, Type type) {
        Map<String, String> p = getParams(type);
        p.put(key, value);
        return this;
    }

    /**
     * 设置参数键值对
     * @param key
     * @param value
     * @param type get表示放在url参数中；header表示放在请求头中；body表示放在post体中
     * @return
     */
    public HttpParams put(String key, Number value, Type type) {
        Map<String, String> p = getParams(type);
        p.put(key, String.valueOf(value));
        return this;
    }

    /**
     * 批量设置参数键值对。
     * @param params
     * @return
     */
    public HttpParams putParams(Map<String, String> params) {
        return putParams(params, null);
    }

    /**
     * 批量设置参数键值对
     * @param params
     * @param type get表示放在url参数中；header表示放在请求头中；body表示放在post体中
     * @return
     */
    public HttpParams putParams(Map<String, String> params, Type type) {
        Map<String, String> p = getParams(type);
        p.putAll(params);
        return this;
    }

    /**
     * 移除参数键值对
     * @param key
     * @return
     */
    public String remove(String key) {
        return remove(key, null);
    }

    /**
     * 移除参数键值对
     * @param key
     * @param type get表示放在url参数中；header表示放在请求头中；body表示放在post体中
     * @return
     */
    public String remove(String key, Type type) {
        Map<String, String> p = getParams(type);
        return p.remove(key);
    }

    /**
     * 获取参数值
     * @param key
     * @return
     */
    public String get(String key) {
        return get(key, null);
    }

    /**
     * 获取参数值
     * @param key
     * @param type get表示url参数中；header表示请求头中；body表示post体中
     * @return
     */
    public String get(String key, Type type) {
        Map<String, String> p = getParams(type);
        return p.get(key);
    }

    /**
     * 批量获取参数键值对
     * @return
     */
    public Map<String, String> getParams() {
        return getParams(null);
    }

    /**
     * 批量获取参数键值对
     * @param type get表示url参数中；header表示请求头中；body表示post体中
     * @return
     */
    public Map<String, String> getParams(Type type) {
        if (type == null) {
            return body;
        }
        Map<String, String> p;
        switch (type) {
            case HEADER:
                p = header;
                break;
            case BODY:
                p = body;
                break;
            default:
                p = get;
        }

        return p;
    }

    /**
     * 以json形式写入post体的内容
     * @return
     */
    public String getBodyJson() {
        return bodyJson;
    }

    /**
     * 以json形式写入post体的内容
     * @param bodyJson
     */
    public HttpParams setBodyJson(String bodyJson) {
        this.bodyJson = bodyJson;
        return this;
    }

    /**
     * 以xml形式写入post体的内容
     * @return
     */
    public String getBodyXml() {
        return bodyXml;
    }

    /**
     * 以xml形式写入post体的内容
     * @param bodyXml
     */
    public HttpParams setBodyXml(String bodyXml) {
        this.bodyXml = bodyXml;
        return this;
    }

    /**
     * 以多段数据写入POST体的内容
     * @return
     */
    public MultiParts getMultiParts() {
        return multiParts;
    }

    /**
     * 以多段数据写入POST体的内容
     * @param multiParts
     */
    public HttpParams setMultiParts(MultiParts multiParts) {
        this.multiParts = multiParts;
        return this;
    }

    /**
     * POST内容是否以GZIP形式传输
     * @return
     */
    public boolean isBodyGZip() {
        return isBodyGZip;
    }

    /**
     * POST内容是否以GZIP形式传输
     * @param bodyGZip
     */
    public HttpParams setBodyGZip(boolean bodyGZip) {
        isBodyGZip = bodyGZip;
        return this;
    }




    private final Collection<String> mExcludedFormKeys = new HashSet<>();

    /**
     * 添加 不参与缓存哈希计算的表单key（get、post以及header中的表单key）
     * @param excludedFormKeys
     */
    public void addExcludedFormKeys(@Nullable Collection<String> excludedFormKeys) {
        if (excludedFormKeys == null || excludedFormKeys.isEmpty()) {
            return;
        }
        mExcludedFormKeys.addAll(excludedFormKeys);
    }

    /**
     * 是否为参与缓存哈希计算的表单key（get、post以及header中的表单key）
     * @param paramKey
     */
    protected boolean isParamServiceForCacheKey(String paramKey) {
        return !mExcludedFormKeys.contains(paramKey);
    }


    public enum Type {
        GET,
        HEADER,
        BODY
    }
}
