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

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import baseandroid.sl.sdk.analytics.util.ChannelUtils;
import baseandroid.sl.sdk.analytics.util.SlDataUtils;
import baseandroid.sl.sdk.analytics.util.SlLog;


class ChannelDeepLink extends AbsDeepLink {

    ChannelDeepLink(Intent intent) {
        super(intent);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void parseDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        Uri uri = intent.getData();
        Set<String> parameterNames = uri.getQueryParameterNames();
        if (parameterNames != null && parameterNames.size() > 0) {
            Map<String, String> uriParams = new HashMap<>();
            for (String name : parameterNames) {
                String value = uri.getQueryParameter(name);
                uriParams.put(name, TextUtils.isEmpty(value) ? "" : value);
            }
            ChannelUtils.parseParams(uriParams);
            if (mCallBack != null) {
                mCallBack.onFinish(DeepLinkManager.DeepLinkType.CHANNEL, null, true, 0);
            }
        }
    }

    @Override
    public void mergeDeepLinkProperty(JSONObject properties) {
        try {
            properties.put("$deeplink_url", getDeepLinkUrl());
        } catch (JSONException e) {
            SlLog.printStackTrace(e);
        }
        SlDataUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
    }
}
