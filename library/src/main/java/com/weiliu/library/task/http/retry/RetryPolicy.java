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

import java.io.IOException;

/**
 * Retry policy for a request.
 */
public interface RetryPolicy {

    /**
     * Returns the current connect timeout (used for logging).
     */
    public int getCurrentTimeout(String originUrl, String method);

    /**
     * Returns the current read timeout (used for logging).
     */
    public int getCurrentReadTimeout(String originUrl, String method);

    /**
     * Returns the current retry count (used for logging).
     */
    public int getCurrentRetryCount(String originUrl, String method);

    /**
     * Returns the current retry interval.
     */
    public int getCurrentRetryInterval(String originUrl, String method);

    /**
     * Prepares for the next retry by applying a backoff to the timeout.
     * 
     * @param error
     *            The error code of the last attempt.
     * @throws Exception
     *             In the event that the retry could not be performed (for
     *             example if we
     *             ran out of attempts), the passed in error is thrown.
     * @return followed retry url replaced if return is not null
     */
    @Nullable
    public String retry(String originUrl, String method, Exception error) throws Exception;


    public void setDebugInterrupter(DebugInterrupter debugInterrupter);

    public DebugInterrupter getDebugInterrupter();


    public interface DebugInterrupter {
        void onDebugInterrupt(int currentRetryCount) throws IOException;
    }
}
