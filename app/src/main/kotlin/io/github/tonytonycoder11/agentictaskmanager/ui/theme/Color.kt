package io.github.tonytonycoder11.agentictaskmanager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Warm Paper": a low-chroma ivory palette. Bespoke rather than system dynamic colour so the
 * identity stays consistent across devices.
 */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF8C6A4D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE0D3),
    onPrimaryContainer = Color(0xFF4A3826),
    secondary = Color(0xFF6B6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE7DB),
    onSecondaryContainer = Color(0xFF383228),
    tertiary = Color(0xFF5E7A90),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE7EE),
    onTertiaryContainer = Color(0xFF1A2A36),
    background = Color(0xFFF7F4EF),
    onBackground = Color(0xFF2C2A26),
    surface = Color(0xFFFCFAF6),
    onSurface = Color(0xFF2C2A26),
    surfaceVariant = Color(0xFFEAE3D7),
    onSurfaceVariant = Color(0xFF6E695F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F1EB),
    surfaceContainer = Color(0xFFEFEBE3),
    surfaceContainerHigh = Color(0xFFE8E3D9),
    surfaceContainerHighest = Color(0xFFE2DCD0),
    outline = Color(0xFF8C857A),
    outlineVariant = Color(0xFFE0DACE),
    error = Color(0xFFA1473D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF4DDD7),
    onErrorContainer = Color(0xFF3A0E08),
    inverseSurface = Color(0xFF322F2A),
    inverseOnSurface = Color(0xFFF3EFE7),
    inversePrimary = Color(0xFFC2A283),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC2A283),
    onPrimary = Color(0xFF3A2A1C),
    primaryContainer = Color(0xFF4A3828),
    onPrimaryContainer = Color(0xFFE8D6C4),
    secondary = Color(0xFFD0C7B6),
    onSecondary = Color(0xFF353026),
    secondaryContainer = Color(0xFF4C4639),
    onSecondaryContainer = Color(0xFFEEE4D4),
    tertiary = Color(0xFF9DB3C4),
    onTertiary = Color(0xFF1E3140),
    tertiaryContainer = Color(0xFF3A4D5B),
    onTertiaryContainer = Color(0xFFDDE7EE),
    background = Color(0xFF1A1916),
    onBackground = Color(0xFFE7E2D8),
    surface = Color(0xFF211F1B),
    onSurface = Color(0xFFE7E2D8),
    surfaceVariant = Color(0xFF4A463C),
    onSurfaceVariant = Color(0xFFA8A293),
    surfaceContainerLowest = Color(0xFF131210),
    surfaceContainerLow = Color(0xFF26241F),
    surfaceContainer = Color(0xFF2C2A24),
    surfaceContainerHigh = Color(0xFF34312A),
    surfaceContainerHighest = Color(0xFF3F3B33),
    outline = Color(0xFF847D70),
    outlineVariant = Color(0xFF3A372F),
    error = Color(0xFFE0A096),
    onError = Color(0xFF44140E),
    errorContainer = Color(0xFF623A33),
    onErrorContainer = Color(0xFFF4DDD7),
    inverseSurface = Color(0xFFE7E2D8),
    inverseOnSurface = Color(0xFF322F2A),
    inversePrimary = Color(0xFF8C6A4D),
)

internal val AtmLightColors = LightScheme
internal val AtmDarkColors = DarkScheme

/**
 * Semantic colours for task status and priority. Pills fill with the *Container* tone and label
 * with the matching colour; tones without a container are rendered as text, not a filled block.
 */
data class StatusColors(
    val actionable: Color,
    val actionableContainer: Color,
    val overdue: Color,
    val blocked: Color,
    val priorityUrgent: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
)

internal val LightStatusColors = StatusColors(
    actionable = Color(0xFF4F7D5C),
    actionableContainer = Color(0xFFE4EDE2),
    overdue = Color(0xFFA65A50),
    blocked = Color(0xFF7C6A9C),
    priorityUrgent = Color(0xFFA85A4F),
    priorityHigh = Color(0xFF9A7038),
    priorityMedium = Color(0xFF5E7A90),
    priorityLow = Color(0xFF7A766A),
)

internal val DarkStatusColors = StatusColors(
    actionable = Color(0xFF8FB89A),
    actionableContainer = Color(0xFF2C3A30),
    overdue = Color(0xFFD49387),
    blocked = Color(0xFFBCA0C0),
    priorityUrgent = Color(0xFFD4948A),
    priorityHigh = Color(0xFFD2AC7E),
    priorityMedium = Color(0xFF9DB3C4),
    priorityLow = Color(0xFFA8A292),
)

/** Lets composables read the active [StatusColors] via `LocalStatusColors.current`. */
val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
