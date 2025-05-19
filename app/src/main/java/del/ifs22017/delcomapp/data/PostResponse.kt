package del.ifs22017.delcomapp.data

data class PostResponse(
    val success: Boolean,
    val message: String,
    val data: PostData? = null
)

data class PostData(
    val post_id: Int
)

data class Author(
    val name: String,
    val photo: String?
)

data class Comment(
    val id: Int,
    val comment: String,
    val created_at: String,
    val updated_at: String
)

data class Post(
    val id: Int,
    val user_id: Int,
    val cover: String,
    val description: String,
    val created_at: String,
    val updated_at: String,
    val author: Author,
    val likes: List<Int>,
    val comments: List<Int>
)

data class DetailedPost(
    val id: Int,
    val user_id: Int,
    val cover: String,
    val description: String,
    val created_at: String,
    val updated_at: String,
    val author: Author,
    val likes: List<Int>,
    val comments: List<Comment>,
    val my_comment: Comment?
)

data class PostsResponse(
    val success: Boolean,
    val message: String,
    val data: PostsData
)

data class PostsData(
    val posts: List<Post>
)

data class SinglePostResponse(
    val success: Boolean,
    val message: String,
    val data: SinglePostData
)

data class SinglePostData(
    val post: DetailedPost
)