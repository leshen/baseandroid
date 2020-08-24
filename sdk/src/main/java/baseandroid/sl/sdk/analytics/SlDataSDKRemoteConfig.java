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

import org.json.JSONObject;

import baseandroid.sl.sdk.analytics.util.SlLog;

public class SlDataSDKRemoteConfig {
    static final int REMOTE_EVENT_TYPE_NO_USE = -1;
    /**
     * config 版本号
     */
    private String v;
    /**
     * 是否关闭 debug 模式
     */
    private boolean disableDebugMode;
    /**
     * 是否关闭 AutoTrack
     */
    private int autoTrackMode;
    /**
     * 是否关闭 SDK
     */
    private boolean disableSDK;

    /**
     * RSA 公钥
     */
    private String rsaPublicKey;

    /**
     * 公钥版本名称
     */
    private int pkv;

    private int mAutoTrackEventType;

    public SlDataSDKRemoteConfig() {
        this.disableDebugMode = false;
        this.disableSDK = false;
        this.autoTrackMode = REMOTE_EVENT_TYPE_NO_USE;
    }

    String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    boolean isDisableDebugMode() {
        return disableDebugMode;
    }

    public void setDisableDebugMode(boolean disableDebugMode) {
        this.disableDebugMode = disableDebugMode;
    }

    boolean isDisableSDK() {
        return disableSDK;
    }

    public void setDisableSDK(boolean disableSDK) {
        this.disableSDK = disableSDK;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public int getPkv() {
        return pkv;
    }

    public void setPkv(int pkv) {
        this.pkv = pkv;
    }


    int getAutoTrackMode() {
        return autoTrackMode;
    }

    public void setAutoTrackMode(int autoTrackMode) {
        this.autoTrackMode = autoTrackMode;

        if (this.autoTrackMode == REMOTE_EVENT_TYPE_NO_USE || this.autoTrackMode == 0) {
            mAutoTrackEventType = 0;
            return;
        }

        if ((this.autoTrackMode & SlAnalyticsAutoTrackEventType.APP_START) == SlAnalyticsAutoTrackEventType.APP_START) {
            this.mAutoTrackEventType |= SlAnalyticsAutoTrackEventType.APP_START;
        }

        if ((this.autoTrackMode & SlAnalyticsAutoTrackEventType.APP_END) == SlAnalyticsAutoTrackEventType.APP_END) {
            this.mAutoTrackEventType |= SlAnalyticsAutoTrackEventType.APP_END;
        }

        if ((this.autoTrackMode & SlAnalyticsAutoTrackEventType.APP_CLICK) == SlAnalyticsAutoTrackEventType.APP_CLICK) {
            this.mAutoTrackEventType |= SlAnalyticsAutoTrackEventType.APP_CLICK;
        }

        if ((this.autoTrackMode & SlAnalyticsAutoTrackEventType.APP_VIEW_SCREEN) == SlAnalyticsAutoTrackEventType.APP_VIEW_SCREEN) {
            this.mAutoTrackEventType |= SlAnalyticsAutoTrackEventType.APP_VIEW_SCREEN;
        }
    }

    int getAutoTrackEventType() {
        return mAutoTrackEventType;
    }

    boolean isAutoTrackEventTypeIgnored(int eventType) {
        if (autoTrackMode == REMOTE_EVENT_TYPE_NO_USE) {
            return false;
        }

        if (autoTrackMode == 0) {
            return true;
        }

        return (mAutoTrackEventType | eventType) != mAutoTrackEventType;
    }

    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("v", v);
            JSONObject configObject = new JSONObject();
            configObject.put("disableDebugMode", disableDebugMode);
            configObject.put("autoTrackMode", autoTrackMode);
            configObject.put("disableSDK", disableSDK);
            jsonObject.put("configs", configObject);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "{ v=" + v + ", disableDebugMode=" + disableDebugMode + ", disableSDK=" + disableSDK + ", autoTrackMode=" + autoTrackMode + "}";
    }
}

