package pe.edu.cibertec.reportaya.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pe.edu.cibertec.reportaya.BuildConfig
import pe.edu.cibertec.reportaya.data.UserSession
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = UserSession.accessToken
        val request = if (token.isNullOrBlank()) original else original.newBuilder().header("Authorization", "Bearer $token").build()
        val response = chain.proceed(request)
        if (response.code() == 401) UserSession.clear()
        response
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: ReportaYaApi by lazy {
        Retrofit.Builder().baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL)).client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ReportaYaApi::class.java)
    }

    private fun normalizeBaseUrl(value: String): String = if (value.endsWith('/')) value else "$value/"
}
