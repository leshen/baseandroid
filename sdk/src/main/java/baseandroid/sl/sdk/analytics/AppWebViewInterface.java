/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sl Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package baseandroid.sl.sdk.analytics;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import baseandroid.sl.sdk.analytics.util.SlLog;

/* package */ class AppWebViewInterface {
    private static final String TAG = "Sl.AppWebViewInterface";
    private Context mContext;
    private JSONObject properties;
    private boolean enableVerify;

    AppWebViewInterface(Context c, JSONObject p, boolean b) {
        this.mContext = c;
        this.properties = p;
        this.enableVerify = b;
    }

    @JavascriptInterface
    public String sldata_call_app() {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = SlDataAPI.sharedInstance(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
                properties.put("is_login", true);
            } else {
                properties.put("distinct_id", SlDataAPI.sharedInstance(mContext).getAnonymousId());
                properties.put("is_login", false);
            }
            return properties.toString();
        } catch (JSONException e) {
            SlLog.i(TAG, e.getMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void sldata_track(String event) {
        try {
            SlDataAPI.sharedInstance(mContext).trackEventFromH5(event, enableVerify);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @JavascriptInterface
    public boolean sldata_verify(String event) {
        try {
            if (!enableVerify) {
                sldata_track(event);
                return true;
            }
            return SlDataAPI.sharedInstance(mContext)._trackEventFromH5(event);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
            return false;
        }
    }

    @JavascriptInterface
    public String sldata_get_server_url() {
        return SlDataAPI.sharedInstance().getConfigOptions().isAutoTrackWebView ? SlDataAPI.sharedInstance().getServerUrl() : "";
    }

    /**
     * 解决用户只调用了 showUpWebView 方法时，此时 App 校验 url。JS 需要拿到 App 校验结果。
     *
     * @param event
     * @return
     */
    @JavascriptInterface
    public boolean sldata_visual_verify(String event) {
        try {
            if (!enableVerify) {
                return true;
            }
            if (TextUtils.isEmpty(event)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(event);
            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(SlDataAPI.sharedInstance().getServerUrl())))) {
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return false;
    }
}
