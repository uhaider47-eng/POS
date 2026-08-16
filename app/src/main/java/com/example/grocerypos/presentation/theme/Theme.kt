package com.example.grocerypos.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF137333)
val GreenOnPrimary = Color(0xFFFFFFFF)
val GreenPrimaryContainer = Color(0xFFE6F4EA)
val GreenOnPrimaryContainer = Color(0xFF0D5223)

val SecondaryTeal = Color(0xFF00796B)
val BackgroundLight = Color(0xFFF9FBF9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F2)
val OutlineLight = Color(0xFFD0D7D1)
val TextPrimary = Color(0xFF1E2922)
val TextSecondary = Color(0xFF5A665E)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun GroceryPosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
