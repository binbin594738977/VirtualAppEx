package com.weiliu.library.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * 以Dialog形式展示进度并管理
 * Created by qumiao on 2016/5/4.
 */
public class TaskProgressDialog implements TaskProgress {

    private int mTotalWeight;
    private Context mContext;

    private ProgressDialog mDialog;
    private boolean mCancelable;
    private boolean mCanceledOnTouchOutside;
    private CharSequence mMessage;
    private boolean mIndeterminate;
    private int mProgress;
    private int mProgressStyle;
    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;

    public TaskProgressDialog(Context context) {
        mContext = context;
    }

    public void setCancelable(boolean cancelable) {
        mCancelable = cancelable;
    }

    public void setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        mCanceledOnTouchOutside = canceledOnTouchOutside;
    }

    public void setMessage(CharSequence message) {
        mMessage = message;
    }

    public void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
    }

    public void setProgressStyle(int progressStyle) {
        mProgressStyle = progressStyle;
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    @Override
    public void show() {
        if (isShowing()) {
            mDialog.dismiss();
        }

        mDialog = new ProgressDialog(mContext);
        mDialog.setCancelable(mCancelable);
        mDialog.setCanceledOnTouchOutside(mCanceledOnTouchOutside);
        mDialog.setMessage(mMessage);
        mDialog.setIndeterminate(mIndeterminate);
        mDialog.setProgress(mProgress);
        mDialog.setProgressStyle(mProgressStyle);
        mDialog.setOnCancelListener(mOnCancelListener);
        mDialog.setOnDismissListener(mOnDismissListener);
        mDialog.show();
    }

    @Override
    public void setProgress(int progress) {
        mProgress = progress;
        if (isShowing()) {
            mDialog.setProgress(progress);
        }
    }

    @Override
    public int getProgress() {
        return mProgress;
    }

    @Override
    public void setTotalWeight(int totalWeight) {
        mTotalWeight = totalWeight;
    }

    @Override
    public int getTotalWeight() {
        return mTotalWeight;
    }

    @Override
    public void hide() {
        if (isShowing()) {
            mDialog.dismiss();
        }
    }
}
