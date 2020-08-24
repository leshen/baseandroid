
package baseandroid.sl.sdk.analytics.network;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;


import org.json.JSONException;
import org.json.JSONObject;

import baseandroid.sl.sdk.analytics.util.SlLog;

public abstract class HttpCallback<T> {
    static Handler sMainHandler = new Handler(Looper.getMainLooper());

    void onError(final RealResponse response) {
        final String errorMessage;
        if (!TextUtils.isEmpty(response.result)) {
            errorMessage = response.result;
        } else if (!TextUtils.isEmpty(response.errorMsg)) {
            errorMessage = response.errorMsg;
        } else if (response.exception != null) {
            errorMessage = response.exception.getMessage();
        } else {
            errorMessage = "unknown error";
        }
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onFailure(response.code, errorMessage);
                onAfter();
            }
        });
    }

    void onSuccess(RealResponse response) {
        final T obj;
        obj = onParseResponse(response.result);
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onResponse(obj);
                onAfter();
            }
        });
    }

    /**
     * 解析 Response，执行在子线程
     *
     * @param result 网络请求返回信息
     * @return T
     */
    public abstract T onParseResponse(String result);

    /**
     * 访问网络失败后被调用，执行在 UI 线程
     *
     * @param code 请求返回的错误 code
     * @param errorMessage 错误信息
     */
    public abstract void onFailure(int code, String errorMessage);

    /**
     * 访问网络成功后被调用，执行在 UI 线程
     *
     * @param response 处理后的对象
     */
    public abstract void onResponse(T response);

    /**
     * 访问网络成功或失败后调用
     */
    public abstract void onAfter();

    public static abstract class StringCallback extends HttpCallback<String> {
        @Override
        public String onParseResponse(String result) {
            return result;
        }
    }

    public static abstract class JsonCallback extends HttpCallback<JSONObject> {
        @Override
        public JSONObject onParseResponse(String result) {
            try {
                return new JSONObject(result);
            } catch (JSONException e) {
                SlLog.printStackTrace(e);
                return null;
            }
        }
    }
}