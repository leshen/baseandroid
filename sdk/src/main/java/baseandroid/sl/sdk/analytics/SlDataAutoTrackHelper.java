package baseandroid.sl.sdk.analytics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import baseandroid.sl.sdk.analytics.internal.ScreenAutoTracker;
import baseandroid.sl.sdk.analytics.util.AopUtil;
import baseandroid.sl.sdk.analytics.util.NetworkUtils;
import baseandroid.sl.sdk.analytics.util.SlDataUtils;
import baseandroid.sl.sdk.analytics.util.SlLog;
import baseandroid.sl.sdk.analytics.util.ViewUtil;
import baseandroid.sl.sdk.analytics.util.WindowHelper;
import baseandroid.sl.sdk.analytics.visual.HeatMapService;
import baseandroid.sl.sdk.analytics.visual.VisualizedAutoTrackService;
import baseandroid.sl.sdk.analytics.visual.WebViewVisualInterface;
import baseandroid.sl.sdk.analytics.visual.util.VisualUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import baseandroid.sl.sdk.R;

import static baseandroid.sl.sdk.analytics.util.Base64Coder.CHARSET_UTF8;

@SuppressWarnings("unused")
public class SlDataAutoTrackHelper {
    private static final String TAG = "SlDataAutoTrackHelper";
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        boolean isDeBounceTrack = false;
        long currentOnClickTimestamp = System.currentTimeMillis();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                isDeBounceTrack = true;
            }
        }

        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return isDeBounceTrack;
    }

    private static void traverseView(final String fragmentName, final ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }
            if (root == null) {
                return;
            }
            final int childCount = root.getChildCount();
            if (childCount == 0 && ViewUtil.instanceOfRecyclerView(root)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            int width = root.getWidth();
                            int height = root.getHeight();
                            if (width > 0 && height > 0) {
                                setFragmentTag(fragmentName, root);
                            }
                        }
                    };
                    final ViewTreeObserver.OnScrollChangedListener onScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            setFragmentTag(fragmentName, root);
                        }
                    };
                    root.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
                    root.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener);
                    root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            root.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                            root.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangedListener);
                        }
                    });
                }
            } else {
                setFragmentTag(fragmentName, root);
            }
        } catch (Exception e) {
            //ignored
        }
    }

    private static void setFragmentTag(final String fragmentName, final ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); ++i) {
            final View child = root.getChildAt(i);
            child.setTag(R.id.sl_analytics_tag_view_fragment_name, fragmentName);
            if (child instanceof ViewGroup && !(child instanceof ListView ||
                    child instanceof GridView ||
                    child instanceof Spinner ||
                    child instanceof RadioGroup)) {
                traverseView(fragmentName, (ViewGroup) child);
            }
        }
    }

    private static boolean isFragment(Object object) {
        try {
            if (object == null) {
                return false;
            }
            Class<?> supportFragmentClass = null;
            Class<?> androidXFragmentClass = null;
            Class<?> fragment = null;
            try {
                fragment = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            if (supportFragmentClass == null && androidXFragmentClass == null && fragment == null) {
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object)) ||
                    (fragment != null && fragment.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            if (!isFragment(object)) {
                return;
            }

            //Fragment名称
            String fragmentName = object.getClass().getName();
            rootView.setTag(R.id.sl_analytics_tag_view_fragment_name, fragmentName);

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            }
            trackFragmentResume(object);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }
//
//    public static void trackRN(Object target, int reactTag, int s, boolean b) {
//        try {
//            if (!SlDataAPI.sharedInstance().isReactNativeAutoTrackEnabled()) {
//                return;
//            }
//
//            //关闭 AutoTrack
//            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
//                return;
//            }
//
//            //$AppClick 被过滤
//            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
//                return;
//            }
//
//            JSONObject properties = new JSONObject();
//            properties.put(AopConstants.ELEMENT_TYPE, "RNView");
//            if (target != null) {
//                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
//                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
//                if (resolveViewMethod != null) {
//                    Object object = resolveViewMethod.invoke(target, reactTag);
//                    if (object != null) {
//                        View view = (View) object;
//                        //获取所在的 Context
//                        Context context = view.getContext();
//
//                        //将 Context 转成 Activity
//                        Activity activity = AopUtil.getActivityFromContext(context, view);
//                        //$screen_name & $title
//                        if (activity != null) {
//                            SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
//                            AopUtil.addViewPathProperties(activity, view, properties);
//                        }
//                        if (view instanceof CompoundButton) {//ReactSwitch
//                            return;
//                        }
//                        if (view instanceof TextView) {
//                            TextView textView = (TextView) view;
//                            if (!(view instanceof EditText) && !TextUtils.isEmpty(textView.getText())) {
//                                properties.put(AopConstants.ELEMENT_CONTENT, textView.getText().toString());
//                            }
//                        } else if (view instanceof ViewGroup) {
//                            StringBuilder stringBuilder = new StringBuilder();
//                            String viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
//                            if (!TextUtils.isEmpty(viewText)) {
//                                viewText = viewText.substring(0, viewText.length() - 1);
//                            }
//                            properties.put(AopConstants.ELEMENT_CONTENT, viewText);
//                        }
//                    }
//                }
//            }
//            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
//        } catch (Exception e) {
//            SlLog.printStackTrace(e);
//        }
//    }

    private static void trackFragmentAppViewScreen(Object fragment) {
        try {
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                return;
            }

            if (!SlDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())
                    || "com.bumptech.glide.manager.RequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

            boolean isAutoTrackFragment = SlDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
            if (!isAutoTrackFragment) {
                return;
            }
            if (SlDataAPI.sharedInstance().getLifeCallEventListener() != null) {
                SlDataAPI.sharedInstance().getLifeCallEventListener().onUpFragment(fragment);
            }
//            else {
                JSONObject properties = new JSONObject();
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, null);
                AppStateManager.getInstance().setFragmentScreenName(fragment, properties.optString(AopConstants.SCREEN_NAME));
                if (fragment instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        SlDataUtils.mergeJSONObject(otherProperties, properties);
                    }
                    properties.put("$isScreenAutoTracker", "1");
                }
                SlDataAPI.sharedInstance().trackViewScreen(SlDataUtils.getScreenUrl(fragment), properties);
//            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    //shenle自己加的先放着
    public static void trackFragmentPause(Object object) {
    }

    //shenle插件里的逻辑改了放在onViewCreated
    public static void trackFragmentResume(Object object) {
        if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            return;
        }

        if (!SlDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(object);
                if (parentFragment == null) {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)) {
                        trackFragmentAppViewScreen(object);
                    }
                } else {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object) && !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        } catch (Exception e) {
            //ignored
        }
    }

    private static boolean fragmentGetUserVisibleHint(Object fragment) {
        try {
            Method getUserVisibleHintMethod = fragment.getClass().getMethod("getUserVisibleHint");
            if (getUserVisibleHintMethod != null) {
                return (boolean) getUserVisibleHintMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    private static boolean fragmentIsHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            if (isHiddenMethod != null) {
                return (boolean) isHiddenMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            return;
        }

        if (!SlDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (isVisibleToUser) {
                if (fragmentIsResumed(object)) {
                    if (!fragmentIsHidden(object)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            } else {
                //返回我不想再统计一次
                if (fragmentIsResumed(object)) {
                    if (SlDataAPI.sharedInstance().getLifeCallEventListener() != null) {
                        SlDataAPI.sharedInstance().getLifeCallEventListener().onPauseParentFragment(object);
                    }
                }
            }
        } else {
            if (isVisibleToUser && fragmentGetUserVisibleHint(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (!fragmentIsHidden(object) && !fragmentIsHidden(parentFragment)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            } else {
                //返回我不想再统计一次
                if (!isVisibleToUser&&fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (SlDataAPI.sharedInstance().getLifeCallEventListener() != null) {
                        SlDataAPI.sharedInstance().getLifeCallEventListener().onPauseParentFragment(object);
                    }
                }
            }
        }
    }

    private static boolean fragmentIsResumed(Object fragment) {
        try {
            Method isResumedMethod = fragment.getClass().getMethod("isResumed");
            if (isResumedMethod != null) {
                return (boolean) isResumedMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            return;
        }

        if (!SlDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (!hidden) {
                if (fragmentIsResumed(object)) {
                    if (fragmentGetUserVisibleHint(object)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        } else {
            if (!hidden && !fragmentIsHidden(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (fragmentGetUserVisibleHint(object) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        }
    }

    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
            if (expandableListView == null || view == null) {
                return;
            }

            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(expandableListView);

            // fragment 忽略
            if (fragment != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            // ExpandableListView Type 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            // View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            // $screen_name & $title
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            // ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }
            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof SlExpandableListViewItemTrackProperties) {
                    try {
                        SlExpandableListViewItemTrackProperties trackProperties = (SlExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getSlGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            AopUtil.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        SlLog.printStackTrace(e);
                    }
                }
            }

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
            if (expandableListView == null || view == null) {
                return;
            }

            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, expandableListView);

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(expandableListView);

            // fragment 忽略
            if (fragment != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //ExpandableListView 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            //获取 View 自定义属性
            JSONObject properties = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof SlExpandableListViewItemTrackProperties) {
                    SlExpandableListViewItemTrackProperties trackProperties = (SlExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getSlChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //$screen_name & $title
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }
            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);

        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackTabHost(String tabName) {
        try {
            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //TabHost 被忽略
            if (AopUtil.isViewIgnored(TabHost.class)) {
                return;
            }

            JSONObject properties = new JSONObject();
            String elementContent = null;
            // 2020/4/27 新增  1. 解决 TabHost 点击取不到 element_content 2. 可视化增加 $element_path
            View view = WindowHelper.getClickView(tabName);
            if (view != null) {
                Context context = view.getContext();
                if (context == null) {
                    return;
                }
                Activity activity = null;
                if (context instanceof Activity) {
                    activity = (Activity) context;
                }
                if (activity != null) {
                    if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                        return;
                    }
                    SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);

                    Object fragment = AopUtil.getFragmentFromView(view);
                    if (fragment != null) {
                        if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                            return;
                        }
                        AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
                    }

                    AopUtil.addViewPathProperties(activity, view, properties);
                }
                elementContent = ViewUtil.getViewContentAndType(view).getViewContent();
            }
            if (TextUtils.isEmpty(elementContent)) {
                elementContent = tabName;
            }
            properties.put(AopConstants.ELEMENT_CONTENT, elementContent);
            properties.put(AopConstants.ELEMENT_TYPE, "TabHost");
            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            if (tab == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            Class<?> supportTabLayoutCLass = null;
            Class<?> androidXTabLayoutCLass = null;
            try {
                supportTabLayoutCLass = Class.forName("android.support.design.widget.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabLayoutCLass = Class.forName("com.google.android.material.tabs.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabLayoutCLass == null && androidXTabLayoutCLass == null) {
                return;
            }

            //TabLayout 被忽略
            if (supportTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(supportTabLayoutCLass)) {
                    return;
                }
            }
            if (androidXTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(androidXTabLayoutCLass)) {
                    return;
                }
            }

            if (isDeBounceTrack(tab)) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            boolean isFragment = false;
            if (object instanceof Context) {
                activity = AopUtil.getActivityFromContext((Context) object, null);
            } else {
                try {
                    Field[] fields = object.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object bridgeObject = field.get(object);
                        if (bridgeObject instanceof Activity) {
                            activity = (Activity) bridgeObject;
                            break;
                        } else if (isFragment(bridgeObject)) {
                            object = bridgeObject;
                            isFragment = true;
                            break;
                        } else if (bridgeObject instanceof View) {
                            View view = (View) bridgeObject;
                            activity = AopUtil.getActivityFromContext(view.getContext(), null);
                        }
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (isFragment) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(object.getClass())) {
                    return;
                }
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (isFragment) {
                activity = AopUtil.getActivityFromFragment(object);
                AopUtil.getScreenNameAndTitleFromFragment(properties, object, activity);
            } else if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            Class<?> supportTabClass = null;
            Class<?> androidXTabClass = null;
            Class<?> currentTabClass;
            try {
                supportTabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabClass = Class.forName("com.google.android.material.tabs.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabClass != null) {
                currentTabClass = supportTabClass;
            } else {
                currentTabClass = androidXTabClass;
            }

            if (currentTabClass != null) {
                Method method = null;
                try {
                    method = currentTabClass.getMethod("getText");
                } catch (NoSuchMethodException e) {
                    //ignored
                }

                if (method != null) {
                    Object text = method.invoke(tab);

                    //Content
                    if (text != null) {
                        properties.put(AopConstants.ELEMENT_CONTENT, text);
                    }
                }

                if (activity != null) {
                    try {
                        Field field;
                        try {
                            field = currentTabClass.getDeclaredField("mCustomView");
                        } catch (NoSuchFieldException ex) {
                            try {
                                field = currentTabClass.getDeclaredField("customView");
                            } catch (NoSuchFieldException e) {
                                field = null;
                            }
                        }

                        View view = null;
                        if (field != null) {
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                            if (view != null) {
                                try {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    String viewText;
                                    if (view instanceof ViewGroup) {
                                        viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                                        if (!TextUtils.isEmpty(viewText)) {
                                            viewText = viewText.substring(0, viewText.length() - 1);
                                        }
                                    } else {
                                        viewText = AopUtil.getViewText(view);
                                    }

                                    if (!TextUtils.isEmpty(viewText)) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                                    }
                                } catch (Exception e) {
                                    SlLog.printStackTrace(e);
                                }
                            }
                        }

                        View tabView = null;
                        try {
                            Field viewField = currentTabClass.getDeclaredField("view");
                            viewField.setAccessible(true);
                            try {
                                tabView = (View) viewField.get(tab);
                            } catch (IllegalAccessException e) {
                                SlLog.printStackTrace(e);
                            }

                        } catch (NoSuchFieldException e) {
                            SlLog.printStackTrace(e);
                        }
                        if (tabView == null) {
                            try {
                                Field mViewField = currentTabClass.getDeclaredField("mView");
                                mViewField.setAccessible(true);
                                try {
                                    tabView = (View) mViewField.get(tab);
                                } catch (IllegalAccessException e) {
                                    SlLog.printStackTrace(e);
                                }
                            } catch (NoSuchFieldException e) {
                                SlLog.printStackTrace(e);
                            }
                        }
                        if (tabView != null) {
                            AopUtil.addViewPathProperties(activity, tabView, properties);
                        }

                        if (view == null || view.getId() == -1) {
                            try {
                                field = currentTabClass.getDeclaredField("mParent");
                            } catch (NoSuchFieldException ex) {
                                field = currentTabClass.getDeclaredField("parent");
                            }
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                        }

                        if (view != null && view.getId() != View.NO_ID) {
                            String resourceId = activity.getResources().getResourceEntryName(view.getId());
                            if (!TextUtils.isEmpty(resourceId)) {
                                properties.put(AopConstants.ELEMENT_ID, resourceId);
                            }
                        }
                    } catch (Exception e) {
                        SlLog.printStackTrace(e);
                    }
                }
            }

            //Type
            properties.put(AopConstants.ELEMENT_TYPE, "TabLayout");

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackMenuItem(MenuItem menuItem) {
        trackMenuItem(null, menuItem);
    }

    public static void trackMenuItem(Object object, MenuItem menuItem) {
        try {
            if (menuItem == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //MenuItem 被忽略
            if (AopUtil.isViewIgnored(MenuItem.class)) {
                return;
            }

            if (isDeBounceTrack(menuItem)) {
                return;
            }

            Context context = null;
            if (object != null) {
                if (object instanceof Context) {
                    context = (Context) object;
                }
            }

            View view = WindowHelper.getClickView(menuItem);
            if (context == null && view != null) {
                context = view.getContext();
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context != null) {
                activity = AopUtil.getActivityFromContext(context, null);
            }

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //获取View ID
            String idString = null;
            try {
                if (context != null) {
                    idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //ViewID
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            // 2020/4/27 新增  1. 解决 Actionbar 返回按钮 获取不到 $element_content
            String elementContent = null;
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                elementContent = menuItem.getTitle().toString();
            }

            if (view != null) {
                if (TextUtils.isEmpty(elementContent)) {
                    elementContent = ViewUtil.getViewContentAndType(view).getViewContent();
                }
                AopUtil.addViewPathProperties(activity, view, properties);
            }
            properties.put(AopConstants.ELEMENT_CONTENT, elementContent);
            //Type
            properties.put(AopConstants.ELEMENT_TYPE, "MenuItem");

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        try {
            if (view == null) {
                return;
            }

            View childView = view.findViewById(checkedId);
            if (childView == null || !childView.isPressed()) {
                return;
            }

            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(view);

            // fragment 忽略
            if (fragment != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            String viewType = "RadioButton";
            if (childView != null) {
                viewType = AopUtil.getViewType(childView.getClass().getCanonicalName(), "RadioButton");
            }
            properties.put(AopConstants.ELEMENT_TYPE, viewType);

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (activity != null) {
                try {
                    RadioButton radioButton = activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                            }
                        }
                        AopUtil.addViewPathProperties(activity, radioButton, properties);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        try {
            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null) {
                return;
            }

            if (isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //Dialog 被忽略
            if (AopUtil.isViewIgnored(Dialog.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

            try {
                if (dialog.getWindow() != null) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.sl_analytics_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }

            //$screen_name & $title
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //由于 RN 中 dialog 未屏蔽，直接走到原生，导致 dialog screen_name 取的是原生的。
            VisualUtil.mergeRnScreenNameAndTitle(properties);

            properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

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

            if (supportAlertDialogClass == null && androidXAlertDialogClass == null) {
                return;
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                    AopUtil.addViewPathProperties(activity, button, properties);
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                            }
                        }
                        View clickView = listView.getChildAt(whichButton);
                        if (clickView != null) {
                            AopUtil.addViewPathProperties(activity, clickView, properties);
                        }
                    }
                }

            } else if (currentAlertDialogClass.isInstance(dialog)) {
                Button button = null;
                try {
                    Method getButtonMethod = dialog.getClass().getMethod("getButton", int.class);
                    if (getButtonMethod != null) {
                        button = (Button) getButtonMethod.invoke(dialog, whichButton);
                    }
                } catch (Exception e) {
                    //ignored
                }

                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                    AopUtil.addViewPathProperties(activity, button, properties);
                } else {
                    try {
                        Method getListViewMethod = dialog.getClass().getMethod("getListView");
                        if (getListViewMethod != null) {
                            ListView listView = (ListView) getListViewMethod.invoke(dialog);
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, object);
                                    }
                                }
                                View clickView = listView.getChildAt(whichButton);
                                if (clickView != null) {
                                    AopUtil.addViewPathProperties(activity, clickView, properties);
                                }
                            }
                        }
                    } catch (Exception e) {
                        //ignored
                    }
                }
            }

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        try {
            //防止 Spinner 恢复数据时造成的空指针问题.
            if (view == null) {
                return;
            }
            //闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(adapterView);

            // fragment 忽略
            if (fragment != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(adapterView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            if (adapterView instanceof ListView) {
                properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                if (AopUtil.isViewIgnored(ListView.class)) {
                    return;
                }
            } else if (adapterView instanceof GridView) {
                properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                if (AopUtil.isViewIgnored(GridView.class)) {
                    return;
                }
            } else if (adapterView instanceof Spinner) {
                properties.put(AopConstants.ELEMENT_TYPE, "Spinner");
                if (AopUtil.isViewIgnored(Spinner.class)) {
                    return;
                }
            }

            //ViewId
            String idString = AopUtil.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }

            if (adapter instanceof SlAdapterViewItemTrackProperties) {
                try {
                    SlAdapterViewItemTrackProperties objectProperties = (SlAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getSlItemTrackProperties(position);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    SlLog.printStackTrace(e);
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //Activity 名称和页面标题
            if (activity != null) {
                SlDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sl_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackDrawerOpened(View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Open");

            SlDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackDrawerClosed(View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Close");

            SlDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void trackViewOnClick(View view) {
        if (view == null) {
            return;
        }
        trackViewOnClick(view, view.isPressed());
    }

    public static void trackViewOnClick(View view, boolean isFromUser) {
        try {
            if (view == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SlDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }
            //$AppClick 被过滤
            if (SlDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SlDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(view);

            // fragment 忽略
            if (fragment != null) {
                if (SlDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            if (SlDataUtils.isDoubleClick(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            if (AopUtil.injectClickInfo(view, properties, isFromUser)) {
                SlDataAPI.sharedInstance().trackInternal(AopConstants.APP_CLICK_EVENT_NAME, properties);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void track(String eventName, String properties) {
        try {
            if (TextUtils.isEmpty(eventName)) {
                return;
            }
            JSONObject pro = null;
            if (!TextUtils.isEmpty(properties)) {
                try {
                    pro = new JSONObject(properties);
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
            SlDataAPI.sharedInstance().trackInternal(eventName, pro);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    public static void handleSchemeUrl(Activity activity, Intent intent) {
        try {
            Uri uri = null;
            if (activity != null && intent != null) {
                uri = intent.getData();
            }
            if (uri != null) {
                String host = uri.getHost();
                if ("heatmap".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    showOpenHeatMapDialog(activity, featureCode, postUrl);
                    intent.setData(null);
                } else if ("debugmode".equals(host)) {
                    String infoId = uri.getQueryParameter("info_id");
                    showDebugModeSelectDialog(activity, infoId);
                    intent.setData(null);
                } else if ("visualized".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    String serverUrl = SlDataAPI.sharedInstance().getServerUrl();
                    String visualizedProject = null, serverProject = null;
                    if (!TextUtils.isEmpty(postUrl)) {
                        Uri visualizedUri = Uri.parse(postUrl);
                        if (visualizedUri != null) {
                            visualizedProject = visualizedUri.getQueryParameter("project");
                        }
                    }
                    if (!TextUtils.isEmpty(serverUrl)) {
                        Uri serverUri = Uri.parse(serverUrl);
                        if (serverUri != null) {
                            serverProject = serverUri.getQueryParameter("project");
                        }
                    }
                    if (!TextUtils.isEmpty(visualizedProject) && !TextUtils.isEmpty(serverProject) && TextUtils.equals(visualizedProject, serverProject)
                    ) {
                        showOpenVisualizedAutoTrackDialog(activity, featureCode, postUrl);
                    } else {
                        showDialog(activity, "App 集成的项目与电脑浏览器打开的项目不同，无法进行可视化全埋点。");
                    }
                    intent.setData(null);
                } else if ("popupwindow".equals(host)) {
                    showPopupWindowDialog(activity, uri);
                    intent.setData(null);
                } else if ("encrypt".equals(host)) {
                    String version = uri.getQueryParameter("v");
                    String key = Uri.decode(uri.getQueryParameter("key"));
                    SlLog.d(TAG, "Encrypt, version = " + version + ", key = " + key);
                    String tip;
                    if (TextUtils.isEmpty(version) || TextUtils.isEmpty(key)) {
                        tip = "密钥验证不通过，所选密钥无效";
                    } else if (SlDataAPI.sharedInstance().mSlDataEncrypt != null) {
                        tip = SlDataAPI.sharedInstance().mSlDataEncrypt.checkRSlSecretKey(version, key);
                    } else {
                        tip = "当前 App 未开启加密，请开启加密后再试";
                    }
                    Toast.makeText(activity, tip, Toast.LENGTH_LONG).show();
                    intent.setData(null);
                }
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static void showPopupWindowDialog(Activity activity, Uri uri) {
        try {
            Class<?> clazz = Class.forName("com.sldata.sf.ui.utils.PreviewUtil");
            String sfPopupTest = uri.getQueryParameter("sf_popup_test");
            String popupWindowId = uri.getQueryParameter("popup_window_id");
            boolean isSfPopupTest = false;
            if (!TextUtils.isEmpty(sfPopupTest)) {
                isSfPopupTest = Boolean.parseBoolean(sfPopupTest);
            }
            Method method = clazz.getDeclaredMethod("showPreview", Context.class, boolean.class, String.class);
            method.invoke(null, activity, isSfPopupTest, popupWindowId);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static void showDebugModeSelectDialog(final Activity activity, final String infoId) {
        try {
            DebugModeSelectDialog dialog = new DebugModeSelectDialog(activity, SlDataAPI.sharedInstance().getDebugMode());
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnDebugModeDialogClickListener(new DebugModeSelectDialog.OnDebugModeViewClickListener() {
                @Override
                public void onCancel(Dialog dialog) {
                    dialog.cancel();
                }

                @Override
                public void setDebugMode(Dialog dialog, SlDataAPI.DebugMode debugMode) {
                    SlDataAPI.sharedInstance().setDebugMode(debugMode);
                    dialog.cancel();
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //如果当前的调试模式不是 DebugOff ,则发送匿名或登录 ID 给服务端
                    String serverUrl = SlDataAPI.sharedInstance().getServerUrl();
                    SlDataAPI.DebugMode mCurrentDebugMode = SlDataAPI.sharedInstance().getDebugMode();
                    if (SlDataAPI.sharedInstance().isNetworkRequestEnable() && !TextUtils.isEmpty(serverUrl) && !TextUtils.isEmpty(infoId) && mCurrentDebugMode != SlDataAPI.DebugMode.DEBUG_OFF) {
                        new SendDebugIdThread(serverUrl, SlDataAPI.sharedInstance().getDistinctId(), infoId, ThreadNameConstants.THREAD_SEND_DISTINCT_ID).start();
                    }
                    String currentDebugToastMsg = "";
                    if (mCurrentDebugMode == SlDataAPI.DebugMode.DEBUG_OFF) {
                        currentDebugToastMsg = "已关闭调试模式，请重新扫描二维码进行开启";
                    } else if (mCurrentDebugMode == SlDataAPI.DebugMode.DEBUG_ONLY) {
                        currentDebugToastMsg = "开启调试模式，校验数据，但不进行数据导入；关闭 App 进程后，将自动关闭调试模式";
                    } else if (mCurrentDebugMode == SlDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        currentDebugToastMsg = "开启调试模式，校验数据，并将数据导入到神策分析中；关闭 App 进程后，将自动关闭调试模式";
                    }
                    Toast.makeText(activity, currentDebugToastMsg, Toast.LENGTH_LONG).show();
                    SlLog.info(TAG, "您当前的调试模式是：" + mCurrentDebugMode, null);
                }
            });
            dialog.show();
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static void showOpenHeatMapDialog(final Activity context, final String featureCode, final String postUrl) {
        try {
            if (!SlDataAPI.sharedInstance().isNetworkRequestEnable()) {
                showDialog(context, "已关闭网络请求（NetworkRequest），无法使用 App 点击分析，请开启后再试！");
                return;
            }
            if (!SlDataAPI.sharedInstance().isAppHeatMapConfirmDialogEnabled()) {
                HeatMapService.getInstance().start(context, featureCode, postUrl);
                return;
            }

            boolean isWifi = false;
            try {
                String networkType = NetworkUtils.networkType(context);
                if ("WIFI".equals(networkType)) {
                    isWifi = true;
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("提示");
            if (isWifi) {
                builder.setMessage("正在连接 App 点击分析...");
            } else {
                builder.setMessage("正在连接 App 点击分析，建议在 WiFi 环境下使用。");
            }
            builder.setCancelable(false);
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HeatMapService.getInstance().start(context, featureCode, postUrl);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static void showOpenVisualizedAutoTrackDialog(final Activity context, final String featureCode, final String postUrl) {
        try {
            if (!SlDataAPI.sharedInstance().isNetworkRequestEnable()) {
                showDialog(context, "已关闭网络请求（NetworkRequest），无法使用 App 可视化全埋点，请开启后再试！");
                return;
            }
            if (!SlDataAPI.sharedInstance().isVisualizedAutoTrackEnabled()) {
                showDialog(context, "SDK 没有被正确集成，请联系贵方技术人员开启可视化全埋点。");
                return;
            }
            if (!SlDataAPI.sharedInstance().isVisualizedAutoTrackConfirmDialogEnabled()) {
                VisualizedAutoTrackService.getInstance().start(context, featureCode, postUrl);
                return;
            }
            boolean isWifi = false;
            try {
                String networkType = NetworkUtils.networkType(context);
                if ("WIFI".equals(networkType)) {
                    isWifi = true;
                }
            } catch (Exception e) {
                // ignore
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("提示");
            if (isWifi) {
                builder.setMessage("正在连接 App 可视化全埋点...");
            } else {
                builder.setMessage("正在连接 App 可视化全埋点，建议在 WiFi 环境下使用。");
            }
            builder.setCancelable(false);
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VisualizedAutoTrackService.getInstance().start(context, featureCode, postUrl);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            }
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static void showDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("确定", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        try {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    private static class SendDebugIdThread extends Thread {
        private String distinctId;
        private String infoId;
        private String serverUrl;

        SendDebugIdThread(String serverUrl, String distinctId, String infoId, String name) {
            super(name);
            this.distinctId = distinctId;
            this.infoId = infoId;
            this.serverUrl = serverUrl;
        }

        @Override
        public void run() {
            super.run();
            sendHttpRequest(serverUrl, false);
        }

        private void sendHttpRequest(String serverUrl, boolean isRedirects) {
            ByteArrayOutputStream out = null;
            OutputStream out2 = null;
            BufferedOutputStream bout = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(String.format(serverUrl + "&info_id=%s", infoId));
                SlLog.info(TAG, String.format("DebugMode URL:%s", url), null);
                connection = (HttpURLConnection) url.openConnection();
                if (connection == null) {
                    SlLog.info(TAG, String.format("can not connect %s,shouldn't happen", url.toString()), null);
                    return;
                }
                SSLSocketFactory sf = SlDataAPI.sharedInstance().getSSLSocketFactory();
                if (sf != null && connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sf);
                }
                connection.setInstanceFollowRedirects(false);
                out = new ByteArrayOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out);
                String requestBody = "{\"distinct_id\": \"" + distinctId + "\"}";
                writer.write(requestBody);
                writer.flush();
                SlLog.info(TAG, String.format("DebugMode request body : %s", requestBody), null);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "text/plain");

                out2 = connection.getOutputStream();
                bout = new BufferedOutputStream(out2);
                bout.write(out.toString().getBytes(CHARSET_UTF8));
                bout.flush();
                out.close();
                int responseCode = connection.getResponseCode();
                SlLog.info(TAG, String.format(Locale.CHINA, "DebugMode 后端的响应码是:%d", responseCode), null);
                if (!isRedirects && SlDataHttpURLConnectionHelper.needRedirects(responseCode)) {
                    String location = SlDataHttpURLConnectionHelper.getLocation(connection, serverUrl);
                    if (!TextUtils.isEmpty(location)) {
                        closeStream(out, out2, bout, connection);
                        sendHttpRequest(location, true);
                    }
                }
            } catch (Exception e) {
                SlLog.printStackTrace(e);
            } finally {
                closeStream(out, out2, bout, connection);
            }
        }

        private void closeStream(ByteArrayOutputStream out, OutputStream out2, BufferedOutputStream bout, HttpURLConnection connection) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
            if (out2 != null) {
                try {
                    out2.close();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
            if (bout != null) {
                try {
                    bout.close();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    SlLog.printStackTrace(e);
                }
            }
        }
    }

    public static void loadUrl(View webView, String url) {
        if (webView == null) {
            throw new NullPointerException("WebView has not initialized.");
        }
        setupH5Bridge(webView);
        invokeWebViewLoad(webView, "loadUrl", new Object[]{url}, new Class[]{String.class});
    }

    public static void loadUrl(View webView, String url, Map<String, String> additionalHttpHeaders) {
        if (webView == null) {
            throw new NullPointerException("WebView has not initialized.");
        }
        setupH5Bridge(webView);
        invokeWebViewLoad(webView, "loadUrl", new Object[]{url, additionalHttpHeaders}, new Class[]{String.class, Map.class});
    }

    public static void loadData(View webView, String data, String mimeType, String encoding) {
        if (webView == null) {
            throw new NullPointerException("WebView has not initialized.");
        }
        setupH5Bridge(webView);
        invokeWebViewLoad(webView, "loadData", new Object[]{data, mimeType, encoding}, new Class[]{String.class, String.class, String.class});
    }

    public static void loadDataWithBaseURL(View webView, String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (webView == null) {
            throw new NullPointerException("WebView has not initialized.");
        }
        setupH5Bridge(webView);
        invokeWebViewLoad(webView, "loadDataWithBaseURL", new Object[]{baseUrl, data, mimeType, encoding, historyUrl},
                new Class[]{String.class, String.class, String.class, String.class, String.class});
    }

    public static void postUrl(View webView, String url, byte[] postData) {
        if (webView == null) {
            throw new NullPointerException("WebView has not initialized.");
        }
        setupH5Bridge(webView);
        invokeWebViewLoad(webView, "postUrl", new Object[]{url, postData},
                new Class[]{String.class, byte[].class});
    }

    private static void setupH5Bridge(View webView) {
        if (SlDataAPI.sharedInstance() instanceof SlDataAPIEmptyImplementation) {
            return;
        }
        if (isSupportJellyBean() && SlDataAPI.sharedInstance().getConfigOptions() != null && SlDataAPI.sharedInstance().getConfigOptions().isAutoTrackWebView) {
            setupWebView(webView);
        }
        if (isSupportJellyBean()) {
            addWebViewVisualInterface(webView);
        }
    }

    private static void invokeWebViewLoad(View webView, String methodName, Object[] params, Class[] paramTypes) {
        try {
            Class<?> clazz = webView.getClass();
            Method loadMethod = clazz.getMethod(methodName, paramTypes);
            loadMethod.invoke(webView, params);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }

    static void addWebViewVisualInterface(View webView) {
        if (webView != null && webView.getTag(R.id.sl_analytics_tag_view_webview_visual) == null) {
            webView.setTag(R.id.sl_analytics_tag_view_webview_visual, new Object());
            addJavascriptInterface(webView, new WebViewVisualInterface(webView), "SlData_App_Visual_Bridge");
        }
    }

    private static boolean isSupportJellyBean() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 && !SlDataAPI.sharedInstance().getConfigOptions().isWebViewSupportJellyBean) {
            SlLog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return false;
        }
        return true;
    }

    private static void setupWebView(View webView) {
        if (webView != null && webView.getTag(R.id.sl_analytics_tag_view_webview) == null) {
            webView.setTag(R.id.sl_analytics_tag_view_webview, new Object());
            addJavascriptInterface(webView, new AppWebViewInterface(SlDataAPI.sharedInstance().getContext(), null, false), "SlData_APP_New_H5_Bridge");
        }
    }

    private static void addJavascriptInterface(View webView, Object obj, String interfaceName) {
        try {
            Class<?> clazz = webView.getClass();
            try {
                Method getSettingsMethod = clazz.getMethod("getSettings");
                Object settings = getSettingsMethod.invoke(webView);
                if (settings != null) {
                    Method setJavaScriptEnabledMethod = settings.getClass().getMethod("setJavaScriptEnabled", boolean.class);
                    setJavaScriptEnabledMethod.invoke(settings, true);
                }
            } catch (Exception e) {
                //ignore
            }
            Method addJSMethod = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            addJSMethod.invoke(webView, obj, interfaceName);
        } catch (Exception e) {
            SlLog.printStackTrace(e);
        }
    }
}