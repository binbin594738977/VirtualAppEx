package com.weiliu.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作者：qumiao
 * 日期：2017/6/10 16:56
 * 说明：<br/>根据requestCode响应onActivityResult方法（resultCode为RESULT_OK）。
 *      <br/>方法参数必须为 (Intent data)。且不同方法的value声明不能相同。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HandleActivityResultOK {
    /**
     * 参考startActivityForResult的request code
     * @return
     */
    int value();
}
