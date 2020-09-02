//
// Created by 屈淼 on 2018/3/7.
//

#ifndef VIRTUALAPP_WUTAHOOK_H
#define VIRTUALAPP_WUTAHOOK_H

#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <dlfcn.h>
#include <stddef.h>
#include <fcntl.h>
#include <sys/system_properties.h>
#include <stdio.h>

#define TAG "WEILIU_HOOK"
#define JAVA_CLASS "library/JniHook"


#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

__BEGIN_DECLS
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved);
__END_DECLS


#endif //VIRTUALAPP_WUTAHOOK_H
