package tools.shenle.slbaseandroid.tool

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tools.shenle.slbaseandroid.BaseApplicationSl
import tools.shenle.slbaseandroid.view.BadgeView

object UIUtils {
    val context: Context by lazy {BaseApplicationSl.CONTEXT}

    private var toast: Toast? = null

    /**
     * dip转换px
     */
    fun dip2px(dip: Int): Int {
        if (dip == 0) return 0
        val scale = context.resources.displayMetrics.density
        return (dip * scale + 0.5f).toInt()
    }

    fun dip2px(dip: Float): Int {
        if (dip == 0f) return 0
        val scale = context.resources.displayMetrics.density
        return (dip * scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param context
     * @param spValue （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    fun px2sp(pxValue: Float): Int {
        val fontScale =
            context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    /**
     * pxz转换dip
     */
    fun px2dip(px: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (px / scale + 0.5f).toInt()
    }

    /**
     * 获取屏幕的宽 单位px
     *
     * @return int
     */
    val screenWidth: Int
        get() = context.resources.displayMetrics.widthPixels//        Display display = getActivity().getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int width = size.x;
//        int height = size.y;
//        return height;
//        return getActivity().getWindowManager().getDefaultDisplay().getHeight();

    /**
     * 获取屏幕的高 单位px
     *
     * @return int
     */
    val screenHeight: Int
        get() = context.resources.displayMetrics.heightPixels
    //        Display display = getActivity().getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int width = size.x;
//        int height = size.y;
//        return height;
//        return getActivity().getWindowManager().getDefaultDisplay().getHeight();



    /*
     * @deprecated use {@link #inflate(int resId,Context context)} instead
     */
    @Deprecated("")
    fun inflate(resId: Int): View {
        return LayoutInflater.from(context).inflate(resId, null)
    }

    fun inflate(resId: Int, context: Context?): View {
        return if (context == null) {
            LayoutInflater.from(context).inflate(resId, null)
        } else LayoutInflater.from(context).inflate(resId, null)
    }

    /**
     * 获取资源
     */
    val resources: Resources
        get() = context.resources

    /**
     * 获取文字
     */
    fun getString(resId: Int): String {
        return resources.getString(resId)
    }

    /**
     * 获取文字数组
     */
    fun getStringArray(resId: Int): Array<String> {
        return resources.getStringArray(resId)
    }

    /**
     * 获取dimen
     */
    fun getDimens(resId: Int): Int {
        return resources.getDimensionPixelSize(resId)
    }

    /**
     * 获取drawable
     */
    fun getDrawable(resId: Int): Drawable {
        return resources.getDrawable(resId)
    }

    /**
     * 获取drawable
     */
    fun getDrawableId(resName: String): Int {
        return resources.getIdentifier(
            context.packageName + ":drawable/" +
                    resName, null, null
        )
    }

    /**
     * 获取颜色
     */
    fun getColor(resId: Int): Int {
        return resources.getColor(resId)
    }

    /**
     * 获取颜色选择器
     */
    fun getColorStateList(resId: Int): ColorStateList {
        return resources.getColorStateList(resId)
    }

    fun startActivity(
        cl: Class<*>?,
        bundle: Bundle?,
        bundlename: String?
    ) {
        val activity = ActivityLifecycleHelper.latestActivity
        val intent = Intent()
        if (bundle != null) {
            intent.putExtra(bundlename, bundle)
        }
        if (activity != null) {
            intent.setClass(activity, cl!!)
            activity.startActivity(intent)
        } else {
            intent.setClass(context, cl!!)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun startActivity(intent: Intent) {
        val activity= ActivityLifecycleHelper.latestActivity
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun startActivity(cl: Class<*>?) {
        val activity= ActivityLifecycleHelper.latestActivity
        val intent = Intent()
        if (activity != null) {
            intent.setClass(activity, cl!!)
            activity.startActivity(intent)
        } else {
            intent.setClass(context, cl!!)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun startActivityForResult(cl: Class<*>?, requestcode: Int) {
        val activity= ActivityLifecycleHelper.latestActivity
        val intent = Intent()
        if (activity != null) {
            intent.setClass(activity, cl!!)
            activity.startActivityForResult(intent, requestcode)
        } else {
            intent.setClass(context, cl!!)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun startActivityForResult(intent: Intent, requestcode: Int) {
        val activity= ActivityLifecycleHelper.latestActivity
        if (activity != null) {
            activity.startActivityForResult(intent, requestcode)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun startActivityForResult(
        cl: Class<*>?,
        bundle: Bundle?,
        requestcode: Int
    ) {
        val activity= ActivityLifecycleHelper.latestActivity
        val intent = Intent()
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        if (activity != null) {
            intent.setClass(activity, cl!!)
            activity.startActivityForResult(intent, requestcode)
        } else {
            intent.setClass(context, cl!!)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun startActivity(cl: Class<*>?, bundle: Bundle?) {
        val activity= ActivityLifecycleHelper.latestActivity
        val intent = Intent()
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        if (activity != null) {
            intent.setClass(activity, cl!!)
            activity.startActivity(intent)
        } else {
            intent.setClass(context, cl!!)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 对toast的简易封装。线程安全，可以在非UI线程调用。
     */
    fun showToastSafe(resId: Int) {
        showToastSafe(getString(resId))
    }

    /**
     * 对toast的简易封装。线程安全，可以在非UI线程调用。
     */
    fun showToastSafe(str: String) {
        if (str.isEmpty()) {
            return
        }
        GlobalScope.launch(Dispatchers.Main){
            showToast(str)
        }
    }

    private fun showToast(str: String) {
        val frontActivity= ActivityLifecycleHelper.latestActivity
        if (frontActivity != null) {
            if (toast == null) {
                toast = Toast.makeText(frontActivity, str, Toast.LENGTH_LONG)
            } else {
                toast!!.setText(str)
            }
            toast!!.show()
        }
    }

    /**
     * 格式化价格,钱符号变小
     *
     * @param total_fee
     * @param textSize  ￥ 的字体大小 sp
     * @return
     */
    fun formatPriceMoney(total_fee: String, textSize: Int): SpannedString {
        var total_fee = total_fee
        total_fee = total_fee.replace("￥", "")
        try {
            total_fee = "￥ " + String.format("%.2f", total_fee.toDouble())
        } catch (e: Exception) {
        }
        val ss = SpannableString(total_fee)
        val ass = AbsoluteSizeSpan(textSize, true)
        ss.setSpan(
            ass, 0, 2,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return SpannedString(ss)
    }

    /**
     * 格式化价格,钱符号变小
     *
     * @param total_fee
     * @param textSize  ¥ 的字体大小 sp
     * @return
     */
    fun formatPriceMoneyNew(total_fee: String, textSize: Int): SpannedString {
        var total_fee = total_fee
        total_fee = total_fee.replace("￥", "")
        try {
            total_fee = "¥ " + String.format("%.2f", total_fee.toDouble())
        } catch (e: Exception) {
        }
        val ss = SpannableString(total_fee)
        val ass = AbsoluteSizeSpan(textSize, true)
        ss.setSpan(
            ass, 0, 2,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return SpannedString(ss)
    }

    /**
     * 格式化价格,钱整数变大
     *
     * @param total_fee
     * @param textSize  小数点后的字体大小 sp
     * @return
     */
    fun formatMidPrice(total_fee: String, textSize: Int): SpannedString {
        var total_fee = total_fee
        total_fee = total_fee.replace("￥", "")
        try {
            total_fee = "￥" + String.format("%.2f", total_fee.toDouble())
        } catch (e: Exception) {
        }
        val ss = SpannableString(total_fee)
        val ass = AbsoluteSizeSpan(textSize, true)
        val ass1 = AbsoluteSizeSpan(textSize, true)
        ss.setSpan(
            ass, 0, 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val start = total_fee.indexOf(".")
        ss.setSpan(
            ass1, start, ss.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return SpannedString(ss)
    }

    /**
     * 格式化价格
     *
     * @param price
     * @return 格式化后的价格 格式为 ￥ 0.00
     */
    fun formatPrice(price: String): String {
        return try {
            "￥" + String.format("%.2f", price.toDouble())
        } catch (e: Exception) {
            price
        }
    }

    /**
     * 格式化价格(电商字体不转换这个符号，以后让小丁加上)
     *
     * @param price
     * @return 格式化后的价格 格式为 ¥ 0.00
     */
    fun formatPriceWithFh(price: String): String {
        return try {
            "¥" + String.format("%.2f", price.toDouble())
        } catch (e: Exception) {
            price.replace("￥", "¥")
        }
    }

    /**
     * 格式化价格,小数点后的变小
     *
     * @param total_fee
     * @param textSize  小数点后的字体大小 sp
     * @return
     */
    fun formatPrice(total_fee: String, textSize: Int): SpannedString {
        val ss = SpannableString(total_fee)
        if (ss.toString().contains(".")) {
            val start = total_fee.indexOf(".")
            if (start >= 0) {
                val ass = AbsoluteSizeSpan(textSize, true)
                ss.setSpan(
                    ass, start, ss.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return SpannedString(ss)
    }

    /**
     * 格式电话号码
     *
     * @param phone 格式化后 xxx-xxxx-xxxx 如果不是11位电话,则不做任何操作
     * @return
     */
    fun formatPhone(phone: String): String {
        var phone = phone
        phone = phone.trim { it <= ' ' }
        if (phone.length == 11) {
            val sBuilder = StringBuilder(phone)
            val line = "-"
            val newString = (sBuilder.substring(0, 3) + line
                    + sBuilder.substring(3, 7) + line
                    + sBuilder.substring(7, sBuilder.length))
            phone = newString
        }
        return phone
    }

    /**
     * 格式电话号码
     *
     * @param tel 格式化后 158****1946 如果不是11位电话,则不做任何操作
     * @return
     */
    fun hidePhone(view: TextView, tel: String?) {
        var tel = tel
        tel = tel!!.trim { it <= ' ' }
        if (null != tel && tel.length > 8) {
            val targetString = tel.substring(3, 7)
            view.text = tel.replace(targetString, "****")
        } else {
            view.text = tel
        }
    }

    /**
     * @param activity
     * @return 状态栏高度  > 0 success; <= 0 fail
     */
    fun getStatusHeight(activity: Activity): Int {
        var statusHeight = 0
        val localRect = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(localRect)
        statusHeight = localRect.top
        if (0 == statusHeight) {
            val localClass: Class<*>
            try {
                localClass = Class.forName("com.android.internal.R\$dimen")
                val localObject = localClass.newInstance()
                val i5 = localClass.getField("status_bar_height")[localObject]
                    .toString().toInt()
                statusHeight = activity.resources.getDimensionPixelSize(i5)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        return statusHeight
    }

    fun closeWindowKeyBoard() {
        val im = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (activity?.currentFocus != null) {
            im.hideSoftInputFromWindow(
                activity!!.currentFocus!!
                    .applicationWindowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    fun closeWindowKeyBoard(et: EditText) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(et.windowToken, 0)
    }

    fun openWindowKeyBoard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun openWindowKeyBoard(et: EditText?) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, 0)
    }

    fun showErr() {
        showToastSafe("网络访问失败")
    }

    val activity: Activity?
        get() = ActivityLifecycleHelper.latestActivity

    val tiaoActivity: Context?
        get() {
            val foregroundActivity= ActivityLifecycleHelper.latestActivity ?: return context
            return ActivityLifecycleHelper.latestActivity
        }

    fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.toString().trim { it <= ' ' }.isEmpty() || s == "null"
    }

    fun isEmptyAnd0(s: CharSequence?): Boolean {
        return s == null || s.toString().trim { it <= ' ' }
            .isEmpty() || s == "null" || s == "0"
    }

    /**
     * EditText粘帖功能(待完善)
     *
     * @param et_content
     */
    fun setClipboard(et_content: EditText?) {
//        et_content.setTextIsSelectable(true);
//        et_content.setOnLongClickListener(new MyClipboardOnLongClick(et_content));
    }

    /**
     * 初始化红点
     *
     * @param view
     * @return
     */
    fun initBadge(view: View?, text: String?): BadgeView {
        return initBadge(view, text, dip2px(5))
    }

    fun initBadge(view: View?, text: String?, margin: Int): BadgeView {
        var text = text
        val badge = BadgeView(activity, view)
        badge.setTextSize(9f)
        if (text == null) text = ""
        badge.setText(text)
        badge.badgePosition = BadgeView.POSITION_TOP_RIGHT
        badge.badgeMargin = margin
        return badge
    }

    /**
     * @param goods_count
     */
    fun showBadge(badge: BadgeView, goods_count: String) {
        showBadge(badge, goods_count, 99)
    }

    /**
     * @param str
     */
    fun showBadgeStr(badge: BadgeView, str: String) {
        GlobalScope.launch(Dispatchers.Main){
            if ("0" == str || "00" == str || isEmpty(str)) {
                badge.hide()
            } else {
                badge.setText(str)
                badge.show()
            }
        }
    }

    /**
     * @param goods_count
     */
    fun showBadge(badge: BadgeView, goods_count: String, max: Int) {
        GlobalScope.launch(Dispatchers.Main){
            if ("0" == goods_count || "00" == goods_count || isEmpty(goods_count)) {
                badge.hide()
            } else {
                if (goods_count.contains("+")) {
                    badge.setText(goods_count)
                } else {
                    val i = goods_count.toInt()
                    if (i > max) {
                        badge.setText("$max+")
                    } else {
                        badge.setText(goods_count)
                    }
                }
                badge.show()
            }
        }
    }

    /**
     * @param goods_count
     */
    fun showBadge(badge: BadgeView, goods_count: Int) {
        showBadge(badge, goods_count, 99)
    }

    /**
     * @param goods_count
     */
    fun showBadge(badge: BadgeView, goods_count: Int, max: Int) {
        GlobalScope.launch(Dispatchers.Main){
            if (goods_count == 0) {
                badge.hide()
            } else {
                if (goods_count > max) {
                    badge.setText("$max+")
                } else {
                    badge.setText(goods_count.toString() + "")
                }
                badge.show()
            }
        }
    }

    /**
     * 获取资源文件ID
     *
     * @param resName
     * @param defType
     * @return
     */
    fun getResId(resName: String?, defType: String?): Int {
        return context.resources
            .getIdentifier(resName, defType, context.packageName)
    }
    /**
     * 替换一段文字里文字颜色及加粗
     *
     * @param str
     * @param content
     * @param color
     * @return
     */
    @JvmOverloads
    fun replaceTextColor(
        str: String,
        content: String,
        color: Int,
        isBold: Boolean = false
    ): SpannableString {
        val spanableInfo = SpannableString(content)
        if (isEmpty(str)) {
            return spanableInfo
        }
        var start = -1
        while (content.indexOf(str, start) != start) {
            start = content.indexOf(str, start)
            val end_l = start + str.length
            spanableInfo.setSpan(
                ForegroundColorSpan(
                    getColor(
                        color
                    )
                ), start, end_l,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (isBold) spanableInfo.setSpan(
                StyleSpan(Typeface.BOLD), start, end_l,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spanableInfo
    }

    /**
     * 替换一段文字里图片
     *
     * @param str
     * @param content
     * @param img
     * @return
     */
    fun replaceTextImg(str: String, content: String, img: Int): SpannableString {
        val spanableInfo = SpannableString(content)
        if (isEmpty(str)) {
            return spanableInfo
        }
        var start = -1
        while (content.indexOf(str, start) != start) {
            start = content.indexOf(str, start)
            val end_l = start + str.length
            spanableInfo.setSpan(
                ImageSpan(
                    getDrawable(
                        img
                    )
                ), start, end_l,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spanableInfo
    }

    /**
     * 坐标求距离
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    fun getDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val radLat1 = lat1 * Math.PI / 180
        val radLat2 = lat2 * Math.PI / 180
        val a = radLat1 - radLat2
        val b = lon1 * Math.PI / 180 - lon2 * Math.PI / 180
        var s = 2 * Math.asin(
            Math.sqrt(
                Math.pow(
                    Math.sin(a / 2),
                    2.0
                ) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(
                    Math.sin(b / 2),
                    2.0
                )
            )
        )
        s = s * 6378137.0 // 取WGS84标准参考椭球中的地球长半径(单位:m)
        s = Math.round(s * 10000) / 10000.toDouble()
        return s
    }

    /**
     * 判断是否到达带HeadView的listView顶部
     *
     * @param listView
     * @return
     */
    fun getHeaderY(listView: AbsListView): Float {
        val headerView = listView.getChildAt(0)
        return headerView?.top?.toFloat() ?: -1f
    }

    /**
     * 测量listView高度
     *
     * @param listView
     * @return
     */
    fun measureListViewHeight(listView: ListView): Int {
        val listAdapter = listView.adapter ?: return 0
        var totalHeight = 0
        val listViewWidth = screenWidth //listView在布局时的宽度
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            listViewWidth,
            View.MeasureSpec.AT_MOST
        )
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(widthSpec, 0)
            val itemHeight = listItem.measuredHeight
            totalHeight += itemHeight
        }
        // 减掉底部分割线的高度
        return (totalHeight
                + (listView.dividerHeight * listAdapter.count - 1))
    }

    fun replaceJson(result: String): String {
        return result.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace(
                "&nbsp;",
                " "
            ) //                .replace("https://bbs.zhue.com.cn","https://bbs.zhue.com.cn")
        //                .replace("https://www.zhue.cn","https://www.zhue.cn")
        //                .replace("https://bj.zhue.com.cn","https://bj.zhue.com.cn")
        //                .replace("https://fuwu.zhue.com.cn","https://fuwu.zhue.com.cn")
        //                .replace("http:\\/\\/bbs.zhue.com.cn","https:\\/\\/bbs.zhue.com.cn")
        //                .replace("http:\\/\\/www.zhue.cn","https:\\/\\/www.zhue.cn")
        //                .replace("http:\\/\\/bj.zhue.com.cn","https:\\/\\/bj.zhue.com.cn")
        //                .replace("http:\\/\\/fuwu.zhue.com.cn","https:\\/\\/fuwu.zhue.com.cn")
    }

    fun replaceFh(result: String): String {
        return result.replace("&quot;", "\"")
            .replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace("&nbsp;", " ")
            .trim { it <= ' ' }
    }

    /**
     * 根据fraction值来计算当前的颜色。
     */
    fun getCurrentColor(fraction: Float, startColor: Int, endColor: Int): Int {
        val redCurrent: Int
        val blueCurrent: Int
        val greenCurrent: Int
        val alphaCurrent: Int
        val redStart = Color.red(startColor)
        val blueStart = Color.blue(startColor)
        val greenStart = Color.green(startColor)
        val alphaStart = Color.alpha(startColor)
        val redEnd = Color.red(endColor)
        val blueEnd = Color.blue(endColor)
        val greenEnd = Color.green(endColor)
        val alphaEnd = Color.alpha(endColor)
        val redDifference = redEnd - redStart
        val blueDifference = blueEnd - blueStart
        val greenDifference = greenEnd - greenStart
        val alphaDifference = alphaEnd - alphaStart
        redCurrent = (redStart + fraction * redDifference).toInt()
        blueCurrent = (blueStart + fraction * blueDifference).toInt()
        greenCurrent = (greenStart + fraction * greenDifference).toInt()
        alphaCurrent = (alphaStart + fraction * alphaDifference).toInt()
        return Color.argb(alphaCurrent, redCurrent, greenCurrent, blueCurrent)
    }

    fun getDataString(s: String): String {
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(s, "")!!
    }

    fun setDataString(key: String, v: String?) {
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(key, v).commit()
    }

    /**
     * 通过设置全屏，设置状态栏透明
     *
     * @param activity
     */
    fun fullScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
                val window = activity.window
                val decorView = window.decorView
                //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
                val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                decorView.systemUiVisibility = option
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.TRANSPARENT
                //导航栏颜色也可以正常设置
//                window.setNavigationBarColor(Color.TRANSPARENT);
            } else {
                val window = activity.window
                val attributes = window.attributes
                val flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                val flagTranslucentNavigation =
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                attributes.flags = attributes.flags or flagTranslucentStatus
                //                attributes.flags |= flagTranslucentNavigation;
                window.attributes = attributes
            }
        }
    }

    //复制链接
    fun onCopyUrl(url: String?) {
        val cm = context
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = ClipData.newRawUri("Label", Uri.parse(url)) //链接
        //        ClipData.newPlainText("Label", "Content");//字符
//        ClipData.newIntent("Label", intent);//Intent型
        cm.setPrimaryClip(label) //将ClipData数据复制到剪贴板
        //        cm.getPrimaryClip();//这是从剪贴板中获取ClipData数据：
        showToastSafe("复制到剪贴板")
    }
}