package tools.shenle.baseandroid.home.fragment

import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.baseandroid.home.viewmodel.PersonFragmentViewModel
import tools.shenle.slbaseandroid.baseall.BaseViewModelFragment

/**
 * Created by shenle on 2020/3/31.
 */
class PersonFragment :BaseViewModelFragment<PersonFragmentViewModel>(){
    override fun initVM(): PersonFragmentViewModel = getViewModel()

    override val layoutId: Int
        get() = R.layout.fragment_person

    override fun initView() {

    }

    override fun initData() {

    }
}