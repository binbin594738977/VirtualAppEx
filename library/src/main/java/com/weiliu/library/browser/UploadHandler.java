/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.weiliu.library.browser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.weiliu.library.R;
import com.weiliu.library.util.FileUtil;

import java.io.File;

/**
 * Handle the file upload callbacks from WebView here.
 */
public class UploadHandler {

    public static final int FILE_SELECTED = 200;

	/** 用来通知WebView的文件上传的回调。 */
	private ValueCallback<Uri> mUploadMessage;

	/** 相机的相片存放目录。当使用相机临时拍摄来作选择文件时，需要用到这个目录。 */
	private String mCameraFilePath;

	/** 是否已经成功上传。 */
	private boolean mHandled;
	/** 如果没有任何Activity来响应选择文件的事件，则该值为true。 */
	private boolean mCaughtActivityNotFoundException;

	/** 该UploadHandler的Activity宿主。 */
	private Activity mActivity;
	private Fragment mFragment;

	/**
	 * 构造。
	 * 
	 * @param activity
	 *            Activity。
	 */
	public UploadHandler(Activity activity, Fragment fragment) {
		mActivity = activity;
        mFragment = fragment;
	}

	/**
	 * 获取相机的相片存放目录。当使用相机临时拍摄来作选择文件时，需要用到这个目录。
	 * 
	 * @return 目录名。
	 */
	@Nullable
    String getFilePath() {
		return mCameraFilePath;
	}

	/**
	 * 是否成功上传。
	 * 
	 * @return 如果成功上传，返回true。
	 */
	public boolean handled() {
		return mHandled;
	}

	/**
	 * 拦截Activity的onResult事件来处理选择文件之后的动作。
	 * 
	 * @param resultCode
	 *            Activity resultCode.
	 * @param intent
	 *            Intent.
	 */
	public void onResult(int resultCode, @Nullable Intent intent) {

		if (resultCode == Activity.RESULT_CANCELED && mCaughtActivityNotFoundException) {
			// Couldn't resolve an activity, we are going to try again so skip
			// this result.
			mCaughtActivityNotFoundException = false;
			return;
		}

		Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();

		// As we ask the camera to save the result of the user taking
		// a picture, the camera application does not return anything other
		// than RESULT_OK. So we need to check whether the file we expected
		// was written to disk in the in the case that we
		// did not get an intent returned but did get a RESULT_OK. If it was,
		// we assume that this result has came back from the camera.
		if (result == null && intent == null && resultCode == Activity.RESULT_OK) {
			File cameraFile = new File(mCameraFilePath);
			if (cameraFile.exists()) {
				result = Uri.fromFile(cameraFile);
				// Broadcast to the media scanner that we have a new photo
				// so it will be added into the gallery for the user.
				mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
			}
		}

		mUploadMessage.onReceiveValue(result);
		mHandled = true;
		mCaughtActivityNotFoundException = false;
	}

	/**
     * Tell the client to open a file chooser.
     * @param uploadMsg A ValueCallback to set the URI of the file to upload.
     *      onReceiveValue must be called to wake up the thread.a
     * @param acceptType The value of the 'accept' attribute of the input tag
     *         associated with this file picker.
     * @param capture The value of the 'capture' attribute of the input tag
     *         associated with this file picker.
     */
	public void openFileChooser(ValueCallback<Uri> uploadMsg, @NonNull String acceptType, @Nullable String capture) {

	    final String imageMimeType = "image/*";
        final String videoMimeType = "video/*";
        final String audioMimeType = "audio/*";
        final String mediaSourceKey = "capture";
        final String mediaSourceValueCamera = "camera";
        final String mediaSourceValueFileSystem = "filesystem";
        final String mediaSourceValueCamcorder = "camcorder";
        final String mediaSourceValueMicrophone = "microphone";

        // According to the spec, media source can be 'filesystem' or 'camera' or 'camcorder'
        // or 'microphone' and the default value should be 'filesystem'.
        String mediaSource = mediaSourceValueFileSystem;

        if (mUploadMessage != null) {
            // Already a file picker operation in progress.
            return;
        }

        mUploadMessage = uploadMsg;

        // Parse the accept type.
        String[] params = acceptType.split(";");
        String mimeType = params[0];

        if (capture != null) {
            if (capture.length() > 0) {
                mediaSource = capture;
            }

            if (capture.equals(mediaSourceValueFileSystem)) {
                // To maintain backwards compatibility with the previous implementation
                // of the media capture API, if the value of the 'capture' attribute is
                // "filesystem", we should examine the accept-type for a MIME type that
                // may specify a different capture value.
                for (String p : params) {
                    String[] keyValue = p.split("=");
                    if (keyValue.length == 2) {
                        // Process key=value parameters.
                        if (mediaSourceKey.equals(keyValue[0])) {
                            mediaSource = keyValue[1];
                        }
                    }
                }
            }
        }

        //Ensure it is not still set from a previous upload.
        mCameraFilePath = null;

		switch (mimeType) {
			case imageMimeType:
				if (mediaSource.equals(mediaSourceValueCamera)) {
					// Specified 'image/*' and requested the camera, so go ahead and launch the
					// camera directly.
					startActivity(createCameraIntent());
					return;
				} else {
					// Specified just 'image/*', capture=filesystem, or an invalid capture parameter.
					// In all these cases we show a traditional picker filetered on accept type
					// so launch an intent for both the Camera and image/* OPENABLE.
					Intent chooser = createChooserIntent(createCameraIntent());
					chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(imageMimeType));
					startActivity(chooser);
					return;
				}
			case videoMimeType:
				if (mediaSource.equals(mediaSourceValueCamcorder)) {
					// Specified 'video/*' and requested the camcorder, so go ahead and launch the
					// camcorder directly.
					startActivity(createCamcorderIntent());
					return;
				} else {
					// Specified just 'video/*', capture=filesystem or an invalid capture parameter.
					// In all these cases we show an intent for the traditional file picker, filtered
					// on accept type so launch an intent for both camcorder and video/* OPENABLE.
					Intent chooser = createChooserIntent(createCamcorderIntent());
					chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(videoMimeType));
					startActivity(chooser);
					return;
				}
			case audioMimeType:
				if (mediaSource.equals(mediaSourceValueMicrophone)) {
					// Specified 'audio/*' and requested microphone, so go ahead and launch the sound
					// recorder.
					startActivity(createSoundRecorderIntent());
					return;
				} else {
					// Specified just 'audio/*',  capture=filesystem of an invalid capture parameter.
					// In all these cases so go ahead and launch an intent for both the sound
					// recorder and audio/* OPENABLE.
					Intent chooser = createChooserIntent(createSoundRecorderIntent());
					chooser.putExtra(Intent.EXTRA_INTENT, createOpenableIntent(audioMimeType));
					startActivity(chooser);
					return;
				}

                default:
                    break;
		}

        // No special handling based on the accept type was necessary, so trigger the default
        // file upload chooser.
        startActivity(createDefaultOpenableIntent());
	}

	/**
	 * 安全启动响应文件选择事件的Activity。
	 * 
	 * @param intent
	 *            用来启动选择文件的Activity的Intent。
	 */
	private void startActivity(Intent intent) {
		try {
            if (mFragment != null) {
                mFragment.startActivityForResult(intent, FILE_SELECTED);
            } else {
                mActivity.startActivityForResult(intent, FILE_SELECTED);
            }
		} catch (ActivityNotFoundException e) {
			// No installed app was able to handle the intent that
			// we sent, so fallback to the default file upload control.
			try {
				mCaughtActivityNotFoundException = true;
                if (mFragment != null) {
                    mFragment.startActivityForResult(createDefaultOpenableIntent(), FILE_SELECTED);
                } else {
                    mActivity.startActivityForResult(createDefaultOpenableIntent(), FILE_SELECTED);
                }
			} catch (ActivityNotFoundException e2) {
				// Nothing can return us a file, so file upload is effectively disabled.
				Toast.makeText(mActivity, R.string.protocol_error, Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * 生成所有响应文件选择的intent.
	 * 
	 * @return Intent。
	 */
	@NonNull
	private Intent createDefaultOpenableIntent() {
		// Create and return a chooser with the default OPENABLE
		// actions including the camera, camcorder and sound
		// recorder where available.
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");

		Intent chooser = createChooserIntent(createCameraIntent(), createCamcorderIntent(),
				createSoundRecorderIntent());
		chooser.putExtra(Intent.EXTRA_INTENT, i);
		return chooser;
	}

	/**
	 * 生成指定的响应文件选择的intent.
	 * 
	 * @param intents
	 *            各种响应文件选择的intent。
	 * @return Intent。
	 */
	@NonNull
	private Intent createChooserIntent(Intent... intents) {
		Intent chooser = new Intent(Intent.ACTION_CHOOSER);
		chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
		chooser.putExtra(Intent.EXTRA_TITLE, mActivity.getResources().getString(R.string.choose_upload));
		return chooser;
	}

	/**
	 * 使用打开文件浏览器来响应文件选择。
	 * 
	 * @param type
	 *            文件类型。
	 * @return Intent.
	 */
	@NonNull
	private Intent createOpenableIntent(String type) {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType(type);
		return i;
	}

	/**
	 * 使用拍照来响应（图片类的）文件选择。
	 * 
	 * @return Intent.
	 */
	@NonNull
	private Intent createCameraIntent() {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File externalDataDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		File cameraDataDir = new File(externalDataDir.getAbsolutePath() + File.separator + "browser-photos");
        FileUtil.mkdirs(cameraDataDir);
        mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator + System.currentTimeMillis()
				+ ".jpg";
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCameraFilePath)));
		return cameraIntent;
	}

	/**
	 * 使用摄像来响应（视频类的）文件选择。
	 * 
	 * @return Intent.
	 */
	@NonNull
	private Intent createCamcorderIntent() {
		return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	}

	/**
	 * 使用录音来响应（音频类的）文件选择。
	 * 
	 * @return Intent.
	 */
	@NonNull
	private Intent createSoundRecorderIntent() {
		return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
	}

}
