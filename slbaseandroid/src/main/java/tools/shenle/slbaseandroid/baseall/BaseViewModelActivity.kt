package tools.shenle.slbaseandroid.baseall

import androidx.lifecycle.ViewModel

/**
 * Activity 基类
 * MV ViewModel 架构
 */
abstract class BaseViewModelActivity<VM : ViewModel> : BaseActivitySl(){
    protected lateinit var mViewModel: VM
    protected abstract fun initVM(): VM

    override fun onCreateVM() {
        mViewModel = initVM()
    }
}