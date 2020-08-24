/*
 * Created by wangzhuozhou on 2019/02/01.
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

package baseandroid.sl.sdk.analytics.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.concurrent.Future;

import baseandroid.sl.sdk.analytics.data.persistent.PersistentAppEndData;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentAppPaused;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentAppStartTime;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentDistinctId;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstDay;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstStart;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstTrackInstallation;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstTrackInstallationWithCallback;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentIdentity;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentLoginId;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentRemoteSDKConfig;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentSessionIntervalTime;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentSuperProperties;

public class PersistentLoader {

    private static volatile PersistentLoader instance;
    private static Context context;
    private static Future<SharedPreferences> storedPreferences;

    private PersistentLoader(Context context) {
        PersistentLoader.context = context.getApplicationContext();
        final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
        final String prefsName = "baseandroid.sl.sdk.analytics.SlDataAPI";
        storedPreferences = sPrefsLoader.loadPreferences(context, prefsName);
    }

    public static PersistentLoader initLoader(Context context) {
        if (instance == null) {
            instance = new PersistentLoader(context);
        }
        return instance;
    }

    public static PersistentIdentity loadPersistent(String persistentKey) {
        if (instance == null) {
            throw new RuntimeException("you should call 'PersistentLoader.initLoader(Context)' first");
        }
        if (TextUtils.isEmpty(persistentKey)) {
            return null;
        }
        switch (persistentKey) {
            case PersistentName.APP_END_DATA:
                return new PersistentAppEndData(storedPreferences);
            case PersistentName.APP_PAUSED_TIME:
                return new PersistentAppPaused(storedPreferences);
            case PersistentName.APP_SESSION_TIME:
                return new PersistentSessionIntervalTime(storedPreferences);
            case PersistentName.APP_START_TIME:
                return new PersistentAppStartTime(storedPreferences);
            case PersistentName.DISTINCT_ID:
                return new PersistentDistinctId(storedPreferences, context);
            case PersistentName.FIRST_DAY:
                return new PersistentFirstDay(storedPreferences);
            case PersistentName.FIRST_INSTALL:
                return new PersistentFirstTrackInstallation(storedPreferences);
            case PersistentName.FIRST_INSTALL_CALLBACK:
                return new PersistentFirstTrackInstallationWithCallback(storedPreferences);
            case PersistentName.FIRST_START:
                return new PersistentFirstStart(storedPreferences);
            case PersistentName.LOGIN_ID:
                return new PersistentLoginId(storedPreferences);
            case PersistentName.REMOTE_CONFIG:
                return new PersistentRemoteSDKConfig(storedPreferences);
            case PersistentName.SUPER_PROPERTIES:
                return new PersistentSuperProperties(storedPreferences);
            default:
                return null;
        }
    }

    public interface PersistentName {
        String APP_END_DATA = DbParams.TABLE_APP_END_DATA;
        String APP_PAUSED_TIME = DbParams.TABLE_APP_END_TIME;
        String APP_START_TIME = DbParams.TABLE_APP_START_TIME;
        String APP_SESSION_TIME = DbParams.TABLE_SESSION_INTERVAL_TIME;
        String DISTINCT_ID = "events_distinct_id";
        String FIRST_DAY = "first_day";
        String FIRST_START = "first_start";
        String FIRST_INSTALL = "first_track_installation";
        String FIRST_INSTALL_CALLBACK = "first_track_installation_with_callback";
        String LOGIN_ID = "events_login_id";
        String REMOTE_CONFIG = "sldata_sdk_configuration";
        String SUPER_PROPERTIES = "super_properties";
    }
}
