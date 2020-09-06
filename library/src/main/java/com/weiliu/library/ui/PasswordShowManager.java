package com.weiliu.library.ui;

import android.support.annotation.DrawableRes;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * 通过一个按钮来控制密码的显示与隐藏。
 * Created by qumiao on 2016/5/9.
 */
public class PasswordShowManager {

    private EditText mPasswordView;
    private ImageView mShowView;

    @DrawableRes
    private int mShowRes;
    @DrawableRes
    private int mHideRes;

    public PasswordShowManager(EditText passwordView, ImageView showView, @DrawableRes int show, @DrawableRes int hide) {
        mPasswordView = passwordView;
        mShowView = showView;
        mShowRes = show;
        mHideRes = hide;

        mShowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransformationMethod method = mPasswordView.getTransformationMethod();
                int cursorPos = mPasswordView.getSelectionStart();
                if (method instanceof PasswordTransformationMethod) {
                    mPasswordView.setTransformationMethod(null);
                } else {
                    mPasswordView.setTransformationMethod(new PasswordTransformationMethod());
                }
                // setTransformationMethod会使光标移到最开始，所以这里再恢复到原来位置
                mPasswordView.setSelection(cursorPos);
                updateShowViewState();
            }
        });
    }

    public void updateShowViewState() {
        TransformationMethod method = mPasswordView.getTransformationMethod();
        if (method instanceof PasswordTransformationMethod) {
            mShowView.setImageResource(mHideRes);
        } else {
            mShowView.setImageResource(mShowRes);
        }
    }

    public void destroy() {

    }

}
