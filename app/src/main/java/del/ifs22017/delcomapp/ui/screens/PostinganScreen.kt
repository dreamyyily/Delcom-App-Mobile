package del.ifs22017.delcomapp.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import del.ifs22017.delcomapp.data.DetailedPost
import del.ifs22017.delcomapp.data.Post
import del.ifs22017.delcomapp.data.ProfileViewModel
import del.ifs22017.delcomapp.data.ProfileViewModelFactory
import del.ifs22017.delcomapp.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current))
) {
    val posts by viewModel.posts
    var showAddPostDialog by remember { mutableStateOf(false) }
    var showEditPostDialog by remember { mutableStateOf(false) }
    var showPostDetailDialog by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var detailedPost by remember { mutableStateOf<DetailedPost?>(null) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var newPostContent by remember { mutableStateOf("") }
    var editPostContent by remember { mutableStateOf("") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedEditCoverUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    val context = LocalContext.current

    // Image picker for cover
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (showEditPostDialog) {
            selectedEditCoverUri = uri
        } else {
            selectedCoverUri = uri
        }
        Log.d("PostsScreen", "Cover image selected: $uri")
    }

    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePicker.launch("image/*")
        } else {
            Toast.makeText(context, "Izin ditolak. Tidak dapat mengakses galeri.", Toast.LENGTH_LONG).show()
            Log.w("PostsScreen", "Izin galeri ditolak")
        }
    }

    // Handle success and error messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.successMessage.value = null
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPostDialog = true },
                shape = CircleShape,
                containerColor = Color(0xFF005A9C),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah Postingan",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (posts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Belum ada postingan tersedia.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedPost = post
                                    viewModel.getPostById(
                                        postId = post.id,
                                        onSuccess = { postDetail ->
                                            detailedPost = postDetail
                                            showPostDetailDialog = true
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Postingan #${post.id} by ${post.author.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = {
                                            editingPost = post
                                            editPostContent = post.description
                                            selectedEditCoverUri = null
                                            showEditPostDialog = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Postingan",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (post.cover != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = post.cover),
                                        contentDescription = "Sampul Postingan",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = post.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Likes: ${post.likes.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Comments: ${post.comments.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Post Dialog
    if (showAddPostDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddPostDialog = false
                newPostContent = ""
                selectedCoverUri = null
            },
            title = { Text("Tambah Postingan") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(permission)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pilih Gambar Sampul")
                    }
                    if (selectedCoverUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = selectedCoverUri),
                            contentDescription = "Sampul Terpilih",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostContent.isNotBlank() && selectedCoverUri != null) {
                            try {
                                val coverFile = FileUtil.fromUri(context, selectedCoverUri!!)
                                if (coverFile.exists() && coverFile.length() > 0) {
                                    viewModel.addPost(
                                        cover = coverFile,
                                        description = newPostContent,
                                        onSuccess = { postId ->
                                            showAddPostDialog = false
                                            newPostContent = ""
                                            selectedCoverUri = null
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Gambar sampul tidak valid", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Kesalahan memproses gambar: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e("PostsScreen", "Kesalahan memproses sampul", e)
                            }
                        } else {
                            Toast.makeText(context, "Deskripsi dan sampul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Tambah")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddPostDialog = false
                        newPostContent = ""
                        selectedCoverUri = null
                    }
                ) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Edit Post Dialog
    if (showEditPostDialog && editingPost != null) {
        AlertDialog(
            onDismissRequest = {
                showEditPostDialog = false
                editingPost = null
                editPostContent = ""
                selectedEditCoverUri = null
            },
            title = { Text("Edit Postingan #${editingPost?.id}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editPostContent,
                        onValueChange = { editPostContent = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            permissionLauncher.launch(permission)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ganti Gambar Sampul")
                    }
                    if (selectedEditCoverUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = selectedEditCoverUri),
                            contentDescription = "Sampul Baru Terpilih",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (editingPost?.cover != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = editingPost?.cover),
                            contentDescription = "Sampul Saat Ini",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val postId = editingPost?.id ?: return@Button
                        var updateCount = 0
                        var successCount = 0

                        // Update description if changed
                        if (editPostContent != editingPost?.description && editPostContent.isNotBlank()) {
                            updateCount++
                            viewModel.updatePostDescription(
                                postId = postId,
                                description = editPostContent,
                                onSuccess = {
                                    successCount++
                                    if (successCount == updateCount) {
                                        showEditPostDialog = false
                                        editingPost = null
                                        editPostContent = ""
                                        selectedEditCoverUri = null
                                    }
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // Update cover if changed
                        if (selectedEditCoverUri != null) {
                            try {
                                val coverFile = FileUtil.fromUri(context, selectedEditCoverUri!!)
                                if (coverFile.exists() && coverFile.length() > 0) {
                                    updateCount++
                                    viewModel.changePostCover(
                                        postId = postId,
                                        cover = coverFile,
                                        onSuccess = {
                                            successCount++
                                            if (successCount == updateCount) {
                                                showEditPostDialog = false
                                                editingPost = null
                                                editPostContent = ""
                                                selectedEditCoverUri = null
                                            }
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Gambar sampul tidak valid", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Kesalahan memproses gambar: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e("PostsScreen", "Kesalahan memproses sampul", e)
                            }
                        } else if (updateCount == 0) {
                            Toast.makeText(context, "Tidak ada perubahan untuk disimpan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Simpan")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditPostDialog = false
                        editingPost = null
                        editPostContent = ""
                        selectedEditCoverUri = null
                    }
                ) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Post Detail Dialog
    if (showPostDetailDialog && detailedPost != null) {
        AlertDialog(
            onDismissRequest = {
                showPostDetailDialog = false
                selectedPost = null
                detailedPost = null
            },
            title = { Text("Detail Postingan #${detailedPost?.id}") },
            text = {
                Column {
                    if (detailedPost?.cover != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = detailedPost?.cover),
                            contentDescription = "Sampul Postingan",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Author: ${detailedPost?.author?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detailedPost?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Likes: ${detailedPost?.likes?.size ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Comments:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (detailedPost?.comments?.isNotEmpty() == true) {
                        detailedPost?.comments?.forEach { comment ->
                            Text(
                                text = "- ${comment.comment} (Created: ${comment.created_at})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No comments yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    detailedPost?.my_comment?.let { myComment ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "My Comment: ${myComment.comment}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPostDetailDialog = false
                        selectedPost = null
                        detailedPost = null
                    }
                ) {
                    Text("Tutup")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}