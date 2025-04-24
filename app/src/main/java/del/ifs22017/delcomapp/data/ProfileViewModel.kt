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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileViewModel(private val context: Context) : ViewModel() {
    val user = mutableStateOf<ProfileUser?>(null)
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

        // Cek apakah email berubah
        if (user.value?.email == tempEmail.value && user.value?.name == tempName.value) {
            // Jika email dan nama tidak berubah, hanya simpan nomor telepon
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

        // Normalisasi nomor telepon
        val normalizedPhone = if (tempPhone.value.isNotEmpty()) {
            tempPhone.value.replace(Regex("[^0-9+]"), "")
        } else {
            null
        }

        // Simpan nomor telepon ke SharedPreferences
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