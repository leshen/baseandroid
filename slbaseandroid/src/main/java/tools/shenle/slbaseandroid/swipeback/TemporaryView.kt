package tools.shenle.slbaseandroid.swipeback

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import java.lang.ref.WeakReference

/**
 * Created by XBeats on 2019/3/20
 */
internal class TemporaryView(context: Context?) :
    View(context) {
    private var mView: WeakReference<View?>? = null
    private val mDrawable: Drawable
    private var mShadowWidth = 0
    private var mPaint: Paint? = null
    fun setShadowWidth(shadowWidth: Int) {
        mShadowWidth = shadowWidth
    }

    fun setBgColor(@ColorInt bgColor: Int) {
        if (mPaint == null) {
            mPaint = Paint()
            mPaint!!.isAntiAlias = true
        }
        mPaint!!.color = if (bgColor == 0) Color.WHITE else bgColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mView != null && mView!!.get() != null) {
            mView!!.get()!!.draw(canvas)
        } else {
            mDrawable.setBounds(0, 0, mShadowWidth, measuredHeight)
            mDrawable.draw(canvas)
            if (mPaint != null) {
                canvas.drawRect(
                    mShadowWidth.toFloat(),
                    0f,
                    measuredWidth.toFloat(),
                    measuredHeight.toFloat(),
                    mPaint!!
                )
            }
        }
    }

    fun cacheView(view: View?) {
        mView = WeakReference(view)
        invalidate()
    }

    init {
        val colors =
            intArrayOf(0x00000000, 0x17000000, 0x43000000) //分别为开始颜色，中间夜色，结束颜色
        mDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
    }
}