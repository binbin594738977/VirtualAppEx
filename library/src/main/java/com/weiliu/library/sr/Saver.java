package com.weiliu.library.sr;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/10 10:28
 * 说明：用来实现保存与恢复现场
 */

public interface Saver {

    /**
     * 保存状态
     * @param outState 状态保存处
     * @param savable 保存的对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 成功与否
     */
    boolean saveInstanceState(@NonNull Bundle outState, @NonNull Savable savable,
                              @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap);

    /**
     * 保存Savable类属性的状态
     * @param savable 保存的对象，Savable类的属性所在的对象
     * @param outState 状态保存处
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @param savableMap 已保存的Savable映射表。用来查询已保存的引用，从而避免循环引用的递归。
     * @return 成功与否
     */
    boolean saveSavableFields(@NonNull Bundle outState, @NonNull Savable savable,
                              @NonNull SavePath path,
                              @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap,
                              @NonNull Map<Savable, List<Savable>> savableMap);

    /**
     * 恢复状态
     * @param savedInstanceState 状态保存处
     * @param savable 保存的对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @return 成功与否
     */
    boolean restoreInstanceState(@Nullable Bundle savedInstanceState, @NonNull Savable savable,
                                 @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap);

    /**
     * 恢复为Savable类的属性
     * @param savedInstanceState 状态保存处
     * @param savable 保存的对象，Savable类的属性所在的对象
     * @param path 相对跟Bundle所处的路径
     * @param referenceMap 引用与保存路径的映射表。用来查询已保存的引用，从而避免循环引用的递归。
     *                      此处没有直接使用Map<Object, SavePath>，
     *                      是为了避免对象之间内容不相同但是equals返回true（其类重写了equals方法），
     *                      导致只保存了其中一份。所以改用List + Pair，并且只用 == 判断对象是否相等。
     * @param savableMap 已恢复的Savable映射表。用来查询已恢复的引用，从而避免循环引用的递归。
     * @return 成功与否
     */
    boolean restoreSavableFields(@NonNull Bundle savedInstanceState, @NonNull Savable savable,
                                 @NonNull SavePath path,
                                 @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap,
                                 @NonNull Map<Savable, List<Savable>> savableMap);
}
