package library;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.text.TextUtils;

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

public class WxUtil {
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

    /**
     * 上报
     */
    public static void com_tencent_mm_plugin_report_service_h_mEJ_h(ClassLoader classLoader, int i, Object... objArr) throws Exception {
        Object mEJ = ClassUtil.getStaticFieldValue(classLoader.loadClass("com.tencent.mm.plugin.report.service.h"), "mEJ");
        ClassUtil.invokeMethod(mEJ, "h"
                , new Class[]{int.class, Object[].class}
                , new Object[]{i, objArr});
    }

    /**
     * 上报
     */
    public static void com_tencent_mm_plugin_report_service_h_mEJ_k(ClassLoader classLoader, int i, String str) throws Exception {
        Object mEJ = ClassUtil.getStaticFieldValue(classLoader.loadClass("com.tencent.mm.plugin.report.service.h"), "mEJ");
        ClassUtil.invokeMethod(mEJ, "k"
                , new Class[]{int.class, String.class}
                , new Object[]{i, str});
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