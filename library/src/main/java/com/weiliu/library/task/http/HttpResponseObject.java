package com.weiliu.library.task.http;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.weiliu.library.json.JsonInterface;

/**
 * http 响应结果
 * Created by qumiao on 2016/5/3.
 */
public class HttpResponseObject implements JsonInterface {
    /**响应码*/
    private int err_code;
    @SerializedName("Code")
    private int code;

    /**提示信息*/
    private String err_msg;

    @SerializedName("Msg")
    private String msg;

    /**数据*/
    @SerializedName("Data")
    public JsonElement data;

    /**将响应结果以二进制方式保存在该通道中（直接引用自HttpRequestObject.channel）*/
    public transient HttpBinaryChannel channel;

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code != 0 ? code : err_code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg != null ? msg : err_msg;
    }
}
