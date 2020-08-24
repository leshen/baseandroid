package tools.shenle.baseandroid

import baseandroid.sl.sdk.analytics.SlAnalyticsAutoTrackEventType
import baseandroid.sl.sdk.analytics.SlConfigOptions
import baseandroid.sl.sdk.analytics.SlDataAPI
import baseandroid.sl.sdk.analytics.internal.SlEventListener
import org.json.JSONObject
import org.koin.core.module.Module
import tools.shenle.slbaseandroid.BaseApplicationSl

/**
 * Created by shenle on 2020/3/27.
 */
class AppInstance : BaseApplicationSl() {
    override fun initApp() {

//        val SA_SERVER_URL = "http://10.10.1.78:8106/sa?project=EbizDemo"
//
//// 初始化配置
//        val saConfigOptions = SAConfigOptions(SA_SERVER_URL)
//// 开启全埋点
//        saConfigOptions.setAutoTrackEventType(
//            SensorsAnalyticsAutoTrackEventType.APP_CLICK or
//                    SensorsAnalyticsAutoTrackEventType.APP_START or
//                    SensorsAnalyticsAutoTrackEventType.APP_END or
//                    SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN
//        )
//            .enableLog(true) //开启 Log
//            .enableTrackAppCrash()//开启appcrash
//
//        /**
//         * 其他配置，如开启可视化全埋点
//         */
//// 需要在主线程初始化神策 SDK
//        /**
//         * 其他配置，如开启可视化全埋点
//         */
//        // 开启可视化全埋点
//        saConfigOptions.enableVisualizedAutoTrack(true)
//// 需要在主线程初始化神策 SDK
//        SensorsDataAPI.startWithConfigOptions(this, saConfigOptions)
////        SensorsDataAPI.sharedInstance().track("BuyProduct", properties);
//        SensorsDataAPI.sharedInstance().trackFragmentAppViewScreen()

        val SA_SERVER_URL = "http://10.10.1.78:8106/sa?project=EbizDemo"

// 初始化配置
        val saConfigOptions = SlConfigOptions(SA_SERVER_URL)
// 开启全埋点
        saConfigOptions.setAutoTrackEventType(
            SlAnalyticsAutoTrackEventType.APP_CLICK or
                    SlAnalyticsAutoTrackEventType.APP_START or
                    SlAnalyticsAutoTrackEventType.APP_END or
                    SlAnalyticsAutoTrackEventType.APP_VIEW_SCREEN
        )
            .enableLog(true) //开启 Log
            .enableTrackAppCrash()//开启appcrash

        /**
         * 其他配置，如开启可视化全埋点
         */
// 需要在主线程初始化神策 SDK
        /**
         * 其他配置，如开启可视化全埋点
         */
        // 开启可视化全埋点
        saConfigOptions.enableVisualizedAutoTrack(true)
// 需要在主线程初始化神策 SDK
        SlDataAPI.startWithConfigOptions(this, saConfigOptions)
//        SlDataAPI.sharedInstance().track("BuyProduct", properties);
        SlDataAPI.sharedInstance().trackFragmentAppViewScreen()
        SlDataAPI.sharedInstance().addEventListener(object :SlEventListener{
            override fun login() {
            }

            override fun identify() {
            }

            override fun resetAnonymousId() {
            }

            override fun logout() {
            }

            override fun trackEvent(jsonObject: JSONObject?) {

            }

        })
    }

    override fun initKoinModules(): List<Module> {
        return appModule
    }
}