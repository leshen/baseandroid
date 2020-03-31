package tools.shenle.slbaseandroid

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.jeremyliao.liveeventbus.LiveEventBus
import com.squareup.leakcanary.LeakCanary
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import tools.shenle.slbaseandroid.tool.ActivityLifecycleHelper
import kotlin.properties.Delegates

/**
 * Created by shenle on 2020/3/16.
 */
abstract class BaseApplicationSl : Application() {
    companion object {
//        lateinit var INSTANCE: BaseApplicationSl
        var CONTEXT: Context by Delegates.notNull()
    }
    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext
        initEventBus()
        initKoin()
        registerActivityLifecycleCallbacks(ActivityLifecycleHelper)
        initPhotoError()
//        initStetho()
        initLeakCanary()
        initApp()
    }

    abstract fun initApp()

    private fun initEventBus() {
        LiveEventBus.get()
            .config()
            .supportBroadcast(this)
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
    }

    private fun initKoin() {
        /*
            开启Koin，这里需要将所有需要注解生成的对象添加进来
         */
        startKoin {
            //给Koin框架添加ApplicationContext
            androidContext(this@BaseApplicationSl)
            /*
                这里设置Koin的日志打印
                Koin提供了三种实现:
                AndroidLogger:使用Android的Log.e/i/d()打印日志
                PrintLogger:使用System.err/out打印日志
                EmptyLogger:不打印日志，默认就是该实现
             */
            logger(AndroidLogger())
            /*
                设置Koin配置文件，需要放在assets文件夹中
                默认名称为：koin.propreties
                可以快速获取配置文件中的内容，文件名可以修改，但是需要在这里保持一致
                [getKoin().getProperty<String>("name")]
             */
            androidFileProperties("koin.properties")
            modules(initKoinModules())
        }
    }

    abstract fun initKoinModules(): List<Module>

    private fun initPhotoError() {
        // android 7.0系统解决拍照的问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val builder = VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            builder.detectFileUriExposure()
        }
    }
//    private fun initStetho() {
//        Stetho.initializeWithDefaults(this)
//    }

    private fun initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
    override fun onTerminate() {
        onDestory()
        System.exit(0)
        super.onTerminate()
    }
    /**
     * 销毁的其他
     */
    private fun onDestory() {
        ActivityLifecycleHelper.finishAll()
        Process.killProcess(Process.myPid())
    }
}