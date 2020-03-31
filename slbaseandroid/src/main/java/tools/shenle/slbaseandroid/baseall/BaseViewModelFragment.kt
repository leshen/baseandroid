package tools.shenle.slbaseandroid.baseall

import androidx.lifecycle.ViewModel

/**
 * Created by shenle on 2020/3/31.
 */
abstract class BaseViewModelFragment<VM : ViewModel>:BaseFragmentSl() {
    protected lateinit var mViewModel: VM
    protected abstract fun initVM(): VM

    override fun onCreateVM() {
        mViewModel = initVM()
    }
}