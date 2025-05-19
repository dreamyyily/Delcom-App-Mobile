package del.ifs22017.delcomapp.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import del.ifs22017.delcomapp.network.ApiClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileViewModel(private val context: Context) : ViewModel() {
    val user = mutableStateOf<ProfileUser?>(null)
    val posts = mutableStateOf<List<Post>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val successMessage = mutableStateOf<String?>(null)
    val isEditMode = mutableStateOf(false)
    val tempName = mutableStateOf("")
    val tempEmail = mutableStateOf("")
    val tempPhone = mutableStateOf("")

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)

    init {
        loadProfile()
        loadPosts()
    }

    fun loadProfile() {
        isLoading.value = true
        Log.d("ProfileViewModel", "Loading profile")
        ApiClient.userApi.getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val serverUser = response.body()?.data?.user
                    val localPhone = sharedPreferences.getString("phone", null)
                    user.value = serverUser?.copy(phone = localPhone)
                    Log.d("ProfileViewModel", "Profile loaded: name=${user.value?.name}, email=${user.value?.email}, phone=${user.value?.phone}, photo=${user.value?.photo}")
                } else {
                    errorMessage.value = "Failed to load profile: ${response.code()} - ${response.message()}"
                    Log.e("ProfileViewModel", "Load profile failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                isLoading.value = false
                errorMessage.value = "Network error: ${t.message}"
                Log.e("ProfileViewModel", "Load profile network error", t)
            }
        })
    }

    fun loadPosts() {
        if (ApiClient.authToken.isNullOrEmpty()) {
            errorMessage.value = "Authentication required. Please log in again."
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            errorMessage.value = "No internet connection. Please check your network."
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Loading posts")
        ApiClient.postApi.getAllPosts(isMe = 1).enqueue(object : Callback<PostsResponse> {
            override fun onResponse(call: Call<PostsResponse>, response: Response<PostsResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val postsResponse = response.body()?.data?.posts ?: emptyList()
                    posts.value = postsResponse
                    Log.d("ProfileViewModel", "Posts loaded: count=${postsResponse.size}")
                } else {
                    errorMessage.value = "Failed to load posts: ${response.code()} - ${response.message()}"
                    Log.e("ProfileViewModel", "Load posts failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<PostsResponse>, t: Throwable) {
                isLoading.value = false
                errorMessage.value = "Network error: ${t.message}"
                Log.e("ProfileViewModel", "Load posts network error", t)
            }
        })
    }

    fun getPostById(postId: Int, onSuccess: (DetailedPost) -> Unit, onFailure: (String) -> Unit) {
        if (ApiClient.authToken.isNullOrEmpty()) {
            onFailure("Authentication required. Please log in again.")
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("No internet connection. Please check your network.")
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Loading post: id=$postId")
        ApiClient.postApi.getPostById(postId).enqueue(object : Callback<SinglePostResponse> {
            override fun onResponse(call: Call<SinglePostResponse>, response: Response<SinglePostResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val post = response.body()?.data?.post
                    if (post != null) {
                        Log.d("ProfileViewModel", "Post loaded: id=$postId")
                        onSuccess(post)
                    } else {
                        onFailure("Post data is null")
                        Log.e("ProfileViewModel", "Post data is null for id=$postId")
                    }
                } else {
                    val errorMsg = "Failed to load post: ${response.code()} - ${response.message()}"
                    onFailure(errorMsg)
                    Log.e("ProfileViewModel", "Load post failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<SinglePostResponse>, t: Throwable) {
                isLoading.value = false
                onFailure("Network error: ${t.message}")
                Log.e("ProfileViewModel", "Load post network error", t)
            }
        })
    }

    fun deletePost(postId: Int, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (ApiClient.authToken.isNullOrEmpty()) {
            onFailure("Authentication required. Please log in again.")
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("No internet connection. Please check your network.")
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Deleting post: id=$postId")
        ApiClient.postApi.deletePost(postId).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Log.d("ProfileViewModel", "Post deleted successfully: id=$postId")
                    successMessage.value = response.body()?.message ?: "Post deleted successfully"
                    onSuccess(response.body()?.message ?: "Post deleted successfully")
                    loadPosts() // Refresh posts after deletion
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No details"
                    val errorMsg = when (response.code()) {
                        401 -> "Authentication failed. Please log in again."
                        403 -> "You are not authorized to delete this post."
                        404 -> "Post not found."
                        429 -> "Too many requests. Try again later."
                        else -> "Failed to delete post: ${response.code()} - ${errorBody}"
                    }
                    onFailure(errorMsg)
                    Log.e("ProfileViewModel", "Delete post failed: $errorBody")
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                isLoading.value = false
                onFailure("Network error: ${t.message}")
                Log.e("ProfileViewModel", "Delete post network error", t)
            }
        })
    }

    fun updateProfilePhoto(file: File) {
        if (!file.exists() || file.length().toInt() == 0) {
            errorMessage.value = "Invalid file: File is empty or does not exist"
            Log.e("ProfileViewModel", "Invalid file: ${file.absolutePath}")
            return
        }

        if (ApiClient.authToken.isNullOrEmpty()) {
            errorMessage.value = "Authentication required. Please log in again."
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            errorMessage.value = "No internet connection. Please check your network."
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Uploading file: ${file.absolutePath}, size: ${file.length()} bytes")

        val requestFile = file.asRequestBody("image/*".toMediaType())
        val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        ApiClient.userApi.updateProfilePhoto(photoPart).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Log.d("ProfileViewModel", "Photo updated successfully")
                    successMessage.value = "Photo updated successfully"
                    loadProfile()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No details"
                    errorMessage.value = when (response.code()) {
                        400 -> "Invalid image format or data. Please try another image."
                        401 -> "Authentication failed. Please log in again."
                        403 -> "You are not authorized to update this photo."
                        413 -> "Image too large. Please choose a smaller file."
                        429 -> "Too many requests. Try again later."
                        else -> "Failed to update photo: ${response.code()} - ${response.message()}"
                    }
                    Log.e("ProfileViewModel", "Update photo failed: $errorBody")
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                isLoading.value = false
                errorMessage.value = "Network error: ${t.message}"
                Log.e("ProfileViewModel", "Upload photo network error", t)
            }
        })
    }

    fun addPost(cover: File, description: String, onSuccess: (Int) -> Unit, onFailure: (String) -> Unit) {
        if (!cover.exists() || cover.length().toInt() == 0) {
            onFailure("Invalid cover image: File is empty or does not exist")
            Log.e("ProfileViewModel", "Invalid cover file: ${cover.absolutePath}")
            return
        }

        if (description.isBlank()) {
            onFailure("Description cannot be empty")
            Log.w("ProfileViewModel", "Add post failed: Description is empty")
            return
        }

        if (ApiClient.authToken.isNullOrEmpty()) {
            onFailure("Authentication required. Please log in again.")
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("No internet connection. Please check your network.")
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Uploading post: cover=${cover.absolutePath}, description=$description")

        val coverRequest = cover.asRequestBody("image/*".toMediaType())
        val coverPart = MultipartBody.Part.createFormData("cover", cover.name, coverRequest)
        val descriptionRequest = description.toRequestBody("text/plain".toMediaType())

        ApiClient.postApi.addPost(coverPart, descriptionRequest).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val postId = response.body()?.data?.post_id ?: -1
                    Log.d("ProfileViewModel", "Post added successfully: post_id=$postId")
                    successMessage.value = response.body()?.message ?: "Post added successfully"
                    onSuccess(postId)
                    loadPosts()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No details"
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid data. Please check your image or description."
                        401 -> "Authentication failed. Please log in again."
                        403 -> "You are not authorized to create posts."
                        413 -> "Image too large. Please choose a smaller file."
                        429 -> "Too many requests. Try again later."
                        else -> "Failed to add post: ${response.code()} - ${errorBody}"
                    }
                    onFailure(errorMsg)
                    Log.e("ProfileViewModel", "Add post failed: $errorBody")
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                isLoading.value = false
                onFailure("Network error: ${t.message}")
                Log.e("ProfileViewModel", "Add post network error", t)
            }
        })
    }

    fun changePostCover(postId: Int, cover: File, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (!cover.exists() || cover.length().toInt() == 0) {
            onFailure("Invalid cover image: File is empty or does not exist")
            Log.e("ProfileViewModel", "Invalid cover file: ${cover.absolutePath}")
            return
        }

        if (ApiClient.authToken.isNullOrEmpty()) {
            onFailure("Authentication required. Please log in again.")
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("No internet connection. Please check your network.")
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Changing cover for post_id=$postId, cover=${cover.absolutePath}")

        val coverRequest = cover.asRequestBody("image/*".toMediaType())
        val coverPart = MultipartBody.Part.createFormData("cover", cover.name, coverRequest)

        ApiClient.postApi.changeCover(postId, coverPart).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Log.d("ProfileViewModel", "Cover changed successfully for post_id=$postId")
                    successMessage.value = response.body()?.message ?: "Cover changed successfully"
                    onSuccess(response.body()?.message ?: "Cover changed successfully")
                    loadPosts()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No details"
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid image format or data. Please try another image."
                        401 -> "Authentication failed. Please log in again."
                        403 -> "You are not authorized to change this cover."
                        404 -> "Post not found."
                        413 -> "Image too large. Please choose a smaller file."
                        429 -> "Too many requests. Try again later."
                        else -> "Failed to change cover: ${response.code()} - ${errorBody}"
                    }
                    onFailure(errorMsg)
                    Log.e("ProfileViewModel", "Change cover failed: $errorBody")
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                isLoading.value = false
                onFailure("Network error: ${t.message}")
                Log.e("ProfileViewModel", "Change cover network error", t)
            }
        })
    }

    fun updatePostDescription(postId: Int, description: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (description.isBlank()) {
            onFailure("Description cannot be empty")
            Log.w("ProfileViewModel", "Update post failed: Description is empty")
            return
        }

        if (ApiClient.authToken.isNullOrEmpty()) {
            onFailure("Authentication required. Please log in again.")
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("No internet connection. Please check your network.")
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Updating post description for post_id=$postId, description=$description")

        ApiClient.postApi.updatePost(postId, description).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    Log.d("ProfileViewModel", "Post description updated successfully for post_id=$postId")
                    successMessage.value = response.body()?.message ?: "Post updated successfully"
                    onSuccess(response.body()?.message ?: "Post updated successfully")
                    loadPosts()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No details"
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid description. Please check your input."
                        401 -> "Authentication failed. Please log in again."
                        403 -> "You are not authorized to update this post."
                        404 -> "Post not found."
                        429 -> "Too many requests. Try again later."
                        else -> "Failed to update post: ${response.code()} - ${errorBody}"
                    }
                    onFailure(errorMsg)
                    Log.e("ProfileViewModel", "Update post failed: $errorBody")
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                isLoading.value = false
                onFailure("Network error: ${t.message}")
                Log.e("ProfileViewModel", "Update post network error", t)
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun enterEditMode() {
        user.value?.let {
            tempName.value = it.name
            tempEmail.value = it.email
            tempPhone.value = it.phone ?: ""
        }
        isEditMode.value = true
    }

    fun cancelEdit() {
        isEditMode.value = false
        tempName.value = ""
        tempEmail.value = ""
        tempPhone.value = ""
    }

    fun updateProfile() {
        if (tempName.value.isBlank()) {
            errorMessage.value = "Name cannot be empty"
            Log.w("ProfileViewModel", "Update failed: Name is empty")
            return
        }

        Log.d("ProfileViewModel", "Validating email: ${tempEmail.value}")
        if (!Patterns.EMAIL_ADDRESS.matcher(tempEmail.value).matches()) {
            errorMessage.value = "Please enter a valid email"
            Log.w("ProfileViewModel", "Update failed: Invalid email format: ${tempEmail.value}")
            return
        }

        if (tempPhone.value.isNotEmpty() && !tempPhone.value.matches(Regex("^[0-9+\\-\\s()\\.]*$"))) {
            errorMessage.value = "Please enter a valid phone number (e.g., +6281234567890)"
            Log.w("ProfileViewModel", "Update failed: Invalid phone number: ${tempPhone.value}")
            return
        }

        if (ApiClient.authToken.isNullOrEmpty()) {
            errorMessage.value = "Authentication required. Please log in again."
            Log.e("ProfileViewModel", "No auth token available")
            return
        }

        if (!isNetworkAvailable()) {
            errorMessage.value = "No internet connection. Please check your network."
            Log.e("ProfileViewModel", "No internet connection")
            return
        }

        if (user.value?.email == tempEmail.value && user.value?.name == tempName.value) {
            val normalizedPhone = if (tempPhone.value.isNotEmpty()) {
                tempPhone.value.replace(Regex("[^0-9+]"), "")
            } else {
                null
            }

            with(sharedPreferences.edit()) {
                if (normalizedPhone != null) {
                    putString("phone", normalizedPhone)
                } else {
                    remove("phone")
                }
                apply()
            }

            isEditMode.value = false
            successMessage.value = "Profile updated successfully"
            loadProfile()
            Log.d("ProfileViewModel", "No changes to name or email, only phone updated locally: $normalizedPhone")
            return
        }

        val normalizedPhone = if (tempPhone.value.isNotEmpty()) {
            tempPhone.value.replace(Regex("[^0-9+]"), "")
        } else {
            null
        }

        with(sharedPreferences.edit()) {
            if (normalizedPhone != null) {
                putString("phone", normalizedPhone)
            } else {
                remove("phone")
            }
            apply()
        }

        isLoading.value = true
        Log.d("ProfileViewModel", "Updating profile: name=${tempName.value}, email=${tempEmail.value}, local phone=$normalizedPhone")
        val updateRequest = ProfileUpdateRequest(
            name = tempName.value,
            email = tempEmail.value
        )

        ApiClient.userApi.updateProfile(updateRequest)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        isEditMode.value = false
                        successMessage.value = "Profile updated successfully"
                        Log.d("ProfileViewModel", "Profile update response: ${response.body()?.data?.user}")
                        loadProfile()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "No details"
                        errorMessage.value = when (response.code()) {
                            400 -> {
                                if (errorBody.contains("email", ignoreCase = true)) {
                                    "Invalid email format. Please use a valid email (e.g., user@example.com)."
                                } else {
                                    "Invalid data. Please check your input."
                                }
                            }
                            401 -> "Authentication failed. Please log in again."
                            403 -> "You are not authorized to update this profile."
                            409 -> "Email already in use. Please use a different email."
                            429 -> "Too many requests. Try again later."
                            else -> "Failed to update profile: ${response.code()} - ${response.message()}"
                        }
                        Log.e("ProfileViewModel", "Update profile failed: $errorBody")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = "Network error: ${t.message}"
                    Log.e("ProfileViewModel", "Update profile network error", t)
                }
            })
    }
}