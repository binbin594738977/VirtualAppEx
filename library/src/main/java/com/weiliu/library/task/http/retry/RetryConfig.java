package com.weiliu.library.task.http.retry;

import android.os.Parcel;
import android.os.Parcelable;

import com.weiliu.library.json.JsonInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：qumiao
 * 日期：2017/1/3 10:01
 * 说明：App业务接口失败自动重试域名替换方案的配置
 */

public class RetryConfig implements JsonInterface, Parcelable {
    /**0表示关闭，1表示开启*/
    public int retryStatus;
    /**表示主域名超时时间，单位为秒*/
    public int mainHostTimeout;
    /**表示重试域名超时时间，单位为秒*/
    public int backupHostTimeout;
    /**重试间隔时间，单位为秒*/
    public int retryInterval;
    /**主域名 -> 重试域名列表 的映射表*/
    public List<Entry> retryHosts = new ArrayList<>();
    /**重试域名（/路径）的黑名单，格式为 host/path */
    public List<String> specialPath = new ArrayList<>();

    /**
     * 主域名 -> 重试域名列表
     */
    public static class Entry implements JsonInterface, Parcelable {
        /**主域名*/
        public String host;
        /**重试域名列表*/
        public List<String> backup = new ArrayList<>();
        /**替换的scheme列表，可以为空，或者数量不等于backup*/
        public List<String> scheme = new ArrayList<>();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.host);
            dest.writeStringList(this.backup);
            dest.writeStringList(this.scheme);
        }

        public Entry() {
        }

        protected Entry(Parcel in) {
            this.host = in.readString();
            this.backup = in.createStringArrayList();
            this.scheme = in.createStringArrayList();
        }

        public static final Creator<Entry> CREATOR = new Creator<Entry>() {
            @Override
            public Entry createFromParcel(Parcel source) {
                return new Entry(source);
            }

            @Override
            public Entry[] newArray(int size) {
                return new Entry[size];
            }
        };
    }

    public RetryConfig() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.retryStatus);
        dest.writeInt(this.mainHostTimeout);
        dest.writeInt(this.backupHostTimeout);
        dest.writeInt(this.retryInterval);
        dest.writeTypedList(this.retryHosts);
        dest.writeStringList(this.specialPath);
    }

    protected RetryConfig(Parcel in) {
        this.retryStatus = in.readInt();
        this.mainHostTimeout = in.readInt();
        this.backupHostTimeout = in.readInt();
        this.retryInterval = in.readInt();
        this.retryHosts = in.createTypedArrayList(Entry.CREATOR);
        this.specialPath = in.createStringArrayList();
    }

    public static final Creator<RetryConfig> CREATOR = new Creator<RetryConfig>() {
        @Override
        public RetryConfig createFromParcel(Parcel source) {
            return new RetryConfig(source);
        }

        @Override
        public RetryConfig[] newArray(int size) {
            return new RetryConfig[size];
        }
    };
}
