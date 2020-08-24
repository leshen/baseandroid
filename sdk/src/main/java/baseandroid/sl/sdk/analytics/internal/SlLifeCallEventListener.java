package baseandroid.sl.sdk.analytics.internal;

import android.app.Activity;

/**
 * 这个是另一种逻辑，就是不统计返回的跳转
 * Created by shenle on 2020/8/14.
 */
public interface SlLifeCallEventListener {
    //回退到上一页activity
    void onResumeActivity(Activity activity);

    void onPauseParentFragment(Object fragment);

    void onUpActivity(Object Activity);

    void onUpFragment(Object fragment);
}
