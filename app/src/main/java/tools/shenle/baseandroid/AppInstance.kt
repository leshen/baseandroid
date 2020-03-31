package tools.shenle.baseandroid

import org.koin.core.module.Module
import tools.shenle.slbaseandroid.BaseApplicationSl

/**
 * Created by shenle on 2020/3/27.
 */
class AppInstance : BaseApplicationSl() {
    override fun initApp() {
    }

    override fun initKoinModules(): List<Module> {
        return appModule
    }
}