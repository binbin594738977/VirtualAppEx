package library;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.lody.virtual.helper.utils.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

/**
 *
 * Created by qumiao on 2018/3/20.
 */

public class WutaHook {

    public static void forWuTaXiangji(Application initialApplication) {
        final String LOG_WEILIU = "WEILIU_HOOK";
        try {
            hookPackageManager();

            Class appClass = Class.forName("io.virtualapp.VApp");
            Field appField = appClass.getDeclaredField("gApp");
            appField.setAccessible(true);
            Application application = (Application) appField.get(null);
            String dexName = "classes.jar";
            InputStream inputStream = application.getAssets().open(dexName);
            File dir = application.getCacheDir();
            File dexFile = new File(dir, dexName);
            FileUtils.writeToFile(inputStream, dexFile);
            Log.d(LOG_WEILIU, "dexFile = " + dexFile + " isFile: " + dexFile.isFile() + " size: " + dexFile.length());


            Class aClass = Class.forName("com.benqu.core.jni.b.a", false, initialApplication.getClassLoader());

            DexClassLoader dexClassLoader = new DexClassLoader(dexFile.getPath(), dir.getPath(), null, aClass.getClassLoader());
            Class weiliuAClass = dexClassLoader.loadClass("com.benqu.core.jni.b.WeiliuA");
            Object weiliuAObj = weiliuAClass.newInstance();

            Class iClass = Class.forName("com.benqu.core.i", false, initialApplication.getClassLoader());
            Field aField = iClass.getDeclaredField("a");
            aField.setAccessible(true);
            Object aArrayObj = aField.get(null);
            Array.set(aArrayObj, 0, weiliuAObj);

            Class cls = Class.forName("com.benqu.core.jni.WTJNI", false, initialApplication.getClassLoader());
            Method[] methods = cls.getDeclaredMethods();
            ArrayList<Method> list = new ArrayList<>();
            for (Method method : methods) {
                if (Modifier.isNative(method.getModifiers())) {
                    method.setAccessible(true);
                    list.add(method);
                }
            }
            if (!list.isEmpty()) {
                JniHook.hookNativeMethods(list.toArray());
                Log.d(LOG_WEILIU, "hook wtjni native methods success!");
            }



            cls = Class.forName("com.benqu.core.jni.a.a", false, initialApplication.getClassLoader());
            Field field = cls.getDeclaredField("a");
            field.setAccessible(true);
            Object a = field.get(null);

            Method bMethod = a.getClass().getMethod("b");
            bMethod.setAccessible(true);
            ByteBuffer byteBuffer = (ByteBuffer) bMethod.invoke(a);
            Log.d(LOG_WEILIU, "vapp invoke com.benqu.core.jni.a.a.b() = " + byteBuffer);

            Class<?> bClass = Class.forName("com.benqu.core.jni.WTJNI", false, a.getClass().getClassLoader());
            Method aMethod = bClass.getDeclaredMethod("w3t2", ByteBuffer.class);
            aMethod.setAccessible(true);
            aMethod.invoke(null, byteBuffer);
            Log.d(LOG_WEILIU, "vapp invoke com.benqu.core.jni.WTJNI.w3t2(ByteBuffer)");
        } catch (Exception e) {
            Log.d(LOG_WEILIU, "", e);
        }
    }

    private static void hookPackageManager() {
        final String LOG_WEILIU = "WEILIU_HOOK";
        try {
            final Object oldPM = FieldUtils.getField("android.app.ActivityThread", null, "sPackageManager");
            @SuppressLint("PrivateApi")
            Class iPM = Class.forName("android.content.pm.IPackageManager");
            Object newPM = Proxy.newProxyInstance(iPM.getClassLoader(), new Class[] {iPM}, new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
//                    String paramStr = objects != null && objects.length > 0 ? TextUtils.join(", ", objects) : "";
//                    Log.d(LOG_WEILIU, "invoke: " + method.getName() + "(" + paramStr + ")");
                    Object result = method.invoke(oldPM, objects);

                    if (TextUtils.equals(method.getName(), "getPackageInfo")
                            && TextUtils.equals(objects[0].toString(), "com.benqu.wuta")
                            && (((int) objects[1] & PackageManager.GET_SIGNATURES) != 0)) {
                        Log.d(LOG_WEILIU, "try to get signatures, fix it");
                        PackageInfo packageInfo = (PackageInfo) result;
                        packageInfo.signatures[0] = new Signature(Base64.decode("MIIDuTCCAqGgAwIBAgIEZvFSXjANBgkqhkiG9w0BAQsFADCBizEMMAoGA1UEBhMDMDg2MREwDwYDVQQIEwhTaGFuZ2hhaTERMA8GA1UEBxMIU2hhbmdoYWkxLTArBgNVBAoMJOS4iua1t+acrOi2o+e9kee7nOenkeaKgOaciemZkOWFrOWPuDESMBAGA1UECxMJQmVucXVtYXJrMRIwEAYDVQQDEwlCZW5xdW1hcmswIBcNMTYwNjE3MDM0NzE0WhgPMjExNjA1MjQwMzQ3MTRaMIGLMQwwCgYDVQQGEwMwODYxETAPBgNVBAgTCFNoYW5naGFpMREwDwYDVQQHEwhTaGFuZ2hhaTEtMCsGA1UECgwk5LiK5rW35pys6Laj572R57uc56eR5oqA5pyJ6ZmQ5YWs5Y+4MRIwEAYDVQQLEwlCZW5xdW1hcmsxEjAQBgNVBAMTCUJlbnF1bWFyazCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI8NywuVhf8kdAnQZAD5Ou1dWUTUji7zeWcHZBrGkrLpLBv/CjXFca4DL2KkAIHJQBnax6hljFXAtvsfjJ2r7gwKsvZ7YeVuz/Uzic7mwNkhE6BjM1FVMSZQoJxY8VvfsZeQPh3DI0POcs8+iY1eoNEdCUiFB+al5uiPw+rm8DpdcqrTsXRYCRwCUHAbSpUDQEqi34zhil2NoyEufsJO6AZwBFPTEHRb3KNyppv1QO7+N5NeDCAdQm4Rqudyj1RzYvdoTIYBvr5RA3OY/3i5PmmWynf4lOF9Kcr5Taa9D4UsOsdmimc/BfnqXI6dlWM8N8SDNoEqnKy6qe0hOoe5YyECAwEAAaMhMB8wHQYDVR0OBBYEFPeU2Bz2mPw6HCRkSWK/c7yr1cQmMA0GCSqGSIb3DQEBCwUAA4IBAQAPGiYkrboNVordrY014dYlUneKtuc88RI6HbahnpzrGD/vO0ymd3O7yrfyTkUrJt/L2NHyhDI52ywaVBovKSF2rX32JDc2IXUd06iES+1MSowUX1MiinW+mzxthJi0CsxMNvPH46G2/ixTFt/sEcuxtInBJVsVEYEG8mmJ7jz3mG5zzfwCEmweG8srB9V5t44qgjfZf60uGdrfqMK+HtQCiwfwdiSlQHTVnSHqysR7InoTBfk2sVYnlQDK1D/LQ7NoL6AYwZQ6XoI6XNSnXs4vba7LB6ENVfbuou8GLhfJnDGXJFdxrYyg4kyhIbgDuZb+cV7z0CowsYDuYNGmwCG9", 0));

                    }
//                    Log.d(LOG_WEILIU, "result: " + result);
                    return result;
                }
            });
            FieldUtils.setField("android.app.ActivityThread", null, "sPackageManager", newPM);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
