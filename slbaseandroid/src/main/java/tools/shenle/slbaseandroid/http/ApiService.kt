package tools.shenle.slbaseandroid.http

import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import tools.shenle.slbaseandroid.baseall.BaseBean

/**
 * 网络服务接口(协程)
 * @author ssq
 * @JvmSuppressWildcards 用来注解类和方法，使得被标记元素的泛型参数不会被编译成通配符?
 */
@JvmSuppressWildcards
interface ApiService {

    /**
     * 异步请求数据(案例)
     */
    @FormUrlEncoded
    @GET("https://bbs.zhue.com.cn/source/plugin/zywx/rpc/zhuyouquan.php?mod=userinfo&userid={userid}")
    suspend fun getMyData(@Path("userid") userid: String): BaseBean
}