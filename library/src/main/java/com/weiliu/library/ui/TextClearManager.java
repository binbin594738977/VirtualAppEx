package com.weiliu.library.ui;

import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.weiliu.library.widget.TextWatcherImpl;

/**
 * 输入框里有内容时，清除按钮（一个小叉叉）显示，并且点击该按钮就能清除输入框里的内容。
 * Created by qumiao on 2016/5/9.
 */
public class TextClearManager {

    private EditText mEditText;
    private View mClearView;

    public TextClearManager(EditText editText, View clearView) {
        mEditText = editText;
        mClearView = clearView;

        initListeners();
    }

    public void updateClearViewState() {
        mClearView.setVisibility(TextUtils.isEmpty(mEditText.getText()) ? View.GONE : View.VISIBLE);
    }

    public void destroy() {

    }

    private void initListeners(){

        mEditText.addTextChangedListener(new TextWatcherImpl() {
            @Override
            public void afterTextChanged(Editable s) {
                updateClearViewState();
            }
        });
        mClearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.getText().clear();
            }
        });
    }
}
