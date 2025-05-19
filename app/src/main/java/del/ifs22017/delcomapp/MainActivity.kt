package del.ifs22017.delcomapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import del.ifs22017.delcomapp.ui.screens.HomeScreen
import del.ifs22017.delcomapp.ui.screens.LoginScreen
import del.ifs22017.delcomapp.ui.screens.RegisterScreen
import del.ifs22017.delcomapp.ui.theme.DelcomAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DelcomAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(navController = navController, onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("register") {
                        RegisterScreen(navController = navController, onRegisterSuccess = {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        })
                    }
                    composable("home") {
                        HomeScreen(navController = navController)
                    }

                }
            }
        }
    }
}