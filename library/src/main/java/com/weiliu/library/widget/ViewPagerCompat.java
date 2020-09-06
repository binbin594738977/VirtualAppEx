package com.weiliu.library.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.weiliu.library.util.ViewUtil;

/**
 * 解决如下问题：
 * ViewPager在Android4.0以下的ViewCompat.canScrollHorizontally误判，
 * 导致其无法准确预知是否有子View能横向滚动，
 * 从而造成ViewPager嵌套可横向滚动的子View时，非法屏蔽了子View的优先滚动事件。
 * @author qumiao
 *
 */
public class ViewPagerCompat extends android.support.v4.view.ViewPager {
	
	public ViewPagerCompat(Context context) {
        super(context);
    }

    public ViewPagerCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && ViewUtil.canScrollHorizontally(v, -dx);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }


 /*
 覆写断onTouchEvent 和 onInterceptTouchEvent 是为了修复下面的bug
 06-08 18:27:02.280: E/AndroidRuntime(32270): FATAL EXCEPTION: main
 06-08 18:27:02.280: E/AndroidRuntime(32270): java.lang.IllegalArgumentException: pointerIndex out of range
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.MotionEvent.nativeGetAxisValue(Native Method)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.MotionEvent.getX(MotionEvent.java:1981)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.support.v4.view.MotionEventCompatEclair.getX(MotionEventCompatEclair.java:32)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.support.v4.view.MotionEventCompat$EclairMotionEventVersionImpl.getX(MotionEventCompat.java:91)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.support.v4.view.MotionEventCompat.getX(MotionEventCompat.java:219)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.support.v4.view.ViewPager.onInterceptTouchEvent(ViewPager.java:1839)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:1935)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:2289)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2032)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:2289)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2032)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:2289)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2032)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:2289)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2032)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 com.android.internal.policy.impl.PhoneWindow$DecorView.superDispatchTouchEvent(PhoneWindow.java:2026)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 com.android.internal.policy.impl.PhoneWindow.superDispatchTouchEvent(PhoneWindow.java:1476)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.app.Activity.dispatchTouchEvent(Activity.java:2473)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 com.android.internal.policy.impl.PhoneWindow$DecorView.dispatchTouchEvent(PhoneWindow.java:1974)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.View.dispatchPointerEvent(View.java:7396)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewRootImpl.deliverPointerEvent(ViewRootImpl.java:3250)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewRootImpl.deliverInputEvent(ViewRootImpl.java:3195)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewRootImpl.doProcessInputEvents(ViewRootImpl.java:4237)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewRootImpl.enqueueInputEvent(ViewRootImpl.java:4216)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.view.ViewRootImpl$WindowInputEventReceiver.onInputEvent(ViewRootImpl.java:4308)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.InputEventReceiver.dispatchInputEvent(InputEventReceiver.java:171)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.InputEventReceiver.nativeConsumeBatchedInputEvents(Native Method)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.view.InputEventReceiver.consumeBatchedInputEvents(InputEventReceiver.java:163)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.ViewRootImpl.doConsumeBatchedInput(ViewRootImpl.java:4287)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.view.ViewRootImpl$ConsumeBatchedInputRunnable.run(ViewRootImpl.java:4327)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.Choreographer$CallbackRecord.run(Choreographer.java:725)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.Choreographer.doCallbacks(Choreographer.java:555)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.view.Choreographer.doFrame(Choreographer.java:523)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at
 android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:711)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.os.Handler.handleCallback(Handler.java:615)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.os.Handler.dispatchMessage(Handler.java:92)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.os.Looper.loop(Looper.java:137)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at android.app.ActivityThread.main(ActivityThread.java:4914)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at java.lang.reflect.Method.invokeNative(Native Method)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at java.lang.reflect.Method.invoke(Method.java:511)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:808)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:575)
 06-08 18:27:02.280: E/AndroidRuntime(32270): at dalvik.system.NativeStart.main(Native Method)*/

}
