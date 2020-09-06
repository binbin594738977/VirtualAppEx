package com.weiliu.library.task.http;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.google.gson.JsonObject;
import com.weiliu.library.json.JsonUtil;
import com.weiliu.library.task.TaskProgressDeliver;
import com.weiliu.library.task.TaskResult;
import com.weiliu.library.task.TaskWorker;
import com.weiliu.library.task.db.ResumeHttpTaskControl;
import com.weiliu.library.task.http.retry.RetryPolicy;
import com.weiliu.library.util.CollectionUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * http任务执行体
 * Created by qumiao on 2016/5/3.
 */
public class HttpWorker implements TaskWorker<HttpRequestObject, HttpResponseObject> {

    private static final boolean DEBUG = true;
    private static final String TAG = "HttpWorker";

    private static final int DEFAULT_TIME_OUT = 15000;

    /**
     * 重试次数
     */
    private static final int RETRY_COUNT = 1;
    private int mCurrentRetryCount;

    private boolean mCanceled;

    private HttpURLConnection mConnection;

    private RetryPolicy mRetryPolicy;

    private int mReadTimeOut = DEFAULT_TIME_OUT;
    private int mConnectTimeOut = DEFAULT_TIME_OUT;
    private int mRetryCount = RETRY_COUNT;

    private SSLContext mSSLContext;

    private Context mContext;


//    private static final String CONTENT_TYPE_HTML = "text/html";
//
//    private static final String CONTENT_TYPE_JSON = "application/json";

    public HttpWorker(@Nullable Context context) {
        mContext = context != null ? context.getApplicationContext() : null;
    }


    public void setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
    }

    /**
     * 设置读取的超时时间，单位为毫秒。
     *
     * @param timeOut 不能小于0
     */
    public void setReadTimeOut(int timeOut) {
        if (timeOut > 0) {
            mReadTimeOut = timeOut;
        }
    }

    /**
     * 设置连接的超时时间，单位为毫秒。
     *
     * @param timeOut 不能小于0
     */
    public void setConnectTimeOut(int timeOut) {
        if (timeOut > 0) {
            mConnectTimeOut = timeOut;
        }
    }

    /**
     * 设置请求失败后的重试次数。
     *
     * @param retryCount
     */
    public void setRetryCount(int retryCount) {
        mRetryCount = retryCount;
    }

    public void setSSLContext(SSLContext sslContext) {
        mSSLContext = sslContext;
    }

    /**
     * 执行请求
     */
    private HttpResponseObject execute(@Nullable TaskProgressDeliver progressDeliver,
                                       HttpRequestObject requestObject, boolean isResumingTask) throws Exception {
        if (requestObject.params == null) {
            requestObject.params = new HttpParams();
        }
        String originUrl = UrlUtil.addParams(requestObject.url,
                encodeMap(requestObject.params.getParams(HttpParams.Type.GET)));
        Map<String, String> headerMap = requestObject.params.getParams(HttpParams.Type.HEADER);
        Map<String, String> bodyMap = encodeMap(requestObject.params.getParams(HttpParams.Type.BODY));

        HttpTraceObject traceData = requestObject.traceData;
        traceData.setStart();
        traceData.setUrl(originUrl);
        traceData.setHeader(headerMap);
        traceData.setBody(bodyMap);

        boolean appendToResumeTaskListIfFailed = requestObject.appendToResumeTaskListIfFailed;

        RetryPolicy retryPolicy = mRetryPolicy;

        traceData.setFinalUrl(originUrl);

        MultiParts multiParts = requestObject.params.getMultiParts();

        String contentType;
        if (!TextUtils.isEmpty(requestObject.params.getBodyXml())) {
            contentType = HttpUtil.CONTENT_TYPE_XML;
        } else if (!TextUtils.isEmpty(requestObject.params.getBodyJson())) {
            contentType = HttpUtil.CONTENT_TYPE_JSON;
        } else if (multiParts != null) {
            contentType = HttpUtil.CONTENT_TYPE_MULTIPART + multiParts.getBoundary();
        } else {
            contentType = HttpUtil.CONTENT_TYPE_FORM_URLENCODE;
        }
        traceData.setContentType(contentType);

        String bodyStr = createBodyString(
                bodyMap, requestObject.params.getBodyJson(), requestObject.params.getBodyXml());
        traceData.setBodyText(bodyStr);
        traceData.setBodyGZip(requestObject.params.isBodyGZip());
        traceData.setMultiParts(requestObject.params.getMultiParts());
        String method = TextUtils.isEmpty(bodyStr) && multiParts == null ? HttpUtil.GET : HttpUtil.POST;
        traceData.setMethod(method);

        byte[] bodyStrData = getRawData(bodyStr, requestObject.params.isBodyGZip());
        long contentLength = multiParts != null ? multiParts.getLength() : bodyStrData.length;

        HttpResponseObject responseObject = null;
        OutputStream output = null;
        while (true) {
            try {
                if (retryPolicy != null && retryPolicy.getDebugInterrupter() != null) {
                    retryPolicy.getDebugInterrupter().onDebugInterrupt(retryPolicy.getCurrentRetryCount(originUrl, method));
                }

                mConnection = openConnection(traceData);
                setParams(traceData.getUrl(), traceData.getFinalUrl(), traceData.getMethod());
                setHeader(traceData.getHeader());
                if (CollectionUtil.isEmpty(traceData.getHeader()) ||
                        (!traceData.getHeader().containsKey("Cookie") && !traceData.getHeader().containsKey("cookie"))) {
                    setCookiesToRequest(traceData.getUrl());
                }

                traceData.setTcpStart();
                mConnection.setRequestMethod(method);
                switch (method) {
                    case HttpUtil.GET:
                        mConnection.setDoOutput(false);
                        break;
                    case HttpUtil.POST:
                        mConnection.setDoOutput(true);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mConnection.setFixedLengthStreamingMode(contentLength);
                        } else {
                            if (contentLength <= Integer.MAX_VALUE) {
                                mConnection.setFixedLengthStreamingMode((int) contentLength);
                            }
                        }
                        if (mConnection.getRequestProperty(HttpUtil.CONTENT_TYPE) == null) {
                            mConnection.setRequestProperty(HttpUtil.CONTENT_TYPE, contentType);
                        }
                        break;
                }
                mConnection.connect();
                traceData.setTcpEnd();

                traceData.setPostStart();
                if (multiParts != null) {
                    output = mConnection.getOutputStream();
                    multiParts.writeTo(output, contentLength, progressDeliver);
                    output.close();
                } else if (bodyStrData.length > 0) {
                    output = mConnection.getOutputStream();
                    output.write(bodyStrData);
                    output.close();
                }
                if (progressDeliver != null) {
                    progressDeliver.publishProgress(100, 0);
                }
                traceData.setPostEnd();

                syncCookiesFromResponse(traceData.getUrl());
                responseObject = handleResponse(requestObject.wholeResponse, requestObject.channel,
                        traceData, progressDeliver);
                traceData.setException(null);

                if (traceData.getHttpStatus() == HttpURLConnection.HTTP_OK) {
                    traceData.setEnd();
                    return responseObject;
                }
            } catch (Exception e) {
                if (handleException(traceData, e)) {
                    break;
                }
            } finally {
                Utility.close(output);
                if (mConnection != null) {
                    mConnection.disconnect();
                }

                if (mContext != null
                        && !canRetry(traceData.getHttpStatus()) && isResumingTask && appendToResumeTaskListIfFailed) {
                    ResumeHttpTaskControl.delete(mContext, Collections.singletonList(requestObject), false);
                }
            }

            if (!retry(traceData, retryPolicy)) {
                break;
            }
        }

        traceData.setEnd();

        if (mContext != null
                && !isResumingTask && appendToResumeTaskListIfFailed && canRetry(traceData.getHttpStatus())) {
            ResumeHttpTaskControl.write(mContext, Collections.singletonList(requestObject), true, false);
        }

        Exception e = traceData.getException();
        if (e != null) {
            throw e;
        }

        return responseObject;
    }

    @NonNull
    private byte[] getRawData(String str, boolean gzip) {
        if (TextUtils.isEmpty(str)) {
            return new byte[0];
        }
        byte[] data = str.getBytes();
        if (gzip) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutput = new GZIPOutputStream(outputStream);
                gzipOutput.write(data);
                gzipOutput.close();
                return outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * 是否进行重试
     *
     * @param traceData   如果原url需要变更成重试url，则可保存在该对象中
     * @param retryPolicy 重试策略
     * @return 如果需要重试，则返回true；否则返回false
     */
    private boolean retry(HttpTraceObject traceData, RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            if (mCurrentRetryCount >= mRetryCount) {
                return false;
            }
            mCurrentRetryCount++;
        } else {
            try {
                String retryUrl = retryPolicy.retry(
                        traceData.getUrl(), traceData.getMethod(), traceData.getException());
                if (retryUrl != null) {
                    traceData.setFinalUrl(retryUrl);
                }

                // 重试间隔
                int interval = retryPolicy.getCurrentRetryInterval(traceData.getUrl(), traceData.getMethod());
                if (interval > 0) {
                    Thread.sleep(interval);
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * 处理异常
     *
     * @param traceData 记录异常情况，并分配相应的异常http status
     * @param e
     * @return 如果需要中断后续的重试请求，则返回true；否则返回false
     */
    private boolean handleException(HttpTraceObject traceData, Exception e) {
        traceData.setException(e);
        if (e instanceof InterruptedIOException) {
            if ((e instanceof SocketTimeoutException)) {
                e.printStackTrace();
                traceData.setHttpStatus(HttpUtil.SC_TIME_OUT);
            } else {
                traceData.setException(null);   // 人为取消，不计入异常情况
                traceData.setHttpStatus(0);
                mCanceled = true;
                return true;
            }
        } else {
            e.printStackTrace();

            if (e instanceof RuntimeException) {
                return true;
            }

            // DNS解析失败，基本上是网络不通，直接中断后续重试
            if (e instanceof UnknownHostException) {
                traceData.setHttpStatus(HttpUtil.SC_UNKNOWN_HOST);
                return true;
            }
            // 网络连接被拒，如端口号被占用等，短时间内不可重连
            if ((e instanceof ConnectException) || (e instanceof SocketException)) {
                traceData.setHttpStatus(HttpUtil.SC_CONNECT);
                return true;
            }

            int code = traceData.getHttpStatus();
            if (code == 0) {
                traceData.setHttpStatus(HttpUtil.SC_UNKNOWN);
            } else if (!canRetry(code)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, String> encodeMap(Map<String, String> src) {
        HashMap<String, String> dst = new HashMap<>();
        if (src != null && !src.isEmpty()) {
            for (Map.Entry<String, String> entry : src.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                dst.put(Uri.encode(key), value != null ? Uri.encode(value) : null);
            }
        }
        return dst;
    }

    private boolean canRetry(int code) {
        return code < 200 || code >= 400;
    }

    /**
     * 设置HTTP请求头信息
     */
    private void setHeader(Map<String, String> header) {
        setDefaultHeader();

        if (header != null && header.size() > 0) {
            for (Map.Entry<String, String> e : header.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                if (value != null && value.contains("\n")) {
                    String[] vals = value.split("\n");
                    for (String val : vals) {
                        mConnection.addRequestProperty(name, val.trim());
                    }
                } else {
                    mConnection.addRequestProperty(name, value);
                }
            }
        }
    }

    private void setParams(String originUrl, String finalUrl, String method) {
        mConnection.setDoInput(true);
        RetryPolicy retryPolicy = mRetryPolicy;
        int overrideTimeOut = retryPolicy != null ? retryPolicy.getCurrentTimeout(originUrl, method) : 0;
        int readTimeOut = overrideTimeOut > 0 ? overrideTimeOut : mReadTimeOut;
        int connectTimeOut = overrideTimeOut > 0 ? overrideTimeOut : mConnectTimeOut;
        mConnection.setReadTimeout(readTimeOut);
        mConnection.setConnectTimeout(connectTimeOut);
    }

    /**
     * 将post里的字段封装
     *
     * @param parameters
     * @param bodyJson
     * @param bodyXml
     * @return
     */
    private static String createBodyString(Map<String, String> parameters, String bodyJson, String bodyXml) {
        if (!TextUtils.isEmpty(bodyXml)) {
            return bodyXml;
        }
        if (!TextUtils.isEmpty(bodyJson)) {
            return bodyJson;
        }
        StringBuilder sb = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (!firstParameter) {
                    sb.append(HttpUtil.PARAMETER_DELIMITER);
                }

                String name = entry.getKey();
                String value = entry.getValue();
                sb.append(name)
                        .append(HttpUtil.PARAMETER_EQUALS_CHAR)
                        .append(!TextUtils.isEmpty(value) ? value : "");

                firstParameter = false;
            }
        }
        return sb.toString();
    }

    /**
     * 设置默认头信息
     */
    private void setDefaultHeader() {
        mConnection.setRequestProperty("Charset", "UTF-8");
        mConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
//        mConnection.setRequestProperty("User-Agent", PhoneInfoUtil.getUserAgent());
    }

    private boolean setCookiesToRequest(String url) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(url);
            if (!TextUtils.isEmpty(cookie)) {
                mConnection.setRequestProperty("Cookie", cookie);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean syncCookiesFromResponse(String url) {
        try {
            // 不能直接用 getHeaderField，否则它会把同一field下面的多个value抹掉成一个value
//            String cookie = mConnection.getHeaderField("Set-Cookie");
            Map<String, List<String>> fields = mConnection.getHeaderFields();
            List<String> cookies = fields.get("Set-Cookie");
            if (CollectionUtil.isEmpty(cookies)) {
                return false;
            }
            CookieManager cookieManager = CookieManager.getInstance();
            for (String cookie : cookies) {
                cookieManager.setCookie(url, cookie);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //noinspection deprecation
                CookieSyncManager.getInstance().sync();
            } else {
                cookieManager.flush();
            }

            String newCookie = cookieManager.getCookie(url);
            return TextUtils.isEmpty(newCookie);

        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 解析url，创建连接
     */
    private HttpURLConnection openConnection(HttpTraceObject traceData) throws Exception {
        traceData.setDnsStart();

        HttpURLConnection connection;

        URL uri = new URL(traceData.getFinalUrl());
        String host = uri.getHost();
        try {
            InetAddress address = InetAddress.getByName(host);
            traceData.setIp(address.getHostAddress());
        } catch (Exception ignore) {
            // 此处主要是为了拿IP地址，发生的错误可以忽略（比如测试的host就无法DNS解析）
        }


        String protocol = uri.getProtocol();
        switch (protocol) {
            case HttpUtil.PROTOCOL_HTTP:
                connection = (HttpURLConnection) uri.openConnection();
                break;

            case HttpUtil.PROTOCOL_HTTPS:
                if (mSSLContext == null) {
                    mSSLContext = SSLContext.getInstance("TLS");
                    mSSLContext.init(null, new TrustManager[]{new TrustAllManager()}, null);
                }

                connection = (HttpsURLConnection) uri.openConnection();

                ((HttpsURLConnection) connection).setSSLSocketFactory(mSSLContext.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {

                    // TODO 暂时接受所有的host
                    @SuppressLint("BadHostnameVerifier")
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                break;

            default:
                throw new UnsupportedProtocolException("not support protocol");
        }
        traceData.setDnsEnd();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Connection", "close");
        return connection;
    }

    /**
     * HttpResponse 处理函数
     */
    private HttpResponseObject handleResponse(boolean wholeResponse, @Nullable HttpBinaryChannel channel,
                                              HttpTraceObject traceData, @Nullable TaskProgressDeliver progressDeliver) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {

            traceData.setResponseStart();

            int httpStatus = mConnection.getResponseCode();
            traceData.setHttpStatus(httpStatus);

            traceData.setResponseHeader(mConnection.getHeaderFields());

            input = getInputStream(mConnection);

            String enc = mConnection.getContentEncoding();
            if (enc != null && enc.equals(HttpUtil.GZIP)) { // 注意这里
                input = new java.util.zip.GZIPInputStream(input);
            }

            HttpResponseObject responseObject;

            if (channel != null) {
                output = channel.createOutputStream();
                long total = getHeaderFieldLong(mConnection, "content-length", -1);
                byte[] buffer = new byte[16 * 1024];
                int current = 0;
                int len;
                while ((len = input.read(buffer)) != -1) {
                    //文件的IO操作无视Thread的interrupt（不产生IOException），所以还是自己来吧
                    if (Thread.interrupted()) {
                        throw new IOException("task cancel");
                    }

                    output.write(buffer, 0, len);
                    current += len;
                    if (progressDeliver != null && len > 0 && total >= current) {
                        progressDeliver.publishProgress(100, (int) (current * 100 / total));
                    }
                }

                output.close();

                traceData.setResponseEnd();
                responseObject = new HttpResponseObject();
                responseObject.channel = channel;
            } else {
                String str = Utility.streamToString(input);
                traceData.setResponse(str);

                traceData.setResponseEnd();

                if (wholeResponse) {
                    responseObject = new HttpResponseObject();
                    responseObject.data = JsonUtil.stringToJson(str);
                    extractCodeAndMessageFromData(httpStatus, responseObject);
                } else {
                    responseObject = JsonUtil.jsonStringToObject(str, HttpResponseObject.class);
                }
                if (responseObject == null) {
                    if (DEBUG) {
                        Log.e(TAG, "httpStatus = " + httpStatus);
                        Log.e(TAG, "url = " + traceData.getUrl());
                        Log.e(TAG, "header = " + traceData.getHeader());
                        Log.e(TAG, "body = " + traceData.getBodyText());
                        Log.e(TAG, "response = " + str);
                    }

                    throw new HttpFormatException("响应数据解析错误");
                }
            }

            if (progressDeliver != null) {
                progressDeliver.publishProgress(100, 100);
            }

//            if (responseObject.data != null) {
//                JsonUtil.removeNullValueProperties(responseObject.data);
//            }
            return responseObject;
        } catch (IOException e) {
            traceData.setException(e);
            throw e;
        } finally {
            Utility.close(input);
            Utility.close(output);
        }
    }

    private static long getHeaderFieldLong(URLConnection connection, String name, long def) {
        String value = connection.getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {

        }
        return def;
    }

    private static InputStream getInputStream(HttpURLConnection urlConnection) {
        try {
            return urlConnection.getInputStream();
        } catch (IOException ioe) {
            return urlConnection.getErrorStream();
        }
    }

    private void extractCodeAndMessageFromData(int httpStatus, HttpResponseObject responseObject) {
        if (responseObject.data != null && responseObject.data.isJsonObject()) {
            JsonObject dataObject = responseObject.data.getAsJsonObject();
            final String[] codeKeyList = {"code", "err_code"};
            for (String codeKey : codeKeyList) {
                try {
                    if (dataObject.has(codeKey)) {
                        int code = dataObject.get(codeKey).getAsInt();
                        responseObject.setCode(code);
                    }
                } catch (Exception ignored) {

                }
            }
            final String[] msgKeyList = {"msg", "err_msg"};
            for (String msgKey : msgKeyList) {
                try {
                    if (dataObject.has(msgKey)) {
                        responseObject.setMsg(dataObject.get(msgKey).getAsString());
                    }
                } catch (Exception ignored) {

                }
            }
        }
    }


    @Override
    public TaskResult<HttpResponseObject> work(
            @Nullable TaskProgressDeliver progressDeliver, HttpRequestObject httpRequestObject) {
        // 趁机执行以前未能成功的任务
        if (mContext != null) {
            synchronized (ResumeHttpTaskControl.class) {
                List<HttpRequestObject> pendingTaskList = ResumeHttpTaskControl.read(mContext);
                HttpRequestObject currentRequest = null;
                try {
                    for (HttpRequestObject pendingRequest : pendingTaskList) {
                        pendingRequest.appendToResumeTaskListIfFailed = true;
                        currentRequest = pendingRequest;
                        execute(progressDeliver, currentRequest, true);
                        if (mCanceled) {
                            return new TaskResult<>(null, false, true, pendingRequest.traceData);
                        }
                    }
                } catch (Exception e) {
                    // 只有httpRequestObject也是appendToResumeTaskListIfFailed时，才中止。
                    // 否则可能造成一个任务意外失败，所有请求都不能成功的问题。
                    if (httpRequestObject.appendToResumeTaskListIfFailed
                            && canRetry(currentRequest.traceData.getHttpStatus())) {
                        return new TaskResult<>(null, false, true, currentRequest.traceData, e);
                    }
                }
            }
        }

        if (httpRequestObject.traceData == null) {
            httpRequestObject.traceData = new HttpTraceObject();
        }

        try {
            HttpResponseObject responseObject = execute(progressDeliver, httpRequestObject, false);
            if (mCanceled) {
                return new TaskResult<>(null, false, true, httpRequestObject.traceData);
            }
            boolean saveCache = httpRequestObject.traceData.getHttpStatus() == HttpURLConnection.HTTP_OK;
            return new TaskResult<>(responseObject, saveCache, false, httpRequestObject.traceData);
        } catch (Exception e) {
            return new TaskResult<>(null, false, true, httpRequestObject.traceData, e);
        }
    }


    @SuppressLint("TrustAllX509TrustManager")
    public static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
