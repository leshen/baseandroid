/*
 * Created by wangzhuozhou on 2017/4/10.
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

package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.data.PersistentLoader;

import java.util.concurrent.Future;

public class PersistentSessionIntervalTime extends PersistentIdentity<Integer> {
    public PersistentSessionIntervalTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, PersistentLoader.PersistentName.APP_SESSION_TIME, new PersistentSerializer<Integer>() {
            @Override
            public Integer load(String value) {
                return Integer.valueOf(value);
            }

            @Override
            public String save(Integer item) {
                return item == null ? "" : item.toString();
            }

            @Override
            public Integer create() {
                return 30 * 1000;
            }
        });
    }
}
