// IStreamProxyService.aidl
package com.weiliu.library.proxy;

// Declare any non-default types here with import statements

interface IStreamProxyService {
    String getAddress();
    void pauseProxy(String url);
    void resumeProxy(String url);
}
