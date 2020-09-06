package com.weiliu.library.sr;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;

import com.weiliu.library.SaveState;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作者：qumiao
 * 日期：2017/3/10 11:55
 * 说明：
 */

public class DefaultSaver implements Saver {

    private static final String TAG = "DefaultSaver";
    private static final boolean DEBUG = true;


    @Override
    public boolean saveInstanceState(@NonNull Bundle outState, @NonNull Savable savable,
                                     @NonNull SavePath path, @NonNull Map<Object, List<Pair<Object, SavePath>>> referenceMap) {
        Bundle tempState = new Bundle();
        Object owner = savable.getOwner();
        for (Field field : SaveRestoreUtil.getSaveStateFields(owner)) {
            SaveState annotation = field.getAnnotation(SaveState.class);
            String key = getSaveStateKey(field);
            if (annotation != null) {
                try {
                    if (!SaveRestoreUtil.saveToBundle(tempState, key, field, owner, path, referenceMap)) {
                        return false;
                    }
                } catch (Exception e) {
                    warn(String.format("field (%s) save failed!", field), e);
                    return false;
                }
            }
        }
        outState.putAll(tempState);
        return true;
    }

    @Override
    public boolean saveSavableFields(@NonNull Bundle outState, @NonNull Savable savable,
                                     @NonNull final SavePath path,
                                     @NonNull final Map<Object, List<Pair<Object, SavePath>>> referenceMap,
                                     @NonNull final Map<Savable, List<Savable>> savableMap) {
        final Bundle tempState = new Bundle();
        Object owner = savable.getOwner();
        boolean result = traverseSavableField(savable, owner, savableMap, new TraverseSavableCallback() {
            @Override
            public boolean traverse(Field savableField, Savable value) {
                try {
                    value.onSave();
                    String key = getSaveStateKey(savableField);
                    Bundle fieldState = new Bundle();
                    Saver saver = value.getSaver();
                    if (!saver.saveInstanceState(fieldState, value, new SavePath(path).appendKey(key), referenceMap)
                            || !saver.saveSavableFields(fieldState, value, new SavePath(path).appendKey(key), referenceMap, savableMap)) {
                        return false;
                    }
                    tempState.putBundle(key, fieldState);
                } catch (Exception e) {
                    warn(String.format("field (%s) save fields failed!", savableField), e);
                    return false;
                }
                return true;
            }
        });

        if (result) {
            outState.putAll(tempState);
        }
        return result;
    }

    @Override
    public boolean restoreInstanceState(@Nullable Bundle savedInstanceState, @NonNull Savable savable,
                                        @NonNull SavePath path, @NonNull Map<SavePath, List<Pair<SavePath, Object>>> referenceMap) {
        Object owner = savable.getOwner();
        for (Field field : SaveRestoreUtil.getSaveStateFields(owner)) {
            SaveState annotation = field.getAnnotation(SaveState.class);
            String key = getSaveStateKey(field);
            if (savedInstanceState != null) {
                try {
                    if (!SaveRestoreUtil.restoreFromBundle(savedInstanceState, key, field, owner, path, referenceMap)) {
                        return false;
                    }
                } catch (Exception e) {
                    warn(String.format("field (%s) restore failed!", field), e);
                    return false;
                }
            } else {
                try {
                    SaveRestoreUtil.check(key, field, owner);
                } catch (Exception e) {
                    warn(String.format("field (%s) check failed!", field), e);
                }
            }
        }

        return true;
    }

    @Override
    public boolean restoreSavableFields(@NonNull final Bundle savedInstanceState, @NonNull Savable savable,
                                        @NonNull final SavePath path,
                                        @NonNull final Map<SavePath, List<Pair<SavePath, Object>>> referenceMap,
                                        @NonNull final Map<Savable, List<Savable>> savableMap) {
        Object owner = savable.getOwner();
        return traverseSavableField(savable, owner, savableMap, new TraverseSavableCallback() {
            @Override
            public boolean traverse(Field savableField, Savable value) {
                try {
                    String key = getSaveStateKey(savableField);
                    Bundle fieldState = savedInstanceState.getBundle(key);
                    Saver saver = value.getSaver();
                    if (!saver.restoreInstanceState(fieldState, value, new SavePath(path).appendKey(key), referenceMap)
                            || !saver.restoreSavableFields(fieldState, value, new SavePath(path).appendKey(key), referenceMap, savableMap)) {
                        return false;
                    }
                } catch (Exception e) {
                    warn(String.format("field (%s) save fields failed!", savableField), e);
                    return false;
                } finally {
                    value.onRestored();
                }
                return true;
            }
        });
    }

    private static String getSaveStateKey(Field field) {
        return field.toString();
    }

    private static void warn(String message, Exception e) {
        Log.e(TAG, message, e);
        if (DEBUG) {
            throw new RuntimeException(message, e);
        }
    }

    private static boolean traverseSavableField(Savable currentSavable,
            Object owner, Map<Savable, List<Savable>> savableMap, TraverseSavableCallback callback) {
        Class<?> cls = owner.getClass();
        ArrayList<Pair<Field, Savable>> savableFieldList = new ArrayList<>();
        while (cls != Object.class) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                // Fragment和Activity本身有保存与恢复的框架，无需再执行一次
                if (Fragment.class.isAssignableFrom(field.getType())
                        || Activity.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                if (Savable.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Savable savableFieldValue = (Savable) field.get(owner);
                        if (savableFieldValue == currentSavable) {
                            continue;
                        }
                        if (savableFieldValue != null && !containsSavable(savableFieldValue, savableMap)) {
                            addSavable(savableFieldValue, savableMap);
                            savableFieldList.add(new Pair<Field, Savable>(field, savableFieldValue));
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            cls = cls.getSuperclass();
        }

        for (Pair<Field, Savable> pair : savableFieldList) {
            if (!callback.traverse(pair.first, pair.second)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsSavable(Savable obj, Map<Savable, List<Savable>> savableMap) {
        if (!savableMap.containsKey(obj)) {
            return false;
        }
        List<?> list = savableMap.get(obj);
        for (Object ob : list) {
            if (obj == ob) {
                return true;
            }
        }
        return false;
    }

    private static void addSavable(Savable obj, Map<Savable, List<Savable>> savableMap) {
        List<Savable> list = savableMap.get(obj);
        if (list == null) {
            list = new ArrayList<>();
            savableMap.put(obj, list);
        }
        list.add(obj);
    }

    public interface TraverseSavableCallback {
        /**
         * 遍历可保存类的属性
         * @param savableField 可保存类的属性
         * @param value 属性值
         * @return 是否成功处理
         */
        boolean traverse(Field savableField, Savable value);
    }
}
