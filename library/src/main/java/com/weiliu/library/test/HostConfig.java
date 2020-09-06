package com.weiliu.library.test;

import android.support.annotation.NonNull;

import com.weiliu.library.json.JsonInterface;


public class HostConfig implements JsonInterface {

    public String host;
    public String ip;
    public String port;
    public boolean enable = true;
    
    @NonNull
    public String getIpHost() {
    	return ip + (port == null ? "" : ":" + port);
    }
}
