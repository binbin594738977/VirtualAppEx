package com.weiliu.library.task.http;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.reflect.TypeToken;
import com.weiliu.library.json.JsonUtil;
import com.weiliu.library.json.JsonVoid;
import com.weiliu.library.task.TaskCallBack;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * http任务请求回调，在结束时显示响应信息（如果有的话），或者根据情况显示异常信息（如果发生了异常）。
 * Created by qumiao on 2016/5/3.
 */
public abstract class HttpCallBack<T>
        implements TaskCallBack<HttpResponseObject> {

    private boolean mInterruptFollowingTask;
    private HttpTraceObject mTraceObject;


    public HttpCallBack() {

    }

    /**
     * http请求跟踪信息。包括各阶段耗时、最终请求的url、statusCode、error等等。
     */
    public HttpTraceObject getTraceObject() {
        return mTraceObject;
    }

    @Override
    public void interruptFollowingTask() {
        mInterruptFollowingTask = true;
    }

    @Override
    public boolean isInterruptedFollowingTask() {
        return mInterruptFollowingTask;
    }

    @Override
    public void onPreviewWithCache(HttpResponseObject cache) {
        if (cache.data != null) {
            try {
                T resultData = getData(cache, true);
                if (resultData != null) {
                    int code = cache.getCode();
                    if (code == HttpURLConnection.HTTP_OK || code == 0) {
                        previewCache(resultData);
                    }
                }
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(Integer... values) {

    }

    @Override
    public void onCancelled(HttpResponseObject result, Object extra, Throwable exception) {
        mTraceObject = (HttpTraceObject) extra;
    }

    @Override
    public boolean isResultFailed(HttpResponseObject result, Object extra, Throwable exception) {
        return exception != null || result == null
                || (result.channel == null && result.getCode() != HttpURLConnection.HTTP_OK)
                || (result.channel != null && ((HttpTraceObject) extra).getHttpStatus() != HttpURLConnection.HTTP_OK);
    }

    @Override
    public void onPostExecute(HttpResponseObject result, Object extra, Throwable exception) {
        mTraceObject = (HttpTraceObject) extra;
        int httpStatus = mTraceObject.getHttpStatus();
        if (exception != null || result == null) {
            failed(null, httpStatus, 0, null, exception);
            return;
        }

        T resultData = null;
        int code = result.getCode();
        String msg = result.getMsg();
        if (httpStatus == HttpURLConnection.HTTP_OK && (code == HttpURLConnection.HTTP_OK || code == 0)) {

            try {
                resultData = getData(result, false);
                if (resultData != null || acceptNullResult()) {
                    success(resultData, msg);
                } else {
                    //若不接受null结果，则视为格式解析失败
                    HttpFormatException formatException = new HttpFormatException();
                    exception = formatException;
                    mTraceObject.setException(formatException);
                    failed(resultData, httpStatus, code, msg, exception);
                }
            } catch (Exception e) {
                e.printStackTrace();
                //视为格式解析失败
                HttpFormatException formatException = new HttpFormatException(e);
                exception = formatException;
                mTraceObject.setException(formatException);
                failed(resultData, httpStatus, code, msg, exception);
            }
        } else {
            try {
                resultData = getData(result, false);
            } catch (Exception ignored) {

            }
            failed(resultData, httpStatus, code, msg, exception);
        }
    }


    /**
     * 是否允许结果为null。默认不允许
     * @return 为true表示允许，从而null结果将回调到{@link #success}中；否则，null结果将回调到{@link #failed}中。
     */
    protected boolean acceptNullResult() {
        return false;
    }

    private T getData(HttpResponseObject result, boolean isCache) throws Exception {
        if (result.channel != null) {
            return getResultBinaryData(result.channel, isCache);
        }
        Type type = getClassTypeParameter(this);
        if (type == JsonVoid.class) {
            //noinspection unchecked
            return (T) new JsonVoid() {};
        }
        return getResultData(result.data, isCache);
    }

    /**
     * 子类可以重载该方法，根据需要返回正确的resultData。
     * 只有处理二进制类型（而非String类型）的响应结果时，才需要重载该方法。
     * @param channel
     * @return
     * @throws Exception
     */
    protected T getResultBinaryData(HttpBinaryChannel channel, boolean isCache) throws Exception {
        return null;
    }

    /**
     * 子类可以重载该方法，根据需要返回正确的resultData。
     * 常规方法下并不需要重载此方法。
     * @return
     */
    protected T getResultData(JsonElement jsonData, boolean isCache) throws Exception {
        Type type = getClassTypeParameter(this);
        if (type == JsonObject.class) {
            //noinspection unchecked
            return (T) jsonData.getAsJsonObject();
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            Type[] arguments = pType.getActualTypeArguments();

            // 如果Result的类型为 List<T extends JsonInterface>，则使用List解析方式
            if ((rawType instanceof Class) && ((Class<?>) rawType).isAssignableFrom(List.class)
                    && arguments.length == 1 && (arguments[0] instanceof Class)) {
                if (jsonData.isJsonArray()) {    //data本身就是一个array，则将整个data作为list解析
                    //noinspection unchecked
                    return (T) JsonUtil.jsonArrayToGenericList(jsonData.getAsJsonArray(), arguments[0]);
                }

                JsonElement jsonElement = jsonData.getAsJsonObject().get(getResultListKey());
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                //noinspection unchecked
                return (T) JsonUtil.jsonArrayToGenericList(jsonArray, arguments[0]);
            }
        }
        //noinspection unchecked
        return JsonUtil.jsonToGenericObjectThrowsException(jsonData, type);
    }

    /**
     * 获取cls中的泛型参数类型。参考自{@link TypeToken#getSuperclassTypeParameter(Class)}。
     * <br/>注意：泛型类本身不能获取参数类型（因为被擦除了），只有泛型类的子类才可以获取。
     * <br/>比如ArrayList&lt;String&gt;无法通过ArrayList.class获取String类型；
     * <br/>但如果class MyList extends ArrayList&lt;String&gt;，那么MyList.class是可以获取String类型的。
     */
    public static Type getClassTypeParameter(Object obj) {
        Class<?> cls = obj.getClass();
        Type superclass = cls.getGenericSuperclass();
        while (superclass instanceof Class) {
            if (superclass == Object.class) {
                throw new RuntimeException(cls.getName() + " extends " + cls.getSuperclass().getName() + ": missing type parameter.");
            } else {
                superclass = ((Class) superclass).getGenericSuperclass();
            }
        }

        ParameterizedType parameterized = (ParameterizedType) superclass;
        Type type = parameterized.getActualTypeArguments()[0];

        // 如果还为变量型参数，则尝试一下外部类
        if (type instanceof TypeVariable) {
            try {
                // 只有非静态类才能引用外部类的泛型参数（非静态类拥有this$0属性，并且为外部类的引用）
                Field field = cls.getDeclaredField("this$0");
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    return getClassTypeParameter(value);
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException ignored) {
            }
            throw new RuntimeException(cls.getName() + " extends " + cls.getSuperclass().getName() + ": missing type parameter.");
        }

        return $Gson$Types.canonicalize(type);
    }


    /**
     * ResultData中映射list的key。默认为"List"，子类可重新定义
     * @return
     */
    protected String getResultListKey() {
        return "List";
    }

    public abstract void previewCache(T resultData);

    public abstract void success(T resultData, @Nullable String info);

    public abstract void failed(@Nullable T resultData, int httpStatus, int code, @Nullable String info, @Nullable Throwable e);
}
