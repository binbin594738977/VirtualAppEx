package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/3 10:33
 * 说明：类T在savedInstanceState中的保存与恢复逻辑
 */
interface SaveRestore<T> {

    /**
     * 将T类型的属性值，保存到bundle中
     * @param bundle 用来保存
     * @param key 保存属性值时的参考key
     * @param fieldType 属性的类型（可能为泛型类型，但其原生类型一定为T）
     * @param value 属性值。需要将其保存到bundle中
     * @param fieldOwner 属性的隶属类对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 是否成功保存
     * @throws Exception
     */
    boolean save(@NonNull Bundle bundle, @NonNull String key,
                 @NonNull Type fieldType, @NonNull T value, @NonNull Object fieldOwner,
                 @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) throws Exception;

    /**
     * 根据bundle中的信息，创建之前保存的属性值的对象。
     * @param bundle 属性值的存放处
     * @param key 保存属性值时的参考key
     * @param fieldType 属性的类型（可能为泛型类型，但其原生类型一定为T）
     * @param fieldOwner 属性的隶属类对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 之前保存的属性值
     * @throws Exception
     */
    @Nullable
    T createInstance(@NonNull Bundle bundle, @NonNull String key,
                     @NonNull Type fieldType, @NonNull Object fieldOwner,
                     @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception;

    /**
     * 从bundle中恢复之前保存属性值里的具体内容
     * @param bundle 属性值的存放处
     * @param key 保存属性值时的参考key
     * @param fieldType 属性的类型（可能为泛型类型，但其原生类型一定为T）
     * @param fieldOwner 属性的隶属类对象
     * @param value 属性值
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 是否成功恢复
     * @throws Exception
     */
    boolean restore(@NonNull Bundle bundle, @NonNull String key,
                    @NonNull Type fieldType, @NonNull Object fieldOwner, @NonNull T value,
                    @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) throws Exception;
}
