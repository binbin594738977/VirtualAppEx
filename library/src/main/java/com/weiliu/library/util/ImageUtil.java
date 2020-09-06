package com.weiliu.library.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * 图片处理工具类 图片裁剪 加圆角 等等
 * 
 * @author lizhiyong<lizhiyong@haodou.com>
 * 
 *         $Id$
 * 
 */
public class ImageUtil {

	private static final int BUFF_SIZE = 8192;

	private static final int QUALITY = 100;

	private ImageUtil() {

	}

	/***
	 * 测试拍照后的图片是正常的还是旋转的
	 * 
	 * @param filepath
	 * @return
	 */
	public static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (exif != null) {
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90; // SUPPRESS CHECKSTYLE
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180; // SUPPRESS CHECKSTYLE
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270; // SUPPRESS CHECKSTYLE
					break;
				default:
					break;
				}
			}
		}
		return degree;
	}

	/**
	 * 
	 * 旋转图片到目标路径
	 * 
	 * @param filePath
	 * @param degree
	 * @return 是否旋转成功
	 */
	public static boolean rotatePhoto(@NonNull String filePath, int degree) {
		boolean result = true;
		if (degree != 0) {
			try {
				Bitmap bitmap = BitmapFactory.decodeFile(filePath);
				int relwidth = bitmap.getWidth();
				int relheight = bitmap.getHeight();
				Matrix matrix = new Matrix();
				matrix.postRotate(degree);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, relwidth, relheight, matrix, true);
				image2File(filePath, bitmap, null, 0);
				bitmap.recycle();

			} catch (OutOfMemoryError e) {
				result = false;
				e.printStackTrace();
			} catch (Exception e) {
				result = false;
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * 
	 * 对Bitmap进行角度旋转
	 * 
	 * @param src
	 * @param degree
	 * @return 旋转后的Bitmap，失败则返回null
	 */
	@Nullable
	public static Bitmap rotatePhoto(@Nullable Bitmap src, int degree) {
		if (src == null || degree == 0) {
			return src;
		}

		try {
			int relwidth = src.getWidth();
			int relheight = src.getHeight();
			Matrix matrix = new Matrix();
			matrix.postRotate(degree);
			return Bitmap.createBitmap(src, 0, 0, relwidth, relheight, matrix, true);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 
	 * 计算图片需要被压缩的比例
	 * 
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public static int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = Math.max(heightRatio, widthRatio);
		}

		if (((height / inSampleSize) < reqHeight) || ((width / inSampleSize) < reqWidth)) {
			inSampleSize--;
		}

		return inSampleSize > 0 ? inSampleSize : 1;
	}

	public static int calculateMinInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = Math.min(heightRatio, widthRatio);
		}

		return inSampleSize;
	}

	/** 图片上传限制在2M内，根据原图大小，计算出sampleSize*/
	public static int calculateInSampleSize(long size, long limitSize) {
		int inSampleSize = 1;
		if (limitSize >= size) {
			return inSampleSize;
		}
		double ratio = Math.ceil(Math.sqrt(size / (double) limitSize));
		inSampleSize = (int) Math.ceil(ratio);
		return inSampleSize;
	}

	/**
	 * 
	 * 从源路径生成一张缩略图到目标路径 没有对图片是否存在做效验
	 * 
	 * @param src
	 *            源路径
	 * @param dst
	 *            目标路径
	 * @param w
	 *            缩略图宽
	 * @param h
	 *            缩略图高
	 * @param square
	 *            是否裁为正方形，此值为true时才会严格按照设置的宽高裁剪，否则只是等比例计算裁剪
	 * @return 是否成功创建缩略图到目标路径
	 */
	public static boolean createThumb(String src, @NonNull String dst, int w, int h, boolean square) {
		boolean result = true;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(src, options);
		if (square) {
			options.inSampleSize = ImageUtil.calculateMinInSampleSize(options, w, h);
		} else {
			options.inSampleSize = ImageUtil.calculateInSampleSize(options, w, h);
		}
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(src, options);

		if (bitmap == null) {
			return false;
		} else {
			if (square) {
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h);
			}
			result = image2File(dst, bitmap, null, 0);
			if (bitmap != null && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
		}

		return result;
	}

	/**
	 * 
	 * 从源路径生成一张缩略图，并返回该图的Bitmap引用 没有对图片是否存在做效验
	 * 
	 * @param src
	 *            源路径
	 * @param w
	 *            缩略图宽
	 * @param h
	 *            缩略图高
	 * @param square
	 *            是否裁为正方形，此值为true时才会严格按照设置的宽高裁剪，否则只是等比例计算裁剪
	 * @return 该图的Bitmap引用
	 */
	public static Bitmap createThumb(String src, int w, int h, boolean square) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(src, options);
		if (square) {
			options.inSampleSize = ImageUtil.calculateMinInSampleSize(options, w, h);
		} else {
			options.inSampleSize = ImageUtil.calculateInSampleSize(options, w, h);
		}
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(src, options);

		if (square && bitmap != null) {
			Bitmap oldBitmap = bitmap;
			bitmap = Bitmap.createBitmap(oldBitmap, 0, 0, w, h);
			if (bitmap != null && bitmap != oldBitmap) {
				oldBitmap.recycle();
			}
		}

		return bitmap;
	}

	/**
	 * 
	 * drawable 转 Bitmap
	 * 
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawable2Bitmap(Drawable drawable) {
		BitmapDrawable bd = (BitmapDrawable) drawable;
		return bd.getBitmap();
	}

	/**
	 * bitmap 转 drawable
	 * 
	 * @param bmp
	 * @return
	 */
	@NonNull
	public static Drawable bitmap2Drawable(Bitmap bmp) {
		return new BitmapDrawable(bmp);
	}

	/**
	 * 
	 * byte转Bitmap
	 * 
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static Bitmap createBmpFromBytes(@NonNull byte[] bytes) {
		Bitmap bmp = null;

		if (bytes.length > 0) {
			bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		}

		return bmp;
	}

	/**
	 * 从路径得到一个图片
	 */
	public static Bitmap createBmpFromPath(String path, int sampSize) {

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sampSize;
		return BitmapFactory.decodeFile(path, opts);

	}

	/**
	 * byte转drawable
	 * 
	 * @param bytes
	 * @return
	 */
	@Nullable
	public static Drawable createDrawableFromBytes(@NonNull byte[] bytes) {
		Drawable drawable = null;
		drawable = bitmap2Drawable(createBmpFromBytes(bytes));
		return drawable;
	}

	/**
	 * 保存图片至SD卡
	 * 
	 * @param file
	 * @param bmp
	 * @return
	 */
	public static boolean image2File(@NonNull String file, Bitmap bmp, String type) {
		return image2File(file, bmp, type, 0);
	}

	/**
	 * 保存图片至SD卡
	 * 
	 * @param file
	 * @param bmp
	 * @param type
	 *            类型："jpg", "png"。如果type为null，则从file中获取后缀。默认为"png"
	 * @param maxSize
	 *            保存后文件的最大size。如果为0表示不限；否则，如果文件的size超过该值则继续折半压缩，直到满足为止。
	 * @return
	 */
	public static boolean image2File(@NonNull String file, Bitmap bmp, @Nullable String type, long maxSize) {
		if (type == null) {
			type = FileUtil.getExtensionName(file);
		}

		float scale = 0.f;
		boolean flag;

		do {
			flag = false;

			if (scale != 0.f) {
				Matrix m = new Matrix();
				m.postScale(scale, scale);
				bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
			}

			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(new FileOutputStream(file), BUFF_SIZE);
				if ("jpg".equalsIgnoreCase(type)) {
					bmp.compress(Bitmap.CompressFormat.JPEG, QUALITY, bos);
				} else {
					bmp.compress(Bitmap.CompressFormat.PNG, QUALITY, bos);
				}
				bos.close();
				flag = true;
			} catch (Exception e) {
				e.printStackTrace();
				break;
			} finally {
				Utility.close(bos);
			}

			long fileSize = new File(file).length();
			scale = fileSize != 0 ? (float) (maxSize / (double) fileSize) : 0;

		} while (maxSize != 0 && scale > 0.f && scale < 1.f);

		return flag;
	}

	/**
	 * 将Bitmap转换到int数组
	 * 
	 * @param bitmap
	 * @return
	 */
	@Nullable
	public static int[] bitmap2IntARGB(@NonNull Bitmap bitmap) {
		int[] arryInt = null;
		try {
			int i = bitmap.getWidth();
			int j = bitmap.getHeight();
			arryInt = new int[i * j];
			bitmap.getPixels(arryInt, 0, i, 0, 0, i, j);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return arryInt;
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private int getBitmapSize(@NonNull Bitmap bitmap) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // API 19
			return bitmap.getAllocationByteCount();
		}
		return bitmap.getRowBytes() * bitmap.getHeight();  // earlier version
	}

}
