package io.github.tonytonycoder11.agentictaskmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/**
 * Soft, rounded shapes to match the calm "Warm Paper" look (cards 14dp, chips 8dp).
 */
private val AtmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The app theme. We deliberately use our own bespoke light/dark schemes (not Material You dynamic
 * colour) so the calm ivory identity is consistent on every device, and expose the semantic
 * [StatusColors] through [LocalStatusColors].
 */
@Composable
fun AtmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AtmDarkColors else AtmLightColors
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AtmTypography,
            shapes = AtmShapes,
            content = content,
        )
    }
}
