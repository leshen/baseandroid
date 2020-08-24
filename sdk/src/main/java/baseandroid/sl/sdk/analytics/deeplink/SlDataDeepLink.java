/*
 * Created by chenru on 2020/06/30.
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

package baseandroid.sl.sdk.analytics.deeplink;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import baseandroid.sl.sdk.analytics.ServerUrl;
import baseandroid.sl.sdk.analytics.SlDataAPI;
import baseandroid.sl.sdk.analytics.network.HttpCallback;
import baseandroid.sl.sdk.analytics.network.HttpMethod;
import baseandroid.sl.sdk.analytics.network.RequestHelper;
import baseandroid.sl.sdk.analytics.util.ChannelUtils;
import baseandroid.sl.sdk.analytics.util.JSONUtils;
import baseandroid.sl.sdk.analytics.util.SlDataUtils;
import baseandroid.sl.sdk.analytics.util.SlLog;

class SlDataDeepLink extends AbsDeepLink {
    private String serverUrl;
    private String project;
    private String pageParams;
    private String errorMsg;
    private boolean success;

    public SlDataDeepLink(Intent intent, String serverUrl) {
        super(intent);
        this.serverUrl = serverUrl;
        project = new ServerUrl(serverUrl).getProject();
    }

    @Override
    public void parseDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        Uri uri = intent.getData();
        String key = uri.getLastPathSegment();
        if (!TextUtils.isEmpty(key)) {
            final long requestDeepLinkStartTime = System.currentTimeMillis();
            final Map<String, String> params = new HashMap<>();
            params.put("key", key);
            params.put("system_type", "ANDROID");
            params.put("project", project);
            new RequestHelper.Builder(HttpMethod.GET, getRequestUrl())
                    .params(params)
                    .callback(new HttpCallback.JsonCallback() {
                        @Override
                        public void onFailure(int code, String errorMessage) {
                            errorMsg = errorMessage;
                            success = false;
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            if (response != null) {
                                success = true;
                                JSONObject channel = response.optJSONObject("channel_params");
                                Map<String, String> params = JSONUtils.json2Map(channel);
                                ChannelUtils.parseParams(params);
                                pageParams = response.optString("page_params");
                                errorMsg = response.optString("errorMsg");
                                if (!TextUtils.isEmpty(errorMsg)) {
                                    success = false;
                                }
                            } else {
                                success = false;
                            }
                        }

                        @Override
                        public void onAfter() {
                            long duration = System.currentTimeMillis() - requestDeepLinkStartTime;
                            JSONObject properties = new JSONObject();
                            try {
                                if (!TextUtils.isEmpty(pageParams)) {
                                    properties.put("$deeplink_options", pageParams);
                                }
                                if (!TextUtils.isEmpty(errorMsg)) {
                                    properties.put("$deeplink_match_fail_reason", errorMsg);
                                }
                                properties.put("$deeplink_url", getDeepLinkUrl());
                                properties.put("$event_duration", String.format(Locale.CHINA, "%.3f", duration / 1000.0f));
                            } catch (JSONException e) {
                                SlLog.printStackTrace(e);
                            }
                            SlDataUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
                            if (mCallBack != null) {
                                mCallBack.onFinish(DeepLinkManager.DeepLinkType.SLDATA, pageParams, success, duration);
                            }
                            SlDataAPI.sharedInstance().track("$AppDeeplinkMatchedResult", properties);
                        }
                    }).execute();
        }
    }

    @Override
    public void mergeDeepLinkProperty(JSONObject properties) {
        try {
            properties.put("$deeplink_url", getDeepLinkUrl());
       } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
    }

    public String getRequestUrl() {
        if (!TextUtils.isEmpty(serverUrl)) {
            int pathPrefix = serverUrl.lastIndexOf("/");
            if (pathPrefix != -1) {
                return serverUrl.substring(0, pathPrefix) + "/sdk/deeplink/param";
            }
        }
        return "";
    }
}
