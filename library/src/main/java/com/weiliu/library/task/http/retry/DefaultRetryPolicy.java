/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weiliu.library.task.http.retry;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

/**
 * Default retry policy for requests.
 */
public class DefaultRetryPolicy implements RetryPolicy {
    /** The current connect timeout in milliseconds. */
    private int mCurrentTimeoutMs;
    /** The current read timeout in milliseconds. */
    private int mCurrentReadTimeoutMs;

    /** The current retry count. */
    private int mCurrentRetryCount;

    /** The maximum number of attempts. */
    private int mMaxNumRetries;

    /** The backoff multiplier for the policy. */
    private final float mBackoffMultiplier;

    private DebugInterrupter mDebugInterrupter;

    /** cache map of url -> retry url list */
    private final Map<String, RetryConfig.Entry> mCachedRetryMap = new Hashtable<>();

    /** The default socket timeout in milliseconds */
    public static final int DEFAULT_TIMEOUT_MS = 2500;

    /** The default number of retries */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /** The default backoff multiplier */
    public static final float DEFAULT_BACKOFF_MULT = 1f;

    @Nullable
    private static volatile RetryConfig sRetryConfig;

    /**
     * Constructs a new retry policy using the default timeouts.
     */
    public DefaultRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT);
    }

    /**
     * Constructs a new retry policy.
     *
     * @param initialTimeoutMs
     *            The initial connect timeout for the policy.
     * @param initialReadTimeoutMs
     *            The initial read timeout for the policy.
     * @param maxNumRetries
     *            The maximum number of retries.
     * @param backoffMultiplier
     *            Backoff multiplier for the policy.
     */
    public DefaultRetryPolicy(int initialTimeoutMs, int initialReadTimeoutMs, int maxNumRetries, float backoffMultiplier) {
        mCurrentTimeoutMs = initialTimeoutMs;
        mCurrentReadTimeoutMs = initialReadTimeoutMs;
        mMaxNumRetries = maxNumRetries;
        mBackoffMultiplier = backoffMultiplier;
    }

    public void setDefaultTimeoutMs(int timeoutMs) {
        mCurrentTimeoutMs = timeoutMs;
    }

    public void setDefaultReadTimeoutMs(int timeoutMs) {
        mCurrentReadTimeoutMs = timeoutMs;
    }

    public void setDefaultMaxRetries(int maxRetries) {
        mMaxNumRetries = maxRetries;
    }

    /**
     * App业务接口失败自动重试域名替换方案的配置。<br/>
     * <b>注意，这里会覆盖所有接口的域名重试机制，切勿随便调用！！！</b>
     * @param retryConfig
     */
    public static void setRetryConfig(@Nullable RetryConfig retryConfig) {
        sRetryConfig = retryConfig;
    }

    /**
     * Returns the current timeout.
     */
    @Override
    public int getCurrentTimeout(String originUrl, String method) {
        RetryConfig config = getRetryConfig();
        if (config == null) {
            return mCurrentTimeoutMs;
        }

        // 命中了才设置RetryConfig里的配置
        if (getRetryHostEntry(originUrl, method) == null) {
            return mCurrentTimeoutMs;
        }

        if (mCurrentRetryCount == 0) {
            return config.mainHostTimeout > 0 ? config.mainHostTimeout * 1000 : mCurrentTimeoutMs;
        }
        return config.backupHostTimeout > 0 ? config.backupHostTimeout * 1000 : mCurrentTimeoutMs;
    }

    @Override
    public int getCurrentReadTimeout(String originUrl, String method) {
        RetryConfig config = getRetryConfig();
        if (config == null) {
            return mCurrentReadTimeoutMs;
        }

        // 命中了才设置RetryConfig里的配置
        if (getRetryHostEntry(originUrl, method) == null) {
            return mCurrentReadTimeoutMs;
        }

        if (mCurrentRetryCount == 0) {
            return config.mainHostTimeout > 0 ? config.mainHostTimeout * 1000 : mCurrentReadTimeoutMs;
        }
        return config.backupHostTimeout > 0 ? config.backupHostTimeout * 1000 : mCurrentReadTimeoutMs;
    }

    /**
     * Returns the current retry count.
     */
    @Override
    public int getCurrentRetryCount(String originUrl, String method) {
        return mCurrentRetryCount;
    }

    @Override
    public int getCurrentRetryInterval(String originUrl, String method) {
        RetryConfig config = getRetryConfig();
        if (config == null) {
            return 0;
        }

        // 命中了才设置RetryConfig里的配置
        if (getRetryHostEntry(originUrl, method) == null) {
            return 0;
        }

        return config.retryInterval * 1000;
    }

    /**
     * Returns the backoff multiplier for the policy.
     */
    public float getBackoffMultiplier() {
        return mBackoffMultiplier;
    }

    @Override
    public DebugInterrupter getDebugInterrupter() {
        return mDebugInterrupter;
    }

    @Override
    public void setDebugInterrupter(DebugInterrupter debugInterrupter) {
        mDebugInterrupter = debugInterrupter;
    }

    /**
     * Prepares for the next retry by applying a backoff to the timeout.
     *
     * @param error
     *            The error code of the last attempt.
     */
    @Override
    public String retry(String originUrl, String method, Exception error) throws Exception {
        mCurrentRetryCount++;
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if (!hasAttemptRemaining(originUrl, method)) {
            throw error;    //终止重试
        }

        RetryConfig.Entry retryEntry = getRetryHostEntry(originUrl, method);

        // 应用域名重试方案
        if (retryEntry != null) {
            int index = mCurrentRetryCount - 1;
            String newHost = index < retryEntry.backup.size() ? retryEntry.backup.get(index) : null;
            if (!TextUtils.isEmpty(newHost)) {
                URL uri = new URL(originUrl);
                String host = uri.getHost();
                String finalUrl = originUrl.replace(host, newHost);
                if (retryEntry.scheme != null && index < retryEntry.scheme.size()) {
                    String newScheme = retryEntry.scheme.get(index);
                    if (!TextUtils.isEmpty(newScheme)) {
                        String scheme = uri.getProtocol();
                        finalUrl = finalUrl.replaceFirst(scheme, newScheme);
                    }
                }
                return finalUrl;
            }

            // 应用了域名重试方案，但又没找到可以替换的域名，则终止重试
            throw error;
        }

        // 没有应用域名重试方案，则使用默认处理
        return null;
    }

    /**
     * Returns true if this policy has attempts remaining, false otherwise.
     */
    protected boolean hasAttemptRemaining(String originUrl, String method) {
//        RetryConfig config = getRetryConfig();
//        if (config == null) {
//            return mCurrentRetryCount <= mMaxNumRetries;
//        }
//
//        if (getRetryHostList(originUrl, method) == null) {
//            return mCurrentRetryCount <= mMaxNumRetries;
//        }
//        return true;
        // 不管怎样，都最多只能重试mMaxNumRetries次
        return mCurrentRetryCount <= mMaxNumRetries;
    }

    /**
     * 是否应用域名重试机制。<br/>
     * 默认策略是：凡是POST请求一律不应用域名重试机制，子类可以重写该方法。
     * @param originUrl 原url（全路径）
     * @param method 请求的method，如"POST", "GET"等。
     * @return true表示应用
     */
    protected boolean shouldApplyRetryConfig(String originUrl, String method) {
        return !"POST".equalsIgnoreCase(method);
    }

    /**
     * 返回命中的重试域名列表
     * @param originUrl
     * @param method
     * @return 为null表示未命中，也意味着该url将不启用重试域名替换机制
     */
    private RetryConfig.Entry getRetryHostEntry(String originUrl, String method) {
        if (!shouldApplyRetryConfig(originUrl, method)) {
            return null;
        }

        RetryConfig config = getRetryConfig();
        if (config == null) {
            return null;
        }

        try {
            URL uri = new URL(originUrl);
            String host = uri.getHost();
            String path = uri.getPath();

            // 缓存命中
            RetryConfig.Entry cachedRetryEntry = mCachedRetryMap.get(originUrl);
            if (cachedRetryEntry != null) {
                return cachedRetryEntry;
            }

            // 黑名单（host/path）过滤
            for (String exclude : config.specialPath) {
                int slashIndex = exclude.indexOf('/');
                if (slashIndex == -1) {
                    continue;
                }
                String excludeHost = exclude.substring(0, slashIndex);
                String excludePath = exclude.substring(slashIndex, exclude.length());
                if (excludeHost.equals(host) && excludePath.equals(path)) {
                    return null;
                }
            }

            // 白名单（host列表）命中
            for (RetryConfig.Entry entry : config.retryHosts) {
                if (!entry.host.equals(host)) {
                    continue;
                }

                if (entry.backup.isEmpty()) {
                    continue;
                }

                // 设置缓存
                mCachedRetryMap.put(originUrl, entry);
                return entry;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 默认返回全局的RetryConfig配置。子类可以重写该方法，应用自己的RetryConfig。
     * 返回空表示不应用任何域名重试机制。
     * @return
     */
    @Nullable
    protected RetryConfig getRetryConfig() {
        RetryConfig config = sRetryConfig;
        if (config == null || config.retryStatus == 0) {
            return null;
        }
        return config;
    }
}

