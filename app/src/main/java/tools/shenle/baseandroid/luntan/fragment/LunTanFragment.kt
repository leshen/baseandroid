package tools.shenle.baseandroid.home.fragment

import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.baseandroid.home.viewmodel.LunTanFragmentViewModel
import tools.shenle.slbaseandroid.baseall.BaseViewModelFragment

/**
 * Created by shenle on 2020/3/31.
 */
class LunTanFragment :BaseViewModelFragment<LunTanFragmentViewModel>(){
    override fun initVM(): LunTanFragmentViewModel = getViewModel()

    override val layoutId: Int
        get() = R.layout.fragment_luntan

    override fun initView() {

    }

    override fun initData() {

    }
}