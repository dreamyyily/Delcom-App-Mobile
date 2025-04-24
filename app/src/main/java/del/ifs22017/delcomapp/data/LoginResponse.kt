package del.ifs22017.delcomapp.data

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: Data
)

data class Data(
    val user: User,
    val token: String
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val photo: String?,
    val token: String?
)