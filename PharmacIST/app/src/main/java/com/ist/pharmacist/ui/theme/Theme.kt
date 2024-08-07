package com.ist.pharmacist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ist.pharmacist.R

//Color scheme for the app theme with light and dark mode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF158441), //darker green
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    primaryContainer = Color(0xf0a1a1a1),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color.LightGray,
    onSecondaryContainer = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1DBB5C), //green
    onPrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    primaryContainer = Color(0xf0a1a1a1),
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color.LightGray,
    onSecondaryContainer = Color.Black
)

object MapStyle {
    var mapStyle: Int = 0
}

@Composable
fun PharmacISTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (!darkTheme) {
            LightColorScheme
        } else {
            DarkColorScheme
        }
    val view = LocalView.current
    if(!darkTheme) {
        MapStyle.mapStyle = R.raw.map_style
    } else {
        MapStyle.mapStyle = R.raw.dark_map_style
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}