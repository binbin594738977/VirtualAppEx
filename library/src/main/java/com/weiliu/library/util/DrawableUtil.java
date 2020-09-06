package com.weiliu.library.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * Drawable方法集合
 * <br/>
 * Created by qumiao on 2016/10/8.
 */

public class DrawableUtil {

    private DrawableUtil() {
        //no instance
    }

    public static Drawable tintDrawable(Context context, @DrawableRes int drawableRes, @ColorRes int selectorColorRes) {
        //noinspection deprecation
        ColorStateList selectorColors = context.getResources().getColorStateList(selectorColorRes);
        //noinspection deprecation
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        return tintDrawable(drawable, selectorColors);
    }

    public static Drawable tintDrawable(Context context, Drawable drawable, @ColorRes int selectorColorRes) {
        //noinspection deprecation
        ColorStateList selectorColors = context.getResources().getColorStateList(selectorColorRes);
        return tintDrawable(drawable, selectorColors);
    }

    public static Drawable tintDrawable(Drawable drawable, ColorStateList selectorColors) {
        final Drawable wrappedDrawable = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTintList(wrappedDrawable, selectorColors);
        return wrappedDrawable;
    }
}
