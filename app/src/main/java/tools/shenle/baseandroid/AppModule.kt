package tools.shenle.baseandroid

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import tools.shenle.baseandroid.home.viewmodel.*

/**
 * Created by luyao
 * on 2019/11/15 15:44
 */

val viewModelModule = module {
    viewModel { InitViewModel(get()) }
    viewModel { MainViewModel(get()) }
    viewModel { HomeFragmentViewModel(get()) }
    viewModel { LunTanFragmentViewModel(get()) }
    viewModel { ZiXunFragmentViewModel(get()) }
    viewModel { ZyscFragmentViewModel(get()) }
    viewModel { PersonFragmentViewModel(get()) }
//    viewModel { LoginViewModel(get(),get()) }
//    viewModel { ArticleViewModel(get(), get(), get(), get(), get()) }
//    viewModel { SystemViewModel(get(), get()) }
//    viewModel { NavigationViewModel(get()) }
//    viewModel { ProjectViewModel(get()) }
//    viewModel { SearchViewModel(get(), get()) }
//    viewModel { ShareViewModel(get()) }
}

val repositoryModule = module {
    single { AppRetrofitClient.getService(AppApiService::class.java, AppApiService.BASE_URL) }
//    single { CoroutinesDispatcherProvider() }
//    single { LoginRepository(get()) }
//    single { SquareRepository() }
//    single { HomeRepository() }
//    single { ProjectRepository() }
//    single { CollectRepository() }
//    single { SystemRepository() }
//    single { NavigationRepository() }
//    single { SearchRepository() }
//    single { ShareRepository() }
}

val appModule = listOf(viewModelModule, repositoryModule)