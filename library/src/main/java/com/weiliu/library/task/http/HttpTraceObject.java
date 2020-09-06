package com.weiliu.library.task.http;


import com.weiliu.library.json.JsonInterface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * http请求跟踪信息
 * Created by qumiao on 2016/5/3.
 */
public class HttpTraceObject implements JsonInterface {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);

    private String url;
    /**最终请求的url。只考虑原url失败后换host重试，不考虑3XX跳转的情况。*/
    private String finalUrl;

    private String method;
    private Map<String, String> header;
    private Map<String, String> body;
    private String bodyText;
    private boolean isBodyGZip;
    private MultiParts multiParts;
    private String contentType;

    private String ip;

    private Exception exception;

    private int httpStatus;
    private String response;

    private Map<String, List<String>> responseHeader;

    // 之所以使用z00之类的前缀，主要是为了保证输出的Json字段顺序，方便日志浏览

    private String z00Start;
    private String z01RealStart;
    private String z02End;

    /**整型start*/
    private long z03StartLong;
    /**整型end*/
    private long z04EndLong;

    private String z10DnsStart;
    private String z11DnsEnd;

    private String z20TcpStart;
    private String z21TcpEnd;

    private String z30PostStart;
    private String z31PostEnd;

    private String z40ResponseStart;
    private String z41ResponseEnd;

    private String z50InputStart;
    private String z51InputEnd;

    public HttpTraceObject() {
        super();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String newUrl) {
        url = newUrl;
    }

    /**最终请求的url。只考虑原url失败后换host重试，不考虑3XX跳转的情况。*/
    public String getFinalUrl() {
        return finalUrl;
    }

    /**最终请求的url。只考虑原url失败后换host重试，不考虑3XX跳转的情况。*/
    public void setFinalUrl(String newUrl) {
        finalUrl = newUrl;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public Map<String, String> getBody() {
        return body;
    }

    public void setBody(Map<String, String> newParams) {
        body = newParams;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public boolean isBodyGZip() {
        return isBodyGZip;
    }

    public void setBodyGZip(boolean bodyGZip) {
        isBodyGZip = bodyGZip;
    }

    public MultiParts getMultiParts() {
        return multiParts;
    }

    public void setMultiParts(MultiParts multiParts) {
        this.multiParts = multiParts;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception e) {
        exception = e;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int status) {
        httpStatus = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Map<String, List<String>> getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(Map<String, List<String>> responseHeader) {
        this.responseHeader = responseHeader;
    }

    private String current() {
        return FORMAT.format(new Date());
    }

    public String getStart() {
        return z00Start;
    }

    public long getStartLong() {
        return z03StartLong;
    }

    public void setStart() {
        z00Start = current();
        z03StartLong = System.currentTimeMillis();
    }

    public String getRealStart() {
        return z01RealStart;
    }

    public void setRealStart() {
        z01RealStart = current();
    }

    public String getEnd() {
        return z02End;
    }

    public long getEndLong() {
        return z04EndLong;
    }

    public void setEnd() {
        z02End = current();
        z04EndLong = System.currentTimeMillis();
    }

    public String getDnsStart() {
        return z10DnsStart;
    }

    public void setDnsStart() {
        z10DnsStart = current();
    }

    public String getDnsEnd() {
        return z11DnsEnd;
    }

    public void setDnsEnd() {
        z11DnsEnd = current();
    }

    public String getTcpStart() {
        return z20TcpStart;
    }

    public void setTcpStart() {
        z20TcpStart = current();
    }

    public String getTcpEnd() {
        return z21TcpEnd;
    }

    public void setTcpEnd() {
        z21TcpEnd = current();
    }

    public String getPostStart() {
        return z30PostStart;
    }

    public void setPostStart() {
        z30PostStart = current();
    }

    public String getPostEnd() {
        return z31PostEnd;
    }

    public void setPostEnd() {
        z31PostEnd = current();
    }

    public String getResponseStart() {
        return z40ResponseStart;
    }

    public void setResponseStart() {
        z40ResponseStart = current();
    }

    public String getResponseEnd() {
        return z41ResponseEnd;
    }

    public void setResponseEnd() {
        z41ResponseEnd = current();
    }

    public String getInputStart() {
        return z50InputStart;
    }

    public void setInputStart() {
        z50InputStart = current();
    }

    public String getInputEnd() {
        return z51InputEnd;
    }

    public void setInputEnd() {
        z51InputEnd = current();
    }
}

