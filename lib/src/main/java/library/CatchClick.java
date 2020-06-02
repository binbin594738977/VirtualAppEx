package library;

import android.app.Activity;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import java.io.File;
import java.util.ArrayList;

public class CatchClick {

    public static void handle(Activity activity) {
        WeiliuLog.log(activity.getClass().getName() + ": 等待完成");
        ViewGroup mContentParent = (ViewGroup) activity.getWindow().getDecorView();
        analysisContentParent(mContentParent, activity);
        try {
            WindowManager windowManager = activity.getWindowManager();
            Object global = ClassUtil.getFieldValue(windowManager, "mGlobal");
            ArrayList<View> mViews = (ArrayList<View>) ClassUtil.getFieldValue(global, "mViews");
            for (View view : mViews) {
                analysisContentParent(view, activity);
            }
        } catch (Exception e) {
            WeiliuLog.log(e);
        }
    }

    /**
     * 解刨视图树
     */
    private static void analysisContentParent(View view, Activity activity) {
        if (view == null) return;

        final View.OnClickListener listener = getViewClickListener(view);
        //如果是它设置的监听器,才设置监听器
        if (listener != null && !(listener instanceof MyOnclickListener)) {
            if (listener.getClass().getName().equals("android.widget.AutoCompleteTextView$PassThroughClickListener")) {
                return;
            }

            view.setOnClickListener(new MyOnclickListener() {
                @Override
                public void onClick(View v) {
                    WeiliuLog.log("onClick viewClass = " + v.getClass().getName());
                    WeiliuLog.log("onClick listenerClass = " + listener.getClass().getName());
//                    if (listener.getClass().getName().equals("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI$3")) {
//                        WeiliuLog.log("哈哈哈，抓住你 QRCodeUI生成");
//                        clazzSave2CodeNote(Utility.getActivity(v.getContext()), 0.02, "测试");
//                    } else {
//                        listener.onClick(v);
//                    }
                    listener.onClick(v);
                }
            });
        }

        if (view instanceof AdapterView) {
            final AdapterView.OnItemClickListener onItemClickListener = getOnItemClickListener((AdapterView) view);
            if (onItemClickListener != null && !(onItemClickListener instanceof MyOnItemClickListener))
                ((AdapterView) view).setOnItemClickListener(new MyOnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        WeiliuLog.log("onItemClickListener: " + onItemClickListener.getClass().getName());
                        //设置页 关于微信 点击
                        onItemClickListener.onItemClick(parent, view, position, id);
                        if ("com.tencent.mm.ui.conversation.ConversationWithAppBrandListView$16".equals(
                                onItemClickListener.getClass().getName()
                        )) {
                            ArrayList<String> arrayList = new ArrayList<>();
                            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/相机/images/1527840777614.jpg");
                            arrayList.add(file.getAbsolutePath());
                        } else {
                        }
                    }
                });
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childAt = viewGroup.getChildAt(i);
                analysisContentParent(childAt, activity);
            }
        }
    }


    public static class MyOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }
    public static class MyOnclickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
        }
    }
    /**
     * 得到view的点击监听
     *
     * @param view
     * @return
     */
    public static View.OnClickListener getViewClickListener(View view) {
        try {
            Object mListenerInfo = ClassUtil.getFieldValue(view, "mListenerInfo");
            //mListenerInfo==null,说明没有设置监听
            if (mListenerInfo == null) return null;
            Object listener = ClassUtil.getFieldValue(mListenerInfo, "mOnClickListener");
            //如果是我们自己的监听器,返回null
            if (listener instanceof MyOnclickListener) return null;
            return (View.OnClickListener) listener;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return null;
        }
    }
    public static synchronized AdapterView.OnItemClickListener getOnItemClickListener(AdapterView adapterView) {
        try {
            Object listenerInfo = ClassUtil.getFieldValue(adapterView, "mOnItemClickListener");
            if (listenerInfo instanceof MyOnItemClickListener) return null;
            return (AdapterView.OnItemClickListener) listenerInfo;
        } catch (Exception e) {
            WeiliuLog.log(e);
            return null;
        }
    }

}
