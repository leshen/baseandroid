package tools.shenle.slbaseandroid.swipeback

import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import java.lang.ref.WeakReference

/**
 * Created by XBeats on 2019/3/20
 */
class SwipeBackHelper(
    private val mCurrentActivity: Activity,
    slideActivityCallback: SlideActivityCallback
) {
    private val mInterpolator: Interpolator = DecelerateInterpolator(2f)
    private val mEdgeSize //px 拦截手势区间
            : Int
    private var mIsSliding = false //是否正在滑动 = false
    private var mIsSlideAnimPlaying = false //滑动动画展示过程中 = false
    private var mDistanceX = 0f //px 当前滑动距离 （正数或0） = 0f
    private var mLastPointX = 0f //记录手势在屏幕上的X轴坐标 = 0f
    private var mEnableSlideBack = true //默认启动滑动返回
    private val mTouchSlop: Int
    private var mIsInThresholdArea = false
    private val mViewManager: ViewManager
    private var mValueAnimator: ValueAnimator? = null
    fun enableSwipeBack(enable: Boolean) {
        if (mEnableSlideBack == enable) {
            return
        }
        mEnableSlideBack = enable
        if (!mEnableSlideBack && mDistanceX != 0f) {
            changeStatus(STATE_BACK_START)
        }
    }

    fun processTouchEvent(ev: MotionEvent): Boolean {
        if (!mEnableSlideBack) { //不支持滑动返回，则手势事件交给View处理
            return false
        }
        if (mIsSlideAnimPlaying) {  //正在滑动动画播放中，直接消费手势事件
            return true
        }
        val action = ev.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_DOWN) {
            mLastPointX = ev.rawX
            mIsInThresholdArea = mLastPointX >= 0 && mLastPointX <= mEdgeSize
        }
        if (!mIsInThresholdArea) {  //不满足滑动区域，不做处理
            return false
        }
        val actionIndex = ev.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN -> changeStatus(STATE_ACTION_DOWN)
            MotionEvent.ACTION_POINTER_DOWN -> if (mIsSliding) {  //有第二个手势事件加入，而且正在滑动事件中，则直接消费事件
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val curPointX = ev.rawX
                val originSlideStatus = mIsSliding
                if (!originSlideStatus) {
                    mIsSliding = if (Math.abs(curPointX - mLastPointX) < mTouchSlop) { //判断是否满足滑动
                        return false
                    } else {
                        true
                    }
                }
                return if (actionIndex == 0) {  //开始滑动
                    val distanceX = curPointX - mLastPointX
                    mLastPointX = curPointX
                    setTranslationX(mDistanceX + distanceX)
                    if (originSlideStatus == mIsSliding) {
                        true
                    } else {
                        val cancelEvent =
                            MotionEvent.obtain(ev) //首次判定为滑动需要修正事件：手动修改事件为 ACTION_CANCEL，并通知底层View
                        cancelEvent.action = MotionEvent.ACTION_CANCEL
                        mCurrentActivity.window.superDispatchTouchEvent(cancelEvent)
                        true
                    }
                } else {
                    true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                if (mDistanceX == 0f) { //没有进行滑动
                    mIsSliding = false
                    changeStatus(STATE_ACTION_UP)
                    return false
                }
                if (mIsSliding && actionIndex == 0) { // 取消滑动 或 手势抬起 ，而且手势事件是第一手势，开始滑动动画
                    mIsSliding = false
                    changeStatus(STATE_ACTION_UP)
                    return true
                } else if (mIsSliding) {
                    return true
                }
            }
            else -> mIsSliding = false
        }
        return false
    }

    fun finishSwipeImmediately() {
        if (mValueAnimator != null) {
            mValueAnimator!!.cancel()
        }
    }

    /**
     * 处理事件（滑动事件除外）
     *
     * @param status
     */
    private fun changeStatus(status: Int) {
        when (status) {
            STATE_ACTION_DOWN -> {
                // hide input method
                val inputMethod =
                    mCurrentActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val view = mCurrentActivity.currentFocus
                if (view != null && inputMethod != null) {
                    inputMethod.hideSoftInputFromWindow(view.windowToken, 0)
                }
                if (!mViewManager.prepareViews()) return
            }
            STATE_ACTION_UP -> {
                val width = mCurrentActivity.resources.displayMetrics.widthPixels
                if (mDistanceX == 0f) {
                    mViewManager.clearViews(false)
                } else if (mDistanceX > width / 4) {
                    changeStatus(STATE_FORWARD_START)
                } else {
                    changeStatus(STATE_BACK_START)
                }
            }
            STATE_BACK_START -> {
                mIsSlideAnimPlaying = true
                startBackAnim()
            }
            STATE_BACK_FINISH -> {
                mDistanceX = 0f
                mIsSliding = false
                mViewManager.clearViews(false)
            }
            STATE_FORWARD_START -> {
                mIsSlideAnimPlaying = true
                startForwardAnim()
            }
            STATE_FORWARD_FINISH -> {
                mViewManager.clearViews(true)
                if (mOnSlideFinishListener != null) {
                    mOnSlideFinishListener!!.onFinish()
                }
            }
            else -> {
            }
        }
    }

    private fun setTranslationX(x: Float) {
        val width = mCurrentActivity.resources.displayMetrics.widthPixels
        mDistanceX = x
        mDistanceX = Math.max(0f, mDistanceX)
        mDistanceX = Math.min(width.toFloat(), mDistanceX)
        mViewManager.translateViews(mDistanceX, width)
    }

    private fun startBackAnim() {
        val maxValue = 150
        mValueAnimator = ValueAnimator()
        mValueAnimator!!.interpolator = mInterpolator
        mValueAnimator!!.setIntValues(0, maxValue)
        mValueAnimator!!.duration = maxValue.toLong()
        mValueAnimator!!.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val currentDistanceX = mDistanceX * (maxValue - value) / maxValue
            setTranslationX(currentDistanceX)
        }
        mValueAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                animation.removeListener(this)
                mIsSlideAnimPlaying = false
                changeStatus(STATE_BACK_FINISH)
            }
        })
        mValueAnimator!!.start()
    }

    private fun startForwardAnim() {
        val maxValue = 300
        mValueAnimator = ValueAnimator()
        mValueAnimator!!.interpolator = mInterpolator
        mValueAnimator!!.setIntValues(0, maxValue)
        mValueAnimator!!.duration = maxValue.toLong()
        mValueAnimator!!.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val width = mCurrentActivity.resources.displayMetrics.widthPixels
            val currentDistanceX =
                mDistanceX + (width - mDistanceX) * value / maxValue
            setTranslationX(currentDistanceX)
        }
        mValueAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                animation.removeListener(this)
                changeStatus(STATE_FORWARD_FINISH)
            }
        })
        mValueAnimator!!.start()
    }

    private class ViewManager(
        private val mCurrentActivity: Activity,
        private val mSlideActivityCallback: SlideActivityCallback
    ) {
        private var mPreviousActivity: WeakReference<Activity?>? = null
        private var mCurrentContentView: ViewGroup? = null
        private var mDisplayView: View? = null
        private var mTemporaryView: TemporaryView? = null
        private var mPreviousDisplayView: View? = null
        private var mStatusBarOffset =
            0 // make up for the different from the current Activity to previous; = 0

        /**
         * Remove view from previous Activity and add into current Activity
         *
         * @return Is view added successfully
         */
        fun prepareViews(): Boolean {
            mCurrentContentView =
                mCurrentActivity.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup
            if (mCurrentContentView!!.childCount == 0) {
                mCurrentContentView = null
                return false
            }
            val previousActivity = mSlideActivityCallback.previousActivity
            if (previousActivity == null) {
                mCurrentContentView = null
                return false
            }
            //previous Activity not support to be swipeBack...
            if (previousActivity is SlideBackManager && !(previousActivity as SlideBackManager).canBeSlideBack()) {
                mCurrentContentView = null
                return false
            }
            val previousActivityContainer =
                previousActivity.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup?
            if (previousActivityContainer == null || previousActivityContainer.childCount == 0) {
                mCurrentContentView = null
                return false
            }

            // Cache the previous Activity, make sure return view to the right Activity!
            mPreviousActivity = WeakReference(previousActivity)

            // add content view from previous Activity
            mPreviousDisplayView = previousActivityContainer.getChildAt(0)
            val height = mCurrentActivity.resources.displayMetrics.heightPixels
            val previousViewHeight = previousActivityContainer.measuredHeight
            val currentViewHeight = mCurrentContentView!!.measuredHeight
            val isCurrentFull = currentViewHeight == height
            val isPreviousFull = previousViewHeight == height
            mStatusBarOffset = if (isCurrentFull) {
                if (isPreviousFull) 0 else height - previousViewHeight
            } else {
                if (isPreviousFull) -(height - currentViewHeight) else 0
            }
            val previousParams =
                mPreviousDisplayView!!.getLayoutParams() as FrameLayout.LayoutParams
            previousParams.topMargin = mStatusBarOffset
            previousActivityContainer.removeView(mPreviousDisplayView)
            mCurrentContentView!!.addView(mPreviousDisplayView, 0, previousParams)
            val width = mCurrentActivity.resources.displayMetrics.widthPixels
            mPreviousDisplayView!!.setTranslationX((-width).toFloat() / 3)

            // add shadow view, make up a background color for TemporaryView.
            mTemporaryView = TemporaryView(mCurrentActivity)
            mTemporaryView!!.setShadowWidth(SHADOW_WIDTH)
            mTemporaryView!!.translationX = -SHADOW_WIDTH.toFloat()
            mCurrentContentView!!.addView(mTemporaryView, 1)
            val color = getWindowBackgroundColor(mCurrentActivity)
            mTemporaryView!!.setBgColor(color)

            // init display view
            mDisplayView = mCurrentContentView!!.getChildAt(2)
            return true
        }

        fun clearViews(forward: Boolean) {
            if (mCurrentContentView == null) {
                return
            }

            // recover the content view from previous Activity
            mPreviousDisplayView!!.x = 0f
            mCurrentContentView!!.removeView(mPreviousDisplayView)
            if (mPreviousActivity != null && mPreviousActivity!!.get() != null && !mPreviousActivity!!.get()!!
                    .isFinishing
            ) {
                val previewContentView = mPreviousActivity!!.get()!!
                    .findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                )
                previewContentView.addView(mPreviousDisplayView, layoutParams)
            }

            // in forward case, TemporaryView should cache the previous view.
            if (forward) {
                mTemporaryView!!.translationX = 0f
                mTemporaryView!!.cacheView(mPreviousDisplayView)
                val params =
                    mTemporaryView!!.layoutParams as MarginLayoutParams
                params.topMargin = mStatusBarOffset
                mCurrentContentView!!.bringChildToFront(mTemporaryView)
            } else {
                mCurrentContentView!!.removeView(mTemporaryView)
                mDisplayView!!.translationX = 0f
            }
            mTemporaryView = null
            mPreviousDisplayView = null
            mCurrentContentView = null
            mDisplayView = null
        }

        fun translateViews(x: Float, screenWidth: Int) {
            if (mCurrentContentView == null) {
                return
            }
            mPreviousDisplayView!!.x = (-screenWidth + x) / 3
            mTemporaryView!!.x = x - SHADOW_WIDTH
            mDisplayView!!.x = x
        }

    }

    interface SlideBackManager {
        /**
         * 是否支持滑动返回
         *
         * @return
         */
        fun supportSlideBack(): Boolean

        /**
         * 能否滑动返回至当前Activity
         *
         * @return
         */
        fun canBeSlideBack(): Boolean
    }

    private var mOnSlideFinishListener: OnSlideFinishListener? = null
    fun setOnSlideFinishListener(onSlideFinishListener: OnSlideFinishListener?) {
        mOnSlideFinishListener = onSlideFinishListener
    }

    interface OnSlideFinishListener {
        fun onFinish()
    }

    companion object {
        private const val TAG = "SwipeBackHelper"
        private const val STATE_ACTION_DOWN = 1 //点击事件
        private const val STATE_ACTION_UP = 2 //点击结束
        private const val STATE_BACK_START = 3 //开始滑动，不返回前一个页面
        private const val STATE_BACK_FINISH = 4 //结束滑动，不返回前一个页面
        private const val STATE_FORWARD_START = 5 //开始滑动，返回前一个页面
        private const val STATE_FORWARD_FINISH = 6 //结束滑动，返回前一个页面
        private const val SHADOW_WIDTH = 50 //px 阴影宽度
        private const val EDGE_SIZE = 20 //dp 默认拦截手势区间
        private fun getWindowBackgroundColor(activity: Activity): Int {
            var array: TypedArray? = null
            return try {
                array = activity.theme
                    .obtainStyledAttributes(intArrayOf(R.attr.windowBackground))
                array.getColor(0, 0)
            } catch (e: Exception) {
                0
            } finally {
                array?.recycle()
            }
        }
    }

    init {
        mViewManager = ViewManager(
            mCurrentActivity,
            slideActivityCallback
        )
        mTouchSlop = ViewConfiguration.get(mCurrentActivity).scaledTouchSlop
        val density = mCurrentActivity.resources.displayMetrics.density
        mEdgeSize = (EDGE_SIZE * density + 0.5f).toInt() //滑动拦截事件的区域
    }
}