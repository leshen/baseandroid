package tools.shenle.slbaseandroid.tool

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import java.util.*

/**
 * Created by fhf11991 on 2016/7/18.
 */
object ActivityLifecycleHelper : ActivityLifecycleCallbacks {
    private fun handleActivity(activity: Activity) {
//        if (activity is HasSupportFragmentInjector) {
//            AndroidInjection.inject(activity)
//        }
//        if (activity is FragmentActivity) {
//            activity.supportFragmentManager
//                .registerFragmentLifecycleCallbacks(
//                    object : FragmentLifecycleCallbacks() {
//                        fun onFragmentCreated(
//                            fm: FragmentManager?, f: Fragment?,
//                            savedInstanceState: Bundle?
//                        ) {
//                            if (f is Injectable) {
//                                AndroidSupportInjection.inject(f)
//                            }
//                        }
//                    }, true
//                )
//        }
    }

    override fun onActivityCreated(activity: Activity,savedInstanceState: Bundle?) {
        handleActivity(activity)
        addActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activities.contains(activity)) {
            activities.remove(activity)
        }
    }

    /**
     * 添加Activity到堆栈
     */
    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    fun finishAll() {
        for (i in activities) {
            i.finish()
        }
    }

    private val activities = LinkedList<Activity>()

    /**
     * 获取集合中当前Activity
     *
     * @return
     */
    val latestActivity: Activity?
        get() {
            val count = activities.size
            return if (count == 0) {
                null
            } else activities[count - 1]
        }

    /**
     * 获取集合中上一个Activity
     *
     * @return
     */
    val previousActivity: Activity?
        get() {
            val count = activities.size
            return if (count < 2) {
                null
            } else activities[count - 2]
        }
}