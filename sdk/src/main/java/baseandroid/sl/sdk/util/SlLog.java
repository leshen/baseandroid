/*
 * Created by wangzhuozhou on 2017/5/5.
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

import android.util.Log;


public class SALog {
    private static boolean debug;
    private static boolean enableLog;

    public static void d(String tag, String msg) {
        if (debug) {
            info(tag, msg, null);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (debug) {
            info(tag, msg, tr);
        }

    }

    public static void i(String tag, String msg) {
        if (enableLog) {
            info(tag, msg, null);
        }
    }

    public static void i(String tag, Throwable tr) {
        if (enableLog) {
            info(tag, "", tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (enableLog) {
            info(tag, msg, tr);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     *
     * @param tag String
     * @param msg String
     * @param tr Throwable
     */
    public static void info(String tag, String msg, Throwable tr) {
        try {
            Log.i(tag, msg, tr);
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     *
     * @param e Exception
     */
    public static void printStackTrace(Exception e) {
        if (enableLog && e != null) {
            e.printStackTrace();
        }
    }

    /**
     * 设置 Debug 状态
     *
     * @param isDebug Debug 状态
     */
    static void setDebug(boolean isDebug) {
        debug = isDebug;
    }

    /**
     * 设置是否打印 Log
     *
     * @param isEnableLog Log 状态
     */
    static void setEnableLog(boolean isEnableLog) {
        enableLog = isEnableLog;
    }

    static boolean isLogEnabled() {
        return enableLog;
    }
}
