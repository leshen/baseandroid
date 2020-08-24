package tools.shenle.baseandroid

import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.slbaseandroid.baseall.BaseViewModelActivity
import tools.shenle.slbaseandroid.tool.UIUtils

class MainActivity : BaseViewModelActivity<MainViewModel>() {
    companion object{
        fun goHere(){
            UIUtils.startActivity(MainActivity::class.java)
        }
    }
    override fun getLayoutResId() = R.layout.activity_main

    override fun initVM(): MainViewModel = getViewModel()

    override fun initView() {

    }
}
