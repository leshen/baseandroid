package tools.shenle.baseandroid

import kotlinx.android.synthetic.main.activity_init.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import tools.shenle.baseandroid.R
import tools.shenle.slbaseandroid.baseall.BaseViewModelActivity

class InitActivity :BaseViewModelActivity<InitViewModel>(){
    override fun getLayoutResId() = R.layout.activity_init

    override fun initVM():InitViewModel = getViewModel()


    override fun initView() {
        tv_init.setOnClickListener {
            MainActivity.goHere()
        }
    }
}
