package del.ifs22017.delcomapp.data

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val data: ProfileData
)

data class ProfileData(
    val user: ProfileUser
)

data class ProfileUser(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val photo: String?,
    val verified_at: String?,
    val created_at: String,
    val updated_at: String


)

data class ProfileUpdateRequest(
    val name: String,
    val email: String,
//    val phone: String?
)
