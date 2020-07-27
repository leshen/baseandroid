/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk;

import org.json.JSONObject;

public class SensorsDataSDKRemoteConfig {
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
    private int mAutoTrackEventType;

    public SensorsDataSDKRemoteConfig() {
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

    int getAutoTrackMode() {
        return autoTrackMode;
    }

    public void setAutoTrackMode(int autoTrackMode) {
        this.autoTrackMode = autoTrackMode;

        if (this.autoTrackMode == REMOTE_EVENT_TYPE_NO_USE || this.autoTrackMode == 0) {
            mAutoTrackEventType = 0;
            return;
        }

        if ((this.autoTrackMode & SensorsAnalyticsAutoTrackEventType.APP_START) == SensorsAnalyticsAutoTrackEventType.APP_START) {
            this.mAutoTrackEventType |= SensorsAnalyticsAutoTrackEventType.APP_START;
        }

        if ((this.autoTrackMode & SensorsAnalyticsAutoTrackEventType.APP_END) == SensorsAnalyticsAutoTrackEventType.APP_END) {
            this.mAutoTrackEventType |= SensorsAnalyticsAutoTrackEventType.APP_END;
        }

        if ((this.autoTrackMode & SensorsAnalyticsAutoTrackEventType.APP_CLICK) == SensorsAnalyticsAutoTrackEventType.APP_CLICK) {
            this.mAutoTrackEventType |= SensorsAnalyticsAutoTrackEventType.APP_CLICK;
        }

        if ((this.autoTrackMode & SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN) == SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN) {
            this.mAutoTrackEventType |= SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN;
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
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "{ v=" + v + ", disableDebugMode=" + disableDebugMode + ", disableSDK=" + disableSDK + ", autoTrackMode=" + autoTrackMode + "}";
    }
}
