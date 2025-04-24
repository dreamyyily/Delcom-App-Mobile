package del.ifs22017.delcomapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import del.ifs22017.delcomapp.data.RegisterRequest
import del.ifs22017.delcomapp.data.RegisterResponse
import del.ifs22017.delcomapp.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val request = RegisterRequest(name, email, password)
                ApiClient.authApi.register(request).enqueue(object :
                    Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val registerResponse = response.body()!!
                            if (registerResponse.success) {
                                Toast.makeText(context, registerResponse.message, Toast.LENGTH_SHORT).show()
                                onRegisterSuccess()
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, registerResponse.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Registration failed. Check inputs.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(context, "All fields required", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Register")
        }
    }
}