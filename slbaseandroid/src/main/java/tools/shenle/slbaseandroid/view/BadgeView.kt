package tools.shenle.slbaseandroid.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TabWidget
import androidx.appcompat.widget.AppCompatTextView
import tools.shenle.slbaseandroid.R
import tools.shenle.slbaseandroid.tool.UIUtils.dip2px

class BadgeView constructor(
    context: Context?,
    attrs: AttributeSet? = null as AttributeSet?,
    defStyle: Int = android.R.attr.textViewStyle,
    target: View? = null,
    tabIndex: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {
    private var context1: Context? = null
    var target: View? = null
        private set
    var badgePosition = 0
    var badgeMargin = 0
    private var badgeMarginTop = -1
    private var badgeMarginRight = -1
    private var badgeColor = 0
    var cardPadding = 0
    var cardPaddingColor = Color.WHITE

    //	private CardView cardView;
    //	public CardView getCardView() {
    //		return cardView;
    //	}
    var cardView: FrameLayout? = null
        private set

    fun setBadgeMarginTopAndRight(badgeMarginTop: Int, badgeMarginRight: Int) {
        this.badgeMarginTop = badgeMarginTop
        this.badgeMarginRight = badgeMarginRight
    }

    private var isShown = false
    private var badgeBg: ShapeDrawable? = null
    private var targetTabIndex = 0

    constructor(context: Context?, target: View?) : this(
        context,
        null,
        android.R.attr.textViewStyle,
        target,
        0
    ) {
    }

    constructor(context: Context?, target: TabWidget?, index: Int) : this(
        context,
        null,
        android.R.attr.textViewStyle,
        target,
        index
    ) {
    }

    private fun init(
        context: Context?,
        target: View?,
        tabIndex: Int
    ) {
        this.context1 = context
        this.target = target
        targetTabIndex = tabIndex

        // apply defaults
        badgePosition = DEFAULT_POSITION
        badgeMargin = dipToPixels(DEFAULT_MARGIN_DIP)
        badgeColor = DEFAULT_BADGE_COLOR
        typeface = Typeface.DEFAULT_BOLD
        val paddingPixels = dipToPixels(DEFAULT_LR_PADDING_DIP)
        setPadding(paddingPixels, 0, paddingPixels, 0)
        setTextColor(DEFAULT_TEXT_COLOR)
        fadeIn = AlphaAnimation(0f, 1f)
        fadeIn!!.interpolator = DecelerateInterpolator()
        fadeIn!!.duration = 200
        fadeOut = AlphaAnimation(1f, 0f)
        fadeOut!!.interpolator = AccelerateInterpolator()
        fadeOut!!.duration = 200
        isShown = false
        if (this.target != null) {
            applyTo(this.target)
        } else {
            show()
        }
    }

    private fun applyTo(target: View?) {
        var target = target
        val lp = target!!.layoutParams
        val parent = target.parent
        cardView = FrameLayout(context1!!)
        cardView!!.setBackgroundResource(R.drawable.white_10_shape)
        //		cardView = new CardView(context);
//		cardView.setCardElevation(0f);
//		cardView.setRadius(dipToPixels(DEFAULT_CORNER_RADIUS_DIP));
        cardView!!.addView(this)
        val container = FrameLayout(context1!!)
        if (target is TabWidget) {
            // set target to the relevant tab child container
            target = target.getChildTabViewAt(targetTabIndex)
            this.target = target
            (target as ViewGroup?)!!.addView(
                container, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            cardView!!.visibility = View.GONE
            container.addView(cardView)
        } else {
            val group = parent as ViewGroup
            val index = group.indexOfChild(target)
            group.removeView(target)
            group.addView(container, index, lp)
            container.addView(target)
            cardView!!.visibility = View.GONE
            container.addView(cardView)
            group.invalidate()
        }
    }

    fun show() {
        show(false, null)
    }

    fun show(animate: Boolean) {
        show(animate, fadeIn)
    }

    fun show(anim: Animation?) {
        show(true, anim)
    }

    fun hide() {
        hide(false, null)
    }

    fun hide(animate: Boolean) {
        hide(animate, fadeOut)
    }

    fun hide(anim: Animation?) {
        hide(true, anim)
    }

    fun toggle() {
        toggle(false, null, null)
    }

    fun toggle(animate: Boolean) {
        toggle(animate, fadeIn, fadeOut)
    }

    fun toggle(animIn: Animation?, animOut: Animation?) {
        toggle(true, animIn, animOut)
    }

    private fun show(animate: Boolean, anim: Animation?) {
        if (background == null) {
            if (badgeBg == null) {
                badgeBg = defaultBackground
            }
            setBackgroundDrawable(badgeBg)
        }
        applyLayoutParams()
        if (text.toString().isEmpty()) {
            cardView!!.layoutParams.height = dip2px(8)
            cardView!!.layoutParams.width = dip2px(8)
        }
        if (animate) {
            cardView!!.startAnimation(anim)
        }
        cardView!!.visibility = View.VISIBLE
        isShown = true
    }

    private fun hide(animate: Boolean, anim: Animation?) {
        cardView!!.visibility = View.GONE
        if (animate) {
            cardView!!.startAnimation(anim)
        }
        isShown = false
    }

    private fun toggle(
        animate: Boolean,
        animIn: Animation?,
        animOut: Animation?
    ) {
        if (isShown) {
            hide(animate && animOut != null, animOut)
        } else {
            show(animate && animIn != null, animIn)
        }
    }

    fun increment(offset: Int): Int {
        val txt = text
        var i: Int
        i = if (txt != null) {
            try {
                txt.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }
        i = i + offset
        text = i.toString()
        return i
    }

    fun decrement(offset: Int): Int {
        return increment(-offset)
    }

    private val defaultBackground: ShapeDrawable
        private get() {
            val r = dipToPixels(DEFAULT_CORNER_RADIUS_DIP)
            val outerR = floatArrayOf(
                r.toFloat(),
                r.toFloat(),
                r.toFloat(),
                r.toFloat(),
                r.toFloat(),
                r.toFloat(),
                r.toFloat(),
                r.toFloat()
            )
            val rr = RoundRectShape(outerR, null, null)
            val drawable = ShapeDrawable(rr)
            drawable.paint.color = resources.getColor(badgeColor)
            return drawable
        }

    private fun applyLayoutParams() {
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        when (badgePosition) {
            POSITION_TOP_LEFT -> {
                lp.gravity = Gravity.LEFT or Gravity.TOP
                lp.setMargins(badgeMargin, badgeMargin, 0, 0)
            }
            POSITION_TOP_RIGHT -> {
                lp.gravity = Gravity.RIGHT or Gravity.TOP
                if (badgeMarginTop != -1 && badgeMarginRight != -1) {
                    lp.setMargins(0, badgeMarginTop, badgeMarginRight, 0)
                } else {
                    lp.setMargins(0, badgeMargin, badgeMargin, 0)
                }
            }
            POSITION_BOTTOM_LEFT -> {
                lp.gravity = Gravity.LEFT or Gravity.BOTTOM
                lp.setMargins(badgeMargin, 0, 0, badgeMargin)
            }
            POSITION_BOTTOM_RIGHT -> {
                lp.gravity = Gravity.RIGHT or Gravity.BOTTOM
                lp.setMargins(0, 0, badgeMargin, badgeMargin)
            }
            POSITION_ALL -> {
                lp.gravity = Gravity.CENTER
                lp.setMargins(badgeMargin, badgeMargin, badgeMargin, badgeMargin)
            }
            else -> {
            }
        }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(cardPadding, cardPadding, cardPadding, cardPadding)
        setLayoutParams(layoutParams)
        //		cardView.setCardBackgroundColor(cardPaddingColor);
        cardView!!.layoutParams = lp
    }

    override fun isShown(): Boolean {
        return isShown
    }

    var badgeBackgroundColor: Int
        get() = badgeColor
        set(badgeColor) {
            this.badgeColor = badgeColor
            badgeBg = defaultBackground
        }

    private fun dipToPixels(dip: Int): Int {
        val r = resources
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(),
            r.displayMetrics
        )
        return px.toInt()
    }

    companion object {
        const val POSITION_TOP_LEFT = 1
        const val POSITION_TOP_RIGHT = 2
        const val POSITION_BOTTOM_LEFT = 3
        const val POSITION_BOTTOM_RIGHT = 4
        const val POSITION_ALL = 5
        private const val DEFAULT_MARGIN_DIP = 7
        private const val DEFAULT_LR_PADDING_DIP = 4
        private const val DEFAULT_CORNER_RADIUS_DIP = 8
        private const val DEFAULT_POSITION = POSITION_TOP_RIGHT
        private const val DEFAULT_BADGE_COLOR = Color.RED
        private const val DEFAULT_TEXT_COLOR = Color.WHITE
        private var fadeIn: Animation? = null
        private var fadeOut: Animation? = null
    }

    init {
        init(context, target, tabIndex)
    }
}