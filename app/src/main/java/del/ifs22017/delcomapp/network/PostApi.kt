package del.ifs22017.delcomapp.network

import del.ifs22017.delcomapp.data.PostResponse
import del.ifs22017.delcomapp.data.PostsResponse
import del.ifs22017.delcomapp.data.SinglePostResponse

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface PostApi {
    @Multipart
    @POST("posts")
    fun addPost(
        @Part cover: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Call<PostResponse>

    @Multipart
    @POST("posts/{id}/cover")
    fun changeCover(
        @Path("id") postId: Int,
        @Part cover: MultipartBody.Part
    ): Call<PostResponse>

    @FormUrlEncoded
    @PUT("posts/{id}")
    fun updatePost(
        @Path("id") postId: Int,
        @Field("description") description: String
    ): Call<PostResponse>

    @GET("posts")
    fun getAllPosts(
        @Query("is_me") isMe: Int = 1
    ): Call<PostsResponse>

    @GET("posts/{id}")
    fun getPostById(
        @Path("id") postId: Int
    ): Call<SinglePostResponse>
}