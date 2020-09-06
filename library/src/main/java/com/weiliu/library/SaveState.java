package com.weiliu.library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 加上该注解，表示该字段需要保存和恢复。
 * <br/>
 * Created by qumiao on 2016/8/18.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SaveState {


    /**
     * 保存和恢复的顺序。越小越靠前，越大越靠后。
     * @return
     */
    int order() default 0;

}
