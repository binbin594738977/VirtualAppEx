package com.weiliu.library.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/6/7 17:37
 * 说明：
 */
public class CollectionUtil {

    private CollectionUtil() {
        //no instance
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean contains(Collection<?> collection, Object data) {
        return !isEmpty(collection) && collection.contains(data);
    }

    public static void removeNullValues(Map<?, ?> map) {
        ArrayList<Object> list = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                list.add(entry.getKey());
            }
        }

        for (Object key : list) {
            map.remove(key);
        }
    }
}
