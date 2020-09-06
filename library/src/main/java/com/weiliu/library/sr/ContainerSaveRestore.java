package com.weiliu.library.sr;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 作者：qumiao
 * 日期：2017/3/3 10:34
 * 说明：泛型容器类T在savedInstanceState中的保存与恢复逻辑
 */
interface ContainerSaveRestore<T> extends SaveRestore<T> {
    /**
     * 将恢复值（泛型容器）中的元素，全拷贝到属性中。比如针对Collection，则使用addAll方法进行全拷贝。<br/>
     * 主要解决属性为final时的恢复逻辑。
     * @param fieldObj 属性的对象引用。
     *                 比如 final List&lt;String&gt; mStringList = new ArrayList&lt;&gt;(); 中的mStringList。
     * @param newValue 从bundle中恢复的值（是个泛型容器，比如 ArrayList&lt;String&gt;）。
     */
    void copyElement(@NonNull T fieldObj, @Nullable T newValue);
}
