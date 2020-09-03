package library;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.text.TextUtils;

import com.lody.virtual.client.core.VirtualCore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyUtil {






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

    public static String InputStringToString(InputStream inputStream) {
        String str = null;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            str = result.toString("UTF-8");
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
        return str;
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
                return netRequestFileSync(strings[0]);
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


    public static List<String> netRequestFileSync(List<String> imagePaths) {
        List<String> filePaths = new ArrayList<>();
        int retryCount = 1;//重试次数
        while (true) {
            try {
                for (String imagePath : imagePaths) {
                    //如果本地存在就不要再下载了
                    File file = new File(Utility.getDefaultFileDirectory(), Md5Util.MD5Encode(imagePath));
                    if (file.exists()) {
                        filePaths.add(file.getAbsolutePath());
                        WeiliuLog.log("filePath: " + file.getAbsolutePath() + "  已存在");
                        continue;
                    }
                    URL url = new URL(imagePath);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    //简单的设置
                    httpURLConnection.setReadTimeout(10000);
                    httpURLConnection.setConnectTimeout(10000);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    //这里直接就用bitmap取出了这个流里面的图片，
                    File tmp = new File(file.getAbsolutePath() + ".tmp");
                    boolean b = Utility.streamToFile(inputStream, tmp);
                    if (b) {
                        FileUtil.renameTo(tmp, file);
                        filePaths.add(file.getAbsolutePath());
                    }
                }
                return filePaths;
            } catch (InterruptedIOException e1) {
                return filePaths;
            } catch (Throwable e) {
                filePaths.clear();
                if (retryCount > 0) {//如果下载失败有剩余的重试次数,重新下载
                    retryCount--;
                    continue;
                }
                WeiliuLog.log(e);
                break;
            }
        }
        return filePaths;
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
    public static void netRequestFileFailforsContiue(final List<String> imagePaths, final RequestImageCallback callback) {
        try {
            final List<String> list = new ArrayList<>();
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
     * @throws IOException
     */
    public static void netRequestString(String url, final RequestStringCallback callback) throws IOException {
        AsyncTask<String, Integer, String> objectObjectObjectAsyncTask = new AsyncTask<String, Integer, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    URL url = new URL(strings[0]);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    //简单的设置
                    httpURLConnection.setReadTimeout(10000);
                    httpURLConnection.setConnectTimeout(10000);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    String json = InputStringToString(inputStream);
                    return json;
                } catch (Throwable e) {
                    WeiliuLog.log(e);
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




    public interface RequestImageCallback {
        void success(List<String> filePaths);
    }

    public interface RequestStringCallback {
        void success(String str);
    }
}