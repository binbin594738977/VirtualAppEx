package com.weiliu.library.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import com.weiliu.library.R;
import com.weiliu.library.RootApplication;
import com.weiliu.library.WeiliuLog;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.entity.EntityDeserializer;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.HttpRequestParser;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicLineParser;
import org.apache.http.params.BasicHttpParams;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipFile;

/**
 * 常用方法集合
 * Created by qumiao on 14-1-15.
 */
public class Utility {

    /**
     * File buffer stream size.
     */
    public static final int FILE_STREAM_BUFFER_SIZE = 16 * 1024;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;

    public static final String UTF8 = "UTF-8";

    private Utility() {

    }

    /**
     * 比较两个对象是否相等（通过equals），并且避免NullPointerException
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equalsSafely(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * String to OutputStream
     *
     * @param str    String
     * @param stream OutputStream
     * @return success or not
     */
    public static boolean stringToStream(@Nullable String str, @NonNull OutputStream stream) {
        if (str == null) {
            return false;
        }

        byte[] data;
        try {
            data = str.getBytes("UTF-8");
            stream.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(stream);
        }

        return false;
    }

    /**
     * String to file
     *
     * @param str  String
     * @param file File
     * @return success or not
     */
    public static boolean stringToFile(@Nullable String str, @NonNull File file) {
        return stringToFile(str, file, false);
    }

    /**
     * String to file
     *
     * @param str    String
     * @param file   File
     * @param append is append mode or not
     * @return success or not
     */
    public static boolean stringToFile(@Nullable String str, @NonNull File file, boolean append) {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file, append);
            return stringToStream(str, stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            close(stream);
        }
    }

    /**
     * stream to file
     *
     * @param in   Inputstream
     * @param file File
     * @return success or not
     */
    public static boolean streamToFile(@NonNull InputStream in, @NonNull File file) {
        return streamToFile(in, file, false);
    }

    /**
     * stream to file
     *
     * @param in     Inputstream
     * @param file   File
     * @param append is append mode or not
     * @return success or not
     */
    public static boolean streamToFile(@NonNull InputStream in, @NonNull File file, boolean append) {
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file, append));
            final byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int n;
            while (-1 != (n = in.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utility.close(output);
            Utility.close(in);
        }
        return false;
    }

    /**
     * stream to bytes
     *
     * @param is inputstream
     * @return bytes
     */
    public static byte[] streamToBytes(@NonNull InputStream is) {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int n;
            while (-1 != (n = is.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(is);
        }
        return null;
    }

    /**
     * 转换Stream成string
     *
     * @param is Stream源
     * @return 目标String
     */
    @NonNull
    public static String streamToString(@NonNull InputStream is) {
        return streamToString(is, Xml.Encoding.UTF_8.toString());
    }

    /**
     * 按照特定的编码格式转换Stream成string
     *
     * @param is  Stream源
     * @param enc 编码格式
     * @return 目标String
     */
    @NonNull
    public static String streamToString(@NonNull InputStream is, String enc) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] data = new byte[FILE_STREAM_BUFFER_SIZE];
        try {
            int count;
            while ((count = is.read(data)) > 0) {
                os.write(data, 0, count);
            }
            return new String(os.toByteArray(), Xml.Encoding.UTF_8.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            close(os);
            close(is);
        }
        return "";
    }

    /**
     * 转换文件内容成string
     *
     * @param file File
     * @return 目标String
     */
    @NonNull
    public static String fileToString(@NonNull File file) {
        try {
            return streamToString(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 按照特定的编码格式转换文件内容成string
     *
     * @param file File
     * @param enc  编码格式
     * @return 目标String
     */
    @NonNull
    public static String fileToString(@NonNull File file, String enc) {
        try {
            return streamToString(new FileInputStream(file), enc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取字串长度（为null则返回0）
     *
     * @param str
     * @param trim 是否忽略前后的空白字符
     * @return
     */
    public static int getStringLength(@Nullable CharSequence str, boolean trim) {
        return TextUtils.isEmpty(str) ? 0 : (trim ? str.toString().trim() : str).length();
    }

    public static int hashCodeSafely(@Nullable Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    /**
     * 获取不为空的字串
     *
     * @param sequence
     * @return
     */
    @NonNull
    public static String getNotNullString(@Nullable CharSequence sequence) {
        return sequence != null ? sequence.toString() : "";
    }

    /**
     * 格式化html字串
     *
     * @param stringRes 为0时返回空字串
     * @return
     */
    public static Spanned fromHtml(@StringRes int stringRes, Object... args) {
        //noinspection deprecation
        return stringRes != 0 ? Html.fromHtml(RootApplication.getInstance().getString(stringRes, args)) : new SpannableStringBuilder("");
    }

    /**
     * 格式化html字串
     *
     * @param stringRes 为0时返回空字串
     * @return
     */
    public static Spanned fromHtml(@StringRes int stringRes) {
        //noinspection deprecation
        return stringRes != 0 ? Html.fromHtml(RootApplication.getInstance().getString(stringRes)) : new SpannableStringBuilder("");
    }

    /**
     * 格式化html字串
     *
     * @param str 为null时返回空字串
     * @return
     */
    public static Spanned fromHtml(@Nullable String str) {
        //noinspection deprecation
        return str != null ? Html.fromHtml(str) : new SpannableStringBuilder("");
    }

    /**
     * 关闭，并捕获IOException
     *
     * @param closeable Closeable
     */
    public static void close(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭ZipFile。之所以提供这个方法是因为在某些操蛋的手机上（例如小米，预祝该公司早日倒闭），ZipFile居然木有实现Closeable
     *
     * @param zipFile ZipFile
     */
    public static void close(@Nullable ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(@Nullable RandomAccessFile file) {
        if (file != null) {
            try {
                file.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(@Nullable ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(@Nullable Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * drawable转Bitmap
     *
     * @param drawable 任意的drawable
     * @return Bitmap
     */
    public static Bitmap convertDrawableToBitmapByCanvas(@NonNull Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap
                .createBitmap(width, height,
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        // canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Hides the input method.
     *
     * @param context context
     * @param view    The currently focused view
     * @return success or not.
     */
    public static boolean hideInputMethod(@Nullable Context context, @Nullable View view) {
        if (context == null || view == null) {
            return false;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        return imm != null && imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    /**
     * Show the input method.
     *
     * @param context context
     * @param view    The currently focused view, which would like to receive soft keyboard input
     * @return success or not.
     */
    public static boolean showInputMethod(@Nullable Context context, @Nullable View view) {
        if (context == null || view == null) {
            return false;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        return imm != null && imm.showSoftInput(view, 0);

    }

    /**
     * 判断点击屏幕时，是否应该关闭输入法。
     *
     * @param activity Activity界面
     * @param event    点击事件
     * @return 如果点击事件在EditText上，则不应该关闭，返回false；否则判断为可关闭，返回true
     */
    public static boolean canHideInputMethod(@NonNull Activity activity, @NonNull MotionEvent event) {
        View view = activity.getCurrentFocus();
        return !(view != null && view.hasWindowFocus() && (view instanceof EditText)) || !isTouchEventHitViewArea(view, event);
    }

    /**
     * 判断触摸事件是否在指定View的区域内
     *
     * @param view  View
     * @param event 触摸事件
     * @return 如果触摸事件在指定View的区域内，返回true；否则返回false
     */
    public static boolean isTouchEventHitViewArea(@NonNull View view, @NonNull MotionEvent event) {
        int[] leftTop = new int[2];
        view.getLocationOnScreen(leftTop);
        int left = leftTop[0];
        int top = leftTop[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        RectF rect = new RectF(left, top, right, bottom);
        return rect.contains(event.getRawX(), event.getRawY());
    }

    /**
     * 通过反射修改（包括非public的）属性。在寻找该属性时，会一直递归到（但不包括）Object class。
     *
     * @param obj       调用该对象所在类的属性
     * @param fieldName 属性名
     * @param newValue  修改成该值
     * @return 如果成功找到并修改该属性，返回true，否则返回false
     */
    public static boolean modifyField(@Nullable Object obj, String fieldName, Object newValue) {
        if (obj == null) {
            return false;
        }

        boolean retval = false;

        Field field = getField(obj, fieldName);
        if (field != null) {
            field.setAccessible(true);
            try {
                field.set(obj, newValue);
                retval = true;
            } catch (IllegalAccessException iae) {
                throw new IllegalArgumentException(fieldName, iae);
            } catch (ExceptionInInitializerError eiie) {
                throw new IllegalArgumentException(fieldName, eiie);
            }
        }

        return retval;
    }

    /**
     * 递归查找声明的属性，一直递归到（但不包括）Object class。
     *
     * @param object    指定对象
     * @param fieldName 属性名
     * @return 属性
     */
    public static Field getField(@NonNull Object object, String fieldName) {
        Field field;

        Class<?> theClass = object.getClass();
        for (; theClass != Object.class; theClass = theClass.getSuperclass()) {
            try {
                field = theClass.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {  //SUPPRESS CHECKSTYLE

            } catch (SecurityException e) {
                throw new IllegalArgumentException(theClass.getName() + "." + theClass, e);
            }
        }

        return null;
    }

    /**
     * 调用一个对象的隐藏方法。
     *
     * @param obj        调用方法的对象.
     * @param methodName 方法名。
     * @param types      方法的参数类型。
     * @param args       方法的参数。
     * @return 如果调用成功，则返回true。
     */
    public static boolean invokeHideMethod(@NonNull Object obj, String methodName, Class<?>[] types, Object[] args) {
        boolean hasInvoked = false;
        try {
            Method method = obj.getClass().getMethod(methodName, types);
            method.invoke(obj, args);
            hasInvoked = true;
        } catch (Exception ignore) {    // SUPPRESS CHECKSTYLE
        }
        return hasInvoked;
    }

    /**
     * 调用一个对象的隐藏方法。
     *
     * @param obj        调用方法的对象.
     * @param methodName 方法名。
     * @param types      方法的参数类型。
     * @param args       方法的参数。
     * @param result     如果该方法有返回值，则将返回值放入result[0]中；否则将result[0]置为null
     * @return 隐藏方法调用的返回值。
     */
    public static boolean invokeHideMethod(@NonNull Object obj,
                                           String methodName, Class<?>[] types, Object[] args, @Nullable Object[] result) {
        boolean retval = false;
        try {
            Method method = obj.getClass().getMethod(methodName, types);
            final Object invocationResult = method.invoke(obj, args);
            if (result != null && result.length > 0) {
                result[0] = invocationResult;
            }
            retval = true;
        } catch (Exception ignore) {    // SUPPRESS CHECKSTYLE
        }
        return retval;
    }

    /**
     * 反射调用（包括非public的）方法。在寻找该方法时，会一直递归到（但不包括）Object class。
     *
     * @param obj        调用该对象所在类的非public方法
     * @param methodName 方法名
     * @param result     如果该方法有返回值，则将返回值放入result[0]中；否则将result[0]置为null
     * @return 如果成功找到并调用该方法，返回true，否则返回false
     */
    public static boolean invokeMethod(Object obj, String methodName, Object[] result) {
        return invokeMethod(obj, methodName, null, result);
    }

    /**
     * 反射调用（包括非public的）方法。在寻找该方法时，会一直递归到（但不包括）Object class。
     *
     * @param obj        调用该对象所在类的非public方法
     * @param methodName 方法名
     * @param params     调用该方法需要的参数
     * @param result     如果该方法有返回值，则将返回值放入result[0]中；否则将result[0]置为null
     * @return 如果成功找到并调用该方法，返回true，否则返回false
     */
    public static boolean invokeMethod(Object obj, String methodName, @Nullable Object[] params, Object[] result) { // SUPPRESS CHECKSTYLE
        Class<?>[] paramTypes = params == null ? null : new Class<?>[params.length];
        if (params != null) {
            for (int i = 0; i < params.length; ++i) {
                paramTypes[i] = params[i] == null ? null : params[i].getClass();
            }
        }
        return invokeMethod(obj, methodName, paramTypes, params, result);
    }

    /**
     * 反射调用（包括非public的）方法。在寻找该方法时，会一直递归到（但不包括）Object class。
     *
     * @param obj        调用该对象所在类的非public方法
     * @param methodName 方法名
     * @param paramTypes 该方法所有参数的类型
     * @param params     调用该方法需要的参数
     * @param result     如果该方法有返回值，则将返回值放入result[0]中；否则将result[0]置为null
     * @return 如果成功找到并调用该方法，返回true，否则返回false
     */
    public static boolean invokeMethod(@Nullable Object obj, String methodName, Class<?>[] paramTypes, Object[] params, // SUPPRESS CHECKSTYLE
                                       @Nullable Object[] result) { // SUPPRESS CHECKSTYLE
        if (obj == null) {
            return false;
        }

        boolean retval = false;

        Method method = getMethod(obj, methodName, paramTypes);
        // invoke
        if (method != null) {
            method.setAccessible(true);
            try {
                final Object invocationResult = method.invoke(obj, params);
                if (result != null && result.length > 0) {
                    result[0] = invocationResult;
                }
                retval = true;
            } catch (IllegalAccessException iae) {
                throw new IllegalArgumentException(methodName, iae);
            } catch (InvocationTargetException ite) {
                throw new IllegalArgumentException(methodName, ite);
            } catch (ExceptionInInitializerError eiie) {
                throw new IllegalArgumentException(methodName, eiie);
            }
        }

        return retval;
    }

    /**
     * 递归查找声明的方法，一直递归到（但不包括）Object class。
     *
     * @param object     指定对象
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @return 指定对象的方法
     */
    public static Method getMethod(@NonNull Object object, String methodName, Class<?>[] paramTypes) {
        Method method;

        Class<?> theClass = object.getClass();
        for (; theClass != Object.class; theClass = theClass.getSuperclass()) {
            try {
                method = theClass.getDeclaredMethod(methodName, paramTypes);
                return method;
            } catch (NoSuchMethodException e) {  //SUPPRESS CHECKSTYLE

            } catch (SecurityException e) {
                throw new IllegalArgumentException(theClass.getName() + "." + methodName, e);
            }
        }

        return null;
    }

    /**
     * 把内容复制到剪切板
     *
     * @param text    复制到剪切板的内容
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void copyToClipboard(final String text, @NonNull Context context) {
        copyToClipboard(text, context, true);
    }

    /**
     * 把内容复制到剪切板
     *
     * @param text    复制到剪切板的内容
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void copyToClipboard(final String text, @NonNull Context context, boolean showToast) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
            // api level < 11
            @SuppressWarnings("deprecation")
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                    context.getApplicationContext().getSystemService(
                            Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setText(text);
                if (showToast) {
                    Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // api level >= 11
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    context.getApplicationContext().getSystemService(
                            Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                //noinspection deprecation
                clipboard.setText(text);
                if (showToast) {
                    Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 清空剪切板
     *
     * @param context
     */
    public static void clearClipboard(@NonNull Context context) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getApplicationContext().getSystemService(
                        Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("", "");
        clipboard.setPrimaryClip(data);
    }

    public static CharSequence getClipbardText(@NonNull Context context) {
        try {
            android.content.ClipboardManager clipboard = null;
            clipboard = (android.content.ClipboardManager)
                    context.getApplicationContext().getSystemService(
                            Context.CLIPBOARD_SERVICE);

            if (!clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().getItemCount() <= 0) {
                return null;
            }
            return clipboard.getPrimaryClip().getItemAt(0).getText();
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
        return "";
    }

    /**
     * 安全启动Activity Intent
     *
     * @param context
     * @param intent
     * @return 成功与否
     */
    public static boolean startActivitySafely(@NonNull Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断Uri是否为web uri（url）
     *
     * @param uri Uri
     * @return true or false
     */
    public static boolean isWebUri(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }


    /**
     * 将data目录中的文件拷贝到外置sd卡中
     *
     * @param context Context
     */
    public static void copyDataToExternalAsync(@NonNull final Context context) {
        final Context appContext = context.getApplicationContext();
        AsyncTask<Void, Void, File> task = new AsyncTask<Void, Void, File>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(appContext, "Start copying data to external...", Toast.LENGTH_SHORT).show();
            }

            @Nullable
            @Override
            protected File doInBackground(Void... params) {
                File sd = Environment.getExternalStorageDirectory();
                if (sd.canWrite()) {
                    String dataPath = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                            appContext.getDataDir().getAbsolutePath() : appContext.getApplicationInfo().dataDir;
                    @SuppressLint("SdCardPath")
                    File srcDir = new File(dataPath);
                    File dstDir = new File(sd.getPath() + dataPath);
                    deleteDir(dstDir);
                    if (copyDir(srcDir)) {
                        return dstDir;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(@Nullable File result) {
                super.onPostExecute(result);
                String msg = result != null ? "success!" : "failed!";
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(result);
                intent.setDataAndType(uri, "resource/folder");
                if (!startActivitySafely(context, intent)) {
                    intent.setDataAndType(uri, "application/zip");
                    startActivitySafely(context, intent);
                }
            }
        };
        task.execute();
    }

    /**
     * copy dir到外置存储中
     *
     * @param dir File
     * @return 是否copy成功
     */
    public static boolean copyDir(@NonNull File dir) {
        boolean rtn = true;
        try {
            File sd = Environment.getExternalStorageDirectory();
            File[] files = dir.listFiles();
            if (sd.canWrite()) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        rtn &= copyDir(file);
                    } else if (file.isFile()) {
                        copyFileToExternalStorage(file);
                    }
                }
            } else {
                rtn = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            rtn = false;
        }
        return rtn;
    }

    /**
     * 将文件拷贝到外置存储中
     *
     * @param file 源文件
     */
    public static boolean copyFileToExternalStorage(@NonNull File file) {
        File sd = Environment.getExternalStorageDirectory();
        File backupFile = new File(sd.getPath() + file.getAbsolutePath());
        return FileUtil.copy(file, backupFile);
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中删除某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean deleteDir(@Nullable File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * 递归清空目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要清空的文件目录
     * @return boolean 成功清除目录及子文件返回true；
     * 若途中清空某一文件或清除目录失败，则终止清除工作并返回false.
     */
    public static boolean clearDir(@Nullable File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取设备上某个volume对应的存储路径
     *
     * @param volume 存储介质
     * @return 存储路径
     */
    public static String getVolumePath(@NonNull Object volume) {
        String[] result = new String[1];
        if (Utility.invokeHideMethod(volume, "getPath", null, null, result)) {
            return result[0];
        }

        return "";
    }

    /**
     * 获取设备上所有volume
     *
     * @param context context
     * @return Volume数组
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Object[] getVolumeList(@NonNull Context context) {
        StorageManager manager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        Object[][] result = new Object[1][];
        if (Utility.invokeHideMethod(manager, "getVolumeList", null, null, result)) {
            return result[0];
        }

        return null;
    }

    /**
     * 获取设备上某个volume的状态
     *
     * @param context    context
     * @param volumePath volumePath
     * @return result
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String getVolumeState(@NonNull Context context, String volumePath) {
        StorageManager manager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        String[] result = new String[1];
        if (Utility.invokeHideMethod(manager, "getVolumeState",
                new Class[]{String.class}, new Object[]{volumePath}, result)) {
            return result[0];
        }

        return "";
    }

    /**
     * 在url后批量增加多个参数。对于每个参数，如果url中已有该参数，则将该参数的value替换为新值.
     *
     * @param url    在url附加参数
     * @param params 多个参数的key - value键值对
     * @return 加完（或修改完）参数后的url.
     */
    public static String addParams(String url, @Nullable Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            for (Entry<String, String> entry : params.entrySet()) {
                url = addParam(url, entry.getKey(), entry.getValue());
            }
        }
        return url;
    }

    /**
     * 在url后增加参数, 如果已有该参数，则将该参数的value替换为新值.
     *
     * @param url   在url附加参数
     * @param key   参数key
     * @param value 参数value
     * @return 加完（或修改完）参数后的url.
     */
    public static String addParam(String url, String key, String value) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        String[] subUrlHolder = new String[1];
        url = removeSubUrl(url, subUrlHolder);

        key += '=';

        int index = url.indexOf('?');
        if (index < 0) { //原来没有参数
            return addFirstParam(url, key, value) + subUrlHolder[0];
        }

        //在'?'后查找是否参数已存在
        int keyIndex = url.indexOf('&' + key, index);
        if (keyIndex == -1) {
            keyIndex = url.indexOf('?' + key, index);
        }
        if (keyIndex != -1) { //已经存在
            return replaceParam(url, value, keyIndex + 1 + key.length()) + subUrlHolder[0];
        }

        return addFollowedParam(url, key, value) + subUrlHolder[0];
    }

    /**
     * 加上第一个参数
     *
     * @param url   在url附加参数
     * @param key   参数key（带"="）
     * @param value 参数value
     * @return 加完参数后的url.
     */
    @NonNull
    private static String addFirstParam(String url, String key, String value) {
        return url + '?' + key + value;
    }

    /**
     * 追加参数
     *
     * @param url   在url附加参数
     * @param key   参数key（带"="）
     * @param value 参数value
     * @return 加完参数后的url.
     */
    private static String addFollowedParam(@NonNull String url, String key, String value) {
        StringBuilder sb = new StringBuilder(url);
        if (!url.endsWith("&") && !url.endsWith("?")) {    //SUPPRESS CHECKSTYLE
            sb.append('&');
        }
        sb.append(key).append(value);
        return sb.toString();
    }

    /**
     * 替换参数值
     *
     * @param url        即将替换参数值的url
     * @param value      参数的新值
     * @param valueStart 原参数值在url中的位置
     * @return 替换完参数值的url
     */
    private static String replaceParam(@NonNull String url, String value, int valueStart) {
        int valueEnd = url.indexOf('&', valueStart);
        if (valueEnd == -1) {
            valueEnd = url.length();
        }

        StringBuilder sb = new StringBuilder(url);
        sb.replace(valueStart, valueEnd, value);

        return sb.toString();
    }

    /**
     * 删除参数
     *
     * @param url       原url
     * @param paramName 参数名
     * @return 删除完参数后的url
     */
    @NonNull
    public static String removeParam(@NonNull String url, String paramName) {
        String value;
        String[] subUrlHolder = new String[1];
        String ret = removeSubUrl(url, subUrlHolder);

        if ((value = getQueryValue(new StringBuilder(ret), '?' + paramName + '=')) // SUPPRESS CHECKSTYLE
                != null) {
            ret = ret.replace(paramName + '=' + value, "");
            if (ret.endsWith("?")) {
                ret = ret.substring(0, ret.length() - 1);
            }
        } else if ((value = getQueryValue(new StringBuilder(ret), '&' + paramName + '='))    // SUPPRESS CHECKSTYLE
                != null) {
            ret = ret.replace('&' + paramName + '=' + value, "");
        }

        return ret + subUrlHolder[0];
    }

    /**
     * 获取url中指定参数的值
     *
     * @param url       Url
     * @param paramName 参数名
     * @return 参数值
     */
    @Nullable
    public static String getParamValue(@NonNull String url, String paramName) {
        String value;
        StringBuilder sb = new StringBuilder(removeSubUrl(url, null));

        if ((value = getQueryValue(sb, '?' + paramName + '=')) == null) {    // SUPPRESS CHECKSTYLE
            value = getQueryValue(sb, '&' + paramName + '=');
        }

        return value;
    }

    @NonNull
    private static String removeSubUrl(@NonNull String url, @Nullable String[] subUrlHolder) {
        int indexHash = url.indexOf('#');
        if (indexHash < 0) { //没有#
            if (subUrlHolder != null) {
                subUrlHolder[0] = "";
            }
            return url;
        }

        if (subUrlHolder != null) {
            subUrlHolder[0] = url.substring(indexHash);
        }
        return url.substring(0, indexHash);
    }

    private static String getQueryValue(@NonNull StringBuilder sb, @NonNull String query) {
        int index = sb.indexOf(query);

        if (index != -1) {
            int startIndex = index + query.length();
            int endIndex = sb.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = sb.length();
            }

            return sb.substring(startIndex, endIndex);
        }

        return null;
    }

    /**
     * 根据原有的Spannable（比如Html.fromHtml后的结果）生成没有下划线并且可以指定字体颜色的Spannable
     *
     * @param src   源Spannable
     * @param color 字体颜色
     * @return 处理后的Spannable
     */
    @NonNull
    public static Spannable makeNoUnderlineColorHtml(@NonNull Spannable src, int color) {
        URLSpan[] spans = src.getSpans(0, src.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = src.getSpanStart(span);
            int end = src.getSpanEnd(span);
            src.removeSpan(span);
            URLSpan urlSpan = new URLSpanNoUnderline(span.getURL());
            src.setSpan(urlSpan, start, end, 0);
            ForegroundColorSpan fColorSpan = new ForegroundColorSpan(color);
            src.setSpan(fColorSpan, start, end, 0);
        }
        return src;
    }

    /**
     * 将Spannable（比如Html.fromHtml后的结果）中的URLSpan替换成自定义的URLSpan，以便自行控制点击跳转
     *
     * @param src 源Spannable
     * @return 处理后的Spannable
     */
    @NonNull
    public static Spannable replaceURLSpan(@NonNull Spannable src) {
        URLSpan[] spans = src.getSpans(0, src.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = src.getSpanStart(span);
            int end = src.getSpanEnd(span);
            src.removeSpan(span);
            URLSpan urlSpan = new SelfURLSpan(span.getURL());
            src.setSpan(urlSpan, start, end, 0);
        }
        return src;
    }

    public static class SelfURLSpan extends URLSpanNoUnderline {

        public SelfURLSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(@NonNull View widget) {
            Uri uri = Uri.parse(getURL());
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());

            List<ResolveInfo> matchList = context.getPackageManager().queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (!matchList.isEmpty()) {
                String packageName = context.getPackageName();
                for (ResolveInfo info : matchList) {
                    //优先用我们自己打开
                    if (TextUtils.equals(info.activityInfo.packageName, packageName)) {
                        intent.setPackage(packageName);
                        break;
                    }
                }
                context.startActivity(intent);
            }

        }
    }

    @NonNull
    public static Spannable inserImageSpan(CharSequence src, BitmapDrawable bd, int index) {
        bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
        ImageSpan span = new CenteredImageSpan(bd);
        SpannableString ss = new SpannableString(src);
        ss.setSpan(span, index, index + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @NonNull
    public static Spannable inserImageSpan(CharSequence src, Bitmap bmp, int index) {
        ImageSpan span = new CenteredImageSpan(bmp);
        SpannableString ss = new SpannableString(src);
        ss.setSpan(span, index, index + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return ss;
    }

    /**
     * 将Spannable（比如Html.fromHtml后的结果）中的ImageSpan替换成CenteredImageSpan
     *
     * @param src 源Spannable
     * @return 处理后的Spannable
     */
    @NonNull
    public static Spannable replaceImageSpan(@NonNull Spannable src) {
        ImageSpan[] spans = src.getSpans(0, src.length(), ImageSpan.class);
        for (ImageSpan span : spans) {
            int start = src.getSpanStart(span);
            int end = src.getSpanEnd(span);
            src.removeSpan(span);
            Drawable d = span.getDrawable();
            ImageSpan newSpan = new CenteredImageSpan(d);
            src.setSpan(newSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return src;
    }

    /**
     * get activity from context
     *
     * @param context
     * @return
     */
    public static Activity getActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }

        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * 将异常的StackTrace输出到字串中
     *
     * @param e 异常
     * @return StackTrace字串
     */
    public static String getPrintStackTrace(@Nullable Throwable e) {
        if (e == null) {
            return "";
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        return writer.toString();
    }


    @SuppressWarnings("deprecation")
    @NonNull
    public static HttpRequest stringToHttpRequest(@NonNull final String requestAsString) {
        try {
            SessionInputBuffer inputBuffer = new AbstractSessionInputBuffer() {
                {
                    init(new ByteArrayInputStream(requestAsString.getBytes(UTF8)),
                            10, new BasicHttpParams());    //SUPPRESS CHECKSTYLE
                }

                @Override
                public boolean isDataAvailable(int timeout) throws IOException {
                    throw new RuntimeException(
                            "have to override but probably not even called");
                }
            };
            HttpMessageParser parser = new HttpRequestParser(inputBuffer,
                    new BasicLineParser(new ProtocolVersion("HTTP", 1, 1)),
                    new DefaultHttpRequestFactory(), new BasicHttpParams());
            HttpMessage message = parser.parse();
            if (message instanceof BasicHttpEntityEnclosingRequest) {
                BasicHttpEntityEnclosingRequest request = (BasicHttpEntityEnclosingRequest) message;
                EntityDeserializer entityDeserializer = new EntityDeserializer(
                        new LaxContentLengthStrategy());
                HttpEntity entity = entityDeserializer.deserialize(inputBuffer,
                        message);
                request.setEntity(entity);
            }
            return (HttpRequest) message;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取IP信息。如果没有，默认返回127.0.0.1
     *
     * @return info
     */
    @Nullable
    public static String getIpInfo() {
        return getIpInfo("127.0.0.1");
    }

    /**
     * 获取IP信息
     *
     * @return info
     */
    @Nullable
    public static String getIpInfo(String defaultIp) {
        String ipInfo = null;
        System.setProperty("java.net.preferIPv6Addresses", "false");
        try {
            Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
            while (faces.hasMoreElements()) {
                Enumeration<InetAddress> addresses = faces.nextElement().getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();

                    if (!inetAddress.isLoopbackAddress()) {
                        ipInfo = inetAddress.getHostAddress();
                        if (!ipInfo.contains("::")) {    //滤过ipv6形式
                            return ipInfo;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ipInfo != null ? ipInfo : defaultIp;
    }

    /**
     * 返回type类型代表的原始Class类型。参考自{@link TypeToken#getRawType()}
     *
     * @param type
     * @return
     */
    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class.
            // Neal isn't either but suspects some pathological case related
            // to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            //noinspection ConstantConditions
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            Type[] upperBoundTypes = ((TypeVariable) type).getBounds();
            if (upperBoundTypes.length == 1) {
                return getRawType(upperBoundTypes[0]);
            }
            // we could use the variable's bounds, but that won't work if there are multiple.
            // having a raw type that's more general than necessary is okay
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                    + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

    /**
     * 获取cls中的泛型参数类型。参考自{@link TypeToken#getSuperclassTypeParameter(Class)}。
     * <br/>注意：泛型类本身不能获取参数类型（因为被擦除了），只有泛型类的子类才可以获取。
     * <br/>比如ArrayList&lt;String&gt;无法通过ArrayList.class获取String类型；
     * <br/>但如果class MyList extends ArrayList&lt;String&gt;，那么MyList.class是可以获取String类型的。
     */
    public static Type getClassTypeParameter(Class<?> cls) {
        Type superclass = cls.getGenericSuperclass();
        while (superclass instanceof Class) {
            if (superclass == Object.class) {
                throw new RuntimeException(cls.getName() + " extends " + cls.getSuperclass().getName() + ": missing type parameter.");
            } else {
                superclass = ((Class) superclass).getGenericSuperclass();
            }
        }
        if (superclass instanceof Class) {
            throw new RuntimeException(cls.getName() + " extends " + cls.getSuperclass().getName() + ": missing type parameter.");
        }
        ParameterizedType parameterized = (ParameterizedType) superclass;
        return $Gson$Types.canonicalize(parameterized.getActualTypeArguments()[0]);
    }

    /**
     * 判断当前线程，如果不是主线程则抛出异常
     */
    public static void checkMain() {
        if (!isMain()) {
            throw new IllegalStateException("Method should must be called in  main thread.");
        }
    }

    /**
     * 判断当前线程是不是主线程
     *
     * @return
     */
    public static boolean isMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static int stringToInt(String content) {
        int result = 0;
        if (TextUtils.isEmpty(content)) {
            return result;
        }
        try {
            result = Integer.parseInt(content);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 从引用中获取具体值。若引用为空，则返回空
     *
     * @param reference 引用
     * @param <T>
     * @return
     */
    public static <T> T getValueFromReference(@Nullable Reference<T> reference) {
        return reference == null ? null : reference.get();
    }

    public static String getDefaultImageDirectory() {
        File file = new File(Environment.getExternalStorageDirectory() + "/sqxbs/images/goods");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }
}
