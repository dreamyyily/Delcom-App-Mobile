package del.ifs22017.delcomapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005A9C), // Biru Tua
    onPrimary = Color.White,
    background = Color.White, // Putih
    onBackground = Color(0xFF005A9C), // Biru Tua
    surface = Color.White, // Putih
    onSurface = Color(0xFF005A9C), // Biru Tua
    secondary = Color(0xFF4B2E83), // Ungu
    secondaryContainer = Color(0xFFE6E1F5), // Ungu muda untuk container
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFF4B2E83), // Ungu
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color(0xFF00AEEF) // Biru Muda untuk border
)

@Composable
fun DelcomAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}