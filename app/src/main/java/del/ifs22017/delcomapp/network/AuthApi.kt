package del.ifs22017.delcomapp.network

import del.ifs22017.delcomapp.data.LoginRequest
import del.ifs22017.delcomapp.data.LoginResponse
import del.ifs22017.delcomapp.data.RegisterRequest
import del.ifs22017.delcomapp.data.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
