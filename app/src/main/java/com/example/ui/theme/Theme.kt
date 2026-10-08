package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AccentColor
import com.example.data.model.ThemeMode

fun getNovaDarkColorScheme(accentHex: Long = 0xFF0EA5E9): androidx.compose.material3.ColorScheme {
    val accent = Color(accentHex)
    return darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = Color(accentHex).copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        secondary = NovaIndigoSecondary,
        onSecondary = Color.White,
        tertiary = NovaVioletAccent,
        background = NovaDarkBackground,
        onBackground = NovaDarkTextPrimary,
        surface = NovaDarkSurface,
        onSurface = NovaDarkTextPrimary,
        surfaceVariant = NovaDarkSurfaceVariant,
        onSurfaceVariant = NovaDarkTextSecondary,
        outline = NovaDarkBorder
    )
}

fun getNovaLightColorScheme(accentHex: Long = 0xFF0EA5E9): androidx.compose.material3.ColorScheme {
    val accent = Color(accentHex)
    return lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = Color(accentHex).copy(alpha = 0.12f),
        onPrimaryContainer = Color(accentHex),
        secondary = NovaIndigoSecondary,
        onSecondary = Color.White,
        tertiary = NovaVioletAccent,
        background = NovaLightBackground,
        onBackground = NovaLightTextPrimary,
        surface = NovaLightSurface,
        onSurface = NovaLightTextPrimary,
        surfaceVariant = NovaLightSurfaceVariant,
        onSurfaceVariant = NovaLightTextSecondary,
        outline = NovaLightBorder
    )
}

val IncognitoColorScheme = darkColorScheme(
    primary = IncognitoAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1E63),
    onPrimaryContainer = IncognitoText,
    secondary = Color(0xFFC084FC),
    background = IncognitoBackground,
    onBackground = IncognitoText,
    surface = IncognitoSurface,
    onSurface = IncognitoText,
    surfaceVariant = IncognitoSurfaceVariant,
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = Color(0xFF4C2889)
)

@Composable
fun NovaBrowserTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.NOVA_CYAN,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        isIncognito -> IncognitoColorScheme
        darkTheme -> getNovaDarkColorScheme(accentColor.hex)
        else -> getNovaLightColorScheme(accentColor.hex)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
