package baseandroid.sl.sdk.analytics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import baseandroid.sl.sdk.analytics.util.SlLog;

@SuppressLint("NewApi")
public class AppStateManager implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "AppStateManager";
    private volatile static AppStateManager mSingleton = null;

    private int mActivityCount;

    private AppStateManager() {
    }

    private WeakReference<Activity> mForeGroundActivity = new WeakReference((Object) null);
    private int mCurrentRootWindowsHashCode = -1;
    private String mCurrentFragmentName = null;

    public static AppStateManager getInstance() {
        if (mSingleton == null) {
            synchronized (AppStateManager.class) {
                if (mSingleton == null) {
                    mSingleton = new AppStateManager();
                }
            }
        }
        return mSingleton;
    }


    public Activity getForegroundActivity() {
        return this.mForeGroundActivity.get();
    }

    private void setForegroundActivity(Activity activity) {
        this.mForeGroundActivity = new WeakReference(activity);
    }

    void setFragmentScreenName(Object fragment, String fragmentScreenName) {
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(fragment);
                // 如果存在 fragment 多层嵌套场景，只取父 fragment
                if (parentFragment == null) {
                    mCurrentFragmentName = fragmentScreenName;
                    SlLog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is not nested fragment and set");
                } else {
                    SlLog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is nested fragment and ignored");
                }
            }
        } catch (Exception e) {
            //ignored
        }
    }

    public String getFragmentScreenName() {
        return mCurrentFragmentName;
    }

    public boolean isInBackground() {
        return mActivityCount <= 0;
    }

    public int getCurrentRootWindowsHashCode() {
        if (this.mCurrentRootWindowsHashCode == -1 && this.mForeGroundActivity != null && this.mForeGroundActivity.get() != null) {
            this.mCurrentRootWindowsHashCode = (this.mForeGroundActivity.get()).getWindow().getDecorView().hashCode();
        }

        return this.mCurrentRootWindowsHashCode;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        setForegroundActivity(activity);
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForegroundActivity(activity);
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = activity.getWindow().getDecorView().hashCode();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivityCount--;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
