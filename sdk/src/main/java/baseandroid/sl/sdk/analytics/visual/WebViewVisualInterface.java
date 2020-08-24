package baseandroid.sl.sdk.analytics.visual;

import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import java.lang.ref.WeakReference;

import baseandroid.sl.sdk.analytics.util.ReflectUtil;
import baseandroid.sl.sdk.analytics.util.SlLog;

public class WebViewVisualInterface {

    private static final String TAG = "Sl.Visual.WebViewVisualInterface";
    private WeakReference<View> mWebView;

    public WebViewVisualInterface(View webView) {
        this.mWebView = new WeakReference(webView);
    }

    /**
     * JS 给 App 提供 H5 页面数据（只有当 Sldata_visualized_mode = true 时 JS 才返回数据）
     *
     * @param msg H5 页面数据
     */
    @JavascriptInterface
    public void Sldata_hover_web_nodes(final String msg) {
        try {
            SlLog.i(TAG, "Sldata_hover_web_nodes msg: " + msg);
            WebNodesManager.getInstance().handlerMessage(msg);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    /**
     * 提供给 JS 判断当前是否正在使用可视化埋点
     *
     * @return true 表示正在进行可视化埋点
     */
    @JavascriptInterface
    public boolean Sldata_visualized_mode() {
        return VisualizedAutoTrackService.getInstance().isVisualizedAutoTrackRunning();
    }


    @JavascriptInterface
    public void Sldata_visualized_alert_info(final String msg) {
        try {
            SlLog.i(TAG, "Sldata_visualized_alert_info msg: " + msg);
            if (mWebView.get() != null) {
                mWebView.get().post(new Runnable() {
                    @Override
                    public void run() {
                        String url = ReflectUtil.callMethod(mWebView.get(), "getUrl");
                        if (!TextUtils.isEmpty(url)) {
                            SlLog.i(TAG, "Sldata_visualized_alert_info url: " + url);
                            WebNodesManager.getInstance().handlerFailure(url, msg);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

}
