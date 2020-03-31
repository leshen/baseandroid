package tools.shenle.slbaseandroid.baseall

import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import tools.shenle.slbaseandroid.swipeback.SwipeBackActivity

/**
 * Created by shenle on 2020/3/27.
 */
abstract class BaseActivitySl: SwipeBackActivity(), CoroutineScope by MainScope()  {
    protected abstract fun getLayoutResId(): Int


    protected abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateVM()
        setContentView(getLayoutResId())
        initView()
    }

    open fun onCreateVM() {}

    override fun onDestroy() {
        cancel()// 取消协程
        super.onDestroy()
    }

    protected open fun showFail(message: String? = null) {
        message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    }
}