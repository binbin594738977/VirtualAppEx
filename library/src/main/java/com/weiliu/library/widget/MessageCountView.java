package com.weiliu.library.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.weiliu.library.R;

/**
 * 显示消息数的圆形背景View。
 * 该View会根据当前设置的消息字串，自动调整字体，以保证字串不会画在圆形背景之外。
 *
 * Created by qumiaowin on 2016/6/2.
 */
public class MessageCountView extends View {

    /**文字与圆边的最小距离*/
    private static float MIN_DIS = 3.f;

    private float mMinDis;

    private Paint mPaint;

    @ColorInt
    private int mBackgroundColor;

    @ColorInt
    private int mBoundColor;

    private int mBoundSize;

    @ColorInt
    private int mTextColor;

    private int mTextSize;

    private CharSequence mText;

    public MessageCountView(Context context) {
        super(context);
        init(context, null);
    }

    public MessageCountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MessageCountView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mMinDis = MIN_DIS * context.getResources().getDisplayMetrics().density;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        Resources res = context.getResources();
        mBackgroundColor = Color.RED;
        mTextColor = Color.WHITE;
        mTextSize = res.getDimensionPixelSize(R.dimen.default_text_size);

        if (attrs == null) {
            return;
        }

        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.MessageCountView,
                    0, 0);
            mBackgroundColor = a.getColor(
                    R.styleable.MessageCountView_backgroundColor, mBackgroundColor);
            mBoundColor = a.getColor(
                    R.styleable.MessageCountView_boundColor, mBoundColor);
            mBoundSize = a.getDimensionPixelSize(
                    R.styleable.MessageCountView_boundSize, mBoundSize);
            mTextColor = a.getColor(
                    R.styleable.MessageCountView_textColor, mTextColor);
            mTextSize = a.getDimensionPixelSize(
                    R.styleable.MessageCountView_textSize, mTextSize);
            mText = a.getText(
                    R.styleable.MessageCountView_text);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);

        // 圆的直径
        int rLength = (int) (getFitRadiusByTextSize(mText) * 2f);

        int width;
        int height;

        switch (wMode) {
            case MeasureSpec.AT_MOST:
                width = Math.min(wSize, rLength);
                if (hMode == MeasureSpec.AT_MOST) {
                    height = Math.min(hSize, width);
                    //noinspection SuspiciousNameCombination
                    width = height;
                } else if (hMode == MeasureSpec.EXACTLY) {
                    height = hSize;
                    width = Math.min(width, height);
                } else {
                    //noinspection SuspiciousNameCombination
                    height = width;
                }
                break;

            case MeasureSpec.EXACTLY:
                width = wSize;
                if (hMode == MeasureSpec.AT_MOST) {
                    height = Math.min(hSize, width);
                } else if (hMode == MeasureSpec.EXACTLY) {
                    height = hSize;
                } else {
                    //noinspection SuspiciousNameCombination
                    height = width;
                }
                break;

            case MeasureSpec.UNSPECIFIED:
            default:
                width = rLength;
                if (hMode == MeasureSpec.AT_MOST) {
                    height = Math.min(hSize, width);
                    //noinspection SuspiciousNameCombination
                    width = height;
                } else if (hMode == MeasureSpec.EXACTLY) {
                    height = hSize;
                    //noinspection SuspiciousNameCombination
                    width = height;
                } else {
                    //noinspection SuspiciousNameCombination
                    height = width;
                }
                break;
        }

        setMeasuredDimension(width, height);
    }

    private float getFitRadiusByTextSize(final CharSequence text) {
        mPaint.setTextSize(mTextSize);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float fontHeight = fontMetrics.bottom - fontMetrics.top;

        if (TextUtils.isEmpty(text)) {
            return fontHeight / 2.f + mMinDis;
        }

        float drawLength = mPaint.measureText(text, 0, text.length());
        float x = drawLength / 2.f;
        float y = fontHeight / 2.f;
        return (float) (Math.sqrt(x * x + y * y) + mMinDis);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        @SuppressLint("DrawAllocation")
        Rect rect = new Rect(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
        float x = rect.exactCenterX();
        float y = rect.exactCenterY();
        float radius = Math.min(x, y);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mBackgroundColor);
        canvas.drawCircle(x, y, radius, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mBoundColor);
        mPaint.setStrokeWidth(mBoundSize);
        canvas.drawCircle(x, y, radius - mBoundSize / 2f, mPaint);

        if (TextUtils.isEmpty(mText)) {
            return;
        }

        setFitTextSize(mText, radius, mTextSize);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(mTextColor);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float textY = -fontMetrics.top - (fontMetrics.bottom - fontMetrics.top) / 2;

        canvas.translate(x, y);
        canvas.drawText(mText, 0, mText.length(), 0, textY, mPaint);

        canvas.restore();
    }

    /**
     * 设置合适的字体，保证字串不会画在圆形区域以外，并离圆边至少距离 mMinDis 个像素
     * @param text
     * @param r
     * @param textSize
     */
    private void setFitTextSize(final CharSequence text, final float r, float textSize) {
        while (textSize > 1f) {
            mPaint.setTextSize(textSize);
            float drawLength = mPaint.measureText(text, 0, text.length());

            float x = drawLength / 2f;

            if (x + mMinDis > r) {
                textSize -= 1f;
                continue;
            }

            /*
            若圆心为原点，且字串矩形区域刚好与圆 外接（PS：这种情形刚好是字体临界值，再大就跑到圆外边了）：
            假设第一象限的外界点为（x, y），且该点与x轴的角度为 α
            则：
            x = r * cos(α)
            y = r * sin(α)

            于是：
            α = arccos(x/r)
            所以：
            y = r * sin( arccos(x/r) )
             */
            double a = Math.acos(x / r);
            float y = (float) (r * Math.sin(a));

            float fontHeight = mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;

            if (fontHeight / 2f > y + mMinDis) {
                textSize -= 1f;
                continue;
            }

            break;
        }
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(@ColorInt int textColor) {
        this.mTextColor = textColor;
        invalidate();
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int textSize) {
        this.mTextSize = textSize;
        invalidate();
    }

    /**
     * 设置消息数。
     * @param count 如果小于等于0，则将View设为GONE；否则，设为VISIBLE
     */
    public void setMessageCount(int count) {
        setText(String.valueOf(count));
        setVisibility(count == 0 ? GONE : VISIBLE);
    }

    /**
     * 设置消息数的字串形式。
     * @param countStr 为空或者为0，则将View设为GONE。
     */
    public void setMessageCount(String countStr) {
        if (!TextUtils.isEmpty(countStr)) {
            try {
                setMessageCount(Integer.parseInt(countStr));
            } catch (NumberFormatException e) {
                setText(countStr);
                setVisibility(VISIBLE);
            }
        } else {
            setText(countStr);
            setVisibility(GONE);
        }
    }

    private void setText(CharSequence text) {
        mText = text;
        invalidate();
    }
}
