package tools.shenle.slbaseandroid.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tools.shenle.slbaseandroid.BuildConfig
import java.util.concurrent.TimeUnit

abstract class BaseRetrofitClientSl {
//    companion object {
//        private const val TIME_OUT = 5
//    }

    private val client: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            val logging = HttpLoggingInterceptor().apply {
                level = when (BuildConfig.DEBUG) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    false -> HttpLoggingInterceptor.Level.NONE
                }
            }
            builder.addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)// 连接时间：30s超时
                .readTimeout(10, TimeUnit.SECONDS)// 读取时间：10s超时
                .writeTimeout(10, TimeUnit.SECONDS)// 写入时间：10s超时
            handleBuilder(builder)

            return builder.build()
        }

    protected abstract fun handleBuilder(builder: OkHttpClient.Builder)

    fun <S> getService(serviceClass: Class<S>, baseUrl: String): S {
        return Retrofit.Builder()
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build().create(serviceClass)
    }
}