package del.ifs22017.delcomapp.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://public-api.delcom.org/api/v1/"

    var authToken: String? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .header("Cache-Control", "no-cache")
            authToken?.let { token ->
                Log.d("ApiClient", "Adding Authorization header for ${originalRequest.url}: Bearer $token")
                requestBuilder.header("Authorization", "Bearer $token")
            } ?: Log.w("ApiClient", "No auth token available for request: ${originalRequest.url}")
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }
}