package com.weiliu.library;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.weiliu.library.task.LoadFrame;
import com.weiliu.library.task.TaskData;
import com.weiliu.library.task.TaskGroupTag;
import com.weiliu.library.task.TaskStarter;

import java.util.ArrayList;
import java.util.List;


/**
 * http加载界面
 * Created by qumiaowin on 2016/7/27.
 */
public class HttpLoadLayout extends FrameLayout implements LoadFrame {

    private View mLoadView;
    private View mFailedView;
    private View mRetryView;

    private TaskStarter mTaskStarter;
    private List<? extends TaskData> mTaskDataList;
    private TaskGroupTag mRetryTaskTag;

    private Callback mCallback;

    private boolean mAutoHideContent = true;

    public HttpLoadLayout(Context context) {
        super(context);
        init(context);
    }

    public HttpLoadLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HttpLoadLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mLoadView = inflater.inflate(R.layout.load_view, this, false);
        addView(mLoadView);
        mFailedView = inflater.inflate(R.layout.load_failed_view, this, false);
        addView(mFailedView);
        mRetryView = inflater.inflate(R.layout.load_retry_view, this, false);
        addView(mRetryView);

        mLoadView.setVisibility(GONE);
        mFailedView.setVisibility(GONE);
        mRetryView.setVisibility(GONE);

        mRetryView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTaskStarter != null && mTaskDataList != null) {
                    mTaskStarter.startTask(mTaskDataList, HttpLoadLayout.this);
                }
            }
        });
    }

    public void setAutoHideContent(boolean autoHideContent) {
        mAutoHideContent = autoHideContent;
    }

    private void showViewHideOthers(View view) {
        mLoadView.setVisibility(view == mLoadView ? VISIBLE : GONE);
        mFailedView.setVisibility(view == mFailedView ? VISIBLE : GONE);
        mRetryView.setVisibility(view == mRetryView ? VISIBLE : GONE);

        ArrayList<View> contentViewList = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v != null && v != mLoadView && v != mFailedView && v != mRetryView) {
                contentViewList.add(v);
            }
        }

        if (view != null) {
            bringChildToFront(view);

            if (mAutoHideContent) {
                setViewListVisibility(contentViewList, GONE);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                requestLayout();
                invalidate();
            }
        } else {
            if (mAutoHideContent) {
                setViewListVisibility(contentViewList, VISIBLE);
            }
        }
    }

    private void setViewListVisibility(List<View> viewList, int visibility) {
        for (View view : viewList) {
            view.setVisibility(visibility);
        }
    }

    @Override
    public void showLoadingUI() {
        showViewHideOthers(mLoadView);
    }

    @Override
    public void showFailedUI() {
        showViewHideOthers(mFailedView);
    }

    @Override
    public void showRetryUI() {
        showViewHideOthers(mRetryView);
    }

    @Override
    public void hide() {
        showViewHideOthers(null);
    }

    @Override
    public boolean shouldFail(List<? extends TaskData> taskDatas) {
        if (mCallback != null) {
            return mCallback.shouldFail(taskDatas);
        }
        return false;
    }

    @Override
    public boolean shouldRetry(List<? extends TaskData> taskDatas) {
        if (mCallback != null) {
            return mCallback.shouldRetry(taskDatas);
        }
        return false;
    }

    @Override
    public void setRetryAction(
            TaskStarter taskStarter, List<? extends TaskData> taskDatas) {
        mTaskStarter = taskStarter;
        mTaskDataList = taskDatas;
    }

    @Override
    public void onRetry(TaskGroupTag retryTaskTag) {
        mRetryTaskTag = retryTaskTag;
    }

    public TaskGroupTag getRetryTaskTag() {
        return mRetryTaskTag;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        boolean shouldRetry(List<? extends TaskData> taskDataList);
        boolean shouldFail(List<? extends TaskData> taskDataList);
    }

}
