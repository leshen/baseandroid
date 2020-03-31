package tools.shenle.baseandroid

import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Path
import tools.shenle.slbaseandroid.baseall.BaseBean

/**
 * 网络服务接口(协程)
 * @author ssq
 * @JvmSuppressWildcards 用来注解类和方法，使得被标记元素的泛型参数不会被编译成通配符?
 */
@JvmSuppressWildcards
interface AppApiService {
    companion object {
        const val BASE_URL = "https://bbs.zhue.com.cn/"
    }
    /**
     * 异步请求数据(案例)
     */
    @FormUrlEncoded
    @GET("source/plugin/zywx/rpc/zhuyouquan.php?mod=userinfo&userid={userid}")
    suspend fun getMyData(@Path("userid") userid: String): BaseBean
}