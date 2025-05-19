package del.ifs22017.delcomapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import del.ifs22017.delcomapp.data.ProfileViewModel
import del.ifs22017.delcomapp.data.ProfileViewModelFactory
import del.ifs22017.delcomapp.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current))
) {
    val homeNavController = rememberNavController()
    val currentRoute = homeNavController.currentBackStackEntryAsState().value?.destination?.route

    val gradientColors = listOf(
        Color(0xFF005A9C),
        Color(0xFF00AEEF)
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode.value) "Edit Profile" else if (currentRoute == "posts") "Postingan" else "Beranda",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.isEditMode.value) viewModel.cancelEdit()
                        else navController.navigateUp()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (!viewModel.isEditMode.value && currentRoute == "profile" && viewModel.user.value != null) {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF005A9C)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    selected = currentRoute == "profile",
                    onClick = { homeNavController.navigate("profile") {
                        popUpTo(homeNavController.graph.startDestinationId)
                        launchSingleTop = true
                    } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Postingan") },
                    label = { Text("Postingan") },
                    selected = currentRoute == "posts",
                    onClick = { homeNavController.navigate("posts") {
                        popUpTo(homeNavController.graph.startDestinationId)
                        launchSingleTop = true
                    } }
                )
            }
        },
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = 0f,
                endY = 2000f
            )
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            NavHost(
                navController = homeNavController,
                startDestination = "profile",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("profile") {
                    ProfileScreen(viewModel)
                }
                composable("posts") {
                    PostsScreen()
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(viewModel: ProfileViewModel) {
    val user by viewModel.user
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    val isEditMode by viewModel.isEditMode
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("ProfileScreen", "Image picker result: uri=$uri")
        uri?.let {
            try {
                val file = FileUtil.fromUri(context, it)
                Log.d("ProfileScreen", "File created: ${file.absolutePath}, size: ${file.length()}")
                if (file.exists() && file.length() > 0) {
                    viewModel.updateProfilePhoto(file)
                } else {
                    Toast.makeText(context, "Invalid image file", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileScreen", "Invalid file: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process image: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileScreen", "Error processing image", e)
            }
        } ?: run {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
            Log.w("ProfileScreen", "No image selected")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("ProfileScreen", "Permission result: granted=$isGranted")
        if (isGranted) {
            imagePicker.launch("image/*")
        } else {
            Toast.makeText(context, "Permission denied. Cannot access gallery.", Toast.LENGTH_LONG).show()
            Log.w("ProfileScreen", "Gallery permission denied")
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            Log.d("ProfileScreen", "Error message displayed: $it")
            viewModel.errorMessage.value = null
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            Log.d("ProfileScreen", "Success message displayed: $it")
            viewModel.successMessage.value = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            user != null -> {
                if (isEditMode) {
                    EditProfileContent(viewModel, imagePicker, permissionLauncher)
                } else {
                    ViewProfileContent(viewModel, imagePicker, permissionLauncher)
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileContent(
    viewModel: ProfileViewModel,
    imagePicker: ManagedActivityResultLauncher<String, Uri?>,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    val user = viewModel.user.value!!
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val isFormValid by derivedStateOf {
        val nameValid = viewModel.tempName.value.isNotBlank()
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(viewModel.tempEmail.value).matches()
        val phoneValid = viewModel.tempPhone.value.isEmpty() || viewModel.tempPhone.value.matches(Regex("^[0-9+\\-\\s()\\.]*$"))

        nameError = if (nameValid) null else "Name cannot be empty"
        emailError = if (emailValid) null else "Please enter a valid email"
        phoneError = if (phoneValid) null else "Please enter a valid phone number (e.g., +6281234567890)"

        nameValid && emailValid && phoneValid
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF005A9C),
                            Color(0xFF4B2E83)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable {
                    Log.d("EditProfileContent", "Photo card clicked")
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            if (user.photo != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = "${user.photo}?t=${System.currentTimeMillis()}"
                    ),
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.tempName.value,
                    onValueChange = { viewModel.tempName.value = it },
                    label = { Text("Name") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Name",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorSupportingTextColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.tempEmail.value,
                    onValueChange = { viewModel.tempEmail.value = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    supportingText = {
                        if (emailError != null) {
                            Text(
                                text = emailError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorSupportingTextColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.tempPhone.value,
                    onValueChange = { viewModel.tempPhone.value = it },
                    label = { Text("Phone") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text("e.g., +6281234567890") },
                    isError = phoneError != null,
                    supportingText = {
                        if (phoneError != null) {
                            Text(
                                text = phoneError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "Note: Phone number is stored locally and will not sync across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorSupportingTextColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.cancelEdit() },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB00020),
                            Color(0xFF8B0000)
                        )
                    )
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    Log.d("EditProfileContent", "Save Changes clicked")
                    viewModel.updateProfile()
                },
                enabled = !viewModel.isLoading.value && isFormValid,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(50.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF005A9C),
                                Color(0xFF00AEEF)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewProfileContent(
    viewModel: ProfileViewModel,
    imagePicker: ManagedActivityResultLauncher<String, Uri?>,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    val user = viewModel.user.value!!

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF005A9C),
                            Color(0xFF4B2E83)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable {
                    Log.d("ViewProfileContent", "Photo card clicked")
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }
                .zIndex(1f),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            if (user.photo != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = "${user.photo}?t=${System.currentTimeMillis()}"
                    ),
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                ProfileDetailItem("Name", user.name)
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = Color(0xFF00AEEF).copy(alpha = 0.3f)
                )
                ProfileDetailItem("Email", user.email)
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = Color(0xFF00AEEF).copy(alpha = 0.3f)
                )
                ProfileDetailItem("Phone", user.phone ?: "-")
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = Color(0xFF00AEEF).copy(alpha = 0.3f)
                )
                ProfileDetailItem("Joined", user.created_at)
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.7f)
        )
    }
}