/*
 * Created by wangzhuozhou on 2017/4/10.
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

package baseandroid.sl.sdk.analytics.data.persistent;

import android.content.SharedPreferences;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

import baseandroid.sl.sdk.analytics.data.PersistentLoader;
import baseandroid.sl.sdk.analytics.util.SlLog;

public class PersistentSuperProperties extends PersistentIdentity<JSONObject> {
    public PersistentSuperProperties(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, PersistentLoader.PersistentName.SUPER_PROPERTIES, new PersistentSerializer<JSONObject>() {
            @Override
            public JSONObject load(String value) {
                try {
                    return new JSONObject(value);
                } catch (JSONException e) {
                    SlLog.d("Persistent", "failed to load SuperProperties from SharedPreferences.", e);
                    return new JSONObject();
                }
            }

            @Override
            public String save(JSONObject item) {
                return item == null ? create().toString() : item.toString();
            }

            @Override
            public JSONObject create() {
                return new JSONObject();
            }
        });
    }
}
