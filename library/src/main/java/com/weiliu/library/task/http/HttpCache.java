package com.weiliu.library.task.http;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.weiliu.library.json.JsonUtil;
import com.weiliu.library.task.TaskCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * http任务的缓存管理
 * Created by qumiao on 2016/5/18.
 */
public class HttpCache implements TaskCache<HttpRequestObject, HttpResponseObject> {

    private static final String HTTP_CACHE_DIR = "http_cache";

    private Context mContext;

    public HttpCache(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @WorkerThread
    @Override
    public boolean hit(HttpRequestObject httpRequestObject) {
        File cacheFile = getCacheFile(mContext, httpRequestObject);
        File md5File = new File(cacheFile + ".md5");
        try {
            String cacheFileMd5 = Utility.fileMD5(cacheFile.getAbsolutePath());
            String md5 = Utility.fileToString(md5File);
            if (TextUtils.equals(cacheFileMd5, md5)) {
                return true;
            }
        } catch (IOException ignored) {
//            e.printStackTrace();
        }
        return false;
    }

    @WorkerThread
    @Override
    public void save(HttpRequestObject httpRequestObject, HttpResponseObject resultValue) {
        File cacheFile = getCacheFile(mContext, httpRequestObject);
        if (resultValue.channel != null) {
            if (!(resultValue.channel instanceof HttpFileChannel) ||
                    !cacheFile.equals(((HttpFileChannel) resultValue.channel).getFile())) {
                InputStream input = null;
                OutputStream output = null;
                byte[] buffer = new byte[16 * 1024];
                int len;
                try {
                    input = resultValue.channel.createInputStream();
                    output = new FileOutputStream(cacheFile);
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Utility.close(input);
                    Utility.close(output);
                }
            }
        } else {
            Utility.stringToFile(JsonUtil.objectToJsonString(resultValue, HttpResponseObject.class), cacheFile);
        }

        File md5File = new File(cacheFile + ".md5");
        try {
            String md5 = Utility.fileMD5(cacheFile.getAbsolutePath());
            Utility.stringToFile(md5, md5File);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    @Override
    public HttpResponseObject read(HttpRequestObject httpRequestObject) {
        File cacheFile = getCacheFile(mContext, httpRequestObject);
        if (httpRequestObject.channel != null) {
            HttpResponseObject responseObject = new HttpResponseObject();
            InputStream input = null;
            OutputStream output = null;
            byte[] buffer = new byte[16 * 1024];
            int len;
            try {
                input = new FileInputStream(cacheFile);
                output = httpRequestObject.channel.createOutputStream();
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
                responseObject.channel = httpRequestObject.channel;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Utility.close(input);
                Utility.close(output);
            }
            return responseObject;
        } else {
            String str = Utility.fileToString(cacheFile);
            return JsonUtil.jsonStringToObject(str, HttpResponseObject.class);
        }
    }


    public static File getCacheFile(Context context, HttpRequestObject httpRequestObject) {
        File dir;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            dir = new File(context.getCacheDir(), HTTP_CACHE_DIR);
        } else {
            dir = new File(context.getExternalCacheDir(), HTTP_CACHE_DIR);
        }
        mkdirs(dir);
        return new File(dir, Utility.MD5Encode(getCacheFileKey(httpRequestObject)));
    }

    private static String getCacheFileKey(HttpRequestObject httpRequestObject) {
        StringBuilder sb = new StringBuilder(httpRequestObject.url);
        if (httpRequestObject.params != null) {
            for (HttpParams.Type type : HttpParams.Type.values()) {
                Map<String, String> map = httpRequestObject.params.getParams(type);
                if (map.isEmpty()) {
                    continue;
                }
                sb.append("||");
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (httpRequestObject.params.isParamServiceForCacheKey(entry.getKey())) {
                        sb.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                }
            }
        }
        return sb.toString();
    }

    private static boolean mkdirs(File file) {
        return file != null && !(!file.mkdirs() && !file.isDirectory());
    }
}
