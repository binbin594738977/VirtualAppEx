package com.weiliu.library.browser;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.weiliu.library.R;

/**
 * 地理定位设置管理类。
 * */
public class GeolocationPermissionsPrompt extends LinearLayout {
    /**地理定位设置框。*/
    private LinearLayout mInner;
    /**信息提示。*/
    private TextView mMessage;
    /**启动共享位置信息的按钮。*/
    private Button mShareButton;
    /**拒绝共享位置信息的按钮。*/
    private Button mDontShareButton;
    /**记住本次设定。*/
    private CheckBox mRemember;
    /**管理定位权限的回调。*/
    private GeolocationPermissions.Callback mCallback;
    /**URL信息。*/
    private String mOrigin;

    /**
     * 地理定位设置管理类。
     * @param context Context
     * */
    public GeolocationPermissionsPrompt(Context context) {
        this(context, null);
    }

    /**
     * 地理定位设置管理类。
     * @param context Context
     * @param attrs AttributeSet
     * */
    public GeolocationPermissionsPrompt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 初始化。
     */
    /*package*/public void init() {
        mInner = (LinearLayout) findViewById(R.id.inner);
        mMessage = (TextView) findViewById(R.id.message);
        mShareButton = (Button) findViewById(R.id.share_button);
        mDontShareButton = (Button) findViewById(R.id.dont_share_button);
        mRemember = (CheckBox) findViewById(R.id.remember);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(true);
            }
        });
        mDontShareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(false);
            }
        });
    }

    /**
     * Shows the prompt for the given origin. When the user clicks on one of
     * the buttons, the supplied callback is be called.
     * @param origin URL
     * @param callback Callback
     */
    public void show(String origin, GeolocationPermissions.Callback callback) {
        mOrigin = origin;
        mCallback = callback;
        Uri uri = Uri.parse(mOrigin);
        String message = mOrigin;
        if ("http".equals(uri.getScheme())) {
            message = mOrigin.substring("http://".length());
        }
        setMessage(message);
//        // The checkbox should always be intially checked.
//        mRemember.setChecked(true);
        showDialog(true);
    }

    /**
     * Hides the prompt.
     */
    public void hide() {
        showDialog(false);
    }

    /**
     * Handles a click on one the buttons by invoking the callback.
     * @param allow If allowed to share geography location.
     */
    private void handleButtonClick(boolean allow) {
        showDialog(false);

        boolean remember = mRemember.isChecked();
        if (remember) {
            int resId;
            if (allow) {
                resId = R.string.geolocation_permissions_prompt_toast_allowed;
            } else {
                resId = R.string.geolocation_permissions_prompt_toast_disallowed;
            }

            Toast toast = Toast.makeText(getContext(), resId, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }

        mCallback.invoke(mOrigin, allow, remember);
    }

    /**
     * Sets the prompt's message.
     * @param origin Message text
     */
    private void setMessage(CharSequence origin) {
        mMessage.setText(String.format(
                getResources().getString(R.string.geolocation_permissions_prompt_message),
                origin));
    }

    /**
     * Shows or hides the prompt.
     * @param shown If showing dialog.
     */
    private void showDialog(boolean shown) {
        if (shown) {
            mInner.setVisibility(View.VISIBLE);
        } else {
            mInner.setVisibility(View.GONE);
        }
    }
}
