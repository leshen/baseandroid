package tools.shenle.baseandroid.home.fragment

import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.baseandroid.home.viewmodel.ZyscFragmentViewModel
import tools.shenle.slbaseandroid.baseall.BaseViewModelFragment

/**
 * Created by shenle on 2020/3/31.
 */
class ZyscFragment :BaseViewModelFragment<ZyscFragmentViewModel>(){
    override fun initVM(): ZyscFragmentViewModel = getViewModel()

    override val layoutId: Int
        get() = R.layout.fragment_zysc

    override fun initView() {

    }

    override fun initData() {

    }
}