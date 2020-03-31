package tools.shenle.slbaseandroid.swipeback

import android.app.Activity
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import tools.shenle.slbaseandroid.R
import tools.shenle.slbaseandroid.swipeback.SwipeBackHelper.SlideBackManager
import tools.shenle.slbaseandroid.tool.ActivityLifecycleHelper

open class SwipeBackActivity : AppCompatActivity(), SlideBackManager {
    private var mSwipeBackHelper: SwipeBackHelper? = null
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!supportSlideBack()) {
            return super.dispatchTouchEvent(ev)
        }
        if (mSwipeBackHelper == null) {
            mSwipeBackHelper = SwipeBackHelper(this, SlideActivityAdapter())
            mSwipeBackHelper!!.setOnSlideFinishListener(object :SwipeBackHelper.OnSlideFinishListener {
                override fun onFinish() {
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, R.anim.hold_on)
                }
            })
        }
        return mSwipeBackHelper!!.processTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    override fun finish() {
        if (mSwipeBackHelper != null) {
            mSwipeBackHelper!!.finishSwipeImmediately()
        }
        super.finish()
    }

    override fun supportSlideBack(): Boolean {
        return true
    }

    override fun canBeSlideBack(): Boolean {
        return true
    }

    private class SlideActivityAdapter : SlideActivityCallback {
        override val previousActivity: Activity?
            get() =ActivityLifecycleHelper.previousActivity
    }
}