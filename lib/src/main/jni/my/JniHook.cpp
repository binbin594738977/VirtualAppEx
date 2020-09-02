#include "JniHook.h"
#include <string.h>
#include <jni.h>

JavaVM *g_vm;

typedef struct {
    const char *method_name;
    void *origin_func;
    void *new_func;
} FuncItem;

typedef JNIEXPORT jobject JNICALL (*openDexFileNative)(JNIEnv *env, jclass cls, jstring p1, jstring p2, jint p3, jobject p4, jobjectArray p5);
typedef JNIEXPORT jobject JNICALL (*getDexNative)(JNIEnv *env, jobject cls);

static jclass gJniHookClass;

static int gNativeOffset;

extern FuncItem gFuncItems[];

FuncItem *getFuncItemFromKey(const char *key);

jobject new_openDexFileNative(JNIEnv *env, jclass cls, jstring  p1, jstring  p2, jint  p3, jobject  p4, jobjectArray  p5) {
    const char *key = "openDexFileNative";
    LOGD("invoke %s", key);
    FuncItem *funcItem = getFuncItemFromKey(key);
    openDexFileNative origin_func = (openDexFileNative) funcItem->origin_func;

    jmethodID onBefore = env->GetStaticMethodID(gJniHookClass,
                                                "on_openDexFileNative_before", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/ClassLoader;[Ljava/lang/Object;[Z)Ljava/lang/Object;");
    jbooleanArray jHookedArray = env->NewBooleanArray(1);
    jobject returnObj = (jobject) env->CallStaticObjectMethod(gJniHookClass, onBefore, p1, p2, p3, p4, p5, jHookedArray);
    jboolean *hookedArray = env->GetBooleanArrayElements(jHookedArray, nullptr);
    jboolean hooked = hookedArray[0];
    env->ReleaseBooleanArrayElements(jHookedArray, hookedArray, 0);

    if (!hooked) {
        returnObj = origin_func(env, cls, p1, p2, p3, p4, p5);
    }

    jmethodID onAfter = env->GetStaticMethodID(gJniHookClass,
                                               "on_openDexFileNative_after", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/ClassLoader;[Ljava/lang/Object;Ljava/lang/Object;[Z)Ljava/lang/Object;");
    jHookedArray = env->NewBooleanArray(1);
    jobject ret = (jobject) env->CallStaticObjectMethod(gJniHookClass, onAfter, p1, p2, p3, p4, p5, returnObj, jHookedArray);
    hookedArray = env->GetBooleanArrayElements(jHookedArray, nullptr);
    hooked = hookedArray[0];
    env->ReleaseBooleanArrayElements(jHookedArray, hookedArray, 0);

    if (hooked) {
        returnObj = ret;
    }

    return returnObj;
}

jobject new_getDexNative(JNIEnv *env, jobject cls ) {
    const char *key = "getDexNative";
    LOGD("invoke %s", key);
    FuncItem *funcItem = getFuncItemFromKey(key);
    getDexNative origin_func = (getDexNative) funcItem->origin_func;

    jmethodID onBefore = env->GetStaticMethodID(gJniHookClass,
                                                "on_getDexNative_before", "(Ljava/lang/Object;[Z)Ljava/lang/Object;");
    jbooleanArray jHookedArray = env->NewBooleanArray(1);
    jobject returnObj = (jobject) env->CallStaticObjectMethod(gJniHookClass, onBefore ,cls, jHookedArray);
    jboolean *hookedArray = env->GetBooleanArrayElements(jHookedArray, nullptr);
    jboolean hooked = hookedArray[0];
    env->ReleaseBooleanArrayElements(jHookedArray, hookedArray, 0);

    if (!hooked) {
        returnObj = origin_func(env, cls );
    }

    jmethodID onAfter = env->GetStaticMethodID(gJniHookClass,
                                               "on_getDexNative_after", "(Ljava/lang/Object;Ljava/lang/Object;[Z)Ljava/lang/Object;");
    jHookedArray = env->NewBooleanArray(1);
    jobject ret = (jobject) env->CallStaticObjectMethod(gJniHookClass, onAfter,cls , returnObj, jHookedArray);
    hookedArray = env->GetBooleanArrayElements(jHookedArray, nullptr);
    hooked = hookedArray[0];
    env->ReleaseBooleanArrayElements(jHookedArray, hookedArray, 0);

    if (hooked) {
        returnObj = ret;
    }

    return returnObj;
}







FuncItem gFuncItems[] = {
        { "openDexFileNative", NULL, (void *)new_openDexFileNative },
        { "getDexNative", NULL, (void *)new_getDexNative },
};

FuncItem *getFuncItemFromKey(const char *key) {
    int i;
    int count = sizeof(gFuncItems) / sizeof(FuncItem);
    for (i = 0; i < count; i++) {
        if (strcmp(gFuncItems[i].method_name, key) == 0) {
            return &gFuncItems[i];
        }
    }
    return NULL;
}


void mark() {
    // Do nothing
};

void searchJniOffset(JNIEnv *env) {

    jclass g_class = env->FindClass(JAVA_CLASS);
    jmethodID mtd_nativeHook = env->GetStaticMethodID(g_class, "nativeMark", "()V");

    size_t startAddress = (size_t) mtd_nativeHook;
    size_t targetAddress = (size_t) mark;

    int offset = 0;
    bool found = false;
    while (true) {
        if (*((size_t *) (startAddress + offset)) == targetAddress) {
            found = true;
            break;
        }
        offset += 4;
        if (offset >= 100) {
            LOGE("Ops: Unable to find the jni function.");
            break;
        }
    }

    if (found) {
        gNativeOffset = offset;
        LOGD("Hoho, Get the offset : %d.", gNativeOffset);
    }
}

void replaceImplementation(JNIEnv *env, jobjectArray methods) {

    int length = env->GetArrayLength(methods);
    jclass cls = env->FindClass("java/lang/reflect/Method");
    jmethodID getNameMethod = env->GetMethodID(cls, "getName", "()Ljava/lang/String;");
    jstring keyString;
    const char *key;
    int i;
    for (i = 0; i < length; ++i) {
        jobject method = env->GetObjectArrayElement(methods, i);
        keyString = (jstring) env->CallObjectMethod(method, getNameMethod);
        key = env->GetStringUTFChars(keyString, NULL);

        size_t mtd_openDexNative = (size_t) env->FromReflectedMethod(method);
        int nativeFuncOffset = gNativeOffset;
        void **jniFuncPtr = (void **) (mtd_openDexNative + nativeFuncOffset);

//        char vmSoName[4] = {0};
//        __system_property_get("ro.build.version.sdk", vmSoName);
//        int sdk;
//        sscanf(vmSoName, "%d", &sdk);
//        LOGD("The vm is art and the sdk int is %d", sdk);

        FuncItem *funcItem = getFuncItemFromKey(key);
        funcItem->origin_func = *jniFuncPtr;
        *jniFuncPtr = funcItem->new_func;

        env->ReleaseStringUTFChars(keyString, key);
    }


}

static JNINativeMethod gMarkMethods[] = {
        {"nativeMark", "()V", (void *) mark}
};


void native_hook(JNIEnv *env, jclass clazz, jobjectArray methods) {
    g_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    g_vm->AttachCurrentThread(&env, NULL);

    jclass g_class = env->FindClass(JAVA_CLASS);
    if (env->RegisterNatives(g_class, gMarkMethods, 1) < 0) {
        return;
    }

    char vmSoName[15] = {0};
    __system_property_get("persist.sys.dalvik.vm.lib", vmSoName);
    LOGD("Find the so name : %s.", strlen(vmSoName) == 0 ? "<EMPTY>" : vmSoName);

    void *vmHandle = dlopen(vmSoName, 0);
    if (!vmHandle) {
        LOGE("Unable to open the %s.", vmSoName);
        vmHandle = RTLD_DEFAULT;
    }

    searchJniOffset(env);
    replaceImplementation(env, methods);
}


static JNINativeMethod gMethods[] = {
        {"hookNativeMethods", "([Ljava/lang/Object;)V",
                (void *) native_hook}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_vm = vm;
    JNIEnv *env;

    LOGE("JNI_Onload start");
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("GetEnv() FAILED!!!");
        return JNI_ERR;
    }

    jclass javaClass = env->FindClass(JAVA_CLASS);
    LOGE("we have found the class: %s", JAVA_CLASS);
    if (javaClass == NULL) {
        LOGE("unable to find class: %s", JAVA_CLASS);
        return JNI_ERR;
    }

    env->UnregisterNatives(javaClass);
    if (env->RegisterNatives(javaClass, gMethods, 1) < 0) {
        LOGE("register methods FAILED!!!");
        return JNI_ERR;
    }

    gJniHookClass = static_cast<jclass>(env->NewGlobalRef(javaClass));

    env->DeleteLocalRef(javaClass);
    LOGI("JavaVM::GetEnv() SUCCESS!");
    return JNI_VERSION_1_6;
}

