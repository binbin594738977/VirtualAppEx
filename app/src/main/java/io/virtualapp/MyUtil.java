package io.virtualapp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.gson.internal.Primitives;
import com.lody.virtual.client.core.VirtualCore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import library.FileUtil;
import library.Md5Util;
import library.PhoneInfoUtil;
import library.Utility;
import library.WeiliuLog;

public class MyUtil {

    public static String bi_WT(String str) {
        if (str == null || str.length() == 0 || VERSION.SDK_INT >= 8) {
            return str;
        }
        int length = str.length();
        char[] cArr = new char[length];
        int i = 0;
        int i2 = 0;
        int i3 = -1;
        while (i < length) {
            int i4;
            char charAt = str.charAt(i);
            int i5 = i2 + 1;
            cArr[i2] = charAt;
            if (charAt == '&' && i3 == -1) {
                i2 = i5;
                i4 = i5;
            } else if (i3 == -1 || Character.isLetter(charAt) || Character.isDigit(charAt) || charAt == '#') {
                i2 = i5;
                i4 = i3;
            } else if (charAt == ';') {
                i2 = a(cArr, i3, (i5 - i3) - 1);
                if (i2 > 65535) {
                    i5 = i2 - 65536;
                    cArr[i3 - 1] = (char) ((i5 >> 10) + 55296);
                    cArr[i3] = (char) ((i5 & 1023) + 56320);
                    i3++;
                } else if (i2 != 0) {
                    cArr[i3 - 1] = (char) i2;
                } else {
                    i3 = i5;
                }
                i2 = i3;
                i4 = -1;
            } else {
                i2 = i5;
                i4 = -1;
            }
            i++;
            i3 = i4;
        }
        return new String(cArr, 0, i2);
    }

    private static int a(char[] cArr, int i, int i2) {
        int i3 = 0;
        if (i2 > 0) {
            if (cArr[i] != '#') {
                String str = new String(cArr, i, i2);
            } else if (i2 <= 1 || !(cArr[i + 1] == 'x' || cArr[i + 1] == 'X')) {
                try {
                    i3 = Integer.parseInt(new String(cArr, i + 1, i2 - 1), 10);
                } catch (NumberFormatException e) {
                }
            } else {
                try {
                    i3 = Integer.parseInt(new String(cArr, i + 2, i2 - 2), 16);
                } catch (NumberFormatException e2) {
                }
            }
        }
        return i3;
    }

    public static int bd_iA(String str) {
        if (str == null) {
            return -1;
        } else if (str.length() <= 0) {
            return -1;
        } else if (str.startsWith("~SEMI_XML~")) {
            return -1;
        } else {
            int indexOf = str.indexOf(58);
            if (indexOf == -1 || !str.substring(0, indexOf).contains("<")) {
                return indexOf;
            }
            return -1;
        }
    }

    public static boolean s_fq(String str) {
        if (str == null || str.length() <= 0) {
            return false;
        }
        return str.endsWith("@chatroom");
    }

    private static File bR(Context context, String str) {
        File dir = context.getDir(str, 0);
        File file = new File(dir.getParentFile(), str);
        dir.renameTo(file);
        return file;
    }

    public static File hV(Context context) {
        return bR(context, "tinker");
    }

    public static File acT(String str) {
        return new File(str + "/patch.info");
    }

    public static String bi_c(List<String> list, String str) {
        if (list == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                stringBuilder.append(((String) list.get(i)).trim());
            } else {
                stringBuilder.append(((String) list.get(i)).trim() + str);
            }
        }
        return stringBuilder.toString();
    }


    public static String JsonArrToString(JSONArray list, String str) throws Exception {
        if (list == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < list.length(); i++) {
            if (i == list.length() - 1) {
                stringBuilder.append(((String) list.getString(i)).trim());
            } else {
                stringBuilder.append(((String) list.get(i)).trim() + str);
            }
        }
        return stringBuilder.toString();
    }

    public static ArrayList<String> F(String[] strArr) {
        if (strArr == null || strArr.length == 0) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList();
        for (String add : strArr) {
            arrayList.add(add);
        }
        return arrayList;
    }

    public static ArrayList<String> arrToArrayList(String[] strArr) {
        if (strArr == null || strArr.length == 0) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList();
        for (String add : strArr) {
            arrayList.add(add);
        }
        return arrayList;
    }


    public static boolean n(Uri uri) {
        if (uri == null) {
            return false;
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Xg(uri.getPath());
        }
        return true;
    }


    public static boolean Xg(String str) {
        if (oW(str)) {
            return false;
        }
        try {
            String canonicalPath = new File(str).getCanonicalPath();
            if (canonicalPath.contains("/com.tencent.mm/cache/")) {
                return true;
            }
            if (canonicalPath.contains("/com.tencent.mm/")) {
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static boolean oW(String str) {
        if (str == null || str.length() <= 0) {
            return true;
        }
        return false;
    }

    public static String getJSONString(JSONObject object, String str) throws Exception {
        if (object.has(str)) {
            return object.getString(str);
        } else {
            return "";
        }
    }

    public static int getJSONInt(JSONObject object, String str) throws Exception {
        if (object.has(str)) {
            return object.getInt(str);
        } else {
            return -1;
        }
    }

    public static long getJSONLong(JSONObject object, String str) throws Exception {
        if (object.has(str)) {
            return object.getLong(str);
        } else {
            return -1;
        }
    }

    public static JSONArray getJSONArray(JSONObject object, String str) throws Exception {
        if (object.has(str)) {
            return object.getJSONArray(str);
        } else {
            return new JSONArray();
        }
    }

    public static JSONObject getJSONObject(JSONObject object, String str) throws Exception {
        if (object.has(str)) {
            return object.getJSONObject(str);
        } else {
            return new JSONObject();
        }
    }

    public static List jsonArrayToList(JSONArray jsonArray) {
        List filePath = new ArrayList();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object obj = jsonArray.getString(i);
                filePath.add(obj);
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
        return filePath;
    }

    /**
     * and 指定的username的条件
     *
     * @param list
     * @return
     */
    public static String sqlAndUserName(List<String> list) {
        if (list == null || list.size() == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                stringBuilder.append(" (username = '").append(list.get(i)).append("'");
            } else {
                stringBuilder.append(" or username = '").append(list.get(i)).append("'");
            }
            if (list.size() - 1 == i) {
                stringBuilder.append(" )");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 打印File
     */
    public static void printFile(String content, String fileName, boolean append) {
        FileWriter fw = null;
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(content)) return;
        try {
            File f = new File(Utility.getDefaultFileDirectory() + "/" + fileName);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (!f.exists()) {
                f.createNewFile();
            }
            fw = new FileWriter(f, append);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(content, 0, content.length());
            out.close();
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    /**
     * 打印File
     */
    public static void printFile(String content, File file, boolean append) {
        FileWriter fw = null;
        if (file == null) return;
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            fw = new FileWriter(file, append);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(content, 0, content.length());
            out.close();
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    public static String InputStringToString(InputStream inputStream) {
        String str = null;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            str = result.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
        return str;
    }


    public static boolean isListEmpty(List list) {
        if (list == null || list.size() == 0) {
            return true;
        }
        return false;
    }





    /**
     * 同步下载
     * 简单的请求图片(如果失败,则全部失败,返回空集合)
     *
     * @param imagePaths
     * @return
     * @throws IOException
     */
    public static void netRequestFile(List<String> imagePaths, final RequestImageCallback callback, boolean isOnExecutor) throws Exception {
        AsyncTask<List<String>, Integer, List<String>> objectObjectObjectAsyncTask = new AsyncTask<List<String>, Integer, List<String>>() {
            @Override
            protected List<String> doInBackground(List<String>... strings) {
                List<String> filePaths = new ArrayList<>();
                int retryCount = 2;//重试次数
                int responseCode = -1;
                while (true) {
                    try {
                        List<String> imagePaths = strings[0];
                        for (String imagePath : imagePaths) {
                            //如果本地存在就不要再下载了
                            File file = new File(Utility.getDefaultDownloadFileDirectory(true), Md5Util.MD5Encode(imagePath));
                            if (file.exists()) {
                                filePaths.add(file.getAbsolutePath());
                                WeiliuLog.log("filePath: " + file.getAbsolutePath() + "  已存在");
                                continue;
                            }
                            URL url = new URL(imagePath);
                            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                            //简单的设置
                            httpURLConnection.setReadTimeout(15000);
                            httpURLConnection.setConnectTimeout(15000);
                            responseCode = httpURLConnection.getResponseCode();
                            WeiliuLog.log("responseCode: " + responseCode);
                            InputStream inputStream = httpURLConnection.getInputStream();
                            //这里直接就用bitmap取出了这个流里面的图片，
                            File tmp = new File(file.getAbsolutePath() + ".tmp");
                            streamToFile(inputStream, tmp);
                            FileUtil.renameTo(tmp, file);
                            filePaths.add(file.getAbsolutePath());
                        }
                        return filePaths;

                    } catch (SocketTimeoutException e) {
                        filePaths.clear();
                        if (retryCount > 0) {//如果下载失败有剩余的重试次数,重新下载
                            retryCount--;
                            WeiliuLog.log(3, "重试下载剩余" + retryCount + "次");
                            continue;
                        }
                        WeiliuLog.log(e);
                        break;
                    } catch (InterruptedIOException e1) {
                        WeiliuLog.log(e1);
                        return filePaths;
                    } catch (Throwable e) {
                        filePaths.clear();
                        if (retryCount > 0) {//如果下载失败有剩余的重试次数,重新下载
                            retryCount--;
                            WeiliuLog.log(3, "重试下载剩余" + retryCount + "次");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e1) {
                                WeiliuLog.log(e1);
                                break;
                            }
                            continue;
                        }
                        WeiliuLog.log(e);
                        break;
                    }
                }
                return filePaths;
            }

            @Override
            protected void onPostExecute(List<String> filePaths) {
                if (callback != null) {
                    callback.success(filePaths);
                }
                super.onPostExecute(filePaths);
            }
        };
        if (isOnExecutor) {
            objectObjectObjectAsyncTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, imagePaths);
        } else {
            objectObjectObjectAsyncTask.execute(imagePaths);
        }
    }


    /**
     * 异步下载
     *
     * @param imagePaths
     * @param callback
     * @throws Exception
     */
    public static void netRequestFile(List<String> imagePaths, final RequestImageCallback callback) throws Exception {
        netRequestFile(imagePaths, callback, false);
    }

    /**
     * 如果有失败,强制继续下载
     *
     * @param imagePaths
     * @param callback
     */
    public static void netRequestFileFailforsContiue(List<String> imagePaths, final RequestImageCallback callback) {
        try {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < imagePaths.size(); i++) {
                final int finalI = i;
                netRequestFile(Arrays.asList(imagePaths.get(i)), new RequestImageCallback() {
                    @Override
                    public void success(List<String> filePaths) {
                        list.addAll(filePaths);
                        if (finalI == imagePaths.size() - 1) {
                            callback.success(list);
                        }
                    }
                }, true);
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }


    /**
     * 简单的请求String
     *
     * @return
     */
    public static void netRequestString(String url, final RequestStringCallback callback) {
        netRequestString(url, null, null, callback);
    }

    public static void netRequestString(String url, Map<String, String> postParam, final RequestStringCallback callback) {
        byte[] body = null;
        if (postParam != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : postParam.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(Uri.encode(entry.getKey())).append("=").append(Uri.encode(entry.getValue()));
            }
            body = sb.toString().getBytes();
        }
        netRequestString(url, body, null, callback);
    }

    /**
     * 简单的请求String
     *
     * @return
     */
    public static void netRequestString(String url, byte[] body, String contentType, final RequestStringCallback callback) {
        AsyncTask<String, Integer, String> objectObjectObjectAsyncTask = new AsyncTask<String, Integer, String>() {
            @Override
            protected String doInBackground(String... strings) {
                OutputStream output = null;
                HttpURLConnection httpURLConnection = null;
                try {
                    URL url = new URL(strings[0]);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    //简单的设置
                    httpURLConnection.setReadTimeout(15000);
                    httpURLConnection.setConnectTimeout(15000);
                    if (body != null) {
                        httpURLConnection.setRequestMethod("POST");
                        httpURLConnection.setDoOutput(true);
                        String type = contentType;
                        if (type == null) {
                            type = "application/x-www-form-urlencoded";
                        }
                        httpURLConnection.setRequestProperty("Content-Type", type);
                        httpURLConnection.setFixedLengthStreamingMode(body.length);
                    }
                    httpURLConnection.connect();
                    if (body != null) {
                        output = httpURLConnection.getOutputStream();
                        output.write(body);
                        output.close();
                    }
                    InputStream inputStream = httpURLConnection.getInputStream();
                    String json = InputStringToString(inputStream);
                    return json;
                } catch (Throwable e) {
                    WeiliuLog.log(e);
                } finally {
                    Utility.close(output);
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
                return "";
            }

            @Override
            protected void onPostExecute(String str) {
                if (callback != null) {
                    callback.success(str);
                }
                super.onPostExecute(str);
            }
        };
        objectObjectObjectAsyncTask.execute(url);
    }


    /**
     * stream to file
     *
     * @param in   Inputstream
     * @param file File
     * @return success or not
     */
    public static boolean streamToFile(InputStream in, File file) throws IOException {
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file));
            final byte[] buffer = new byte[Utility.FILE_STREAM_BUFFER_SIZE];
            int n;
            while (-1 != (n = in.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return true;
        } catch (IOException e) {
            throw e;
        } finally {
            Utility.close(output);
            Utility.close(in);
        }
    }


    /**
     * x-y之间的随机数
     */
    public static long x_y_random(int x, int y) {
        return Math.round(Math.random() * (y - x) + x);
    }



    /**
     * put全局本地配置(需要指定配置文件)
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public static boolean putGlobalNativeConfigs(File file, String key, Object value) {
        try {
            String configs = loadFileString(file);
            JSONObject jsonObject;
            if (TextUtils.isEmpty(configs)) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = new JSONObject(configs);
            }
            jsonObject.put(key, value);
            printFile(jsonObject.toString(), file, false);
            return true;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return false;
        }
    }

    /**
     * put全局本地配置(默认配置文件)
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public static boolean putGlobalNativeConfigs(String key, Object value) {
        String fileName = "configs.json";
        File file = new File(Utility.getDefaultFileDirectory(), fileName);
        return putGlobalNativeConfigs(file, key, value);
    }


    /**
     * 得到全局的本地配置(需要指定配置文件)
     *
     * @param key   键
     * @param clazz 值的类型
     * @param <T>   值的类型
     * @return
     */
    public static <T> T getGlobalNativeConfigs(File file, String key, Class<T> clazz) {
        try {
            String configs = loadFileString(file);
            JSONObject jsonObject = new JSONObject(configs);
            Object obj = jsonObject.get(key);
            return Primitives.wrap(clazz).cast(obj);
        } catch (Exception e) {
            if (isPrimitive(clazz)) {
                if (TextUtils.equals(Boolean.class.getName(), clazz.getName())) {
                    return (T) Boolean.valueOf(false);
                }
                if (TextUtils.equals(boolean.class.getName(), clazz.getName())) {
                    return (T) Boolean.valueOf(false);
                }
                if (TextUtils.equals(char.class.getName(), clazz.getName())) {
                    return null;
                }
                if (TextUtils.equals(Character.class.getName(), clazz.getName())) {
                    return null;
                }
                if (TextUtils.equals(String.class.getName(), clazz.getName())) {
                    return null;
                }
                return (T) Integer.valueOf(0);
            }
        }
        return null;
    }

    /**
     * 得到全局的本地配置(默认配置文件)
     *
     * @param key   键
     * @param clazz 值的类型
     * @param <T>   值的类型
     * @return
     */
    public static <T> T getGlobalNativeConfigs(String key, Class<T> clazz) {
        String fileName = "configs.json";
        File file = new File(Utility.getDefaultFileDirectory(), fileName);
        return getGlobalNativeConfigs(file, key, clazz);
    }

    /**
     * 判断一个对象是否是基本类型或基本类型的封装类型
     */
    private static boolean isPrimitive(Class cla) {
        try {
            boolean isPrimitive;
            isPrimitive = cla.isPrimitive();
            if (!isPrimitive) {
                isPrimitive = ((Class<?>) cla.getField("TYPE").get(null)).isPrimitive();
            }
            return isPrimitive;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取文件的字符串，简单的使用，目录定死了
     */
    public static String loadFileString(String fileName) {
        File file = new File(Utility.getDefaultFileDirectory() + "/" + fileName);
        return loadFileString(file);
    }

    /**
     * 读取文件的字符串
     */
    public static String loadFileString(File file) {
        if (file == null || !file.exists()) return "";
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            in = new FileInputStream(file);//文件名
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }


    public interface RequestImageCallback {
        void success(List<String> filePaths);
    }

    public interface RequestStringCallback {
        void success(String str);
    }
}
