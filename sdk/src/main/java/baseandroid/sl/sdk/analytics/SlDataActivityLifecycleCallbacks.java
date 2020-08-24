/*
 * Created by wangzhuozhou on 2017/4/12.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;


import org.json.JSONObject;

import java.util.Locale;

import baseandroid.sl.sdk.analytics.data.DbAdapter;
import baseandroid.sl.sdk.analytics.data.DbParams;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstDay;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstStart;
import baseandroid.sl.sdk.analytics.deeplink.DeepLinkManager;
import baseandroid.sl.sdk.analytics.deeplink.DeepLinkProcessor;
import baseandroid.sl.sdk.analytics.internal.ScreenAutoTracker;
import baseandroid.sl.sdk.analytics.visual.HeatMapService;
import baseandroid.sl.sdk.analytics.visual.VisualizedAutoTrackService;
import baseandroid.sl.sdk.analytics.util.AopUtil;
import baseandroid.sl.sdk.analytics.util.ChannelUtils;
import baseandroid.sl.sdk.analytics.util.SlDataTimer;
import baseandroid.sl.sdk.analytics.util.SlDataUtils;
import baseandroid.sl.sdk.analytics.util.SlLog;
import baseandroid.sl.sdk.analytics.util.TimeUtils;

import static baseandroid.sl.sdk.analytics.deeplink.DeepLinkManager.IS_ANALYTICS_DEEPLINK;
import static baseandroid.sl.sdk.analytics.deeplink.DeepLinkManager.IS_RESUMED_ANALYTICS_DEEPLINK;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SlDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "Sl.LifecycleCallbacks";
    private static final String EVENT_TIMER = "event_timer";
    private static final String LIB_VERSION = "$lib_version";
    private static final String APP_VERSION = "$app_version";
    private final SlDataAPI mSlDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private Context mContext;
    private boolean resumeFromBackground = false;
    // AppEnd 事件触发默认 Session = 30s shenle改成10秒
    private int sessionTime;
    private DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private JSONObject endDataProperty = new JSONObject();
    private boolean isMultiProcess;
    private int startActivityCount;
    private int startTimerCount;
    // $AppEnd 消息标记位
    private final int MESSAGE_END = 0;
    // $AppStart 事件的时间戳
    private final String APP_START_TIME = "app_start_time";
    // $AppEnd 事件的时间戳
    private final String APP_END_TIME = "app_end_time";
    // $AppEnd 事件属性
    private final String APP_END_DATA = "app_end_data";
    // App 是否重置标记位
    private final String APP_RESET_STATE = "app_reset_state";
    // App 版本号
    private String app_version;
    // SDK 版本号
    private String lib_version;
    private Handler handler;
    /* 兼容由于在魅族手机上退到后台后，线程会被休眠，导致 $AppEnd 无法触发，造成再次打开重复发送。*/
    private long messageReceiveTime = 0L;

    private DeepLinkProcessor mDeepLinkInfo;
    /**
     * 打点时间间隔：2000 毫秒
     */
    private static final int TIME_INTERVAL = 2000;
    private Runnable timer = new Runnable() {
        @Override
        public void run() {
            if (mSlDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd()) {
                generateAppEndData();
            }
        }
    };

    SlDataActivityLifecycleCallbacks(SlDataAPI instance, PersistentFirstStart firstStart,
                                     PersistentFirstDay firstDay, Context context) {
        this.mSlDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mContext = context;
        this.mDbAdapter = DbAdapter.getInstance();
        this.isMultiProcess = mSlDataInstance.isMultiProcess();
        this.sessionTime = mDbAdapter.getSessionIntervalTime();
        try {
            final PackageManager manager = mContext.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            app_version = info.versionName;
            lib_version = SlDataAPI.VERSION;
        } catch (final Exception e) {
            SlLog.i(TAG, "Exception getting version name = ", e);
        }
        initHandler();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        SlDataUtils.handleSchemeUrl(activity, activity.getIntent());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            activityProperty = AopUtil.buildTitleNoAutoTrackerProperties(activity);
            SlDataUtils.mergeJSONObject(activityProperty, endDataProperty);
            if (isMultiProcess) {
                startActivityCount = mDbAdapter.getActivityCount();
                mDbAdapter.commitActivityCount(++startActivityCount);
            } else {
                ++startActivityCount;
            }
            // 如果是第一个页面
            if (startActivityCount == 1) {
                if (mSlDataInstance.isSaveDeepLinkInfo()) {// 保存 utm 信息时,在 endData 中合并保存的 latestUtm 信息。
                    SlDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                }
                handler.removeMessages(MESSAGE_END);
                boolean sessionTimeOut = isSessionTimeOut();
                if (sessionTimeOut) {
                    // 超时尝试补发 $AppEnd
                    handler.sendMessage(generateMessage(false));
                    checkFirstDay();
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();

                    try {
                        mSlDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }

                    //从后台恢复，从缓存中读取 SDK 控制配置信息
                    if (resumeFromBackground) {
                        //先从缓存中读取 SDKConfig
                        mSlDataInstance.applySDKConfigFromCache();
                        mSlDataInstance.resumeTrackScreenOrientation();
//                    mSlDataInstance.resumeTrackTaskThread();
                    }
                    //每次启动 App，重新拉取最新的配置信息
                    mSlDataInstance.pullSDKConfigFromServer();

                    try {
                        if (mSlDataInstance.isAutoTrackEnabled() && !mSlDataInstance.isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_START)) {
                            if (firstStart) {
                                mFirstStart.commit(false);
                            }
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            SlDataUtils.mergeJSONObject(activityProperty, properties);
                            Intent intent = activity.getIntent();
                            mDeepLinkInfo = DeepLinkManager.createDeepLink(intent, mSlDataInstance.getServerUrl());
                            if (mDeepLinkInfo != null) {
                                if (!intent.getBooleanExtra(IS_ANALYTICS_DEEPLINK, false)) {
                                    ChannelUtils.removeDeepLinkInfo(endDataProperty);
                                    //清除本地 utm 属性
                                    ChannelUtils.clearUtm(activity.getApplicationContext());
                                    DeepLinkManager.parseDeepLink(mDeepLinkInfo, activity, properties, endDataProperty, mSlDataInstance.isSaveDeepLinkInfo(), mSlDataInstance.getDeepLinkCallback());
                                    intent.putExtra(IS_ANALYTICS_DEEPLINK, true);
                                }
                            }
                            mSlDataInstance.trackInternal("$AppStart", properties);
                        }

                        try {
                            mDbAdapter.commitAppStartTime(SystemClock.elapsedRealtime());   // 防止动态开启 $AppEnd 时，启动时间戳不对的问题。
                        } catch (Exception ex) {
                            // 出现异常，在重新存储一次，防止使用原有的时间戳造成时长计算错误
                            mDbAdapter.commitAppStartTime(SystemClock.elapsedRealtime());
                        }
                    } catch (Exception e) {
                        SlLog.i(TAG, e);
                    }

                    if (resumeFromBackground) {
                        try {
                            HeatMapService.getInstance().resume();
                            VisualizedAutoTrackService.getInstance().resume();
                        } catch (Exception e) {
                            SlLog.printStackTrace(e);
                        }
                    }

                    // 下次启动时，从后台恢复
                    resumeFromBackground = true;
                }
            }

            if (startTimerCount++ == 0) {
                /*
                 * 在启动的时候开启打点，退出时停止打点，在此处可以防止两点：
                 *  1. App 在 onResume 之前 Crash，导致只有启动没有退出；
                 *  2. 多进程的情况下只会开启一个打点器；
                 */
                SlDataTimer.getInstance().timer(timer, 0, TIME_INTERVAL);
            }
            try {
                if (mSlDataInstance.isAutoTrackEnabled() && !mSlDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())
                        && !mSlDataInstance.isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                    if (mSlDataInstance.getLifeCallEventListener() != null) {
                        doOnActivityPush(activity);
                    }
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        try {
            if (mSlDataInstance.isAutoTrackEnabled() && !mSlDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())
                    && !mSlDataInstance.isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                if (mSlDataInstance.getLifeCallEventListener() != null) {
                    mSlDataInstance.getLifeCallEventListener().onResumeActivity(activity);
                } else {
                    doOnActivityPush(activity);
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private void doOnActivityPush(Activity activity) {
        try {
            JSONObject properties = new JSONObject();
            Intent intent = activity.getIntent();
            if (mDeepLinkInfo == null) {
                mDeepLinkInfo = DeepLinkManager.createDeepLink(intent, mSlDataInstance.getServerUrl());
            }
            if (mDeepLinkInfo != null) {
                //判断 deepLink 信息是否已处理过
                if (!intent.getBooleanExtra(IS_ANALYTICS_DEEPLINK, false)) {
                    ChannelUtils.removeDeepLinkInfo(endDataProperty);
                    //清除本地 utm 属性
                    ChannelUtils.clearUtm(activity.getApplicationContext());
                    DeepLinkManager.parseDeepLink(mDeepLinkInfo, activity, properties, endDataProperty, mSlDataInstance.isSaveDeepLinkInfo(), mSlDataInstance.getDeepLinkCallback());
                    intent.putExtra(IS_ANALYTICS_DEEPLINK, true);
                    intent.putExtra(IS_RESUMED_ANALYTICS_DEEPLINK, true);
                } else if (!intent.getBooleanExtra(IS_RESUMED_ANALYTICS_DEEPLINK, false)) {
                    mDeepLinkInfo.mergeDeepLinkProperty(properties);
                    intent.putExtra(IS_RESUMED_ANALYTICS_DEEPLINK, true);
                }
                mDeepLinkInfo = null;
            }
            if (mSlDataInstance.isAutoTrackEnabled() && !mSlDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())
                    && !mSlDataInstance.isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                if (SlDataAPI.sharedInstance().getLifeCallEventListener() != null) {
                    SlDataAPI.sharedInstance().getLifeCallEventListener().onUpActivity(activity);
                }
//                else {
                if (activity instanceof ScreenAutoTracker) {

                }else{
                    properties.put("$isScreenAutoTracker", 0);
                }
                SlDataUtils.mergeJSONObject(activityProperty, properties);
                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        SlDataUtils.mergeJSONObject(otherProperties, properties);
                    }
                    properties.put("$isScreenAutoTracker", "1");
                }
                mSlDataInstance.trackViewScreen(SlDataUtils.getScreenUrl(activity), properties);
//                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        try {
            // 停止计时器，针对跨进程的情况，要停止当前进程的打点器
            startTimerCount--;
            if (startTimerCount == 0) {
                SlDataTimer.getInstance().shutdownTimerTask();
            }

            if (mSlDataInstance.isMultiProcess()) {
                startActivityCount = mDbAdapter.getActivityCount();
                startActivityCount = startActivityCount > 0 ? --startActivityCount : 0;
                mDbAdapter.commitActivityCount(startActivityCount);
            } else {
                startActivityCount--;
            }

            /*
             * 为了处理跨进程之间跳转 Crash 的情况，由于在 ExceptionHandler 中进行重置，
             * 所以会引起的计数器小于 0 的情况。
             */
            if (startActivityCount <= 0) {
                generateAppEndData();
                handler.sendMessageDelayed(generateMessage(true), sessionTime);
            }
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    /**
     * 发送 $AppEnd 事件
     *
     * @param pausedTime  退出时间
     * @param jsonEndData $AppEnd 事件属性
     */
    private void trackAppEnd(long startTime, long pausedTime, String jsonEndData) {
        try {
            if (mSlDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd()) {
                if (!TextUtils.isEmpty(jsonEndData)) {
                    JSONObject endDataJsonObject = new JSONObject(jsonEndData);
                    long endTime = endDataJsonObject.optLong(EVENT_TIMER); // 获取结束时间戳
                    // 读取指定的字段，防止别人篡改写入脏属性
                    JSONObject properties = new JSONObject();
                    properties.put("$screen_name", endDataJsonObject.optString("$screen_name"));
                    properties.put("$title", endDataJsonObject.optString("$title"));
                    properties.put(LIB_VERSION, endDataJsonObject.optString(LIB_VERSION));
                    properties.put(APP_VERSION, endDataJsonObject.optString(APP_VERSION));
                    properties.put("event_duration", duration(startTime, endTime));
                    properties.put("event_time", pausedTime);
                    ChannelUtils.mergeUtmToEndData(endDataJsonObject, properties);
                    mSlDataInstance.trackInternal("$AppEnd", properties);
                    mDbAdapter.commitAppEndData(""); // 保存的信息只使用一次就置空，防止后面状态错乱再次发送。
                    mSlDataInstance.flushSync();
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 计算退出事件时长
     *
     * @param startTime 启动时间
     * @param endTime   退出时间
     * @return 时长
     */
    private String duration(long startTime, long endTime) {
        long duration = endTime - startTime;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return String.valueOf(0);
            }
            float durationFloat = duration / 1000.0f;
            return durationFloat < 0 ? String.valueOf(0) : String.format(Locale.CHINA, "%.3f", durationFloat);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
            return String.valueOf(0);
        }
    }

    /**
     * 存储当前的 AppEnd 事件关键信息
     */
    private void generateAppEndData() {
        try {
            endDataProperty.put(EVENT_TIMER, SystemClock.elapsedRealtime());
            endDataProperty.put(APP_VERSION, app_version);
            endDataProperty.put(LIB_VERSION, lib_version);
            mDbAdapter.commitAppEndData(endDataProperty.toString());
            mDbAdapter.commitAppEndTime(System.currentTimeMillis());
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 判断是否超出 Session 时间间隔
     *
     * @return true 超时，false 未超时
     */
    private boolean isSessionTimeOut() {
        long currentTime = System.currentTimeMillis() > 946656000000L ? System.currentTimeMillis() : 946656000000L;
        boolean sessionTimeOut = Math.abs(currentTime - mDbAdapter.getAppEndTime()) > sessionTime;
        SlLog.d(TAG, "SessionTimeOut:" + sessionTimeOut);
        return sessionTimeOut;
    }

    /**
     * 构建 Message 对象
     *
     * @param resetState 是否重置状态
     * @return Message
     */
    private Message generateMessage(boolean resetState) {
        Message message = Message.obtain(handler);
        message.what = MESSAGE_END;
        Bundle bundle = new Bundle();
        bundle.putLong(APP_START_TIME, DbAdapter.getInstance().getAppStartTime());
        bundle.putLong(APP_END_TIME, DbAdapter.getInstance().getAppEndTime());
        bundle.putString(APP_END_DATA, DbAdapter.getInstance().getAppEndData());
        bundle.putBoolean(APP_RESET_STATE, resetState);
        message.setData(bundle);
        return message;
    }

    private void initHandler() {
        try {
            HandlerThread handlerThread = new HandlerThread("app_end_timer");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (messageReceiveTime != 0 && SystemClock.elapsedRealtime() - messageReceiveTime < sessionTime) {
                        SlLog.i(TAG, "$AppEnd 事件已触发。");
                        return;
                    }
                    messageReceiveTime = SystemClock.elapsedRealtime();
                    if (msg != null) {
                        Bundle bundle = msg.getData();
                        long startTime = bundle.getLong(APP_START_TIME);
                        long endTime = bundle.getLong(APP_END_TIME);
                        String endData = bundle.getString(APP_END_DATA);
                        boolean resetState = bundle.getBoolean(APP_RESET_STATE);
                        // 如果是正常的退到后台，需要重置标记位
                        if (resetState) {
                            resetState();
                        } else {// 如果是补发则需要添加打点间隔，防止 $AppEnd 在 AppCrash 事件序列之前
                            endTime = endTime + TIME_INTERVAL;
                        }
                        trackAppEnd(startTime, endTime, endData);
                    }
                }
            };
            // 注册 Session 监听，防止多进程
            mContext.getContentResolver().registerContentObserver(DbParams.getInstance().getSessionTimeUri(),
                    false, new SlActivityStateObserver(handler));
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
    }

    /**
     * AppEnd 正常结束时，重置一些设置状态
     */
    private void resetState() {
        try {
            mSlDataInstance.stopTrackScreenOrientation();
            mSlDataInstance.resetPullSDKConfigTimer();
            HeatMapService.getInstance().stop();
            VisualizedAutoTrackService.getInstance().stop();
            mSlDataInstance.appEnterBackground();
            resumeFromBackground = true;
            mSlDataInstance.clearLastScreenUrl();
//            mSlDataInstance.stopTrackTaskThread();
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 检查 DateFormat 是否为空，如果为空则进行初始化
     */
    private void checkFirstDay() {
        if (mFirstDay.get() == null) {
            mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
        }
    }

    private class SlActivityStateObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        SlActivityStateObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            try {
                if (DbParams.getInstance().getSessionTimeUri().equals(uri)) {
                    sessionTime = mDbAdapter.getSessionIntervalTime();
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }
        }
    }

    private boolean isAutoTrackAppEnd() {
        return !mSlDataInstance.isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_END);
    }
}
