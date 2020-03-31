package tools.shenle.baseandroid

import okhttp3.OkHttpClient
import tools.shenle.slbaseandroid.http.BaseRetrofitClientSl

/**
 * Created by shenle on 2020/3/27.
 */
object AppRetrofitClient : BaseRetrofitClientSl() {
    override fun handleBuilder(builder: OkHttpClient.Builder) {

    }
}