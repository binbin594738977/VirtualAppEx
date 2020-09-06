package com.weiliu.library.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;

import com.weiliu.library.RootApplication;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class PhotoUtil {
    public static final int TAKE_PHOTO_FROM_CAMERA = 1000;
    public static final int PICK_PHOTO_FROM_GALLERY = 1001;
    public static final int CROP_PHOTO = 1002;
    public static final String IMAGE_TYPE = "image/*";

    public static final String ACTION_CROP = "com.android.camera.action.CROP";

    private PhotoUtil() {
        
    }

    /***
     * 拍照或是旋转图片的时候需要生成一张新的图片，临时保存的缓存目录里面
     * @return
     */
    public static String getTempPhotoPath() {
        String dir = getTempPhotoDir();
        FileUtil.createDirIfMissed(dir);
        return dir + File.separator + System.currentTimeMillis() + ".jpg";
    }

    public static String getTempPhotoDir() {
        File dir = new File(RootApplication.getInstance().getCacheDir(), "photo_dir");
        FileUtil.createDirIfMissed(dir.getAbsolutePath());
        return dir.getAbsolutePath();
    }

    public static String getScreenshotDir() {
        File dir = new File(RootApplication.getInstance().getCacheDir(), "screenshot");
        FileUtil.createDirIfMissed(dir.getAbsolutePath());
        return dir.getAbsolutePath();
    }

    public static String getScreenshotPath() {
        String dir = getScreenshotDir();
        FileUtil.createDirIfMissed(dir);
        return dir + File.separator + System.currentTimeMillis() + ".jpg";
    }

    public static String getImagePath(Context context, Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = context.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null) {
            return "";
        }
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String imagePath = cursor.getString(columnIndex);
        cursor.close();
        return imagePath;
    }


    /**
     * 从图片库中获取图片
     * @param context
     * @param result 从图片库中选取图片后的activity result intent
     * @param listener 选取并拷贝成功后的回调
     */
    public static void getPhotoFromStorageResult(@NonNull Context context, @NonNull Intent result,
                                                 @Nullable OnPickPhotoListener listener) {
        Uri uri = result.getData();
        if (uri != null) {
            String dir = getTempPhotoDir();
            FileUtil.createDirIfMissed(dir);
            File dstFile = new File(dir, Md5Util.MD5Encode(uri.toString()) + ".jpg");
            try {
//                dstFile.createNewFile();
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null && Utility.streamToFile(inputStream, dstFile)) {
                    if (listener != null) {
                        listener.onPickPhoto(dstFile);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 启动相机获取照片
     * @param context 如果fragment为null，则使用startActivityForResult
     * @param fragment 如果不为null，则使用fragment.startActivityForResult
     * @return 用来接收照片的文件
     */
    public static File openCamera(Context context, Fragment fragment) {
        File file = new File(PhotoUtil.getTempPhotoPath());

        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);

        Uri outputUri = getUriFromFile(file);
        grantUriPermission(intent, outputUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        startActivityForResult(context, fragment, intent, TAKE_PHOTO_FROM_CAMERA);
        return file;
    }

    /**
     * 从相册上获取图片
     * @param context 如果fragment为null，则使用startActivityForResult
     * @param fragment 如果不为null，则使用fragment.startActivityForResult
     */
    public static void openGallery(Context context, Fragment fragment) {
        Intent intentPick = new Intent(Intent.ACTION_GET_CONTENT);
        intentPick.setType("image/*");
        intentPick.addCategory(Intent.CATEGORY_OPENABLE);
        int action = PICK_PHOTO_FROM_GALLERY;
        startActivityForResult(context, fragment, intentPick, action);
    }

    /**
     * 启动图片裁剪
     * @param context 如果fragment为null，则使用startActivityForResult
     * @param fragment 如果不为null，则使用fragment.startActivityForResult
     * @param imageFile 源图片路径
     * @param dstFile 裁剪后的图片路径
     */
    public static void cropPhoto(Context context, Fragment fragment, File imageFile, String dstFile) {
        Intent intent = new Intent(ACTION_CROP);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (imageFile == null) {
            return;
        }

        FileUtil.parentFolder(dstFile);

        final int x = 500;
        final int y = 500;

        intent.setDataAndType(getUriFromFile(imageFile), IMAGE_TYPE);
        intent.putExtra("crop", true); // crop=true 有这句才能出来最后的裁剪页面.
        intent.putExtra("scale", true);
        Uri outputUri = getUriFromFile(new File(dstFile));
        grantUriPermission(intent, outputUri);
        intent.putExtra("output", outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()); //返回格式

        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        intent.putExtra("outputX", x);
        intent.putExtra("outputY", y);
        intent.putExtra("return-data", false);

        startActivityForResult(context, fragment, intent, CROP_PHOTO);
    }

    private static Uri getUriFromFile(File file) {
        Context context = RootApplication.getInstance();
        String authority = context.getPackageName() + ".provider";
        return FileProvider.getUriForFile(context, authority, file);
    }

    private static void grantUriPermission(Intent intent, Uri uri) {
        Context context = RootApplication.getInstance();
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private static void startActivityForResult(
            Context context, Fragment fragment, Intent intent, int requestCode) {
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            Activity activity = Utility.getActivity(context);
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public interface OnPickPhotoListener {
        void onPickPhoto(File file);
    }
}
