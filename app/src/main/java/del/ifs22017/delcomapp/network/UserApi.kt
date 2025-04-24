package del.ifs22017.delcomapp.network

import del.ifs22017.delcomapp.data.ProfileResponse
import del.ifs22017.delcomapp.data.ProfileUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface UserApi {

    @GET("users/me")
    fun getProfile(): Call<ProfileResponse>

    @Multipart
    @POST("users/photo")
    fun updateProfilePhoto(
        @Part photo: MultipartBody.Part
    ): Call<ProfileResponse>

    @PUT("users/me")
    fun updateProfile(
        @Body profileUpdateRequest: ProfileUpdateRequest
    ): Call<ProfileResponse>

}