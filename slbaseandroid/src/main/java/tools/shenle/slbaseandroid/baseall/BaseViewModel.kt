package tools.shenle.slbaseandroid.baseall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tools.shenle.slbaseandroid.http.LoadState

open class BaseViewModel(): ViewModel() {
//    val myData = MutableLiveData<BaseBean>()
    /**
     * 获取用户信息(案例)
     */
//    fun getData(context: Context?) = launch({
//        if (!MyUtil.validateLogin()) return@launch
//        loadState.value = LoadState.Loading()
//        myData.value = Repository.getMyData(context)
//        loadState.value = LoadState.Success()
//    }, {
//        loadState.value = LoadState.Fail()
//    })
}


/**
 * ViewModel扩展方法：启动协程
 * @param block 协程逻辑
 * @param onError 错误回调方法
 * @param onComplete 完成回调方法
 */
fun ViewModel.launch(
    block: suspend CoroutineScope.() -> Unit,
    onError: (e: Throwable) -> Unit = {},
    onComplete: () -> Unit = {}
) {
    viewModelScope.launch(
        CoroutineExceptionHandler { _, throwable ->
            run {
                // 这里统一处理错误
                ExceptionUtil.catchException(throwable)
                onError(throwable)
            }
        }
    ) {
        try {
            block()
        } finally {
            onComplete()
        }
    }
}

/**
 * ViewModel扩展属性：加载状态
 */
val ViewModel.loadState: MutableLiveData<LoadState>
    get() = MutableLiveData()