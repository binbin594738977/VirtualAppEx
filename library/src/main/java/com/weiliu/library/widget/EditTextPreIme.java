package com.weiliu.library.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import com.weiliu.library.util.Utility;

public class EditTextPreIme extends android.support.v7.widget.AppCompatEditText {
    public EditTextPreIme(Context context) {
        super(context);
    }

    public EditTextPreIme(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public EditTextPreIme(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Activity activity = Utility.getActivity(getContext());
            if (activity != null) {
//                activity.onKeyDown(KeyEvent.KEYCODE_BACK, event);
                Utility.hideInputMethod(activity, this);
                activity.finish();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}