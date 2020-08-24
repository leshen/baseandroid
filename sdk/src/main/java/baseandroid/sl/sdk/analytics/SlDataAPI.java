package baseandroid.sl.sdk.analytics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import baseandroid.sl.sdk.R;
import baseandroid.sl.sdk.analytics.data.DbAdapter;
import baseandroid.sl.sdk.analytics.data.PersistentLoader;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentDistinctId;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstDay;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstStart;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstTrackInstallation;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentFirstTrackInstallationWithCallback;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentRemoteSDKConfig;
import baseandroid.sl.sdk.analytics.data.persistent.PersistentSuperProperties;
import baseandroid.sl.sdk.analytics.deeplink.SlDataDeepLinkCallback;
import baseandroid.sl.sdk.analytics.encrypt.SecreteKey;
import baseandroid.sl.sdk.analytics.encrypt.SlDataEncrypt;
import baseandroid.sl.sdk.analytics.exceptions.InvalidDataException;
import baseandroid.sl.sdk.analytics.internal.FragmentAPI;
import baseandroid.sl.sdk.analytics.internal.IFragmentAPI;
import baseandroid.sl.sdk.analytics.internal.ISlDataAPI;
import baseandroid.sl.sdk.analytics.internal.ScreenAutoTracker;
import baseandroid.sl.sdk.analytics.internal.SlEventListener;
import baseandroid.sl.sdk.analytics.internal.SlLifeCallEventListener;
import baseandroid.sl.sdk.analytics.util.AopUtil;
import baseandroid.sl.sdk.analytics.util.AppInfoUtils;
import baseandroid.sl.sdk.analytics.util.ChannelUtils;
import baseandroid.sl.sdk.analytics.util.DeviceUtils;
import baseandroid.sl.sdk.analytics.util.JSONUtils;
import baseandroid.sl.sdk.analytics.util.NetworkUtils;
import baseandroid.sl.sdk.analytics.util.SlDataUtils;
import baseandroid.sl.sdk.analytics.util.SlLog;
import baseandroid.sl.sdk.analytics.util.TimeUtils;

import static baseandroid.sl.sdk.analytics.util.Base64Coder.CHARSET_UTF8;
import static baseandroid.sl.sdk.analytics.util.SlDataHelper.assertKey;
import static baseandroid.sl.sdk.analytics.util.SlDataHelper.assertPropertyTypes;
import static baseandroid.sl.sdk.analytics.util.SlDataHelper.assertValue;

public class SlDataAPI implements ISlDataAPI {
    // 可视化埋点功能最低 API 版本
    public static final int VTRACK_SUPPORTED_MIN_API = 16;
    // SDK版本
    static final String VERSION = BuildConfig.SDK_VERSION;
    // 此属性插件会进行访问，谨慎删除。当前 SDK 版本所需插件最低版本号，设为空，意为没有任何限制
    static final String MIN_PLUGIN_VERSION = BuildConfig.MIN_PLUGIN_VERSION;

    // Maps each token to a singleton SlDataAPI instance
    private static final Map<Context, SlDataAPI> sInstanceMap = new HashMap<>();
    private static final String TAG = "Sl.SlDataAPI";
    static boolean mIsMainProcess = false;
    static boolean SHOW_DEBUG_INFO_VIEW = true;
    private static SlDataSDKRemoteConfig mSDKRemoteConfig;
    private static SlDataGPSLocation mGPSLocation;
    /* 远程配置 */
    private static SlConfigOptions mSlConfigOptions;
    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final PersistentDistinctId mDistinctId;
    private final PersistentSuperProperties mSuperProperties;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private final PersistentFirstTrackInstallation mFirstTrackInstallation;
    private final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
    private final PersistentRemoteSDKConfig mPersistentRemoteSDKConfig;
    private final Map<String, Object> mDeviceInfo;
    private final Map<String, EventTimer> mTrackTimer;
    private final Object mLoginIdLock = new Object();
    /**
     * 每次启动 App 时，最多尝试三次
     */
    private CountDownTimer mPullSDKConfigCountDownTimer;
    private List<Class> mIgnoredViewTypeList = new ArrayList<>();
    /* AndroidID */
    private String mAndroidId = null;
    /* LoginId */
    private String mLoginId = null;
    /* SlAnalytics 地址 */
    private String mServerUrl;
    private String mOriginServerUrl;
    /* SDK 配置是否初始化 */
    private boolean mSDKConfigInit;
    /* Debug 模式选项 */
    private DebugMode mDebugMode = DebugMode.DEBUG_OFF;
    /* SDK 自动采集事件 */
    private boolean mAutoTrack;
    /* 上个页面的 Url*/
    private String mLastScreenUrl;
    private JSONObject mLastScreenTrackProperties;
    /* 是否请求网络 */
    private boolean mEnableNetworkRequest = true;
    private boolean mClearReferrerWhenAppEnd = false;
    private boolean mDisableDefaultRemoteConfig = false;
    private boolean mDisableTrackDeviceId = false;
    private List<Integer> mAutoTrackIgnoredActivities;
    private List<Integer> mHeatMapActivities;
    private List<Integer> mVisualizedAutoTrackActivities;
    /* 主进程名称 */
    private String mMainProcessName;
    private String mCookie;
    private TrackTaskManager mTrackTaskManager;
    private TrackTaskManagerThread mTrackTaskManagerThread;
    private SlDataScreenOrientationDetector mOrientationDetector;
    private SlDataDynamicSuperProperties mDynamicSuperProperties;
    private SimpleDateFormat mIsFirstDayDateFormat;
    private SSLSocketFactory mSSLSocketFactory;
    private SlDataTrackEventCallBack mTrackEventCallBack;
    private List<SlEventListener> mEventListenerList;

    private SlLifeCallEventListener mLifeCallEventListener;
    private IFragmentAPI mFragmentAPI;
    SlDataEncrypt mSlDataEncrypt;
    private SlDataDeepLinkCallback mDeepLinkCallback;

    //private
    SlDataAPI() {
        mContext = null;
        mMessages = null;
        mDistinctId = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mPersistentRemoteSDKConfig = null;
        mDeviceInfo = null;
        mTrackTimer = null;
        mMainProcessName = null;
        mSlDataEncrypt = null;
    }

    SlDataAPI(Context context, String serverURL, DebugMode debugMode) {
        mContext = context;
        setDebugMode(debugMode);
        final String packageName = context.getApplicationContext().getPackageName();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mHeatMapActivities = new ArrayList<>();
        mVisualizedAutoTrackActivities = new ArrayList<>();
        PersistentLoader.initLoader(context);
        mDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.DISTINCT_ID);
        mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.SUPER_PROPERTIES);
        mFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_START);
        mFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL);
        mFirstTrackInstallationWithCallback = (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL_CALLBACK);
        mPersistentRemoteSDKConfig = (PersistentRemoteSDKConfig) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.REMOTE_CONFIG);
        mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_DAY);

        mTrackTaskManager = TrackTaskManager.getInstance();
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
        SlDataExceptionHandler.init();
        initSlConfig(serverURL, packageName);
        if (mSlConfigOptions.mEnableEncrypt) {
            mSlDataEncrypt = new SlDataEncrypt(context, mSlConfigOptions.mPersistentSecretKey);
        }

        DbAdapter.getInstance(context, packageName, mSlDataEncrypt);
        mMessages = AnalyticsMessages.getInstance(mContext);
        mAndroidId = SlDataUtils.getAndroidID(mContext);

        //先从缓存中读取 SDKConfig
        applySDKConfigFromCache();

        //打开 debug 模式，弹出提示
        if (mDebugMode != DebugMode.DEBUG_OFF && mIsMainProcess) {
            if (SHOW_DEBUG_INFO_VIEW) {
                if (!isSDKDisabled()) {
                    showDebugModeWarning();
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();
            final SlDataActivityLifecycleCallbacks lifecycleCallbacks =
                    new SlDataActivityLifecycleCallbacks(this, mFirstStart, mFirstDay, context);
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            app.registerActivityLifecycleCallbacks(AppStateManager.getInstance());
        }

        SlLog.i(TAG, String.format(Locale.CHINA, "Initialized the instance of Sl Analytics SDK with server"
                + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mSlConfigOptions.mFlushInterval, debugMode));

        mDeviceInfo = setupDeviceInfo();
        mTrackTimer = new HashMap<>();
        mFragmentAPI = new FragmentAPI(context);
    }

    /**
     * 获取 SlDataAPI 单例
     *
     * @param context App的Context
     * @return SlDataAPI 单例
     */
    public static SlDataAPI sharedInstance(Context context) {
        if (isSDKDisabled()) {
            return new SlDataAPIEmptyImplementation();
        }

        if (null == context) {
            return new SlDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            SlDataAPI instance = sInstanceMap.get(appContext);

            if (null == instance) {
                SlLog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
                return new SlDataAPIEmptyImplementation();
            }
            return instance;
        }
    }

    /**
     * 初始化并获取 SlDataAPI 单例
     *
     * @param context   App 的 Context
     * @param serverURL 用于收集事件的服务地址
     * @param debugMode Debug 模式,
     *                  {@link SlDataAPI.DebugMode}
     * @return SlDataAPI 单例
     */
    @Deprecated
    public static SlDataAPI sharedInstance(Context context, String serverURL, DebugMode debugMode) {
        return getInstance(context, serverURL, debugMode);
    }

    /**
     * 初始化并获取 SlDataAPI 单例
     *
     * @param context   App 的 Context
     * @param serverURL 用于收集事件的服务地址
     * @return SlDataAPI 单例
     */
    @Deprecated
    public static SlDataAPI sharedInstance(Context context, String serverURL) {
        return getInstance(context, serverURL, DebugMode.DEBUG_OFF);
    }

    /**
     * 初始化并获取 SlDataAPI 单例
     *
     * @param context         App 的 Context
     * @param saConfigOptions SDK 的配置项
     * @return SlDataAPI 单例
     */
    @Deprecated
    public static SlDataAPI sharedInstance(Context context, SlConfigOptions saConfigOptions) {
        mSlConfigOptions = saConfigOptions;
        SlDataAPI slDataAPI = getInstance(context, saConfigOptions.mServerUrl, DebugMode.DEBUG_OFF);
        if (!slDataAPI.mSDKConfigInit) {
            slDataAPI.applySlConfigOptions();
        }
        return slDataAPI;
    }

    /**
     * 初始化神策 SDK
     *
     * @param context         App 的 Context
     * @param saConfigOptions SDK 的配置项
     */
    public static void startWithConfigOptions(Context context, SlConfigOptions saConfigOptions) {
        if (context == null || saConfigOptions == null) {
            throw new NullPointerException("Context、SlConfigOptions 不可以为 null");
        }
        mSlConfigOptions = saConfigOptions;
        SlDataAPI slDataAPI = getInstance(context, saConfigOptions.mServerUrl, DebugMode.DEBUG_OFF);
        if (!slDataAPI.mSDKConfigInit) {
            slDataAPI.applySlConfigOptions();
        }
    }

    private static SlDataAPI getInstance(Context context, String serverURL, DebugMode debugMode) {
        if (null == context) {
            return new SlDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            SlDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance) {
                instance = new SlDataAPI(appContext, serverURL, debugMode);
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    public static SlDataAPI sharedInstance() {
        if (isSDKDisabled()) {
            return new SlDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<SlDataAPI> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return new SlDataAPIEmptyImplementation();
        }
    }

    /**
     * 返回是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    public static boolean isSDKDisabled() {
        if (mSDKRemoteConfig == null) {
            return false;
        }

        return mSDKRemoteConfig.isDisableSDK();
    }

    /**
     * 获取并配置 App 的一些基本属性
     */
    private Map<String, Object> setupDeviceInfo() {
        final Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("$lib", "Android");
        deviceInfo.put("$lib_version", VERSION);
        deviceInfo.put("$os", "Android");
        deviceInfo.put("$os_version", DeviceUtils.getOS());
        deviceInfo.put("$manufacturer", DeviceUtils.getManufacturer());
        deviceInfo.put("$model", DeviceUtils.getModel());
        deviceInfo.put("$app_version", AppInfoUtils.getAppVersionName(mContext));
        int[] size = DeviceUtils.getDeviceSize(mContext);
        deviceInfo.put("$screen_width", size[0]);
        deviceInfo.put("$screen_height", size[1]);

        String carrier = SlDataUtils.getCarrier(mContext);
        if (!TextUtils.isEmpty(carrier)) {
            deviceInfo.put("$carrier", carrier);
        }

        if (!mDisableTrackDeviceId && !TextUtils.isEmpty(mAndroidId)) {
            deviceInfo.put("$device_id", mAndroidId);
        }

        Integer zone_offset = TimeUtils.getZoneOffset();
        if (zone_offset != null) {
            deviceInfo.put("$timezone_offset", zone_offset);
        }

        deviceInfo.put("$app_id", AppInfoUtils.getProcessName(mContext));
        return Collections.unmodifiableMap(deviceInfo);
    }

    /**
     * 更新 SlDataSDKRemoteConfig
     *
     * @param sdkRemoteConfig   SlDataSDKRemoteConfig 在线控制 SDK 的配置
     * @param effectImmediately 是否立即生效
     */
    private void setSDKRemoteConfig(SlDataSDKRemoteConfig sdkRemoteConfig, boolean effectImmediately) {
        try {
            if (sdkRemoteConfig.isDisableSDK()) {
                SlDataSDKRemoteConfig cachedConfig = SlDataUtils.toSDKRemoteConfig(mPersistentRemoteSDKConfig.get());
                if (!cachedConfig.isDisableSDK()) {
                    trackInternal("DisableSlDataSDK", null);
                }
            }
            mPersistentRemoteSDKConfig.commit(sdkRemoteConfig.toJson().toString());
            if (effectImmediately) {
                mSDKRemoteConfig = sdkRemoteConfig;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    void pullSDKConfigFromServer() {
        if (!isNetworkRequestEnable()) {
            return;
        }

        boolean enableConfigV = true;
        // 如果是开启加密的情况下，则判断密钥是否为空或是否是版本升级
        if (mSlConfigOptions.mEnableEncrypt) {
            if (SlDataUtils.checkVersionIsNew(mContext, VERSION)) {
                enableConfigV = false;
            } else if (mSlDataEncrypt.isRSlSecretKeyNull()) {
                enableConfigV = false;
            }
        }

        /*
         * 满足以下条件则触发远程配置请求：
         * 1. 开启加密，但是密钥为空或升级版本，即 enableConfigV = false；
         * 2. 禁用分散请求时，即 mDisableRandomTimeRequestRemoteConfig = true；
         * 3. 分散请求满足条件时；
         */
        if (enableConfigV && mSlConfigOptions != null && !mSlConfigOptions.mDisableRandomTimeRequestRemoteConfig
                && !SlDataUtils.isRequestValid(mContext, mSlConfigOptions.mMinRequestInterval, mSlConfigOptions.mMaxRequestInterval)) {
            return;
        }

        if (mDisableDefaultRemoteConfig) {
            return;
        }

        if (mPullSDKConfigCountDownTimer != null) {
            mPullSDKConfigCountDownTimer.cancel();
            mPullSDKConfigCountDownTimer = null;
        }

        final boolean finalEnableConfigV = enableConfigV;
        mPullSDKConfigCountDownTimer = new CountDownTimer(120 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStreamReader in = null;
                        HttpURLConnection urlConnection = null;
                        try {
                            URL url;
                            String configUrl = null;
                            if (mSlConfigOptions != null && !TextUtils.isEmpty(mSlConfigOptions.mRemoteConfigUrl)) {
                                configUrl = mSlConfigOptions.mRemoteConfigUrl;
                            } else {
                                if (!TextUtils.isEmpty(mServerUrl)) {
                                    int pathPrefix = mServerUrl.lastIndexOf("/");
                                    if (pathPrefix != -1) {
                                        configUrl = mServerUrl.substring(0, pathPrefix);
                                        configUrl = configUrl + "/config/Android.conf";
                                    }
                                }
                            }

                            if (TextUtils.isEmpty(configUrl)) {
                                SlLog.i(TAG, "Remote config url is null or empty.");
                                return;
                            }

                            if (!TextUtils.isEmpty(configUrl) && finalEnableConfigV) {
                                String configVersion = null;
                                if (mSDKRemoteConfig != null) {
                                    configVersion = mSDKRemoteConfig.getV();
                                }

                                if (!TextUtils.isEmpty(configVersion)) {
                                    if (configUrl.contains("?")) {
                                        configUrl = configUrl + "&v=" + configVersion;
                                    } else {
                                        configUrl = configUrl + "?v=" + configVersion;
                                    }
                                }
                            }

                            SlLog.i(TAG, "Android remote config url:" + configUrl);
                            url = new URL(configUrl);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            if (urlConnection == null) {
                                SlLog.i(TAG, String.format("can not connect %s, it shouldn't happen", url.toString()), null);
                                return;
                            }
                            if (mSSLSocketFactory != null && urlConnection instanceof HttpsURLConnection) {
                                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(mSSLSocketFactory);
                            }
                            int responseCode = urlConnection.getResponseCode();

                            //配置没有更新
                            if (responseCode == 304 || responseCode == 404) {
                                resetPullSDKConfigTimer();
                                return;
                            }

                            if (responseCode == 200) {
                                resetPullSDKConfigTimer();

                                in = new InputStreamReader(urlConnection.getInputStream());
                                BufferedReader bufferedReader = new BufferedReader(in);
                                StringBuilder result = new StringBuilder();
                                String data;
                                while ((data = bufferedReader.readLine()) != null) {
                                    result.append(data);
                                }
                                data = result.toString();
                                if (!TextUtils.isEmpty(data)) {
                                    SlDataSDKRemoteConfig sdkRemoteConfig = SlDataUtils.toSDKRemoteConfig(data);
                                    try {
                                        if (mSlConfigOptions.mEnableEncrypt) {
                                            mSlDataEncrypt.saveSecretKey(new SecreteKey(sdkRemoteConfig.getRsaPublicKey(), sdkRemoteConfig.getPkv()));
                                        }
                                    } catch (Exception e) {
                                        SlLog.printStackTrace(e);
                                    }

                                    setSDKRemoteConfig(sdkRemoteConfig, false);
                                }
                            }
                        } catch (Exception e) {
                            SlLog.printStackTrace(e);
                        } finally {
                            try {
                                if (in != null) {
                                    in.close();
                                }

                                if (urlConnection != null) {
                                    urlConnection.disconnect();
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }
                }, ThreadNameConstants.THREAD_GET_SDK_REMOTE_CONFIG).start();
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    void resetPullSDKConfigTimer() {
        try {
            if (mPullSDKConfigCountDownTimer != null) {
                mPullSDKConfigCountDownTimer.cancel();
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        } finally {
            mPullSDKConfigCountDownTimer = null;
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    void applySDKConfigFromCache() {
        try {
            SlDataSDKRemoteConfig sdkRemoteConfig = SlDataUtils.toSDKRemoteConfig(mPersistentRemoteSDKConfig.get());
            //关闭 debug 模式
            if (sdkRemoteConfig.isDisableDebugMode()) {
                setDebugMode(DebugMode.DEBUG_OFF);
            }

            //开启关闭 AutoTrack
            if (sdkRemoteConfig.getAutoTrackEventType() != 0) {
                enableAutoTrack(sdkRemoteConfig.getAutoTrackEventType());
            }

            if (sdkRemoteConfig.isDisableSDK()) {
                try {
                    flush();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }

            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    @Override
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("$app_version", mDeviceInfo.get("$app_version"));
            properties.put("$lib", "Android");
            properties.put("$lib_version", VERSION);
            properties.put("$manufacturer", mDeviceInfo.get("$manufacturer"));
            properties.put("$model", mDeviceInfo.get("$model"));
            properties.put("$os", "Android");
            properties.put("$os_version", mDeviceInfo.get("$os_version"));
            properties.put("$screen_height", mDeviceInfo.get("$screen_height"));
            properties.put("$screen_width", mDeviceInfo.get("$screen_width"));
            String networkType = NetworkUtils.networkType(mContext);
            properties.put("$wifi", "WIFI".equals(networkType));
            properties.put("$network_type", networkType);
            properties.put("$carrier", mDeviceInfo.get("$carrier"));
            properties.put("$is_first_day", isFirstDay(System.currentTimeMillis()));
            properties.put("$app_id", mDeviceInfo.get("$app_id"));
            properties.put("$timezone_offset", mDeviceInfo.get("$timezone_offset"));
            if (mDeviceInfo.containsKey("$device_id")) {
                properties.put("$device_id", mDeviceInfo.get("$device_id"));
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return properties;
    }

    @Override
    public void enableLog(boolean enable) {
        SlLog.setEnableLog(enable);
    }

    @Override
    public long getMaxCacheSize() {
        return mSlConfigOptions.mMaxCacheSize;
    }

    @Override
    public void setMaxCacheSize(long maxCacheSize) {
        mSlConfigOptions.setMaxCacheSize(maxCacheSize);
    }

    @Override
    public void setFlushNetworkPolicy(int networkType) {
        mSlConfigOptions.setNetworkTypePolicy(networkType);
    }

    int getFlushNetworkPolicy() {
        return mSlConfigOptions.mNetworkTypePolicy;
    }

    @Override
    public int getFlushInterval() {
        return mSlConfigOptions.mFlushInterval;
    }

    @Override
    public void setFlushInterval(int flushInterval) {
        mSlConfigOptions.setFlushInterval(flushInterval);
    }

    @Override
    public int getFlushBulkSize() {
        return mSlConfigOptions.mFlushBulkSize;
    }

    @Override
    public void setFlushBulkSize(int flushBulkSize) {
        if (flushBulkSize < 0) {
            SlLog.i(TAG, "The value of flushBulkSize is invalid");
        }
        mSlConfigOptions.setFlushBulkSize(flushBulkSize);
    }

    @Override
    public int getSessionIntervalTime() {
        if (DbAdapter.getInstance() == null) {
            SlLog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
            //shenle 改为10秒
            return 10 * 1000;
//            return 30 * 1000;
        }

        return DbAdapter.getInstance().getSessionIntervalTime();
    }

    @Override
    public void setSessionIntervalTime(int sessionIntervalTime) {
        if (DbAdapter.getInstance() == null) {
            SlLog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
            return;
        }

        if (sessionIntervalTime < 10 * 1000 || sessionIntervalTime > 5 * 60 * 1000) {
            SlLog.i(TAG, "SessionIntervalTime:" + sessionIntervalTime + " is invalid, session interval time is between 10s and 300s.");
            return;
        }

        DbAdapter.getInstance().commitSessionIntervalTime(sessionIntervalTime);
    }

    @Override
    public void setGPSLocation(double latitude, double longitude) {
        try {
            if (mGPSLocation == null) {
                mGPSLocation = new SlDataGPSLocation();
            }

            mGPSLocation.setLatitude((long) (latitude * Math.pow(10, 6)));
            mGPSLocation.setLongitude((long) (longitude * Math.pow(10, 6)));
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void clearGPSLocation() {
        mGPSLocation = null;
    }

    @Override
    public void enableTrackScreenOrientation(boolean enable) {
        try {
            if (enable) {
                if (mOrientationDetector == null) {
                    mOrientationDetector = new SlDataScreenOrientationDetector(mContext, SensorManager.SENSOR_DELAY_NORMAL);
                }
                mOrientationDetector.enable();
            } else {
                if (mOrientationDetector != null) {
                    mOrientationDetector.disable();
                    mOrientationDetector = null;
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void resumeTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.enable();
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void stopTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.disable();
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public String getScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                return mOrientationDetector.getOrientation();
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return null;
    }

    @Override
    public void setCookie(String cookie, boolean encode) {
        try {
            if (encode) {
                this.mCookie = URLEncoder.encode(cookie, CHARSET_UTF8);
            } else {
                this.mCookie = cookie;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public String getCookie(boolean decode) {
        try {
            if (decode) {
                return URLDecoder.decode(this.mCookie, CHARSET_UTF8);
            } else {
                return this.mCookie;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
            return null;
        }

    }

    @Deprecated
    @Override
    public void enableAutoTrack() {
        List<AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(AutoTrackEventType.APP_START);
        eventTypeList.add(AutoTrackEventType.APP_END);
        eventTypeList.add(AutoTrackEventType.APP_VIEW_SCREEN);
        enableAutoTrack(eventTypeList);
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        try {
            if (eventTypeList == null || eventTypeList.isEmpty()) {
                return;
            }
            this.mAutoTrack = true;
            for (AutoTrackEventType autoTrackEventType : eventTypeList) {
                mSlConfigOptions.setAutoTrackEventType(mSlConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private void enableAutoTrack(int autoTrackEventType) {
        try {
            if (autoTrackEventType <= 0 || autoTrackEventType > 15) {
                return;
            }
            this.mAutoTrack = true;
            mSlConfigOptions.setAutoTrackEventType(mSlConfigOptions.mAutoTrackEventType | autoTrackEventType);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        ignoreAutoTrackEventType(eventTypeList);
    }

    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        ignoreAutoTrackEventType(autoTrackEventType);
    }

    @Override
    public void trackAppCrash() {
        SlDataExceptionHandler.enableAppCrash();
    }

    @Override
    public boolean isAutoTrackEnabled() {
        if (isSDKDisabled()) {
            return false;
        }

        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                return false;
            } else if (mSDKRemoteConfig.getAutoTrackMode() > 0) {
                return true;
            }
        }

        return mAutoTrack;
    }

    @Override
    public void trackFragmentAppViewScreen() {
        mFragmentAPI.trackFragmentAppViewScreen();
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return mFragmentAPI.isTrackFragmentAppViewScreenEnabled();
    }

    @Override
    public void enableReactNativeAutoTrack() {
        mSlConfigOptions.enableReactNativeAutoTrack(true);
    }

    @Override
    public boolean isReactNativeAutoTrackEnabled() {
        return mSlConfigOptions.mRNAutoTrackEnabled;
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {
        showUpWebView(webView, isSupportJellyBean, null);
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify) {
        showUpWebView(webView, null, isSupportJellyBean, enableVerify);
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    @Deprecated
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            SlLog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mContext, properties, enableVerify), "SlData_APP_JS_Bridge");
            SlDataAutoTrackHelper.addWebViewVisualInterface(webView);
        }
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    @Deprecated
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        showUpWebView(webView, properties, isSupportJellyBean, false);
    }

    @Override
    @Deprecated
    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        try {
            if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
                SlLog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
                return;
            }

            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, properties, enableVerify), "SlData_APP_JS_Bridge");
            SlDataAutoTrackHelper.addWebViewVisualInterface((View) x5WebView);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {
        try {
            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, null, enableVerify), "SlData_APP_JS_Bridge");
            SlDataAutoTrackHelper.addWebViewVisualInterface((View) x5WebView);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView(x5WebView, false);
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        int hashCode;
        for (Class<?> activity : activitiesList) {
            if (activity != null) {
                hashCode = activity.hashCode();
                if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                    mAutoTrackIgnoredActivities.add(hashCode);
                }
            }
        }
    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode;
            for (Class activity : activitiesList) {
                if (activity != null) {
                    hashCode = activity.hashCode();
                    if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                        mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
                    }
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.add(hashCode);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.enableAutoTrackFragment(fragment);
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        mFragmentAPI.enableAutoTrackFragments(fragmentsList);
    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SlDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SlDataIgnoreTrackAppViewScreen.class) != null;

    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        return mFragmentAPI.isFragmentAutoTrackAppViewScreen(fragment);
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.ignoreAutoTrackFragments(fragmentList);
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.ignoreAutoTrackFragment(fragment);
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.resumeIgnoredAutoTrackFragments(fragmentList);
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.resumeIgnoredAutoTrackFragment(fragment);
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SlDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SlDataIgnoreTrackAppClick.class) != null;

    }

    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mSlConfigOptions.mAutoTrackEventType == 0) {
            return;
        }

        int union = mSlConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue;
        if (union == autoTrackEventType.eventValue) {
            mSlConfigOptions.setAutoTrackEventType(0);
        } else {
            mSlConfigOptions.setAutoTrackEventType(autoTrackEventType.eventValue ^ union);
        }

        if (mSlConfigOptions.mAutoTrackEventType == 0) {
            this.mAutoTrack = false;
        }
    }

    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null) {
            return;
        }

        if (mSlConfigOptions.mAutoTrackEventType == 0) {
            return;
        }

        for (AutoTrackEventType autoTrackEventType : eventTypeList) {
            if ((mSlConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue) == mSlConfigOptions.mAutoTrackEventType) {
                mSlConfigOptions.setAutoTrackEventType(mSlConfigOptions.mAutoTrackEventType ^ autoTrackEventType.eventValue);
            }
        }

        if (mSlConfigOptions.mAutoTrackEventType == 0) {
            this.mAutoTrack = false;
        }
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        if (eventType == null) {
            return false;
        }
        return isAutoTrackEventTypeIgnored(eventType.eventValue);
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() != SlDataSDKRemoteConfig.REMOTE_EVENT_TYPE_NO_USE) {
                if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                    return true;
                }
                return mSDKRemoteConfig.isAutoTrackEventTypeIgnored(autoTrackEventType);
            }
        }

        return (mSlConfigOptions.mAutoTrackEventType | autoTrackEventType) != mSlConfigOptions.mAutoTrackEventType;
    }

    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(R.id.sl_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(R.id.sl_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void setViewID(Object alertDialog, String viewID) {
        try {
            if (alertDialog == null) {
                return;

            }

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (currentAlertDialogClass == null) {
                return;
            }

            if (!currentAlertDialogClass.isInstance(alertDialog)) {
                return;
            }

            if (!TextUtils.isEmpty(viewID)) {
                Method getWindowMethod = alertDialog.getClass().getMethod("getWindow");
                if (getWindowMethod == null) {
                    return;
                }

                Window window = (Window) getWindowMethod.invoke(alertDialog);
                if (window != null) {
                    window.getDecorView().setTag(R.id.sl_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void setViewActivity(View view, Activity activity) {
        try {
            if (view == null || activity == null) {
                return;
            }
            view.setTag(R.id.sl_analytics_tag_view_activity, activity);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        try {
            if (view == null || TextUtils.isEmpty(fragmentName)) {
                return;
            }
            view.setTag(R.id.sl_analytics_tag_view_fragment_name2, fragmentName);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.sl_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        if (view != null) {
            view.setTag(R.id.sl_analytics_tag_view_ignored, ignore ? "1" : "0");
        }
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }
        JSONObject propertiesOld = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);
        if (propertiesOld != null) {
            AopUtil.mergeJSONObject(propertiesOld, properties);
        }
        view.setTag(R.id.sl_analytics_tag_view_properties, properties);
    }

    @Override
    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains(viewType)) {
            mIgnoredViewTypeList.add(viewType);
        }
    }

    @Override
    public boolean isVisualizedAutoTrackActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return false;
            }
            if (mVisualizedAutoTrackActivities.size() == 0) {
                return true;
            }
            if (mVisualizedAutoTrackActivities.contains(activity.hashCode())) {
                return true;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return false;
    }

    @Override
    public void addVisualizedAutoTrackActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return;
            }
            mVisualizedAutoTrackActivities.add(activity.hashCode());
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList) {
        try {
            if (activitiesList == null || activitiesList.size() == 0) {
                return;
            }

            for (Class<?> activity : activitiesList) {
                if (activity != null) {
                    int hashCode = activity.hashCode();
                    if (!mVisualizedAutoTrackActivities.contains(hashCode)) {
                        mVisualizedAutoTrackActivities.add(hashCode);
                    }
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public boolean isVisualizedAutoTrackEnabled() {
        return mSlConfigOptions.mVisualizedEnabled;
    }

    boolean isVisualizedAutoTrackConfirmDialogEnabled() {
        return mSlConfigOptions.mVisualizedConfirmDialogEnabled;
    }

    @Override
    public void enableVisualizedAutoTrackConfirmDialog(boolean enable) {
        mSlConfigOptions.enableVisualizedAutoTrackConfirmDialog(enable);
    }

    @Override
    public void enableVisualizedAutoTrack() {
        mSlConfigOptions.enableVisualizedAutoTrack(true);
    }

    @Override
    public boolean isHeatMapActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return false;
            }
            if (mHeatMapActivities.size() == 0) {
                return true;
            }
            if (mHeatMapActivities.contains(activity.hashCode())) {
                return true;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return false;
    }

    @Override
    public void addHeatMapActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return;
            }

            mHeatMapActivities.add(activity.hashCode());
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void addHeatMapActivities(List<Class<?>> activitiesList) {
        try {
            if (activitiesList == null || activitiesList.size() == 0) {
                return;
            }

            for (Class<?> activity : activitiesList) {
                if (activity != null) {
                    int hashCode = activity.hashCode();
                    if (!mHeatMapActivities.contains(hashCode)) {
                        mHeatMapActivities.add(hashCode);
                    }
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public boolean isHeatMapEnabled() {
        return mSlConfigOptions.mHeatMapEnabled;
    }

    /**
     * 返回是否开启点击图的提示框
     *
     * @return true 代表开启了点击图的提示框， false 代表关闭了点击图的提示框
     */
    boolean isAppHeatMapConfirmDialogEnabled() {
        return mSlConfigOptions.mHeatMapConfirmDialogEnabled;
    }

    @Override
    public void enableAppHeatMapConfirmDialog(boolean enable) {
        mSlConfigOptions.enableHeatMapConfirmDialog(enable);
    }

    @Override
    public void enableHeatMap() {
        mSlConfigOptions.enableHeatMap(true);
    }

    @Override
    public String getDistinctId() {
        String loginId = getLoginId();
        if (TextUtils.isEmpty(loginId)) {// 如果从本地缓存读取失败，则尝试使用内存中的 LoginId 值
            loginId = mLoginId;
        }
        if (!TextUtils.isEmpty(loginId)) {
            return loginId;
        }
        return getAnonymousId();
    }

    @Override
    public String getAnonymousId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    @Override
    public void resetAnonymousId() {
        synchronized (mDistinctId) {
            if (SlDataUtils.isValidAndroidId(mAndroidId)) {
                mDistinctId.commit(mAndroidId);
            } else {
                mDistinctId.commit(UUID.randomUUID().toString());
            }

            // 通知调用 resetAnonymousId 接口
            if (mEventListenerList != null) {
                for (SlEventListener eventListener : mEventListenerList) {
                    eventListener.resetAnonymousId();
                }
            }
        }
    }

    @Override
    public String getLoginId() {
        return DbAdapter.getInstance().getLoginId();
    }

    @Override
    public void identify(final String distinctId) {
        try {
            assertValue(distinctId);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
            return;
        }
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mDistinctId) {
                        mDistinctId.commit(distinctId);
                        // 通知调用 identify 接口
                        if (mEventListenerList != null) {
                            for (SlEventListener eventListener : mEventListenerList) {
                                eventListener.identify();
                            }
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void login(final String loginId) {
        login(loginId, null);
    }

    @Override
    public void login(final String loginId, final JSONObject properties) {
        try {
            assertValue(loginId);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
            return;
        }
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mLoginIdLock) {
                        if (!loginId.equals(DbAdapter.getInstance().getLoginId()) && !loginId.equals(getAnonymousId())) {
                            mLoginId = loginId;
                            DbAdapter.getInstance().commitLoginId(loginId);
                            trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, getAnonymousId());
                            // 通知调用 login 接口
                            if (mEventListenerList != null) {
                                for (SlEventListener eventListener : mEventListenerList) {
                                    eventListener.login();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void logout() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mLoginIdLock) {
                        DbAdapter.getInstance().commitLoginId(null);
                        mLoginId = null;
                        // 进行通知调用 logout 接口
                        if (mEventListenerList != null) {
                            for (SlEventListener eventListener : mEventListenerList) {
                                eventListener.logout();
                            }
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getAnonymousId();

                    synchronized (mDistinctId) {
                        mDistinctId.commit(newDistinctId);
                    }

                    trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, originalDistinctId);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getAnonymousId();
                    synchronized (mDistinctId) {
                        mDistinctId.commit(newDistinctId);
                    }

                    trackEvent(EventType.TRACK_SIGNUP, "$SignUp", null, originalDistinctId);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackInstallation(final String eventName, final JSONObject properties, final boolean disableCallback) {
        //只在主进程触发 trackInstallation
        final JSONObject _properties;
        if (properties != null) {
            _properties = properties;
        } else {
            _properties = new JSONObject();
        }

        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                if (!mIsMainProcess) {
                    return;
                }
                try {
                    boolean firstTrackInstallation;
                    if (disableCallback) {
                        firstTrackInstallation = mFirstTrackInstallationWithCallback.get();
                    } else {
                        firstTrackInstallation = mFirstTrackInstallation.get();
                    }
                    if (firstTrackInstallation) {
                        try {
                            if (!ChannelUtils.hasUtmProperties(_properties)) {
                                ChannelUtils.mergeUtmByMetaData(mContext, _properties);
                            }

                            if (!ChannelUtils.hasUtmProperties(_properties)) {
                                String installSource;
                                if (_properties.has("$oaid")) {
                                    String oaid = _properties.optString("$oaid");
                                    installSource = ChannelUtils.getDeviceInfo(mContext,
                                            mAndroidId, mSlConfigOptions.isSDKInitOAID, oaid);
                                    SlLog.i(TAG, "properties has oaid " + oaid);
                                } else {
                                    installSource = ChannelUtils.getDeviceInfo(mContext,
                                            mAndroidId, mSlConfigOptions.isSDKInitOAID);
                                }

                                if (_properties.has("$gaid")) {
                                    installSource = String.format("%s##gaid=%s", installSource, _properties.optString("$gaid"));
                                }
                                _properties.put("$ios_install_source", installSource);
                            }
                            if (_properties.has("$oaid")) {
                                _properties.remove("$oaid");
                            }

                            if (_properties.has("$gaid")) {
                                _properties.remove("$gaid");
                            }

                            if (disableCallback) {
                                _properties.put("$ios_install_disable_callback", disableCallback);
                            }
                        } catch (Exception e) {
                            SlLog.printStackTrace(e);
                        }

                        // 先发送 track
                        trackEvent(EventType.TRACK, eventName, _properties, null);

                        // 再发送 profile_set_once 或者 profile_set
                        JSONObject profileProperties = new JSONObject();
                        SlDataUtils.mergeJSONObject(_properties, profileProperties);
                        profileProperties.put("$first_visit_time", new java.util.Date());
                        if (mSlConfigOptions.mEnableMultipleChannelMatch) {
                            trackEvent(EventType.PROFILE_SET, null, profileProperties, null);
                        } else {
                            trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null);
                        }

                        if (disableCallback) {
                            mFirstTrackInstallationWithCallback.commit(false);
                        } else {
                            mFirstTrackInstallation.commit(false);
                        }
                    }
                    flushSync();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties) {
        trackInstallation(eventName, properties, false);
    }

    @Override
    public void trackInstallation(String eventName) {
        trackInstallation(eventName, null, false);
    }

    @Override
    public void trackChannelEvent(String eventName) {
        trackChannelEvent(eventName, null);
    }

    @Override
    public void trackChannelEvent(final String eventName, JSONObject properties) {
        if (getConfigOptions().isAutoAddChannelCallbackEvent) {
            track(eventName, properties);
            return;
        }
        final JSONObject _properties;
        if (properties != null) {
            _properties = properties;
        } else {
            _properties = new JSONObject();
        }
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        _properties.put("$is_channel_callback_event", DbAdapter.getInstance().isFirstChannelEvent(eventName));
                        if (!ChannelUtils.hasUtmProperties(_properties)) {
                            ChannelUtils.mergeUtmByMetaData(mContext, _properties);
                        }
                        if (!ChannelUtils.hasUtmProperties(_properties)) {
                            if (_properties.has("$oaid")) {
                                String oaid = _properties.optString("$oaid");
                                _properties.put("$channel_device_info",
                                        ChannelUtils.getDeviceInfo(mContext, mAndroidId, mSlConfigOptions.isSDKInitOAID, oaid));
                                SlLog.i(TAG, "properties has oaid " + oaid);
                            } else {
                                _properties.put("$channel_device_info",
                                        ChannelUtils.getDeviceInfo(mContext, mAndroidId, mSlConfigOptions.isSDKInitOAID));
                            }
                        }
                        if (_properties.has("$oaid")) {
                            _properties.remove("$oaid");
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }

                    // 先发送 track
                    trackEvent(EventType.TRACK, eventName, _properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void track(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject _properties = ChannelUtils.checkOrSetChannelCallbackEvent(getConfigOptions().isAutoAddChannelCallbackEvent, eventName, properties, mContext);
                    trackEvent(EventType.TRACK, eventName, _properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void track(final String eventName) {
        track(eventName, null);
    }

    /**
     * shenle add click中间
     *
     * @param eventName  事件名称
     * @param properties 事件属性
     */
    void trackClick(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName  事件名称
     * @param properties 事件属性
     */
    void trackInternal(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Deprecated
    @Override
    public void trackTimer(final String eventName) {
        trackTimer(eventName, TimeUnit.MILLISECONDS);
    }

    @Deprecated
    @Override
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, new EventTimer(timeUnit, startTime));
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void removeTimer(final String eventName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.remove(eventName);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public String trackTimerStart(String eventName) {
        try {
            final String eventNameRegex = String.format("%s_%s_%s", eventName, UUID.randomUUID().toString().replace("-", "_"), "SlTimer");
            trackTimerBegin(eventNameRegex, TimeUnit.SECONDS);
            trackTimerBegin(eventName, TimeUnit.SECONDS);
            return eventNameRegex;
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 触发事件的暂停/恢复
     *
     * @param eventName 事件名称
     * @param isPause   设置是否暂停
     */
    private void trackTimerState(final String eventName, final boolean isPause) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        EventTimer eventTimer = mTrackTimer.get(eventName);
                        if (eventTimer != null && eventTimer.isPaused() != isPause) {
                            eventTimer.setTimerState(isPause, startTime);
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackTimerPause(String eventName) {
        trackTimerState(eventName, true);
    }

    @Override
    public void trackTimerResume(String eventName) {
        trackTimerState(eventName, false);
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName) {
        trackTimer(eventName);
    }

    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit) {
        trackTimer(eventName, timeUnit);
    }

    @Override
    public void trackTimerEnd(final String eventName, final JSONObject properties) {
        final long endTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                if (eventName != null) {
                    synchronized (mTrackTimer) {
                        EventTimer eventTimer = mTrackTimer.get(eventName);
                        if (eventTimer != null) {
                            eventTimer.setEndTime(endTime);
                        }
                    }
                }
                try {
                    JSONObject _properties = ChannelUtils.checkOrSetChannelCallbackEvent(getConfigOptions().isAutoAddChannelCallbackEvent, eventName, properties, mContext);
                    trackEvent(EventType.TRACK, eventName, _properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackTimerEnd(final String eventName) {
        trackTimerEnd(eventName, null);
    }

    @Override
    public void clearTrackTimer() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mTrackTimer) {
                        mTrackTimer.clear();
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    @Override
    public void clearReferrerWhenAppEnd() {
        mClearReferrerWhenAppEnd = true;
    }

    @Override
    public void clearLastScreenUrl() {
        if (mClearReferrerWhenAppEnd) {
            mLastScreenUrl = null;
        }
    }

    @Override
    @Deprecated
    public String getMainProcessName() {
        return mMainProcessName;
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    @Override
    @Deprecated
    public void trackViewScreen(final String url, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!TextUtils.isEmpty(url) || properties != null) {
                        JSONObject trackProperties = new JSONObject();
                        mLastScreenTrackProperties = properties;

                        if (mLastScreenUrl != null) {
                            trackProperties.put("$referrer", mLastScreenUrl);
                        }

                        trackProperties.put("$url", url);
                        mLastScreenUrl = url;
                        if (properties != null) {
                            SlDataUtils.mergeJSONObject(properties, trackProperties);
                        }
                        trackInternal("$AppViewScreen", trackProperties);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity == null) {
                        return;
                    }
                    JSONObject properties = AopUtil.buildTitleAndScreenName(activity);
                    trackViewScreen(SlDataUtils.getScreenUrl(activity), properties);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment)) &&
                !(appFragmentClass != null && appFragmentClass.isInstance(fragment)) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject properties = new JSONObject();
                    String screenName = fragment.getClass().getCanonicalName();

                    String title = null;

                    if (fragment.getClass().isAnnotationPresent(SlDataFragmentTitle.class)) {
                        SlDataFragmentTitle slDataFragmentTitle = fragment.getClass().getAnnotation(SlDataFragmentTitle.class);
                        if (slDataFragmentTitle != null) {
                            title = slDataFragmentTitle.title();
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        Activity activity = null;
                        try {
                            Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                            if (getActivityMethod != null) {
                                activity = (Activity) getActivityMethod.invoke(fragment);
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                        if (activity != null) {
                            if (TextUtils.isEmpty(title)) {
                                title = SlDataUtils.getActivityTitle(activity);
                            }
                            screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                        }
                    }

                    if (!TextUtils.isEmpty(title)) {
                        properties.put(AopConstants.TITLE, title);
                    }
                    properties.put("$screen_name", screenName);
                    if (fragment instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            SlDataUtils.mergeJSONObject(otherProperties, properties);
                        }
                    }
                    trackViewScreen(SlDataUtils.getScreenUrl(fragment), properties);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewAppClick(View view) {
        trackViewAppClick(view, null);
    }

    @Override
    public void trackViewAppClick(final View view, JSONObject properties) {
        if (view == null) {
            return;
        }
        if (properties == null) {
            properties = new JSONObject();
        }
        if (AopUtil.injectClickInfo(view, properties, true)) {
            trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        }
    }

    /**
     * App 进入后台，遍历 mTrackTimer
     * eventAccumulatedDuration =
     * eventAccumulatedDuration + System.currentTimeMillis() - startTime - SessionIntervalTime
     */
    void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        if ("$AppEnd".equals(entry.getKey().toString())) {
                            continue;
                        }
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null && !eventTimer.isPaused()) {
                            long eventAccumulatedDuration =
                                    eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime() - getSessionIntervalTime();
                            eventTimer.setEventAccumulatedDuration(eventAccumulatedDuration);
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SlLog.i(TAG, "appEnterBackground error:" + e.getMessage());
            }
        }
    }

    /**
     * App 从后台恢复，遍历 mTrackTimer
     * startTime = System.currentTimeMillis()
     */
    void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SlLog.i(TAG, "appBecomeActive error:" + e.getMessage());
            }
        }
    }

    @Override
    public void flush() {
        mMessages.flush();
    }

    @Override
    public void flushSync() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mMessages.flush();
            }
        });
    }

    @Override
    public void registerDynamicSuperProperties(SlDataDynamicSuperProperties dynamicSuperProperties) {
        mDynamicSuperProperties = dynamicSuperProperties;
    }

    @Override
    public void setTrackEventCallBack(SlDataTrackEventCallBack trackEventCallBack) {
        mTrackEventCallBack = trackEventCallBack;
    }

    @Override
    public void setDeepLinkCallback(SlDataDeepLinkCallback deepLinkCallback) {
        mDeepLinkCallback = deepLinkCallback;
    }

    @Override
    public void stopTrackThread() {
        if (mTrackTaskManagerThread != null && !mTrackTaskManagerThread.isStopped()) {
            mTrackTaskManagerThread.stop();
            SlLog.i(TAG, "Data collection thread has been stopped");
        }
    }

    @Override
    public void startTrackThread() {
        if (mTrackTaskManagerThread == null || mTrackTaskManagerThread.isStopped()) {
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread).start();
            SlLog.i(TAG, "Data collection thread has been started");
        }
    }

    @Override
    public void deleteAll() {
        mMessages.deleteAll();
    }

    @Override
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            try {
                return new JSONObject(mSuperProperties.get().toString());
            } catch (JSONException e) {
                SlLog.printStackTrace(e);
                return new JSONObject();
            }
        }
    }

    @Override
    public void registerSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null) {
                return;
            }
            assertPropertyTypes(superProperties);
            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                SlDataUtils.mergeSuperJSONObject(superProperties, properties);
                mSuperProperties.commit(properties);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void unregisterSuperProperty(String superPropertyName) {
        try {
            synchronized (mSuperProperties) {
                JSONObject superProperties = mSuperProperties.get();
                superProperties.remove(superPropertyName);
                mSuperProperties.commit(superProperties);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void clearSuperProperties() {
        synchronized (mSuperProperties) {
            mSuperProperties.commit(new JSONObject());
        }
    }

    @Override
    public void profileSet(final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET, null, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileSet(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileSetOnce(final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET_ONCE, null, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileSetOnce(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET_ONCE, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileIncrement(final Map<String, ? extends Number> properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject(properties), null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileIncrement(final String property, final Number value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileAppend(final String property, final String value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    append_values.put(value);
                    final JSONObject properties = new JSONObject();
                    properties.put(property, append_values);
                    trackEvent(EventType.PROFILE_APPEND, null, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileAppend(final String property, final Set<String> values) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    for (String value : values) {
                        append_values.put(value);
                    }
                    final JSONObject properties = new JSONObject();
                    properties.put(property, append_values);
                    trackEvent(EventType.PROFILE_APPEND, null, properties, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileUnset(final String property) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_UNSET, null, new JSONObject().put(property, true), null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileDelete() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_DELETE, null, null, null);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return mEnableNetworkRequest;
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {
        this.mEnableNetworkRequest = isRequest;
    }

    DebugMode getDebugMode() {
        return mDebugMode;
    }

    void setDebugMode(DebugMode debugMode) {
        mDebugMode = debugMode;
        if (debugMode == DebugMode.DEBUG_OFF) {
            enableLog(false);
            SlLog.setDebug(false);
            mServerUrl = mOriginServerUrl;
        } else {
            enableLog(true);
            SlLog.setDebug(true);
            setServerUrl(mOriginServerUrl);
        }
    }

    String getServerUrl() {
        return mServerUrl;
    }

    @Override
    public void setServerUrl(String serverUrl) {
        try {
            mOriginServerUrl = serverUrl;
            if (TextUtils.isEmpty(serverUrl)) {
                mServerUrl = serverUrl;
                SlLog.i(TAG, "Server url is null or empty.");
                return;
            }

            Uri serverURI = Uri.parse(serverUrl);
            String hostServer = serverURI.getHost();
            if (!TextUtils.isEmpty(hostServer) && hostServer.contains("_")) {
                SlLog.i(TAG, "Server url " + serverUrl + " contains '_' is not recommend，" +
                        "see details: https://en.wikipedia.org/wiki/Hostname");
            }

            if (mDebugMode != DebugMode.DEBUG_OFF) {
                String uriPath = serverURI.getPath();
                if (TextUtils.isEmpty(uriPath)) {
                    return;
                }

                int pathPrefix = uriPath.lastIndexOf('/');
                if (pathPrefix != -1) {
                    String newPath = uriPath.substring(0, pathPrefix) + "/debug";
                    // 将 URI Path 中末尾的部分替换成 '/debug'
                    mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
                }
            } else {
                mServerUrl = serverUrl;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private void showDebugModeWarning() {
        try {
            if (mDebugMode == DebugMode.DEBUG_OFF) {
                return;
            }
            if (TextUtils.isEmpty(getServerUrl())) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String info = null;
                    if (mDebugMode == DebugMode.DEBUG_ONLY) {
                        info = "现在您打开了 SlData SDK 的 'DEBUG_ONLY' 模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    } else if (mDebugMode == DebugMode.DEBUG_AND_TRACK) {
                        info = "现在您打开了神策 SlData SDK 的 'DEBUG_AND_TRACK' 模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    }
                    CharSequence appName = AppInfoUtils.getAppName(mContext);
                    if (!TextUtils.isEmpty(appName)) {
                        info = String.format(Locale.CHINA, "%s：%s", appName, info);
                    }
                    Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    @Override
    public void trackEventFromH5(String eventInfo, boolean enableVerify) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            if (enableVerify) {
                String serverUrl = eventObject.optString("server_url");
                if (!TextUtils.isEmpty(serverUrl)) {
                    if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                        return;
                    }
                } else {
                    //防止 H5 集成的 JS SDK 版本太老，没有发 server_url
                    return;
                }
            }
            trackEventFromH5(eventInfo);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    boolean _trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(eventInfo);

            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                    return false;
                }
                trackEventFromH5(eventInfo);
                return true;
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return false;

    }

    @Override
    public void trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            eventObject.put("_hybrid_h5", true);
            String type = eventObject.getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase(Locale.getDefault()));

            String distinctIdKey = "distinct_id";
            if (eventType == EventType.TRACK_SIGNUP) {
                eventObject.put("original_id", getAnonymousId());
            } else if (!TextUtils.isEmpty(getLoginId())) {
                eventObject.put(distinctIdKey, getLoginId());
            } else {
                eventObject.put(distinctIdKey, getAnonymousId());
            }
            eventObject.put("anonymous_id", getAnonymousId());
            long eventTime = System.currentTimeMillis();
            eventObject.put("time", eventTime);

            try {
                SecureRandom secureRandom = new SecureRandom();
                eventObject.put("_track_id", secureRandom.nextInt());
            } catch (Exception e) {
                //ignore
            }

            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            // 校验 H5 属性
            assertPropertyTypes(propertiesObject);
            if (propertiesObject == null) {
                propertiesObject = new JSONObject();
            }

            JSONObject libObject = eventObject.optJSONObject("lib");
            if (libObject != null) {
                if (mDeviceInfo.containsKey("$app_version")) {
                    libObject.put("$app_version", mDeviceInfo.get("$app_version"));
                }

                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libObject.put("$app_version", superProperties.get("$app_version"));
                    }
                }
            }

            if (eventType.isTrack()) {
                if (mDeviceInfo != null) {
                    for (Map.Entry<String, Object> entry : mDeviceInfo.entrySet()) {
                        String key = entry.getKey();
                        if (!TextUtils.isEmpty(key)) {
                            if ("$lib".equals(key) || "$lib_version".equals(key)) {
                                continue;
                            }
                            propertiesObject.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                // 当前网络状况
                String networkType = NetworkUtils.networkType(mContext);
                propertiesObject.put("$wifi", "WIFI".equals(networkType));
                propertiesObject.put("$network_type", networkType);

                // SuperProperties
                synchronized (mSuperProperties) {
                    JSONObject superProperties = mSuperProperties.get();
                    SlDataUtils.mergeJSONObject(superProperties, propertiesObject);
                }

                try {
                    if (mDynamicSuperProperties != null) {
                        JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
                        if (dynamicSuperProperties != null) {
                            assertPropertyTypes(dynamicSuperProperties);
                            SlDataUtils.mergeJSONObject(dynamicSuperProperties, propertiesObject);
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }

                //是否首日访问
                if (eventType.isTrack()) {
                    propertiesObject.put("$is_first_day", isFirstDay(eventTime));
                }
                SlDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), propertiesObject);
            }

            if (eventObject.has("_nocache")) {
                eventObject.remove("_nocache");
            }

            if (eventObject.has("server_url")) {
                eventObject.remove("server_url");
            }

            if (propertiesObject.has("$project")) {
                eventObject.put("project", propertiesObject.optString("$project"));
                propertiesObject.remove("$project");
            }

            if (propertiesObject.has("$token")) {
                eventObject.put("token", propertiesObject.optString("$token"));
                propertiesObject.remove("$token");
            }

            if (propertiesObject.has("$time")) {
                try {
                    long time = propertiesObject.getLong("$time");
                    if (TimeUtils.isDateValid(time)) {
                        eventObject.put("time", time);
                    }
                } catch (Exception ex) {
                    SlLog.printStackTrace(ex);
                }
                propertiesObject.remove("$time");
            }

            String eventName = eventObject.optString("event");
            if (eventType.isTrack()) {
                // 校验 H5 事件名称
                assertKey(eventName);
                boolean enterDb = isEnterDb(eventName, propertiesObject);
                if (!enterDb) {
                    SlLog.d(TAG, eventName + " event can not enter database");
                    return;
                }
            }
            eventObject.put("properties", propertiesObject);

            if (eventType == EventType.TRACK_SIGNUP) {
                String loginId = eventObject.getString("distinct_id");
                synchronized (mLoginIdLock) {
                    if (!loginId.equals(DbAdapter.getInstance().getLoginId()) && !loginId.equals(getAnonymousId())) {
                        DbAdapter.getInstance().commitLoginId(loginId);
                        eventObject.put("login_id", loginId);
                        mMessages.enqueueEventMessage(type, eventObject);
                        if (SlLog.isLogEnabled()) {
                            SlLog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventObject.toString()));
                        }
                    }
                }
            } else {
                if (!TextUtils.isEmpty(getLoginId())) {
                    eventObject.put("login_id", getLoginId());
                }
                mMessages.enqueueEventMessage(type, eventObject);
                if (SlLog.isLogEnabled()) {
                    SlLog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventObject.toString()));
                }
            }
        } catch (Exception e) {
            //ignore
            SlLog.printStackTrace(e);
        }
    }

    /**
     * @param eventName       事件名
     * @param eventProperties 事件属性
     * @return 该事件是否入库
     */
    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mTrackEventCallBack != null) {
            SlLog.d(TAG, "SDK have set trackEvent callBack");
            try {
                enterDb = mTrackEventCallBack.onTrackEvent(eventName, eventProperties);
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }
            if (enterDb) {
                try {
                    Iterator<String> it = eventProperties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        try {
                            assertKey(key);
                        } catch (Exception e) {
                            SlLog.printStackTrace(e);
                            return false;
                        }
                        Object value = eventProperties.opt(key);
                        if (!(value instanceof CharSequence || value instanceof Number || value
                                instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                            SlLog.d(TAG, String.format("The property value must be an instance of " +
                                    "CharSequence/Number/Boolean/JSONArray. [key='%s', value='%s']", key, value == null ? "" : value.toString()));
                            return false;
                        }

                        if ("app_crashed_reason".equals(key)) {
                            if (value instanceof String && ((String) value).length() > 8191 * 2) {
                                SlLog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191 * 2) + "$";
                            }
                        } else {
                            if (value instanceof String && ((String) value).length() > 8191) {
                                SlLog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191) + "$";
                            }
                        }
                        if (value instanceof Date) {
                            eventProperties.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                        } else {
                            eventProperties.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        }
        return enterDb;
    }

    void trackEvent(final EventType eventType, String eventName, final JSONObject properties, final String
            originalDistinctId) {
        try {
            EventTimer eventTimer = null;
            if (!TextUtils.isEmpty(eventName)) {
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(eventName);
                    mTrackTimer.remove(eventName);
                }

                if (eventName.endsWith("_SlTimer") && eventName.length() > 45) {// Timer 计时交叉计算拼接的字符串长度 45
                    eventName = eventName.substring(0, eventName.length() - 45);
                }
            }

            if (eventType.isTrack()) {
                assertKey(eventName);
            }
            assertPropertyTypes(properties);

            try {
                JSONObject sendProperties;

                if (eventType.isTrack()) {
                    sendProperties = new JSONObject(mDeviceInfo);

                    //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                    try {
                        if (TextUtils.isEmpty(sendProperties.optString("$carrier"))) {
                            String carrier = SlDataUtils.getCarrier(mContext);
                            if (!TextUtils.isEmpty(carrier)) {
                                sendProperties.put("$carrier", carrier);
                            }
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }
                    if (!"$AppEnd".equals(eventName)) {
                        //合并 $latest_utm 属性
                        SlDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), sendProperties);
                    }

                    synchronized (mSuperProperties) {
                        JSONObject superProperties = mSuperProperties.get();
                        SlDataUtils.mergeJSONObject(superProperties, sendProperties);
                    }

                    try {
                        if (mDynamicSuperProperties != null) {
                            JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
                            if (dynamicSuperProperties != null) {
                                assertPropertyTypes(dynamicSuperProperties);
                                SlDataUtils.mergeJSONObject(dynamicSuperProperties, sendProperties);
                            }
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }

                    // 当前网络状况
                    String networkType = NetworkUtils.networkType(mContext);
                    sendProperties.put("$wifi", "WIFI".equals(networkType));
                    sendProperties.put("$network_type", networkType);

                    // GPS
                    try {
                        if (mGPSLocation != null) {
                            sendProperties.put("$latitude", mGPSLocation.getLatitude());
                            sendProperties.put("$longitude", mGPSLocation.getLongitude());
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }

                    // 屏幕方向
                    try {
                        String screenOrientation = getScreenOrientation();
                        if (!TextUtils.isEmpty(screenOrientation)) {
                            sendProperties.put("$screen_orientation", screenOrientation);
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }
                } else if (eventType.isProfile()) {
                    sendProperties = new JSONObject();
                } else {
                    return;
                }

                String libDetail = null;
                String lib_version = VERSION;
                String app_version = mDeviceInfo.containsKey("$app_version") ? (String) mDeviceInfo.get("$app_version") : "";
                long eventTime = System.currentTimeMillis();
                if (null != properties) {
                    try {
                        if (properties.has("$lib_detail")) {
                            libDetail = properties.getString("$lib_detail");
                            properties.remove("$lib_detail");
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }

                    try {
                        if ("$AppEnd".equals(eventName)) {
                            long appEndTime = properties.getLong("event_time");
                            eventTime = appEndTime > 0 ? appEndTime : eventTime;
                            String appEnd_lib_version = properties.optString("$lib_version");
                            String appEnd_app_version = properties.optString("$app_version");
                            if (!TextUtils.isEmpty(appEnd_lib_version)) {
                                lib_version = appEnd_lib_version;
                            } else {
                                properties.remove("$lib_version");
                            }

                            if (!TextUtils.isEmpty(appEnd_app_version)) {
                                app_version = appEnd_app_version;
                            } else {
                                properties.remove("$app_version");
                            }

                            properties.remove("event_time");
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }
                    SlDataUtils.mergeJSONObject(properties, sendProperties);
                }

                if (null != eventTimer) {
                    try {
                        Double duration = Double.valueOf(eventTimer.duration());
                        if (duration > 0) {
                            sendProperties.put("event_duration", duration);
                        }
                    } catch (Exception e) {
                        //ignore
                        SlLog.printStackTrace(e);
                    }
                }

                JSONObject libProperties = new JSONObject();
                libProperties.put("$lib", "Android");
                libProperties.put("$lib_version", lib_version);
                libProperties.put("$app_version", app_version);

                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libProperties.put("$app_version", superProperties.get("$app_version"));
                    }
                }

                final JSONObject dataObj = new JSONObject();

                try {
                    SecureRandom random = new SecureRandom();
                    dataObj.put("_track_id", random.nextInt());
                } catch (Exception e) {
                    // ignore
                }

                dataObj.put("time", eventTime);
                dataObj.put("type", eventType.getEventType());

                try {
                    if (sendProperties.has("$project")) {
                        dataObj.put("project", sendProperties.optString("$project"));
                        sendProperties.remove("$project");
                    }

                    if (sendProperties.has("$token")) {
                        dataObj.put("token", sendProperties.optString("$token"));
                        sendProperties.remove("$token");
                    }

                    if (sendProperties.has("$time")) {
                        try {
                            Object timeDate = sendProperties.opt("$time");
                            if (timeDate instanceof Date) {
                                if (TimeUtils.isDateValid((Date) timeDate)) {
                                    dataObj.put("time", ((Date) timeDate).getTime());
                                }
                            }
                        } catch (Exception ex) {
                            SlLog.printStackTrace(ex);
                        }
                        sendProperties.remove("$time");
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }

                dataObj.put("distinct_id", getDistinctId());
                if (!TextUtils.isEmpty(getLoginId())) {
                    dataObj.put("login_id", getLoginId());
                }
                dataObj.put("anonymous_id", getAnonymousId());
                dataObj.put("lib", libProperties);

                if (eventType == EventType.TRACK) {
                    dataObj.put("event", eventName);
                    //是否首日访问
                    sendProperties.put("$is_first_day", isFirstDay(eventTime));
                } else if (eventType == EventType.TRACK_SIGNUP) {
                    dataObj.put("event", eventName);
                    dataObj.put("original_id", originalDistinctId);
                }

                libProperties.put("$lib_method", "code");

                if (mAutoTrack && properties != null) {
                    if (AutoTrackEventType.isAutoTrackType(eventName)) {
                        AutoTrackEventType trackEventType = AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
                        if (trackEventType != null) {
                            if (!isAutoTrackEventTypeIgnored(trackEventType)) {
                                if (properties.has("$screen_name")) {
                                    String screenName = properties.getString("$screen_name");
                                    if (!TextUtils.isEmpty(screenName)) {
                                        String[] screenNameArray = screenName.split("\\|");
                                        if (screenNameArray.length > 0) {
                                            libDetail = String.format("%s##%s##%s##%s", screenNameArray[0], "", "", "");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (TextUtils.isEmpty(libDetail)) {
                    StackTraceElement[] trace = (new Exception()).getStackTrace();
                    if (trace.length > 1) {
                        StackTraceElement traceElement = trace[0];
                        libDetail = String.format("%s##%s##%s##%s", traceElement
                                        .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                                traceElement.getLineNumber());
                    }
                }

                libProperties.put("$lib_detail", libDetail);

                //防止用户自定义事件以及公共属性可能会加$device_id属性，导致覆盖sdk原始的$device_id属性值
                if (sendProperties.has("$device_id")) {//由于profileSet等类型事件没有$device_id属性，故加此判断
                    if (mDeviceInfo.containsKey("$device_id")) {
                        sendProperties.put("$device_id", mDeviceInfo.get("$device_id"));
                    }
                }
                if (eventType.isTrack()) {
                    boolean isEnterDb = isEnterDb(eventName, sendProperties);
                    if (!isEnterDb) {
                        SlLog.d(TAG, eventName + " event can not enter database");
                        return;
                    }
                }
                dataObj.put("properties", sendProperties);

                if (mEventListenerList != null && eventType.isTrack()) {
                    for (SlEventListener eventListener : mEventListenerList) {
                        eventListener.trackEvent(dataObj);
                    }
                }

                mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
                if (SlLog.isLogEnabled()) {
                    SlLog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
                }
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    boolean isMultiProcess() {
        return mSlConfigOptions.mEnableMultiProcess;
    }

    private boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            String current = mIsFirstDayDateFormat.format(eventTime);
            return firstDay.equals(current);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
        return true;
    }

    private void trackItemEvent(String itemType, String itemId, String eventType, JSONObject properties) {
        try {
            assertKey(itemType);
            assertValue(itemId);
            assertPropertyTypes(properties);

            String eventProject = null;
            if (properties != null && properties.has("$project")) {
                eventProject = (String) properties.get("$project");
                properties.remove("$project");
            }

            JSONObject libProperties = new JSONObject();
            libProperties.put("$lib", "Android");
            libProperties.put("$lib_version", VERSION);
            libProperties.put("$lib_method", "code");

            if (mDeviceInfo.containsKey("$app_version")) {
                libProperties.put("$app_version", mDeviceInfo.get("$app_version"));
            }

            JSONObject superProperties = mSuperProperties.get();
            if (superProperties != null) {
                if (superProperties.has("$app_version")) {
                    libProperties.put("$app_version", superProperties.get("$app_version"));
                }
            }

            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                String libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
                if (!TextUtils.isEmpty(libDetail)) {
                    libProperties.put("$lib_detail", libDetail);
                }
            }

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("item_type", itemType);
            eventProperties.put("item_id", itemId);
            eventProperties.put("type", eventType);
            eventProperties.put("time", System.currentTimeMillis());
            eventProperties.put("properties", TimeUtils.formatDate(properties));
            eventProperties.put("lib", libProperties);

            if (!TextUtils.isEmpty(eventProject)) {
                eventProperties.put("project", eventProject);
            }
            mMessages.enqueueEventMessage(eventType, eventProperties);
            SlLog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventProperties.toString()));
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
    }

    private void initSlConfig(String serverURL, String packageName) {
        Bundle configBundle = null;
        try {
            final ApplicationInfo appInfo = mContext.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            configBundle = appInfo.metaData;
        } catch (final PackageManager.NameNotFoundException e) {
            SlLog.printStackTrace(e);
        }

        if (null == configBundle) {
            configBundle = new Bundle();
        }

        if (mSlConfigOptions == null) {
            this.mSDKConfigInit = false;
            mSlConfigOptions = new SlConfigOptions(serverURL);
        } else {
            this.mSDKConfigInit = true;
        }

        if (mSlConfigOptions.mInvokeLog) {
            enableLog(mSlConfigOptions.mLogEnabled);
        } else {
            enableLog(configBundle.getBoolean("com.sldata.analytics.android.EnableLogging",
                    this.mDebugMode != DebugMode.DEBUG_OFF));
        }

        setServerUrl(serverURL);

        if (mSlConfigOptions.mEnableTrackAppCrash) {
            SlDataExceptionHandler.enableAppCrash();
        }

        if (mSlConfigOptions.mFlushInterval == 0) {
            mSlConfigOptions.setFlushInterval(configBundle.getInt("com.sldata.analytics.android.FlushInterval",
                    15000));
        }

        if (mSlConfigOptions.mFlushBulkSize == 0) {
            mSlConfigOptions.setFlushBulkSize(configBundle.getInt("com.sldata.analytics.android.FlushBulkSize",
                    100));
        }

        if (mSlConfigOptions.mMaxCacheSize == 0) {
            mSlConfigOptions.setMaxCacheSize(32 * 1024 * 1024L);
        }

        this.mAutoTrack = configBundle.getBoolean("com.sldata.analytics.android.AutoTrack",
                false);
        if (mSlConfigOptions.mAutoTrackEventType != 0) {
            enableAutoTrack(mSlConfigOptions.mAutoTrackEventType);
            this.mAutoTrack = true;
        }

        if (!mSlConfigOptions.mInvokeHeatMapEnabled) {
            mSlConfigOptions.mHeatMapEnabled = configBundle.getBoolean("com.sldata.analytics.android.HeatMap",
                    false);
        }

        if (!mSlConfigOptions.mInvokeHeatMapConfirmDialog) {
            mSlConfigOptions.mHeatMapConfirmDialogEnabled = configBundle.getBoolean("com.sldata.analytics.android.EnableHeatMapConfirmDialog",
                    true);
        }

        if (!mSlConfigOptions.mInvokeVisualizedEnabled) {
            mSlConfigOptions.mVisualizedEnabled = configBundle.getBoolean("com.sldata.analytics.android.VisualizedAutoTrack",
                    false);
        }

        if (!mSlConfigOptions.mInvokeVisualizedConfirmDialog) {
            mSlConfigOptions.mVisualizedConfirmDialogEnabled = configBundle.getBoolean("com.sldata.analytics.android.EnableVisualizedAutoTrackConfirmDialog",
                    true);
        }

        enableTrackScreenOrientation(mSlConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mSlConfigOptions.mAnonymousId)) {
            identify(mSlConfigOptions.mAnonymousId);
        }

        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean("com.sldata.analytics.android.ShowDebugInfoView",
                true);

        this.mDisableDefaultRemoteConfig = configBundle.getBoolean("com.sldata.analytics.android.DisableDefaultRemoteConfig",
                true);

        this.mMainProcessName = AppInfoUtils.getMainProcessName(mContext);
        if (TextUtils.isEmpty(this.mMainProcessName)) {
            this.mMainProcessName = configBundle.getString("com.sldata.analytics.android.MainProcessName");
        }
        mIsMainProcess = AppInfoUtils.isMainProcess(mContext, mMainProcessName);

        this.mDisableTrackDeviceId = configBundle.getBoolean("com.sldata.analytics.android.DisableTrackDeviceId",
                false);
        if (isSaveDeepLinkInfo()) {
            ChannelUtils.loadUtmByLocal(mContext);
        } else {
            ChannelUtils.clearLocalUtm(mContext);
        }
    }

    private void applySlConfigOptions() {
        if (mSlConfigOptions.mEnableTrackAppCrash) {
            SlDataExceptionHandler.enableAppCrash();
        }

        if (mSlConfigOptions.mAutoTrackEventType != 0) {
            this.mAutoTrack = true;
        }

        if (mSlConfigOptions.mInvokeLog) {
            enableLog(mSlConfigOptions.mLogEnabled);
        }

        enableTrackScreenOrientation(mSlConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mSlConfigOptions.mAnonymousId)) {
            identify(mSlConfigOptions.mAnonymousId);
        }
    }

    @Override
    public void profilePushId(final String pushTypeKey, final String pushId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(pushTypeKey);
                    if (TextUtils.isEmpty(pushId)) {
                        SlLog.d(TAG, "pushId is empty");
                        return;
                    }
                    String distinctId = getDistinctId();
                    String distinctPushId = distinctId + pushId;
                    SharedPreferences sp = SlDataUtils.getSharedPreferences(mContext);
                    String spDistinctPushId = sp.getString("distinctId_" + pushTypeKey, "");
                    if (!spDistinctPushId.equals(distinctPushId)) {
                        profileSet(pushTypeKey, pushId);
                        sp.edit().putString("distinctId_" + pushTypeKey, distinctPushId).apply();
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileUnsetPushId(final String pushTypeKey) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(pushTypeKey);
                    String distinctId = getDistinctId();
                    SharedPreferences sp = SlDataUtils.getSharedPreferences(mContext);
                    String key = "distinctId_" + pushTypeKey;
                    String spDistinctPushId = sp.getString(key, "");

                    if (spDistinctPushId.startsWith(distinctId)) {
                        profileUnset(pushTypeKey);
                        sp.edit().remove(key).apply();
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void itemSet(final String itemType, final String itemId, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                trackItemEvent(itemType, itemId, EventType.ITEM_SET.getEventType(), properties);
            }
        });
    }

    @Override
    public void itemDelete(final String itemType, final String itemId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                trackItemEvent(itemType, itemId, EventType.ITEM_DELETE.getEventType(), null);
            }
        });
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory;
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        mSSLSocketFactory = sf;
    }

    public void addEventListener(SlEventListener eventListener) {
        try {
            if (this.mEventListenerList == null) {
                this.mEventListenerList = new ArrayList<>();
            }
            this.mEventListenerList.add(eventListener);
        } catch (Exception ex) {
            SlLog.printStackTrace(ex);
        }
    }

    /**
     * 生命周期
     *
     * @return
     */
    public void setSlLifeCallEventListener(SlLifeCallEventListener eventListener) {
        this.mLifeCallEventListener = eventListener;
    }

    public SlLifeCallEventListener getLifeCallEventListener() {
        return mLifeCallEventListener;
    }

    SlConfigOptions getConfigOptions() {
        return mSlConfigOptions;
    }

    Context getContext() {
        return mContext;
    }

    boolean isSaveDeepLinkInfo() {
        return mSlConfigOptions.mEnableSaveDeepLinkInfo;
    }

    SlDataDeepLinkCallback getDeepLinkCallback() {
        return mDeepLinkCallback;
    }

    /**
     * Debug 模式，用于检验数据导入是否正确。该模式下，事件会逐条实时发送到 Sl Analytics，并根据返回值检查
     * 数据导入是否正确。
     * Debug 模式的具体使用方式，请参考:
     * http://www.sldata.cn/manual/debug_mode.html
     * Debug 模式有三种：
     * DEBUG_OFF - 关闭DEBUG模式
     * DEBUG_ONLY - 打开DEBUG模式，但该模式下发送的数据仅用于调试，不进行数据导入
     * DEBUG_AND_TRACK - 打开DEBUG模式，并将数据导入到SlAnalytics中
     */
    public enum DebugMode {
        DEBUG_OFF(false, false),
        DEBUG_ONLY(true, false),
        DEBUG_AND_TRACK(true, true);

        private final boolean debugMode;
        private final boolean debugWriteData;

        DebugMode(boolean debugMode, boolean debugWriteData) {
            this.debugMode = debugMode;
            this.debugWriteData = debugWriteData;
        }

        boolean isDebugMode() {
            return debugMode;
        }

        boolean isDebugWriteData() {
            return debugWriteData;
        }
    }

    /**
     * AutoTrack 默认采集的事件类型
     */
    public enum AutoTrackEventType {
        APP_START(1),
        APP_END(1 << 1),
        APP_CLICK(1 << 2),
        APP_VIEW_SCREEN(1 << 3);
        private final int eventValue;

        AutoTrackEventType(int eventValue) {
            this.eventValue = eventValue;
        }

        static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty(eventName)) {
                return null;
            }

            switch (eventName) {
                case "$AppStart":
                    return APP_START;
                case "$AppEnd":
                    return APP_END;
                case "$AppClick":
                    return APP_CLICK;
                case "$AppViewScreen":
                    return APP_VIEW_SCREEN;
                default:
                    break;
            }

            return null;
        }

        static boolean isAutoTrackType(String eventName) {
            if (!TextUtils.isEmpty(eventName)) {
                switch (eventName) {
                    case "$AppStart":
                    case "$AppEnd":
                    case "$AppClick":
                    case "$AppViewScreen":
                        return true;
                    default:
                        break;
                }
            }
            return false;
        }

        int getEventValue() {
            return eventValue;
        }
    }

    /**
     * 网络类型
     */
    public final class NetworkType {
        public static final int TYPE_NONE = 0;//NULL
        public static final int TYPE_2G = 1;//2G
        public static final int TYPE_3G = 1 << 1;//3G
        public static final int TYPE_4G = 1 << 2;//4G
        public static final int TYPE_WIFI = 1 << 3;//WIFI
        public static final int TYPE_5G = 1 << 4;//5G
        public static final int TYPE_ALL = 0xFF;//ALL
    }
}