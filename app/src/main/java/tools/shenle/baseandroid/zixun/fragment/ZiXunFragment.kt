package tools.shenle.baseandroid.home.fragment

import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.baseandroid.home.viewmodel.ZiXunFragmentViewModel
import tools.shenle.slbaseandroid.baseall.BaseViewModelFragment

/**
 * Created by shenle on 2020/3/31.
 */
class ZiXunFragment :BaseViewModelFragment<ZiXunFragmentViewModel>(){
    override fun initVM(): ZiXunFragmentViewModel = getViewModel()

    override val layoutId: Int
        get() = R.layout.fragment_zixun

    override fun initView() {

    }

    override fun initData() {

    }
}