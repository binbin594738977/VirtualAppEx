package com.weiliu.library.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.weiliu.library.R;

/**
 * 根据高宽比来决定高度的ImageView。
 * 由于该View多用于List中显示大图，所以默认会在onDetachedFromWindow时回收BitmapDrawable
 * @author qumiao
 *
 */
public class RatioImageView extends RoundRectImageView {

    /**高宽比，如果为0，则按ImageView原本的行为来决定高度*/
    private float mRatio;

    /**是否在onDetachedFromWindow时保留Bitmap。否则将清除Bitmap，以便释放内存*/
    private boolean mRetainBitmap = true;

    public RatioImageView(Context context) {
        super(context);
    }

    public RatioImageView(@NonNull Context context, @NonNull AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RatioImageView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromAttributes(context, attrs);
    }

    private void initFromAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.RatioImageView);
        mRatio = array.getFloat(R.styleable.RatioImageView_ratio, 0);
        array.recycle();

        if (getRoundRadius() == -1) {
            setRoundRadius(0);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio != 0) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mRatio);
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 设置高宽比。
     * @param ratio 如果为0，则按RelativeLayout原本的行为来决定高度；否则，高度等于宽度乘以这个值。
     */
    public void setRatio(float ratio) {
        mRatio = ratio;
        requestLayout();
    }

    /**
     * 是否在onDetachedFromWindow时保留Bitmap。否则将清除Bitmap，以便释放内存
     * @param retainBitmap
     */
    public void setRetainBitmap(boolean retainBitmap) {
        mRetainBitmap = retainBitmap;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!mRetainBitmap) {
            setImageDrawable(null);
        }
    }
}
