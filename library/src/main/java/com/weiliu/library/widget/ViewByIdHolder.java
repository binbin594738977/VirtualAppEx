package com.weiliu.library.widget;

import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.weiliu.library.ViewById;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 自动通过@ViewById 来解析View的ViewHolder。
 * Created by qumiaowin on 2016/6/29.
 */
public class ViewByIdHolder extends RecyclerView.ViewHolder {

    public ViewByIdHolder(View itemView) {
        super(itemView);
        initViewsByAnnotation(itemView);
    }

    private void initViewsByAnnotation(View itemView) {
        Class<?> cls = getClass();
        while (cls != Object.class) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                ViewById viewByIdAnnotation = field.getAnnotation(ViewById.class);
                if (viewByIdAnnotation != null) {
                    handleViewById(itemView, field, viewByIdAnnotation);
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private void handleViewById(View itemView, Field field, ViewById viewByIdAnnotation) {
        checkAnnotationOwner(field, viewByIdAnnotation);
        try {
            field.setAccessible(true);
            field.set(this, itemView.findViewById(viewByIdAnnotation.value()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void checkAnnotationOwner(Field field, Annotation annotation) {
        if (!View.class.isAssignableFrom(field.getType())) {
            throw new RuntimeException(field.getName() + " is not a view, cannot define annotation " + annotation);
        }
    }

    public View findViewById(@IdRes int id) {
        return itemView.findViewById(id);
    }
}
