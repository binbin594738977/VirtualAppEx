package com.weiliu.library.sr;

import android.support.annotation.NonNull;

/**
 * 作者：qumiao
 * 日期：2017/3/10 10:02
 * 说明：可保存与恢复的类
 */

public interface Savable {

    /**
     * 默认可以使用{@link DefaultSaver}
     * @return
     */
    @NonNull
    Saver getSaver();

    @NonNull
    Object getOwner();

    void onSave();

    void onRestored();
}
