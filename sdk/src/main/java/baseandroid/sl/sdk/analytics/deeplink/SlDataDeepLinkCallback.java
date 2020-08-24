package baseandroid.sl.sdk.analytics.deeplink;

/**
 * DeepLink Callback
 */
public interface SlDataDeepLinkCallback {
    /**
     * @param params 链接设置的 App 内参数
     * @param success 是否请求成功
     * @param appAwakePassedTime 请求时长
     */
    void onReceive(String params, boolean success, long appAwakePassedTime);
}