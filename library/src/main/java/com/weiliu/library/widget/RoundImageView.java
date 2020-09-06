package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.weiliu.library.R;
import com.weiliu.library.util.Utility;

public class RoundImageView extends android.support.v7.widget.AppCompatImageView {

    private Paint mMaskPaint;
    private Paint mPaint;

    @ColorInt
    private int mRingColor;

    private int mRingRadius;

    public RoundImageView(Context context) {
        super(context);
        init(context, null);
    }

    public RoundImageView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundImageView(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mMaskPaint = new Paint();
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setAntiAlias(true);
        mMaskPaint.setFilterBitmap(true);
        mMaskPaint.setDither(true);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);

        if (attrs == null) {
            return;
        }

        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.RoundImageView,
                    0, 0);
            mRingRadius = a.getDimensionPixelSize(R.styleable.RoundImageView_ringRadius, mRingRadius);
            mRingColor = a.getColor(R.styleable.RoundImageView_ringColor, mRingColor);
        }
    }

    public int getRingColor() {
        return mRingColor;
    }

    public void setRingColor(int ringColor) {
        this.mRingColor = ringColor;
        invalidate();
    }

    public int getRingRadius() {
        return mRingRadius;
    }

    public void setRingRadius(int ringRadius) {
        this.mRingRadius = ringRadius;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        Drawable drawable = getDrawable();

        if (drawable == null) {
            return;
        }

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Bitmap b;
        if (drawable instanceof BitmapDrawable) {
            b = ((BitmapDrawable) drawable).getBitmap();
        } else {
            b = Utility.convertDrawableToBitmapByCanvas(drawable,
                    getWidth() - getPaddingLeft() - getPaddingRight(),
                    getHeight() - getPaddingTop() - getPaddingBottom());
        }

        if (b == null) {
            return;
        }

        drawBitmapRound(canvas, b);
    }

    private void drawBitmapRound(Canvas canvas, Bitmap bitmap) {
        int saveCount = canvas.getSaveCount();
        canvas.save();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && getCropToPadding()) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                    scrollX + getRight() - getLeft() - getPaddingRight(),
                    scrollY + getBottom() - getTop() - getPaddingBottom());
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());

        Bitmap roundBitmap = getRoundBitmap(bitmap);

        Matrix matrix = getMatrix(roundBitmap);
        canvas.concat(matrix);

        canvas.drawBitmap(roundBitmap, 0, 0, null);

        float radius = roundBitmap.getWidth() / 2f;
        if (mRingRadius > 0) {
            mPaint.setStrokeWidth(mRingRadius);
            mPaint.setColor(mRingColor);
            canvas.drawCircle(radius + 1, radius + 1, radius - (mRingRadius / 2f) - 1, mPaint);
        }

        canvas.restoreToCount(saveCount);
    }

    public Bitmap getRoundBitmap(@NonNull Bitmap bmp) {
        int length = Math.min(bmp.getWidth(), bmp.getHeight());
        float radius = length / 2f;

        Bitmap output = Bitmap.createBitmap(length, length, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        int left = (bmp.getWidth() - length) >> 1;
        int right = left + length;
        int top = (bmp.getHeight() - length) >> 1;
        int bottom = top + length;
        final Rect src = new Rect(left, top, right, bottom);
        final Rect dst = new Rect(0, 0, length, length);

        canvas.drawARGB(0, 0, 0, 0);

        mMaskPaint.setXfermode(null);
        mMaskPaint.setColor(Color.WHITE);
        canvas.drawCircle(radius + 1, radius + 1, radius - 2, mMaskPaint);

        mMaskPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bmp, src, dst, mMaskPaint);
        return output;
    }


    private Matrix mMatrix = new Matrix();

    /**
     * 以下参考自ImageView的SCALE_TYPE的处理逻辑。
     * 不能直接在onDraw里setScaleType之类的，那样会触发invalidate，造成onDraw递归
     * @param roundBitmap
     * @return
     */
    private Matrix getMatrix(Bitmap roundBitmap) {
        float scale;
        float dx = 0, dy = 0;

        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

        int bwidth = roundBitmap.getWidth();
        int bheight = roundBitmap.getHeight();

        if (bwidth * vheight > vwidth * bheight) {
            scale = (float) vheight / (float) bheight;
            dx = (vwidth - bwidth * scale) * 0.5f;
        } else {
            scale = (float) vwidth / (float) bwidth;
            dy = (vheight - bheight * scale) * 0.5f;
        }

        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(Math.round(dx), Math.round(dy));
        return mMatrix;
    }
}