package io.virtualapp.core;

import android.content.Context;

import com.weiliu.library.task.http.HttpParams;
import com.weiliu.library.util.ManifestMetaDataUtil;
import com.weiliu.library.util.NetUtil;
import com.weiliu.library.util.PhoneInfoUtil;

/**
 * 作者：
 * 日期：2017/6/7 11:54
 * 说明：
 */
public class BaseParams extends HttpParams {

    private static final int APP_ID = 1;

    private boolean mShouldAddCommonParams;

    private String mUrlType;
    private String mUrlEvent;

    public BaseParams(String urlType, String urlEvent) {
        this(null, urlType, urlEvent);
    }

    public BaseParams(HttpParams params) {
        this(params, null, null, true);
    }

    public BaseParams(HttpParams params, String urlType, String urlEvent) {
        this(params, urlType, urlEvent, true);
    }

    public BaseParams(HttpParams params, String urlType, String urlEvent,
                      boolean shouldAddCommonParams) {
        super(params);
        mShouldAddCommonParams = shouldAddCommonParams;
        mUrlType = urlType;
        mUrlEvent = urlEvent;

        if (mUrlType != null) {
            put("type", urlType, Type.GET);
        }
        if (mUrlEvent != null) {
            put("event", urlEvent, Type.GET);
        }
        if (mShouldAddCommonParams) {
            addCommonParams(mUrlType, mUrlEvent);
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        if (mShouldAddCommonParams) {
            addCommonParams(mUrlType, mUrlEvent);
        }
    }

    private void addCommonParams(String className, String funcName) {
        Context context = BaseApplication.app();
        put("uuid", PhoneInfoUtil.getDeviceId(context));
        put("channel", BaseApplication.getChannelName());
        put("network_type", NetUtil.getNetType(context));
        put("operator", PhoneInfoUtil.getOperateName(context));
        put("version_code", ManifestMetaDataUtil.getVersionCode(context));
        put("cersion_name", ManifestMetaDataUtil.getVersionName(context));
        put("platform", "1");
    }

    @Override
    protected boolean isParamServiceForCacheKey(String paramKey) {
        switch (paramKey) {
            case "v":
            case "version_code":
            case "cersion_name":
            case "network_type":
            case "operator":
            case "channel":
                return false;
        }
        return super.isParamServiceForCacheKey(paramKey);
    }
}
