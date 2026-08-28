package com.manidigit.flashlearn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF126E82), secondary = Color(0xFF7655C7), tertiary = Color(0xFFFFA62B),
    background = Color(0xFFF7F8FC), surface = Color(0xFFFFFFFF),
    onPrimary = Color.White, onSecondary = Color.White
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF5ED7D0), secondary = Color(0xFFB9A1FF), tertiary = Color(0xFFFFC56A),
    background = Color(0xFF0E1117), surface = Color(0xFF171B24), surfaceVariant = Color(0xFF242A35)
)

@Composable
fun FlashLearnTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
